/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
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
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.GameManager;

import java.util.UUID;

public class PvPChecker {

    private static MobArenaHandler pluginMobArena = null;

    public static boolean canHurt(Player attacker, Player defender, boolean bothWays) {
        if (!canHurt(attacker, defender) || (bothWays && !canHurt(defender, attacker))) {
            return false;
        }
        return true;
    }

    public static boolean canHurt(Player attacker, Player defender) {
        if (Configuration.Misc.DISABLE_PET_VS_PLAYER) {
            return false;
        }
        if (attacker != null && defender != null && attacker != defender) {
            return canHurtMcMMO(attacker, defender) && canHurtFactions(attacker, defender) && canHurtTowny(attacker, defender) && canHurtHeroes(attacker, defender) && canHurtAncientRPG(attacker, defender) && canHurtGriefPrevention(attacker, defender) && canHurtPvPArena(attacker, defender) && canHurtPvPManager(attacker, defender) && canHurt(defender);
        }
        return false;
    }

    public static boolean canHurt(Player attacker, Entity defender) {
        if (defender instanceof Player) {
            return canHurt(attacker, (Player) defender);
        }
        if (attacker != null && defender != null && attacker != defender) {
            return canHurtTowny(attacker, defender) && canHurtGriefPrevention(attacker, defender) && canHurt(defender);
        }
        return false;
    }

    public static boolean canHurt(Entity defender) {
        if (defender instanceof Player) {
            return canHurt((Player) defender);
        }
        if (defender != null) {
            return canHurtCitizens(defender);
        }
        return false;
    }

    public static boolean canHurt(Player defender) {
        if (Configuration.Misc.DISABLE_PET_VS_PLAYER) {
            return false;
        }
        if (defender != null) {
            return canHurtMobArena(defender) && canHurtResidence(defender.getLocation()) && canHurtRegios(defender) && canHurtCitizens(defender) && canHurtWorldGuard(defender.getLocation()) && canHurtSurvivalGame(defender) && defender.getGameMode() != GameMode.CREATIVE && defender.getLocation().getWorld().getPVP();
        }
        return false;
    }

    public static boolean canHurtCitizens(Entity defender) {
        if (Configuration.Hooks.USE_Citizens && PluginHookManager.isPluginUsable("Citizens")) {
            try {
                if (CitizensAPI.getNPCRegistry().isNPC(defender)) {
                    NPC npc = CitizensAPI.getNPCRegistry().getNPC(defender);
                    if (npc == null || npc.data() == null) {
                        return true;
                    }
                    return !npc.data().get("protected", true);
                }
            } catch (Throwable e) {
                Configuration.Hooks.USE_Citizens = false;
            }
        }
        return true;
    }

    public static boolean canHurtWorldGuard(Location location) {
        if (Configuration.Hooks.USE_WorldGuard && PluginHookManager.isPluginUsable("WorldGuard")) {
            try {
                WorldGuardPlugin wgp = PluginHookManager.getPluginInstance(WorldGuardPlugin.class);
                RegionManager mgr = wgp.getRegionManager(location.getWorld());
                ApplicableRegionSet set = mgr.getApplicableRegions(location);
                StateFlag.State s = set.queryState(null, DefaultFlag.PVP);
                return s == null || s == StateFlag.State.ALLOW;
            } catch (Throwable e) {
                Configuration.Hooks.USE_WorldGuard = false;
            }
        }
        return true;
    }

    public static boolean canHurtFactions(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_Factions && PluginHookManager.isPluginUsable("Factions")) {
            try {
                EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0.);
                return EngineMain.get().canCombatDamageHappen(sub, false);
            } catch (Throwable e) {
                Configuration.Hooks.USE_Factions = false;
            }
        }
        return true;
    }

    public static boolean canHurtTowny(Player attacker, Entity defender) {
        if (Configuration.Hooks.USE_Towny && PluginHookManager.isPluginUsable("Towny")) {
            try {
                if (CombatUtil.preventDamageCall(PluginHookManager.getPluginInstance(Towny.class), attacker, defender)) {
                    return false;
                }
            } catch (Throwable e) {
                Configuration.Hooks.USE_Towny = false;
            }
        }
        return true;
    }

    public static boolean canHurtHeroes(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_Heroes && PluginHookManager.isPluginUsable("Heroes")) {
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
            } catch (Throwable e) {
                Configuration.Hooks.USE_Heroes = false;
            }
        }
        return true;
    }

    public static boolean canHurtRegios(Player defender) {
        if (Configuration.Hooks.USE_Regios && PluginHookManager.isPluginUsable("Regios")) {
            try {
                RegiosAPI pluginRegios = PluginHookManager.getPluginInstance(RegiosPlugin.class);
                for (Region region : pluginRegios.getRegions(defender.getLocation())) {
                    if (!region.isPvp()) {
                        return false;
                    }
                }
                return pluginRegios.getRegion(defender).isPvp();
            } catch (Throwable e) {
                Configuration.Hooks.USE_Regios = false;
            }
        }
        return true;
    }

    public static boolean canHurtResidence(Location location) {
        if (Configuration.Hooks.USE_Residence && PluginHookManager.isPluginUsable("Residence")) {
            try {
                FlagPermissions flagPermissions = Residence.getPermsByLoc(location);
                return flagPermissions.has("pvp", true);
            } catch (Throwable e) {
                Configuration.Hooks.USE_Residence = false;
            }
        }
        return true;
    }

    public static boolean canHurtMobArena(Player defender) {
        if (Configuration.Hooks.USE_MobArena && PluginHookManager.isPluginUsable("MobArena")) {
            try {
                if (pluginMobArena == null) {
                    pluginMobArena = new MobArenaHandler();
                }
                if (pluginMobArena.isPlayerInArena(defender)) {
                    return pluginMobArena.getArenaWithPlayer(defender).getSettings().getBoolean("pvp-enabled", true);
                }
            } catch (Throwable e) {
                Configuration.Hooks.USE_MobArena = false;
            }
        }
        return true;
    }

    public static boolean canHurtSurvivalGame(Player defender) {
        if (Configuration.Hooks.USE_SurvivalGame && PluginHookManager.isPluginUsable("SurvivalGames")) {
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
            } catch (Throwable e) {
                Configuration.Hooks.USE_SurvivalGame = false;
            }
        }
        return true;
    }

    public static boolean canHurtPvPArena(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_PvPArena && PluginHookManager.isPluginUsable("pvparena")) {
            try {
                if (!PVPArenaAPI.getArenaName(defender).equals("")) {
                    if (PVPArenaAPI.getArenaName(attacker).equals(PVPArenaAPI.getArenaName(defender))) {
                        return PVPArenaAPI.getArenaTeam(attacker) != PVPArenaAPI.getArenaTeam(defender);
                    }
                }
            } catch (Throwable e) {
                Configuration.Hooks.USE_PvPArena = false;
            }
        }
        return true;
    }

    public static boolean canHurtMcMMO(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_McMMO && PluginHookManager.isPluginUsable("mcMMO")) {
            try {
                return !PartyAPI.inSameParty(attacker, defender);
            } catch (Throwable e) {
                Configuration.Hooks.USE_McMMO = false;
            }
        }
        return true;
    }

    public static boolean canHurtAncientRPG(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_AncientRPG && PluginHookManager.isPluginUsable("SurvivalGames")) {
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
            } catch (Throwable e) {
                Configuration.Hooks.USE_AncientRPG = false;
            }
        }
        return true;
    }

    public static boolean canHurtGriefPrevention(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_GriefPrevention && PluginHookManager.isPluginUsable("GriefPrevention")) {
            try {
                if (GriefPrevention.instance.pvpRulesApply(attacker.getWorld())) {
                    if (attacker != defender) {
                        DataStore dataStore = GriefPrevention.instance.dataStore;

                        PlayerData defenderData = dataStore.getPlayerData(defender.getUniqueId());
                        PlayerData attackerData = dataStore.getPlayerData(attacker.getUniqueId());
                        if (GriefPrevention.instance.config_pvp_protectFreshSpawns) {
                            if (defenderData.pvpImmune || attackerData.pvpImmune) {
                                return false;
                            }
                        }
                        if ((GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims) || (GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims)) {
                            Claim attackerClaim = dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                            if (!attackerData.ignoreClaims) {
                                if ((attackerClaim != null) && (!attackerData.inPvpCombat())) {
                                    if (attackerClaim.isAdminClaim() && attackerClaim.parent == null && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims) {
                                        return false;
                                    }
                                    if (attackerClaim.isAdminClaim() && attackerClaim.parent != null && GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions) {
                                        return false;
                                    }
                                    if (!attackerClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims) {
                                        return false;
                                    }
                                }
                                Claim defenderClaim = dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                                if (defenderClaim != null && !defenderData.inPvpCombat()) {
                                    if (defenderClaim.isAdminClaim() && defenderClaim.parent == null && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims) {
                                        return false;
                                    }
                                    if (defenderClaim.isAdminClaim() && defenderClaim.parent != null && GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions) {
                                        return false;
                                    }
                                    if (!defenderClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                Configuration.Hooks.USE_GriefPrevention = false;
            }
        }
        return true;
    }

    public static boolean canHurtGriefPrevention(Player attacker, Entity defender) {
        if (defender instanceof Player) {
            return canHurtGriefPrevention(attacker, (Player) defender);
        }
        if (Configuration.Hooks.USE_GriefPrevention && PluginHookManager.isPluginUsable("GriefPrevention")) {
            try {
                if (!GriefPrevention.instance.claimsEnabledForWorld(defender.getWorld())) {
                    return true;
                }
                DataStore dataStore = GriefPrevention.instance.dataStore;
                if (defender.getType() == EntityType.VILLAGER) {
                    if (!GriefPrevention.instance.config_claims_protectCreatures) {
                        return true;
                    }
                    PlayerData playerData = dataStore.getPlayerData(attacker.getUniqueId());
                    Claim claim = dataStore.getClaimAt(defender.getLocation(), false, playerData.lastClaim);
                    if (claim != null) {
                        String failureReason = claim.allowBuild(attacker, Material.AIR);
                        if (failureReason != null) {
                            return false;
                        }
                    }
                }
                if ((defender instanceof Creature || defender instanceof WaterMob) && GriefPrevention.instance.config_claims_protectCreatures) {
                    if (defender instanceof Tameable) {
                        Tameable tameable = (Tameable) defender;
                        if (tameable.isTamed() && tameable.getOwner() != null) {
                            if (attacker != null) {
                                UUID ownerID = tameable.getOwner().getUniqueId();
                                if (attacker.getUniqueId().equals(ownerID)) {
                                    return true;
                                }
                                PlayerData attackerData = dataStore.getPlayerData(attacker.getUniqueId());
                                if (attackerData.ignoreClaims) {
                                    return true;
                                }
                                if (!GriefPrevention.instance.pvpRulesApply(defender.getLocation().getWorld())) {
                                    return false;
                                }
                                if (attackerData.pvpImmune) {
                                    return false;
                                }
                            }
                        }
                    }

                    PlayerData playerData = dataStore.getPlayerData(attacker.getUniqueId());
                    Claim cachedClaim = playerData.lastClaim;

                    Claim claim = dataStore.getClaimAt(defender.getLocation(), false, cachedClaim);
                    if (claim != null) {
                        if (!defender.getWorld().getPVP() || defender.getType() != EntityType.WOLF) {
                            if (claim.allowContainers(attacker) != null) {
                                return false;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                Configuration.Hooks.USE_GriefPrevention = false;
            }
        }
        return true;
    }

    public static boolean canHurtPvPManager(Player attacker, Player defender) {
        if (Configuration.Hooks.USE_PvPManager && PluginHookManager.isPluginUsable("PvPManager")) {
            try {
                PvPManager plugin = PluginHookManager.getPluginInstance(PvPManager.class);
                return plugin.getPlayerHandler().canAttack(attacker, defender);
            } catch (Throwable e) {
                MyPetApi.getLogger().warning("Please use PvPManager build 113+");
                Configuration.Hooks.USE_PvPManager = false;
            }
        }
        return true;
    }

    public static void reset() {
        pluginMobArena = null;
    }
}