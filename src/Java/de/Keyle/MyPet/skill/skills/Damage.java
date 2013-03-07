/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

@SkillName("Damage")
@SkillProperties(
        parameterNames = {"damage", "addset_damage"}, parameterTypes = {NBTdatatypes.Int, NBTdatatypes.String},
        parameterDefaultValues = {"1", "add"})
public class Damage extends MyPetGenericSkill
{
    private boolean isPassive = true;
    private int damageIncrease = 0;

    public Damage(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return damageIncrease > 0;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Damage)
        {
            if (getMyPet().getDamage() > 0)
            {
                isPassive = false;
            }
            if (upgrade.getProperties().getValue().containsKey("damage"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_damage") || ((StringTag) upgrade.getProperties().getValue().get("addset_damage")).getValue().equals("add"))
                {
                    damageIncrease += ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                }
                else
                {
                    damageIncrease = ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                }
                if (damageIncrease > 0 && isPassive)
                {
                    if (myPet.getStatus() == PetState.Here)
                    {
                        getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().setPathfinder();
                    }
                    isPassive = false;
                }
                else if (damageIncrease <= 0 && !isPassive)
                {
                    if (myPet.getStatus() == PetState.Here)
                    {
                        getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().setPathfinder();
                    }
                    isPassive = true;
                }
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddDamage")).replace("%petname%", myPet.petName).replace("%dmg%", "" + damageIncrease));
                }
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return "+" + damageIncrease;
    }

    public void reset()
    {
        damageIncrease = 0;
    }

    public int getDamageIncrease()
    {
        return damageIncrease;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().getValue().containsKey("damage"))
        {
            int damage = ((IntTag) getProperties().getValue().get("damage")).getValue();
            html = html.replace("value=\"0\"", "value=\"" + damage + "\"");
            if (getProperties().getValue().containsKey("addset_damage"))
            {
                if (((StringTag) getProperties().getValue().get("addset_damage")).getValue().equals("set"))
                {
                    html = html.replace("name=\"addset_damage\" value=\"add\" checked", "name=\"addset_damage\" value=\"add\"");
                    html = html.replace("name=\"addset_damage\" value=\"set\"", "name=\"addset_damage\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Damage(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}