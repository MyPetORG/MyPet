package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.entity.ai.attack.PetProjectileHitListener;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.util.CompatUtil;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers MyPet's bundled Bukkit event listeners with Paper's plugin manager.
 *
 * <p>Listeners are split between generic player/world bookkeeping
 * (e.g. {@link PlayerListener}, {@link WorldListener}) and pet-specific behavior
 * (e.g. {@link PetInteractionListener}, {@link PetDeathListener}). Both groups share the
 * same registration sweep — Bukkit handler dispatch is driven by {@code @EventHandler}
 * priority, not source-package.</p>
 *
 * <p><b>Order matters within an {@link org.bukkit.event.EventPriority} bucket.</b> Bukkit
 * invokes handlers in registration order when priorities are equal; the list below preserves
 * the historical ordering used before this class was extracted. New listeners should be
 * appended unless they intentionally need to interpose at a specific point.</p>
 *
 * <p>Invoked once during plugin enable, after all services have been activated.</p>
 */
public final class PetListeners {

    private static final List<Supplier<Listener>> LISTENERS = List.of(
            PlayerListener::new,
            VehicleListener::new,
            EntityListener::new,
            LevelListener::new,
            WorldListener::new,
            RideInteractListener::new,
            PetDamageTracker::new,
            PetProjectileHitListener::new,
            PetInteractionListener::new,
            PetEnvironmentListener::new,
            PetInfoOnLeashListener::new,
            PetSurvivalListener::new,
            PetXpAttributionListener::new,
            PetPvPListener::new,
            PetSkillTriggerListener::new,
            PetDeathListener::new,
            PetDespawnListener::new,
            PetDropListener::new
    );

    private PetListeners() {
    }

    /**
     * Constructs a fresh instance of each listener and registers it with the plugin's
     * {@link PluginManager}. After the unconditional listeners,
     * {@link CreakingHeartListener} is registered when running on Minecraft 1.21.4 or
     * newer (the version that introduced the Creaking Heart block).
     *
     * @param plugin the plugin to associate registrations with; events fire only while this
     *               plugin is enabled
     */
    public static void registerAll(@NotNull Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        for (Supplier<Listener> listener : LISTENERS) {
            pm.registerEvents(listener.get(), plugin);
        }
        // CreakingHeartListener registers last. Bukkit invokes handlers in registration order
        // within the same EventPriority; if a new listener handles BlockBreakEvent (HIGH),
        // PlayerInteractEvent (MONITOR), or PlayerJoinEvent (MONITOR), confirm whether it
        // should run before or after CreakingHeartListener and reorder accordingly.
        if (CompatUtil.minecraftVersionEqualsOrAbove("1.21.4")) {
            pm.registerEvents(new CreakingHeartListener(), plugin);
        }
    }
}
