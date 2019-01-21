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

import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.DataStore;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.UUID;

@PluginHookName("GriefPreventionPlus")
public class GriefPreventionPlusHook implements PlayerVersusEntityHook, PlayerVersusPlayerHook {

    protected GriefPreventionPlus griefPrevention;

    @Override
    public boolean onEnable() {
        griefPrevention = GriefPreventionPlus.getInstance();
        return griefPrevention != null;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            if (!griefPrevention.claimsEnabledForWorld(defender.getWorld())) {
                return true;
            }

            DataStore dataStore = griefPrevention.getDataStore();

            if (!(defender instanceof Monster) && griefPrevention.config.claims_protectCreatures) {
                if (defender instanceof Tameable && !griefPrevention.config.pvp_enabledWorlds.contains(defender.getWorld().getUID())) {
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

                if (griefPrevention.config.pvp_protectFreshSpawns) {
                    if (defenderData.pvpImmune || attackerData.pvpImmune) {
                        return false;
                    }
                }

                if (griefPrevention.config.pvp_noCombatInPlayerLandClaims || griefPrevention.config.pvp_noCombatInAdminLandClaims) {
                    final Claim attackerClaim = dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                    if (attackerClaim != null) {
                        if (attackerClaim.isAdminClaim() && (attackerClaim.getParent() == null) && griefPrevention.config.pvp_noCombatInAdminLandClaims) {
                            return false;
                        }
                        if (attackerClaim.isAdminClaim() && (attackerClaim.getParent() != null) && griefPrevention.config.pvp_noCombatInAdminSubdivisions) {
                            return false;
                        }
                        if (!attackerClaim.isAdminClaim() && griefPrevention.config.pvp_noCombatInPlayerLandClaims) {
                            return false;
                        }
                    }

                    final Claim defenderClaim = dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if (defenderClaim != null) {
                        if (defenderClaim.isAdminClaim() && (defenderClaim.getParent() == null) && griefPrevention.config.pvp_noCombatInAdminLandClaims) {
                            return false;
                        }
                        if (defenderClaim.isAdminClaim() && (defenderClaim.getParent() != null) && griefPrevention.config.pvp_noCombatInAdminSubdivisions) {
                            return false;
                        }
                        if (!defenderClaim.isAdminClaim() && griefPrevention.config.pvp_noCombatInPlayerLandClaims) {
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