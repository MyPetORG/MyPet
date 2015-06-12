/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import com.ancientshores.AncientRPG.API.ApiManager;
import com.ancientshores.AncientRPG.Guild.AncientRPGGuild;
import com.ancientshores.AncientRPG.Party.AncientRPGParty;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.garbagemule.MobArena.MobArenaHandler;
import com.gmail.nossr50.api.PartyAPI;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.massivecraft.factions.engine.EngineMain;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import me.NoChance.PvPManager.PvPManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.jzx7.regios.RegiosPlugin;
import net.jzx7.regiosapi.RegiosAPI;
import net.jzx7.regiosapi.regions.Region;
import net.slipcor.pvparena.api.PVPArenaAPI;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.GameManager;

public class PvPChecker {
    public static boolean USE_Towny = true;
    public static boolean USE_Factions = true;
    public static boolean USE_WorldGuard = true;
    public static boolean USE_Citizens = true;
    public static boolean USE_Heroes = true;
    public static boolean USE_Regios = true;
    public static boolean USE_MobArena = true;
    public static boolean USE_McMMO = true;
    public static boolean USE_Residence = true;
    public static boolean USE_AncientRPG = true;
    public static boolean USE_GriefPrevention = true;
    public static boolean USE_PvPArena = true;
    public static boolean USE_PvPManager = true;
    public static boolean USE_SurvivalGame = true;

    private static MobArenaHandler pluginMobArena = null;

    public static boolean canHurt(Player attacker, Player defender) {
        if (Configuration.DISABLE_PET_VS_PLAYER) {
            return false;
        }
        if (attacker != null && defender != null && attacker != defender) {
            return canHurtMcMMO(attacker, defender) && canHurtFactions(attacker, defender) && canHurtTowny(attacker, defender) && canHurtHeroes(attacker, defender) && canHurtAncientRPG(attacker, defender) && canHurtGriefPrevention(attacker, defender) && canHurtPvPArena(attacker, defender) && canHurtPvPManager(attacker, defender) && canHurt(defender);
        }
        return false;
    }

    public static boolean canHurt(Player defender) {
        if (Configuration.DISABLE_PET_VS_PLAYER) {
            return false;
        }
        if (defender != null) {
            return canHurtMobArena(defender) && canHurtResidence(defender.getLocation()) && canHurtRegios(defender) && canHurtCitizens(defender) && canHurtWorldGuard(defender.getLocation()) && canHurtSurvivalGame(defender) && defender.getGameMode() != GameMode.CREATIVE && defender.getLocation().getWorld().getPVP();
        }
        return false;
    }

    public static boolean canHurtCitizens(Entity defender) {
        if (USE_Citizens && PluginHookManager.isPluginUsable("Citizens")) {
            try {
                if (CitizensAPI.getNPCRegistry().isNPC(defender)) {
                    NPC npc = CitizensAPI.getNPCRegistry().getNPC(defender);
                    if (npc == null || npc.data() == null) {
                        return true;
                    }
                    return !npc.data().get("protected", true);
                }
            } catch (Error e) {
                USE_Citizens = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtWorldGuard(Location location) {
        if (USE_WorldGuard && PluginHookManager.isPluginUsable("WorldGuard")) {
            try {
                WorldGuardPlugin wgp = PluginHookManager.getPluginInstance(WorldGuardPlugin.class);
                if (wgp.getDescription().getVersion().startsWith("6")) {
                    RegionManager mgr = wgp.getRegionManager(location.getWorld());
                    ApplicableRegionSet set = mgr.getApplicableRegions(location);
                    StateFlag.State s = set.queryState(null, DefaultFlag.PVP);
                    return s == null || s == StateFlag.State.ALLOW;
                } else {
                    RegionManager mgr = wgp.getGlobalRegionManager().get(location.getWorld());
                    ApplicableRegionSet set = mgr.getApplicableRegions(location);
                    return set.allows(DefaultFlag.PVP);
                }
            } catch (Error e) {
                USE_WorldGuard = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtFactions(Player attacker, Player defender) {
        if (USE_Factions && PluginHookManager.isPluginUsable("Factions")) {
            try {
                EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0.);
                return EngineMain.get().canCombatDamageHappen(sub, false);
            } catch (Error e) {
                USE_Factions = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtTowny(Player attacker, Player defender) {
        if (USE_Towny && PluginHookManager.isPluginUsable("Towny")) {
            try {
                if (CombatUtil.preventDamageCall(PluginHookManager.getPluginInstance(Towny.class), attacker, defender)) {
                    return false;
                }
            } catch (Error e) {
                USE_Towny = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtHeroes(Player attacker, Player defender) {
        if (USE_Heroes && PluginHookManager.isPluginUsable("Heroes")) {
            try {
                Heroes pluginHeroes = PluginHookManager.getPluginInstance(Heroes.class);
                Hero heroAttacker = pluginHeroes.getCharacterManager().getHero(attacker);
                Hero heroDefender = pluginHeroes.getCharacterManager().getHero(defender);
                int attackerLevel = heroAttacker.getTieredLevel(false);
                int defenderLevel = heroDefender.getTieredLevel(false);

                if (Math.abs(attackerLevel - defenderLevel) > Heroes.properties.pvpLevelRange) {
                    return false;
                }
                if ((defenderLevel < Heroes.properties.minPvpLevel) || (attackerLevel < Heroes.properties.minPvpLevel)) {
                    return false;
                }
                HeroParty party = heroDefender.getParty();
                if ((party != null) && (party.isNoPvp()) && party.isPartyMember(heroAttacker)) {
                    return false;
                }
            } catch (Error e) {
                USE_Heroes = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtRegios(Player defender) {
        if (USE_Regios && PluginHookManager.isPluginUsable("Regios")) {
            try {
                RegiosAPI pluginRegios = PluginHookManager.getPluginInstance(RegiosPlugin.class);
                for (Region region : pluginRegios.getRegions(defender.getLocation())) {
                    if (!region.isPvp()) {
                        return false;
                    }
                }
                return pluginRegios.getRegion(defender).isPvp();
            } catch (Error e) {
                USE_Regios = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtResidence(Location location) {
        if (USE_Residence && PluginHookManager.isPluginUsable("Residence")) {
            try {
                FlagPermissions flagPermissions = Residence.getPermsByLoc(location);
                return flagPermissions.has("pvp", true);
            } catch (Error e) {
                USE_Residence = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtMobArena(Player defender) {
        if (USE_MobArena && PluginHookManager.isPluginUsable("MobArena")) {
            try {
                if (pluginMobArena == null) {
                    pluginMobArena = new MobArenaHandler();
                }
                if (pluginMobArena.isPlayerInArena(defender)) {
                    return pluginMobArena.getArenaWithPlayer(defender).getSettings().getBoolean("pvp-enabled", true);
                }
            } catch (Error e) {
                USE_MobArena = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtSurvivalGame(Player defender) {
        if (USE_SurvivalGame && PluginHookManager.isPluginUsable("SurvivalGames")) {
            try {
                int gameid = GameManager.getInstance().getPlayerGameId(defender);
                if (gameid == -1) {
                    return true;
                }
                if (!GameManager.getInstance().isPlayerActive(defender)) {
                    return true;
                }
                Game game = GameManager.getInstance().getGame(gameid);
                if (game.getMode() != Game.GameMode.INGAME) {
                    return false;
                }
                if (game.isProtectionOn()) {
                    return false;
                }
            } catch (Error e) {
                USE_SurvivalGame = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtPvPArena(Player attacker, Player defender) {
        if (USE_PvPArena && PluginHookManager.isPluginUsable("pvparena")) {
            try {
                if (!PVPArenaAPI.getArenaName(defender).equals("")) {
                    if (PVPArenaAPI.getArenaName(attacker).equals(PVPArenaAPI.getArenaName(defender))) {
                        return PVPArenaAPI.getArenaTeam(attacker) != PVPArenaAPI.getArenaTeam(defender);
                    }
                }
            } catch (Error e) {
                USE_PvPArena = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtMcMMO(Player attacker, Player defender) {
        if (USE_McMMO && PluginHookManager.isPluginUsable("mcMMO")) {
            try {
                return !PartyAPI.inSameParty(attacker, defender);
            } catch (Error e) {
                USE_McMMO = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtAncientRPG(Player attacker, Player defender) {
        if (USE_AncientRPG && PluginHookManager.isPluginUsable("SurvivalGames")) {
            try {
                AncientRPGParty party = ApiManager.getApiManager().getPlayerParty(attacker.getUniqueId());
                if (party != null) {
                    if (!party.isFriendlyFireEnabled() && party.containsUUID(defender.getUniqueId())) {
                        return false;
                    }
                }

                AncientRPGGuild guild = ApiManager.getApiManager().getPlayerGuild(attacker.getUniqueId());
                if (guild != null) {
                    if (!guild.friendlyFire && guild == ApiManager.getApiManager().getPlayerGuild(defender.getUniqueId())) {
                        return false;
                    }
                }
            } catch (Error e) {
                USE_AncientRPG = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtGriefPrevention(Player attacker, Player defender) {
        if (USE_GriefPrevention && PluginHookManager.isPluginUsable("GriefPrevention")) {
            try {
                if (!GriefPrevention.instance.config_pvp_enabledWorlds.contains(attacker.getWorld())) {
                    return true;
                }

                GriefPrevention pluginGriefPrevention = GriefPrevention.instance;

                DataStore ds = pluginGriefPrevention.dataStore;

                PlayerData defenderData = ds.getPlayerData(defender.getUniqueId());
                PlayerData attackerData = ds.getPlayerData(attacker.getUniqueId());

                if (attacker.hasPermission("griefprevention.nopvpimmunity")) {
                    return true;
                }

                if (defenderData.pvpImmune || attackerData.pvpImmune) {
                    return false;
                }

                if (GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims || GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims) {
                    Claim attackerClaim = ds.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                    if (attackerClaim != null) {
                        if (attackerClaim.isAdminClaim()) {
                            if (attackerClaim.parent == null && (GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims || GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions)) {
                                return false;
                            }
                        } else if (GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims) {
                            return false;
                        }
                    }

                    Claim defenderClaim = ds.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if (defenderClaim != null) {
                        if (defenderClaim.isAdminClaim()) {
                            if (defenderClaim.parent == null && (GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims || GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions)) {
                                return false;
                            }
                        } else if (GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims) {
                            return false;
                        }
                    }
                }
            } catch (Error e) {
                USE_GriefPrevention = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtPvPManager(Player attacker, Player defender) {
        if (USE_PvPManager && PluginHookManager.isPluginUsable("PvPManager")) {
            try {
                PvPManager plugin = PluginHookManager.getPluginInstance(PvPManager.class);
                return plugin.getPlayerHandler().canAttack(attacker, defender);
            } catch (Error e) {
                MyPetLogger.write("Please use PvPManager build 113+");
                USE_PvPManager = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static void reset() {
        pluginMobArena = null;

        PluginHookManager.reset();
    }
}