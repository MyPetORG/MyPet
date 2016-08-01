/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.util.PluginHook;
import me.NoChance.PvPManager.PvPManager;
import org.bukkit.entity.Player;

@PluginHookName("PvPManager")
public class PvPManagerHook extends PluginHook implements PlayerVersusPlayerHook {

    PvPManager pvpmanager;

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_PvPManager) {
            pvpmanager = MyPetApi.getPluginHookManager().getPluginInstance(PvPManager.class).get();

            try {
                pvpmanager.getClass().getDeclaredMethod("canAttack", Player.class, Player.class);
            } catch (NoSuchMethodException e) {
                MyPetApi.getLogger().warning("Please use PvPManager build 113+");
                return false;
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            return pvpmanager.getPlayerHandler().canAttack(attacker, defender);
        } catch (Throwable ignored) {
        }
        return true;
    }
}