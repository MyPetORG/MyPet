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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

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

        Pet pet = getPetManager().getPetFromEntity(event.getEntity());
        if (pet == null) return;

        Component killer;
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent e) {

            if (e.getDamager().getType() == EntityType.PLAYER) {
                if (e.getDamager() == pet.getOwner().getPlayer()) {
                    killer = Locale.getComponent("Name.You", pet.getOwner());
                } else {
                    killer = Component.text(((Player) e.getDamager()).getName());
                }
            } else if (e.getDamager().getType() == EntityType.WOLF) {
                Wolf w = (Wolf) e.getDamager();
                killer = resolveName("Name.Wolf", pet.getOwner(), e.getDamager(), null);
                if (w.isTamed()) {
                    killer = killer.append(Component.text(" (" + w.getOwner().getName() + ")"));
                }
            } else if (PetEntityMarker.isMarked(e.getDamager())) {
                Pet damagerPet = getPetManager().getPetFromEntity(e.getDamager());
                if (damagerPet != null) {
                    killer = damagerPet.getDisplayName().append(Component.text(" (" + damagerPet.getOwner().getName() + ")"));
                } else {
                    killer = Component.text(e.getDamager().getType().name());
                }
            } else if (e.getDamager() instanceof Projectile projectile) {
                Component projectileName = resolveName("Name." + capitalizeName(projectile.getType().name()), pet.getOwner(), projectile, null);
                Component shooterName;
                if (projectile.getShooter() instanceof Player) {
                    if (projectile.getShooter() == pet.getOwner().getPlayer()) {
                        shooterName = Locale.getComponent("Name.You", pet.getOwner());
                    } else {
                        shooterName = Component.text(((Player) projectile.getShooter()).getName());
                    }
                } else {
                    Entity shooterEntity = projectile.getShooter() instanceof Entity se ? se : null;
                    if (MyPetApi.getPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                        shooterName = resolveName("Name." + capitalizeName(PetType.byEntityTypeName(e.getDamager().getType().name()).name()), pet.getOwner(), shooterEntity, null);
                    } else if (e.getDamager().getType().getName() != null) {
                        shooterName = resolveName("Name." + capitalizeName(e.getDamager().getType().getName()), pet.getOwner(), shooterEntity, null);
                    } else {
                        shooterName = resolveName(null, pet.getOwner(), shooterEntity, null);
                    }
                }
                killer = projectileName.append(Component.text(" (")).append(shooterName).append(Component.text(")"));
            } else {
                if (MyPetApi.getPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                    killer = resolveName("Name." + capitalizeName(PetType.byEntityTypeName(e.getDamager().getType().name()).name()), pet.getOwner(), e.getDamager(), null);
                } else {
                    if (e.getDamager().getType().getName() != null) {
                        killer = resolveName("Name." + capitalizeName(e.getDamager().getType().getName()), pet.getOwner(), e.getDamager(), null);
                    } else {
                        killer = resolveName(null, pet.getOwner(), e.getDamager(), null);
                    }
                }
            }
        } else {
            if (event.getEntity().getLastDamageCause() != null) {
                EntityDamageEvent.DamageCause cause = event.getEntity().getLastDamageCause().getCause();
                killer = resolveName("Name." + capitalizeName(cause.name()), pet.getOwner(), null, cause);
            } else {
                killer = resolveName(null, pet.getOwner(), null, null);
            }
        }

        pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.DeathMessage", pet.getOwner(), pet.getDisplayName(), killer));
    }

    /**
     * Resolves the killer's display name without requiring per-mob/per-cause locale keys.
     * Ladder: (1) MyPet {@code Name.*} translation if present; (2) the killer entity's own
     * name (custom name, else client-localized type name); (3) the humanized damage cause;
     * (4) {@code Name.Unknow} when nothing is known.
     */
    private static Component resolveName(String key, MyPetPlayer owner, Entity entity, EntityDamageEvent.DamageCause cause) {
        if (key != null && Locale.hasKey(key, owner.getLanguage())) {
            return Locale.getComponent(key, owner);
        }
        if (entity != null) {
            return entityName(entity);
        }
        if (cause != null) {
            return Component.text(humanize(cause.name()));
        }
        return Locale.getComponent("Name.Unknow", owner);
    }

    /** The entity's custom name if set, otherwise its translatable type name so the client localizes it. */
    private static Component entityName(Entity entity) {
        Component custom = entity.customName();
        if (custom != null) {
            return custom;
        }
        return Component.translatable(entity.getType().translationKey());
    }

    /** Title-cases an underscore-separated enum constant, e.g. {@code SONIC_BOOM} to "Sonic Boom". English-only fallback. */
    private static String humanize(String enumName) {
        String[] words = enumName.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(enumName.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return sb.toString();
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
