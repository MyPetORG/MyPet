#!/usr/bin/env python3
"""Deterministic generator for testdata/legacy/pets.db.

Produces a MyPet 3.x-shaped SQLite database (pre-v4 schema: `players` table
keyed on `internal_uuid` + a separate `mojang_uuid` column, `pets.owner_uuid`
pointing at `internal_uuid`) that the plugin's `MigrationService` detects as
`InstallType.UPGRADE_3X` (schema probe: `players.internal_uuid` column
present) and converts on first boot via the migration chain:
`BackfillOfflineUuidsFromName` -> `TranslatePetOwnerUuidToMojang` ->
`RebuildPlayersSchemaOnMojangUuid` ->
`EntitySnapshotMigration` (+ `DropLegacyInfoVersionColumn`).

Schema derived by reading, byte-for-byte:
  - plugin/src/main/java/de/Keyle/MyPet/repository/types/SqLiteRepository.java
    (initStructure() — the v4 `pets`/`players`/`info` shapes)
  - plugin/src/main/java/de/Keyle/MyPet/migration/migrations/
    RebuildPlayersSchemaOnMojangUuid.java (legacy `players` columns it reads:
    internal_uuid, mojang_uuid, name, auto_respawn, auto_respawn_min,
    capture_mode, health_bar, pet_idle_volume, extended_info, multi_world,
    last_update)
  - .../TranslatePetOwnerUuidToMojang.java (pets.owner_uuid must reference
    players.internal_uuid pre-migration)
  - .../DropLegacyInfoVersionColumn.java (legacy `info.version` column)
  - .../entitysnapshot/LegacyPetReader.java (per-species legacy `info` NBT
    keys: Rabbit's byte "Variant" id and TropicalFish's packed "Variant" int)

`pets.info` / `pets.skills` / `players.extended_info` are GZIP-compressed
Mojang NBT (`de.Keyle.MyPet.util.NbtUtil.writeCompressed` — Adventure's
`BinaryTagIO.writer().write(compound, out, GZIP)`), reimplemented here with a
tiny hand-rolled NBT writer (only TAG_Compound/TAG_End/TAG_Int are needed).

Usage: python3 generate_mypet3_db.py [output_path]
(no args -> writes ./pets.db next to this script, which is the committed
artifact `writeFiles` stages into the plugwright run dir.)
"""
import gzip
import hashlib
import io
import os
import sqlite3
import struct
import sys

# ---------------------------------------------------------------------------
# Offline-mode UUID (Bukkit's UUID.nameUUIDFromBytes("OfflinePlayer:" + name))
# Mirrors src/test/e2e/taming/taming.spec.ts's `offlineUuid` (MD5, version 3,
# RFC 4122 variant) and the Java `offlineUuid` in
# migration/migrations/BackfillOfflineUuidsFromName.java bit-for-bit — all
# three must agree, since the plugin derives the same value at migration time
# that an offline-mode server hands the plugin at login.
# ---------------------------------------------------------------------------
def offline_uuid(username: str) -> str:
    md5 = bytearray(hashlib.md5(f"OfflinePlayer:{username}".encode("utf-8")).digest())
    md5[6] = (md5[6] & 0x0F) | 0x30  # version 3
    md5[8] = (md5[8] & 0x3F) | 0x80  # variant RFC 4122
    h = md5.hex()
    return f"{h[0:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"


# ---------------------------------------------------------------------------
# Minimal Mojang-NBT writer (TAG_Compound/TAG_End/TAG_Int, plus TAG_Byte,
# TAG_String and TAG_List-of-compound for the legacy Equipment fixture),
# GZIP-wrapped to match NbtUtil.writeCompressed's on-disk format exactly.
# ---------------------------------------------------------------------------
TAG_END = 0
TAG_BYTE = 1
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10


def _write_named(buf: io.BytesIO, tag_type: int, name: str):
    buf.write(struct.pack(">B", tag_type))
    name_bytes = name.encode("utf-8")
    buf.write(struct.pack(">H", len(name_bytes)))
    buf.write(name_bytes)


def _write_string_payload(buf: io.BytesIO, value: str):
    encoded = value.encode("utf-8")
    buf.write(struct.pack(">H", len(encoded)))
    buf.write(encoded)


class Byte(int):
    """Marker so a dict value round-trips as TAG_Byte rather than TAG_Int."""


def _write_payload(buf: io.BytesIO, value):
    """Writes a value's payload; the caller has already written type + name."""
    if isinstance(value, Byte):
        buf.write(struct.pack(">b", int(value)))
    elif isinstance(value, bool):
        buf.write(struct.pack(">b", 1 if value else 0))
    elif isinstance(value, int):
        buf.write(struct.pack(">i", value))
    elif isinstance(value, str):
        _write_string_payload(buf, value)
    elif isinstance(value, dict):
        _write_compound_body(buf, value)
    elif isinstance(value, list):
        # TAG_List of TAG_Compound — the only list shape these fixtures need.
        buf.write(struct.pack(">B", TAG_COMPOUND))
        buf.write(struct.pack(">i", len(value)))
        for entry in value:
            _write_compound_body(buf, entry)
    else:
        raise TypeError(f"unsupported NBT value: {value!r}")


def _tag_type_of(value) -> int:
    if isinstance(value, Byte) or isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, dict):
        return TAG_COMPOUND
    if isinstance(value, list):
        return TAG_LIST
    raise TypeError(f"unsupported NBT value: {value!r}")


def _write_compound_body(buf: io.BytesIO, fields: dict):
    for key, value in fields.items():
        _write_named(buf, _tag_type_of(value), key)
        _write_payload(buf, value)
    buf.write(struct.pack(">B", TAG_END))


def compound_bytes(fields: dict) -> bytes:
    """Root TAG_Compound (unnamed). Values map to NBT types by Python type;
    wrap an int in Byte(...) to force TAG_Byte."""
    buf = io.BytesIO()
    _write_named(buf, TAG_COMPOUND, "")  # root tag, empty name (Minecraft convention)
    _write_compound_body(buf, fields)
    return buf.getvalue()


def gzip_compress(data: bytes) -> bytes:
    out = io.BytesIO()
    # mtime=0 for reproducible bytes across regenerations.
    with gzip.GzipFile(fileobj=out, mode="wb", mtime=0) as gz:
        gz.write(data)
    return out.getvalue()


EMPTY_NBT = gzip_compress(compound_bytes({}))


def legacy_info_blob(int_fields: dict[str, int]) -> bytes:
    """A pre-v4 curated NBT blob: has no root "id" key, which is exactly how
    EntitySnapshotMigration.isLegacy() distinguishes it from vanilla NBT."""
    return gzip_compress(compound_bytes(int_fields))


# ---------------------------------------------------------------------------
# Fixture identities (fixed, not random, so the spec's assertions are
# computable in advance rather than read back from a live run).
# ---------------------------------------------------------------------------
LEGACY_OWNER_NAME = "LegacyOwner"
INTERNAL_UUID = "44444444-4444-4444-4444-444444444444"  # pre-v4 opaque internal id
MOJANG_UUID = offline_uuid(LEGACY_OWNER_NAME)  # what the bot's offline UUID resolves to

COW_UUID = "11111111-1111-1111-1111-111111111111"
RABBIT_UUID = "22222222-2222-2222-2222-222222222222"
FISH_UUID = "33333333-3333-3333-3333-333333333333"
ZOMBIE_UUID = "99999999-9999-9999-9999-999999999999"

# Zombie wearing armor, stored the way MyPet 3 stored it: an "Equipment" list of
# item compounds, each being `itemStack.save(...)` output with an extra "Slot"
# string. The item payload is the PRE-1.20.5 vanilla shape — capital "Count" and a
# "tag" sub-compound — which is what any server that ran MyPet 3 on <=1.20.4 has
# on disk, and what needs Mojang's DataFixerUpper to become a modern
# count/components item. The enchantment lives in "tag" specifically so a decode
# that silently drops it is visible: a helmet that arrives unenchanted proves the
# fixer never ran, even though the item itself survived.
ZOMBIE_EQUIPMENT = [
    {
        "id": "minecraft:iron_helmet",
        "Count": Byte(1),
        "tag": {"Damage": 0, "RepairCost": 0},
        "Slot": "HEAD",
    },
]

COW_EXP = 500.0

# --- Offline-mode rows (BackfillOfflineUuidsFromName) ------------------------
# A MyPet 3 server running offline mode never populated mojang_uuid: its join
# handler only wrote that column when the joining UUID *differed* from the
# name-derived offline UUID, which offline-mode servers never do. These rows
# reproduce that state faithfully. Before BackfillOfflineUuidsFromName they
# were deleted outright by RebuildPlayersSchemaOnMojangUuid.
OFFLINE_OWNER_NAME = "OfflineOwner"
OFFLINE_INTERNAL_UUID = "55555555-5555-5555-5555-555555555555"
OFFLINE_COW_UUID = "77777777-7777-7777-7777-777777777777"

# Unrecoverable: no Mojang UUID *and* no name. Nothing identifies this player,
# so the backfill must skip it (not throw) and leave it for the rebuild to
# drop. Its presence in the fixture is the regression test for the backfill's
# post-condition query: a post-condition written as plain
# `WHERE mojang_uuid IS NULL`, without the name guard, throws here, fails the
# migration, and MigrationService disables MyPet for the whole suite.
NAMELESS_INTERNAL_UUID = "66666666-6666-6666-6666-666666666666"
NAMELESS_COW_UUID = "88888888-8888-8888-8888-888888888888"

# Rabbit: MyPet 3 stored fur type as a byte id under "Variant" — see
# LegacyPetReader.rabbitTypeByLegacyId: 4 = GOLD (never the post-migration
# default BROWN, so a lost-migration regression is observable).
RABBIT_VARIANT = 4  # -> Rabbit.Type.GOLD -> vanilla NBT RabbitType:4 (TAG_Int on this Paper build, no byte suffix)

# TropicalFish: MyPet 3 packed shape|pattern<<8|bodyColor<<16|patternColor<<24
# into one int (LegacyPetReader.applyTropicalFish's own comment: "vanilla
# layout" — Mojang's real TropicalFish "Variant" NBT int uses the identical
# bit layout, confirmed against org.bukkit.entity.TropicalFish$Pattern's and
# org.bukkit.DyeColor's *ordinal* order, both extracted from the installed
# paper-api jar with javap — DyeColor's ordinal equals Mojang's internal wool-
# style color id (WHITE=0..BLACK=15), matching LegacyPetReader's own
# `dyes[idx]` ordinal indexing). shape=1 (large) + patternIndex=3 (BLOCKFISH
# within the large group) + bodyColor=ORANGE(1) + patternColor=BLUE(11):
FISH_SHAPE = 1
FISH_PATTERN_INDEX = 3
FISH_BODY_COLOR = 1   # DyeColor.ORANGE (ordinal)
FISH_PATTERN_COLOR = 11  # DyeColor.BLUE (ordinal)
FISH_VARIANT = (
    (FISH_SHAPE & 0xFF)
    | ((FISH_PATTERN_INDEX & 0xFF) << 8)
    | ((FISH_BODY_COLOR & 0xFF) << 16)
    | ((FISH_PATTERN_COLOR & 0xFF) << 24)
)
# = 184615681 (0x0B010301) at time of writing; recomputed here, never hardcoded twice.

LAST_USED_MS = 1750000000000  # fixed, arbitrary — repository.cleanup() is admin-triggered only

# CommandSwitch.openSwitchMenu filters stored pets by
# `WorldGroup.getGroupByWorld(world).getName()` — the un-configured default
# group is literally named "default" (api/.../api/WorldGroup.java,
# DEFAULT_GROUP = new WorldGroup("default", false)), NOT the world's own name
# ("world"). A pet whose stored world_group doesn't match is silently
# filtered out of the /petswitch GUI (first live-run bug found in Task 9:
# the selection GUI opened but showed zero items).
WORLD_GROUP = "default"


def build_schema(conn: sqlite3.Connection):
    conn.executescript(
        """
        CREATE TABLE players (
            internal_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
            mojang_uuid VARCHAR(36) UNIQUE,
            name VARCHAR(16) UNIQUE,
            auto_respawn BOOLEAN,
            auto_respawn_min INTEGER,
            capture_mode BOOLEAN,
            health_bar INTEGER,
            pet_idle_volume FLOAT,
            extended_info BLOB,
            multi_world VARCHAR(2000),
            last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE pets (
            uuid VARCHAR(36) NOT NULL PRIMARY KEY,
            owner_uuid VARCHAR(36) NOT NULL,
            exp DOUBLE,
            health DOUBLE,
            respawn_time INTEGER,
            name VARCHAR(1024),
            type VARCHAR(20),
            last_used BIGINT,
            hunger INTEGER,
            world_group VARCHAR(255),
            wants_to_spawn BOOLEAN,
            skilltree VARCHAR(255),
            skills BLOB,
            info BLOB,
            last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE info (
            mypet_version VARCHAR(20),
            mypet_build VARCHAR(20),
            version INTEGER UNIQUE,
            last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- SqLiteRepository#createTimestampTrigger, one per table (pets/uuid,
        -- players/internal_uuid, info/version). Reproduced verbatim so the fixture is a
        -- structurally real 3.x database: a migration that mishandles them (or that a
        -- trigger fires against) fails here rather than only on a live server.
        CREATE TRIGGER [update_time_trigger_pets]
        AFTER UPDATE ON pets FOR EACH ROW
        WHEN NEW.last_update < OLD.last_update
        BEGIN
          UPDATE pets SET last_update=CURRENT_TIMESTAMP WHERE NEW.uuid=OLD.uuid;
        END;

        CREATE TRIGGER [update_time_trigger_players]
        AFTER UPDATE ON players FOR EACH ROW
        WHEN NEW.last_update < OLD.last_update
        BEGIN
          UPDATE players SET last_update=CURRENT_TIMESTAMP
            WHERE NEW.internal_uuid=OLD.internal_uuid;
        END;

        CREATE TRIGGER [update_time_trigger_info]
        AFTER UPDATE ON info FOR EACH ROW
        WHEN NEW.last_update < OLD.last_update
        BEGIN
          UPDATE info SET last_update=CURRENT_TIMESTAMP WHERE NEW.version=OLD.version;
        END;
        """
    )


def insert_rows(conn: sqlite3.Connection):
    def insert_player(internal_uuid: str, mojang_uuid: str | None, name: str | None):
        conn.execute(
            "INSERT INTO players (internal_uuid, mojang_uuid, name, auto_respawn, "
            "auto_respawn_min, capture_mode, health_bar, pet_idle_volume, "
            "extended_info, multi_world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (internal_uuid, mojang_uuid, name, False, 0, False, 0, 1.0,
             EMPTY_NBT, "{}"),
        )

    def insert_pet(uuid: str, name: str, pet_type: str, info: bytes,
                   owner: str = INTERNAL_UUID):
        conn.execute(
            "INSERT INTO pets (uuid, owner_uuid, exp, health, respawn_time, name, "
            "type, last_used, hunger, world_group, wants_to_spawn, skilltree, "
            "skills, info) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (uuid, owner, COW_EXP if pet_type == "Cow" else 0.0, 20.0, 0,
             name, pet_type, LAST_USED_MS, 100, WORLD_GROUP, False, None,
             EMPTY_NBT, info),
        )

    # Online-mode-shaped row: mojang_uuid populated. Migrates via the
    # pre-existing chain; guards against a regression there.
    insert_player(INTERNAL_UUID, MOJANG_UUID, LEGACY_OWNER_NAME)
    insert_pet(COW_UUID, "LegacyCow", "Cow", b"")  # empty info: no legacy visual state to migrate
    insert_pet(RABBIT_UUID, "LegacyRabbit", "Rabbit", legacy_info_blob({"Variant": RABBIT_VARIANT}))
    insert_pet(FISH_UUID, "LegacyFish", "TropicalFish", legacy_info_blob({"Variant": FISH_VARIANT}))
    insert_pet(ZOMBIE_UUID, "LegacyZombie", "Zombie",
               legacy_info_blob({"Equipment": ZOMBIE_EQUIPMENT}))

    # Offline-mode-shaped row: mojang_uuid NULL, name present -> recoverable.
    insert_player(OFFLINE_INTERNAL_UUID, None, OFFLINE_OWNER_NAME)
    insert_pet(OFFLINE_COW_UUID, "OfflineCow", "Cow", b"", owner=OFFLINE_INTERNAL_UUID)

    # Unrecoverable row: mojang_uuid NULL, name NULL.
    insert_player(NAMELESS_INTERNAL_UUID, None, None)
    insert_pet(NAMELESS_COW_UUID, "OrphanCow", "Cow", b"", owner=NAMELESS_INTERNAL_UUID)

    conn.execute(
        "INSERT INTO info (mypet_version, mypet_build, version) VALUES (?, ?, ?)",
        ("3.7.4", "legacy", 15),
    )


def main():
    out_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "pets.db")
    if os.path.exists(out_path):
        os.remove(out_path)
    conn = sqlite3.connect(out_path)
    try:
        build_schema(conn)
        insert_rows(conn)
        conn.commit()
    finally:
        conn.close()

    print(f"Wrote {out_path}")
    print(f"  LegacyOwner internal_uuid={INTERNAL_UUID} mojang_uuid={MOJANG_UUID}")
    print(f"  OfflineOwner internal_uuid={OFFLINE_INTERNAL_UUID} mojang_uuid=NULL "
          f"(backfills to {offline_uuid(OFFLINE_OWNER_NAME)})")
    print(f"  Nameless internal_uuid={NAMELESS_INTERNAL_UUID} mojang_uuid=NULL name=NULL "
          f"(unrecoverable, dropped by the rebuild)")
    print(f"  Cow uuid={COW_UUID} exp={COW_EXP}")
    print(f"  Rabbit uuid={RABBIT_UUID} legacy Variant={RABBIT_VARIANT} (-> Rabbit.Type.GOLD, vanilla RabbitType:4 (TAG_Int on this Paper build, no byte suffix))")
    print(f"  TropicalFish uuid={FISH_UUID} legacy Variant={FISH_VARIANT} "
          f"(shape={FISH_SHAPE} patternIndex={FISH_PATTERN_INDEX} "
          f"bodyColor={FISH_BODY_COLOR} patternColor={FISH_PATTERN_COLOR})")


if __name__ == "__main__":
    main()
