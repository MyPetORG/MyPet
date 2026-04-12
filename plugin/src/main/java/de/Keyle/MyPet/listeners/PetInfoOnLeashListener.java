package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.ContributorCheck;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.CommandInfo;
import de.Keyle.MyPet.commands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.util.PetInfoBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * UI gesture listener: when a player punches a pet while holding its
 * leash item, display the pet's info report instead of dealing damage.
 *
 * <p>This is an "event-as-RPC" pattern — a damage event repurposed as a
 * UI input. Extracted from {@code MyPetEntityListener} to make the intent
 * explicit: this file handles the info-display gesture, not damage logic.
 */
public class PetInfoOnLeashListener implements Listener {

    @EventHandler
    public void onLeashInfoGesture(final EntityDamageByEntityEvent event) {
        MyPet myPet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (myPet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        // Resolve the player behind the damager (direct hit or projectile)
        if (!(event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player))) {
            return;
        }
        Player damager;
        if (event.getDamager() instanceof Projectile) {
            damager = (Player) ((Projectile) event.getDamager()).getShooter();
        } else {
            damager = (Player) event.getDamager();
        }

        ItemStack leashItem = damager.getEquipment().getItemInMainHand();
        if (!MyPetApi.getMyPetInfo().getLeashItem(myPet.getPetType()).compare(leashItem)) {
            return;
        }

        boolean infoShown = false;

        // Pet name header
        if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, damager, myPet)) {
            damager.sendMessage(PetInfoBuilder.petNameHeader(myPet));
            infoShown = true;
        }

        // Owner line (only show if viewing someone else's pet)
        if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, damager, myPet) && myPet.getOwner().getPlayer() != damager) {
            damager.sendMessage(PetInfoBuilder.ownerLine(myPet, damager));
            infoShown = true;
        }

        // HP line
        if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, damager, myPet)) {
            damager.sendMessage(PetInfoBuilder.hpLine(myPet, damager));
            infoShown = true;
        }

        // Respawn time (if dead)
        if (CommandInfo.canSee(PetInfoDisplay.RespawnTime.adminOnly, damager, myPet)) {
            Component respawnTime = PetInfoBuilder.respawnTimeLine(myPet, damager);
            if (respawnTime != null) {
                damager.sendMessage(respawnTime);
                infoShown = true;
            }
        }

        // Damage line
        if (CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, damager, myPet)) {
            Component damage = PetInfoBuilder.damageLine(myPet, damager);
            if (damage != null) {
                damager.sendMessage(damage);
                infoShown = true;
            }
        }

        // Ranged damage line
        if (CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, damager, myPet)) {
            Component rangedDamage = PetInfoBuilder.rangedDamageLine(myPet, damager);
            if (rangedDamage != null) {
                damager.sendMessage(rangedDamage);
                infoShown = true;
            }
        }

        // Hunger system
        if (CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, damager, myPet)) {
            Component hunger = PetInfoBuilder.hungerLine(myPet, damager);
            if (hunger != null) {
                damager.sendMessage(hunger);
                infoShown = true;
            }

            Component food = PetInfoBuilder.foodLine(myPet, damager);
            if (food != null) {
                damager.sendMessage(food);
                infoShown = true;
            }
        }

        // Behavior line
        if (CommandInfo.canSee(PetInfoDisplay.Behavior.adminOnly, damager, myPet)) {
            Component behavior = PetInfoBuilder.behaviorLine(myPet, damager);
            if (behavior != null) {
                damager.sendMessage(behavior);
                infoShown = true;
            }
        }

        // Skilltree line
        if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, damager, myPet)) {
            Component skilltree = PetInfoBuilder.skilltreeLine(myPet, damager);
            if (skilltree != null) {
                damager.sendMessage(skilltree);
                infoShown = true;
            }
        }

        // Level line
        if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, damager, myPet)) {
            damager.sendMessage(PetInfoBuilder.levelLine(myPet, damager));
            infoShown = true;
        }

        // Experience line
        if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, damager, myPet)) {
            Component exp = PetInfoBuilder.expLine(myPet, damager);
            if (exp != null) {
                damager.sendMessage(exp);
                infoShown = true;
            }
        }
        if (myPet.getOwner().getContributorRank() != ContributorCheck.ContributorRank.None) {
            infoShown = true;
            String icon = myPet.getOwner().getContributorRank().getDefaultIcon();
            String title = Translation.getString("Name.Title." + myPet.getOwner().getContributorRank().name(), damager);
            damager.sendMessage(Component.text("   " + icon + " " + title + " " + icon).color(NamedTextColor.GOLD));
        }

        if (!infoShown) {
            damager.sendMessage(Translation.getComponent("Message.No.NothingToSeeHere", myPet.getOwner()));
        }

        event.setCancelled(true);
    }
}
