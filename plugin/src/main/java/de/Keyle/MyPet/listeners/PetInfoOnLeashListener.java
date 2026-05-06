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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.commands.CommandInfo;
import de.Keyle.MyPet.commands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.util.PetInfoBuilder;
import de.Keyle.MyPet.util.player.ContributorCheck;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
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
 * UI input.
 */
public class PetInfoOnLeashListener implements Listener {

    @EventHandler
    public void onLeashInfoGesture(final EntityDamageByEntityEvent event) {
        Pet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
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
        if (!MyPetApi.getPetInfo().getLeashItem(pet.getPetType()).compare(leashItem)) {
            return;
        }

        boolean infoShown = false;

        // Pet name header
        if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, damager, pet)) {
            damager.sendMessage(PetInfoBuilder.petNameHeader(pet));
            infoShown = true;
        }

        // Owner line (only show if viewing someone else's pet)
        if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, damager, pet) && pet.getOwner().getPlayer() != damager) {
            damager.sendMessage(PetInfoBuilder.ownerLine(pet, damager));
            infoShown = true;
        }

        // HP line
        if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, damager, pet)) {
            damager.sendMessage(PetInfoBuilder.hpLine(pet, damager));
            infoShown = true;
        }

        // Respawn time (if dead)
        if (CommandInfo.canSee(PetInfoDisplay.RespawnTime.adminOnly, damager, pet)) {
            Component respawnTime = PetInfoBuilder.respawnTimeLine(pet, damager);
            if (respawnTime != null) {
                damager.sendMessage(respawnTime);
                infoShown = true;
            }
        }

        // Damage line
        if (CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, damager, pet)) {
            Component damage = PetInfoBuilder.damageLine(pet, damager);
            if (damage != null) {
                damager.sendMessage(damage);
                infoShown = true;
            }
        }

        // Ranged damage line
        if (CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, damager, pet)) {
            Component rangedDamage = PetInfoBuilder.rangedDamageLine(pet, damager);
            if (rangedDamage != null) {
                damager.sendMessage(rangedDamage);
                infoShown = true;
            }
        }

        // Hunger system
        if (CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, damager, pet)) {
            Component hunger = PetInfoBuilder.hungerLine(pet, damager);
            if (hunger != null) {
                damager.sendMessage(hunger);
                infoShown = true;
            }

            Component food = PetInfoBuilder.foodLine(pet, damager);
            if (food != null) {
                damager.sendMessage(food);
                infoShown = true;
            }
        }

        // Behavior line
        if (CommandInfo.canSee(PetInfoDisplay.Behavior.adminOnly, damager, pet)) {
            Component behavior = PetInfoBuilder.behaviorLine(pet, damager);
            if (behavior != null) {
                damager.sendMessage(behavior);
                infoShown = true;
            }
        }

        // Skilltree line
        if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, damager, pet)) {
            Component skilltree = PetInfoBuilder.skilltreeLine(pet, damager);
            if (skilltree != null) {
                damager.sendMessage(skilltree);
                infoShown = true;
            }
        }

        // Level line
        if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, damager, pet)) {
            damager.sendMessage(PetInfoBuilder.levelLine(pet, damager));
            infoShown = true;
        }

        // Experience line
        if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, damager, pet)) {
            Component exp = PetInfoBuilder.expLine(pet, damager);
            if (exp != null) {
                damager.sendMessage(exp);
                infoShown = true;
            }
        }
        ContributorCheck.ContributorRank rank = ((MyPetPlayerImpl) pet.getOwner()).getContributorRank();
        if (rank != ContributorCheck.ContributorRank.None) {
            infoShown = true;
            String icon = rank.getDefaultIcon();
            String title = Locale.getString("Name.Title." + rank.name(), damager);
            damager.sendMessage(Component.text("   " + icon + " " + title + " " + icon).color(NamedTextColor.GOLD));
        }

        if (!infoShown) {
            damager.sendMessage(Locale.getComponent("Message.No.NothingToSeeHere", pet.getOwner()));
        }

        event.setCancelled(true);
    }
}
