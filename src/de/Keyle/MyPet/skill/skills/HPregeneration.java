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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class HPregeneration extends MyPetGenericSkill
{
    public static int HealtregenTime = 60;
    private int timeCounter = HealtregenTime - Level;

    public HPregeneration()
    {
        super("HPregeneration");
    }

    @Override
    public void upgrade()
    {
        Level++;
        MPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddHPregeneration")).replace("%wolfname%", MPet.Name).replace("%sec%", "" + (HealtregenTime - Level)));
    }

    public void schedule()
    {
        if (Level > 0 && MPet.Status == PetState.Here)
        {
            timeCounter--;
            if (timeCounter <= 0)
            {
                MPet.Pet.getHandle().heal(1, EntityRegainHealthEvent.RegainReason.REGEN);
                timeCounter = HealtregenTime - Level;
            }
        }
    }
}