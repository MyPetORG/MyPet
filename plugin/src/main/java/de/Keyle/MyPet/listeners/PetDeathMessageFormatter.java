package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
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
        if (!Boolean.TRUE.equals(event.getEntity().getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES))) return;

        MyPet myPet = getMyPetManager().getMyPetFromEntity(event.getEntity());
        if (myPet == null) return;

        Component killer;
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent e) {

            if (e.getDamager().getType() == EntityType.PLAYER) {
                if (e.getDamager() == myPet.getOwner().getPlayer()) {
                    killer = Locale.getComponent("Name.You", myPet.getOwner());
                } else {
                    killer = Component.text(((Player) e.getDamager()).getName());
                }
            } else if (e.getDamager().getType() == EntityType.WOLF) {
                Wolf w = (Wolf) e.getDamager();
                killer = Locale.getComponent("Name.Wolf", myPet.getOwner());
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
                Component projectileName = Locale.getComponent("Name." + capitalizeName(projectile.getType().name()), myPet.getOwner());
                Component shooterName;
                if (projectile.getShooter() instanceof Player) {
                    if (projectile.getShooter() == myPet.getOwner().getPlayer()) {
                        shooterName = Locale.getComponent("Name.You", myPet.getOwner());
                    } else {
                        shooterName = Component.text(((Player) projectile.getShooter()).getName());
                    }
                } else {
                    if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                        shooterName = Locale.getComponent("Name." + capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner());
                    } else if (e.getDamager().getType().getName() != null) {
                        shooterName = Locale.getComponent("Name." + capitalizeName(e.getDamager().getType().getName()), myPet.getOwner());
                    } else {
                        shooterName = Locale.getComponent("Name.Unknow", myPet.getOwner());
                    }
                }
                killer = projectileName.append(Component.text(" (")).append(shooterName).append(Component.text(")"));
            } else {
                if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                    killer = Locale.getComponent("Name." + capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner());
                } else {
                    if (e.getDamager().getType().getName() != null) {
                        killer = Locale.getComponent("Name." + capitalizeName(e.getDamager().getType().getName()), myPet.getOwner());
                    } else {
                        killer = Locale.getComponent("Name.Unknow", myPet.getOwner());
                    }
                }
            }
        } else {
            if (event.getEntity().getLastDamageCause() != null) {
                killer = Locale.getComponent("Name." + capitalizeName(event.getEntity().getLastDamageCause().getCause().name()), myPet.getOwner());
            } else {
                killer = Locale.getComponent("Name.Unknow", myPet.getOwner());
            }
        }

        myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.DeathMessage", myPet.getOwner(), myPet.getDisplayName(), killer));
    }

    private static String capitalizeName(String name) {
        if (name == null) {
            MyPetApi.getLogger().warning("Name is null");
            return null;
        }
        name = name.replace("_", " ");
        StringBuilder sb = new StringBuilder(name.length());
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (Character.isLetter(c) && capitalizeNext) {
                sb.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
                capitalizeNext = !Character.isLetter(c);
            }
        }
        return sb.toString().replace(" ", "");
    }
}
