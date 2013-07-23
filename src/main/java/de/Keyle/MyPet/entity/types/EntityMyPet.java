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
import de.Keyle.MyPet.entity.ai.MyPetAIGoalSelector;
import de.Keyle.MyPet.entity.ai.attack.MyPetAIMeleeAttack;
import de.Keyle.MyPet.entity.ai.attack.MyPetAIRangedAttack;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.ai.navigation.VanillaNavigation;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.skill.skills.implementation.Control;
import de.Keyle.MyPet.skill.skills.implementation.Ride;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import net.minecraft.server.v1_6_R2.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import java.lang.reflect.Field;
import java.util.List;

public abstract class EntityMyPet extends EntityCreature implements IMonster
{
    public MyPetAIGoalSelector petPathfinderSelector, petTargetSelector;
    public EntityLiving goalTarget = null;
    protected double walkSpeed = 0.3F;
    protected boolean isRidden = false;
    protected boolean isMyPet = false;
    protected MyPet myPet;
    protected int idleSoundTimer = 0;
    public AbstractNavigation petNavigation;

    int donatorParticleCounter = 0;

    private Field jump = null;

    public EntityMyPet(World world, MyPet myPet)
    {
        super(world);

        try
        {
            setSize();

            setMyPet(myPet);
            myPet.craftMyPet = (CraftMyPet) this.getBukkitEntity();

            this.petPathfinderSelector = new MyPetAIGoalSelector();
            this.petTargetSelector = new MyPetAIGoalSelector();

            this.walkSpeed = MyPet.getStartSpeed(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
            getAttributeInstance(GenericAttributes.d).setValue(walkSpeed);

            petNavigation = new VanillaNavigation(this);

            this.setPathfinder();

            try
            {
                jump = EntityLiving.class.getDeclaredField("bd");
                jump.setAccessible(true);
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean isMyPet()
    {
        return isMyPet;
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.myPet = myPet;
            isMyPet = true;

            this.getAttributeInstance(GenericAttributes.a).setValue(myPet.getMaxHealth());
            this.setHealth((float) myPet.getHealth());
            this.setCustomName("");
        }
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new MyPetAIFloat(this));
        petPathfinderSelector.addGoal("Sprint", new MyPetAISprint(this, 0.25F));
        petPathfinderSelector.addGoal("RangedTarget", new MyPetAIRangedAttack(this, -0.1F, 35, 12.0F));
        petPathfinderSelector.addGoal("MeleeAttack", new MyPetAIMeleeAttack(this, 0.1F, 3, 20));
        petPathfinderSelector.addGoal("Control", new MyPetAIControl(myPet, 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new MyPetAIFollowOwner(this, 0F, MyPetConfiguration.MYPET_FOLLOW_START_DISTANCE, 2.0F, 17F));
        petPathfinderSelector.addGoal("LookAtPlayer", new MyPetAILookAtPlayer(this, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new MyPetAIRandomLookaround(this));
        petTargetSelector.addGoal("OwnerHurtByTarget", new MyPetAIOwnerHurtByTarget(this));
        petTargetSelector.addGoal("OwnerHurtTarget", new MyPetAIOwnerHurtTarget(this));
        petTargetSelector.addGoal("HurtByTarget", new MyPetAIHurtByTarget(this));
        petTargetSelector.addGoal("ControlTarget", new MyPetAIControlTarget(this, 1));
        petTargetSelector.addGoal("AggressiveTarget", new MyPetAIAggressiveTarget(this, 15));
        petTargetSelector.addGoal("FarmTarget", new MyPetAIFarmTarget(this, 15));
        petTargetSelector.addGoal("DuelTarget", new MyPetAIDuelTarget(this, 5));
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public void setSize()
    {
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null)
        {
            this.a(es.width(), es.height());
        }
    }

    public void setSize(float extra)
    {
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null)
        {
            this.a(es.width(), es.height() + extra);
        }
    }

    public boolean hasRider()
    {
        return passenger != null && getOwner().equals(passenger);
    }

    public void setLocation(Location loc)
    {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    @Override
    public void setCustomName(String ignored)
    {
        try
        {
            if (getCustomNameVisible())
            {
                super.setCustomName(MyPetUtil.cutString(MyPetConfiguration.PET_INFO_OVERHEAD_PREFIX + myPet.getPetName() + MyPetConfiguration.PET_INFO_OVERHEAD_SUFFIX, 64));
                this.setCustomNameVisible(false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public String getCustomName()
    {
        try
        {
            return myPet.getPetName();
        }
        catch (Exception e)
        {
            return super.getCustomName();
        }
    }

    @Override
    public boolean getCustomNameVisible()
    {
        return MyPetConfiguration.PET_INFO_OVERHEAD_NAME;
    }

    @Override
    public void setCustomNameVisible(boolean ignored)
    {
        this.datawatcher.watch(11, Byte.valueOf((byte) (MyPetConfiguration.PET_INFO_OVERHEAD_NAME ? 1 : 0)));
    }

    public boolean canMove()
    {
        return true;
    }

    public double getWalkSpeed()
    {
        return walkSpeed;
    }

    public boolean canEat(ItemStack itemstack)
    {
        List<Integer> foodList = MyPet.getFood(myPet.getClass());
        for (int foodItem : foodList)
        {
            if (itemstack.id == foodItem)
            {
                return true;
            }
        }
        return false;
    }

    public boolean canEquip()
    {
        return MyPetPermissions.hasExtended(getOwner().getPlayer(), "MyPet.user.extended.Equip") && canUseItem();
    }

    public boolean canUseItem()
    {
        return !getOwner().isInExternalGames();
    }

    public boolean playIdleSound()
    {
        if (idleSoundTimer-- <= 0)
        {
            idleSoundTimer = 5;
            return true;
        }
        return false;
    }

    public MyPetPlayer getOwner()
    {
        return myPet.getOwner();
    }

    public boolean damageEntity(DamageSource damagesource, int i)
    {
        boolean damageEntity = false;
        try
        {
            Entity entity = damagesource.getEntity();

            if (entity != null && !(entity instanceof EntityHuman) && !(entity instanceof EntityArrow))
            {
                i = (i + 1) / 2;
            }
            damageEntity = super.damageEntity(damagesource, i);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return damageEntity;
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean attack(Entity entity)
    {
        boolean damageEntity = false;
        try
        {
            double damage = isMyPet() ? myPet.getDamage() : 0;
            if (entity instanceof EntityPlayer)
            {
                Player victim = (Player) entity.getBukkitEntity();
                if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), victim))
                {
                    if (myPet.hasTarget())
                    {
                        myPet.getCraftPet().getHandle().setGoalTarget(null);
                    }
                    return false;
                }
            }
            damageEntity = entity.damageEntity(DamageSource.mobAttack(this), (float) damage);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return damageEntity;
    }

    @Override
    public CraftEntity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
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
    public boolean handlePlayerInteraction(EntityHuman entityhuman)
    {
        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack == null)
        {
            return false;
        }

        Player owner = this.getOwner().getPlayer();

        if (isMyPet() && myPet.getOwner().equals(entityhuman))
        {
            if (myPet.getSkills().isSkillActive("Ride"))
            {
                if (itemStack.id == Ride.RIDE_ITEM && canMove())
                {
                    if (MyPetPermissions.hasExtended(owner, "MyPet.user.extended.Ride"))
                    {
                        ((CraftPlayer) owner).getHandle().setPassengerOf(this);
                        return true;
                    }
                    else
                    {
                        getMyPet().sendMessageToOwner(MyPetLocales.getString("Message.CantUse", myPet.getOwner().getLanguage()));
                    }
                }
            }
            if (myPet.getSkills().isSkillActive("Control"))
            {
                if (itemStack.id == Control.CONTROL_ITEM)
                {
                    return true;
                }
            }
        }
        if (canEat(itemStack) && canUseItem())
        {
            if (owner != null && !MyPetPermissions.hasExtended(owner, "MyPet.user.extended.CanFeed"))
            {
                return false;
            }
            if (this.petTargetSelector.hasGoal("DuelTarget"))
            {
                MyPetAIDuelTarget duelTarget = (MyPetAIDuelTarget) this.petTargetSelector.getGoal("DuelTarget");
                if (duelTarget.getDuelOpponent() != null)
                {
                    return true;
                }
            }
            int addHunger = MyPetConfiguration.HUNGER_SYSTEM_POINTS_PER_FEED;
            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                addHunger -= Math.min(3, getMaxHealth() - getHealth()) * 2;
                this.heal(Math.min(3, getMaxHealth() - getHealth()), RegainReason.EATING);
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                MyPetBukkitUtil.playParticleEffect(myPet.getLocation().add(0, MyPet.getEntitySize(this.getClass())[0] + 0.15, 0), "heart", 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);
            }
            else if (myPet.getHungerValue() < 100)
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                MyPetBukkitUtil.playParticleEffect(myPet.getLocation().add(0, MyPet.getEntitySize(this.getClass())[0] + 0.15, 0), "heart", 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);
            }
            if (addHunger > 0 && myPet.getHungerValue() < 100)
            {
                myPet.setHungerValue(myPet.getHungerValue() + addHunger);
                addHunger = 0;
            }
            if (addHunger < MyPetConfiguration.HUNGER_SYSTEM_POINTS_PER_FEED)
            {
                return true;
            }
        }
        return false;
    }

    public void onLivingUpdate()
    {
        if (MyPetConfiguration.DONATOR_EFFECT && getOwner().isDonator() && donatorParticleCounter-- <= 0)
        {
            donatorParticleCounter = 20 + aC().nextInt(10);
            MyPetBukkitUtil.playParticleEffect(this.getBukkitEntity().getLocation().add(0, 1, 0), "happyVillager", 0.4F, 0.4F, 0.4F, 0.4F, 5, 10);
        }
    }

    protected void initDatawatcher()
    {
    }

    /**
     * Returns the speed of played sounds
     * The faster the higher the sound will be
     */
    public float getSoundSpeed()
    {
        return super.ba();
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

    public void playStepSound()
    {
    }

    public void playStepSound(int i, int j, int k, int l)
    {
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * -> initDatawatcher()
     */
    protected void a()
    {
        super.a();
        try
        {
            initDatawatcher();
        }
        catch (Exception e)
        {
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
    protected boolean a(EntityHuman entityhuman)
    {
        try
        {
            return handlePlayerInteraction(entityhuman);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * -> playStepSound()
     */
    protected void a(int i, int j, int k, int l)
    {
        try
        {
            playStepSound();
            playStepSound(i, j, k, l);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     * -> getHurtSound()
     */
    protected String aN()
    {
        try
        {
            getHurtSound();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the sound that is played when the MyPet dies
     * -> getDeathSound()
     */
    protected String aO()
    {
        try
        {
            getDeathSound();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the speed of played sounds
     */
    protected float ba()
    {
        try
        {
            getSoundSpeed();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return super.ba();
    }

    /**
     * Set weather the "new" AI is used
     */
    public boolean be()
    {
        return true;
    }

    /**
     * Entity AI tick method
     * -> updateAITasks()
     */
    @Override
    protected void bh()
    {
        try
        {
            aV += 1; // entityAge

            getEntitySenses().a(); // sensing
            petTargetSelector.tick(); // target selector
            petPathfinderSelector.tick(); // pathfinder selector
            petNavigation.tick(); // navigation
            bj(); // "mob tick"

            // controls
            getControllerMove().c(); // move
            getControllerLook().a(); // look
            getControllerJump().b(); // jump
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void e(float motionSideways, float motionForward)
    {
        if (this.passenger == null || !(this.passenger instanceof EntityPlayer))
        {
            super.e(motionSideways, motionForward);
            this.Y = 0.5F; // climb height -> halfslab
            return;
        }
        else
        {
            // just the owner can ride the pet
            EntityPlayer passenger = (EntityPlayer) this.passenger;
            if (!getOwner().equals(passenger))
            {
                super.e(motionSideways, motionForward);
                this.Y = 0.5F; // climb height -> halfslab
                return;
            }
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
        if (motionForward <= 0.0F)
        {
            motionForward *= 0.25F;
        }
        // sideways is slower too
        motionSideways *= 0.85F;

        i(0.22222F); // set ride speed
        super.e(motionSideways, motionForward); // apply motion

        // jump when the player jumps
        if (jump != null && onGround)
        {
            try
            {
                if (jump.getBoolean(this.passenger))
                {
                    this.motY = 0.525D;
                }
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
    }

    public void l_()
    {
        super.l_();
        try
        {
            onLivingUpdate();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Returns the default sound of the MyPet
     * -> getLivingSound()
     */
    protected String r()
    {
        try
        {
            getLivingSound();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}