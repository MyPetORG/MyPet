package de.Keyle.MyPet.repository;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.MyPetManager;
import de.Keyle.MyPet.api.repository.PlayerManager;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Restores active pets for players who are already online when the plugin enables —
 * the typical {@code /reload} case where {@link org.bukkit.event.player.PlayerJoinEvent}
 * never fires for currently connected players.
 *
 * <p>Walks the same scheduling pattern the join listener would normally take, in three
 * Folia-compatible region hops:</p>
 * <ol>
 *   <li>Hop into the global region scheduler to iterate online players</li>
 *   <li>For each player, asynchronously load their {@link de.Keyle.MyPet.api.player.MyPetPlayer}
 *       record from the repository, then hop into the player's region to apply state</li>
 *   <li>If the player has a pet for the current world group, asynchronously load the
 *       {@link StoredMyPet} and hop back into the player's region to activate and (if
 *       requested) respawn it</li>
 * </ol>
 *
 * <p>All scheduler hops use the per-region scheduler API, so the code is correct on both
 * Paper and Folia. Repository calls return {@link java.util.concurrent.CompletableFuture}s
 * that complete on async threads; UI/state mutation is always performed back inside the
 * relevant region's thread.</p>
 */
public final class OnlinePlayerPetLoader {

    private OnlinePlayerPetLoader() {
    }

    /**
     * Kicks off restoration for every player currently online. Returns immediately; all work
     * happens asynchronously and across multiple region scheduler hops.
     *
     * @param plugin         the plugin instance used for scheduler ownership
     * @param repository     the active repository, queried for the per-player record
     * @param myPetManager   used to activate the loaded {@link StoredMyPet} into a live
     *                       {@link MyPet} and to deactivate stale per-world-group pets
     * @param playerManager  receives each restored {@link de.Keyle.MyPet.api.player.MyPetPlayer}
     *                       via {@link PlayerManager#setOnline}
     */
    public static void restoreForOnlinePlayers(@NotNull JavaPlugin plugin,
                                               @NotNull Repository repository,
                                               @NotNull MyPetManager myPetManager,
                                               @NotNull PlayerManager playerManager) {
        Bukkit.getServer().getGlobalRegionScheduler().run(plugin, deferredTask -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                repository.getMyPetPlayer(player).thenAccept(loadedPlayer -> {
                    if (loadedPlayer == null) return;
                    player.getScheduler().run(plugin, playerTask ->
                            handlePlayer(plugin, player, (MyPetPlayerImpl) loadedPlayer,
                                    myPetManager, playerManager), null);
                });
            }
        });
    }

    private static void handlePlayer(JavaPlugin plugin,
                                     Player player,
                                     MyPetPlayerImpl onlinePlayer,
                                     MyPetManager myPetManager,
                                     PlayerManager playerManager) {
        playerManager.setOnline(onlinePlayer);

        WorldGroup joinGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
        if (joinGroup.isDisabled()) {
            return;
        }

        if (onlinePlayer.hasMyPet()) {
            MyPet myPet = onlinePlayer.getMyPet();
            if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                myPetManager.deactivateMyPet(onlinePlayer, true);
            }
        }

        if (!onlinePlayer.hasMyPet() && onlinePlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
            UUID petUUID = onlinePlayer.getMyPetForWorldGroup(joinGroup.getName());
            MyPetPlugin.getInstance().getRepository().getPet(petUUID).thenAccept(storedMyPet ->
                    player.getScheduler().run(plugin, petTask ->
                            activateAndMaybeRespawn(myPetManager, onlinePlayer, storedMyPet), null));
        }
        onlinePlayer.checkForContribution();
    }

    private static void activateAndMaybeRespawn(MyPetManager myPetManager,
                                                MyPetPlayerImpl onlinePlayer,
                                                StoredMyPet storedMyPet) {
        myPetManager.activateMyPet(storedMyPet);
        if (!onlinePlayer.hasMyPet()) {
            return;
        }
        MyPet myPet = onlinePlayer.getMyPet();
        MyPetPlayer myPetPlayer = myPet.getOwner();
        if (!myPet.wantsToRespawn() || !myPetPlayer.hasMyPet()) {
            return;
        }
        MyPet runMyPet = myPetPlayer.getMyPet();
        sendSpawnMessage(runMyPet, runMyPet.createEntity());
    }

    private static void sendSpawnMessage(MyPet pet, MyPet.SpawnFlags result) {
        MyPetPlayer owner = pet.getOwner();
        switch (result) {
            case Canceled:
                owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", owner, pet.getDisplayName()));
                break;
            case NoSpace:
                owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", owner, pet.getDisplayName()));
                break;
            case NotAllowed:
                owner.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", owner, pet.getDisplayName()));
                break;
            case Dead:
                if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                    owner.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", owner, pet.getDisplayName()));
                } else {
                    owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", owner, pet.getDisplayName(), pet.getRespawnTime()));
                }
                break;
            case Flying:
                owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", owner, pet.getDisplayName()));
                break;
            case Success:
                owner.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", owner, pet.getDisplayName()));
                break;
            default:
                throw new IllegalStateException("Unhandled SpawnFlags: " + result);
        }
    }
}
