/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.event.PetActivatedEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.experience.modifier.ExperienceModifier;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.api.util.hooks.types.*;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityInteractEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ServiceName("WorldGuard")
@RequiresPlugin("WorldGuard")
@Load(Load.State.Hooks)
public class WorldGuardHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook, FlyHook, AllowedHook, MountInsideHook, BeaconHook {

    public static final StateFlag FLY_FLAG = new StateFlag("mypet-fly", false);
    public static final StateFlag DAMAGE_FLAG = new StateFlag("mypet-damage", false);
    public static final StateFlag DENY_FLAG = new StateFlag("mypet-deny", false);
    public static final StateFlag LEASH_FLAG = new StateFlag("mypet-leash", true);
    public static final DoubleFlag EXP_ADD_FLAG = new DoubleFlag("mypet-exp-add");
    public static final DoubleFlag EXP_MULT_FLAG = new DoubleFlag("mypet-exp-mult");

    // Beacon flags
    public static final StateFlag BEACON_FLAG = new StateFlag("mypet-beacon", true);
    public static final StateFlag BEACON_SHARE_FLAG = new StateFlag("mypet-beacon-share", true);
    public static final StateFlag BEACON_SELF_FLAG = new StateFlag("mypet-beacon-self", true);
    public static final DoubleFlag BEACON_RANGE_MULT_FLAG = new DoubleFlag("mypet-beacon-range-mult");
    public static final DoubleFlag BEACON_DURATION_MULT_FLAG = new DoubleFlag("mypet-beacon-duration-mult");
    public static final IntegerFlag BEACON_AMPLIFIER_ADD_FLAG = new IntegerFlag("mypet-beacon-amplifier-add");

    public static StateFlag PVP;
    public static StateFlag DAMAGE_ANIMALS;
    protected WorldGuardPlugin wgp;
    protected boolean customFlags = false;
    protected Map<String, Boolean> missingEntityTypeFixValue = new HashMap<>();

    public WorldGuardHook() {
        if (MyPetApi.getServiceManager().getConfig().getConfig().getBoolean("WorldGuard.Enabled")) {
            wgp = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");

            try {
                FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
                PVP = Flags.PVP;
                DAMAGE_ANIMALS = Flags.DAMAGE_ANIMALS;

                if (flagRegistry != null) {
                    try {
                        // Register core flags
                        registerFlag(flagRegistry, FLY_FLAG);
                        registerFlag(flagRegistry, DAMAGE_FLAG);
                        registerFlag(flagRegistry, DENY_FLAG);
                        registerFlag(flagRegistry, LEASH_FLAG);

                        // Register experience flags
                        registerFlag(flagRegistry, EXP_ADD_FLAG);
                        registerFlag(flagRegistry, EXP_MULT_FLAG);

                        // Register beacon flags
                        registerFlag(flagRegistry, BEACON_FLAG);
                        registerFlag(flagRegistry, BEACON_SHARE_FLAG);
                        registerFlag(flagRegistry, BEACON_SELF_FLAG);
                        registerFlag(flagRegistry, BEACON_RANGE_MULT_FLAG);
                        registerFlag(flagRegistry, BEACON_DURATION_MULT_FLAG);
                        registerFlag(flagRegistry, BEACON_AMPLIFIER_ADD_FLAG);

                        // The leash flag is registered in onEnable() — LeashFlagManager isn't
                        // available yet during onLoad, when this constructor runs.
                        customFlags = true;
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("Could not register WorldGuard flags: " + e.getMessage());
                    }
                }
            } catch (NoSuchMethodError e) {
                ErrorUtil.reportWarning("Third-party plugin integration failed", e);
            }
        }
    }

    private void registerFlag(FlagRegistry flagRegistry, com.sk89q.worldguard.protection.flags.Flag<?> flag) {
        try {
            flagRegistry.register(flag);
        } catch (IllegalStateException e) {
            // Flag may already be registered (e.g., server reload) - this is OK
            MyPetApi.getLogger().info("WorldGuard flag '" + flag.getName() + "' already registered or could not be registered: " + e.getMessage());
        }
    }

    @Override
    public boolean onEnable() {
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
            ErrorUtil.reportWarning("Third-party plugin integration failed", e);
        }
    }

    // Callers (canHurt/canFly/isPetAllowed/isBeacon*/exp modifier) invoke this synchronously
    // from the region that owns `loc`, or from a thread where WorldGuard region reads are safe
    // (combat target goals, for example, query a target's location from the pet's region thread).
    // WorldGuard region queries are thread-safe in-memory reads, so no region scheduling is
    // needed — and could not be added anyway, since a synchronous predicate cannot block on a
    // cross-region Folia task.
    public StateFlag.State getState(Location loc, Player player, StateFlag... flags) {
        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (rc != null) {
            return rc.createQuery().queryState(
                    BukkitAdapter.adapt(loc),
                    player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null,
                    flags);
        }
        return StateFlag.State.ALLOW;
    }

    public Collection<Double> getDoubleValue(Location loc, Player player, DoubleFlag flag) {
        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return rc.createQuery().queryAllValues(
                BukkitAdapter.adapt(loc),
                player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null,
                flag);
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

    // BeaconHook implementation

    @Override
    public boolean isBeaconAllowed(Location location) {
        if (customFlags) {
            StateFlag.State s = getState(location, null, BEACON_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        }
        return true;
    }

    @Override
    public boolean isBeaconShareAllowed(Location location) {
        if (customFlags) {
            StateFlag.State s = getState(location, null, BEACON_SHARE_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        }
        return true;
    }

    @Override
    public boolean isBeaconSelfAllowed(Location location) {
        if (customFlags) {
            StateFlag.State s = getState(location, null, BEACON_SELF_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        }
        return true;
    }

    @Override
    public double getBeaconRangeMultiplier(Location location) {
        if (customFlags) {
            Collection<Double> values = getDoubleValue(location, null, BEACON_RANGE_MULT_FLAG);
            if (!values.isEmpty()) {
                double multiplier = 1.0;
                for (double d : values) {
                    multiplier *= d;
                }
                return multiplier;
            }
        }
        return 1.0;
    }

    @Override
    public double getBeaconDurationMultiplier(Location location) {
        if (customFlags) {
            Collection<Double> values = getDoubleValue(location, null, BEACON_DURATION_MULT_FLAG);
            if (!values.isEmpty()) {
                double multiplier = 1.0;
                for (double d : values) {
                    multiplier *= d;
                }
                return multiplier;
            }
        }
        return 1.0;
    }

    @Override
    public int getBeaconAmplifierModifier(Location location) {
        if (customFlags) {
            Collection<Integer> values = getIntegerValue(location, null, BEACON_AMPLIFIER_ADD_FLAG);
            if (!values.isEmpty()) {
                int modifier = 0;
                for (int i : values) {
                    modifier += i;
                }
                return modifier;
            }
        }
        return 0;
    }

    public Collection<Integer> getIntegerValue(Location loc, Player player, IntegerFlag flag) {
        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return rc.createQuery().queryAllValues(
                BukkitAdapter.adapt(loc),
                player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null,
                flag);
    }

    @EventHandler
    public void on(PetActivatedEvent event) {
        if (customFlags) {
            event.getPet().getExperience().addModifier("WorldGuard-Region", new RegionModifier(event.getPet()));
        }
    }

    @EventHandler
    public void on(EntityInteractEvent event) {
        Entity ent = event.getEntity();
        if (PetEntityMarker.isMarked(ent)) {
            Block block = event.getBlock();
            String blockTypeName = block.getType().name();

            if (blockTypeName.contains("PRESSURE_PLATE")) {
                // Check INTERACT where the interaction happens — the pet's location.
                // On Folia this handler runs on the pet's region thread, so ent.getLocation()
                // is owned by the current region; the owner may be in a different region.
                StateFlag.State s = getState(ent.getLocation(), null, Flags.INTERACT);
                if (s == null || s == StateFlag.State.DENY) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public boolean playerCanMount(MyPetPlayer player, Entity pet) {
        //TODO implement
        return true;
    }

    public class RegionModifier extends ExperienceModifier {

        Pet pet;

        public RegionModifier(Pet pet) {
            this.pet = pet;
        }

        @Override
        public double modify(double experience, double baseExperience) {
            Mob entity = pet.getBukkitEntity();
            if (entity != null) {
                try {
                    // Query at the pet's location (owned by the current region thread during
                    // exp gain). Pass the owner only when they're on the current region thread,
                    // so WorldGuard can still evaluate region-group (-g members/nonmembers) exp
                    // flags; otherwise pass null to avoid a cross-region entity access on Folia.
                    Location location = entity.getLocation();
                    Player owner = pet.getOwner().getPlayer();
                    Player wgPlayer = owner != null && Bukkit.isOwnedByCurrentRegion(owner) ? owner : null;
                    Collection<Double> values = getDoubleValue(location, wgPlayer, EXP_ADD_FLAG);
                    for (double d : values) {
                        experience += d;
                    }
                    values = getDoubleValue(location, wgPlayer, EXP_MULT_FLAG);
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
        public Component getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
            if (this.check(player, entity, damage, settings)) {
                return Locale.getComponent("Message.Command.CaptureHelper.WorldGuard.Allowed", player);
            } else {
                return Locale.getComponent("Message.Command.CaptureHelper.WorldGuard.Denied", player);
            }
        }
    }
}