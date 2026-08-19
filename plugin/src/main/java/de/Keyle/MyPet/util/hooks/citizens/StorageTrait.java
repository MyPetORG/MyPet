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

package de.Keyle.MyPet.util.hooks.citizens;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.NpcStorageConfirmContext;
import de.Keyle.MyPet.gui.context.PetSelectionContext;
import de.Keyle.MyPet.util.hooks.CitizensHook;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.Keyle.MyPet.api.MyPetGlobal.Misc;

public class StorageTrait extends Trait {

    public StorageTrait() {
        super("mypet-storage");
    }

    @EventHandler
    public void onRightClick(final NPCRightClickEvent npcEvent) {
        if (this.npc != npcEvent.getNPC()) {
            return;
        }

        final Player player = npcEvent.getClicker();

        if (!Permissions.has(player, "MyPet.npc.storage")) {
            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            return;
        }


        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            assert myPetPlayer != null;
            // Primary pet only: the Citizens storage NPC operates on one pet per
            // interaction. Phase 2 gives the flow explicit selection -- MyPetORG/MyPet#1435.
            if (myPetPlayer.hasPet()) {

                final NPC npc = this.npc;

                MyPetPlugin.getInstance().getRepository().getPets(myPetPlayer).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        WorldGroup wg = WorldGroup.getGroupByWorld(myPetPlayer.getPlayer().getWorld().getName());
                        int inactivePetCount = 0;
                        UUID activePetUUID = myPetPlayer.getPet().getUUID();

                        for (StoredPet storedPet : pets) {
                            if (activePetUUID.equals(storedPet.getUUID()) || (!storedPet.getWorldGroup().isEmpty() && !storedPet.getWorldGroup().equals(wg.getName()))) {
                                continue;
                            }
                            inactivePetCount++;
                        }

                        int maxPetCount = 0;
                        if (!Permissions.has(player, AdminPermissions.PETSTORAGE_LIMIT_ALL)) {
                            for (int i = Misc.MAX_STORED_PET_COUNT.get(); i > 0; i--) {
                                if (Permissions.has(player, "MyPet.petstorage.limit." + i)) {
                                    maxPetCount = i;
                                    break;
                                }
                            }
                        } else {
                            maxPetCount = Misc.MAX_STORED_PET_COUNT.get();
                        }

                        if (inactivePetCount == 0 && maxPetCount == 0) {
                            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
                            return;
                        }

                        if (inactivePetCount >= maxPetCount) {
                            UUID activePetUUID2 = myPetPlayer.getPet().getUUID();
                            List<StoredPet> selectablePets = pets.stream()
                                    .filter(p -> !p.getWorldGroup().isEmpty() && p.getWorldGroup().equals(wg.getName()))
                                    .filter(p -> !activePetUUID2.equals(p.getUUID()))
                                    .collect(Collectors.toList());

                            MyPetApi.getGuiService().openMenu(
                                    player,
                                    (MenuId<PetSelectionContext>) (MenuId<?>) MenuIds.PET_SELECTION,
                                    new PetSelectionContext(player,
                                            () -> CompletableFuture.completedFuture(selectablePets),
                                            storedPet -> {
                                                MyPetApi.getPetManager().deactivatePet(myPetPlayer, myPetPlayer.getPet(), true);
                                                Optional<Pet> activePet = MyPetApi.getPetManager().activatePet(storedPet);
                                                if (activePet.isPresent() && myPetPlayer.isOnline()) {
                                                    Player p = myPetPlayer.getPlayer();
                                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Npc.ChosenPet", player, activePet.get().getDisplayName()));
                                                    WorldGroup activeWg = WorldGroup.getGroupByWorld(p.getWorld().getName());
                                                    myPetPlayer.setPetForWorldGroup(activeWg, activePet.get().getUUID());

                                                    switch (activePet.get().createEntity()) {
                                                        case Canceled:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", player, activePet.get().getDisplayName()));
                                                            break;
                                                        case NoSpace:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", player, activePet.get().getDisplayName()));
                                                            break;
                                                        case NotAllowed:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", player, activePet.get().getDisplayName()));
                                                            break;
                                                        case Dead:
                                                            if (MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
                                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", myPetPlayer, activePet.get().getDisplayName()));
                                                            } else {
                                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", myPetPlayer, activePet.get().getDisplayName(), activePet.get().getRespawnTime()));
                                                            }
                                                            break;
                                                        case Spectator:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Spectator", myPetPlayer, activePet.get().getDisplayName()));
                                                            break;
                                                    }
                                                }
                                            }));
                        } else {
                            Pet currentPet = myPetPlayer.getPet();
                            MyPetApi.getGuiService().openMenu(
                                    player,
                                    (MenuId<NpcStorageConfirmContext>) (MenuId<?>) MenuIds.NPC_STORAGE_CONFIRM,
                                    new NpcStorageConfirmContext(player, currentPet, () -> {
                                        if (!myPetPlayer.hasPet()) {
                                            return;
                                        }
                                        boolean store = true;
                                        double costs = calculateStorageCosts(myPetPlayer.getPet());
                                        if (MyPetApi.getHookHelper().isEconomyEnabled() && costs > 0 && npc.hasTrait(WalletTrait.class)) {
                                            WalletTrait walletTrait = npc.getTrait(WalletTrait.class);
                                            if (!MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, costs)) {
                                                player.sendMessage(Locale.getFormattedComponent("Message.No.Money", myPetPlayer, myPetPlayer.getPet().getDisplayName(), npcEvent.getNPC().getName()));
                                                store = false;
                                            }
                                            if (store && MyPetApi.getHookHelper().getEconomy().pay(myPetPlayer, costs)) {
                                                walletTrait.deposit(costs);
                                            } else {
                                                store = false;
                                            }
                                        }

                                        if (store) {
                                            Pet activePet = myPetPlayer.getPet();
                                            StoredPet storedPet = activePet;
                                            if (MyPetApi.getPetManager().deactivatePet(myPetPlayer, activePet, true)) {
                                                String wg1 = myPetPlayer.getWorldGroupForPet(storedPet.getUUID());
                                                myPetPlayer.setPetForWorldGroup(wg1, null);
                                                MyPetPlugin.getInstance().getRepository().updateMyPetPlayer(myPetPlayer);
                                                player.sendMessage(Locale.getFormattedComponent("Message.Npc.HandOver", myPetPlayer, storedPet.getDisplayName(), npcEvent.getNPC().getName()));
                                            }
                                        }
                                    }));
                        }
                }, null));
            } else {
                MyPetPlugin.getInstance().getRepository().getPets(myPetPlayer).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        if (!pets.isEmpty()) {
                            String takeWg = WorldGroup.getGroupByWorld(myPetPlayer.getPlayer().getWorld().getName()).getName();
                            List<StoredPet> takePets = pets.stream()
                                    .filter(p -> !p.getWorldGroup().isEmpty() && p.getWorldGroup().equals(takeWg))
                                    .collect(Collectors.toList());
                            MyPetApi.getGuiService().openMenu(
                                    player,
                                    (MenuId<PetSelectionContext>) (MenuId<?>) MenuIds.PET_SELECTION,
                                    new PetSelectionContext(player,
                                            () -> CompletableFuture.completedFuture(takePets),
                                            storedPet -> {
                                                Optional<Pet> pet = MyPetApi.getPetManager().activatePet(storedPet);
                                                if (pet.isPresent()) {
                                                    Player ownerPlayer = myPetPlayer.getPlayer();
                                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Npc.ChosenPet", myPetPlayer, pet.get().getDisplayName()));
                                                    WorldGroup petWg = WorldGroup.getGroupByWorld(ownerPlayer.getWorld().getName());
                                                    myPetPlayer.setPetForWorldGroup(petWg, pet.get().getUUID());
                                                    MyPetPlugin.getInstance().getRepository().updateMyPetPlayer(myPetPlayer);

                                                    switch (pet.get().createEntity()) {
                                                        case Canceled:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", myPetPlayer, pet.get().getDisplayName()));
                                                            break;
                                                        case NoSpace:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", myPetPlayer, pet.get().getDisplayName()));
                                                            break;
                                                        case NotAllowed:
                                                            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", myPetPlayer, pet.get().getDisplayName()));
                                                            break;
                                                        case Dead:
                                                            if (MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
                                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", myPetPlayer, pet.get().getDisplayName()));
                                                            } else {
                                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", myPetPlayer, pet.get().getDisplayName(), pet.get().getRespawnTime()));
                                                            }
                                                            break;
                                                    }
                                                }
                                            }));
                        } else {
                            myPetPlayer.sendMessage(Locale.getComponent("Message.No.HasPet", myPetPlayer), 5000);
                        }
                }, null));
            }
            return;
        }
        player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
    }

    public double calculateStorageCosts(Pet pet) {
        return CitizensHook.NPC_STORAGE_COSTS_FIXED + (pet.getExperience().getLevel() * CitizensHook.NPC_STORAGE_COSTS_FACTOR);
    }
}
