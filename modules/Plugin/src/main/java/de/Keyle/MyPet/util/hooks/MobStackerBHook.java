/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import com.kiwifisher.mobstacker.MobStacker;
import com.kiwifisher.mobstacker.utils.StackUtils;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.LeashEntityHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;

@PluginHookName(value = "MobStacker", classPath = "com.kiwifisher.mobstacker.MobStacker")
public class MobStackerBHook implements LeashEntityHook {

    MobStacker plugin;

    @Override
    public boolean onEnable() {
        plugin = (MobStacker) MyPetApi.getPluginHookManager().getPluginInstance("StackMob").get();
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean prepare(LivingEntity leashedEntity) {
        if (!StackUtils.hasRequiredData(leashedEntity)) {
            return true;
        }
        if (StackUtils.getStackSize(leashedEntity) <= 1) {
            return true;
        }
        LivingEntity peeledEntity = plugin.getStackUtils().peelOffStack(leashedEntity, false);
        peeledEntity.remove();
        return false;
    }
}