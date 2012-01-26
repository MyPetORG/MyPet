/*
* Copyright (C) 2011 Keyle
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

package de.Keyle.MyWolf.Listeners;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class MyWolfWorldListener implements Listener
{
    @EventHandler()
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        for (Entity e : event.getChunk().getEntities())
        {
            if (e instanceof Wolf)
            {
                for (MyWolf w : ConfigBuffer.mWolves.values())
                {
                    if (w.getID() == e.getEntityId())
                    {
                        w.removeWolf();
                        break;
                    }
                }
            }
        }
    }
}
