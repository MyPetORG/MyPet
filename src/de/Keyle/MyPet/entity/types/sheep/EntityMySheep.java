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

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalControl;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMySheep extends EntityMyPet
{
    public EntityMySheep(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/sheep.png";
        this.a(0.9F, 1.3F);
        this.getNavigation().a(true);

        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F);
        PathfinderGoalEatTile eatGrassPathfinder = new PathfinderGoalEatTile(this);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, controlPathfinder);
        this.goalSelector.a(3, new PathfinderGoalPanic(this, this.walkSpeed + 0.1F));
        this.goalSelector.a(4, new PathfinderGoalFollowOwner(this, this.walkSpeed, 5.0F, 2.0F, controlPathfinder));
        this.goalSelector.a(5, eatGrassPathfinder);
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setColor(((MySheep) myPet).getColor());
            this.setSheared(((MySheep) myPet).isSheared());
        }
    }

    public int getMaxHealth()
    {
        return MyPet.getStartHP(MySheep.class) + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    public int getColor()
    {
        return this.datawatcher.getByte(16) & 15;
    }

    public void setColor(int color)
    {
        byte b0 = this.datawatcher.getByte(16);

        this.datawatcher.watch(16, (byte) (b0 & 240 | color & 15));
    }

    public boolean isSheared()
    {
        return (this.datawatcher.getByte(16) & 16) != 0;
    }

    public void setSheared(boolean sheared)
    {

        byte b0 = this.datawatcher.getByte(16);
        if (sheared)
        {
            this.datawatcher.watch(16, (byte) (b0 | 16));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & -17));
        }
    }

    public int getAge()
    {
        return this.datawatcher.getInt(12);
    }

    public void setAge(int age)
    {
        this.datawatcher.watch(12, age);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMySheep(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // color/sheared
        this.datawatcher.a(12, new Integer(0));     // age
    }

    protected void a(int i, int j, int k, int l)
    {
        this.world.makeSound(this, "mob.sheep.step", 0.15F, 1.0F);
    }

    /**
     * Called when the sheeps eat grass
     */
    public void aG()
    {
        ((MySheep) myPet).setSheared(false);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aW()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aX()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aY()
    {
        return "mob.sheep.say";
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
                this.heal(3, RegainReason.EATING);
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
            if (itemStack != null && itemStack.id == 351)
            {
                if (itemStack.getData() <= 15)
                {
                    ((MySheep) myPet).setColor(15 - itemStack.getData());
                    return true;

                }
            }
            else if (itemStack != null && itemStack.id == Item.SHEARS.id && !((MySheep) myPet).isSheared())
            {
                if (!this.world.isStatic)
                {
                    ((MySheep) myPet).setSheared(true);
                    int i = 1 + this.random.nextInt(3);

                    for (int j = 0 ; j < i ; ++j)
                    {
                        EntityItem entityitem = this.a(new ItemStack(Block.WOOL.id, 1, ((MySheep) myPet).getColor()), 1.0F);

                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                }
                itemStack.damage(1, entityhuman);
            }
        }
        return false;
    }

    /**
     * Called when MyPet will do damage to another entity
     */
    public boolean l(Entity entity)
    {
        int damage = 1 + (isMyPet && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}