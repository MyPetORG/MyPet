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

package de.Keyle.MyPet.util;

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
import com.massivecraft.factions.P;
import com.massivecraft.factions.listeners.FactionsListenerMain;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.jzx7.regiosapi.RegiosAPI;
import net.jzx7.regiosapi.regions.Region;
import net.slipcor.pvparena.api.PVPArenaAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.GameManager;

public class PvPChecker
{
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

    private static boolean searchedCitizens = false;
    private static boolean searchedWorldGuard = false;
    private static boolean searchedFactions = false;
    private static boolean searchedFactions2 = false;
    private static boolean searchedTowny = false;
    private static boolean searchedHeroes = false;
    private static boolean searchedRegios = false;
    private static boolean searchedResidence = false;
    private static boolean searchedMobArena = false;
    private static boolean searchedMcMMO = false;
    private static boolean searchedAncientRPG = false;
    private static boolean searchedGriefPrevention = false;
    private static boolean searchedPvPArena = false;
    private static boolean searchedSurvivalGame = false;

    private static boolean pluginCitizens = false;
    private static boolean pluginFactions = false;
    private static boolean pluginFactions2 = false;
    private static boolean pluginTowny = false;
    private static boolean pluginMcMMO = false;
    private static boolean pluginResidence = false;
    private static boolean pluginPvPArena = false;
    private static boolean pluginSurvivalGame = false;
    private static WorldGuardPlugin pluginWorldGuard = null;
    private static Heroes pluginHeroes = null;
    private static RegiosAPI pluginRegios = null;
    private static MobArenaHandler pluginMobArena = null;
    private static ApiManager pluginAncientRPG = null;
    private static GriefPrevention pluginGriefPrevention = null;

    public static boolean canHurt(Player attacker, Player defender)
    {
        if (Configuration.DISABLE_PET_VS_PLAYER)
        {
            return false;
        }
        if (attacker != null && defender != null)
        {
            return canHurtMcMMO(attacker, defender) && canHurtFactions(attacker, defender) && canHurtFactions2(attacker, defender) && canHurtTowny(attacker, defender) && canHurtHeroes(attacker, defender) && canHurtAncientRPG(attacker, defender) && canHurtGriefPrevention(attacker, defender) && canHurtPvPArena(attacker, defender) && canHurt(defender);
        }
        return false;
    }

    public static boolean canHurt(Player defender)
    {
        if (Configuration.DISABLE_PET_VS_PLAYER)
        {
            return false;
        }
        if (defender != null)
        {
            return canHurtMobArena(defender) && canHurtResidence(defender.getLocation()) && canHurtRegios(defender) && canHurtCitizens(defender) && canHurtWorldGuard(defender.getLocation()) && canHurtSurvivalGame(defender) && defender.getGameMode() != GameMode.CREATIVE && defender.getLocation().getWorld().getPVP();
        }
        return false;
    }

    public static boolean canHurtCitizens(Player defender)
    {
        if (!searchedCitizens)
        {
            searchedCitizens = true;
            pluginCitizens = Bukkit.getServer().getPluginManager().isPluginEnabled("Citizens");
        }
        if (USE_Citizens && pluginCitizens)
        {
            if (CitizensAPI.getNPCRegistry().isNPC(defender))
            {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(defender);
                if (npc == null || npc.data() == null)
                {
                    return true;
                }
                return !npc.data().get("protected", true);
            }
        }
        return true;
    }

    public static boolean canHurtWorldGuard(Location location)
    {
        if (!searchedWorldGuard)
        {
            searchedWorldGuard = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard"))
            {
                pluginWorldGuard = (WorldGuardPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
            }
        }
        if (USE_WorldGuard && pluginWorldGuard != null)
        {
            RegionManager mgr = pluginWorldGuard.getGlobalRegionManager().get(location.getWorld());
            Vector pt = new Vector(location.getX(), location.getY(), location.getZ());
            ApplicableRegionSet set = mgr.getApplicableRegions(pt);

            return set.allows(DefaultFlag.PVP);
        }
        return true;
    }

    public static boolean canHurtFactions(Player attacker, Player defender)
    {
        if (!searchedFactions)
        {
            searchedFactions = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("Factions"))
            {
                Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");
                pluginFactions = plugin.getDescription().getVersion().startsWith("1.");
            }
        }
        if (USE_Factions && pluginFactions)
        {
            EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0);
            return P.p.entityListener.canDamagerHurtDamagee(sub, false);
        }
        return true;
    }

    public static boolean canHurtFactions2(Player attacker, Player defender)
    {
        if (!searchedFactions2)
        {
            searchedFactions2 = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("Factions"))
            {
                Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");
                pluginFactions2 = plugin.getDescription().getVersion().startsWith("2.");
            }
        }
        if (USE_Factions && pluginFactions2)
        {
            EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0);
            return FactionsListenerMain.get().canCombatDamageHappen(sub, false);
        }
        return true;
    }

    public static boolean canHurtTowny(Player attacker, Player defender)
    {
        if (!searchedTowny)
        {
            searchedTowny = true;
            pluginTowny = Bukkit.getServer().getPluginManager().isPluginEnabled("Towny");
        }
        if (USE_Towny && pluginTowny)
        {
            if (CombatUtil.preventDamageCall(attacker, defender))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean canHurtHeroes(Player attacker, Player defender)
    {
        if (!searchedHeroes)
        {
            searchedHeroes = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("Heroes"))
            {
                pluginHeroes = (Heroes) Bukkit.getServer().getPluginManager().getPlugin("Heroes");
            }
        }
        if (USE_Heroes && pluginHeroes != null)
        {
            Hero heroAttacker = pluginHeroes.getCharacterManager().getHero(attacker);
            Hero heroDefender = pluginHeroes.getCharacterManager().getHero(defender);
            int attackerLevel = heroAttacker.getTieredLevel(false);
            int defenderLevel = heroDefender.getTieredLevel(false);

            if (Math.abs(attackerLevel - defenderLevel) > Heroes.properties.pvpLevelRange)
            {
                return false;
            }
            if ((defenderLevel < Heroes.properties.minPvpLevel) || (attackerLevel < Heroes.properties.minPvpLevel))
            {
                return false;
            }
            HeroParty party = heroDefender.getParty();
            if ((party != null) && (party.isNoPvp()) && party.isPartyMember(heroAttacker))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean canHurtRegios(Player defender)
    {
        if (!searchedRegios)
        {
            searchedRegios = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("Regios"))
            {
                pluginRegios = (RegiosAPI) Bukkit.getServer().getPluginManager().getPlugin("Regios");
            }
        }
        if (USE_Regios && pluginRegios != null)
        {
            for (Region region : pluginRegios.getRegions(defender.getLocation()))
            {
                if (!region.isPvp())
                {
                    return false;
                }
            }
            return pluginRegios.getRegion(defender).isPvp();
        }
        return true;
    }

    public static boolean canHurtResidence(Location location)
    {
        if (!searchedResidence)
        {
            searchedResidence = true;
            pluginResidence = Bukkit.getServer().getPluginManager().isPluginEnabled("Residence");
        }
        if (USE_Residence && pluginResidence)
        {
            FlagPermissions flagPermissions = Residence.getPermsByLoc(location);
            return flagPermissions.has("pvp", true);
        }
        return true;
    }

    public static boolean canHurtMobArena(Player defender)
    {
        if (!searchedMobArena)
        {
            searchedMobArena = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("MobArena"))
            {
                pluginMobArena = new MobArenaHandler();
            }
        }
        if (USE_MobArena && pluginMobArena != null)
        {
            if (pluginMobArena.isPlayerInArena(defender))
            {
                return pluginMobArena.getArenaWithPlayer(defender).getSettings().getBoolean("pvp-enabled", true);
            }
        }
        return true;
    }

    public static boolean canHurtSurvivalGame(Player defender)
    {
        if (!searchedSurvivalGame)
        {
            searchedSurvivalGame = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("SurvivalGames"))
            {
                pluginSurvivalGame = true;
            }
        }
        if (USE_SurvivalGame && pluginSurvivalGame)
        {
            int gameid = GameManager.getInstance().getPlayerGameId(defender);
            if (gameid == -1)
            {
                return true;
            }
            if (!GameManager.getInstance().isPlayerActive(defender))
            {
                return true;
            }
            Game game = GameManager.getInstance().getGame(gameid);
            if (game.getMode() != Game.GameMode.INGAME)
            {
                return false;
            }
            if (game.isProtectionOn())
            {
                return false;
            }
        }
        return true;
    }

    public static boolean canHurtPvPArena(Player attacker, Player defender)
    {
        if (!searchedPvPArena)
        {
            searchedPvPArena = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("pvparena"))
            {
                pluginPvPArena = true;
            }
        }
        if (USE_PvPArena && pluginPvPArena)
        {
            if (!PVPArenaAPI.getArenaName(defender).equals(""))
            {
                if (PVPArenaAPI.getArenaName(attacker).equals(PVPArenaAPI.getArenaName(defender)))
                {
                    return PVPArenaAPI.getArenaTeam(attacker) != PVPArenaAPI.getArenaTeam(defender);
                }
            }
        }
        return true;
    }

    public static boolean canHurtMcMMO(Player attacker, Player defender)
    {
        if (!searchedMcMMO)
        {
            searchedMcMMO = true;
            pluginMcMMO = Bukkit.getServer().getPluginManager().isPluginEnabled("mcMMO");
        }
        if (USE_McMMO && pluginMcMMO)
        {
            return !PartyAPI.inSameParty(attacker, defender);
        }
        return true;
    }

    public static boolean canHurtAncientRPG(Player attacker, Player defender)
    {
        if (!searchedAncientRPG)
        {
            searchedAncientRPG = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("AncientRPG"))
            {
                pluginAncientRPG = ApiManager.getApiManager();
            }
        }
        if (USE_AncientRPG && pluginAncientRPG != null)
        {
            AncientRPGParty party = pluginAncientRPG.getPlayerParty(attacker);
            if (party != null)
            {
                if (!party.friendlyFire && party.containsName(defender.getName()))
                {
                    return false;
                }
            }

            AncientRPGGuild guild = pluginAncientRPG.getPlayerGuild(attacker.getName());
            if (guild != null)
            {
                if (!guild.friendlyFire && guild == pluginAncientRPG.getPlayerGuild(defender.getName()))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean canHurtGriefPrevention(Player attacker, Player defender)
    {
        if (!searchedGriefPrevention)
        {
            searchedGriefPrevention = true;
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("GriefPrevention"))
            {
                pluginGriefPrevention = GriefPrevention.instance;
            }
        }
        if (USE_GriefPrevention && pluginGriefPrevention != null)
        {
            PlayerData defenderData = pluginGriefPrevention.dataStore.getPlayerData(defender.getName());
            PlayerData attackerData = pluginGriefPrevention.dataStore.getPlayerData(attacker.getName());

            if (defenderData.pvpImmune || attackerData.pvpImmune)
            {
                return false;
            }

            if (pluginGriefPrevention.getDescription().getVersion().equals("7.8"))
            {
                WorldConfig worldConfig = pluginGriefPrevention.getWorldCfg(defender.getWorld());
                DataStore dataStore = pluginGriefPrevention.dataStore;

                if (worldConfig.getPvPNoCombatinPlayerClaims() || worldConfig.getNoPvPCombatinAdminClaims())
                {
                    Claim localClaim = dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if (localClaim != null)
                    {
                        if ((localClaim.isAdminClaim() && worldConfig.getNoPvPCombatinAdminClaims()) || (!localClaim.isAdminClaim() && worldConfig.getPvPNoCombatinPlayerClaims()))
                        {
                            return false;
                        }
                    }
                }
                if (worldConfig.getPvPNoCombatinPlayerClaims() || worldConfig.getNoPvPCombatinAdminClaims())
                {
                    Claim localClaim = dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                    if (localClaim != null)
                    {
                        if ((localClaim.isAdminClaim() && worldConfig.getNoPvPCombatinAdminClaims()) || (!localClaim.isAdminClaim() && worldConfig.getPvPNoCombatinPlayerClaims()))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static void reset()
    {
        searchedCitizens = false;
        searchedHeroes = false;
        searchedTowny = false;
        searchedFactions = false;
        searchedFactions2 = false;
        searchedWorldGuard = false;
        searchedRegios = false;
        searchedResidence = false;
        searchedMobArena = false;
        searchedMcMMO = false;
        searchedAncientRPG = false;
        searchedGriefPrevention = false;
        searchedSurvivalGame = false;

        pluginFactions = false;
        pluginFactions2 = false;
        pluginCitizens = false;
        pluginTowny = false;
        pluginMcMMO = false;
        pluginResidence = false;
        pluginSurvivalGame = false;
        pluginWorldGuard = null;
        pluginHeroes = null;
        pluginRegios = null;
        pluginMobArena = null;
        pluginAncientRPG = null;
        pluginGriefPrevention = null;
    }
}