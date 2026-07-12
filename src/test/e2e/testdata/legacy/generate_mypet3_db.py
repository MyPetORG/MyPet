#!/usr/bin/env python3
"""Deterministic generator for testdata/legacy/pets.db.

Produces a MyPet 3.x-shaped SQLite database (pre-v4 schema: `players` table
keyed on `internal_uuid` + a separate `mojang_uuid` column, `pets.owner_uuid`
pointing at `internal_uuid`) that the plugin's `MigrationService` detects as
`InstallType.UPGRADE_3X` (schema probe: `players.internal_uuid` column
present) and converts on first boot via the migration chain:
`TranslatePetOwnerUuidToMojang` -> `RebuildPlayersSchemaOnMojangUuid` ->
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
# RFC 4122 variant) and src/test/e2e/lib/uuid.ts (the TS port used by
# migration/mypet3.spec.ts) bit-for-bit — all three must agree.
# ---------------------------------------------------------------------------
def offline_uuid(username: str) -> str:
    md5 = bytearray(hashlib.md5(f"OfflinePlayer:{username}".encode("utf-8")).digest())
    md5[6] = (md5[6] & 0x0F) | 0x30  # version 3
    md5[8] = (md5[8] & 0x3F) | 0x80  # variant RFC 4122
    h = md5.hex()
    return f"{h[0:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"


# ---------------------------------------------------------------------------
# Minimal Mojang-NBT writer (just enough for TAG_Compound/TAG_End/TAG_Int),
# GZIP-wrapped to match NbtUtil.writeCompressed's on-disk format exactly.
# ---------------------------------------------------------------------------
TAG_END = 0
TAG_INT = 3
TAG_COMPOUND = 10


def _write_named(buf: io.BytesIO, tag_type: int, name: str):
    buf.write(struct.pack(">B", tag_type))
    name_bytes = name.encode("utf-8")
    buf.write(struct.pack(">H", len(name_bytes)))
    buf.write(name_bytes)


def compound_bytes(int_fields: dict[str, int]) -> bytes:
    """Root TAG_Compound (unnamed) containing only TAG_Int entries."""
    buf = io.BytesIO()
    _write_named(buf, TAG_COMPOUND, "")  # root tag, empty name (Minecraft convention)
    for key, value in int_fields.items():
        _write_named(buf, TAG_INT, key)
        buf.write(struct.pack(">i", value))
    buf.write(struct.pack(">B", TAG_END))  # close root compound
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

COW_EXP = 500.0

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
            mojang_uuid VARCHAR(36),
            name VARCHAR(255),
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
        """
    )


def insert_rows(conn: sqlite3.Connection):
    conn.execute(
        "INSERT INTO players (internal_uuid, mojang_uuid, name, auto_respawn, "
        "auto_respawn_min, capture_mode, health_bar, pet_idle_volume, "
        "extended_info, multi_world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        (INTERNAL_UUID, MOJANG_UUID, LEGACY_OWNER_NAME, False, 0, False, 0, 1.0,
         EMPTY_NBT, "{}"),
    )

    def insert_pet(uuid: str, name: str, pet_type: str, info: bytes):
        conn.execute(
            "INSERT INTO pets (uuid, owner_uuid, exp, health, respawn_time, name, "
            "type, last_used, hunger, world_group, wants_to_spawn, skilltree, "
            "skills, info) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (uuid, INTERNAL_UUID, COW_EXP if pet_type == "Cow" else 0.0, 20.0, 0,
             name, pet_type, LAST_USED_MS, 100, WORLD_GROUP, False, None,
             EMPTY_NBT, info),
        )

    insert_pet(COW_UUID, "LegacyCow", "Cow", b"")  # empty info: no legacy visual state to migrate
    insert_pet(RABBIT_UUID, "LegacyRabbit", "Rabbit", legacy_info_blob({"Variant": RABBIT_VARIANT}))
    insert_pet(FISH_UUID, "LegacyFish", "TropicalFish", legacy_info_blob({"Variant": FISH_VARIANT}))

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
    print(f"  Cow uuid={COW_UUID} exp={COW_EXP}")
    print(f"  Rabbit uuid={RABBIT_UUID} legacy Variant={RABBIT_VARIANT} (-> Rabbit.Type.GOLD, vanilla RabbitType:4 (TAG_Int on this Paper build, no byte suffix))")
    print(f"  TropicalFish uuid={FISH_UUID} legacy Variant={FISH_VARIANT} "
          f"(shape={FISH_SHAPE} patternIndex={FISH_PATTERN_INDEX} "
          f"bodyColor={FISH_BODY_COLOR} patternColor={FISH_PATTERN_COLOR})")


if __name__ == "__main__":
    main()
