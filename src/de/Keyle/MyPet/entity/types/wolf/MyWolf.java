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

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillSystem;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.util.MyPetConfig;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetSkillTreeConfigLoader;
import org.bukkit.OfflinePlayer;

public class MyWolf extends MyPet
{
    public String Name = "Wolf";

    public MyWolf(OfflinePlayer Owner)
    {
        super(Owner);

        if (MyPetSkillTreeConfigLoader.getSkillTreeNames().length > 0)
        {
            for (String ST : MyPetSkillTreeConfigLoader.getSkillTreeNames())
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyPet.custom.skilltree." + ST))
                {
                    this.skillTree = MyPetSkillTreeConfigLoader.getSkillTree(ST);
                    break;
                }
            }
        }
        if (this.skillTree == null)
        {
            this.skillTree = new MyPetSkillTree("%+-%NoNe%-+%");
        }
        skillSystem = new MyPetSkillSystem(this);
        experience = new MyPetExperience(this);
    }

    public int getMaxHealth()
    {
        return MyPetConfig.StartHP + (skillSystem.hasSkill("HP") ? skillSystem.getSkill("HP").getLevel() : 0);
    }

    public void scheduleTask()
    {
        if (Status != PetState.Despawned && getOwner() != null)
        {
            if (skillSystem.getSkills().size() > 0)
            {
                for (MyPetGenericSkill skill : skillSystem.getSkills())
                {
                    skill.schedule();
                }
            }
            if (Status == PetState.Here)
            {
                if (MyPetConfig.SitdownTime > 0 && SitTimer <= 0)
                {
                    Pet.setSitting(true);
                    ResetSitTimer();
                }
                SitTimer--;
            }
            else if (Status == PetState.Dead)
            {
                RespawnTime--;
                if (RespawnTime <= 0)
                {
                    RespawnPet();
                }
            }
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Wolf;
    }

    @Override
    public String toString()
    {
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + Name + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + Status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}