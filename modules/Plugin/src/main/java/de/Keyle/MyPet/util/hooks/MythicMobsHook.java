/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSetting;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSettings;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerLeashEntityHook;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@PluginHookName("MythicMobs")
public class MythicMobsHook implements PlayerLeashEntityHook {

    @Override
    public boolean onEnable() {
        MyPetApi.getLeashFlagManager().registerLeashFlag(new MythicMobFlag());
        return true;
    }

    @Override
    public void onDisable() {
        MyPetApi.getLeashFlagManager().removeFlag("MythicMobs");
    }

    @Override
    public boolean canLeash(Player attacker, Entity defender) {
        if (Configuration.Hooks.DISABLE_MYTHIC_MOB_LEASHING) {
            try {
                if (MythicMobs.inst().getMobManager().isActiveMob(BukkitAdapter.adapt(defender))) {
                    MythicMob defenderType = MythicMobs.inst().getMobManager().getMythicMobInstance(defender).getType();
                    for (MythicMob m : MythicMobs.inst().getMobManager().getVanillaTypes()) {
                        if (m.equals(defenderType)) {
                            return true;
                        }
                    }
                    return false;
                }
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @LeashFlagName("MythicMobs")
    class MythicMobFlag implements LeashFlag {
        @Override
        public boolean check(Player player, LivingEntity entity, double damage, LeashFlagSettings settings) {
            if (MythicMobs.inst().getMobManager().isActiveMob(BukkitAdapter.adapt(entity))) {
                String name = MythicMobs.inst().getMobManager().getMythicMobInstance(entity).getType().getInternalName();
                for (LeashFlagSetting setting : settings.all()) {
                    if (setting.getValue().equalsIgnoreCase(name)) {
                        return true;
                    }
                }
                return false;
            }

            return true;
        }
    }
}