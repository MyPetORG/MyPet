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

import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalControl;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalAggressiveTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalControlTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalFarmTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;

public class EntityMySheep extends EntityMyPet
{
    public EntityMySheep(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/sheep.png";
        this.a(0.9F, 1.3F);

        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F);
        PathfinderGoalEatTile eatGrassPathfinder = new PathfinderGoalEatTile(this);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(4, controlPathfinder);
        this.goalSelector.a(5, new PathfinderGoalFollowOwner(this, this.walkSpeed, 5.0F, 2.0F, controlPathfinder));
        this.goalSelector.a(6, eatGrassPathfinder);
        this.goalSelector.a(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(7, new PathfinderGoalRandomLookaround(this));

        if(MyPet.getStartDamage(MySheep.class) > 0)
        {
            this.goalSelector.a(2, new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            this.goalSelector.a(3, new PathfinderGoalMeleeAttack(this, this.walkSpeed, true));
            this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
            this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
            this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
            this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinder, 1));
            this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 15));
            this.targetSelector.a(6, new PathfinderGoalFarmTarget(myPet, 15));
        }
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

    @Override
    public boolean canEat(ItemStack itemstack)
    {
        return itemstack.id == org.bukkit.Material.WHEAT.getId();
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

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a(EntityHuman entityhuman)
    {
        super.a(entityhuman);

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (entityhuman == getOwner())
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
                        makeSound("mob.sheep.shear", 1.0F, 1.0F);
                    }
                }
                itemStack.damage(1, entityhuman);
            }
        }
        return false;
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.sheep.step", 0.15F, 1.0F);
    }

    /**
     * Called when the sheeps eat grass
     */
    public void aH()
    {
        ((MySheep) myPet).setSheared(false);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.sheep.say";
    }
}