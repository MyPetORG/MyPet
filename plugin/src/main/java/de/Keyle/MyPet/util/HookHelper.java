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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.types.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class HookHelper extends de.Keyle.MyPet.api.util.hooks.HookHelper {

    @Override
    public boolean canHurt(Player attacker, Player defender, boolean viceversa) {
        if (attacker == null || defender == null) {
            return false;
        }
        if (!canHurt(attacker, defender) || (viceversa && !canHurt(defender, attacker))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        if (Configuration.Misc.DISABLE_PET_VS_PLAYER) {
            return false;
        }
        if (attacker != null && defender != null && attacker != defender) {
            if (!attacker.getWorld().getPVP()) {
                return false;
            }
            if (MyPetApi.getCompatUtil().isCompatible("1.10") && defender.isInvulnerable()) {
                return false;
            }
            if (defender.getGameMode() == GameMode.CREATIVE) {
                return false;
            }
            List<PlayerVersusPlayerHook> pvpHooks = MyPetApi.getPluginHookManager().getHooks(PlayerVersusPlayerHook.class);
            for (PlayerVersusPlayerHook hook : pvpHooks) {
                if (!hook.canHurt(attacker, defender)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        if (defender instanceof Player) {
            return canHurt(attacker, (Player) defender);
        }
        if (MyPetApi.getCompatUtil().isCompatible("1.10") && defender.isInvulnerable()) {
            return false;
        }
        if (attacker != null && defender != null && attacker != defender) {
            List<PlayerVersusEntityHook> pveHooks = MyPetApi.getPluginHookManager().getHooks(PlayerVersusEntityHook.class);
            for (PlayerVersusEntityHook hook : pveHooks) {
                if (!hook.canHurt(attacker, defender)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isPetAllowed(MyPetPlayer player) {
        List<AllowedHook> allowedHooks = MyPetApi.getPluginHookManager().getHooks(AllowedHook.class);
        for (AllowedHook hook : allowedHooks) {
            if (!hook.isPetAllowed(player)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canMyPetFlyAt(Location location) {
        List<FlyHook> flyHooks = MyPetApi.getPluginHookManager().getHooks(FlyHook.class);
        for (FlyHook hook : flyHooks) {
            if (!hook.canFly(location)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isInParty(Player player) {
        List<PartyHook> partyHooks = MyPetApi.getPluginHookManager().getHooks(PartyHook.class);
        for (PartyHook hook : partyHooks) {
            if (!hook.isInParty(player)) {
                return false;
            }
        }
        return false;
    }

    @Override
    public List<Player> getPartyMembers(Player player) {
        List<PartyHook> partyHooks = MyPetApi.getPluginHookManager().getHooks(PartyHook.class);
        for (PartyHook hook : partyHooks) {
            List<Player> members = hook.getPartyMembers(player);
            if (members != null) {
                return members;
            }
        }
        return null;
    }

    @Override
    public boolean isVanished(Player player) {
        // commonly shared method of vanish plugins to mark a player as vanished
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) {
                return true;
            }
        }
        // allow hooks to vanish players too
        List<VanishedHook> vanishedHooks = MyPetApi.getPluginHookManager().getHooks(VanishedHook.class);
        for (VanishedHook hook : vanishedHooks) {
            if (hook.isVanished(player)) {
                return true;
            }
        }
        return false;
    }

    public EconomyHook getEconomy() {
        List<EconomyHook> economyHooks = MyPetApi.getPluginHookManager().getHooks(EconomyHook.class);
        return economyHooks.stream().findFirst().orElse(null);
    }

    public boolean isEconomyEnabled() {
        EconomyHook economyHook = getEconomy();
        return economyHook != null && economyHook.checkEconomy();
    }
}
