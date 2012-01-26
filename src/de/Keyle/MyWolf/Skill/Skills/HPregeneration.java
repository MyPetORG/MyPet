/*
* Copyright (C) 2011-2012 Keyle
*
* This file is part of MyWolf.
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

package de.Keyle.MyWolf.Skill.Skills;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.Skill.MyWolfSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class HPregeneration extends MyWolfSkill
{
    public HPregeneration()
    {
        super("HPregeneration");
        registerSkill();
    }

    @Override
    public void activate(MyWolf wolf, Object args)
    {
        if (!MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills." + this.Name))
        {
            return;
        }
        wolf.Healthregen -= 1;
        wolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_AddHPregeneration").replace("%wolfname%", wolf.Name)));
    }
}
