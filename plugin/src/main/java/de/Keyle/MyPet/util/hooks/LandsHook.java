/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.role.enums.RoleSetting;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;

@PluginHookName("Lands")
public class LandsHook implements PlayerVersusEntityHook, PlayerVersusPlayerHook {

    private LandsIntegration landsAddon;
    private String key;

    @Override
    public boolean onEnable() {
        landsAddon = new LandsIntegration(MyPetApi.getPlugin(), false);
        key = landsAddon.initialize();
        return key != null;
    }

    @Override
    public void onDisable() {
        landsAddon.disable(key);
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        if (!(defender instanceof Animals) && !isPassiveMob(defender)) {
            return true;
        }
        try {
            Land land = landsAddon.getLand(defender.getLocation());
            if (land == null) {
                return true;
            }
            return land.canSetting(attacker.getUniqueId(), RoleSetting.ATTACK_ANIMAL);
        } catch (Throwable t) {
            return ProtectionHookErrors.failClosed(LandsHook.class, t);
        }
    }

    /**
     * Lands protects passive mobs with the attack-animals setting. Villagers, wandering traders
     * and iron/snow golems are not {@link Animals} in the Bukkit API.
     */
    private static boolean isPassiveMob(Entity entity) {
        return entity instanceof NPC || entity instanceof IronGolem || entity instanceof Snowman;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Land land = landsAddon.getLand(defender.getLocation());
            if (land == null) {
                return true;
            }
            return land.canSetting(attacker.getUniqueId(), RoleSetting.ATTACK_PLAYER);
        } catch (Throwable t) {
            return ProtectionHookErrors.failClosed(LandsHook.class, t);
        }
    }
}