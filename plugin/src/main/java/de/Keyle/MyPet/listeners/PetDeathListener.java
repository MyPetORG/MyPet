package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.event.MyPetRemoveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import static de.Keyle.MyPet.MyPetApi.getMyPetManager;

/**
 * Handles the full pet death pipeline: release-on-death, respawn timer
 * calculation (including duel-mode fast respawn), drop suppression,
 * XP loss, backpack drop, death message, and auto-respawn economy.
 */
public class PetDeathListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPetDeath(final EntityDeathEvent event) {
        MyPet myPet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (myPet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        LivingEntity deadEntity = event.getEntity();

        // check health for death events where the pet isn't really dead (/killall)
        if (myPet.getHealth() > 0) return;

        final MyPetPlayer owner = myPet.getOwner();

        // Release-on-death: permanently remove the pet
        if (MyPetApi.getMyPetInfo().getReleaseOnDeath(myPet.getPetType()) && !owner.isMyPetAdmin()) {
            MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.Death);
            Bukkit.getServer().getPluginManager().callEvent(removeEvent);

            if (myPet.getSkills().isActive(Backpack.class)) {
                CustomInventory inv = myPet.getSkills().get(Backpack.class).getInventory();
                inv.dropContentAt(myPet.getLocation().get());
            }
            if (myPet instanceof MyPetEquipment) {
                ((MyPetEquipment) myPet).dropEquipment();
            }

            myPet.removePet();
            owner.setMyPetForWorldGroup(WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()), null);

            myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Command.Release.Dead", owner, myPet.getDisplayName()));

            getMyPetManager().deactivateMyPet(owner, false);
            MyPetApi.getRepository().removeMyPet(myPet.getUUID());

            return;
        }

        // Calculate respawn time
        myPet.setRespawnTime((Configuration.Respawn.TIME_FIXED + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(myPet.getPetType())) + (myPet.getExperience().getLevel() * (Configuration.Respawn.TIME_FACTOR + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(myPet.getPetType()))));
        myPet.setStatus(PetState.Dead);

        if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent e) {
            if (e.getDamager() instanceof Player) {
                myPet.setRespawnTime((Configuration.Respawn.TIME_PLAYER_FIXED + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(myPet.getPetType())) + (myPet.getExperience().getLevel() * (Configuration.Respawn.TIME_PLAYER_FACTOR + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(myPet.getPetType()))));
            } else if (PetEntityMarker.isMarked(e.getDamager())) {
                MyPet killerMyPet = getMyPetManager().getMyPetFromEntity(e.getDamager());
                if (myPet.getSkills().isActive(Behavior.class) && killerMyPet.getSkills().isActive(Behavior.class)) {
                    Behavior killerBehaviorSkill = killerMyPet.getSkills().get(Behavior.class);
                    Behavior deadBehaviorSkill = myPet.getSkills().get(Behavior.class);
                    if (deadBehaviorSkill.getBehavior() == BehaviorMode.Duel && killerBehaviorSkill.getBehavior() == BehaviorMode.Duel) {
                        MyPet myPetForEntity = getMyPetManager().getMyPetFromEntity(deadEntity);
                        if (myPetForEntity != null && e.getDamager().equals(myPetForEntity.getMyPetTarget())) {
                            myPet.setRespawnTime(10);
                            killerMyPet.setHealth(Double.MAX_VALUE);
                        }
                    }
                }
            }
        }

        // Suppress vanilla drops and XP
        event.setDroppedExp(0);
        event.getDrops().clear();

        // XP loss on death
        if (Configuration.LevelSystem.Experience.LOSS_FIXED > 0 || Configuration.LevelSystem.Experience.LOSS_PERCENT > 0) {
            double lostExpirience = Configuration.LevelSystem.Experience.LOSS_FIXED;
            lostExpirience += myPet.getExperience().getRequiredExp() * Configuration.LevelSystem.Experience.LOSS_PERCENT / 100;
            if (lostExpirience > myPet.getExp()) {
                lostExpirience = myPet.getExp();
            }
            if (myPet.getSkilltree() != null) {
                int requiredLevel = myPet.getSkilltree().getRequiredLevel();
                if (requiredLevel > 1) {
                    double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                    lostExpirience = myPet.getExp() - lostExpirience < minExp ? myPet.getExp() - minExp : lostExpirience;
                }
            }
            if (Configuration.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE) {
                lostExpirience = myPet.getExperience().removeExp(lostExpirience);
            } else {
                lostExpirience = myPet.getExperience().removeCurrentExp(lostExpirience);
            }
            if (Configuration.LevelSystem.Experience.DROP_LOST_EXP && lostExpirience < 0) {
                event.setDroppedExp((int) (Math.abs(lostExpirience)));
            }
        }

        // Backpack drop on death
        if (myPet.getSkills().isActive(Backpack.class)) {
            BackpackImpl inventorySkill = myPet.getSkills().get(BackpackImpl.class);
            inventorySkill.closeInventory();
            if (inventorySkill.getDropOnDeath().getValue() && !owner.isMyPetAdmin()) {
                inventorySkill.getInventory().dropContentAt(myPet.getLocation().get());
            }
        }

        // Death message and respawn notification
        PetDeathMessageFormatter.sendDeathMessage(event);
        myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn.In", owner.getPlayer(), myPet.getDisplayName(), myPet.getRespawnTime()));

        // Auto-respawn via economy
        if (MyPetApi.getHookHelper().isEconomyEnabled() && owner.hasAutoRespawnEnabled() && myPet.getRespawnTime() <= owner.getAutoRespawnMin() && Permissions.has(owner.getPlayer(), "MyPet.command.respawn")) {
            double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
            if (MyPetApi.getHookHelper().getEconomy().canPay(owner, costs)) {
                MyPetApi.getHookHelper().getEconomy().pay(owner, costs);
                myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Command.Respawn.Paid", owner.getPlayer(), myPet.getDisplayName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                myPet.setRespawnTime(1);
            } else {
                myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Command.Respawn.NoMoney", owner.getPlayer(), myPet.getDisplayName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
            }
        }
    }
}
