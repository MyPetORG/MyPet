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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;

@PluginHookName("Factions")
public class FactionsHook implements PlayerVersusPlayerHook {

    enum ApiVersion {
        V1, V2, V3,
        Savage
    }

    ApiVersion apiVersion = ApiVersion.V2;
    Method engineMethod;
    Object engine;

    @SuppressWarnings("unchecked")
    @Override
    public boolean onEnable() {
        try {
            Class engineClass = ReflectionUtil.getClass("com.massivecraft.factions.engine.EngineMain");
            Method getMethod = engineClass.getDeclaredMethod("get");
            engine = getMethod.invoke(null);
            engineMethod = engineClass.getDeclaredMethod("canCombatDamageHappen", EntityDamageByEntityEvent.class, boolean.class);
            engineMethod.setAccessible(true);
            apiVersion = ApiVersion.V1;
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Class engineClass = ReflectionUtil.getClass("com.massivecraft.factions.engine.EngineCombat");
            Method getMethod = engineClass.getDeclaredMethod("get");
            engine = getMethod.invoke(null);
            engineMethod = engineClass.getDeclaredMethod("canCombatDamageHappen", EntityDamageByEntityEvent.class, boolean.class);
            engineMethod.setAccessible(true);
            apiVersion = ApiVersion.V2;
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Class engineClass = ReflectionUtil.getClass("com.massivecraft.factions.engine.EngineCanCombatHappen");
            Method getMethod = engineClass.getDeclaredMethod("get");
            engine = getMethod.invoke(null);
            engineMethod = engineClass.getDeclaredMethod("canCombatDamageHappen", EntityDamageByEntityEvent.class, boolean.class);
            engineMethod.setAccessible(true);
            apiVersion = ApiVersion.V3;
            return true;
        } catch (Throwable ignored) {
        }
        try {
            for (RegisteredListener rl : EntityDamageEvent.getHandlerList().getRegisteredListeners()) {
                Listener l = rl.getListener();
                if (l.getClass().getName().equalsIgnoreCase("com.massivecraft.factions.listeners.FactionsEntityListener")) {
                    engine = l;
                    engineMethod = l.getClass().getDeclaredMethod("canDamagerHurtDamagee", EntityDamageByEntityEvent.class, boolean.class);
                    engineMethod.setAccessible(true);
                    apiVersion = ApiVersion.Savage;
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        MyPetApi.getLogger().warning("Factions was found but no suitable MyPet hook was provided. Please report this to the MyPet developer.");
        MyPetApi.getLogger().warning("Factions version: " + MyPetApi.getPluginHookManager().getPluginInstance("Factions").get().getDescription().getVersion());
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0.);
            switch (apiVersion) {
                case V1:
                case V2:
                case V3:
                case Savage:
                    return engineMethod.invoke(engine, sub, false).equals(true);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}