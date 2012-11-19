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

package de.Keyle.MyPet.entity.types.silverfish;

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;

public class EntityMySilverfish extends EntityMyPet
{
    public EntityMySilverfish(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/silverfish.png";
        this.a(0.3F, 0.7F);
        this.walkSpeed = 0.45F;

        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed+0.1F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(4, controlPathfinder);
        this.goalSelector.a(5, new PathfinderGoalFollowOwner(this, this.walkSpeed, 5.0F, 2.0F, controlPathfinder));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));

        if(MyPet.getStartDamage(MySilverfish.class) > 0)
        {
            this.goalSelector.a(2, new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            this.goalSelector.a(3, new PathfinderGoalMeleeAttack(this, this.walkSpeed, true));
            this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
            this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
            this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
            this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinder, 1));
            this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 13));
            this.targetSelector.a(6, new PathfinderGoalFarmTarget(myPet, 10));
        }
    }

    public int getMaxHealth()
    {
        return MyPet.getStartHP(MySilverfish.class) + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    @Override
    public boolean canEat(ItemStack itemstack)
    {
        return itemstack.id == org.bukkit.Material.SUGAR.getId();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMySilverfish(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
    }

    @Override
    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.silverfish.step", 1.0F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return "mob.silverfish.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.silverfish.hit";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.silverfish.kill";
    }
}