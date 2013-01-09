package de.Keyle.MyPet.util;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.garbagemule.MobArena.MobArenaHandler;
import com.gmail.nossr50.api.PartyAPI;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.massivecraft.factions.P;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
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
    public static boolean useTowny = true;
    public static boolean useFactions = true;
    public static boolean useWorldGuard = true;
    public static boolean useCitizens = true;
    public static boolean useHeroes = true;
    public static boolean useRegios = true;
    public static boolean useMobArena = true;
    public static boolean useMcMMO = true;
    public static boolean useResidence = true;

    private static boolean searchedCitizens = false;
    private static boolean searchedWorldGuard = false;
    private static boolean searchedFactions = false;
    private static boolean searchedTowny = false;
    private static boolean searchedHeroes = false;
    private static boolean searchedRegios = false;
    private static boolean searchedResidence = false;
    private static boolean searchedMobArena = false;
    private static boolean searchedMcMMO = false;

    private static boolean pluginCitizens = false;
    private static boolean pluginFactions = false;
    private static boolean pluginTowny = false;
    private static boolean pluginMcMMO = false;
    private static boolean pluginResidence = false;
    private static WorldGuardPlugin pluginWorldGuard = null;
    private static Heroes pluginHeroes = null;
    private static RegiosAPI pluginRegios = null;
    private static MobArenaHandler pluginMobArena = null;

    public static boolean canHurt(Player attacker, Player defender)
    {
        return canHurtMcMMO(attacker, defender) && canHurtFactions(attacker, defender) && canHurtTowny(attacker, defender) && canHurtHeroes(attacker, defender) && canHurt(defender);
    }

    public static boolean canHurt(Player defender)
    {
        return canHurtMobArena(defender) && canHurtResidence(defender.getLocation()) && canHurtRegios(defender) && canHurtCitizens(defender) && canHurtWorldGuard(defender.getLocation()) && defender.getGameMode() != GameMode.CREATIVE && defender.getLocation().getWorld().getPVP();
    }

    public static boolean canHurtCitizens(Player defender)
    {
        if (!searchedCitizens)
        {
            searchedCitizens = true;
            pluginCitizens = MyPetUtil.getServer().getPluginManager().isPluginEnabled("Citizens");
        }
        if (useCitizens && pluginCitizens)
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
            if (MyPetUtil.getServer().getPluginManager().isPluginEnabled("WorldGuard"))
            {
                pluginWorldGuard = (WorldGuardPlugin) MyPetUtil.getServer().getPluginManager().getPlugin("WorldGuard");
            }
        }
        if (useWorldGuard && pluginWorldGuard != null)
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
            pluginFactions = MyPetUtil.getServer().getPluginManager().isPluginEnabled("Factions");
        }
        if (useFactions && pluginFactions)
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
            pluginTowny = MyPetUtil.getServer().getPluginManager().isPluginEnabled("Towny");
        }
        if (useTowny && pluginTowny)
        {
            try
            {
                TownyWorld world = TownyUniverse.getDataSource().getWorld(defender.getWorld().getName());
                if (CombatUtil.preventDamageCall(world, attacker, defender, attacker, defender))
                {
                    return false;
                }
            }
            catch (Exception ignored)
            {
                MyPetUtil.getDebugLogger().info("Towny Exception!");
                return true;
            }
        }
        return true;
    }

    public static boolean canHurtHeroes(Player attacker, Player defender)
    {
        if (!searchedHeroes)
        {
            searchedHeroes = true;
            if (MyPetUtil.getServer().getPluginManager().isPluginEnabled("Heroes"))
            {
                pluginHeroes = (Heroes) MyPetUtil.getServer().getPluginManager().getPlugin("Heroes");
            }
        }
        if (useHeroes && MyPetUtil.getServer().getPluginManager().isPluginEnabled("Heroes"))
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
            if (MyPetUtil.getServer().getPluginManager().isPluginEnabled("Regios"))
            {
                pluginRegios = (RegiosAPI) MyPetUtil.getServer().getPluginManager().getPlugin("Regios");
            }
        }
        if (useRegios && pluginRegios != null)
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
            pluginResidence = MyPetUtil.getServer().getPluginManager().isPluginEnabled("mcMMO");
        }
        if (useResidence && pluginResidence)
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
            if (MyPetUtil.getServer().getPluginManager().isPluginEnabled("MobArena"))
            {
                pluginMobArena = new MobArenaHandler();
            }
        }
        if (useMobArena && pluginMobArena != null)
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
            pluginMcMMO = MyPetUtil.getServer().getPluginManager().isPluginEnabled("mcMMO");
        }
        if (useMcMMO && pluginMcMMO)
        {
            return !PartyAPI.inSameParty(attacker, defender);
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

        pluginFactions = false;
        pluginCitizens = false;
        pluginTowny = false;
        pluginMcMMO = false;
        pluginResidence = false;
        pluginWorldGuard = null;
        pluginHeroes = null;
        pluginRegios = null;
        pluginMobArena = null;
    }
}
