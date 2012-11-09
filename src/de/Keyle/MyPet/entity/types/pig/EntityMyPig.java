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

package de.Keyle.MyPet.entity.types.pig;

import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalControl;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMyPig extends EntityMyPet
{
    public EntityMyPig(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/pig.png";
        this.a(0.9F, 0.9F);
        this.getNavigation().a(true);

        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, controlPathfinder);
        this.goalSelector.a(3, new PathfinderGoalPanic(this, this.walkSpeed + 0.1F));
        this.goalSelector.a(4, new PathfinderGoalFollowOwner(this, this.walkSpeed, 5.0F, 2.0F, controlPathfinder));
        this.goalSelector.a(5, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(5, new PathfinderGoalRandomLookaround(this));
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setSaddle(((MyPig) myPet).hasSaddle());
        }
    }

    public int getMaxHealth()
    {
        return MyPet.getStartHP(MyPig.class) + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    public boolean hasSaddle()
    {
        return (this.datawatcher.getByte(16) & 0x1) != 0;
    }

    public void setSaddle(boolean saddle)
    {
        if (saddle)
        {
            this.datawatcher.watch(16, (byte) 1);
        }
        else
        {
            this.datawatcher.watch(16, (byte) 0);
        }
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyPig(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // saddle
        this.datawatcher.a(12, new Integer(0));        // age
    }

    protected void a(int i, int j, int k, int l)
    {
        this.world.makeSound(this, "mob.pig.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aW()
    {
        return "mob.pig.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aX()
    {
        return "mob.pig.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aY()
    {
        return "mob.pig.death";
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

        if (itemStack != null && itemStack.id == org.bukkit.Material.WHEAT.getId())
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
        else if (entityhuman == getOwner())
        {
            if (itemStack != null && itemStack.id == 329 && !((MyPig) myPet).hasSaddle())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                ((MyPig) myPet).setSaddle(true);
            }
        }
        return false;
    }

    public boolean l(Entity entity)
    {
        int damage = MyPet.getStartDamage(this.myPet.getClass()) + (isMyPet() && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}