package de.Keyle.MyPet.listeners;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suppresses the {@code minecraft:end/kill_dragon} ("Free the End")
 * advancement when a player kills an EnderDragon pet.
 *
 * <p>Vanilla's kill-trigger fires on any {@code ender_dragon} entity death,
 * regardless of whether it's the boss, a custom-summoned dragon, or a pet.
 * The trigger fires inside {@code LivingEntity#die}, which runs <i>before</i>
 * {@link org.bukkit.event.entity.EntityDeathEvent} — so we can't gate from
 * the death listener; by then the advancement is already granted.
 *
 * <p>The flow is therefore:
 * <ol>
 *   <li>{@link EntityDamageByEntityEvent} fires when a player damages a
 *   pet EnderDragon. We mark the player UUID in {@link #recentPetDragonKillers}
 *   regardless of whether the hit is lethal — false positives on the
 *   advancement-grant side are harmless because the only check is
 *   "did the player damage a pet dragon recently AND are they being granted
 *   the dragon-kill criterion right now."</li>
 *
 *   <li>The marking is cleared after 5 ticks via
 *   {@code player.getScheduler().runDelayed} so unrelated future grants
 *   (a real dragon kill in the End some time later) are unaffected.</li>
 *
 *   <li>{@link PlayerAdvancementCriterionGrantEvent} cancels when the
 *   advancement key matches {@link #END_KILL_DRAGON} and the player is
 *   in the recent-killer set.</li>
 * </ol>
 *
 * <p>The {@link Configuration.MyPet.EnderDragon#GRANT_END_ADVANCEMENT_ON_KILL}
 * flag is checked at both event handlers — when {@code true}, both bail
 * early so vanilla behavior is preserved.
 */
public class PetEnderDragonAdvancementListener implements Listener {

    private static final NamespacedKey END_KILL_DRAGON = NamespacedKey.minecraft("end/kill_dragon");

    private static final Set<UUID> recentPetDragonKillers = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPetDragonDamage(EntityDamageByEntityEvent event) {
        if (Configuration.MyPet.EnderDragon.GRANT_END_ADVANCEMENT_ON_KILL) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;
        if (!PetEntityMarker.isMarked(event.getEntity())) return;

        Player killer = resolvePlayerDamager(event.getDamager());
        if (killer == null) return;

        UUID id = killer.getUniqueId();
        recentPetDragonKillers.add(id);
        killer.getScheduler().runDelayed(MyPetApi.getPlugin(),
                t -> recentPetDragonKillers.remove(id),
                () -> recentPetDragonKillers.remove(id),
                5L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        if (Configuration.MyPet.EnderDragon.GRANT_END_ADVANCEMENT_ON_KILL) return;
        if (!END_KILL_DRAGON.equals(event.getAdvancement().getKey())) return;
        if (!recentPetDragonKillers.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    private static Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }
}
