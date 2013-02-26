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

package de.Keyle.MyPet.util;

import com.ancientshores.AncientRPG.API.ARPGEntityDamageByEntityEvent;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AncientRpgDamageFix implements Listener
{
    public static void findAncientRpgPlugin()
    {
        boolean active = false;
        if (MyPetUtil.getServer().getPluginManager().isPluginEnabled("AncientRPG"))
        {
            Bukkit.getPluginManager().registerEvents(new AncientRpgDamageFix(), MyPetPlugin.getPlugin());
            active = true;
        }
        MyPetUtil.getDebugLogger().info("AncientRPG DamageFix " + (active ? "" : "not ") + "activated.");
    }

    @EventHandler
    public void onAncientDamageEvent(ARPGEntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof CraftMyPet)
        {
            CraftMyPet craftMyPet = (CraftMyPet) event.getEntity();
            event.setDamage(craftMyPet.getMyPet().getDamage());
        }
    }
}