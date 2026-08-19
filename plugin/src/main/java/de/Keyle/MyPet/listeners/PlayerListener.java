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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.event.PetPlayerJoinEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.repository.types.SqLiteRepository;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.BeaconImpl;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import de.Keyle.MyPet.skill.skills.ShieldImpl;
import de.Keyle.MyPet.util.Updater;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.util.CompatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    // Beacon zone state tracking (ConcurrentHashMap for thread safety)
    private final Map<UUID, BeaconZoneState> beaconZoneStates = new ConcurrentHashMap<>();

    private static final class BeaconZoneState {
        private static final double EPSILON = 0.0001;
        final boolean deny, selfDeny, shareDeny;
        final double rangeMult, durationMult;
        final int amplifierMod;

        BeaconZoneState(boolean deny, boolean selfDeny, boolean shareDeny,
                        double rangeMult, double durationMult, int amplifierMod) {
            this.deny = deny;
            this.selfDeny = selfDeny;
            this.shareDeny = shareDeny;
            this.rangeMult = rangeMult;
            this.durationMult = durationMult;
            this.amplifierMod = amplifierMod;
        }

        boolean hasModifications() {
            return deny || selfDeny || shareDeny ||
                   Math.abs(rangeMult - 1.0) > EPSILON ||
                   Math.abs(durationMult - 1.0) > EPSILON ||
                   amplifierMod != 0;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BeaconZoneState)) return false;
            BeaconZoneState other = (BeaconZoneState) o;
            return deny == other.deny && selfDeny == other.selfDeny &&
                   shareDeny == other.shareDeny &&
                   Math.abs(rangeMult - other.rangeMult) < EPSILON &&
                   Math.abs(durationMult - other.durationMult) < EPSILON &&
                   amplifierMod == other.amplifierMod;
        }

        @Override
        public int hashCode() {
            // Use rounded values for hash to be consistent with equals
            return Objects.hash(deny, selfDeny, shareDeny,
                   Math.round(rangeMult * 10000),
                   Math.round(durationMult * 10000),
                   amplifierMod);
        }
    }

    @EventHandler
    public void on(PlayerInteractEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        // Multi-Pet Phase 2 (MyPetORG/MyPet#1435): the control item steers the primary
        // Pet. Which Pet a right-click should steer has no unambiguous answer once
        // several are out, so this waits for explicit Pet selection.
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) && MyPetApi.getPetManager().hasActivePet(event.getPlayer()) && MyPetGlobal.Skilltree.Skill.CONTROL_ITEM.get().compare(event.getPlayer().getInventory().getItemInMainHand())) {
            Pet pet = MyPetApi.getPetManager().getPet(event.getPlayer());
            if (pet.getStatus() == Pet.PetState.Here && pet.getBukkitEntity() != null && pet.canMove()) {
                if (pet.getSkills().isActive(ControlImpl.class)) {
                    if (pet.getSkills().isActive(Behavior.class)) {
                        Behavior behavior = pet.getSkills().get(Behavior.class);
                        if (behavior.getBehavior() == BehaviorMode.Aggressive || behavior.getBehavior() == BehaviorMode.Farm) {
                            event.getPlayer().sendMessage(Locale.getFormattedComponent("Message.Skill.Control.AggroFarm", event.getPlayer(), pet.getDisplayName(), behavior.getBehavior().name()));
                            return;
                        }
                    }
                    if (pet.getSkills().isActive(Ride.class)) {
                        if (pet.hasPetRider()) {
                            // Suppress the "can't control while ridden" message when the
                            // clicker IS the rider — they're using Ride, not Control.
                            // Default config has CONTROL_ITEM == RIDE_ITEM == lead, so
                            // a single right-click on the pet fires both paths: the
                            // PlayerInteractEntityEvent mounts the player, then this
                            // PlayerInteractEvent (RIGHT_CLICK_AIR) sees the rider and
                            // misfires the Control-skill error.
                            Mob mob = pet.getBukkitEntity();
                            if (mob != null && mob.getPassengers().contains(event.getPlayer())) {
                                return;
                            }
                            event.getPlayer().sendMessage(Locale.getFormattedComponent("Message.Skill.Control.Ride", event.getPlayer(), pet.getDisplayName()));
                            return;
                        }
                    }
                    if (!Permissions.hasExtended(event.getPlayer(), "MyPet.extended.control")) {
                        pet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", pet.getOwner()), 10000);
                        return;
                    }
                    Block block = event.getPlayer().getTargetBlock(null, 100);
                    if (block != null && block.getType() != Material.AIR) {
                        if (!block.getType().isSolid()) {
                            block = block.getRelative(BlockFace.DOWN);
                        }
                        pet.getSkills().get(ControlImpl.class).setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(PlayerGameModeChangeEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (event.getNewGameMode().name().equals("SPECTATOR")) {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                for (Pet pet : myPetPlayerDamagee.getPets()) {
                    pet.removePet();
                }
            }
        } else {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                for (Pet pet : myPetPlayerDamagee.getPets()) {
                    if (pet.wantsToRespawn()) {
                        switch (pet.createEntity()) {
                            case Success:
                                myPetPlayerDamagee.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", myPetPlayerDamagee, pet.getDisplayName()));
                                break;
                        }
                    }
                }
            }
        }
    }

    // The un-cancel logic in the handler below is only needed on 1.20.x;
    // 1.21+ handles pet right-click interactions correctly without it.
    private static final boolean MODERN_PET_INTERACT = CompatUtil.minecraftVersionEqualsOrAbove("1.21");

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerInteractEntityEvent event) {
        if (MODERN_PET_INTERACT) {
            return;
        }
        if (event.isCancelled()) {
            if (PetEntityMarker.isMarked(event.getRightClicked())) {
                Pet clickedPet = MyPetApi.getPetManager().getPetFromEntity(event.getRightClicked());
                if (clickedPet != null && clickedPet.getOwner() != null
                        && clickedPet.getOwner().equals(event.getPlayer())) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        long delay = MyPetPlugin.getInstance().getRepository() instanceof SqLiteRepository ? 1L : MyPetGlobal.Repository.EXTERNAL_LOAD_DELAY.get();

        final Player joinPlayer = event.getPlayer();
        joinPlayer.getScheduler().runDelayed(MyPetApi.getPlugin(), delayedTask -> {
            MyPetPlugin.getInstance().getRepository().getMyPetPlayer(joinPlayer).thenAccept(p -> {
                if (p == null) return;
                joinPlayer.getScheduler().run(MyPetApi.getPlugin(), joinTask -> {
                    final MyPetPlayerImpl joinedPlayer = (MyPetPlayerImpl) p;

                    MyPetApi.getPlayerManager().setOnline(joinedPlayer);
                    Timer.startPlayerTicking(joinedPlayer);

                    final WorldGroup joinGroup = WorldGroup.getGroupByWorld(joinPlayer.getWorld().getName());
                    for (Pet pet : joinedPlayer.getPets()) {
                        if (!pet.getWorldGroup().equals(joinGroup.getName())) {
                            MyPetApi.getPetManager().deactivatePet(joinedPlayer, pet, true);
                        }
                    }

                    // hasPet() is correct here rather than a per-pet loop: this restores
                    // the group's binding only when the player joined with nothing out.
                    if (!joinedPlayer.hasPet() && joinedPlayer.hasPetInWorldGroup(joinGroup.getName())) {
                        final UUID petUUID = joinedPlayer.getPetForWorldGroup(joinGroup.getName());
                        MyPetPlugin.getInstance().getRepository().getPet(petUUID).thenAccept(storedPet -> {
                            joinPlayer.getScheduler().run(MyPetApi.getPlugin(), petTask -> {
                                // Use the pet activatePet actually returned rather than
                                // re-reading the owner's primary pet, which need not be
                                // the one just activated once a player can have several.
                                Optional<Pet> activated = MyPetApi.getPetManager().activatePet(storedPet);

                                if (activated.isPresent()) {
                                    final Pet pet = activated.get();
                                    if (pet.wantsToRespawn()) {
                                        switch (pet.createEntity()) {
                                            case Canceled:
                                                joinedPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", joinedPlayer, pet.getDisplayName()));
                                                break;
                                            case NotAllowed:
                                                joinedPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", joinedPlayer, pet.getDisplayName()));
                                                break;
                                            case Dead:
                                                if (MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
                                                    joinedPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", joinedPlayer, pet.getDisplayName()));
                                                } else {
                                                    joinedPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", joinedPlayer, pet.getDisplayName(), pet.getRespawnTime()));
                                                }
                                                break;
                                            case Flying:
                                                joinedPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", joinedPlayer, pet.getDisplayName()));
                                                break;
                                            case NoSpace:
                                                joinedPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", joinedPlayer, pet.getDisplayName()));
                                                break;
                                        }
                                    }
                                }
                            }, null);
                        });
                    }
                    joinedPlayer.checkForContribution();

                    Bukkit.getServer().getPluginManager().callEvent(new PetPlayerJoinEvent(joinedPlayer));
                }, null);
            });
        }, null, Math.max(1L, delay));

        if (MyPetGlobal.Update.SHOW_OP.get() && event.getPlayer().isOp() && Updater.isUpdateAvailable()) {
            String versionUrl = Updater.RESOURCE_PAGE_URL;
            event.getPlayer().sendMessage(Component.text()
                    .append(Locale.getFormattedComponent("Message.Update.Available", event.getPlayer()))
                    .append(Component.text(" [" + Updater.getLatest().getVersion() + "]")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.openUrl(versionUrl))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text(versionUrl).color(NamedTextColor.GRAY)
                            )))
                    .asComponent());
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            if (WorldGroup.getGroupByWorld(victim.getWorld()).isDisabled()) {
                return;
            }
            if (event.getDamager() instanceof Projectile projectile) {
                // Identify Pet-fired projectiles via the PDC owner tag that
                // PetRangedAttackGoal sets at launch. Replaces the legacy
                // `instanceof CraftMyPetProjectile` check against NMS projectile
                // subclasses that no longer exist after the Paper-goal migration.
                Pet shooterPet = de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal.getSourcePet(projectile);
                if (shooterPet != null) {
                    // Owner-protection: a pet's projectile cannot hit its own owner.
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(victim)) {
                        MyPetPlayer victimMyPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(victim);
                        // Membership, not identity against the primary pet: any of the
                        // victim's own pets shooting them must be blocked, not just
                        // whichever one happens to be first in activation order.
                        if (victimMyPetPlayer.getPets().contains(shooterPet)) {
                            event.setCancelled(true);
                        }
                    }
                    // PvP respect: shooter's owner must be allowed to hurt the victim.
                    if (!MyPetApi.getHookHelper().canHurt(shooterPet.getOwner().getPlayer(), victim, true)) {
                        event.setCancelled(true);
                    }
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Player victim) {
            if (WorldGroup.getGroupByWorld(victim.getWorld()).isDisabled()) {
                return;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(victim)) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                        victim.isInsideVehicle() &&
                        (PetEntityMarker.isMarked(victim.getVehicle()) ||
                                (victim.getVehicle().getType() == EntityType.ARMOR_STAND && victim.getVehicle().isInsideVehicle()))) {
                    event.setCancelled(true);
                    return;
                }
                MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(victim);
                for (Pet pet : myPetPlayerDamagee.getPets()) {
                    if (pet.getSkills().has(ShieldImpl.class)) {
                        ShieldImpl shield = pet.getSkills().get(ShieldImpl.class);
                        if (shield.trigger()) {
                            shield.apply(event);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerQuitEvent event) {
        // Clean up beacon zone state
        beaconZoneStates.remove(event.getPlayer().getUniqueId());

        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            for (Pet pet : player.getPets()) {
                if (pet.getStatus() == Pet.PetState.Here) {
                    pet.removePet(true);
                }
            }
            MyPetApi.getPetManager().deactivatePets(player, true);

            Timer.stopPlayerTicking(player);
            MyPetApi.getPlayerManager().setOffline(player);
        }
    }

    @EventHandler
    public void onPetPlayerChangeWorld(final PlayerChangedWorldEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }

        final WorldGroup toGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());

        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());

            final WorldGroup fromGroup = WorldGroup.getGroupByWorld(event.getFrom().getName());

            // Snapshot of the pets that were already out before the world change.
            // Used below to suppress call messages for pets that were merely
            // carried across rather than newly summoned.
            final List<Pet> petsBeforeChange = myPetPlayer.getPets();
            final Player worldChangedPlayer = event.getPlayer();
            final Runnable callPetBody = () -> {
                if (!myPetPlayer.isOnline()) {
                    return;
                }
                for (Pet runPet : myPetPlayer.getPets()) {
                    switch (runPet.createEntity()) {
                        case Canceled:
                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", myPetPlayer, runPet.getDisplayName()));
                            break;
                        case NoSpace:
                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", myPetPlayer, runPet.getDisplayName()));
                            break;
                        case NotAllowed:
                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", myPetPlayer, runPet.getDisplayName()));
                            break;
                        case Dead:
                            if (!petsBeforeChange.contains(runPet)) {
                                if (MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", runPet.getOwner(), runPet.getDisplayName()));
                                } else {
                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", runPet.getOwner(), runPet.getDisplayName(), runPet.getRespawnTime()));
                                }
                            }
                            break;
                        case Flying:
                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", myPetPlayer, runPet.getDisplayName()));
                            break;
                        case Success:
                            if (!petsBeforeChange.contains(runPet)) {
                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", myPetPlayer, runPet.getDisplayName()));
                            }
                            break;
                    }
                }
            };

            if (fromGroup != toGroup) {
                final boolean hadMyPetInFromWorld = MyPetApi.getPetManager().deactivatePets(myPetPlayer, true);

                if (toGroup.isDisabled()) {
                    return;
                }

                if (myPetPlayer.hasPetInWorldGroup(toGroup)) {
                    final UUID groupMyPetUUID = myPetPlayer.getPetForWorldGroup(toGroup);
                    MyPetPlugin.getInstance().getRepository().getPets(myPetPlayer).thenAccept(pets -> {
                        worldChangedPlayer.getScheduler().run(MyPetApi.getPlugin(), runTask -> {
                            for (StoredPet storedPet : pets) {
                                if (storedPet.getUUID().equals(groupMyPetUUID)) {
                                    MyPetApi.getPetManager().activatePet(storedPet);
                                    break;
                                }
                            }
                            List<Pet> activeInToGroup = myPetPlayer.getPets();
                            if (activeInToGroup.isEmpty()) {
                                myPetPlayer.setPetForWorldGroup(toGroup, null);
                            } else if (activeInToGroup.stream().anyMatch(Pet::wantsToRespawn)) {
                                worldChangedPlayer.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> callPetBody.run(), null, 20L);
                            }
                        }, null);
                    });
                } else if (hadMyPetInFromWorld) {
                    myPetPlayer.sendMessage(Locale.getComponent("Message.MultiWorld.NoActivePetInThisWorld", myPetPlayer));
                }
            } else if (petsBeforeChange.stream().anyMatch(Pet::wantsToRespawn)) {
                worldChangedPlayer.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> callPetBody.run(), null, 20L);
            }
        }
    }

    @EventHandler
    //This does not work for new minecraft versions (1.19+) as the player gets dismounted before the TeleportEvent is thrown. Yay.
    public void onPet(PlayerTeleportEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        Player player = event.getPlayer();
        if ((player.isInsideVehicle() && PetEntityMarker.isMarked(player.getVehicle())) ||
                (player.isInsideVehicle() && player.getVehicle().isInsideVehicle() && PetEntityMarker.isMarked(player.getVehicle().getVehicle()))) {
            if (player.getLocation().getWorld() != event.getTo().getWorld() || event.getFrom().distance(event.getTo()) > 10) {
                if (MyPetGlobal.Skilltree.Skill.Ride.PREVENT_TELEPORTATION.get()) {
                    event.setCancelled(true);
                    player.sendMessage(Locale.getComponent("Message.Skill.Ride.NoTeleport", player));
                    return;
                }
                player.getVehicle().eject();
            }
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            if (event.getFrom().getWorld() != event.getTo().getWorld() || event.getFrom().distance(event.getTo()) > 10) {
                final boolean sameWorld = event.getFrom().getWorld() == event.getTo().getWorld();
                // Despawn the pets currently in the world, and re-summon exactly those.
                // Iterating getPets() in the delayed task instead would also summon pets
                // the player deliberately sent away, since createEntity() spawns any pet
                // whose status is not Here and whose respawn timer has elapsed.
                final List<Pet> despawnedByTeleport = new ArrayList<>();
                for (Pet pet : myPetPlayer.getPets()) {
                    if (pet.getStatus() == Pet.PetState.Here) {
                        pet.removePet();
                        despawnedByTeleport.add(pet);
                    }
                }
                if (!despawnedByTeleport.isEmpty()) {
                        player.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> {
                            if (!myPetPlayer.isOnline()) {
                                return;
                            }
                            for (Pet runPet : despawnedByTeleport) {
                                switch (runPet.createEntity()) {
                                    case Canceled:
                                        myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", myPetPlayer, runPet.getDisplayName()));
                                        break;
                                    case NoSpace:
                                        if (sameWorld) {
                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", myPetPlayer, runPet.getDisplayName()));
                                        }
                                        break;
                                    case Flying:
                                        if (sameWorld) {
                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", myPetPlayer, runPet.getDisplayName()));
                                        }
                                        break;
                                    case NotAllowed:
                                        myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", myPetPlayer, runPet.getDisplayName()));
                                        break;
                                    case SourceUnavailable:
                                        myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.SourceUnavailable", myPetPlayer, runPet.getDisplayName()));
                                        break;
                                }
                            }
                        }, null, 20L);
                }
            }
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                // Collect first, then ask the hooks. isPetAllowed walks every registered
                // AllowedHook (WorldGuard region query, Factions, Residence, arenas), and
                // PlayerMoveEvent fires on every block boundary for every player -- so the
                // cheap check has to come first or players with no pet out pay for it too.
                List<Pet> spawnedPets = new ArrayList<>();
                for (Pet pet : player.getPets()) {
                    if (pet.getStatus() == Pet.PetState.Here) {
                        spawnedPets.add(pet);
                    }
                }
                if (!spawnedPets.isEmpty() && !MyPetApi.getHookHelper().isPetAllowed(player)) {
                    for (Pet pet : spawnedPets) {
                        pet.removePet(true);
                    }
                    player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player.getPlayer()));
                }

                // Track beacon zone state changes
                checkBeaconZoneState(event.getPlayer(), player, event.getTo());
            }
        }
    }

    private void checkBeaconZoneState(Player player, MyPetPlayer mpPlayer, Location to) {
        // Only process if zone messages are enabled
        if (!MyPetGlobal.Skilltree.Skill.Beacon.ZONE_MESSAGES.get()) {
            return;
        }

        // Only process if the player has at least one spawned pet with the beacon skill
        boolean hasSpawnedBeaconPet = false;
        for (Pet pet : mpPlayer.getPets()) {
            if (pet.getStatus() == Pet.PetState.Here && pet.getSkills().has(BeaconImpl.class)) {
                hasSpawnedBeaconPet = true;
                break;
            }
        }
        if (!hasSpawnedBeaconPet) {
            return;
        }

        // Build current zone state (immutable)
        BeaconZoneState current = new BeaconZoneState(
            !MyPetApi.getHookHelper().isBeaconAllowed(to),
            !MyPetApi.getHookHelper().isBeaconSelfAllowed(to),
            !MyPetApi.getHookHelper().isBeaconShareAllowed(to),
            MyPetApi.getHookHelper().getBeaconRangeMultiplier(to),
            MyPetApi.getHookHelper().getBeaconDurationMultiplier(to),
            MyPetApi.getHookHelper().getBeaconAmplifierModifier(to)
        );

        BeaconZoneState previous = beaconZoneStates.get(player.getUniqueId());

        if (previous == null || !current.equals(previous)) {
            beaconZoneStates.put(player.getUniqueId(), current);

            // Don't message on first check (login/join)
            if (previous == null) {
                return;
            }

            if (!current.hasModifications() && previous.hasModifications()) {
                // Left all modifications
                player.sendMessage(Locale.getComponent("Message.Skill.Beacon.Zone.Leave", player));
            } else if (current.hasModifications()) {
                // Entered or changed zone - build multi-line message
                List<Component> lines = new ArrayList<>();
                lines.add(Locale.getComponent("Message.Skill.Beacon.Zone.Enter", player));

                if (current.deny) {
                    lines.add(Locale.getComponent("Message.Skill.Beacon.Zone.Deny", player));
                }
                if (current.selfDeny) {
                    lines.add(Locale.getComponent("Message.Skill.Beacon.Zone.SelfDeny", player));
                }
                if (current.shareDeny) {
                    lines.add(Locale.getComponent("Message.Skill.Beacon.Zone.ShareDeny", player));
                }
                if (current.rangeMult < 1.0) {
                    lines.add(Locale.getFormattedComponent(
                        "Message.Skill.Beacon.Zone.RangeReduced", player,
                        (int)(current.rangeMult * 100)));
                } else if (current.rangeMult > 1.0) {
                    lines.add(Locale.getFormattedComponent(
                        "Message.Skill.Beacon.Zone.RangeIncreased", player,
                        (int)(current.rangeMult * 100)));
                }
                if (current.durationMult < 1.0) {
                    lines.add(Locale.getFormattedComponent(
                        "Message.Skill.Beacon.Zone.DurationReduced", player,
                        (int)(current.durationMult * 100)));
                } else if (current.durationMult > 1.0) {
                    lines.add(Locale.getFormattedComponent(
                        "Message.Skill.Beacon.Zone.DurationIncreased", player,
                        (int)(current.durationMult * 100)));
                }
                if (current.amplifierMod < 0) {
                    lines.add(Locale.getFormattedComponent(
                        "Message.Skill.Beacon.Zone.AmplifierReduced", player,
                        Math.abs(current.amplifierMod)));
                } else if (current.amplifierMod > 0) {
                    lines.add(Locale.getFormattedComponent(
                        "Message.Skill.Beacon.Zone.AmplifierIncreased", player,
                        current.amplifierMod));
                }

                for (Component line : lines) {
                    player.sendMessage(line);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void on(final PlayerDeathEvent event) {
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getEntity())) {
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getEntity());
            for (Pet pet : myPetPlayer.getPets()) {
                if (pet.getStatus() == Pet.PetState.Here && MyPetGlobal.Skilltree.Skill.Backpack.DROP_WHEN_OWNER_DIES.get()) {
                    if (pet.getSkills().isActive(BackpackImpl.class)) {
                        pet.getSkills().get(BackpackImpl.class).dropContents(pet.getLocation().get());
                    }
                }
                pet.removePet();
            }
        }
    }

    @EventHandler
    public void on(final PlayerRespawnEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            final MyPetPlayer respawnedMyPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            // Snapshot before the delayed task so "already out" can be distinguished
            // from "summoned by this respawn" when deciding which messages to send.
            final List<Pet> petsAtRespawn = respawnedMyPetPlayer.getPets();

            if (petsAtRespawn.stream().anyMatch(Pet::wantsToRespawn)) {
                event.getPlayer().getScheduler().runDelayed(MyPetApi.getPlugin(), t -> {
                    for (Pet runPet : respawnedMyPetPlayer.getPets()) {
                        switch (runPet.createEntity()) {
                            case Canceled:
                                respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", respawnedMyPetPlayer, runPet.getDisplayName()));
                                break;
                            case NoSpace:
                                respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", respawnedMyPetPlayer, runPet.getDisplayName()));
                                break;
                            case NotAllowed:
                                respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", respawnedMyPetPlayer, runPet.getDisplayName()));
                                break;
                            case Dead:
                                if (!petsAtRespawn.contains(runPet)) {
                                    if (MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
                                        respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", respawnedMyPetPlayer, runPet.getDisplayName()));
                                    } else {
                                        respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", respawnedMyPetPlayer, runPet.getDisplayName(), runPet.getRespawnTime()));
                                    }
                                }
                                break;
                            case Flying:
                                respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", respawnedMyPetPlayer, runPet.getDisplayName()));
                                break;
                            case Success:
                                if (!petsAtRespawn.contains(runPet)) {
                                    respawnedMyPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", respawnedMyPetPlayer, runPet.getDisplayName()));
                                }
                                break;
                        }
                    }
                }, null, 25L);
            }
        }
    }

}
