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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.event.MyPetCreateEvent;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.types.LeashEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EntityConverterService;
import de.Keyle.MyPet.entity.InactiveMyPet;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creaking;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Listener for capturing heart-linked Creaking entities when their Creaking Heart block is destroyed.
 * <p>
 * In vanilla Minecraft, heart-linked Creaking are invulnerable to damage and can only be killed
 * by destroying their linked Creaking Heart block. This listener enables MyPet to capture these
 * Creaking by intercepting the heart destruction and converting the linked entity into a pet.
 */
public class CreakingHeartListener implements Listener {

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
        if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
            return;
        }

        MyPetType petType = MyPetType.byEntityTypeName(linkedCreaking.getType().name());
        ConfigItem neededLeashItem = MyPetApi.getMyPetInfo().getLeashItem(petType);

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
        for (LeashHook hook : MyPetApi.getPluginHookManager().getHooks(LeashHook.class)) {
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

        for (Settings flagSettings : MyPetApi.getMyPetInfo().getLeashFlagSettings(petType)) {
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
                    String message = flag.getMissingMessage(player, linkedCreaking, 0, flagSettings);
                    if (message != null) {
                        myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(false) + message, 10000);
                    }
                }
            } else {
                if (myPetPlayer != null && myPetPlayer.isCaptureHelperActive()) {
                    String message = flag.getMissingMessage(player, linkedCreaking, 0, flagSettings);
                    if (message != null) {
                        myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(true) + message, 10000);
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

        final InactiveMyPet inactiveMyPet = new InactiveMyPet(owner);
        inactiveMyPet.setPetType(petType);
        inactiveMyPet.setPetName(Translation.getString("Name." + petType.name(), inactiveMyPet.getOwner()));

        WorldGroup worldGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
        inactiveMyPet.setWorldGroup(worldGroup.getName());
        inactiveMyPet.getOwner().setMyPetForWorldGroup(worldGroup, inactiveMyPet.getUUID());

        Optional<EntityConverterService> converter = MyPetApi.getServiceManager().getService(EntityConverterService.class);
        converter.ifPresent(service -> inactiveMyPet.setInfo(service.convertEntity(linkedCreaking)));

        // Store the location before removing
        final Location capturedEntityLocation = linkedCreaking.getLocation().clone();

        // Run LeashEntityHook.prepare() and remove entity before breaking heart
        // to prevent vanilla death effects
        for (LeashEntityHook hook : MyPetApi.getPluginHookManager().getHooks(LeashEntityHook.class)) {
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
        if (Configuration.Misc.CONSUME_LEASH_ITEM && player.getGameMode() != GameMode.CREATIVE && leashItem != null) {
            if (leashItem.getAmount() > 1) {
                leashItem.setAmount(leashItem.getAmount() - 1);
            } else {
                player.getEquipment().setItemInMainHand(null);
            }
        }

        // Fire events
        MyPetCreateEvent createEvent = new MyPetCreateEvent(inactiveMyPet, MyPetCreateEvent.Source.Leash);
        Bukkit.getServer().getPluginManager().callEvent(createEvent);

        MyPetSaveEvent saveEvent = new MyPetSaveEvent(inactiveMyPet);
        Bukkit.getServer().getPluginManager().callEvent(saveEvent);

        // Save and activate
        MyPetApi.getPlugin().getRepository().addMyPet(inactiveMyPet, new RepositoryCallback<Boolean>() {
            @Override
            public void callback(Boolean value) {
                if (value == null || !value) {
                    MyPetApi.getLogger().warning("Failed to save captured Creaking pet for " + owner.getName());
                    return;
                }

                owner.sendMessage(Translation.getString("Message.Leash.Add", owner));

                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(inactiveMyPet);
                myPet.ifPresent(pet -> pet.createEntity(capturedEntityLocation));
                if (owner.isCaptureHelperActive()) {
                    owner.setCaptureHelperActive(false);
                    owner.sendMessage(Util.formatText(Translation.getString("Message.Command.CaptureHelper.Mode", owner), Translation.getString("Name.Disabled", owner)));
                }
            }
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
            myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(false) + "No Creaking linked to this heart", 2000);
            return;
        }

        MyPetType petType = MyPetType.Creaking;

        // Check permission
        if (!Permissions.has(player, "MyPet.leash." + petType.name())) {
            myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(false) + Translation.getString("Message.No.Allowed", player), 2000);
            return;
        }

        // Check leash item
        ConfigItem neededLeashItem = MyPetApi.getMyPetInfo().getLeashItem(petType);
        ItemStack leashItem = player.getInventory().getItemInMainHand();
        String itemName = neededLeashItem.getItem().getType().name().toLowerCase().replace("_", " ");
        if (!neededLeashItem.compare(leashItem)) {
            myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(false) + "Hold a " + itemName, 2000);
        } else {
            myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(true) + "Holding " + itemName, 2000);
        }

        // Run LeashHook checks
        for (LeashHook hook : MyPetApi.getPluginHookManager().getHooks(LeashHook.class)) {
            if (!hook.canLeash(player, linkedCreaking)) {
                return;
            }
        }

        // Show LeashFlag status
        for (Settings flagSettings : MyPetApi.getMyPetInfo().getLeashFlagSettings(petType)) {
            String flagName = flagSettings.getName();
            LeashFlag flag = MyPetApi.getLeashFlagManager().getLeashFlag(flagName);
            if (flag == null) {
                continue;
            }
            // Use damage=0 to indicate heart-based capture check
            boolean passed = flag.check(player, linkedCreaking, 0, flagSettings);
            String message = flag.getMissingMessage(player, linkedCreaking, 0, flagSettings);
            if (message != null) {
                myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(passed) + message, 2000);
            }
        }

        // Player already has an active pet
        if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
            myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(false) + Translation.getString("Message.Command.CaptureHelper.HasPet", player), 2000);
        }
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
