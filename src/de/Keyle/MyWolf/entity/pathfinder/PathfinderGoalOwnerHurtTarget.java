package de.Keyle.MyWolf.entity.pathfinder;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.entity.EntityMyWolf;
import de.Keyle.MyWolf.skill.skills.Behavior;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityTameableAnimal;
import net.minecraft.server.PathfinderGoalTarget;

public class PathfinderGoalOwnerHurtTarget extends PathfinderGoalTarget
{

    EntityTameableAnimal wolf;
    EntityLiving owner;
    MyWolf MWolf;

    public PathfinderGoalOwnerHurtTarget(MyWolf MWolf)
    {
        super(MWolf.Wolf.getHandle(), 32.0F, false);
        this.wolf = MWolf.Wolf.getHandle();
        this.MWolf = MWolf;
        this.a(1);
    }

    public boolean a()
    {
        if (!this.wolf.isTamed())
        {
            return false;
        }
        else
        {
            EntityLiving entityliving = this.wolf.getOwner();

            if (entityliving == null)
            {
                return false;
            }
            else
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
                    }
                }
                this.owner = ((EntityMyWolf) this.wolf).Goaltarget;
                return this.a(this.owner, false);
            }
        }
    }

    public void c()
    {
        this.c.b(this.owner);
        super.c();
    }
}
