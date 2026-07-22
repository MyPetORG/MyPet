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

package de.Keyle.MyPet.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.skill.skills.ArmorImpl;
import de.Keyle.MyPet.util.CompatUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Mob;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Syncs the Armor skill's armor / armor-toughness values onto the spawned mob as
 * attribute modifiers, letting vanilla's damage formula handle the reduction.
 *
 * <p>Application is idempotent (remove-then-add under a stable {@link NamespacedKey}),
 * so respawns and repeated level-ups never stack modifiers. Called from
 * {@code VanillaMobSpawner.configureMob} (spawn/respawn/convert) and from
 * {@code LevelListener} after skilltree upgrades change while the pet is spawned.
 * Transient modifiers are used so the bonus never leaks into the full-NBT
 * {@code PetEntitySnapshot} round-trip or a released wild mob.
 */
public final class PetArmorApplier {

    private static final NamespacedKey ARMOR_KEY = new NamespacedKey("mypet", "skill_armor");
    private static final NamespacedKey TOUGHNESS_KEY = new NamespacedKey("mypet", "skill_armor_toughness");

    /**
     * NamespacedKey-identified attribute modifiers exist since MC 1.21; on 1.20.5/6
     * the legacy UUID-identified API is the only one available at runtime.
     */
    private static final boolean KEYED_MODIFIERS = CompatUtil.minecraftVersionEqualsOrAbove("1.21");

    private PetArmorApplier() {
    }

    /** Re-applies (or clears) the pet's Armor-skill modifiers on its live mob. No-op while despawned. */
    public static void update(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        ArmorImpl skill = pet.getSkills().get(ArmorImpl.class);
        double armor = skill != null ? skill.getArmor().getValue() : 0;
        double toughness = skill != null ? skill.getToughness().getValue() : 0;
        Runnable apply = () -> {
            applyModifier(mob.getAttribute(PetAttributes.ARMOR), ARMOR_KEY, armor);
            applyModifier(mob.getAttribute(PetAttributes.ARMOR_TOUGHNESS), TOUGHNESS_KEY, toughness);
        };
        if (Bukkit.isOwnedByCurrentRegion(mob)) {
            apply.run();
        } else {
            mob.getScheduler().run(MyPetApi.getPlugin(), task -> apply.run(), null);
        }
    }

    private static void applyModifier(AttributeInstance instance, NamespacedKey key, double amount) {
        if (instance == null) {
            return;
        }
        if (KEYED_MODIFIERS) {
            applyKeyed(instance, key, amount);
        } else {
            applyLegacy(instance, key, amount);
        }
    }

    private static void applyKeyed(AttributeInstance instance, NamespacedKey key, double amount) {
        instance.removeModifier(key);
        if (amount > 0) {
            instance.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /** Pre-1.21 path: same stable identity, derived deterministically from the key. */
    @SuppressWarnings({"deprecation", "removal"})
    private static void applyLegacy(AttributeInstance instance, NamespacedKey key, double amount) {
        UUID id = UUID.nameUUIDFromBytes(key.toString().getBytes(StandardCharsets.UTF_8));
        for (AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (id.equals(modifier.getUniqueId())) {
                instance.removeModifier(modifier);
            }
        }
        if (amount > 0) {
            instance.addTransientModifier(
                    new AttributeModifier(id, key.toString(), amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}
