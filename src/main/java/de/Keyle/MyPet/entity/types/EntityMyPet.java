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
import de.Keyle.MyPet.entity.ai.MyPetEntityAISelector;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.ai.navigation.MCNavigation;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.skill.skills.implementation.Control;
import de.Keyle.MyPet.skill.skills.implementation.Ride;
import de.Keyle.MyPet.util.*;
import net.minecraft.server.v1_5_R2.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import java.util.List;

public abstract class EntityMyPet extends EntityCreature implements IMonster
{
    public MyPetEntityAISelector petPathfinderSelector, petTargetSelector;
    public EntityLiving goalTarget = null;
    protected float walkSpeed = 0.3F;
    protected boolean isRidden = false;
    protected boolean isMyPet = false;
    protected MyPet myPet;
    protected int idleSoundTimer = 0;
    public AbstractNavigation petNavigation;

    public EntityMyPet(World world, MyPet myPet)
    {
        super(world);

        setSize();

        setMyPet(myPet);
        myPet.craftMyPet = (CraftMyPet) this.getBukkitEntity();

        this.petPathfinderSelector = new MyPetEntityAISelector();
        this.petTargetSelector = new MyPetEntityAISelector();

        this.walkSpeed = MyPet.getStartSpeed(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());

        petNavigation = new MCNavigation(this);

        this.setPathfinder();
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

            ((LivingEntity) this.getBukkitEntity()).setMaxHealth(myPet.getMaxHealth());
            this.setHealth(myPet.getHealth());
            this.setCustomName("");
        }
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new EntityAIFloat(this));
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed));
        petPathfinderSelector.addGoal("Sprint", new EntityAISprint(this, 0.25F));
        petPathfinderSelector.addGoal("RangedTarget", new EntityAIRangedAttack(myPet, -0.1F, 35, 12.0F));
        petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, 0.1F, 3, 20));
        petPathfinderSelector.addGoal("Control", new EntityAIControl(myPet, 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new EntityAIFollowOwner(this, 0F, MyPetConfiguration.MYPET_FOLLOW_START_DISTANCE, 2.0F, 20F));
        petPathfinderSelector.addGoal("LookAtPlayer", new EntityAILookAtPlayer(this, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new EntityAIRandomLookaround(this));
        petTargetSelector.addGoal("OwnerHurtByTarget", new EntityAIOwnerHurtByTarget(this));
        petTargetSelector.addGoal("OwnerHurtTarget", new EntityAIOwnerHurtTarget(myPet));
        petTargetSelector.addGoal("HurtByTarget", new EntityAIHurtByTarget(this));
        petTargetSelector.addGoal("ControlTarget", new EntityAIControlTarget(myPet, 1));
        petTargetSelector.addGoal("AggressiveTarget", new EntityAIAggressiveTarget(myPet, 15));
        petTargetSelector.addGoal("FarmTarget", new EntityAIFarmTarget(myPet, 15));
        petTargetSelector.addGoal("DuelTarget", new EntityAIDuelTarget(myPet, 5));
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
        return isRidden;
    }

    public void setRidden(boolean flag)
    {
        isRidden = flag;
    }

    public void setLocation(Location loc)
    {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    @Override
    public void setCustomName(String ignored)
    {
        if (getCustomNameVisible())
        {
            super.setCustomName(MyPetUtil.cutString(MyPetConfiguration.PET_INFO_OVERHEAD_PREFIX + myPet.getPetName() + MyPetConfiguration.PET_INFO_OVERHEAD_SUFFIX, 64));
            this.setCustomNameVisible(false);
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
        this.datawatcher.watch(6, Byte.valueOf((byte) (MyPetConfiguration.PET_INFO_OVERHEAD_NAME ? 1 : 0)));
    }

    public boolean canMove()
    {
        return true;
    }

    public float getWalkSpeed()
    {
        return walkSpeed;
    }

    public boolean canEat(ItemStack itemstack)
    {
        List<Material> foodList = MyPet.getFood(myPet.getClass());
        for (Material foodItem : foodList)
        {
            if (itemstack.id == foodItem.getId())
            {
                return true;
            }
        }
        return false;
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

    public EntityLiving getOwner()
    {
        return this.world.a(myPet.getOwner().getName());
    }

    public boolean damageEntity(DamageSource damagesource, int i)
    {
        Entity entity = damagesource.getEntity();

        if (entity != null && !(entity instanceof EntityHuman) && !(entity instanceof EntityArrow))
        {
            i = (i + 1) / 2;
        }
        return super.damageEntity(damagesource, i);
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean attack(Entity entity)
    {
        int damage = isMyPet() ? myPet.getDamage() : 0;
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
        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }

    @Override
    public int getMaxHealth()
    {
        return this.maxHealth;
    }

    protected void tamedEffect(boolean tamed)
    {
        String str = tamed ? "heart" : "smoke";
        for (int i = 0 ; i < 7 ; i++)
        {
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            double d3 = this.random.nextGaussian() * 0.02D;
            this.world.addParticle(str, this.locX + this.random.nextFloat() * this.width * 2.0F - this.width, this.locY + 0.5D + this.random.nextFloat() * this.length, this.locZ + this.random.nextFloat() * this.width * 2.0F - this.width, d1, d2, d3);
        }
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

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a_(EntityHuman entityhuman)
    {
        if (super.a_(entityhuman))
        {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack == null)
        {
            return false;
        }

        Player owner = (Player) this.getOwner().getBukkitEntity();

        if (isMyPet() && entityhuman.getBukkitEntity() == owner)
        {
            if (this.hasRider())
            {
                this.getOwner().mount(null);
                return true;
            }
            if (myPet.getSkills().isSkillActive("Ride"))
            {
                if (itemStack.id == Ride.ITEM.getId() && canMove())
                {
                    if (MyPetPermissions.hasExtended(owner, "MyPet.user.extended.Ride"))
                    {
                        this.getOwner().mount(this);
                        return true;
                    }
                    else
                    {
                        getMyPet().sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CantUse")));
                    }
                }
            }
            if (myPet.getSkills().isSkillActive("Control"))
            {
                if (itemStack.id == Control.ITEM.getId())
                {
                    return true;
                }
            }
        }
        if (canEat(itemStack))
        {
            if (owner != null && !MyPetPermissions.hasExtended(owner, "MyPet.user.extended.CanFeed"))
            {
                return false;
            }
            if (this.petTargetSelector.hasGoal("DuelTarget"))
            {
                EntityAIDuelTarget duelTarget = (EntityAIDuelTarget) this.petTargetSelector.getGoal("DuelTarget");
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
                this.tamedEffect(true);
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
                this.tamedEffect(true);
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

    /**
     * Returns the default sound of the MyPet
     */
    protected abstract String bb();

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected abstract String bc();

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected abstract String bd();

    /**
     * Set weather the "new" AI is used
     */
    public boolean bh()
    {
        return true;
    }


    /**
     * Entity AI tick method
     */
    @Override
    protected void bo()
    {
        bC += 1; // N/A

        aD().a(); // sensing
        petTargetSelector.tick(); // target selector
        petPathfinderSelector.tick(); // pathfinder selector
        petNavigation.tick(); // navigation
        bp(); // "mob tick"

        // controls
        getControllerMove().c(); // move
        getControllerLook().a(); // look
        getControllerJump().b(); // jump
    }
}