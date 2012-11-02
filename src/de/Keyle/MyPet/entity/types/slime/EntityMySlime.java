/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.slime;

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMySlime extends EntityMyPet
{
    public EntityMySlime(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/slime.png";
        this.a(0.3F, 0.7F);
        this.getNavigation().a(true);
        this.walkSpeed = 0.25F;

        PathfinderGoalControl controlPathfinderGoal = new PathfinderGoalControl(myPet, this.walkSpeed+0.1F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new PathfinderGoalLeapAtTarget(this, this.walkSpeed+0.1F));
        this.goalSelector.a(3, new PathfinderGoalMeleeAttack(this, this.walkSpeed+0.1F, true));
        this.goalSelector.a(4, controlPathfinderGoal);
        this.goalSelector.a(5, new PathfinderGoalFollowOwner(this, this.walkSpeed, 5.0F, 2.0F, controlPathfinderGoal));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinderGoal, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 10));
    }

    @Override
    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.myPet = myPet;
            isMyPet = true;

            this.setHealth(myPet.getHealth() >= getMaxHealth() ? getMaxHealth() : myPet.getHealth());
            setSize(((MySlime)myPet).getSize());
        }
    }

    public int getMaxHealth()
    {
        return MyPet.getStartHP(MySlime.class) + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    public int getSize()
    {
        return this.datawatcher.getByte(16);
    }

    public void setSize(int i)
    {
        this.datawatcher.watch(16, (byte) i);
        a(0.6F * i, 0.6F * i);
        this.aV = i;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMySlime(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, (byte) 1); //size
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aW()
    {
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aX()
    {
        return "mob.slime." + (getSize() > 1 ? "big" : "small");
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aY()
    {
        return "mob.slime." + (getSize() > 1 ? "big" : "small");
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean c(EntityHuman entityhuman)
    {
        super.c(entityhuman);

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack != null && itemStack.id == org.bukkit.Material.SUGAR.getId())
        {
            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                this.heal(1, RegainReason.EATING);
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                this.tamedEffect(true);
                return true;
            }
        }
        return false;
    }

    public boolean l(Entity entity)
    {
        int damage = 1 + (isMyPet && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}