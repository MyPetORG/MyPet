package de.Keyle.MyWolf.entity.pathfinder;

import de.Keyle.MyWolf.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import de.Keyle.MyWolf.skill.skills.Behavior;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityTameableAnimal;
import net.minecraft.server.PathfinderGoalTarget;
import org.bukkit.entity.Player;

public class PathfinderGoalOwnerHurtTarget extends PathfinderGoalTarget
{

    EntityTameableAnimal wolf;
    EntityLiving target;
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
            EntityLiving owner = this.wolf.getOwner();

            if (owner == null)
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
                this.target = ((EntityMyWolf) this.wolf).Goaltarget;
                ((EntityMyWolf) this.wolf).Goaltarget = null;

                if (this.target instanceof EntityPlayer)
                {
                    String playerName = ((EntityPlayer) this.target).name;
                    if (!MyWolfUtil.getOfflinePlayer(playerName).isOnline())
                    {
                        this.target = null;
                        return false;
                    }
                    Player target = MyWolfUtil.getOfflinePlayer(playerName).getPlayer();

                    if (target == MWolf.getOwner())
                    {
                        this.target = null;
                        return false;
                    }
                    else if (!MyWolfUtil.canHurtFactions(MWolf.getOwner().getPlayer(), target))
                    {
                        this.target = null;
                        return false;
                    }
                    else if (!MyWolfUtil.canHurtTowny(MWolf.getOwner().getPlayer(), target))
                    {
                        this.target = null;
                        return false;
                    }
                    else if (!MyWolfUtil.canHurtWorldGuard(target))
                    {
                        this.target = null;
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public void c()
    {
        this.c.b(this.target);
        super.c();
    }
}