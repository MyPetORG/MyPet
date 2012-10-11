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
    public static int healtregenTime = 60;
    private int timeCounter = healtregenTime - level;

    public HPregeneration()
    {
        super("HPregeneration");
    }

    @Override
    public void upgrade()
    {
        level++;
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddHPregeneration")).replace("%petname%", myPet.petName).replace("%sec%", "" + (healtregenTime - level)));
    }

    public void schedule()
    {
        if (level > 0 && myPet.status == PetState.Here)
        {
            timeCounter--;
            if (timeCounter <= 0)
            {
                myPet.getPet().getHandle().heal(1, EntityRegainHealthEvent.RegainReason.REGEN);
                timeCounter = healtregenTime - level;
            }
        }
    }
}