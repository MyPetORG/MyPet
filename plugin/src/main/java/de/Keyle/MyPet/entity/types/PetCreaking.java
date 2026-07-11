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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.util.translation.PetDefaultNameResolver;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.event.PetCreateEvent;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.types.LeashEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creaking;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.RESIN_CLUMP}, leashFlags = {"HeartLinked"}, flySpeed = 0.8811D)
public class PetCreaking extends PetImpl {

    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "Creaking",
            ActivationSuppressor::startForPet,
            ActivationSuppressor::stopForPet
    );

    public static final Supplier<Listener> HEART_LISTENER =
            PetListenerRegistry.register(HeartListener::new);

    public PetCreaking(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Bukkit listener for Creaking-specific pet flows. Registered only on 1.21.4+
     * (gated in {@code PetListeners}) so direct {@code Creaking} references are
     * safe on older versions that don't ship the class.
     *
     * <p>Responsibilities:
     * <ul>
     *   <li><b>Heart-link capture</b> — in vanilla Minecraft, heart-linked
     *       Creaking are invulnerable to damage and can only be killed by
     *       destroying their linked Creaking Heart block. This listener
     *       intercepts {@link BlockBreakEvent} on a Creaking Heart and converts
     *       the linked Creaking into a Pet instead of letting vanilla kill it.</li>
     *   <li><b>Capture-helper interaction hint</b> — when a player with the
     *       capture helper active right-clicks a Creaking Heart, report whether
     *       they meet all leash requirements for the heart-based capture.</li>
     *   <li><b>Creaking allies team membership</b> — forward {@link PlayerJoinEvent}
     *       to {@link ActivationSuppressor#onPlayerJoin} so newly-joined
     *       players are added to the shared scoreboard team while any Creaking
     *       pet is active (the team makes pet Creakings treat every player as
     *       allied, preventing the vanilla stare-freeze + autonomous attack
     *       behaviours). See {@link ActivationSuppressor} for details.</li>
     * </ul>
     */
    public static final class HeartListener implements Listener {

        // 32 blocks matches vanilla Creaking heart link range
        private static final int SEARCH_RADIUS = 32;

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onCreakingHeartBreak(BlockBreakEvent event) {
            Block block = event.getBlock();

            // Check if the broken block is a Creaking Heart
            if (block.getType() != Material.CREAKING_HEART) {
                return;
            }

            Player player = event.getPlayer();

            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                return;
            }

            // Find the linked Creaking entity
            LivingEntity linkedCreaking = findLinkedCreaking(block);
            if (linkedCreaking == null) {
                return;
            }

            // Player already has an active pet
            if (MyPetApi.getPetManager().hasActivePet(player)) {
                return;
            }

            PetType petType = PetType.byEntityTypeName(linkedCreaking.getType().name());

            // Only allow heart-based capture if HeartLinked is a configured leash requirement
            if (!isHeartLinkedRequired(petType)) {
                return;
            }

            ConfigItem neededLeashItem = MyPetApi.getPetInfo().getLeashItem(petType);

            // Check permission
            if (!Permissions.has(player, "MyPet.leash." + petType.name())) {
                return;
            }

            // Check leash item in main hand
            ItemStack leashItem = player.getInventory().getItemInMainHand();
            if (!neededLeashItem.compare(leashItem)) {
                return;
            }

            // Run LeashHook checks
            for (LeashHook hook : MyPetApi.getServiceManager().getServices(LeashHook.class)) {
                if (!hook.canLeash(player, linkedCreaking)) {
                    return;
                }
            }

            // Run LeashFlag checks with damage=0 (heart-based capture)
            boolean willBeLeashed = true;
            MyPetPlayer myPetPlayer = null;
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            }

            for (Settings flagSettings : MyPetApi.getPetInfo().getLeashFlagSettings(petType)) {
                String flagName = flagSettings.getName();
                LeashFlag flag = MyPetApi.getLeashFlagManager().getLeashFlag(flagName);
                if (flag == null) {
                    MyPetApi.getLogger().warning("\"" + flagName + "\" is not a valid leash requirement!");
                    continue;
                }
                // Use damage=0 to indicate heart-based capture
                if (!flag.check(player, linkedCreaking, 0, flagSettings)) {
                    willBeLeashed = false;
                    if (myPetPlayer != null && myPetPlayer.isCaptureHelperActive()) {
                        Component message = flag.getMissingMessage(player, linkedCreaking, 0, flagSettings);
                        if (message != null) {
                            myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(false).append(message), 10000);
                        }
                    }
                } else {
                    if (myPetPlayer != null && myPetPlayer.isCaptureHelperActive()) {
                        Component message = flag.getMissingMessage(player, linkedCreaking, 0, flagSettings);
                        if (message != null) {
                            myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(true).append(message), 10000);
                        }
                    }
                }
            }

            if (!willBeLeashed) {
                return;
            }

            // Cancel the event and handle block break manually to prevent race conditions
            // This ensures the entity is removed before the heart breaks (preventing vanilla death effects)
            event.setCancelled(true);

            // Create the pet
            final MyPetPlayer owner;
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            } else {
                owner = MyPetApi.getPlayerManager().registerMyPetPlayer(player);
            }

            WorldGroup worldGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
            CompoundBinaryTag snapshot = PetEntitySnapshot.capture((Mob) linkedCreaking);

            final PersistedPet inactivePet = PersistedPet.builder(owner)
                    .petType(petType)
                    .petName(PetDefaultNameResolver.resolve(petType, owner))
                    .worldGroup(worldGroup.getName())
                    .info(snapshot)
                    .build();
            inactivePet.getOwner().setPetForWorldGroup(worldGroup, inactivePet.getUUID());

            // Store the location before removing
            final Location capturedEntityLocation = linkedCreaking.getLocation().clone();

            // Run LeashEntityHook.prepare() and remove entity before breaking heart
            // to prevent vanilla death effects
            for (LeashEntityHook hook : MyPetApi.getServiceManager().getServices(LeashEntityHook.class)) {
                hook.prepare(linkedCreaking);
            }
            linkedCreaking.remove();

            // Now break the heart block (we cancelled the event earlier)
            // In Creative mode, just remove the block without drops (vanilla behavior)
            if (player.getGameMode() == GameMode.CREATIVE) {
                block.setType(Material.AIR);
            } else {
                block.breakNaturally(leashItem);
            }

            // Consume leash item
            if (MyPetGlobal.Misc.CONSUME_LEASH_ITEM.get() && player.getGameMode() != GameMode.CREATIVE && leashItem != null) {
                if (leashItem.getAmount() > 1) {
                    leashItem.setAmount(leashItem.getAmount() - 1);
                } else {
                    player.getEquipment().setItemInMainHand(null);
                }
            }

            // Fire events
            PetCreateEvent createEvent = new PetCreateEvent(inactivePet, PetCreateEvent.Source.LEASH);
            Bukkit.getServer().getPluginManager().callEvent(createEvent);

            PetSaveEvent saveEvent = new PetSaveEvent(inactivePet);
            Bukkit.getServer().getPluginManager().callEvent(saveEvent);

            // Save and activate
            MyPetPlugin.getInstance().getRepository().addPet(inactivePet).thenAccept(value -> {
                player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                    if (value == null || !value) {
                        MyPetApi.getLogger().warning("Failed to save captured Creaking pet for " + owner.getName());
                        return;
                    }

                    owner.sendMessage(Locale.getComponent("Message.Leash.Add", owner));

                    Optional<Pet> activePet = MyPetApi.getPetManager().activatePet(inactivePet);
                    activePet.ifPresent(pet -> pet.createEntity(capturedEntityLocation));
                    if (owner.isCaptureHelperActive()) {
                        owner.setCaptureHelperActive(false);
                        owner.sendMessage(Locale.getFormattedComponent("Message.Command.CaptureHelper.Mode", owner, Locale.getComponent("Name.Disabled", owner)));
                    }
                }, null);
            }).exceptionally(err -> {
                MyPetApi.getLogger().warning("Failed to save captured Creaking pet: " + err);
                return null;
            });
        }

        /**
         * Shows capture requirements when a player with capture helper interacts with a Creaking Heart.
         */
        @EventHandler(priority = EventPriority.MONITOR)
        public void onCreakingHeartInteract(PlayerInteractEvent event) {
            // Only handle right-click on block
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.CREAKING_HEART) {
                return;
            }

            Player player = event.getPlayer();

            // Check if capture helper is active
            if (!MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                return;
            }
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            if (!myPetPlayer.isCaptureHelperActive()) {
                return;
            }

            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                return;
            }

            // Find the linked Creaking entity
            LivingEntity linkedCreaking = findLinkedCreaking(block);
            if (linkedCreaking == null) {
                myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(false).append(Component.text("No Creaking linked to this heart")), 2000);
                return;
            }

            PetType petType = PetType.byName("Creaking");

            // Only show heart-based capture info if HeartLinked is a configured leash requirement
            if (!isHeartLinkedRequired(petType)) {
                return;
            }

            // Check permission
            if (!Permissions.has(player, "MyPet.leash." + petType.name())) {
                myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(false).append(Locale.getComponent("Message.No.Allowed", player)), 2000);
                return;
            }

            // Check leash item
            ConfigItem neededLeashItem = MyPetApi.getPetInfo().getLeashItem(petType);
            ItemStack leashItem = player.getInventory().getItemInMainHand();
            String itemName = neededLeashItem.getItem().getType().name().toLowerCase().replace("_", " ");
            if (!neededLeashItem.compare(leashItem)) {
                myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(false).append(Component.text("Hold a " + itemName)), 2000);
            } else {
                myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(true).append(Component.text("Holding " + itemName)), 2000);
            }

            // Run LeashHook checks
            for (LeashHook hook : MyPetApi.getServiceManager().getServices(LeashHook.class)) {
                if (!hook.canLeash(player, linkedCreaking)) {
                    return;
                }
            }

            // Show LeashFlag status
            for (Settings flagSettings : MyPetApi.getPetInfo().getLeashFlagSettings(petType)) {
                String flagName = flagSettings.getName();
                LeashFlag flag = MyPetApi.getLeashFlagManager().getLeashFlag(flagName);
                if (flag == null) {
                    continue;
                }
                // Use damage=0 to indicate heart-based capture check
                boolean passed = flag.check(player, linkedCreaking, 0, flagSettings);
                Component message = flag.getMissingMessage(player, linkedCreaking, 0, flagSettings);
                if (message != null) {
                    myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(passed).append(message), 2000);
                }
            }

            // Player already has an active pet
            if (MyPetApi.getPetManager().hasActivePet(player)) {
                myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(false).append(Locale.getComponent("Message.Command.CaptureHelper.HasPet", player)), 2000);
            }
        }

        /**
         * Adds the joining player to the Creaking allies team if any Creaking pet
         * is currently active. See {@link ActivationSuppressor} for why
         * every online player is added to the team.
         */
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            ActivationSuppressor.onPlayerJoin(event.getPlayer());
        }

        /**
         * Checks if the HeartLinked leash flag is configured as a requirement for the given pet type.
         */
        private boolean isHeartLinkedRequired(PetType petType) {
            for (Settings flagSettings : MyPetApi.getPetInfo().getLeashFlagSettings(petType)) {
                if ("HeartLinked".equals(flagSettings.getName())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Gets the home location of a Creaking entity.
         *
         * @param entity The entity to get the home location from
         * @return The home Location, or null if not a Creaking or no home set
         */
        private static Location getCreakingHome(Entity entity) {
            if (entity instanceof Creaking) {
                return ((Creaking) entity).getHome();
            }
            return null;
        }

        /**
         * Finds a Creaking entity whose home position matches the given block location.
         *
         * @param heartBlock The Creaking Heart block that was destroyed
         * @return The linked Creaking entity, or null if not found
         */
        private LivingEntity findLinkedCreaking(Block heartBlock) {
            Location heartLocation = heartBlock.getLocation();

            for (Entity entity : heartBlock.getWorld().getNearbyEntities(heartLocation, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
                // Check entity type
                if (entity.getType() != EntityType.CREAKING) {
                    continue;
                }

                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                Location homePos = getCreakingHome(entity);
                if (homePos != null &&
                    homePos.getWorld() != null &&
                    homePos.getWorld().equals(heartLocation.getWorld()) &&
                    homePos.getBlockX() == heartLocation.getBlockX() &&
                    homePos.getBlockY() == heartLocation.getBlockY() &&
                    homePos.getBlockZ() == heartLocation.getBlockZ()) {
                    return (LivingEntity) entity;
                }
            }
            return null;
        }
    }

    /**
     * Prevents a Creaking pet from freezing or autonomously attacking players via
     * a shared scoreboard team ({@value #TEAM_NAME}).
     *
     * <p>Every active Creaking pet and every online player is added to a single
     * shared team. {@link org.bukkit.entity.LivingEntity#isAlliedTo} and
     * {@link Mob#canAttack} both resolve team membership as "allied", so inside
     * {@code Creaking#checkCanMove()} every player fails the
     * {@code canAttack(p) && !isAlliedTo(p)} filter — {@code hasPotentialTarget}
     * stays false, the freeze branch is never entered, and the brain's
     * {@code StartAttacking} behaviour can never acquire a player target.
     *
     * <p>MyPet's own combat pipeline is unaffected: {@code PetAggressiveTargetGoal}
     * selects targets via {@code HookHelper.canHurt(...)} (not teams), and
     * {@code PetMeleeAttackGoal#applyPetDamage} applies damage via
     * {@code target.damage(amount, damageSource)} which bypasses team friendly-fire
     * checks. Aggressive mode still attacks non-owner players as designed.
     *
     * <h2>Folia safety</h2>
     *
     * <p>Scoreboard mutations run on {@link Bukkit#getGlobalRegionScheduler()}
     * and operate exclusively on string entries (pet UUID string, player name)
     * captured at entity-region call time — the global-region tasks never
     * dereference live entities or players.
     *
     */
    public static final class ActivationSuppressor {

        static final String TEAM_NAME = "mypet_creaking_allies";

        private static final Map<UUID, String> registrations = new ConcurrentHashMap<>();
        private static final AtomicInteger activePetCount = new AtomicInteger(0);

        private ActivationSuppressor() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Creaking)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID petKey = pet.getUUID();
            String mobUuidEntry = mob.getUniqueId().toString();
            stopForPet(pet);

            registrations.put(petKey, mobUuidEntry);
            activePetCount.incrementAndGet();

            // Scoreboard mutations must run on the global region on Folia.
            // hasEntry is a local lookup; addEntry on an existing member
            // broadcasts remove+add team packets to every client.
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                Team team = ensureTeam();
                if (team == null) return;
                if (!team.hasEntry(mobUuidEntry)) {
                    team.addEntry(mobUuidEntry);
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!team.hasEntry(p.getName())) {
                        team.addEntry(p.getName());
                    }
                }
            });
        }

        public static void stopForPet(Pet pet) {
            UUID petKey = pet.getUUID();
            String mobUuidEntry = registrations.remove(petKey);
            if (mobUuidEntry == null) return;

            activePetCount.decrementAndGet();

            Plugin plugin = MyPetApi.getPlugin();
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Team team = scoreboard.getTeam(TEAM_NAME);
                if (team == null) return;
                team.removeEntry(mobUuidEntry);
                // The main scoreboard is persisted — without cleanup the team's player
                // entries grow without bound across the server lifetime and every
                // joining client receives the full list. Re-read the count here: a
                // spawn may have raced in since the decrement above, and its add-task
                // (queued after this one) rebuilds the team via ensureTeam anyway.
                if (activePetCount.get() <= 0) {
                    team.unregister();
                }
            });
        }

        /**
         * Adds a freshly-joined player to the allies team if at least one Creaking
         * pet is currently active. Called from {@link HeartListener}'s
         * {@code PlayerJoinEvent} handler.
         */
        public static void onPlayerJoin(Player player) {
            if (activePetCount.get() <= 0) return;

            String name = player.getName();
            Plugin plugin = MyPetApi.getPlugin();
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Team team = scoreboard.getTeam(TEAM_NAME);
                if (team == null) return;
                if (!team.hasEntry(name)) {
                    team.addEntry(name);
                }
            });
        }

        private static Team ensureTeam() {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) {
                team = scoreboard.registerNewTeam(TEAM_NAME);
                // Owner->pet damage still needs to work for the dismiss/damage flows.
                team.setAllowFriendlyFire(true);
                team.setCanSeeFriendlyInvisibles(false);
            }
            return team;
        }
    }
}
