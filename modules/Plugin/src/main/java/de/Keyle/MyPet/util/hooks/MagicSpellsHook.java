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

import com.nisovin.magicspells.events.SpellEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@PluginHookName("MagicSpells")
public class MagicSpellsHook implements PluginHook {

    Method getCasterMethod;

    @Override
    public boolean onEnable() {
        this.getCasterMethod = ReflectionUtil.getMethod(SpellEvent.class, "getCaster");
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerExpGain(SpellTargetEvent event) {
        if (event.getTarget() instanceof MyPetBukkitEntity) {
            try {
                LivingEntity caster = (LivingEntity) this.getCasterMethod.invoke(event);
                if (((MyPetBukkitEntity) event.getTarget()).getOwner().equals(caster)) {
                    event.setCancelled(true);
                } else if (!MyPetApi.getHookHelper().canHurt(event.getCaster(), ((MyPetBukkitEntity) event.getTarget()).getOwner().getPlayer())) {
                    event.setCancelled(true);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}