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

package de.Keyle.MyPet.entity.types.zombie;

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;

public class EntityMyZombie extends EntityMyPet
{
    public EntityMyZombie(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/zombie.png";
        this.a(0.9F, 0.9F);
        this.getNavigation().a(true);

        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
        this.goalSelector.a(3, new PathfinderGoalMeleeAttack(this, this.walkSpeed, true));
        this.goalSelector.a(4, controlPathfinder);
        this.goalSelector.a(5, new PathfinderGoalFollowOwner(this, this.walkSpeed, 10.0F, 5.0F, controlPathfinder));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinder, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 13));
    }

    public boolean isBaby()
    {
        return getDataWatcher().getByte(12) == 1;
    }

    public void setBaby(boolean flag)
    {
        getDataWatcher().watch(12, (byte) 1);
    }

    public boolean isVillager()
    {
        return getDataWatcher().getByte(13) == 1;
    }

    public void setVillager(boolean flag)
    {
        getDataWatcher().watch(13, (byte) (flag ? 1 : 0));
    }

    public int getMaxHealth()
    {
        return MyPet.getStartHP(MyZombie.class) + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    @Override
    public boolean canEat(ItemStack itemstack)
    {
        return itemstack.id == org.bukkit.Material.ROTTEN_FLESH.getId();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyZombie(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        getDataWatcher().a(12, Byte.valueOf((byte) 0)); // is baby
        getDataWatcher().a(13, Byte.valueOf((byte) 0)); // is villager
    }

    protected void a(int i, int j, int k, int l)
    {
        this.world.makeSound(this, "mob.zombie.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aW()
    {
        return "mob.zombie.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aX()
    {
        return "mob.zombie.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aY()
    {
        return "mob.zombie.death";
    }

    public boolean l(Entity entity)
    {
        int damage = MyPet.getStartDamage(this.myPet.getClass()) + (isMyPet() && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}