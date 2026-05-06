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

import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers MyPet's bundled third-party plugin hook classes with the {@link PluginHookManager}.
 *
 * <p>Each hook adapts MyPet to a specific external plugin (Vault, WorldGuard, Towny, MythicMobs,
 * etc.). Registration only adds the hook class to the manager's catalog — the manager later
 * inspects {@code Bukkit.getPluginManager()} during {@link PluginHookManager#enableHooks()} and
 * activates only the hooks whose target plugin is actually installed on the server.</p>
 *
 * <p>The list is kept in alphabetical order; insertion order has no semantic effect because each
 * hook gates itself on plugin presence at activation time.</p>
 *
 * <p>Invoked once during plugin load.</p>
 */
public final class BuiltInHooks {

    private static final List<Class<? extends PluginHook>> HOOKS = List.of(
            BattleArenaHook.class,
            CitizensHook.class,
            CombatLogXHook.class,
            FabledSkyBlockHook.class,
            FactionsUUIDHook.class,
            GangsPlusHook.class,
            GriefPreventionHook.class,
            GuildsHook.class,
            HeroesHook.class,
            KingdomsHook.class,
            LandsHook.class,
            McMMOHook.class,
            MiniaturePetsHook.class,
            MobArenaHook.class,
            MythicMobsHook.class,
            NoCheatPlusHook.class,
            PlaceholderApiHook.class,
            PlotSquaredHook.class,
            PremiumVanishHook.class,
            PvPArenaHook.class,
            PvPManagerHook.class,
            RedProtectHook.class,
            ResidenceHook.class,
            SimpleClansHook.class,
            StackMobHook.class,
            SuperVanishHook.class,
            SurvivalGamesHook.class,
            TownyHook.class,
            UltimateSurvivalGamesHook.class,
            VaultHook.class,
            WorldGuardHook.class
    );

    private BuiltInHooks() {
    }

    /**
     * Registers every built-in hook class with the supplied {@link PluginHookManager}.
     * Hooks remain dormant until the manager later calls {@link PluginHookManager#enableHooks()}.
     *
     * @param manager the manager to populate; not retained by this class
     */
    public static void register(@NotNull PluginHookManager manager) {
        for (Class<? extends PluginHook> hook : HOOKS) {
            manager.registerHook(hook);
        }
    }
}
