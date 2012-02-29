/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.skill.skills;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class HPregeneration extends MyWolfGenericSkill
{
    int HealtregenTime = 60;
    int timeCounter = HealtregenTime - Level;

    public HPregeneration()
    {
        super("HPregeneration");
    }

    @Override
    public void upgrade()
    {
        Level++;
        MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_AddHPregeneration")).replace("%wolfname%", MWolf.Name).replace("%sec%", "" + (HealtregenTime - Level)));
    }

    @Override
    public void schedule()
    {
        if (Level > 0 && MWolf.Status == MyWolf.WolfState.Here)
        {
            timeCounter--;
            if (timeCounter <= 0)
            {
                MWolf.Wolf.getHandle().heal(1, EntityRegainHealthEvent.RegainReason.REGEN);
                timeCounter = HealtregenTime - Level;
            }
        }
    }
}
