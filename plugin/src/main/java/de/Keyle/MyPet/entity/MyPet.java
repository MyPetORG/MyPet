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
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.event.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.*;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.ai.navigation.PaperNavigation;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import de.Keyle.MyPet.api.util.Timer;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.entity.visual.CreakingActivationSuppressor;
import de.Keyle.MyPet.entity.visual.PetNoPushSuppressor;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.entity.visual.WitherAutonomousAttackSuppressor;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.entity.visual.PetVisualSyncer;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.DamageImpl;
import de.Keyle.MyPet.skill.skills.LifeImpl;
import de.Keyle.MyPet.skill.skills.RangedImpl;
import de.Keyle.MyPet.util.StackTraces;
import de.Keyle.MyPet.util.hooks.VaultHook;
import de.Keyle.MyPet.util.hooks.WorldGuardHook;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public abstract class MyPet implements de.Keyle.MyPet.api.entity.MyPet, NBTStorage {

    protected final MyPetPlayer petOwner;
    protected Mob bukkitEntity;
    protected boolean sitting = false;
    protected LivingEntity targetEntity;
    protected TargetPriority targetPriority = TargetPriority.None;
    @Getter
    protected String petName;
    protected double health;
    @Getter
    protected int respawnTime = 0;
    protected int hungerTime;
    protected double saturation = 100;
    protected UUID uuid = null;
    protected String worldGroup = "";
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

    /**
     * Most recent vanilla-NBT snapshot for this pet — captured at despawn
     * ({@link #removePet}) or supplied by repo-load ({@link #setInfo}).
     * Serves a dual purpose:
     *
     * <ul>
     *   <li>Save fallback: when {@link #getInfo} runs after the live entity
     *       has been detached, we replay this compound so the saved row
     *       still carries the most-recent state.</li>
     *   <li>Respawn input: {@link #consumePendingSnapshot} hands the
     *       compound to {@code VanillaMobSpawner} so the new mob
     *       deserializes from vanilla NBT, preserving
     *       variant/color/equipment/etc. across death-respawn,
     *       sendaway-recall, and store-switchback cycles. Single-use and
     *       cleared on consumption.</li>
     * </ul>
     */
    private CompoundBinaryTag pendingSnapshot;
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

    protected AbstractNavigation petNavigation;

    @Override
    public Mob getBukkitEntity() {
        return bukkitEntity;
    }

    @Override
    public void setBukkitEntity(Mob mob) {
        this.bukkitEntity = mob;
        if (mob != null) {
            double walkSpeed = MyPetApi.getMyPetInfo().getSpeed(getPetType());
            this.petNavigation = new PaperNavigation(mob, walkSpeed);
        } else {
            this.petNavigation = null;
        }
    }

    @Override
    public AbstractNavigation getPetNavigation() {
        return petNavigation;
    }

    @Override
    public boolean isSitting() {
        return sitting;
    }

    @Override
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
        if (bukkitEntity instanceof Sittable s) {
            s.setSitting(sitting);
        }
    }

    @Override
    public boolean canMove() {
        return !sitting;
    }

    @Override
    public LivingEntity getMyPetTarget() {
        return targetEntity;
    }

    @Override
    public void setTarget(LivingEntity target) {
        this.targetEntity = target;
    }

    @Override
    public void setTarget(LivingEntity target, TargetPriority priority) {
        this.targetEntity = target;
        this.targetPriority = priority;
    }

    @Override
    public void forgetTarget() {
        this.targetEntity = null;
        this.targetPriority = TargetPriority.None;
    }

    @Override
    public TargetPriority getTargetPriority() {
        return targetPriority;
    }

    @Override
    public boolean hasTarget() {
        return targetEntity != null && !targetEntity.isDead();
    }

    @Override
    public void removeEntity() {
        Timer.stopPetTicking(this);
        PetSitParticleController.stopForPet(this);
        PetPotionParticleController.stopForPet(this);
        RideSkillFlightController.stopForPet(this);
        CreakingActivationSuppressor.stopForPet(this);
        WitherAutonomousAttackSuppressor.stopForPet(this);
        PetNoPushSuppressor.stopForPet(this);
        if (bukkitEntity != null) {
            bukkitEntity.remove();
        }
        bukkitEntity = null;
    }

    @Override
    public void updateVisuals() {
        Mob mob = getBukkitEntity();
        if (mob != null) {
            PetVisualSyncer.sync(this, mob);
        }
    }

    @Override
    public void updateNameTag() {
        Mob mob = getBukkitEntity();
        if (mob == null) return;
        // Touching customName / setCustomNameVisible requires owning the pet's region on Folia.
        // If called from another region (e.g. a level-up event fired by an admin command), dispatch
        // to the pet's scheduler so the mutation runs on the correct thread.
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            mob.getScheduler().run(MyPetApi.getPlugin(), task -> applyNameTag(mob), null);
            return;
        }
        applyNameTag(mob);
    }

    private void applyNameTag(Mob mob) {
        if (!Configuration.Name.Tag.SHOW) {
            mob.setCustomNameVisible(false);
            return;
        }
        String prefix = resolveTagPlaceholders(Configuration.Name.Tag.PREFIX);
        String suffix = resolveTagPlaceholders(Configuration.Name.Tag.SUFFIX);
        String miniMessageString = prefix + petName + suffix;
        try {
            mob.customName(Util.SANITIZED_MINIMESSAGE.deserialize(miniMessageString));
        } catch (Throwable t) {
            mob.customName(net.kyori.adventure.text.Component.text(petName));
        }
        mob.setCustomNameVisible(true);
    }

    private String resolveTagPlaceholders(String template) {
        return template
                .replace("<level>", Integer.toString(getExperience().getLevel()))
                .replace("<owner>", getOwner().getName());
    }

    @Override
    public void showPotionParticles(Color color) {
        PetPotionParticleController.show(this, color);
    }

    @Override
    public void hidePotionParticles() {
        PetPotionParticleController.hide(this);
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, EquipmentSlot hand) {
        if (bukkitEntity == null || !player.equals(getOwner().getPlayer())) {
            return false;
        }

        // Empty hand: sneak-toggle sit
        if (item == null || item.getType().isAir()) {
            if (player.isSneaking()) {
                boolean willSit = !isSitting();
                MyPetSitEvent sitEvent = new MyPetSitEvent(this,
                        willSit ? MyPetSitEvent.Action.Stay : MyPetSitEvent.Action.Follow);
                Bukkit.getPluginManager().callEvent(sitEvent);
                if (sitEvent.isCancelled()) {
                    return true;
                }
                setSitting(willSit);
                String messageKey = willSit ? "Message.Sit.Stay" : "Message.Sit.Follow";
                player.sendMessage(Translation.getFormattedComponent(messageKey, getOwner(), getDisplayName()));
                final boolean finalWillSit = willSit;
                if (Bukkit.isOwnedByCurrentRegion(bukkitEntity)) {
                    bukkitEntity.getWorld().playSound(bukkitEntity.getLocation(),
                            finalWillSit ? Sound.ENTITY_WOLF_WHINE : Sound.ENTITY_WOLF_AMBIENT,
                            0.8f, 1.2f);
                } else {
                    bukkitEntity.getScheduler().run(MyPetApi.getPlugin(), task ->
                            bukkitEntity.getWorld().playSound(bukkitEntity.getLocation(),
                                    finalWillSit ? Sound.ENTITY_WOLF_WHINE : Sound.ENTITY_WOLF_AMBIENT,
                                    0.8f, 1.2f), null);
                }
                return true;
            }
            // Right-click command: owner-only, empty hand, not sneaking. Mirrors
            // the legacy EntityMyPet#mobInteract branch — runs a configured
            // command as the player after substituting per-pet placeholders.
            if (!Configuration.Misc.RIGHT_CLICK_COMMAND.isEmpty()) {
                String command = Configuration.Misc.RIGHT_CLICK_COMMAND
                        .replace("%pet_name%", getPetName())
                        .replace("%pet_owner%", getOwner().getName())
                        .replace("%pet_level%", Integer.toString(getExperience().getLevel()))
                        .replace("%pet_status%", getStatus().name())
                        .replace("%pet_type%", getPetType().name())
                        .replace("%pet_uuid%", getUUID().toString())
                        .replace("%pet_world_group%", getWorldGroup())
                        .replace("%pet_skilltree_name%",
                                getSkilltree() != null ? getSkilltree().getName() : "");
                return player.performCommand(command);
            }
            return false;
        }

        // Grow up: a baby pet right-clicked with its configured grow-up item
        // becomes an adult. Gated on MyPetBaby so the branch only fires for
        // types that actually have an Ageable Bukkit counterpart — matches the
        // ConfigurationLoader gate that writes the GrowUpItem row.
        if (this instanceof MyPetBaby baby && baby.isBaby()) {
            ConfigItem growUpItem = Configuration.MyPet.getGrowUpItem(getPetType());
            if (growUpItem != null && growUpItem.compare(item)) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                baby.setBaby(false);
                if (Bukkit.isOwnedByCurrentRegion(bukkitEntity)) {
                    bukkitEntity.getWorld().spawnParticle(
                            Particle.HAPPY_VILLAGER,
                            bukkitEntity.getLocation().add(0, bukkitEntity.getHeight() * 0.5, 0),
                            8, 0.3, 0.3, 0.3, 0.0);
                } else {
                    bukkitEntity.getScheduler().run(MyPetApi.getPlugin(), task ->
                            bukkitEntity.getWorld().spawnParticle(
                                    Particle.HAPPY_VILLAGER,
                                    bukkitEntity.getLocation().add(0, bukkitEntity.getHeight() * 0.5, 0),
                                    8, 0.3, 0.3, 0.3, 0.0), null);
                }
                return true;
            }
        }

        // Feed: check if the item matches any configured food
        java.util.List<ConfigItem> foods = MyPetApi.getMyPetInfo().getFood(getPetType());
        for (ConfigItem food : foods) {
            if (food.compare(item)) {
                double saturationPerFeed = Configuration.HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED;
                MyPetFeedEvent feedEvent = new MyPetFeedEvent(
                        this, item, saturationPerFeed, MyPetFeedEvent.Result.Eat);
                Bukkit.getPluginManager().callEvent(feedEvent);
                if (feedEvent.isCancelled()) {
                    return false;
                }
                setSaturation(Math.min(100, getSaturation() + feedEvent.getSaturation()));
                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                setHealth(Math.min(getMaxHealth(), getHealth() + 1));
                final ItemStack finalItem = item;
                if (Bukkit.isOwnedByCurrentRegion(bukkitEntity)) {
                    bukkitEntity.getWorld().spawnParticle(
                            Particle.ITEM,
                            bukkitEntity.getLocation().add(0, bukkitEntity.getHeight() * 0.5, 0),
                            6, 0.2, 0.2, 0.2, 0.05, finalItem);
                    bukkitEntity.getWorld().playSound(bukkitEntity.getLocation(),
                            Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                } else {
                    bukkitEntity.getScheduler().run(MyPetApi.getPlugin(), task -> {
                        bukkitEntity.getWorld().spawnParticle(
                                Particle.ITEM,
                                bukkitEntity.getLocation().add(0, bukkitEntity.getHeight() * 0.5, 0),
                                6, 0.2, 0.2, 0.2, 0.05, finalItem);
                        bukkitEntity.getWorld().playSound(bukkitEntity.getLocation(),
                                Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                    }, null);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundBinaryTag getInfo() {
        // Prefer a fresh capture from the live entity over the cached
        // pendingSnapshot — the live state may have advanced since the last
        // despawn (e.g. saved while the pet is alive after respawn).
        CompoundBinaryTag snapshot = null;
        final Mob entityRef = bukkitEntity;
        if (entityRef != null && Bukkit.isOwnedByCurrentRegion(entityRef)) {
            try {
                snapshot = PetEntitySnapshot.capture(entityRef);
            } catch (Throwable t) {
                MyPetApi.getLogger().warning("Failed to capture live snapshot "
                        + "for pet " + getUUID() + " — falling back to pending snapshot. "
                        + t.getMessage());
            }
        }
        if (snapshot == null) snapshot = pendingSnapshot;
        return snapshot != null ? snapshot : CompoundBinaryTag.empty();
    }

    @Override
    public void setInfo(CompoundBinaryTag info) {
        this.pendingSnapshot = (info != null && !info.keySet().isEmpty()) ? info : null;
    }

    @Override
    public CompoundBinaryTag consumePendingSnapshot() {
        CompoundBinaryTag s = pendingSnapshot;
        pendingSnapshot = null;
        return s;
    }

    // getEntity() is provided as a default method on the MyPet api interface (returns
    // Optional.ofNullable(getBukkitEntity())). No override needed here.

    public double getYSpawnOffset() {
        return 0;
    }

    public java.util.Optional<Location> getLocation() {
        // The bukkitEntity != null guard protects against the window in which
        // status is still PetState.Here but bukkitEntity has been detached
        // (e.g. VanillaMobSpawner#releaseToWild between the detach and the
        // subsequent removePet() status transition). Without the guard this
        // method NPEs for any caller that runs inside that window.
        if (status == PetState.Here && bukkitEntity != null) {
            return java.util.Optional.of(bukkitEntity.getLocation());
        } else if (petOwner.isOnline()) {
            return java.util.Optional.of(petOwner.getPlayer().getLocation());
        } else {
            return java.util.Optional.empty();
        }
    }

    @Override
    public void setLocation(Location loc) {
        if (status == PetState.Here && bukkitEntity != null
                && loc != null && loc.getWorld() != null && loc.getBlock().isPassable()) {
            bukkitEntity.teleportAsync(loc);
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

    public double getExp() {
        return getExperience().getExp();
    }

    @Override
    public void setExp(double exp) {
        getExperience().setExp(exp);
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
        if (item == null || item.getType().isAir()) {
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
            updateVisuals();
        }
    }

    public double getMaxHealth() {
        return MyPetApi.getMyPetInfo().getStartHP(getPetType()) + (skills.isActive(LifeImpl.class) ? skills.get(LifeImpl.class).getLife().getValue().doubleValue() : 0);
    }

    public double getHealth() {
        double health;
        // bukkitEntity != null guard — see getLocation() for the rationale.
        if (status == PetState.Here && bukkitEntity != null) {
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
            this.health = health;
            final double finalHealth = health;
            final double finalMaxHealth = Math.max(1.0, maxHealth);
            Runnable apply = () -> {
                AttributeInstance attr = bukkitEntity.getAttribute(PetAttributes.MAX_HEALTH);
                if (attr != null && attr.getBaseValue() != finalMaxHealth) {
                    // Cap must move before health: vanilla setHealth() throws when value > current attribute max,
                    // which would happen on Life-skill upgrade if we wrote health first.
                    attr.setBaseValue(finalMaxHealth);
                }
                bukkitEntity.setHealth(finalHealth);
            };
            if (Bukkit.isOwnedByCurrentRegion(bukkitEntity)) {
                apply.run();
            } else {
                bukkitEntity.getScheduler().run(MyPetApi.getPlugin(), task -> apply.run(), null);
            }
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
            MyPetApi.getLogger().warning("Saturation was set to an invalid number!\n" + StackTraces.currentThread());
        }
    }

    public void decreaseSaturation(double value) {
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            saturation = Math.max(1, Math.min(100, saturation - value));
        } else {
            MyPetApi.getLogger().warning("Saturation was decreased by an invalid number!\n" + StackTraces.currentThread());
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
                updateNameTag();
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
            if (bukkitEntity == null) {
                updateStatus(PetState.Despawned);
            } else if (Bukkit.isOwnedByCurrentRegion(bukkitEntity)) {
                // Only touch the live entity when we own its region (always true on Paper,
                // conditional on Folia). Otherwise fall through and return the cached status.
                if (bukkitEntity.getHealth() <= 0 || bukkitEntity.isDead()) {
                    updateStatus(PetState.Dead);
                }
            }
        }
        return status;
    }

    /**
     * Returns the last cached pet status without touching the Bukkit entity.
     * Safe to call from any thread on Folia (will not trip the region thread check).
     * Callers that need an up-to-date status must call {@link #getStatus()} from the pet's
     * owning region thread.
     */
    public PetState getCachedStatus() {
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

    public void updateStatus(PetState status) {
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

                if (getYSpawnOffset() > 0) {
                    loc = loc.add(0, getYSpawnOffset(), 0);
                }
                loc.setPitch(0);
                loc.setYaw(0);

                WorldGuardHook wgHook = MyPetApi.getPluginHookManager().getHook(WorldGuardHook.class);
                if (wgHook != null) {
                    wgHook.fixMissingEntityType(loc.getWorld(), true);
                }
                boolean spawned = new VanillaMobSpawner().spawn(this, loc);
                if (wgHook != null) {
                    wgHook.fixMissingEntityType(loc.getWorld(), false);
                }

                if (!spawned) {
                    updateStatus(PetState.Despawned);
                    return SpawnFlags.NoSpace;
                }

                // bukkitEntity is now set by VanillaMobSpawner via setBukkitEntity().
                bukkitEntity.setMetadata("MyPet", new FixedMetadataValue(MyPetApi.getPlugin(), true));

                updateStatus(PetState.Here);

                if (worldGroup == null || worldGroup.isEmpty()) {
                    setWorldGroup(WorldGroup.getGroupByWorld(loc.getWorld().getName()).getName());
                }

                autoAssignSkilltree();

                wantsToRespawn = false;

                return SpawnFlags.Success;
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
            // bukkitEntity may be null if ownership has already been handed
            // off (e.g. VanillaMobSpawner#releaseToWild during /petrelease).
            // In that case only the status transition and backpack-close
            // steps apply.
            if (bukkitEntity != null) {
                final Mob entityRef = bukkitEntity;
                final boolean ownedByCurrentRegion = Bukkit.isOwnedByCurrentRegion(entityRef);

                // Only read live entity state when we own its region. Cross-region reads would
                // trip Folia's thread check and log (even when the exception is caught). If we
                // can't read, fall back to the last cached health/state on this object.
                if (ownedByCurrentRegion) {
                    health = entityRef.getHealth();
                    try {
                        // Stash bytes into pendingSnapshot — covers both
                        // save-while-detached (consumed by getInfo()) and
                        // in-memory respawn (consumed by VanillaMobSpawner).
                        this.pendingSnapshot = PetEntitySnapshot.capture(entityRef);
                    } catch (Throwable t) {
                        MyPetApi.getLogger().warning("Failed to capture EntitySnapshot "
                                + "for pet " + getUUID() + " during removePet — pet "
                                + "will respawn with default state. " + t.getMessage());
                        this.pendingSnapshot = null;
                    }
                }
                // Drop the pet's entry from the damage tracker before clearing
                // bukkitEntity — otherwise the ConcurrentHashMap in
                // PetDamageTracker grows unboundedly as pets are despawned and
                // respawned with new UUIDs. Tracker uses only UUID so this is safe
                // from any thread.
                PetDamageTracker.cleanup(entityRef.getUniqueId());
                Timer.stopPetTicking(this);
                PetSitParticleController.stopForPet(this);
                PetPotionParticleController.stopForPet(this);
                RideSkillFlightController.stopForPet(this);
                CreakingActivationSuppressor.stopForPet(this);
                WitherAutonomousAttackSuppressor.stopForPet(this);
                PetNoPushSuppressor.stopForPet(this);
                bukkitEntity = null;

                if (ownedByCurrentRegion) {
                    try {
                        entityRef.remove();
                    } catch (NullPointerException foliaShutdown) {
                        // On Folia, the region tick scheduler has already stopped by the time
                        // onDisable runs, so the entity chunk system can't complete the removal
                        // callback (ServerLevel#getCurrentWorldData() is null). The entity will
                        // be saved with the world at shutdown step 7.
                    }
                } else {
                    // Cross-region: dispatch the remove() to the entity's owning scheduler so it
                    // runs on the correct region thread.
                    entityRef.getScheduler().run(MyPetApi.getPlugin(), task -> {
                        try {
                            entityRef.remove();
                        } catch (Throwable ignored) {
                            // Entity may have already been removed or region may have transitioned
                            // by the time this task runs — best-effort.
                        }
                    }, null);
                }
            }
            updateStatus(PetState.Despawned);

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

    @Override
    public void tickRespawnTimer() {
        if (status != PetState.Dead || !getOwner().isOnline()) {
            return;
        }
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

    public void schedule() {
        if (status != PetState.Despawned && getOwner().isOnline()) {
            if (status == PetState.Here) {
                for (Skill skill : skills.all()) {
                    if (skill instanceof Scheduler scheduler) {
                        scheduler.schedule();
                    }
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
                            && this.bukkitEntity != null
                            && this.bukkitEntity.getTicksLived() >= Configuration.HungerSystem.HUNGER_SYSTEM_TIME_BEFORE_DAMAGE * 20) {
                        Mob entity = this.bukkitEntity;
                        double leDamage = Configuration.HungerSystem.HUNGER_SYSTEM_FIXED +
                                getMaxHealth() * Configuration.HungerSystem.HUNGER_SYSTEM_FACTOR;
                        if (leDamage >= entity.getHealth() && !Configuration.HungerSystem.HUNGER_SYSTEM_CAN_KILL)
                            leDamage = entity.getHealth() - 1;
                        entity.damage(leDamage);
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
        petNBT.put("Info", getInfo());
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