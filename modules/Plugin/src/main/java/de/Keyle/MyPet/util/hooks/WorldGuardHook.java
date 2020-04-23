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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.config.ConfigurationManager;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.event.MyPetActivatedEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.experience.modifier.ExperienceModifier;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.hooks.types.FlyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityInteractEvent;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@PluginHookName("WorldGuard")
public class WorldGuardHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook, FlyHook, AllowedHook {

    public static final StateFlag FLY_FLAG = new StateFlag("mypet-fly", false);
    public static final StateFlag DAMAGE_FLAG = new StateFlag("mypet-damage", false);
    public static final StateFlag DENY_FLAG = new StateFlag("mypet-deny", false);
    public static final StateFlag LEASH_FLAG = new StateFlag("mypet-leash", true);
    public static final DoubleFlag EXP_ADD_FLAG = new DoubleFlag("mypet-exp-add");
    public static final DoubleFlag EXP_MULT_FLAG = new DoubleFlag("mypet-exp-mult");

    public static StateFlag PVP;
    public static StateFlag DAMAGE_ANIMALS;

    protected WorldGuardPlugin wgp;
    protected boolean customFlags = false;

    protected Map<String, Boolean> missingEntityTypeFixValue = new HashMap<>();
    protected boolean is7 = false;
    protected static Method METHOD_getRegionManager = ReflectionUtil.getMethod(WorldGuardPlugin.class, "getRegionManager", World.class);
    protected static Method METHOD_getFlagRegistry = ReflectionUtil.getMethod(WorldGuardPlugin.class, "getFlagRegistry");
    protected static Method METHOD_getApplicableRegions = ReflectionUtil.getMethod(RegionManager.class, "getApplicableRegions", Location.class);

    public WorldGuardHook() {
        if (MyPetApi.getPluginHookManager().getConfig().getConfig().getBoolean("WorldGuard.Enabled")) {
            if (ReflectionUtil.getMethod(com.sk89q.worldedit.util.Location.class, "toVector") == null) {
                return;
            }

            wgp = MyPetApi.getPluginHookManager().getPluginInstance(WorldGuardPlugin.class).get();

            if (wgp.getDescription().getVersion().startsWith("7.")) {
                is7 = true;
            }

            try {
                FlagRegistry flagRegistry = null;
                if (is7) {
                    flagRegistry = WorldGuard.getInstance().getFlagRegistry();
                    PVP = Flags.PVP;
                    DAMAGE_ANIMALS = Flags.DAMAGE_ANIMALS;
                } else {
                    try {
                        flagRegistry = (FlagRegistry) METHOD_getFlagRegistry.invoke(wgp);
                    } catch (Throwable ignore) {
                    }
                    PVP = (StateFlag) ReflectionUtil
                            .getClass("com.sk89q.worldguard.protection.flags.DefaultFlag")
                            .getDeclaredField("PVP")
                            .get(null);
                    DAMAGE_ANIMALS = (StateFlag) ReflectionUtil
                            .getClass("com.sk89q.worldguard.protection.flags.DefaultFlag")
                            .getDeclaredField("DAMAGE_ANIMALS")
                            .get(null);
                }

                if (flagRegistry != null) {
                    try {
                        flagRegistry.register(FLY_FLAG);
                        flagRegistry.register(DAMAGE_FLAG);
                        flagRegistry.register(DENY_FLAG);
                        flagRegistry.register(LEASH_FLAG);

                        MyPetApi.getLeashFlagManager().registerLeashFlag(new RegionFlag());
                        customFlags = true;
                    } catch (IllegalStateException e) {
                        MyPetApi.getLogger().warning("Could not register WorldGuard flags!");
                    }
                }
            } catch (NoSuchMethodError | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onEnable() {
        if (ReflectionUtil.getMethod(com.sk89q.worldedit.util.Location.class, "toVector") == null) {
            return false;
        }
        if (customFlags) {
            Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
            MyPetApi.getLeashFlagManager().registerLeashFlag(new RegionFlag());
        }
        return customFlags;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        MyPetApi.getLeashFlagManager().removeFlag("WorldGuard");
    }

    public void fixMissingEntityType(World world, boolean apply) {
        if (is7) {
            try {
                ConfigurationManager cfg = WorldGuard.getInstance().getPlatform().getGlobalStateManager();
                com.sk89q.worldedit.world.World w = BukkitAdapter.adapt(world);
                BukkitWorldConfiguration wcfg = (BukkitWorldConfiguration) cfg.get(w);
                if (apply) {
                    if (missingEntityTypeFixValue.containsKey(world.getName())) {
                        fixMissingEntityType(world, false);
                    }
                    missingEntityTypeFixValue.put(world.getName(), wcfg.blockPluginSpawning);
                    wcfg.blockPluginSpawning = false;
                } else if (missingEntityTypeFixValue.containsKey(world.getName())) {
                    wcfg.blockPluginSpawning = missingEntityTypeFixValue.get(world.getName());
                    missingEntityTypeFixValue.remove(world.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public StateFlag.State getState(Location loc, Player player, StateFlag... flags) {
        if (is7) {
            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (rc != null) {
                return rc.createQuery().queryState(
                        BukkitAdapter.adapt(loc),
                        player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null,
                        flags);
            }
        } else {
            try {
                RegionManager mgr = (RegionManager) METHOD_getRegionManager.invoke(wgp, loc.getWorld());
                ApplicableRegionSet set = (ApplicableRegionSet) METHOD_getApplicableRegions.invoke(mgr, loc);
                return set.queryState(player != null ? wgp.wrapPlayer(player) : null, flags);
            } catch (Exception ignored) {
            }
        }
        return StateFlag.State.ALLOW;
    }

    public Collection<Double> getDoubleValue(Location loc, Player player, DoubleFlag flag) {
        if (is7) {
            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            return rc.createQuery().queryAllValues(
                    BukkitAdapter.adapt(loc),
                    player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null,
                    flag);
        } else {
            try {
                RegionManager mgr = (RegionManager) METHOD_getRegionManager.invoke(wgp, loc.getWorld());
                ApplicableRegionSet set = (ApplicableRegionSet) METHOD_getApplicableRegions.invoke(mgr, loc);
                return set.queryAllValues(player != null ? wgp.wrapPlayer(player) : null, flag);
            } catch (Exception ignored) {
                return Collections.emptyList();
            }
        }
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        if (customFlags) {
            try {
                Location location = defender.getLocation();
                StateFlag.State s;
                if (defender instanceof Animals) {
                    s = getState(location, null, DAMAGE_ANIMALS, DAMAGE_FLAG);
                } else {
                    s = getState(location, null, DAMAGE_FLAG);
                }
                return s == null || s == StateFlag.State.ALLOW;
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Location location = defender.getLocation();
            StateFlag.State s;
            if (customFlags) {
                s = getState(location, defender, PVP, DAMAGE_FLAG);
            } else {
                s = getState(location, defender, PVP);
            }
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable ignored) {
        }
        return true;
    }

    public boolean canFly(Location location) {
        if (customFlags) {
            StateFlag.State s = getState(location, null, FLY_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        }

        return true;
    }

    @Override
    public boolean isPetAllowed(MyPetPlayer player) {
        if (customFlags) {
            Player p = player.getPlayer();
            StateFlag.State s = getState(p.getLocation(), null, DENY_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        }
        return true;
    }

    @EventHandler
    public void on(MyPetActivatedEvent event) {
        if (customFlags) {
            event.getMyPet().getExperience().addModifier("WorldGuard-Region", new RegionModifier(event.getMyPet()));
        }
    }

    @EventHandler
    public void on(EntityInteractEvent event) {
        if (is7) {
            Entity ent = event.getEntity();
            if (ent instanceof MyPetBukkitEntity) {
                Block block = event.getBlock();
                switch (block.getType().name()) {
                    case "WOOD_PLATE":
                    case "STONE_PLATE":
                    case "IRON_PLATE":
                    case "GOLD_PLATE":
                    case "ACACIA_PRESSURE_PLATE":
                    case "STONE_PRESSURE_PLATE":
                    case "BIRCH_PRESSURE_PLATE":
                    case "DARK_OAK_PRESSURE_PLATE":
                    case "HEAVY_WEIGHTED_PRESSURE_PLATE":
                    case "JUNGLE_PRESSURE_PLATE":
                    case "LIGHT_WEIGHTED_PRESSURE_PLATE":
                    case "OAK_PRESSURE_PLATE":
                    case "SPRUCE_PRESSURE_PLATE":
                        Player p = ((MyPetBukkitEntity) ent).getOwner().getPlayer();
                        StateFlag.State s = getState(p.getLocation(), null, Flags.INTERACT);
                        if (s == null || s == StateFlag.State.DENY) {
                            event.setCancelled(true);
                        }
                }
            }
        }
    }

    public class RegionModifier extends ExperienceModifier {

        MyPet myPet;

        public RegionModifier(MyPet myPet) {
            this.myPet = myPet;
        }

        @Override
        public double modify(double experience, double baseExperience) {
            if (myPet.getEntity().isPresent()) {
                try {
                    Location location = myPet.getEntity().get().getLocation();
                    Collection<Double> values = getDoubleValue(location, myPet.getOwner().getPlayer(), EXP_ADD_FLAG);
                    for (double d : values) {
                        experience += d;
                    }
                    values = getDoubleValue(location, myPet.getOwner().getPlayer(), EXP_MULT_FLAG);
                    for (double d : values) {
                        experience *= d;
                    }
                } catch (Throwable ignored) {
                }
            }
            return experience;
        }
    }

    @LeashFlagName("WorldGuard")
    class RegionFlag implements LeashFlag {

        @Override
        public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
            Location location = entity.getLocation();
            StateFlag.State s = getState(location, null, LEASH_FLAG);

            return s == null || s == StateFlag.State.ALLOW;
        }

        @Override
        public String getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
            if (this.check(player, entity, damage, settings)) {
                return Translation.getString("Message.Command.CaptureHelper.WorldGuard.Allowed", player);
            } else {
                return Translation.getString("Message.Command.CaptureHelper.WorldGuard.Denied", player);
            }
        }
    }
}