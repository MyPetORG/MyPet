package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.NBTStorage;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Immutable, value-typed pet record — the canonical "pet at rest"
 * representation that the repository loads from disk and that callers pass
 * around between deactivation and reactivation.
 *
 * <p><b>Mutation pattern.</b> Use {@link Builder} (via {@link #builder} or
 * {@link #toBuilder}) for multi-field construction; use {@code withX} for
 * single-field updates. Every "mutation" returns a new instance — callers that
 * cached a prior reference must re-publish (e.g. {@code repo.updatePet(newPet)}).
 *
 * <p><b>Validation.</b> The compact constructor normalizes inputs:
 * {@code saturation} is clamped to {@code [1, 100]} (and coerced to {@code 100}
 * with a warning if {@code NaN}/{@code Infinite}); empty {@code info}/null
 * {@code skillInfo} become {@link CompoundBinaryTag#empty()}; {@code uuid} is
 * generated if absent; {@code petType} defaults to {@code Wolf}; {@code petName}
 * and {@code worldGroup} default to empty strings.
 */
public record PersistedPet(
        UUID uuid,
        MyPetPlayer owner,
        PetType petType,
        String petName,
        String worldGroup,
        double exp,
        double health,
        double saturation,
        int respawnTime,
        boolean wantsToRespawn,
        long lastUsed,
        Skilltree skilltree,
        CompoundBinaryTag skillInfo,
        CompoundBinaryTag info
) implements StoredPet {

    public PersistedPet {
        if (owner == null) throw new IllegalArgumentException("Owner must not be null.");
        if (uuid == null) uuid = UUID.randomUUID();
        if (petType == null) petType = PetType.byName("Wolf");
        if (petName == null) petName = "";
        if (worldGroup == null) worldGroup = "";
        if (Double.isNaN(saturation) || Double.isInfinite(saturation)) {
            MyPetApi.getLogger().log(Level.WARNING, "Saturation was set to an invalid number!", new Throwable());
            saturation = 100;
        } else {
            saturation = Math.max(1, Math.min(100, saturation));
        }
        if (skillInfo == null) skillInfo = CompoundBinaryTag.empty();
        if (info == null || info.keySet().isEmpty()) info = CompoundBinaryTag.empty();
    }

    // --- StoredPet bridge accessors (records expose components via x(), interface expects getX()) ---

    @Override public UUID getUUID() { return uuid; }
    @Override public MyPetPlayer getOwner() { return owner; }
    @Override public PetType getPetType() { return petType; }
    @Override public String getPetName() { return petName; }
    @Override public String getWorldGroup() { return worldGroup; }
    @Override public double getExp() { return exp; }
    @Override public double getHealth() { return health; }
    @Override public double getSaturation() { return saturation; }
    @Override public int getRespawnTime() { return respawnTime; }
    @Override public long getLastUsed() { return lastUsed; }
    @Override public Skilltree getSkilltree() { return skilltree; }

    /**
     * Single-field updaters — each returns a new {@code PersistedPet}
     * with the named field changed and all other fields copied verbatim.
     */

    public PersistedPet withUuid(UUID v) {
        return new PersistedPet(v, owner, petType, petName, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withOwner(MyPetPlayer v) {
        return new PersistedPet(uuid, v, petType, petName, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withPetType(PetType v) {
        return new PersistedPet(uuid, owner, v, petName, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withPetName(String v) {
        return new PersistedPet(uuid, owner, petType, v, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withWorldGroup(String v) {
        return new PersistedPet(uuid, owner, petType, petName, v, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withExp(double v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, v, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withHealth(double v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, v, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    /**
     * Returns {@code this} unchanged (with a logged warning) when {@code v} is
     * {@code NaN} or {@code Infinite}, preserving the prior value rather than
     * coercing — silently substituting a default would mask data corruption.
     */
    public PersistedPet withSaturation(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            MyPetApi.getLogger().log(Level.WARNING, "Saturation was set to an invalid number!", new Throwable());
            return this;
        }
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, v, respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withRespawnTime(int v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, saturation, v, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withWantsToRespawn(boolean v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, saturation, respawnTime, v, lastUsed, skilltree, skillInfo, info);
    }
    public PersistedPet withLastUsed(long v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, v, skilltree, skillInfo, info);
    }
    public PersistedPet withSkilltree(Skilltree v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, v, skillInfo, info);
    }
    public PersistedPet withSkillInfo(CompoundBinaryTag v) {
        return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, saturation, respawnTime, wantsToRespawn, lastUsed, skilltree, v, info);
    }

    /**
     * Merges per-skill NBT into the existing {@link #skillInfo}, preserving any
     * keys not produced by the active {@code skills} collection so removing
     * a skill from the live tree does not erase its persisted state.
     */
    public PersistedPet withSkills(Collection<Skill> skills) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder().put(skillInfo);
        for (Skill skill : skills) {
            if (skill instanceof NBTStorage storageSkill) {
                CompoundBinaryTag s = storageSkill.save();
                if (s != null) {
                    builder.put(skill.getName(), s);
                }
            }
        }
        return withSkillInfo(builder.build());
    }

    // --- Construction ---

    public static Builder builder(MyPetPlayer owner) {
        return new Builder(owner);
    }

    public Builder toBuilder() {
        return new Builder(owner)
                .uuid(uuid)
                .petType(petType)
                .petName(petName)
                .worldGroup(worldGroup)
                .exp(exp)
                .health(health)
                .saturation(saturation)
                .respawnTime(respawnTime)
                .wantsToRespawn(wantsToRespawn)
                .lastUsed(lastUsed)
                .skilltree(skilltree)
                .skillInfo(skillInfo)
                .info(info);
    }

    /**
     * Fluent builder. Exists because most construction sites (DB row → record,
     * MyPet → record, clone, listener-driven taming) populate 8–12 fields at
     * once; chained {@code with} calls would allocate a record per step.
     *
     * <p>{@link #petType} also seeds {@code health} from the type's
     * {@code startHP} when the pet is fresh (no respawn timer, no health set
     * yet), so callers don't have to remember to seed it themselves after
     * picking a type.
     */
    public static final class Builder {
        private UUID uuid;
        private final MyPetPlayer owner;
        private PetType petType = PetType.byName("Wolf");
        private String petName = "";
        private String worldGroup = "";
        private double exp = 0;
        private double health = -1;
        private double saturation = 100;
        private int respawnTime = 0;
        private boolean wantsToRespawn = false;
        private long lastUsed = -1;
        private Skilltree skilltree;
        private CompoundBinaryTag skillInfo = CompoundBinaryTag.empty();
        private CompoundBinaryTag info = CompoundBinaryTag.empty();

        Builder(MyPetPlayer owner) {
            if (owner == null) throw new IllegalArgumentException("Owner must not be null.");
            this.owner = owner;
        }

        public Builder uuid(UUID v) { this.uuid = v; return this; }
        public Builder petType(PetType v) {
            this.petType = v;
            if (this.respawnTime <= 0 && this.health == -1) {
                this.health = MyPetApi.getMyPetInfo().getStartHP(v);
            }
            return this;
        }
        public Builder petName(String v) { this.petName = v; return this; }
        public Builder worldGroup(String v) { if (v != null) this.worldGroup = v; return this; }
        public Builder exp(double v) { this.exp = v; return this; }
        public Builder health(double v) { this.health = v; return this; }
        public Builder saturation(double v) { this.saturation = v; return this; }
        public Builder respawnTime(int v) { this.respawnTime = v; return this; }
        public Builder wantsToRespawn(boolean v) { this.wantsToRespawn = v; return this; }
        public Builder lastUsed(long v) { this.lastUsed = v; return this; }
        public Builder skilltree(Skilltree v) { this.skilltree = v; return this; }
        public Builder skillInfo(CompoundBinaryTag v) { this.skillInfo = v != null ? v : CompoundBinaryTag.empty(); return this; }
        public Builder info(CompoundBinaryTag v) { this.info = v != null ? v : CompoundBinaryTag.empty(); return this; }

        public PersistedPet build() {
            return new PersistedPet(uuid, owner, petType, petName, worldGroup, exp, health, saturation,
                    respawnTime, wantsToRespawn, lastUsed, skilltree, skillInfo, info);
        }
    }

}
