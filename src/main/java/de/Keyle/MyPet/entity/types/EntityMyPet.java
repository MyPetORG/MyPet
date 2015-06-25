/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
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
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.hooks.PvPChecker;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

public abstract class EntityMyPet extends EntityCreature implements IMonster {
    public AIGoalSelector petPathfinderSelector, petTargetSelector;
    public EntityLiving goalTarget = null;
    protected double walkSpeed = 0.3F;
    protected boolean hasRider = false;
    protected boolean isMyPet = false;
    protected boolean isInvisible = false;
    protected MyPet myPet;
    protected int idleSoundTimer = 0;
    public AbstractNavigation petNavigation;
    Ride rideSkill = null;

    int donatorParticleCounter = 0;

    private static Field jump = null;

    public EntityMyPet(World world, MyPet myPet) {
        super(world);

        try {
            setSize();

            setMyPet(myPet);
            myPet.craftMyPet = (CraftMyPet) this.getBukkitEntity();

            this.petPathfinderSelector = new AIGoalSelector();
            this.petTargetSelector = new AIGoalSelector();

            this.walkSpeed = MyPet.getStartSpeed(MyPetType.getMyPetTypeByEntityClass(getClass()));
            getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(walkSpeed);

            petNavigation = new VanillaNavigation(this);

            this.setPathfinder();

            if (jump == null) {
                try {
                    jump = EntityLiving.class.getDeclaredField("aY");
                    jump.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    public void applyLeash() {
        if (Configuration.ALWAYS_SHOW_LEASH_FOR_OWNER) {
            ((EntityPlayer) this.bI()).playerConnection.sendPacket(new PacketPlayOutAttachEntity(1, this, this.bI()));
        }
    }
    */

    public boolean isMyPet() {
        return isMyPet;
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            this.myPet = myPet;
            isMyPet = true;

            this.getAttributeInstance(GenericAttributes.maxHealth).setValue(myPet.getMaxHealth());
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
        petPathfinderSelector.addGoal("FollowOwner", new FollowOwner(this, Configuration.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F));
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
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null) {
            this.setSize(es.width(), es.length());
        }
    }

    public float getHeadHeight() {
        float height;
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null) {
            if (es.height() != java.lang.Float.NaN) {
                height = es.height();
            } else {
                height = es.length() * 0.85F;
            }
        } else {
            height = length * 0.85F;
        }
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

    @Override
    public void setCustomName(String ignored) {
        try {
            if (getCustomNameVisible()) {
                String prefix = Configuration.PET_INFO_OVERHEAD_PREFIX;
                String suffix = Configuration.PET_INFO_OVERHEAD_SUFFIX;
                prefix = prefix.replace("<ownername>", getOwner().getName());
                prefix = prefix.replace("<level>", "" + getMyPet().getExperience().getLevel());
                suffix = suffix.replace("<ownername>", getOwner().getName());
                suffix = suffix.replace("<level>", "" + getMyPet().getExperience().getLevel());
                this.setCustomNameVisible(getCustomNameVisible());
                super.setCustomName(Util.cutString(prefix + myPet.getPetName() + suffix, 64));
            }
        } catch (Exception e) {
            MyPetLogger.write("Ignore-------------------------");
            e.printStackTrace();
            MyPetLogger.write("Ignoreend----------------------");
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
        super.setCustomNameVisible(Configuration.PET_INFO_OVERHEAD_NAME);
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

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean attack(Entity entity) {
        boolean damageEntity = false;
        try {
            double damage = isMyPet() ? myPet.getDamage() : 0;
            if (entity instanceof EntityPlayer) {
                Player victim = (Player) entity.getBukkitEntity();
                if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), victim, true)) {
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

        //applyLeash();

        if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
            if (Ride.RIDE_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isSkillActive(Ride.class) && canMove()) {
                    if (Permissions.hasExtended(owner, "MyPet.user.extended.Ride")) {
                        ((CraftPlayer) owner).getHandle().mount(this);
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
                if (itemStack.getItem() == Items.NAME_TAG) {
                    if (itemStack.hasName()) {
                        final String name = itemStack.getName();
                        getMyPet().setPetName(name);
                        EntityMyPet.super.setCustomName("-");
                        myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Name.New", myPet.getOwner()), name));
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            --itemStack.count;
                        }
                        if (itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                        MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                            public void run() {
                                setCustomName("");
                            }
                        }, 1L);
                        return true;
                    }
                }
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
                        BukkitUtil.playParticleEffect(myPet.getLocation().add(0, MyPet.getEntitySize(this.getClass())[0] + 0.15, 0), EnumParticle.HEART, 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);
                    } else if (myPet.getHungerValue() < 100) {
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            --itemStack.count;
                        }
                        if (itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                        BukkitUtil.playParticleEffect(myPet.getLocation().add(0, MyPet.getEntitySize(this.getClass())[0] + 0.15, 0), EnumParticle.HEART, 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);
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
        } else {
            if (itemStack != null) {
                if (itemStack.getItem() == Items.NAME_TAG) {
                    if (itemStack.hasName()) {
                        EntityMyPet.super.setCustomName("-");
                        MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                            public void run() {
                                setCustomName("");
                            }
                        }, 1L);
                        return false;
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
        if (hasRider) {
            if (this.passenger == null || !(this.passenger instanceof EntityPlayer)) {
                hasRider = false;
                //applyLeash();
                this.S = 0.5F; // climb height -> halfslab
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
                        this.S = 1.0F; // climb height -> 1 block
                    } else {
                        this.passenger.mount(null); // just the owner can ride a pet
                    }
                } else {
                    this.passenger.mount(null);
                }
            }
        }
        if (Configuration.INVISIBLE_LIKE_OWNER) {
            if (!isInvisible && getOwner().getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                isInvisible = true;
                myPet.craftMyPet.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false));
            } else if (isInvisible && !getOwner().getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                myPet.craftMyPet.removePotionEffect(PotionEffectType.INVISIBILITY);
                isInvisible = false;
            }
        }

        // donate delete start
        if (!this.isInvisible() && getOwner().getDonationRank() != DonateCheck.DonationRank.None && donatorParticleCounter-- <= 0) {
            donatorParticleCounter = 20 + getRandom().nextInt(10);
            BukkitUtil.playParticleEffect(this.getBukkitEntity().getLocation().add(0, 1, 0), EnumParticle.VILLAGER_HAPPY, 0.4F, 0.4F, 0.4F, 0.4F, 5, 10);
        }
        // donate delete end
    }

    public void setHealth(float f) {
        float deltaHealth = getHealth();
        super.setHealth(f);
        deltaHealth = getHealth() - deltaHealth;

        String msg = myPet.getPetName() + ChatColor.RESET + ": ";
        if (getHealth() > myPet.getMaxHealth() / 3 * 2) {
            msg += org.bukkit.ChatColor.GREEN;
        } else if (getHealth() > myPet.getMaxHealth() / 3) {
            msg += org.bukkit.ChatColor.YELLOW;
        } else {
            msg += org.bukkit.ChatColor.RED;
        }
        if (getHealth() > 0) {
            msg += String.format("%1.2f", getHealth()) + org.bukkit.ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());

            if (!myPet.getOwner().isHealthBarActive()) {
                if (deltaHealth > 0) {
                    msg += " (" + ChatColor.GREEN + "+" + String.format("%1.2f", deltaHealth) + ChatColor.RESET + ")";
                } else {
                    msg += " (" + ChatColor.RED + String.format("%1.2f", deltaHealth) + ChatColor.RESET + ")";
                }
            }
        } else {
            msg += Locales.getString("Name.Dead", getOwner());
        }

        BukkitUtil.sendMessageActionBar(getOwner().getPlayer(), msg);
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

    public void playStepSound(BlockPosition blockposition, Block block) {
        playStepSound();
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * -> initDatawatcher()
     */
    protected void h() {
        super.h();
        try {
            initDatawatcher();
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
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
            DebugLogger.printThrowable(e);
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
            DebugLogger.printThrowable(e);
        }
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     * -> getHurtSound()
     */
    protected String bn() {
        try {
            return getHurtSound();
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     * -> getDeathSound()
     */
    protected String bo() {
        try {
            return getDeathSound();
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return "";
    }

    /**
     * Returns the speed of played sounds
     */
    protected float bB() {
        try {
            return getSoundSpeed();
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return super.bg();
    }


    private int bn;

    public void m() {
        if (this.bn > 0) {
            --this.bn;
        }

        if (this.bc > 0) {
            double d0 = this.locX + (this.bd - this.locX) / (double) this.bc;
            double d1 = this.locY + (this.be - this.locY) / (double) this.bc;
            double d2 = this.locZ + (this.bf - this.locZ) / (double) this.bc;
            double d3 = MathHelper.g(this.bg - (double) this.yaw);
            this.yaw = (float) ((double) this.yaw + d3 / (double) this.bc);
            this.pitch = (float) ((double) this.pitch + (this.bh - (double) this.pitch) / (double) this.bc);
            --this.bc;
            this.setPosition(d0, d1, d2);
            this.setYawPitch(this.yaw, this.pitch);
        } else if (!this.bM()) {
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
        this.doMyPetTick();

        this.world.methodProfiler.b();
        this.world.methodProfiler.a("jump");
        if (this.aY) {
            if (this.V()) {
                this.bG();
            } else if (this.ab()) {
                this.bH();
            } else if (this.onGround && this.bn == 0) {
                this.bF();
                this.bn = 10;
            }
        } else {
            this.bn = 0;
        }

        this.world.methodProfiler.b();
        this.world.methodProfiler.a("travel");
        this.aZ *= 0.98F;
        this.ba *= 0.98F;
        this.bb *= 0.9F;
        this.g(this.aZ, this.ba);
        this.world.methodProfiler.b();
        this.world.methodProfiler.a("push");
        if (!this.world.isClientSide) {
            this.bL();
        }

        this.world.methodProfiler.b();
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

                petTargetSelector.tick(); // target selector
                petPathfinderSelector.tick(); // pathfinder selector
                petNavigation.tick(); // navigation
            }

            E(); // "mob tick"

            // controls
            getControllerMove().c(); // move
            getControllerLook().a(); // look
            getControllerJump().b(); // jump
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
    }

    /*
    public Entity bI() {
        if (Configuration.ALWAYS_SHOW_LEASH_FOR_OWNER) {
            return ((CraftPlayer) getOwner().getPlayer()).getHandle();
        }
        return null;
    }
    */

    @Override
    public boolean d(NBTTagCompound nbttagcompound) {
        return false;
    }

    public void g(float motionSideways, float motionForward) {
        if (!hasRider || this.passenger == null) {
            super.g(motionSideways, motionForward);
            return;
        }

        //apply pitch & yaw
        this.lastYaw = (this.yaw = this.passenger.yaw);
        this.pitch = this.passenger.pitch * 0.5F;
        setYawPitch(this.yaw, this.pitch);
        this.aI = (this.aG = this.yaw);

        // get motion from passenger (player)
        motionSideways = ((EntityLiving) this.passenger).aZ * 0.5F;
        motionForward = ((EntityLiving) this.passenger).ba;

        // backwards is slower
        if (motionForward <= 0.0F) {
            motionForward *= 0.25F;
        }
        // sideways is slower too but not as slow as backwards
        motionSideways *= 0.85F;

        float speed = 0.22222F;
        double jumpHeight = 0.3D;
        if (rideSkill != null) {
            speed *= 1F + (rideSkill.getSpeedPercent() / 100F);
            jumpHeight = rideSkill.getJumpHeight() * 0.18D;
        }
        k(speed); // set ride speed
        super.g(motionSideways, motionForward); // apply motion

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

    public void t_() {
        super.t_();
        try {
            onLivingUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
    }

    /**
     * Returns the default sound of the MyPet
     * -> getLivingSound()
     */
    protected String z() {
        try {
            return playIdleSound() ? getLivingSound() : null;
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return null;
    }
}