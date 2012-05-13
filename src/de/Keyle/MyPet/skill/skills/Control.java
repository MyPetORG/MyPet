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

import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.Location;
import org.bukkit.Material;

public class Control extends MyPetGenericSkill
{
    public static Material Item = Material.STRING;
    private Location moveTo;

    public Control()
    {
        super("Control");
    }

    @Override
    public void upgrade()
    {
        Level = 1;
        MWolf.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddControl")).replace("%wolfname%", MWolf.Name).replace("%item%", Item.name()));
    }

    public Location getLocation()
    {
        Location tmpMoveTo = moveTo;
        moveTo = null;
        return tmpMoveTo;
    }

    public Location getLocation(boolean delete)
    {
        Location tmpMoveTo = moveTo;
        if (delete)
        {
            moveTo = null;
        }
        return tmpMoveTo;
    }

    public void setMoveTo(Location loc)
    {
        moveTo = loc;
    }
}