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

package de.Keyle.MyPet.compat.v1_7_R4.entity;

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
import de.Keyle.MyPet.compat.v1_7_R4.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.compat.v1_7_R4.entity.ai.attack.RangedAttack;
import de.Keyle.MyPet.compat.v1_7_R4.entity.ai.movement.Float;
import de.Keyle.MyPet.compat.v1_7_R4.entity.ai.movement.*;
import de.Keyle.MyPet.compat.v1_7_R4.entity.ai.navigation.VanillaNavigation;
import de.Keyle.MyPet.compat.v1_7_R4.entity.ai.target.*;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import de.Keyle.MyPet.skill.skills.RideImpl;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

public abstract class EntityMyPet extends EntityCreature implements IAnimal, MyPetMinecraftEntity {
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
    protected int sitCounter = 0;
    protected int flyCheckCounter = 0;
    protected AbstractNavigation petNavigation;
    protected Sit sitPathfinder;
    protected int donatorParticleCounter = 0;
    protected float limitCounter = 0;

    private static Field jump = ReflectionUtil.getField(EntityLiving.class, "bc");

    public EntityMyPet(World world, MyPet myPet) {
        super(world);

        try {
            setSize();

            this.myPet = myPet;
            this.petPathfinderSelector = new AIGoalSelector(0);
            this.petTargetSelector = new AIGoalSelector(Configuration.Entity.SKIP_TARGET_AI_TICKS);
            this.walkSpeed = MyPetApi.getMyPetInfo().getSpeed(myPet.getPetType());
            this.getAttributeInstance(GenericAttributes.d).setValue(walkSpeed);
            this.getAttributeInstance(GenericAttributes.b).setValue(32.0f);
            this.petNavigation = new VanillaNavigation(this);
            this.sitPathfinder = new Sit(this);
            this.setHealth((float) myPet.getHealth());
            this.updateNameTag();
            this.setPathfinder();
            this.updateVisuals();
            this.isMyPet = true;
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
            this.a(width, height); // -> this.setSize(float, float)
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
        return passenger != null && getOwner().equals(passenger);
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
        if (entity == null || entity.isDead() || !(entity instanceof CraftLivingEntity)) {
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
    public void setCustomName(String ignored) {
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
                super.setCustomName(Util.cutString(prefix + name + suffix, 64));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCustomName() {
        try {
            return myPet.getPetName();
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
        if (idleSoundTimer-- <= 0) {
            idleSoundTimer = 5;
            return true;
        }
        return false;
    }

    @Override
    public void showPotionParticles(Color color) {
        getDataWatcher().watch(7, color.asRGB());
    }

    @Override
    public void hidePotionParticles() {
        int potionEffects = 0;
        if (!effects.isEmpty()) {
            potionEffects = PotionBrewer.a(effects.values());
        }
        getDataWatcher().watch(7, potionEffects);
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
        MyPetSitEvent sitEvent = new MyPetSitEvent(getMyPet(), this.sitPathfinder.isSitting() ? MyPetSitEvent.Action.Follow : MyPetSitEvent.Action.Stay);
        Bukkit.getPluginManager().callEvent(sitEvent);
        if (!sitEvent.isCancelled()) {
            this.sitPathfinder.toggleSitting();
            if (this.sitPathfinder.isSitting()) {
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
    public boolean handlePlayerInteraction(final EntityHuman entityhuman) {
        new BukkitRunnable() {
            public void run() {
                EntityPlayer player = ((EntityPlayer) entityhuman);
                if (player.getBukkitEntity().isOnline()) {
                    player.updateInventory(entityhuman.defaultContainer);
                }
            }
        }.runTaskLater(MyPetApi.getPlugin(), 5);
        ItemStack itemStack = entityhuman.inventory.getItemInHand();
        Player owner = this.getOwner().getPlayer();

        if (itemStack != null && itemStack.getItem() == Items.LEASH) {
            ((EntityPlayer) entityhuman).playerConnection.sendPacket(new PacketPlayOutAttachEntity(1, this, null));
        }

        if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
            if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isActive(RideImpl.class) && canMove()) {
                    if (Permissions.hasExtended(owner, "MyPet.extended.ride")) {
                        ((CraftPlayer) owner).getHandle().mount(this);
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
                        final String name = itemStack.getName();
                        getMyPet().setPetName(name);
                        EntityMyPet.super.setCustomName("-");
                        myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", myPet.getOwner()), name));
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            --itemStack.count;
                        }
                        if (itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
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
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            if (--itemStack.count <= 0) {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
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
                        EntityMyPet.super.setCustomName("-");
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
            if (!(this.passenger instanceof EntityPlayer)) {
                hasRider = false;
                this.W = 0.5F; // climb height -> halfslab
                Location playerLoc = getOwner().getPlayer().getLocation();
                Location petLoc = getBukkitEntity().getLocation();
                petLoc.setYaw(playerLoc.getYaw());
                petLoc.setPitch(playerLoc.getPitch());
                getOwner().getPlayer().teleport(petLoc);
            }
        } else {
            if (this.passenger != null) {
                if (this.passenger instanceof EntityPlayer) {
                    if (getOwner().equals(this.passenger)) {
                        hasRider = true;
                        this.W = 1.0F; // climb height -> 1 block
                        petTargetSelector.finish();
                        petPathfinderSelector.finish();
                    } else {
                        this.passenger.mount(null); // just the owner can ride a pet
                    }
                } else {
                    this.passenger.mount(null);
                }
            }
        }
        if (sitPathfinder.isSitting() && sitCounter-- <= 0) {
            MyPetApi.getPlatformHelper().playParticleEffect(getOwner().getPlayer(), this.getBukkitEntity().getLocation().add(0, getHeadHeight() + 1, 0), "spell", 0F, 0F, 0F, 1F, 3, 32);
            sitCounter = 30;
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

    public void setHealth(float f) {
        double maxHealth = myPet.getMaxHealth();
        this.getAttributeInstance(GenericAttributes.maxHealth).setValue(maxHealth);
        this.datawatcher.watch(6, MathHelper.a(f, 0.0F, (float) maxHealth));
    }

    protected void initDatawatcher() {
    }

    public Random getRandom() {
        return this.random;
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

    public void playStepSound() {
    }

    public void playStepSound(int i, int j, int k, Block block) {
        playStepSound();
    }

    private void makeLivingSound() {
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

                entityplayer.playerConnection.sendPacket(new PacketPlayOutNamedSoundEffect(getLivingSound(), locX, locY, locZ, volume, getSoundSpeed()));
            }
        }
    }

    private void ride(float motionSideways, float motionForward, float speedModifier) {
        double dX;
        float swimmSpeedModifier;
        float speed;
        float swimmSpeed;

        if (this.M()) { // in water
            dX = this.locY;
            speed = 0.8F;
            swimmSpeed = 0.02F;
            swimmSpeedModifier = 0.5f;

            speed += (0.54600006F - speed) * swimmSpeedModifier / 3.0F;
            swimmSpeed += (speedModifier * 1.0F - swimmSpeed) * swimmSpeedModifier / 3.0F;

            this.a(motionSideways, motionForward, swimmSpeed);
            this.move(this.motX, this.motY, this.motZ);
            this.motX *= (double) speed;
            this.motY *= 0.800000011920929D;
            this.motZ *= (double) speed;
            this.motY -= 0.02D;
            if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + dX, this.motZ)) {
                this.motY = 0.30000001192092896D;
            }
        } else if (this.P()) { // in lava
            dX = this.locY;
            this.a(motionSideways, motionForward, 0.02F);
            this.move(this.motX, this.motY, this.motZ);
            this.motX *= 0.5D;
            this.motY *= 0.5D;
            this.motZ *= 0.5D;
            this.motY -= 0.02D;
            if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + dX, this.motZ)) {
                this.motY = 0.30000001192092896D;
            }
        } else {
            float friction = 0.91F;
            if (this.onGround) {
                friction = this.world.getType(MathHelper.floor(this.locX), MathHelper.floor(this.boundingBox.b) - 1, MathHelper.floor(this.locZ)).frictionFactor * 0.91F;
            }

            speed = speedModifier * (0.16277136F / (friction * friction * friction));

            this.a(motionSideways, motionForward, speed);
            friction = 0.91F;
            if (this.onGround) {
                friction = this.world.getType(MathHelper.floor(this.locX), MathHelper.floor(this.boundingBox.b) - 1, MathHelper.floor(this.locZ)).frictionFactor * 0.91F;
            }

            if (this.h_()) {
                swimmSpeed = 0.15F;
                this.motX = MathHelper.a(this.motX, (double) (-swimmSpeed), (double) swimmSpeed);
                this.motZ = MathHelper.a(this.motZ, (double) (-swimmSpeed), (double) swimmSpeed);
                this.fallDistance = 0.0F;
                if (this.motY < -0.15D) {
                    this.motY = -0.15D;
                }
            }

            this.move(this.motX, this.motY, this.motZ);
            if (this.positionChanged && this.h_()) {
                this.motY = 0.2D;
            }

            this.motY -= 0.08D;

            this.motY *= 0.9800000190734863D;
            this.motX *= (double) friction;
            this.motZ *= (double) friction;
        }

        this.aE = this.aF;
        dX = this.locX - this.lastX;
        double dZ = this.locZ - this.lastZ;
        swimmSpeedModifier = MathHelper.sqrt(dX * dX + dZ * dZ) * 4.0F;
        if (swimmSpeedModifier > 1.0F) {
            swimmSpeedModifier = 1.0F;
        }

        this.aF += (swimmSpeedModifier - this.aF) * 0.4F;
        this.aG += this.aF;
    }


    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * -> initDatawatcher()
     */
    protected void c() {
        super.c();
        try {
            initDatawatcher();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     * -> handlePlayerInteraction(EntityHuman)
     */
    protected boolean a(EntityHuman entityhuman) {
        try {
            boolean result = handlePlayerInteraction(entityhuman);
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
    protected void a(int i, int j, int k, Block block) {
        try {
            playStepSound(i, j, k, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     * -> getHurtSound()
     */
    protected String aT() {
        try {
            return getHurtSound();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     * -> getDeathSound()
     */
    protected String aU() {
        try {
            return getDeathSound();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Returns the speed of played sounds
     */
    protected float bg() {
        try {
            return getSoundSpeed();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.bg();
    }

    public void e() {
        if (this.jumpDelay > 0) {
            --this.jumpDelay;
        }

        if (this.bg > 0) {
            double d0 = this.locX + (this.bh - this.locX) / (double) this.bg;
            double d1 = this.locY + (this.bi - this.locY) / (double) this.bg;
            double d2 = this.locZ + (this.bj - this.locZ) / (double) this.bg;
            double d3 = MathHelper.g(this.bk - (double) this.yaw);
            this.yaw = (float) ((double) this.yaw + d3 / (double) this.bg);
            this.pitch = (float) ((double) this.pitch + (this.bl - (double) this.pitch) / (double) this.bg);
            --this.bg;
            this.setPosition(d0, d1, d2);
            this.b(this.yaw, this.pitch);
        } else if (!this.br()) {
            this.motX *= 0.98D;
            this.motY *= 0.98D;
            this.motZ *= 0.98D;
        }

        if (Math.abs(this.motX) < 0.005D) {
            this.motX = 0.0D;
        }

        if (Math.abs(this.motY) < 0.005D) {
            this.motY = 0.0D;
        }

        if (Math.abs(this.motZ) < 0.005D) {
            this.motZ = 0.0D;
        }

        this.world.methodProfiler.a("ai");
        if (this.bh()) {
            this.bc = false;
            this.bd = 0.0F;
            this.be = 0.0F;
            this.bf = 0.0F;
        } else if (this.br()) {
            doMyPetTick();
        }

        this.world.methodProfiler.b();
        this.world.methodProfiler.a("jump");
        if (this.bc) {
            if (!this.M() && !this.P()) {
                if (this.onGround && this.jumpDelay == 0) {
                    this.bj();
                    this.jumpDelay = 10;
                }
            } else {
                this.motY += 0.03999999910593033D;
            }
        } else {
            this.jumpDelay = 0;
        }

        this.world.methodProfiler.b();
        this.world.methodProfiler.a("travel");
        this.bd *= 0.98F;
        this.be *= 0.98F;
        this.bf *= 0.9F;
        this.e(this.bd, this.be);
        this.world.methodProfiler.b();
        this.world.methodProfiler.a("push");
        this.bo();

        this.world.methodProfiler.b();
    }

    protected void bn() {
        doMyPetTick();
    }

    public boolean bk() {
        return true;
    }

    /**
     * Entity AI tick method
     * -> updateAITasks()
     */
    protected void doMyPetTick() {
        try {
            ++this.aU; // ticksFarFromPlayer

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
            }

            bp(); // "mob tick"

            // controls
            getControllerMove().c(); // move
            getControllerLook().a(); // look
            getControllerJump().b(); // jump

            Ride rideSkill = myPet.getSkills().get(RideImpl.class);
            if (this.onGround && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
                limitCounter += rideSkill.getFlyRegenRate().getValue().doubleValue();
                if (limitCounter > rideSkill.getFlyLimit().getValue().doubleValue()) {
                    limitCounter = rideSkill.getFlyLimit().getValue().floatValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean d(NBTTagCompound nbttagcompound) {
        return false;
    }

    /**
     * -> falldamage
     */
    public void b(float f) {
        if (!this.isFlying) {
            super.b(f);
        }
    }

    /**
     * Allows handlePlayerInteraction() to
     * be fired when a lead is used
     */
    public boolean bN() {
        return true;
    }

    public void e(float motionSideways, float motionForward) {
        if (!this.hasRider || this.passenger == null) {
            super.e(motionSideways, motionForward);
            return;
        }

        if (this.onGround && this.isFlying) {
            isFlying = false;
            this.fallDistance = 0;
        }

        Ride rideSkill = myPet.getSkills().get(RideImpl.class);
        if (rideSkill == null || !rideSkill.getActive().getValue()) {
            this.passenger.mount(null);
            return;
        }

        //apply pitch & yaw
        this.lastYaw = (this.yaw = this.passenger.yaw);
        this.pitch = (this.passenger.pitch * 0.5F);
        b(this.yaw, this.pitch);
        this.aP = (this.aN = this.yaw);


        motionSideways = ((EntityLiving) this.passenger).bd * 0.5F;
        motionForward = ((EntityLiving) this.passenger).be;
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

        if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
            double factor = Math.log10(myPet.getSaturation()) / 2;
            speed *= factor;
            jumpHeight *= factor;
            ascendSpeed *= factor;
        }

        ride(motionSideways, motionForward, speed);

        boolean doJump = false;
        if (jump != null && this.passenger != null) {
            try {
                doJump = jump.getBoolean(this.passenger);
            } catch (IllegalAccessException ignored) {
            }
            if (doJump) {
                if (onGround) {
                    jumpHeight = new BigDecimal(jumpHeight).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
                    String jumpHeightString = JumpHelper.JUMP_FORMAT.format(jumpHeight);
                    Double jumpVelocity = JumpHelper.JUMP_MAP.get(jumpHeightString);
                    jumpVelocity = jumpVelocity == null ? 0.44161199999510264 : jumpVelocity;
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
                        this.motY = ascendSpeed;
                        this.fallDistance = 0;
                        this.isFlying = true;
                    }
                }
            } else {
                flyCheckCounter = 0;
            }
        }

        // decrease saturation
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
            }
        }
    }

    public void h() {
        super.h();
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
    protected String t() {
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
}