/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_13_R2.entity;

import com.google.common.base.Preconditions;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.ai.AIGoalSelector;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.event.MyPetFeedEvent;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.event.MyPetSitEvent;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.compat.v1_13_R2.PlatformHelper;
import de.Keyle.MyPet.compat.v1_13_R2.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.compat.v1_13_R2.entity.ai.attack.RangedAttack;
import de.Keyle.MyPet.compat.v1_13_R2.entity.ai.movement.Float;
import de.Keyle.MyPet.compat.v1_13_R2.entity.ai.movement.*;
import de.Keyle.MyPet.compat.v1_13_R2.entity.ai.navigation.VanillaNavigation;
import de.Keyle.MyPet.compat.v1_13_R2.entity.ai.target.*;
import de.Keyle.MyPet.compat.v1_13_R2.entity.types.EntityMyHorse;
import de.Keyle.MyPet.compat.v1_13_R2.util.FieldCompat;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import de.Keyle.MyPet.skill.skills.RideImpl;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

public abstract class EntityMyPet extends EntityCreature implements IAnimal, MyPetMinecraftEntity {

    protected static final DataWatcherObject<Byte> POTION_PARTICLE_WATCHER = aw;

    protected AIGoalSelector petPathfinderSelector, petTargetSelector;
    protected EntityLiving target = null;
    protected TargetPriority targetPriority = TargetPriority.None;
    protected double walkSpeed = 0.3F;
    protected boolean hasRider = false;
    protected boolean isMyPet = false;
    protected boolean isFlying = false;
    protected boolean canFly = true;
    protected boolean isInvisible = false;
    protected MyPet myPet;
    protected int jumpDelay = 0;
    protected int idleSoundTimer = 0;
    protected int flyCheckCounter = 0;
    protected int sitCounter = 0;
    protected AbstractNavigation petNavigation;
    protected Sit sitPathfinder;
    protected float jumpPower = 0;
    protected int donatorParticleCounter = 0;
    protected float limitCounter = 0;
    protected DamageSource deathReason = null;

    private static Field jump = ReflectionUtil.getField(EntityLiving.class, "bg");

    public EntityMyPet(EntityTypes<?> entitytypes, World world, MyPet myPet) {
        super(entitytypes, world);

        try {
            setSize();

            this.myPet = myPet;
            this.isMyPet = true;
            this.petPathfinderSelector = new AIGoalSelector(0);
            this.petTargetSelector = new AIGoalSelector(Configuration.Entity.SKIP_TARGET_AI_TICKS);
            this.walkSpeed = MyPetApi.getMyPetInfo().getSpeed(myPet.getPetType());
            this.petNavigation = new VanillaNavigation(this);
            this.sitPathfinder = new Sit(this);
            this.getAttributeInstance(GenericAttributes.maxHealth).setValue(Integer.MAX_VALUE);
            this.setHealth((float) myPet.getHealth());
            this.updateNameTag();
            this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(walkSpeed);
            this.setPathfinder();
            this.updateVisuals();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isMyPet() {
        return isMyPet;
    }

    public void setPathfinder() {
        petPathfinderSelector.addGoal("Float", new Float(this));
        petPathfinderSelector.addGoal("Sit", sitPathfinder);
        petPathfinderSelector.addGoal("Sprint", new Sprint(this, 0.25F));
        petPathfinderSelector.addGoal("RangedTarget", new RangedAttack(this, -0.1F, 12.0F));
        petPathfinderSelector.addGoal("MeleeAttack", new MeleeAttack(this, 0.1F, this.width + 1.3, 20));
        petPathfinderSelector.addGoal("Control", new Control(this, 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new FollowOwner(this, Configuration.Entity.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F));
        petPathfinderSelector.addGoal("LookAtPlayer", new LookAtPlayer(this, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new RandomLookaround(this));
        petTargetSelector.addGoal("OwnerHurtByTarget", new OwnerHurtByTarget(this));
        petTargetSelector.addGoal("HurtByTarget", new HurtByTarget(this));
        petTargetSelector.addGoal("ControlTarget", new ControlTarget(this, 1));
        petTargetSelector.addGoal("AggressiveTarget", new BehaviorAggressiveTarget(this, 15));
        petTargetSelector.addGoal("FarmTarget", new BehaviorFarmTarget(this, 15));
        petTargetSelector.addGoal("DuelTarget", new BehaviorDuelTarget(this, 5));
    }

    @Override
    public AbstractNavigation getPetNavigation() {
        return petNavigation;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public void setSize() {
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null) {
            float width = es.width();
            float height = es.height() == java.lang.Float.NaN ? width : es.height();
            this.setSize(width, height);
        }
    }

    public float getHeadHeight() {
        float height = super.getHeadHeight();
        if (hasRider()) {
            height += 1;
        }
        return height;
    }

    public boolean hasRider() {
        return isVehicle();
    }

    public void setLocation(Location loc) {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    public AIGoalSelector getPathfinder() {
        return petPathfinderSelector;
    }

    public AIGoalSelector getTargetSelector() {
        return petTargetSelector;
    }

    public boolean hasTarget() {
        if (target != null) {
            if (target.isAlive()) {
                return true;
            }
            target = null;
        }
        return false;
    }

    public TargetPriority getTargetPriority() {
        return targetPriority;
    }

    public LivingEntity getTarget() {
        if (target != null) {
            if (target.isAlive()) {
                return (LivingEntity) target.getBukkitEntity();
            }
            target = null;
        }
        return null;
    }

    public void setTarget(LivingEntity entity, TargetPriority priority) {
        if (entity == null || entity.isDead() || entity instanceof ArmorStand || !(entity instanceof CraftLivingEntity)) {
            forgetTarget();
            return;
        }
        if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), entity)) {
            forgetTarget();
            return;
        }
        if (priority.getPriority() > getTargetPriority().getPriority()) {
            target = ((CraftLivingEntity) entity).getHandle();
        }
    }

    public void forgetTarget() {
        target = null;
        targetPriority = TargetPriority.None;
    }

    @Override
    public void setCustomName(IChatBaseComponent ignored) {
        updateNameTag();
    }

    public void updateNameTag() {
        try {
            if (getCustomNameVisible()) {
                String prefix = Configuration.Name.Tag.PREFIX;
                String suffix = Configuration.Name.Tag.SUFFIX;
                prefix = prefix.replace("<owner>", getOwner().getName());
                prefix = prefix.replace("<level>", "" + getMyPet().getExperience().getLevel());
                suffix = suffix.replace("<owner>", getOwner().getName());
                suffix = suffix.replace("<level>", "" + getMyPet().getExperience().getLevel());
                this.setCustomNameVisible(getCustomNameVisible());
                String name = myPet.getPetName();
                if (!Permissions.has(getOwner(), "MyPet.command.name.color")) {
                    name = ChatColor.stripColor(name);
                }
                super.setCustomName(CraftChatMessage.fromStringOrNull(Util.cutString(prefix + name + suffix, 64)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IChatBaseComponent getCustomName() {
        try {
            return CraftChatMessage.fromStringOrNull(myPet.getPetName());
        } catch (Exception e) {
            return super.getCustomName();
        }
    }

    @Override
    public boolean getCustomNameVisible() {
        return Configuration.Name.Tag.SHOW;
    }

    @Override
    public void setCustomNameVisible(boolean ignored) {
        super.setCustomNameVisible(Configuration.Name.Tag.SHOW);
    }

    public boolean canMove() {
        return !sitPathfinder.isSitting();
    }

    public double getWalkSpeed() {
        return walkSpeed;
    }

    public boolean canEat(ItemStack itemstack) {
        List<ConfigItem> foodList = MyPetApi.getMyPetInfo().getFood(myPet.getPetType());
        for (ConfigItem foodItem : foodList) {
            if (foodItem.compare(itemstack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canEquip() {
        return Permissions.hasExtended(getOwner().getPlayer(), "MyPet.extended.equip") && canUseItem();
    }

    public boolean canUseItem() {
        MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Use);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public boolean playIdleSound() {
        if (getLivingSound() != null) {
            if (idleSoundTimer-- <= 0) {
                idleSoundTimer = 5;
                return true;
            }
        }
        return false;
    }

    @Override
    public void showPotionParticles(Color color) {
        getDataWatcher().set(POTION_PARTICLE_WATCHER, (byte) color.asRGB());
    }

    @Override
    public void hidePotionParticles() {
        int potionEffects = 0;
        if (!effects.isEmpty()) {
            potionEffects = PotionUtil.a(effects.values());
        }
        getDataWatcher().set(POTION_PARTICLE_WATCHER, (byte) potionEffects);
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean attack(Entity entity) {
        boolean damageEntity = false;
        try {
            double damage = isMyPet() ? myPet.getDamage() : 0;
            if (entity instanceof EntityPlayer) {
                Player victim = (Player) entity.getBukkitEntity();
                if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), victim, true)) {
                    if (myPet.hasTarget()) {
                        setGoalTarget(null);
                    }
                    return false;
                }
            }
            damageEntity = entity.damageEntity(DamageSource.mobAttack(this), (float) damage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return damageEntity;
    }

    @Override
    public void updateVisuals() {
    }

    public boolean toggleSitting() {
        MyPetSitEvent sitEvent = new MyPetSitEvent(getMyPet(), isSitting() ? MyPetSitEvent.Action.Follow : MyPetSitEvent.Action.Stay);
        Bukkit.getPluginManager().callEvent(sitEvent);
        if (!sitEvent.isCancelled()) {
            this.sitPathfinder.toggleSitting();
            if (isSitting()) {
                getOwner().sendMessage(Util.formatText(Translation.getString("Message.Sit.Stay", myPet.getOwner()), getMyPet().getPetName()));
            } else {
                getOwner().sendMessage(Util.formatText(Translation.getString("Message.Sit.Follow", myPet.getOwner()), getMyPet().getPetName()));
            }
            sitCounter = 0;
        }
        return !sitEvent.isCancelled();
    }

    @Override
    public void setSitting(boolean sitting) {
        if (isSitting() != sitting) {
            MyPetSitEvent sitEvent = new MyPetSitEvent(getMyPet(), sitting ? MyPetSitEvent.Action.Follow : MyPetSitEvent.Action.Stay);
            Bukkit.getPluginManager().callEvent(sitEvent);
            if (!sitEvent.isCancelled()) {
                this.sitPathfinder.toggleSitting();
                sitCounter = 0;
            }
        }
    }

    @Override
    public boolean isSitting() {
        return this.sitPathfinder.isSitting();
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public CraftMyPet getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPet(this.world.getServer(), this);
        }
        return (CraftMyPet) this.bukkitEntity;
    }

    // Obfuscated Method handler ------------------------------------------------------------------------------------

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        new BukkitRunnable() {
            public void run() {
                EntityPlayer player = ((EntityPlayer) entityhuman);
                if (player.getBukkitEntity().isOnline()) {
                    player.updateInventory(entityhuman.defaultContainer);
                }
            }
        }.runTaskLater(MyPetApi.getPlugin(), 5);
        if (itemStack != null && itemStack.getItem() == Items.LEAD) {
            ((EntityPlayer) entityhuman).playerConnection.sendPacket(new PacketPlayOutAttachEntity(this, null));
        }

        if (enumhand == EnumHand.OFF_HAND) {
            return true;
        }

        Player owner = this.getOwner().getPlayer();

        if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
            if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isActive(RideImpl.class) && canMove()) {
                    if (Permissions.hasExtended(owner, "MyPet.extended.ride")) {
                        ((CraftPlayer) owner).getHandle().startRiding(this);
                        return true;
                    } else {
                        getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner()), 2000);
                    }
                }
            }
            if (Configuration.Skilltree.Skill.CONTROL_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isActive(ControlImpl.class)) {
                    return true;
                }
            }
            if (itemStack != null) {
                if (itemStack.getItem() == Items.NAME_TAG && itemStack.hasName()) {
                    if (Permissions.has(getOwner(), "MyPet.command.name") && Permissions.hasExtended(getOwner(), "MyPet.extended.nametag")) {
                        final String name = itemStack.getName().getString();
                        getMyPet().setPetName(name);
                        EntityMyPet.super.setCustomName(CraftChatMessage.fromStringOrNull("-"));
                        myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", myPet.getOwner()), name));
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            itemStack.subtract(1);
                        }
                        if (itemStack.getCount() <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                        }
                        new BukkitRunnable() {
                            public void run() {
                                updateNameTag();
                            }
                        }.runTaskLater(MyPetApi.getPlugin(), 1L);
                        return true;
                    }
                }
                if (canEat(itemStack) && canUseItem()) {
                    if (owner != null && !Permissions.hasExtended(owner, "MyPet.extended.feed")) {
                        return false;
                    }
                    if (this.petTargetSelector.hasGoal("DuelTarget")) {
                        BehaviorDuelTarget duelTarget = (BehaviorDuelTarget) this.petTargetSelector.getGoal("DuelTarget");
                        if (duelTarget.getDuelOpponent() != null) {
                            return true;
                        }
                    }
                    boolean used = false;
                    double saturation = Configuration.HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED;
                    if (saturation > 0) {
                        if (myPet.getSaturation() < 100) {
                            MyPetFeedEvent feedEvent = new MyPetFeedEvent(getMyPet(), CraftItemStack.asCraftMirror(itemStack), saturation, MyPetFeedEvent.Result.Eat);
                            Bukkit.getPluginManager().callEvent(feedEvent);
                            if (!feedEvent.isCancelled()) {
                                saturation = feedEvent.getSaturation();
                                double missingSaturation = 100 - myPet.getSaturation();
                                myPet.setSaturation(myPet.getSaturation() + saturation);
                                saturation = Math.max(0, saturation - missingSaturation);
                                used = true;
                            }
                        }
                    }
                    if (saturation > 0) {
                        if (getHealth() < myPet.getMaxHealth()) {
                            MyPetFeedEvent feedEvent = new MyPetFeedEvent(getMyPet(), CraftItemStack.asCraftMirror(itemStack), saturation, MyPetFeedEvent.Result.Heal);
                            Bukkit.getPluginManager().callEvent(feedEvent);
                            if (!feedEvent.isCancelled()) {
                                saturation = feedEvent.getSaturation();
                                float missingHealth = (float) (myPet.getMaxHealth() - getHealth());
                                this.heal(Math.min((float) saturation, missingHealth), RegainReason.EATING);
                                used = true;
                            }
                        }
                    }

                    if (used) {
                        if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                            itemStack.subtract(1);
                            if (itemStack.getCount() <= 0) {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                            }
                        }
                        MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, getHeadHeight(), 0), ParticleCompat.HEART.get(), 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);

                        return true;
                    }
                }
            }
            if (!owner.isSneaking() && !Configuration.Misc.RIGHT_CLICK_COMMAND.isEmpty()) {
                String command = Configuration.Misc.RIGHT_CLICK_COMMAND;
                command = command.replaceAll("%pet_name%", myPet.getPetName());
                command = command.replaceAll("%pet_owner%", myPet.getOwner().getName());
                command = command.replaceAll("%pet_level%", "" + myPet.getExperience().getLevel());
                command = command.replaceAll("%pet_status%", "" + myPet.getStatus().name());
                command = command.replaceAll("%pet_type%", myPet.getPetType().name());
                command = command.replaceAll("%pet_uuid%", myPet.getUUID().toString());
                command = command.replaceAll("%pet_world_group%", myPet.getWorldGroup());
                command = command.replaceAll("%pet_skilltree_name%", myPet.getSkilltree() != null ? myPet.getSkilltree().getName() : "");
                return owner.performCommand(command);
            }
        } else {
            if (itemStack != null) {
                if (itemStack.getItem() == Items.NAME_TAG) {
                    if (itemStack.hasName()) {
                        EntityMyPet.super.setCustomName(CraftChatMessage.fromStringOrNull("-"));
                        new BukkitRunnable() {
                            public void run() {
                                updateNameTag();
                            }
                        }.runTaskLater(MyPetApi.getPlugin(), 1L);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public void onLivingUpdate() {
        if (hasRider) {
            if (!isVehicle()) {
                hasRider = false;
                this.Q = 0.5F; // climb height -> halfslab
                Location playerLoc = getOwner().getPlayer().getLocation();
                Location petLoc = getBukkitEntity().getLocation();
                petLoc.setYaw(playerLoc.getYaw());
                petLoc.setPitch(playerLoc.getPitch());
                getOwner().getPlayer().teleport(petLoc);
            }
        } else {
            if (isVehicle()) {
                for (Entity e : passengers) {
                    if (e instanceof EntityPlayer && getOwner().equals(e)) {
                        hasRider = true;
                        this.Q = 1.0F; // climb height -> 1 block
                        petTargetSelector.finish();
                        petPathfinderSelector.finish();
                    } else {
                        e.stopRiding(); // just the owner can ride a pet
                    }
                }
            }
        }
        if (sitPathfinder.isSitting() && sitCounter-- <= 0) {
            MyPetApi.getPlatformHelper().playParticleEffect(getOwner().getPlayer(), this.getBukkitEntity().getLocation().add(0, getHeadHeight() + 1, 0), ParticleCompat.BARRIER.get(), 0F, 0F, 0F, 5F, 1, 32);
            sitCounter = 60;
        }
        Player p = myPet.getOwner().getPlayer();
        if (p != null && p.isOnline() && !p.isDead()) {
            if (p.isSneaking() != isSneaking()) {
                this.setSneaking(!isSneaking());
            }
            if (Configuration.Misc.INVISIBLE_LIKE_OWNER) {
                if (!isInvisible && p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    isInvisible = true;
                    getBukkitEntity().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false));
                } else if (isInvisible && !p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    getBukkitEntity().removePotionEffect(PotionEffectType.INVISIBILITY);
                    isInvisible = false;
                }
            }
            if (!this.isInvisible() && getOwner().getDonationRank() != DonateCheck.DonationRank.None && donatorParticleCounter-- <= 0) {
                donatorParticleCounter = 20 + getRandom().nextInt(10);
                MyPetApi.getPlatformHelper().playParticleEffect(this.getBukkitEntity().getLocation().add(0, 1, 0), ParticleCompat.VILLAGER_HAPPY.get(), 0.4F, 0.4F, 0.4F, 0.4F, 5, 10);
            }
        }
    }

    public float getHealth() {
        double health = this.datawatcher.get(HEALTH);
        double maxHealth = myPet.getMaxHealth();
        if (health > maxHealth) {
            setHealth((float) maxHealth);
            health = this.datawatcher.get(HEALTH);
        }
        return (float) health;
    }

    public void setHealth(float f) {
        double deltaHealth = this.datawatcher.get(HEALTH);
        double maxHealth = myPet.getMaxHealth();

        boolean silent = this.getAttributeInstance(GenericAttributes.maxHealth).getValue() != maxHealth;
        this.getAttributeInstance(GenericAttributes.maxHealth).setValue(maxHealth);

        this.datawatcher.set(HEALTH, MathHelper.a(f, 0.0F, (float) maxHealth));

        double health = this.datawatcher.get(HEALTH);
        if (deltaHealth > maxHealth) {
            deltaHealth = 0;
        } else {
            deltaHealth = health - deltaHealth;
        }

        if (!silent && !Configuration.Misc.DISABLE_ALL_ACTIONBAR_MESSAGES) {
            String msg = myPet.getPetName() + ChatColor.RESET + ": ";
            if (health > maxHealth / 3 * 2) {
                msg += ChatColor.GREEN;
            } else if (health > maxHealth / 3) {
                msg += ChatColor.YELLOW;
            } else {
                msg += ChatColor.RED;
            }
            if (health > 0) {
                msg += String.format("%1.2f", health) + ChatColor.WHITE + "/" + String.format("%1.2f", maxHealth);

                if (!myPet.getOwner().isHealthBarActive()) {
                    if (deltaHealth > 0) {
                        msg += " (" + ChatColor.GREEN + "+" + String.format("%1.2f", deltaHealth) + ChatColor.RESET + ")";
                    } else if (deltaHealth < 0) {
                        msg += " (" + ChatColor.RED + String.format("%1.2f", deltaHealth) + ChatColor.RESET + ")";
                    }
                }
            } else {
                msg += Translation.getString("Name.Dead", getOwner());
            }

            MyPetApi.getPlatformHelper().sendMessageActionBar(getOwner().getPlayer(), msg);
        }
    }

    public void die(DamageSource damagesource) {
        this.deathReason = damagesource;
        super.die(damagesource);
    }

    protected void initDatawatcher() {
    }

    /**
     * Returns the speed of played sounds
     * The faster the higher the sound will be
     */
    public float getSoundSpeed() {
        float pitchAddition = 0;
        if (getMyPet() instanceof MyPetBaby) {
            if (((MyPetBaby) getMyPet()).isBaby()) {
                pitchAddition += 0.5F;
            }
        }
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1 + pitchAddition;
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected abstract String getLivingSound();

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected abstract String getHurtSound();

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected abstract String getDeathSound();

    public void playPetStepSound() {
    }

    public void playStepSound(BlockPosition blockposition, Block block) {
        playPetStepSound();
    }

    private void makeLivingSound() {
        if (getLivingSound() != null) {
            SoundEffect se = IRegistry.SOUND_EVENT.get(new MinecraftKey(getLivingSound()));
            if (se != null) {
                for (int j = 0; j < this.world.players.size(); ++j) {
                    EntityPlayer entityplayer = (EntityPlayer) this.world.players.get(j);

                    float volume = 1f;
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(entityplayer.getBukkitEntity())) {
                        volume = MyPetApi.getPlayerManager().getMyPetPlayer(entityplayer.getBukkitEntity()).getPetLivingSoundVolume();
                    }

                    double deltaX = locX - entityplayer.locX;
                    double deltaY = locY - entityplayer.locY;
                    double deltaZ = locZ - entityplayer.locZ;
                    double maxDistance = volume > 1.0F ? (double) (16.0F * volume) : 16.0D;
                    if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ < maxDistance * maxDistance) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutNamedSoundEffect(se, SoundCategory.HOSTILE, locX, locY, locZ, volume, getSoundSpeed()));
                    }
                }
            } else {
                MyPetApi.getLogger().warning("Sound \"" + getLivingSound() + "\" not found. Please report this to the developer.");
            }
        }
    }


    /**
     * returns the first passenger
     */
    public Entity getFirstPassenger() {
        return this.bP().isEmpty() ? null : this.bP().get(0);
    }

    private void ride(float motionSideways, float f, float motionForward, float speedModifier) {
        double locY;
        float f2;
        float speed;
        float swimmSpeed;

        if (this.isInWater()) {
            locY = this.locY;
            speed = 0.8F;
            swimmSpeed = 0.02F;

            this.a(motionSideways, motionForward, f, swimmSpeed);
            this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
            this.motX *= (double) speed;
            this.motY *= 0.800000011920929D;
            this.motZ *= (double) speed;
            this.motY -= 0.02D;
            if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + locY, this.motZ)) {
                this.motY = 0.30000001192092896D;
            }
        } else if (this.ax()) { // in lava
            locY = this.locY;
            this.a(motionSideways, motionForward, f, 0.02F);
            this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
            this.motX *= 0.5D;
            this.motY *= 0.5D;
            this.motZ *= 0.5D;
            this.motY -= 0.02D;
            if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + locY, this.motZ)) {
                this.motY = 0.30000001192092896D;
            }
        } else {
            double minY;
            if (FieldCompat.AxisAlignedBB_Fields.get()) {
                minY = (double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_minY.get(), this.getBoundingBox());
            } else {
                minY = this.getBoundingBox().minY;
            }

            float friction = 0.91F;
            if (this.onGround) {
                friction = this.world.getType(new BlockPosition(MathHelper.floor(this.locX), MathHelper.floor(minY) - 1, MathHelper.floor(this.locZ))).getBlock().n() * 0.91F;
            }

            speed = speedModifier * (0.16277136F / (friction * friction * friction));

            this.a(motionSideways, motionForward, f, speed);
            friction = 0.91F;
            if (this.onGround) {
                friction = this.world.getType(new BlockPosition(MathHelper.floor(this.locX), MathHelper.floor(minY) - 1, MathHelper.floor(this.locZ))).getBlock().n() * 0.91F;
            }

            if (this.z_()) {
                swimmSpeed = 0.15F;
                this.motX = MathHelper.a(this.motX, (double) (-swimmSpeed), (double) swimmSpeed);
                this.motZ = MathHelper.a(this.motZ, (double) (-swimmSpeed), (double) swimmSpeed);
                this.fallDistance = 0.0F;
                if (this.motY < -0.15D) {
                    this.motY = -0.15D;
                }
            }

            this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
            if (this.positionChanged && this.z_()) {
                this.motY = 0.2D;
            }

            this.motY -= 0.08D;

            this.motY *= 0.9800000190734863D;
            this.motX *= (double) friction;
            this.motZ *= (double) friction;
        }

        this.aI = this.aJ;
        locY = this.locX - this.lastX;
        double d1 = this.locZ - this.lastZ;
        f2 = MathHelper.sqrt(locY * locY + d1 * d1) * 4.0F;
        if (f2 > 1.0F) {
            f2 = 1.0F;
        }

        this.aJ += (f2 - this.aJ) * 0.4F;
        this.aK += this.aJ;
    }

    public void makeSound(String sound, float volume, float pitch) {
        if (sound != null) {
            SoundEffect se = IRegistry.SOUND_EVENT.get(new MinecraftKey(sound));
            if (se != null) {
                this.a(se, volume, pitch);
            } else {
                MyPetApi.getLogger().warning("Sound \"" + sound + "\" not found. Please report this to the developer.");
            }
        }
    }

    /**
     * do NOT drop anything
     */
    protected boolean isDropExperience() {
        return false;
    }

    /**
     * do NOT drop anything
     */
    protected void dropDeathLoot(boolean flag, int i) {
    }

    /**
     * do NOT drop anything
     */
    protected void dropEquipment(boolean flag, int i) {
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * -> initDatawatcher()
     */
    protected void x_() {
        super.x_();
        try {
            initDatawatcher();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows handlePlayerInteraction() to
     * be fired when a lead is used
     */
    public boolean a(EntityHuman entityhuman) {
        return false;
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     * -> handlePlayerInteraction(EntityHuman)
     */
    public boolean a(EntityHuman entityhuman, EnumHand enumhand) {
        try {
            ItemStack itemstack = entityhuman.b(enumhand);
            boolean result = handlePlayerInteraction(entityhuman, enumhand, itemstack);
            if (!result && getMyPet().getOwner().equals(entityhuman) && entityhuman.isSneaking()) {
                result = toggleSitting();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * -> playStepSound()
     */
    protected void a(BlockPosition blockposition, Block block) {
        try {
            playStepSound(blockposition, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     * -> getHurtSound()
     */
    protected SoundEffect d(DamageSource damagesource) {
        try {
            return IRegistry.SOUND_EVENT.get(new MinecraftKey(getHurtSound()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the sound that is played when the MyPet dies
     * -> getDeathSound()
     */
    protected SoundEffect cs() {
        try {
            return IRegistry.SOUND_EVENT.get(new MinecraftKey(getDeathSound()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the speed of played sounds
     */
    protected float cE() {
        try {
            return getSoundSpeed();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.cE();
    }

    /*
     *  old movementTick method
     */
    public void k() {
        movementTick();
    }

    public void movementTick() {
        if (this.jumpDelay > 0) {
            --this.jumpDelay;
        }

        if (this.bl > 0 && !this.bT()) {
            double d0 = this.locX + (this.bj - this.locX) / (double) this.bl;
            double d1 = this.locY + (this.bk - this.locY) / (double) this.bl;
            double d2 = this.locZ + (this.bl - this.locZ) / (double) this.bl;
            double d3 = MathHelper.g(this.bm - (double) this.yaw);
            this.yaw = (float) ((double) this.yaw + d3 / (double) this.bl);
            this.pitch = (float) ((double) this.pitch + (this.bn - (double) this.pitch) / (double) this.bl);
            --this.bl;
            this.setPosition(d0, d1, d2);
            this.setYawPitch(this.yaw, this.pitch);
        } else if (!this.cP()) {
            this.motX *= 0.98D;
            this.motY *= 0.98D;
            this.motZ *= 0.98D;
        }

        if (Math.abs(this.motX) < 0.003D) {
            this.motX = 0.0D;
        }

        if (Math.abs(this.motY) < 0.003D) {
            this.motY = 0.0D;
        }

        if (Math.abs(this.motZ) < 0.003D) {
            this.motZ = 0.0D;
        }

        this.doMyPetTick();

        if (this.bg) {
            if (this.W > 0.0D && (!this.onGround || this.W > 0.4D)) {
                this.c(TagsFluid.WATER);
            } else if (this.ax()) {
                this.c(TagsFluid.LAVA);
            } else if (this.onGround && this.jumpDelay == 0) {
                this.cH();
                this.jumpDelay = 10;
            }
        } else {
            this.jumpDelay = 0;
        }

        this.bh *= 0.98F;
        this.bj *= 0.98F;
        this.bk *= 0.9F;
        this.n();
        this.a(this.bh, this.bi, this.bj);
        this.cN();
    }

    /**
     * -> addPassenger(Entity)
     */
    protected boolean o(Entity entity) {
        return addPassenger(entity);
    }

    protected boolean addPassenger(Entity entity) {
        boolean returnVal = false;
        // don't allow anything but the owner to ride this entity
        Ride rideSkill = myPet.getSkills().get(RideImpl.class);
        if (rideSkill != null && entity instanceof EntityPlayer && getOwner().equals(entity)) {
            if (entity.getVehicle() != this) {
                throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
            } else {
                Preconditions.checkState(!entity.passengers.contains(this), "Circular entity riding! %s %s", this, entity);
                boolean cancelled = false;
                if (MyPetApi.getPlatformHelper().isSpigot()) {
                    cancelled = MountEventWrapper.callEvent(entity.getBukkitEntity(), this.getBukkitEntity());
                }
                if (cancelled) {
                    returnVal = false;
                } else {
                    if (!(this.bO() instanceof EntityHuman)) {
                        this.passengers.add(0, entity);
                    } else {
                        this.passengers.add(entity);
                    }
                    returnVal = true;
                }
            }

            if (this instanceof IJumpable) {
                double factor = 1;
                if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
                    factor = Math.log10(myPet.getSaturation()) / 2;
                }
                getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F))) * factor);
            }
        }
        return returnVal;
    }

    private static class MountEventWrapper {

        public static boolean callEvent(final CraftEntity player, final CraftMyPet pet) {
            Event event = new org.spigotmc.event.entity.EntityMountEvent(player, pet);
            Bukkit.getPluginManager().callEvent(event);
            return ((Cancellable) event).isCancelled();
        }
    }

    /**
     * -> unmount(Entity)
     */
    protected boolean removePassenger(Entity entity) {
        boolean result = super.removePassenger(entity);
        PlatformHelper platformHelper = (PlatformHelper) MyPetApi.getPlatformHelper();
        AxisAlignedBB bb = entity.getBoundingBox();
        bb = getBBAtPosition(bb, this.locX, this.locY, this.locZ);
        if (!platformHelper.canSpawn(getBukkitEntity().getLocation(), bb)) {
            entity.a(this, true);
        } else {
            entity.setPosition(locX, locY, locZ);
        }
        return result;
    }

    protected AxisAlignedBB getBBAtPosition(AxisAlignedBB bb, double x, double y, double z) {
        double width = (bb.maxX - bb.minX) / 2;
        double height = bb.maxY - bb.minY;
        double depth = (bb.maxZ - bb.minZ) / 2;
        return new AxisAlignedBB(x - width, y, z - depth, x + width, y + height, z + width);
    }

    /**
     * Entity AI tick method
     * -> updateAITasks()
     */
    protected void doMyPetTick() {
        try {
            ++this.ticksFarFromPlayer;

            if (isAlive()) {
                getEntitySenses().a(); // sensing

                Player p = getOwner().getPlayer();
                if (p == null || !p.isOnline()) {

                    MyPetApi.getLogger().warning("==========================================");
                    MyPetApi.getLogger().warning("Please report this to the MyPet developer!");

                    MyPetApi.getLogger().warning("MyPet: " + getMyPet());
                    MyPetApi.getLogger().warning("MyPetOwner: " + getOwner());
                    MyPetApi.getLogger().warning("Owner online: " + (getOwner() != null ? getOwner().isOnline() : "null"));

                    MyPetApi.getLogger().warning("==========================================");
                    this.die();
                    return;
                }

                if (!hasRider()) {
                    petTargetSelector.tick(); // target selector
                    petPathfinderSelector.tick(); // pathfinder selector
                    petNavigation.tick(); // navigation
                }

                Ride rideSkill = myPet.getSkills().get(RideImpl.class);
                if (this.onGround && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
                    limitCounter += rideSkill.getFlyRegenRate().getValue().doubleValue();
                    if (limitCounter > rideSkill.getFlyLimit().getValue().doubleValue()) {
                        limitCounter = rideSkill.getFlyLimit().getValue().floatValue();
                    }
                }
            }

            mobTick();

            // controls
            getControllerMove().a(); // move
            getControllerLook().a(); // look
            getControllerJump().b(); // jump
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * MyPets are not persistant so no data needs to be saved
     */
    @Override
    public boolean d(NBTTagCompound nbttagcompound) {
        return false;
    }

    /**
     * -> falldamage
     */
    public void c(float f, float f1) {
        if (!this.isFlying) {
            super.c(f, f1);
        }
    }

    public void a(float motionSideways, float motionForward, float f) {
        if (!hasRider || !this.isVehicle()) {
            super.a(motionSideways, motionForward, f);
            return;
        }

        if (this.onGround && this.isFlying) {
            isFlying = false;
            this.fallDistance = 0;
        }

        EntityLiving passenger = (EntityLiving) this.getFirstPassenger();

        if (this.a(TagsFluid.WATER)) {
            this.motY += 0.4;
        }

        Ride rideSkill = myPet.getSkills().get(RideImpl.class);
        if (rideSkill == null || !rideSkill.getActive().getValue()) {
            passenger.stopRiding();
            return;
        }

        //apply pitch & yaw
        this.lastYaw = (this.yaw = passenger.yaw);
        this.pitch = passenger.pitch * 0.5F;
        setYawPitch(this.yaw, this.pitch);
        this.aS = (this.aQ = this.yaw);

        // get motion from passenger (player)
        motionSideways = passenger.bh * 0.5F;
        motionForward = passenger.bj;

        // backwards is slower
        if (motionForward <= 0.0F) {
            motionForward *= 0.25F;
        }
        // sideways is slower too but not as slow as backwards
        motionSideways *= 0.85F;

        float speed = 0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F));
        double jumpHeight = Util.clamp(1 + rideSkill.getJumpHeight().getValue().doubleValue(), 0, 10);
        float ascendSpeed = 0.2f;

        if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
            double factor = Math.log10(myPet.getSaturation()) / 2;
            speed *= factor;
            jumpHeight *= factor;
            ascendSpeed *= factor;
        }

        ride(motionSideways, motionForward, f, speed); // apply motion

        // throw player move event
        if (Configuration.Misc.THROW_PLAYER_MOVE_EVENT_WHILE_RIDING && !(this instanceof EntityMyHorse)) {
            double delta = Math.pow(this.locX - this.lastX, 2.0D) + Math.pow(this.locY - this.lastY, 2.0D) + Math.pow(this.locZ - this.lastZ, 2.0D);
            float deltaAngle = Math.abs(this.yaw - lastYaw) + Math.abs(this.pitch - lastPitch);
            if (delta > 0.00390625D || deltaAngle > 10.0F) {
                Location to = getBukkitEntity().getLocation();
                Location from = new Location(world.getWorld(), this.lastX, this.lastY, this.lastZ, this.lastYaw, this.lastPitch);
                if (from.getX() != Double.MAX_VALUE) {
                    Location oldTo = to.clone();
                    PlayerMoveEvent event = new PlayerMoveEvent((Player) passenger.getBukkitEntity(), from, to);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        passenger.getBukkitEntity().teleport(from);
                        return;
                    }
                    if ((!oldTo.equals(event.getTo())) && (!event.isCancelled())) {
                        passenger.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        return;
                    }
                }
            }
        }

        if (jump != null && this.isVehicle()) {
            boolean doJump = false;
            if (this instanceof IJumpable) {
                if (this.jumpPower > 0.0F) {
                    doJump = true;
                    this.jumpPower = 0.0F;
                } else if (!this.onGround && jump != null) {
                    try {
                        doJump = jump.getBoolean(passenger);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            } else {
                if (jump != null) {
                    try {
                        doJump = jump.getBoolean(passenger);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }

            if (doJump) {
                if (onGround) {
                    jumpHeight = new BigDecimal(jumpHeight).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
                    String jumpHeightString = JumpHelper.JUMP_FORMAT.format(jumpHeight);
                    Double jumpVelocity = JumpHelper.JUMP_MAP.get(jumpHeightString);
                    jumpVelocity = jumpVelocity == null ? 0.44161199999510264 : jumpVelocity;
                    if (this instanceof IJumpable) {
                        getAttributeInstance(EntityHorseAbstract.attributeJumpStrength).setValue(jumpVelocity);
                    }
                    this.motY = jumpVelocity;
                } else if (rideSkill.getCanFly().getValue()) {
                    if (limitCounter <= 0 && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
                        canFly = false;
                    } else if (flyCheckCounter-- <= 0) {
                        canFly = MyPetApi.getHookHelper().canMyPetFlyAt(getBukkitEntity().getLocation());
                        if (canFly && !Permissions.hasExtended(getOwner().getPlayer(), "MyPet.extended.ride.fly")) {
                            canFly = false;
                        }
                        flyCheckCounter = 5;
                    }
                    if (canFly) {
                        if (this.motY < ascendSpeed) {
                            this.motY = ascendSpeed;
                            this.fallDistance = 0;
                            this.isFlying = true;
                        }
                    }
                }
            } else {
                flyCheckCounter = 0;
            }
        }

        if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.Skilltree.Skill.Ride.HUNGER_PER_METER > 0) {
            double dX = locX - lastX;
            double dY = Math.max(0, locY - lastY);
            double dZ = locZ - lastZ;
            if (dX != 0 || dY != 0 || dZ != 0) {
                double distance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
                if (isFlying && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
                    limitCounter -= distance;
                }
                myPet.decreaseSaturation(Configuration.Skilltree.Skill.Ride.HUNGER_PER_METER * distance);
                double factor = Math.log10(myPet.getSaturation()) / 2;
                getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F))) * factor);
            }
        }
    }

    /**
     * -> onLivingUpdate()
     */
    public void tick() {
        super.tick();
        try {
            onLivingUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the default sound of the MyPet
     * -> getLivingSound()
     */
    protected SoundEffect D() {
        try {
            if (getLivingSound() != null && playIdleSound()) {
                makeLivingSound();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DamageSource cr() {
        DamageSource source = super.cr();
        if (deathReason != null) {
            return deathReason;
        }
        return source;
    }
}