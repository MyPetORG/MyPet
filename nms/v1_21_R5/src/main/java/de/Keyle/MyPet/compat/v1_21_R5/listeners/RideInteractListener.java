package de.Keyle.MyPet.compat.v1_21_R5.listeners;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import de.Keyle.MyPet.compat.v1_21_R5.entity.CraftMyPet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;

/**
 * 1.21_R5-specific riding compatibility:
 * - On 1.21+, Mojang has made changes to lead logic. We handle riding here using NMS startRiding.
 * - Kept entirely within the v1_21_R5 module to avoid legacy API issues on 1.8.8.
 */
public class RideInteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRideWithConfiguredItem(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getRightClicked() instanceof MyPetBukkitEntity)) {
            return;
        }
        // Only consider main-hand interactions
        try {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                return;
            }
        } catch (NoSuchMethodError ignored) {
            // Not applicable on legacy, but this class is only loaded on 1.21_R5
        }

        final Player player = event.getPlayer();
        final MyPetBukkitEntity petEntity = (MyPetBukkitEntity) event.getRightClicked();

        if (!isOwner(player, petEntity)) {
            return;
        }

        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }

        MyPet myPet = petEntity.getMyPet();
        if (myPet == null || !petEntity.canMove() || !myPet.getSkills().isActive(Ride.class)) {
            return;
        }
        if (!Permissions.hasExtended(player, "MyPet.extended.ride")) {
            myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner()), 2000);
            return;
        }

        // Use NMS directly for riding in this version module
        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            Entity nmsPet = ((CraftMyPet) petEntity).getHandle();
            if (!petEntity.getPassengers().contains(player)) {
                boolean mounted = nmsPlayer.startRiding(nmsPet, true);
                if (mounted) {
                    event.setCancelled(true);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonitorRideFinisher(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof MyPetBukkitEntity)) {
            return;
        }
        try {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                return;
            }
        } catch (NoSuchMethodError ignored) {
        }
        final Player player = event.getPlayer();
        final MyPetBukkitEntity petEntity = (MyPetBukkitEntity) event.getRightClicked();
        if (!isOwner(player, petEntity)) {
            return;
        }
        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (event.isCancelled() && !petEntity.getPassengers().contains(player)) {
            event.setCancelled(false);
        }
    }

    private static boolean isOwner(Player player, MyPetBukkitEntity petEntity) {
        MyPet apiPet = petEntity.getMyPet();
        return apiPet != null && apiPet.getOwner() != null && apiPet.getOwner().getPlayer() != null
                && apiPet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }
}