/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.ai.movement.MyPetRandomStroll;
import de.Keyle.MyPet.api.event.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.*;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.DamageImpl;
import de.Keyle.MyPet.skill.skills.LifeImpl;
import de.Keyle.MyPet.skill.skills.RangedImpl;
import de.Keyle.MyPet.util.hooks.VaultHook;
import de.Keyle.MyPet.util.hooks.WorldGuardHook;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public abstract class MyPet implements de.Keyle.MyPet.api.entity.MyPet, NBTStorage {

    protected final MyPetPlayer petOwner;
    protected MyPetBukkitEntity bukkitEntity;
    @Getter
    protected String petName;
    protected double health;
    @Getter
    protected int respawnTime = 0;
    protected int hungerTime;
    protected double saturation = 100;
    protected UUID uuid = null;
    protected String worldGroup = "";
    protected CompoundBinaryTag storage = CompoundBinaryTag.empty();
    protected PetState status = PetState.Despawned;
    @Setter
    protected boolean wantsToRespawn = false;
    @Getter
    protected Skilltree skilltree = null;
    @Getter
    protected Skills skills;
    @Getter
    protected MyPetExperience experience;
    @Setter
    protected long lastUsed = -1;
    protected Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
    @Getter
    protected boolean isBaby = false;
    private MyPetType petType;

    protected MyPet(MyPetPlayer petOwner) {
        if (petOwner == null) {
            throw new IllegalArgumentException("Owner must not be null.");
        }
        this.petOwner = petOwner;
        skills = new Skills(this);
        experience = new MyPetExperience(this);
        hungerTime = Configuration.HungerSystem.HUNGER_SYSTEM_TIME;
        petName = Translation.getString("Name." + getPetType().name(), petOwner);
    }

    public static float[] getEntitySize(Class<? extends MyPetMinecraftEntity> entityMyPetClass) {
        EntitySize es = entityMyPetClass.getAnnotation(EntitySize.class);
        if (es != null) {
            return new float[]{es.height(), es.width()};
        }
        return new float[]{0, 0};
    }

    @Override
    public CompoundBinaryTag getInfo() {
        CompoundBinaryTag tag = writeExtendedInfo();

        // TODO replace with proper storage
        storage = storage.putInt("level", getExperience().getLevel());
        tag = tag.put("storage", storage);

        return tag;
    }

    @Override
    public void setInfo(CompoundBinaryTag info) {
        readExtendedInfo(info);

        // TODO replace with proper storage
        if (info.keySet().contains("storage")) {
            CompoundBinaryTag loadedStorage = info.getCompound("storage");
            // Merge loaded storage into our storage
            CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
            for (String key : this.storage.keySet()) {
                builder.put(key, this.storage.get(key));
            }
            for (String key : loadedStorage.keySet()) {
                builder.put(key, loadedStorage.get(key));
            }
            this.storage = builder.build();
        }
    }

    public java.util.Optional<MyPetBukkitEntity> getEntity() {
        if (getStatus() == PetState.Here) {
            return java.util.Optional.of(bukkitEntity);
        }
        return java.util.Optional.empty();
    }

    public double getYSpawnOffset() {
        return 0;
    }

    public java.util.Optional<Location> getLocation() {
        if (status == PetState.Here) {
            return java.util.Optional.of(bukkitEntity.getLocation());
        } else if (petOwner.isOnline()) {
            return java.util.Optional.of(petOwner.getPlayer().getLocation());
        } else {
            return java.util.Optional.empty();
        }
    }

    public void setLocation(Location loc) {
        if (status == PetState.Here && MyPetApi.getPlatformHelper().canSpawn(loc, this.bukkitEntity.getHandle())) {
            bukkitEntity.teleport(loc);
        }
    }

    public double getDamage() {
        return getSkills().has(DamageImpl.class) ? getSkills().get(DamageImpl.class).getDamage().getValue().doubleValue() : 0;
    }

    public double getRangedDamage() {
        return getSkills().has(RangedImpl.class) ? getSkills().get(RangedImpl.class).getDamage().getValue().doubleValue() : 0;
    }

    public boolean isPassiv() {
        return getDamage() == 0 && getRangedDamage() == 0;
    }

    public boolean hasTarget() {
        return this.getStatus() == PetState.Here && bukkitEntity.getHandle().hasTarget();
    }

    public double getExp() {
        return getExperience().getExp();
    }

    @Override
    public void setExp(double exp) {
        getExperience().setExp(exp);
    }

    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

        List<BinaryTag> itemList = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (getEquipment(slot) != null) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(getEquipment(slot));
                item = item.putString("Slot", slot.name());
                itemList.add(item);
            }
        }
        if (!itemList.isEmpty()) {
            builder.put("Equipment", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, itemList));
        }
        if (this instanceof MyPetBaby) {
            builder.putBoolean("Baby", isBaby());
        }
        return builder.build();
    }

    public void readExtendedInfo(CompoundBinaryTag info) {
        if (info.keySet().contains("Equipment")) {
            ListBinaryTag equipmentList = info.getList("Equipment", BinaryTagTypes.COMPOUND);
            for (int i = 0; i < equipmentList.size(); i++) {
                CompoundBinaryTag itemTag = equipmentList.getCompound(i);
                String slotName = itemTag.getString("Slot");
                if (!slotName.isEmpty()) {
                    try {
                        ItemStack itemStack = MyPetApi.getPlatformHelper().compoundToItemStack(itemTag);
                        setEquipmentBySlotName(slotName, itemStack);
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("Could not load Equipment item from pet data!");
                    }
                }
            }
        }
        if (info.keySet().contains("Baby")) {
            setBaby(info.getBoolean("Baby"));
        }
    }

    /**
     * Sets equipment by slot name string. Subclasses can override this to handle
     * special slot names (like "BODY" for horses) that may not exist as EquipmentSlot
     * enums in all Minecraft versions.
     */
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        try {
            EquipmentSlot slot = EquipmentSlot.valueOf(slotName);
            setEquipment(slot, item);
        } catch (IllegalArgumentException e) {
            // Slot doesn't exist in this MC version - subclasses can override to handle
        }
    }

    public ItemStack[] getEquipment() {
        ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment[slot.ordinal()] = getEquipment(slot);
        }
        return equipment;
    }

    public ItemStack getEquipment(EquipmentSlot slot) {
        return equipment.get(slot);
    }

    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        ItemStack finalItem = null;
        if (item == null) {
            equipment.remove(slot);
        } else {
            finalItem = item.clone();
            finalItem.setAmount(1);
            equipment.put(slot, finalItem);
        }
        if (status == PetState.Here) {
            ItemStack itemToSet = finalItem;
            getEntity().ifPresent(entity -> entity.getEquipment().setItem(slot, itemToSet));
        }
    }

    public void dropEquipment() {
        if (getStatus() == PetState.Here) {
            Location dropLocation = getLocation().get();
            for (ItemStack itemStack : equipment.values()) {
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    dropLocation.getWorld().dropItem(dropLocation, itemStack);
                }
            }
        }
    }

    public void setBaby(boolean flag) {
        this.isBaby = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public double getMaxHealth() {
        return MyPetApi.getMyPetInfo().getStartHP(getPetType()) + (skills.isActive(LifeImpl.class) ? skills.get(LifeImpl.class).getLife().getValue().doubleValue() : 0);
    }

    public double getHealth() {
        double health;
        if (status == PetState.Here) {
            health = bukkitEntity.getHealth();
        } else {
            health = this.health;
        }
        if (health > getMaxHealth()) {
            this.setHealth(Double.MAX_VALUE);
            health = getMaxHealth();
        }
        return health;
    }

    public void setHealth(double health) {
        double maxHealth = getMaxHealth();
        health = Math.min(health, maxHealth);
        if (status == PetState.Here) {
            bukkitEntity.setHealth(health);
        } else {
            this.health = health;
        }
    }

    public double getSaturation() {
        if (Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
            return saturation;
        } else {
            return 100;
        }
    }

    public void setSaturation(double value) {
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            saturation = Math.max(1, Math.min(100, value));
            hungerTime = Configuration.HungerSystem.HUNGER_SYSTEM_TIME;
        } else {
            MyPetApi.getLogger().warning("Saturation was set to an invalid number!\n" + Util.stackTraceToString());
        }
    }

    public void decreaseSaturation(double value) {
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            saturation = Math.max(1, Math.min(100, saturation - value));
        } else {
            MyPetApi.getLogger().warning("Saturation was decreased by an invalid number!\n" + Util.stackTraceToString());
        }
    }

    public void setPetName(String newName) {
        if (!NameFilter.isClean(newName)) {
            newName = Translation.getString("Name." + getPetType().name(), getOwner().getLanguage());
        }
        if (!this.petName.equals(newName)) {
            MyPetNameEvent event = new MyPetNameEvent(this, newName);
            Bukkit.getPluginManager().callEvent(event);
            newName = event.getNewName();
        }
        this.petName = newName;
        if (status == PetState.Here) {
            if (Configuration.Name.Tag.SHOW) {
                getEntity().ifPresent(entity -> entity.getHandle().updateNameTag());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Util.SANITIZED_MINIMESSAGE.deserialize(getPetName());
    }

    public MyPetType getPetType() {
        if (petType == null) {
            for (MyPetType type : MyPetType.values()) {
                if (type.getMyPetClass().isAssignableFrom(this.getClass())) {
                    petType = type;
                    break;
                }
            }
        }
        return petType;
    }

    @Override
    public void setPetType(MyPetType petType) {
        throw new UnsupportedOperationException("You can't change the type for an active MyPet!");
    }

    public void setRespawnTime(int time) {
        respawnTime = Math.max(time, 0);

        if (respawnTime > 0) {
            updateStatus(PetState.Dead);
        }
    }

    public boolean autoAssignSkilltree() {
        if (skilltree == null && this.petOwner.isOnline()) {
            if (Configuration.Skilltree.RANDOM_SKILLTREE_ASSIGNMENT) {
                return setSkilltree(MyPetApi.getSkilltreeManager().getRandomSkilltree(this), MyPetSelectSkilltreeEvent.Source.Auto);
            } else if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT) {
                List<Skilltree> skilltrees = new ArrayList<>(MyPetApi.getSkilltreeManager().getOrderedSkilltrees());

                for (Skilltree skilltree : skilltrees) {
                    if (skilltree.getMobTypes().contains(getPetType()) && skilltree.checkRequirements(this)) {
                        return setSkilltree(skilltree, MyPetSelectSkilltreeEvent.Source.Auto);
                    }
                }
                return false;
            }
            getOwner().sendMessage(Translation.getFormattedComponent("Message.Skilltree.SelectionPrompt", getOwner(), getDisplayName()), 120000);
        }
        return true;
    }

    public CompoundBinaryTag getSkillInfo() {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
        Collection<Skill> skillList = this.getSkills().all();
        if (!skillList.isEmpty()) {
            for (Skill skill : skillList) {
                if (skill instanceof NBTStorage storageSkill) {
                    CompoundBinaryTag s = storageSkill.save();
                    if (s != null) {
                        builder.put(skill.getName(), s);
                    }
                }
            }
        }
        return builder.build();
    }

    @Override
    public void setSkills(CompoundBinaryTag skills) {
    }

    public PetState getStatus() {
        if (status == PetState.Here) {
            if (bukkitEntity == null || bukkitEntity.getHandle() == null) {
                updateStatus(PetState.Despawned);
            } else if (bukkitEntity.getHealth() <= 0 || bukkitEntity.isDead()) {
                updateStatus(PetState.Dead);
            }
        }
        return status;
    }

    public void setStatus(PetState status) {
        if (status == PetState.Here) {
            if (this.status == PetState.Dead) {
                respawnPet();
            } else if (this.status == PetState.Despawned) {
                createEntity();
            }
        } else if (status == PetState.Dead) {
            updateStatus(PetState.Dead);
        } else {
            if (this.status == PetState.Here) {
                removePet();
            }
        }
    }

    protected void updateStatus(PetState status) {
        if (this.status != status) {
            this.status = status;
            Bukkit.getPluginManager().callEvent(new MyPetStatusEvent(this, status));
        }
    }

    public UUID getUUID() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        return this.uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public long getLastUsed() {
        return lastUsed;
    }

    @Override
    public String getWorldGroup() {
        return this.worldGroup;
    }

    public void setWorldGroup(String worldGroup) {
        if (worldGroup == null) {
            return;
        }
        if (WorldGroup.getGroupByName(worldGroup) == null) {
            worldGroup = "default";
        }
        this.worldGroup = worldGroup;
        experience.setMaxLevel(Configuration.LevelSystem.Experience.LEVEL_CAP);
    }

    public SpawnFlags createEntity() {
        return createEntity(null);
    }

    public SpawnFlags createEntity(Location spawnLocation) {
        lastUsed = System.currentTimeMillis();
        if (status != PetState.Here && getOwner().isOnline()) {
            Player owner = getOwner().getPlayer();
            if (owner.isDead()) {
                updateStatus(PetState.Despawned);
                return SpawnFlags.OwnerDead;
            }
            if (owner.getGameMode().name().equals("SPECTATOR")) {
                return SpawnFlags.Spectator;
            }

            if (respawnTime <= 0) {
                Location loc = spawnLocation != null ? spawnLocation : petOwner.getPlayer().getLocation();

                if (!WorldGroup.getGroupByWorld(loc.getWorld().getName()).getName().equals(getWorldGroup())) {
                    return SpawnFlags.WrongWorldGroup;
                }

                int ownerX = owner.getLocation().getChunk().getX();
                int ownerZ = owner.getLocation().getChunk().getZ();
                if (!owner.getWorld().isChunkLoaded(ownerX, ownerZ)) {
                    return SpawnFlags.InvalidPosition;
                }

                if (owner.isFlying() && !(this instanceof MyPetFlyingEntity)) {
                    boolean groundFound = false;
                    for (int i = 10; i >= 0; i--) {
                        Block b = loc.getBlock();
                        if (b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                            groundFound = true;
                            break;
                        }
                        loc = loc.subtract(0, 1, 0);
                    }

                    if (!groundFound) {
                        return SpawnFlags.Flying;
                    }
                }

                MyPetCallEvent event = new MyPetCallEvent(this);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return SpawnFlags.NotAllowed;
                }

                if (!MyPetApi.getHookHelper().isPetAllowed(getOwner())) {
                    return SpawnFlags.NotAllowed;
                }

                MyPetMinecraftEntity minecraftEntity = MyPetApi.getEntityRegistry().createMinecraftEntity(this, loc.getWorld());

                if (minecraftEntity == null) {
                    updateStatus(PetState.Despawned);
                    return SpawnFlags.Canceled;
                }
                bukkitEntity = minecraftEntity.getBukkitEntity();

                bukkitEntity.setMetadata("MyPet", new FixedMetadataValue(MyPetApi.getPlugin(), true));

                Random r = new Random(petOwner.getUniqueId().toString().hashCode());
                final char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
                StringBuilder sb = new StringBuilder(10);

                for (int i = 0; i < 10; i++) {
                    sb.append(chars[r.nextInt(chars.length)]);
                }

                String random = sb.toString();

                Team t;
                if (owner.getScoreboard().getTeam("MyPet-" + random) != null) {
                    t = owner.getScoreboard().getTeam("MyPet-" + random);
                } else {
                    t = owner.getScoreboard().registerNewTeam("MyPet-" + random);
                    t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                }

                for (String entry : t.getEntries()) {
                    try {
                        t.removeEntry(entry);
                    } catch (IllegalStateException ignored) {
                    }
                }
                t.addEntry(minecraftEntity.getUniqueID().toString());

                if (getYSpawnOffset() > 0) {
                    loc = loc.add(0, getYSpawnOffset(), 0);
                }
                loc.setPitch(0);
                loc.setYaw(0);

                Location origin = loc.clone();
                boolean positionFound = false;

                // If an explicit spawn location was provided (e.g., from capturing), try to use it directly first
                if (spawnLocation != null) {
                    minecraftEntity.setLocation(loc);
                    if (MyPetApi.getPlatformHelper().canSpawn(loc, minecraftEntity)) {
                        positionFound = true;
                    }
                }

                // If the exact location doesn't work, search for a nearby valid position
                if (!positionFound) {
                    loc.subtract(1, 0, 1);
                    for (double x = 0; x <= 2; x += 0.5) {
                        for (double z = 0; z <= 2; z += 0.5) {
                            if (x != 1 && z != 1) {
                                minecraftEntity.setLocation(loc);
                                if (MyPetApi.getPlatformHelper().canSpawn(loc, minecraftEntity)) {
                                    Block b = loc.getBlock();
                                    if (b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                                        positionFound = true;
                                        break;
                                    }
                                }
                            }
                            loc.add(0, 0, 0.5);
                        }
                        if (positionFound) {
                            break;
                        }
                        loc.subtract(0, 0, 2);
                        loc.add(0.5, 0, 0);
                    }
                }

                if (!positionFound) {
                    minecraftEntity.setLocation(origin);
                    if (!MyPetApi.getPlatformHelper().canSpawn(origin, minecraftEntity)) {
                        updateStatus(PetState.Despawned);
                        return SpawnFlags.NoSpace;
                    }
                }

                // Sync equipment before spawning so it's included in the spawn packet
                if (this instanceof MyPetEquipment equipmentPet) {
                    EntityEquipment equipment = bukkitEntity.getEquipment();
                    for (org.bukkit.inventory.EquipmentSlot slot : EquipmentSlot.values()) {
                        equipment.setItem(slot, equipmentPet.getEquipment(slot));
                    }
                }

                WorldGuardHook wgHook = MyPetApi.getPluginHookManager().getHook(WorldGuardHook.class);
                if (wgHook != null) {
                    wgHook.fixMissingEntityType(loc.getWorld(), true);
                }
                if (MyPetApi.getEntityRegistry().spawnMinecraftEntity(minecraftEntity, loc.getWorld())) {
                    if (wgHook != null) {
                        wgHook.fixMissingEntityType(loc.getWorld(), false);
                    }

                    updateStatus(PetState.Here);

                    if (worldGroup == null || worldGroup.isEmpty()) {
                        setWorldGroup(WorldGroup.getGroupByWorld(loc.getWorld().getName()).getName());
                    }

                    autoAssignSkilltree();

                    wantsToRespawn = false;

                    return SpawnFlags.Success;
                }
                if (wgHook != null) {
                    wgHook.fixMissingEntityType(loc.getWorld(), false);
                }
                return SpawnFlags.Canceled;
            }
        }
        if (status == PetState.Dead) {
            return SpawnFlags.Dead;
        } else {
            return SpawnFlags.AlreadyHere;
        }
    }

    public void removePet() {
        if (status == PetState.Here) {
            health = bukkitEntity.getHealth();
            updateStatus(PetState.Despawned);
            bukkitEntity.removeEntity();
            bukkitEntity = null;

            getSkills().get(BackpackImpl.class).closeInventory();
        }
    }

    public void removePet(boolean wantToRespawn) {
        this.wantsToRespawn = wantToRespawn;
        removePet();
    }

    public void respawnPet() {
        if (status != PetState.Here && getOwner().isOnline()) {
            updateStatus(PetState.Despawned);
            respawnTime = 0;
            switch (createEntity()) {
                case Success:
                    getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn", petOwner, getDisplayName()));
                    break;
                case Canceled:
                    getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", petOwner, getDisplayName()));
                    break;
                case NoSpace:
                    getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", petOwner, getDisplayName()));
                    break;
                case Flying:
                    getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Flying", petOwner, getDisplayName()));
                    break;
            }
            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
                setHealth((int) Math.ceil(getMaxHealth() / 100. * (saturation + 1 - (saturation % 10))));
            } else {
                setHealth(getMaxHealth());
            }
        }
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    @Override
    public void setOwner(MyPetPlayer owner) {
        throw new UnsupportedOperationException("You can't change the owner for an active MyPet!");
    }

    public boolean wantsToRespawn() {
        return wantsToRespawn;
    }

    public void schedule() {
        if (status != PetState.Despawned && getOwner().isOnline()) {
            if (status == PetState.Dead) {
                if (!Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                    respawnTime--;
                }
                if (respawnTime <= 0) {
                    respawnPet();
                } else if (MyPetApi.getPluginHookManager().isHookActive(VaultHook.class) && getOwner().hasAutoRespawnEnabled() && respawnTime <= getOwner().getAutoRespawnMin() && Permissions.has(getOwner().getPlayer(), "MyPet.user.respawn")) {
                    double cost = respawnTime * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
                    VaultHook vaultHook = MyPetApi.getPluginHookManager().getHook(VaultHook.class);
                    if (vaultHook.canPay(getOwner().getPlayer(), cost)) {
                        vaultHook.pay(getOwner().getPlayer(), cost);
                        getOwner().sendMessage(Translation.getFormattedComponent("Message.Command.Respawn.Paid", petOwner.getLanguage(), getDisplayName(), cost + " " + vaultHook.currencyNameSingular()));
                        respawnTime = 0;
                    }
                }
            }
            if (status == PetState.Here) {
                for (Skill skill : skills.all()) {
                    if (skill instanceof Scheduler scheduler) {
                        scheduler.schedule();
                    }
                }

                if (bukkitEntity.getHandle().getPathfinder().hasGoal("RandomStroll")) {
                    ((MyPetRandomStroll) bukkitEntity.getHandle().getPathfinder().getGoal("RandomStroll")).schedule();
                } else if (bukkitEntity.getHandle().getPathfinder().hasGoal("RandomSwim")) {
                    ((MyPetRandomStroll) bukkitEntity.getHandle().getPathfinder().getGoal("RandomSwim")).schedule();
                } else if (bukkitEntity.getHandle().getPathfinder().hasGoal("RandomFly")) {
                    ((MyPetRandomStroll) bukkitEntity.getHandle().getPathfinder().getGoal("RandomFly")).schedule();
                }

                if (Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
                    if (saturation > 1 && --hungerTime <= 0) {
                        hungerTime = Configuration.HungerSystem.HUNGER_SYSTEM_TIME;
                        MyPetExhaustionEvent event = new MyPetExhaustionEvent(this);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                        trySelfFeeding();
                        if (!event.isCancelled()) {
                            saturation--;
                            if (saturation == 66) {
                                getOwner().sendMessage(Translation.getFormattedComponent("Message.Hunger.Rumbling", getOwner(), getDisplayName()));
                            } else if (saturation == 33) {
                                getOwner().sendMessage(Translation.getFormattedComponent("Message.Hunger.Hungry", getOwner(), getDisplayName()));
                            } else if (saturation == 1) {
                                getOwner().sendMessage(Translation.getFormattedComponent("Message.Hunger.Starving", getOwner(), getDisplayName()));
                            }
                        }
                    }
                    if (saturation == 1 && (getHealth() >= 2 || Configuration.HungerSystem.HUNGER_SYSTEM_CAN_KILL)
                            && this.bukkitEntity.getTicksLived() >= Configuration.HungerSystem.HUNGER_SYSTEM_TIME_BEFORE_DAMAGE * 20) {
                        getEntity().ifPresent(entity -> {
                            double leDamage = Configuration.HungerSystem.HUNGER_SYSTEM_FIXED +
                                    entity.getMyPet().getMaxHealth() * Configuration.HungerSystem.HUNGER_SYSTEM_FACTOR;
                            if (leDamage >= entity.getHealth() && !Configuration.HungerSystem.HUNGER_SYSTEM_CAN_KILL)
                                leDamage = entity.getHealth() - 1;
                            entity.damage(leDamage);
                        });
                    }
                }
            }
        }
    }

    @Override
    public void load(CompoundBinaryTag myPetNBT) {
    }

    @Override
    public CompoundBinaryTag save() {
        CompoundBinaryTag.Builder petNBT = CompoundBinaryTag.builder();

        petNBT.putString("UUID", getUUID().toString());
        petNBT.putString("Type", this.getPetType().name());
        petNBT.putDouble("Health", this.getHealth());
        petNBT.putInt("Respawntime", this.respawnTime);
        petNBT.putDouble("Hunger", this.saturation);
        petNBT.putString("Name", this.petName);
        petNBT.putString("WorldGroup", this.worldGroup);
        petNBT.putDouble("Exp", this.getExp());
        petNBT.putLong("LastUsed", this.lastUsed);
        petNBT.put("Info", writeExtendedInfo());
        petNBT.putString("Owner-UUID", this.petOwner.getUniqueId().toString());
        petNBT.putBoolean("Wants-To-Respawn", wantsToRespawn);
        if (this.skilltree != null) {
            petNBT.putString("Skilltree", skilltree.getName());
        }

        CompoundBinaryTag.Builder skillsBuilder = CompoundBinaryTag.builder();
        Collection<Skill> skillList = this.getSkills().all();
        if (!skillList.isEmpty()) {
            for (Skill skill : skillList) {
                if (skill instanceof NBTStorage storageSkill) {
                    CompoundBinaryTag s = storageSkill.save();
                    if (s != null) {
                        skillsBuilder.put(skill.getName(), s);
                    }
                }
            }
        }
        petNBT.put("Skills", skillsBuilder.build());

        return petNBT.build();
    }

    public boolean setSkilltree(Skilltree skilltree, MyPetSelectSkilltreeEvent.Source source) {
        if (skilltree == null || this.skilltree == skilltree) {
            return false;
        }
        if (skilltree.getRequiredLevel() > 1 && getExperience().getLevel() < skilltree.getRequiredLevel()) {
            return false;
        }
        this.skilltree = skilltree;
        getServer().getPluginManager().callEvent(new MyPetLevelEvent(this, experience.getLevel()));
        MyPetSelectSkilltreeEvent selectEvent = new MyPetSelectSkilltreeEvent(this, skilltree, source);
        Bukkit.getServer().getPluginManager().callEvent(selectEvent);
        return true;
    }

    public boolean setSkilltree(Skilltree skilltree) {
        if (skilltree == null || this.skilltree == skilltree) {
            return false;
        }
        if (skilltree.getRequiredLevel() > 1 && getExperience().getLevel() < skilltree.getRequiredLevel()) {
            return false;
        }
        this.skilltree = skilltree;
        getServer().getPluginManager().callEvent(new MyPetLevelEvent(this, experience.getLevel()));
        return true;
    }

    public void trySelfFeeding() {
        if (!getSkills().has(BackpackImpl.class))
            return;
        if (!Configuration.HungerSystem.FEED_FROM_INVENTORY)
            return;
        double foodSaturation = Configuration.HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED;
        if (!(foodSaturation + saturation <= 100))
            return;

        Inventory bukkitInventory = getSkills().get(BackpackImpl.class).getInventory().getBukkitInventory();
        if (bukkitInventory == null)
            return;
        //Check Inventory for food first, then get that food
        List<ConfigItem> foodList = MyPetApi.getMyPetInfo().getFood(getPetType());
        for (ConfigItem foodItem : foodList) {
            if (bukkitInventory.contains(foodItem.getItem().getType())) {
                ItemStack item = bukkitInventory.getItem(bukkitInventory.first(foodItem.getItem().getType()));

                MyPetFeedEvent feedEvent = new MyPetFeedEvent(this, item, foodSaturation, MyPetFeedEvent.Result.Self_Feed);
                Bukkit.getPluginManager().callEvent(feedEvent);
                if (!feedEvent.isCancelled()) {
                    foodSaturation = feedEvent.getSaturation();
                    setSaturation(getSaturation() + foodSaturation);
                    item.setAmount(item.getAmount() - 1);
                }
                return;
            }
        }
    }
}