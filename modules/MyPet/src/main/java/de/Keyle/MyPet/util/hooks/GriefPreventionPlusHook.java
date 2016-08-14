/*
 * This file is part of mypet-plugin_main
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-plugin_main is licensed under the GNU Lesser General Public License.
 *
 * mypet-plugin_main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-plugin_main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.util.PluginHook;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.DataStore;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.UUID;

@PluginHookName("GriefPreventionPlus")
public class GriefPreventionPlusHook extends PluginHook implements PlayerVersusEntityHook, PlayerVersusPlayerHook {

    protected GriefPreventionPlus griefPrevention;

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_GriefPrevention) {
            griefPrevention = GriefPreventionPlus.getInstance();
            return griefPrevention != null;
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            if (!GriefPreventionPlus.getInstance().claimsEnabledForWorld(defender.getWorld())) {
                return true;
            }

            DataStore dataStore = griefPrevention.getDataStore();

            if (defender instanceof Creature && GriefPreventionPlus.getInstance().config.claims_protectCreatures) {
                if (defender instanceof Tameable && !GriefPreventionPlus.getInstance().config.pvp_enabledWorlds.contains(defender.getWorld().getUID())) {
                    final Tameable tameable = (Tameable) defender;
                    if (tameable.isTamed() && (tameable.getOwner() != null)) {
                        final UUID ownerID = tameable.getOwner().getUniqueId();

                        if (attacker.getUniqueId().equals(ownerID)) {
                            return false;
                        }

                        final PlayerData attackerData = dataStore.getPlayerData(attacker.getUniqueId());
                        if (attackerData.ignoreClaims) {
                            return true;
                        }
                    }
                }

                PlayerData playerData = dataStore.getPlayerData(attacker.getUniqueId());
                Claim claim = dataStore.getClaimAt(defender.getLocation(), false, playerData.lastClaim);

                if (claim != null) {
                    if (claim.canOpenContainers(attacker) != null) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            if (attacker.hasPermission("griefprevention.nopvpimmunity")) {
                return true;
            }

            if (attacker != defender) {
                DataStore dataStore = griefPrevention.getDataStore();
                final PlayerData defenderData = dataStore.getPlayerData(defender.getUniqueId());
                final PlayerData attackerData = dataStore.getPlayerData(attacker.getUniqueId());

                if (GriefPreventionPlus.getInstance().config.pvp_protectFreshSpawns) {
                    if (defenderData.pvpImmune || attackerData.pvpImmune) {
                        return false;
                    }
                }

                if (GriefPreventionPlus.getInstance().config.pvp_noCombatInPlayerLandClaims || GriefPreventionPlus.getInstance().config.pvp_noCombatInAdminLandClaims) {
                    final Claim attackerClaim = dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                    if (attackerClaim != null) {
                        if (attackerClaim.isAdminClaim() && (attackerClaim.getParent() == null) && GriefPreventionPlus.getInstance().config.pvp_noCombatInAdminLandClaims) {
                            return false;
                        }
                        if (attackerClaim.isAdminClaim() && (attackerClaim.getParent() != null) && GriefPreventionPlus.getInstance().config.pvp_noCombatInAdminSubdivisions) {
                            return false;
                        }
                        if (!attackerClaim.isAdminClaim() && GriefPreventionPlus.getInstance().config.pvp_noCombatInPlayerLandClaims) {
                            return false;
                        }
                    }

                    final Claim defenderClaim = dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if (defenderClaim != null) {
                        if (defenderClaim.isAdminClaim() && (defenderClaim.getParent() == null) && GriefPreventionPlus.getInstance().config.pvp_noCombatInAdminLandClaims) {
                            return false;
                        }
                        if (defenderClaim.isAdminClaim() && (defenderClaim.getParent() != null) && GriefPreventionPlus.getInstance().config.pvp_noCombatInAdminSubdivisions) {
                            return false;
                        }
                        if (!defenderClaim.isAdminClaim() && GriefPreventionPlus.getInstance().config.pvp_noCombatInPlayerLandClaims) {
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}