/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util.support;

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
import com.massivecraft.factions.listeners.FactionsListenerMain;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.Keyle.MyPet.util.Configuration;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.GameManager;

import static org.bukkit.Bukkit.getPluginManager;

public class PvPChecker {
    public static boolean USE_PlayerDamageEntityEvent = false;
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
    public static boolean USE_SurvivalGame = true;

    private static MobArenaHandler pluginMobArena = null;

    public static boolean canHurt(Player attacker, Player defender) {
        if (Configuration.DISABLE_PET_VS_PLAYER) {
            return false;
        }
        if (attacker != null && defender != null) {
            return canHurtMcMMO(attacker, defender) && canHurtFactions(attacker, defender) && canHurtTowny(attacker, defender) && canHurtHeroes(attacker, defender) && canHurtAncientRPG(attacker, defender) && canHurtGriefPrevention(attacker, defender) && canHurtPvPArena(attacker, defender) && canHurtEvent(attacker, defender) && canHurt(defender);
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

    public static boolean canHurtEvent(Player attacker, LivingEntity defender) {
        if (USE_PlayerDamageEntityEvent) {
            EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, defender, DamageCause.ENTITY_ATTACK, 0.1D);
            getPluginManager().callEvent(event);
            return !event.isCancelled();
        }
        return true;
    }

    public static boolean canHurtCitizens(Entity defender) {
        if (USE_Citizens && PluginSupportManager.isPluginUsable("Citizens")) {
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
        if (USE_WorldGuard && PluginSupportManager.isPluginUsable("WorldGuard")) {
            try {
                WorldGuardPlugin wgp = PluginSupportManager.getPluginInstance(WorldGuardPlugin.class);
                RegionManager mgr = wgp.getGlobalRegionManager().get(location.getWorld());
                Vector pt = new Vector(location.getX(), location.getY(), location.getZ());
                ApplicableRegionSet set = mgr.getApplicableRegions(pt);

                return set.allows(DefaultFlag.PVP);
            } catch (Error e) {
                USE_WorldGuard = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtFactions(Player attacker, Player defender) {
        if (USE_Factions && PluginSupportManager.isPluginUsable("Factions")) {
            try {
                EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0.);
                return FactionsListenerMain.get().canCombatDamageHappen(sub, false);
            } catch (Error e) {
                USE_Factions = false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public static boolean canHurtTowny(Player attacker, Player defender) {
        if (USE_Towny && PluginSupportManager.isPluginUsable("Towny")) {
            try {
                if (CombatUtil.preventFriendlyFire(attacker, defender)) {
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
        if (USE_Heroes && PluginSupportManager.isPluginUsable("Heroes")) {
            try {
                Heroes pluginHeroes = PluginSupportManager.getPluginInstance(Heroes.class);
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
        if (USE_Regios && PluginSupportManager.isPluginUsable("Regios")) {
            try {
                RegiosAPI pluginRegios = PluginSupportManager.getPluginInstance(RegiosPlugin.class);
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
        if (USE_Residence && PluginSupportManager.isPluginUsable("Residence")) {
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
        if (USE_MobArena && PluginSupportManager.isPluginUsable("MobArena")) {
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
        if (USE_SurvivalGame && PluginSupportManager.isPluginUsable("SurvivalGames")) {
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
        if (USE_PvPArena && PluginSupportManager.isPluginUsable("pvparena")) {
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
        if (USE_McMMO && PluginSupportManager.isPluginUsable("mcMMO")) {
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
        if (USE_AncientRPG && PluginSupportManager.isPluginUsable("SurvivalGames")) {
            try {
                AncientRPGParty party = ApiManager.getApiManager().getPlayerParty(attacker);
                if (party != null) {
                    if (!party.friendlyFire && party.containsName(defender.getName())) {
                        return false;
                    }
                }

                AncientRPGGuild guild = ApiManager.getApiManager().getPlayerGuild(attacker.getName());
                if (guild != null) {
                    if (!guild.friendlyFire && guild == ApiManager.getApiManager().getPlayerGuild(defender.getName())) {
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
        if (USE_GriefPrevention && PluginSupportManager.isPluginUsable("GriefPrevention")) {
            try {
                GriefPrevention pluginGriefPrevention = GriefPrevention.instance;

                PlayerData defenderData = pluginGriefPrevention.dataStore.getPlayerData(defender.getName());
                PlayerData attackerData = pluginGriefPrevention.dataStore.getPlayerData(attacker.getName());

                if (defenderData.pvpImmune || attackerData.pvpImmune) {
                    return false;
                }

                if (pluginGriefPrevention.getDescription().getVersion().equals("7.8")) {
                    WorldConfig worldConfig = pluginGriefPrevention.getWorldCfg(defender.getWorld());
                    DataStore dataStore = pluginGriefPrevention.dataStore;

                    if (worldConfig.getPvPNoCombatinPlayerClaims() || worldConfig.getNoPvPCombatinAdminClaims()) {
                        Claim localClaim = dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                        if (localClaim != null) {
                            if ((localClaim.isAdminClaim() && worldConfig.getNoPvPCombatinAdminClaims()) || (!localClaim.isAdminClaim() && worldConfig.getPvPNoCombatinPlayerClaims())) {
                                return false;
                            }
                        }
                    }
                    if (worldConfig.getPvPNoCombatinPlayerClaims() || worldConfig.getNoPvPCombatinAdminClaims()) {
                        Claim localClaim = dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                        if (localClaim != null) {
                            if ((localClaim.isAdminClaim() && worldConfig.getNoPvPCombatinAdminClaims()) || (!localClaim.isAdminClaim() && worldConfig.getPvPNoCombatinPlayerClaims())) {
                                return false;
                            }
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

    public static void reset() {
        pluginMobArena = null;

        PluginSupportManager.reset();
    }
}