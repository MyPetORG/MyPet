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

package de.Keyle.MyPet.entity.types.spider;

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMySpider extends EntityMyPet
{
    public EntityMySpider(World world, MyPet MPet)
    {
        super(world, MPet);
        this.texture = "/mob/spider.png";
        this.a(0.7F, 0.5F);
        this.getNavigation().a(true);

        PathfinderGoalControl Control = new PathfinderGoalControl(MPet, 0.8F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new PathfinderGoalLeapAtTarget(this, 0.8F));
        this.goalSelector.a(3, new PathfinderGoalMeleeAttack(this, 0.8F, true));
        this.goalSelector.a(4, Control);
        this.goalSelector.a(5, new PathfinderGoalFollowOwner(this, 0.8F, 5.0F, 2.0F, Control));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(MPet));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(MPet, Control, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(MPet, 10));
    }

    @Override
    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.myPet = myPet;
            isMyPet = true;

            this.setPathEntity(null);
            this.setHealth(myPet.getHealth() >= getMaxHealth() ? getMaxHealth() : myPet.getHealth());
        }
    }

    public int getMaxHealth()
    {
        return MySpider.getStartHP() + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    public EnumMonsterType getMonsterType()
    {
        return EnumMonsterType.ARTHROPOD;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMySpider(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, (byte) 0); // N/A
    }

    public void a(boolean flag)
    {
        byte b0 = this.datawatcher.getByte(16);
        if (flag)
        {
            b0 = (byte) (b0 | 1);
        }
        else
        {
            b0 &= -2;
        }
        this.datawatcher.watch(16, b0);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aQ()
    {
        return "mob.spider";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aR()
    {
        return "mob.spider";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aS()
    {
        return "mob.spiderdeath";
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

        if (itemStack != null && itemStack.id == org.bukkit.Material.ROTTEN_FLESH.getId())
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
        return false;
    }

    public boolean k(Entity entity)
    {
        int damage = 2 + (isMyPet && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}