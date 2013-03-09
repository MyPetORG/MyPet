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
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.jzx7.regiosapi.RegiosAPI;
import net.jzx7.regiosapi.regions.Region;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class MyPetPvP
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

    private static boolean searchedCitizens = false;
    private static boolean searchedWorldGuard = false;
    private static boolean searchedFactions = false;
    private static boolean searchedTowny = false;
    private static boolean searchedHeroes = false;
    private static boolean searchedRegios = false;
    private static boolean searchedResidence = false;
    private static boolean searchedMobArena = false;
    private static boolean searchedMcMMO = false;
    private static boolean searchedAncientRPG = false;

    private static boolean pluginCitizens = false;
    private static boolean pluginFactions = false;
    private static boolean pluginTowny = false;
    private static boolean pluginMcMMO = false;
    private static boolean pluginResidence = false;
    private static WorldGuardPlugin pluginWorldGuard = null;
    private static Heroes pluginHeroes = null;
    private static RegiosAPI pluginRegios = null;
    private static MobArenaHandler pluginMobArena = null;
    private static ApiManager pluginAncientRPG = null;

    public static boolean canHurt(Player attacker, Player defender)
    {
        if (attacker != null && defender != null)
        {
            return canHurtMcMMO(attacker, defender) && canHurtFactions(attacker, defender) && canHurtTowny(attacker, defender) && canHurtHeroes(attacker, defender) && canHurtAncientRPG(attacker, defender) && canHurt(defender);
        }
        return false;
    }

    public static boolean canHurt(Player defender)
    {
        if (defender != null)
        {
            return canHurtMobArena(defender) && canHurtResidence(defender.getLocation()) && canHurtRegios(defender) && canHurtCitizens(defender) && canHurtWorldGuard(defender.getLocation()) && defender.getGameMode() != GameMode.CREATIVE && defender.getLocation().getWorld().getPVP();
        }
        return false;
    }

    public static boolean canHurtCitizens(Player defender)
    {
        if (!searchedCitizens)
        {
            searchedCitizens = true;
            pluginCitizens = MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("Citizens");
        }
        if (USE_Citizens && pluginCitizens)
        {
            if (defender.hasMetadata("NPC"))
            {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(defender);
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
            if (MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("WorldGuard"))
            {
                pluginWorldGuard = (WorldGuardPlugin) MyPetBukkitUtil.getServer().getPluginManager().getPlugin("WorldGuard");
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
            pluginFactions = MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("Factions");
        }
        if (USE_Factions && pluginFactions)
        {
            EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, 0);
            return P.p.entityListener.canDamagerHurtDamagee(sub, false);
        }
        return true;
    }

    public static boolean canHurtTowny(Player attacker, Player defender)
    {
        if (!searchedTowny)
        {
            searchedTowny = true;
            pluginTowny = MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("Towny");
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
            if (MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("Heroes"))
            {
                pluginHeroes = (Heroes) MyPetBukkitUtil.getServer().getPluginManager().getPlugin("Heroes");
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
            if (MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("Regios"))
            {
                pluginRegios = (RegiosAPI) MyPetBukkitUtil.getServer().getPluginManager().getPlugin("Regios");
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
            pluginResidence = MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("Residence");
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
            if (MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("MobArena"))
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

    public static boolean canHurtMcMMO(Player attacker, Player defender)
    {
        if (!searchedMcMMO)
        {
            searchedMcMMO = true;
            pluginMcMMO = MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("mcMMO");
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
            if (MyPetBukkitUtil.getServer().getPluginManager().isPluginEnabled("AncientRPG"))
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

    public static void reset()
    {
        searchedCitizens = false;
        searchedHeroes = false;
        searchedTowny = false;
        searchedFactions = false;
        searchedWorldGuard = false;
        searchedRegios = false;
        searchedResidence = false;
        searchedMobArena = false;
        searchedMcMMO = false;
        searchedAncientRPG = false;

        pluginFactions = false;
        pluginCitizens = false;
        pluginTowny = false;
        pluginMcMMO = false;
        pluginResidence = false;
        pluginWorldGuard = null;
        pluginHeroes = null;
        pluginRegios = null;
        pluginMobArena = null;
        pluginAncientRPG = null;
    }
}
