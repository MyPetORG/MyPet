package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import static de.Keyle.MyPet.MyPetApi.getMyPetManager;

/**
 * Formats and sends the death message when a pet dies. Handles all
 * killer-name variants: player, wolf (tamed/wild), other pet,
 * projectile (with shooter resolution), generic mob, and environment.
 */
@SuppressWarnings("RedundantCast")
final class PetDeathMessageFormatter {

    private PetDeathMessageFormatter() {}

    static void sendDeathMessage(final EntityDeathEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) return;
        if (!MyPetApi.getPlatformHelper().gameruleDoDeathMessages(event.getEntity())) return;

        MyPet myPet = getMyPetManager().getMyPetFromEntity(event.getEntity());
        if (myPet == null) return;

        Component killer;
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent e) {

            if (e.getDamager().getType() == EntityType.PLAYER) {
                if (e.getDamager() == myPet.getOwner().getPlayer()) {
                    killer = Translation.getComponent("Name.You", myPet.getOwner());
                } else {
                    killer = Component.text(((Player) e.getDamager()).getName());
                }
            } else if (e.getDamager().getType() == EntityType.WOLF) {
                Wolf w = (Wolf) e.getDamager();
                killer = Translation.getComponent("Name.Wolf", myPet.getOwner());
                if (w.isTamed()) {
                    killer = killer.append(Component.text(" (" + w.getOwner().getName() + ")"));
                }
            } else if (PetEntityMarker.isMarked(e.getDamager())) {
                MyPet damagerPet = getMyPetManager().getMyPetFromEntity(e.getDamager());
                if (damagerPet != null) {
                    killer = damagerPet.getDisplayName().append(Component.text(" (" + damagerPet.getOwner().getName() + ")"));
                } else {
                    killer = Component.text(e.getDamager().getType().name());
                }
            } else if (e.getDamager() instanceof Projectile projectile) {
                Component projectileName = Translation.getComponent("Name." + Util.capitalizeName(projectile.getType().name()), myPet.getOwner());
                Component shooterName;
                if (projectile.getShooter() instanceof Player) {
                    if (projectile.getShooter() == myPet.getOwner().getPlayer()) {
                        shooterName = Translation.getComponent("Name.You", myPet.getOwner());
                    } else {
                        shooterName = Component.text(((Player) projectile.getShooter()).getName());
                    }
                } else {
                    if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                        shooterName = Translation.getComponent("Name." + Util.capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner());
                    } else if (e.getDamager().getType().getName() != null) {
                        shooterName = Translation.getComponent("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner());
                    } else {
                        shooterName = Translation.getComponent("Name.Unknow", myPet.getOwner());
                    }
                }
                killer = projectileName.append(Component.text(" (")).append(shooterName).append(Component.text(")"));
            } else {
                if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                    killer = Translation.getComponent("Name." + Util.capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner());
                } else {
                    if (e.getDamager().getType().getName() != null) {
                        killer = Translation.getComponent("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner());
                    } else {
                        killer = Translation.getComponent("Name.Unknow", myPet.getOwner());
                    }
                }
            }
        } else {
            if (event.getEntity().getLastDamageCause() != null) {
                killer = Translation.getComponent("Name." + Util.capitalizeName(event.getEntity().getLastDamageCause().getCause().name()), myPet.getOwner());
            } else {
                killer = Translation.getComponent("Name.Unknow", myPet.getOwner());
            }
        }

        myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.DeathMessage", myPet.getOwner(), myPet.getDisplayName(), killer));
    }
}
