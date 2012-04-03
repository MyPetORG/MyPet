package de.Keyle.MyWolf.entity.pathfinder;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.entity.EntityMyWolf;
import de.Keyle.MyWolf.skill.skills.Behavior;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.*;

import java.util.List;

public class PathfinderGoalAggressive extends PathfinderGoal
{
    private MyWolf MWolf;
    private EntityMyWolf wolf;
    private EntityLiving target;
    Class b;
    private DistanceComparator g;
    private float range;

    public PathfinderGoalAggressive(MyWolf MWolf, float range)
    {
        this.wolf = MWolf.Wolf.getHandle();
        this.MWolf = MWolf;
        this.range = range;
    }

    public boolean a()
    {
        if (MWolf.SkillSystem.hasSkill("Behavior"))
        {
            Behavior behavior = (Behavior) MWolf.SkillSystem.getSkill("Behavior");
            if (behavior.getLevel() > 0)
            {
                if (behavior.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return false;
                }
                else if (behavior.getBehavior() == Behavior.BehaviorState.Aggressive && !MWolf.isSitting())
                {
                    if (target == null || !target.isAlive())
                    {
                        List list = this.wolf.world.a(EntityLiving.class, this.wolf.boundingBox.grow((double) this.range, 4.0D, (double) this.range));

                        for (Object aList : list)
                        {
                            Entity entity = (Entity) aList;
                            EntityLiving entityliving = (EntityLiving) entity;

                            if (wolf.am().canSee(entityliving) && !(entityliving instanceof EntityHuman && ((EntityHuman) entityliving).name.equals(MWolf.getOwner().getName())))
                            {
                                this.target = entityliving;
                                MyWolfUtil.getLogger().info("target: " + entityliving);
                                return true;
                            }
                        }
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void c()
    {
        wolf.al().a(target.getBukkitEntity().getLocation().getX(), target.getBukkitEntity().getLocation().getY(), target.getBukkitEntity().getLocation().getZ(), 0.5f);
        wolf.al().a(false);
        wolf.setTarget(target);
    }

    public void d()
    {
        wolf.setTarget(null);
        target = null;
    }
}
