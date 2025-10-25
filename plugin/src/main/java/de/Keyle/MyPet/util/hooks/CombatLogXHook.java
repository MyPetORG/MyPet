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

import com.SirBlobman.combatlogx.config.ConfigOptions;
import com.SirBlobman.combatlogx.utility.CombatUtil;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static com.SirBlobman.combatlogx.event.PlayerTagEvent.TagReason;
import static com.SirBlobman.combatlogx.event.PlayerTagEvent.TagType;

@PluginHookName("CombatLogX")
public class CombatLogXHook implements PluginHook {

    public static boolean IGNORE_PLUGIN_SETTINGS = false;

    @Override
    public boolean onEnable() {
        try {
            TagType.PLAYER.ordinal();
            Class.forName("com.SirBlobman.combatlogx.config.ConfigOptions");
            if (ReflectionUtil.getField(ConfigOptions.class, "OPTION_LINK_PROJECTILES") == null) {
                throw new Throwable();
            }
            if (ReflectionUtil.getField(ConfigOptions.class, "OPTION_LINK_PETS") == null) {
                throw new Throwable();
            }
        } catch (Throwable e) {
            return false;
        }
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void loadConfig(ConfigurationSection config) {
        config.addDefault("MyPet.Hooks.CombatLogX.Ignore-Plugin-Settings", IGNORE_PLUGIN_SETTINGS);

        IGNORE_PLUGIN_SETTINGS = config.getBoolean("Ignore-Plugin-Settings", false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityDamageByEntityEvent e) {
        Entity damaged = e.getEntity();
        Entity damager = e.getDamager();

        if ((damager instanceof CraftMyPetProjectile) && (ConfigOptions.OPTION_LINK_PROJECTILES || IGNORE_PLUGIN_SETTINGS)) {
            damager = ((CraftMyPetProjectile) damager).getShootingMyPet();
        }

        if ((damager instanceof MyPetBukkitEntity) && (ConfigOptions.OPTION_LINK_PETS || IGNORE_PLUGIN_SETTINGS)) {
            damager = ((MyPetBukkitEntity) damager).getOwner().getPlayer();
        } else {
            return;
        }

        if (damager != null && damaged instanceof LivingEntity) {
            if (damaged instanceof Player) {
                Player p = (Player) damaged;
                LivingEntity enemy = (LivingEntity) damager;
                TagReason reason = TagReason.ATTACKED;
                CombatUtil.tag(p, enemy, TagType.PLAYER, reason);
            }

            Player p = (Player) damager;
            LivingEntity enemy = (LivingEntity) damaged;
            TagType type = damaged instanceof Player ? TagType.PLAYER : TagType.MOB;
            TagReason reason = TagReason.ATTACKER;
            CombatUtil.tag(p, enemy, type, reason);
        }
    }
}