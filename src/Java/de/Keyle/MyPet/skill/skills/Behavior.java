/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.enderman.MyEnderman;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.EntityLiving;
import net.minecraft.server.v1_4_6.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

@SkillName("Behavior")
@SkillProperties(
        parameterNames = {"friend", "aggro", "farm", "raid"},
        parameterTypes = {NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean})
public class Behavior extends MyPetGenericSkill
{
    private BehaviorState behavior = BehaviorState.Normal;
    private Map<BehaviorState, Boolean> behaviorActive = new HashMap<BehaviorState, Boolean>();
    private boolean active = false;

    public Behavior(boolean addedByInheritance)
    {
        super(addedByInheritance);
        behaviorActive.put(BehaviorState.Normal, true);
        behaviorActive.put(BehaviorState.Aggressive, false);
        behaviorActive.put(BehaviorState.Farm, false);
        behaviorActive.put(BehaviorState.Friendly, false);
        behaviorActive.put(BehaviorState.Raid, false);
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Behavior)
        {
            active = true;
            boolean valuesEdit = false;
            String activeModes = "";
            if (upgrade.getProperties().hasKey("friend"))
            {
                behaviorActive.put(BehaviorState.Friendly, upgrade.getProperties().getBoolean("friend"));
                if (behaviorActive.get(BehaviorState.Friendly))
                {
                    activeModes = "Friendly";
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("aggro"))
            {
                behaviorActive.put(BehaviorState.Aggressive, upgrade.getProperties().getBoolean("aggro"));
                if (behaviorActive.get(BehaviorState.Aggressive))
                {
                    if (!activeModes.equalsIgnoreCase(""))
                    {
                        activeModes += ", ";
                    }
                    activeModes += "Aggressive";
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("farm"))
            {
                behaviorActive.put(BehaviorState.Farm, upgrade.getProperties().getBoolean("farm"));
                if (behaviorActive.get(BehaviorState.Farm))
                {
                    if (!activeModes.equalsIgnoreCase(""))
                    {
                        activeModes += ", ";
                    }
                    activeModes += "Farm";
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("raid"))
            {
                behaviorActive.put(BehaviorState.Raid, upgrade.getProperties().getBoolean("raid"));
                if (behaviorActive.get(BehaviorState.Raid))
                {
                    if (!activeModes.equalsIgnoreCase(""))
                    {
                        activeModes += ", ";
                    }
                    activeModes += "Raid";
                }
                valuesEdit = true;
            }
            if (!quiet && valuesEdit)
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(getFormattedValue()));
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        String activeModes = MyPetLanguage.getString("Name_Normal");
        if (behaviorActive.get(BehaviorState.Friendly))
        {

            activeModes += ", " + MyPetLanguage.getString("Name_Friendly");
        }
        if (behaviorActive.get(BehaviorState.Aggressive))
        {
            if (!activeModes.equalsIgnoreCase(""))
            {
                activeModes += ", ";
            }
            activeModes += MyPetLanguage.getString("Name_Aggressive");
        }
        if (behaviorActive.get(BehaviorState.Farm))
        {
            if (!activeModes.equalsIgnoreCase(""))
            {
                activeModes += ", ";
            }
            activeModes += MyPetLanguage.getString("Name_Farm");
        }
        if (behaviorActive.get(BehaviorState.Raid))
        {
            if (!activeModes.equalsIgnoreCase(""))
            {
                activeModes += ", ";
            }
            activeModes += MyPetLanguage.getString("Name_Raid");
        }
        return MyPetLanguage.getString("Name_Modes") + ": " + activeModes;
    }

    public void reset()
    {
        behavior = BehaviorState.Normal;
        behaviorActive.put(BehaviorState.Normal, true);
        behaviorActive.put(BehaviorState.Aggressive, false);
        behaviorActive.put(BehaviorState.Farm, false);
        behaviorActive.put(BehaviorState.Friendly, false);
        behaviorActive.put(BehaviorState.Raid, false);
        active = false;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        for (String name : getClass().getAnnotation(SkillProperties.class).parameterNames())
        {
            if (getProperties().hasKey(name))
            {
                if (!getProperties().getBoolean(name))
                {
                    html = html.replace("name=\"" + name + "\" checked", "name=\"" + name + "\"");
                }
            }
        }
        return html;
    }

    public static enum BehaviorState
    {
        Normal(true), Friendly(true), Aggressive(true), Raid(true), Farm(true);

        boolean active;

        BehaviorState(boolean active)
        {
            this.active = active;
        }

        public void setActive(boolean active)
        {
            if (this != Normal)
            {
                this.active = active;
            }
        }

        public boolean isActive()
        {
            return this.active;
        }
    }


    public void setBehavior(BehaviorState behaviorState)
    {
        behavior = behaviorState;
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", MyPetLanguage.getString("Name_" + behavior.name())));
        if (behavior == BehaviorState.Friendly)
        {
            myPet.getCraftPet().setTarget(null);
        }
    }

    public void activateBehavior(BehaviorState behaviorState)
    {
        if (active)
        {
            behavior = behaviorState;
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", MyPetLanguage.getString("Name_" + behavior.name())));
            if (behavior == BehaviorState.Friendly)
            {
                myPet.getCraftPet().getHandle().b((EntityLiving) null);
            }
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.getName()));
        }
    }

    public BehaviorState getBehavior()
    {
        return behavior;
    }

    @Override
    public void activate()
    {
        if (active)
        {
            if (behavior == BehaviorState.Normal)
            {
                if (BehaviorState.Friendly.isActive())
                {
                    behavior = BehaviorState.Friendly;
                    myPet.getCraftPet().setTarget(null);
                }
                else if (BehaviorState.Aggressive.isActive())
                {
                    behavior = BehaviorState.Aggressive;
                }
            }
            else if (behavior == BehaviorState.Friendly)
            {
                if (BehaviorState.Aggressive.isActive())
                {
                    behavior = BehaviorState.Aggressive;
                }
                else
                {
                    behavior = BehaviorState.Normal;
                }
            }
            else
            {
                behavior = BehaviorState.Normal;
            }
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", MyPetLanguage.getString("Name_" + behavior.name())));
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.getName()));
        }
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        behavior = BehaviorState.valueOf(nbtTagCompound.getString("Mode"));
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound(getName());
        nbtTagCompound.setString("Mode", behavior.name());
        return nbtTagCompound;
    }

    @Override
    public void schedule()
    {
        if (myPet instanceof MyEnderman)
        {
            MyEnderman myEnderman = (MyEnderman) myPet;
            if (behavior == BehaviorState.Aggressive)
            {
                if (!myEnderman.isScreaming())
                {
                    myEnderman.setScreaming(true);
                }
            }
            else
            {
                if (myEnderman.isScreaming())
                {
                    myEnderman.setScreaming(false);
                }
            }
        }
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Behavior(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}