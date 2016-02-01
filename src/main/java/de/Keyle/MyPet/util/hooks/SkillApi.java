/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util.hooks;

import com.sucy.skill.api.event.PlayerGainSkillPointsEvent;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.util.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillApi implements Listener {
    public static int EXP_PERCENT = 100;
    public static boolean DISABLE_VANILLA_EXP = false;
    public static boolean GRANT_EXP = true;


    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("SkillAPI")) {
            active = true;
            Bukkit.getPluginManager().registerEvents(new SkillApi(), MyPetPlugin.getPlugin());
        }
    }

    @EventHandler
    public void onPlayerExpGain(PlayerGainSkillPointsEvent event) {
        if (GRANT_EXP) {
            Player player = event.getPlayerData().getPlayer();
            if (PlayerList.isMyPetPlayer(player)) {
                MyPetPlayer petPlayer = PlayerList.getMyPetPlayer(player);
                if (petPlayer.hasMyPet()) {
                    MyPet myPet = petPlayer.getMyPet();
                    if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null) {
                        if (!myPet.autoAssignSkilltree()) {
                            return;
                        }
                    }
                    myPet.getExperience().addExp(event.getAmount() * EXP_PERCENT / 100);
                }
            }
        }
    }

    public static boolean isActive() {
        return active;
    }
}