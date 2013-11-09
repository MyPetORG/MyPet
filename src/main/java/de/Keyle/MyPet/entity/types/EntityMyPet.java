/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.AIGoalSelector;
import de.Keyle.MyPet.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.entity.ai.attack.RangedAttack;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.movement.Float;
import de.Keyle.MyPet.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.ai.navigation.VanillaNavigation;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.skill.skills.implementation.Ride;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.itemstringinterpreter.ConfigItem;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.support.Permissions;
import de.Keyle.MyPet.util.support.PvPChecker;
import net.minecraft.server.v1_6_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

public abstract class EntityMyPet extends EntityCreature implements IMonster {
    public AIGoalSelector petPathfinderSelector, petTargetSelector;
    public EntityLiving goalTarget = null;
    protected double walkSpeed = 0.3F;
    protected boolean hasRider = false;
    protected boolean isMyPet = false;
    protected MyPet myPet;
    protected int idleSoundTimer = 0;
    public AbstractNavigation petNavigation;
    Ride rideSkill = null;

    private Field jump = null;

    public EntityMyPet(World world, MyPet myPet) {
        super(world);

        try {
            setSize();

            setMyPet(myPet);
            myPet.craftMyPet = (CraftMyPet) this.getBukkitEntity();

            this.petPathfinderSelector = new AIGoalSelector();
            this.petTargetSelector = new AIGoalSelector();

            this.walkSpeed = MyPet.getStartSpeed(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
            getAttributeInstance(GenericAttributes.d).setValue(walkSpeed);

            petNavigation = new VanillaNavigation(this);

            this.setPathfinder();

            try {
                jump = EntityLiving.class.getDeclaredField("bd");
                jump.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyLeash() {
        if (Configuration.ALWAYS_SHOW_LEASH_FOR_OWNER) {
            ((EntityPlayer) this.bI()).playerConnection.sendPacket(new Packet39AttachEntity(1, this, this.bI()));
        }
    }

    public boolean isMyPet() {
        return isMyPet;
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            this.myPet = myPet;
            isMyPet = true;

            this.getAttributeInstance(GenericAttributes.a).setValue(myPet.getMaxHealth());
            this.setHealth((float) myPet.getHealth());
            this.setCustomName("");

            rideSkill = myPet.getSkills().getSkill(Ride.class);
        }
    }

    public void setPathfinder() {
        petPathfinderSelector.addGoal("Float", new Float(this));
        petPathfinderSelector.addGoal("Sprint", new Sprint(this, 0.25F));
        petPathfinderSelector.addGoal("RangedTarget", new RangedAttack(this, -0.1F, 12.0F));
        petPathfinderSelector.addGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3, 20));
        petPathfinderSelector.addGoal("Control", new Control(myPet, 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new FollowOwner(this, 0F, Configuration.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F));
        petPathfinderSelector.addGoal("LookAtPlayer", new LookAtPlayer(this, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new RandomLookaround(this));
        petTargetSelector.addGoal("OwnerHurtByTarget", new OwnerHurtByTarget(this));
        petTargetSelector.addGoal("OwnerHurtTarget", new OwnerHurtTarget(this));
        petTargetSelector.addGoal("HurtByTarget", new HurtByTarget(this));
        petTargetSelector.addGoal("ControlTarget", new ControlTarget(this, 1));
        petTargetSelector.addGoal("AggressiveTarget", new BehaviorAggressiveTarget(this, 15));
        petTargetSelector.addGoal("FarmTarget", new BehaviorFarmTarget(this, 15));
        petTargetSelector.addGoal("DuelTarget", new BehaviorDuelTarget(this, 5));
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public void setSize() {
        setSize(0F);
    }

    public void setSize(float extra) {
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null) {
            this.a(es.width(), es.height() + extra);
        }
    }

    public boolean hasRider() {
        return passenger != null && getOwner().equals(passenger);
    }

    public void setLocation(Location loc) {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    @Override
    public void setCustomName(String ignored) {
        try {
            if (getCustomNameVisible()) {
                super.setCustomName(Util.cutString(Configuration.PET_INFO_OVERHEAD_PREFIX + myPet.getPetName() + Configuration.PET_INFO_OVERHEAD_SUFFIX, 64));
                this.setCustomNameVisible(false);
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
        return Configuration.PET_INFO_OVERHEAD_NAME;
    }

    @Override
    public void setCustomNameVisible(boolean ignored) {
        this.datawatcher.watch(11, Byte.valueOf((byte) (Configuration.PET_INFO_OVERHEAD_NAME ? 1 : 0)));
    }

    public boolean canMove() {
        return true;
    }

    public double getWalkSpeed() {
        return walkSpeed;
    }

    public boolean canEat(ItemStack itemstack) {
        List<ConfigItem> foodList = MyPet.getFood(myPet.getClass());
        for (ConfigItem foodItem : foodList) {
            if (foodItem.compare(itemstack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canEquip() {
        return Permissions.hasExtended(getOwner().getPlayer(), "MyPet.user.extended.Equip") && canUseItem();
    }

    public boolean canUseItem() {
        return !getOwner().isInExternalGames();
    }

    public boolean playIdleSound() {
        if (idleSoundTimer-- <= 0) {
            idleSoundTimer = 5;
            return true;
        }
        return false;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public boolean damageEntity(DamageSource damagesource, int i) {
        boolean damageEntity = false;
        try {
            Entity entity = damagesource.getEntity();

            if (entity != null && !(entity instanceof EntityHuman) && !(entity instanceof EntityArrow)) {
                i = (i + 1) / 2;
            }
            damageEntity = super.damageEntity(damagesource, i);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return damageEntity;
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
                if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), victim)) {
                    if (myPet.hasTarget()) {
                        myPet.getCraftPet().getHandle().setGoalTarget(null);
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
    public boolean isPersistent() {
        return false;
    }

    @Override
    public CraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPet(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Method handler ------------------------------------------------------------------------------------

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        ItemStack itemStack = entityhuman.inventory.getItemInHand();
        Player owner = this.getOwner().getPlayer();

        applyLeash();

        if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
            if (Ride.RIDE_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isSkillActive(Ride.class) && canMove()) {
                    if (Permissions.hasExtended(owner, "MyPet.user.extended.Ride")) {
                        ((CraftPlayer) owner).getHandle().setPassengerOf(this);
                        return true;
                    } else {
                        getMyPet().sendMessageToOwner(Locales.getString("Message.No.CanUse", myPet.getOwner().getLanguage()));
                    }
                }
            }
            if (de.Keyle.MyPet.skill.skills.implementation.Control.CONTROL_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isSkillActive(de.Keyle.MyPet.skill.skills.implementation.Control.class)) {
                    return true;
                }
            }
            if (itemStack != null) {
                if (canEat(itemStack) && canUseItem()) {
                    if (owner != null && !Permissions.hasExtended(owner, "MyPet.user.extended.CanFeed")) {
                        return false;
                    }
                    if (this.petTargetSelector.hasGoal("DuelTarget")) {
                        BehaviorDuelTarget duelTarget = (BehaviorDuelTarget) this.petTargetSelector.getGoal("DuelTarget");
                        if (duelTarget.getDuelOpponent() != null) {
                            return true;
                        }
                    }
                    int addHunger = Configuration.HUNGER_SYSTEM_POINTS_PER_FEED;
                    if (getHealth() < getMaxHealth()) {
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            --itemStack.count;
                        }
                        addHunger -= Math.min(3, getMaxHealth() - getHealth()) * 2;
                        this.heal(Math.min(3, getMaxHealth() - getHealth()), RegainReason.EATING);
                        if (itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                        BukkitUtil.playParticleEffect(myPet.getLocation().add(0, MyPet.getEntitySize(this.getClass())[0] + 0.15, 0), "heart", 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);
                    } else if (myPet.getHungerValue() < 100) {
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            --itemStack.count;
                        }
                        if (itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                        BukkitUtil.playParticleEffect(myPet.getLocation().add(0, MyPet.getEntitySize(this.getClass())[0] + 0.15, 0), "heart", 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);
                    }
                    if (addHunger > 0 && myPet.getHungerValue() < 100) {
                        myPet.setHungerValue(myPet.getHungerValue() + addHunger);
                        addHunger = 0;
                    }
                    if (addHunger < Configuration.HUNGER_SYSTEM_POINTS_PER_FEED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void onLivingUpdate() {
        if (getOwner().getPlayer().isSneaking() != isSneaking()) {
            this.setSneaking(!isSneaking());
        }
    }

    protected void initDatawatcher() {
    }

    public Random getRandom() {
        return aD();
    }

    /**
     * Returns the speed of played sounds
     * The faster the higher the sound will be
     */
    public float getSoundSpeed() {
        float pitchAddition = 0;
        if (getMyPet() instanceof IMyPetBaby) {
            if (((IMyPetBaby) getMyPet()).isBaby()) {
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

    public void playStepSound(int i, int j, int k, int l) {
        playStepSound();
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * -> initDatawatcher()
     */
    protected void a() {
        super.a();
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
            return handlePlayerInteraction(entityhuman);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * -> playStepSound()
     */
    protected void a(int i, int j, int k, int l) {
        try {
            playStepSound(i, j, k, l);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     * -> getHurtSound()
     */
    protected String aO() {
        try {
            return getHurtSound();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the sound that is played when the MyPet dies
     * -> getDeathSound()
     */
    protected String aP() {
        try {
            return getDeathSound();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the speed of played sounds
     */
    protected float bb() {
        try {
            return getSoundSpeed();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.bb();
    }

    /**
     * Set weather the "new" AI is used
     */
    public boolean bf() {
        return true;
    }

    /**
     * Entity AI tick method
     * -> updateAITasks()
     */
    @Override
    protected void bi() {
        try {
            aV += 1; // entityAge

            if (isAlive()) {
                getEntitySenses().a(); // sensing

                petTargetSelector.tick(); // target selector
                petPathfinderSelector.tick(); // pathfinder selector
                petNavigation.tick(); // navigation
            }

            bj(); // "mob tick"

            // controls
            getControllerMove().c(); // move
            getControllerLook().a(); // look
            getControllerJump().b(); // jump
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Entity bI() {
        if (Configuration.ALWAYS_SHOW_LEASH_FOR_OWNER) {
            return ((CraftPlayer) getOwner().getPlayer()).getHandle();
        }
        return null;
    }

    @Override
    public boolean d(NBTTagCompound nbttagcompound) {
        return false;
    }

    public void e(float motionSideways, float motionForward) {
        if (this.passenger == null || !(this.passenger instanceof EntityPlayer)) {
            if (hasRider) {
                hasRider = false;
                applyLeash();
                setSize();
            }
            super.e(motionSideways, motionForward);
            this.Y = 0.5F; // climb height -> halfslab
            return;
        } else {
            // just the owner can ride the pet
            EntityPlayer passenger = (EntityPlayer) this.passenger;
            if (!getOwner().equals(passenger)) {
                super.e(motionSideways, motionForward);
                this.Y = 0.5F; // climb height -> halfslab
                return;
            }
        }

        if (!hasRider) {
            hasRider = true;
            setSize(1F);
        }

        this.Y = 1.0F; // climb height -> 1 block

        //apply pitch & yaw
        this.lastYaw = (this.yaw = this.passenger.yaw);
        this.pitch = this.passenger.pitch * 0.5F;
        b(this.yaw, this.pitch);
        this.aP = (this.aN = this.yaw);

        // get motion from passenger (player)
        motionSideways = ((EntityLiving) this.passenger).be * 0.5F;
        motionForward = ((EntityLiving) this.passenger).bf;

        // backwards is slower
        if (motionForward <= 0.0F) {
            motionForward *= 0.25F;
        }
        // sideways is slower too
        motionSideways *= 0.85F;

        float speed = 0.22222F;
        double jumpHeight = 0.3D;
        if (rideSkill != null) {
            speed *= 1F + (rideSkill.getSpeedPercent() / 100F);
            jumpHeight = rideSkill.getJumpHeight() * 0.18D;
        }
        i(speed); // set ride speed
        super.e(motionSideways, motionForward); // apply motion

        // jump when the player jumps
        if (jump != null && onGround) {
            try {
                if (jump.getBoolean(this.passenger)) {
                    this.motY = Math.sqrt(jumpHeight);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    public void l_() {
        super.l_();
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
    protected String r() {
        try {
            return playIdleSound() ? getLivingSound() : null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}