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

package de.Keyle.MyPet.api.entity;

/**
 * Marker for pets that can be saddled in vanilla Minecraft, via any of
 * the three Bukkit-API saddle shapes:
 *
 * <ul>
 *   <li>{@link org.bukkit.inventory.SaddledMountInventory} — inventory
 *       saddle slot (Horse, Donkey, Mule, SkeletonHorse, ZombieHorse,
 *       Camel, CamelHusk via {@link org.bukkit.inventory.AbstractHorseInventory};
 *       Nautilus, ZombieNautilus via {@link org.bukkit.inventory.ArmoredSaddledMountInventory})</li>
 *   <li>{@link org.bukkit.entity.Steerable} — boolean saddle flag
 *       (Pig, Strider). Saddle item not stored — vanilla consumes
 *       the right-clicked item and tracks only a boolean state.</li>
 *   <li>{@link org.bukkit.inventory.EquipmentSlot#SADDLE} — equipment-slot
 *       saddle/harness (HappyGhast harness, also the standard backing for
 *       the inventory-based saddle types above).</li>
 * </ul>
 *
 * <p>The runtime listener calls {@code PetSaddleHelper.isSaddled} /
 * {@code applySaddle} / {@code removeSaddle} to abstract over these
 * shapes; callers never need to switch on the underlying Bukkit class.
 *
 * <p>Drives two pieces of MyPet machinery:
 *
 * <ul>
 *   <li>{@code PetCreationOptions} auto-generates a {@code saddle} flag
 *       creation option for each implementer where {@code PetSaddleHelper.getDefaultSaddleStack}
 *       returns a non-null default. (HappyGhast returns {@code null} —
 *       its harness goes through a separate per-pet {@code harness:<color>}
 *       creation option.)</li>
 *   <li>{@code ConfigurationLoader} auto-registers
 *       {@code RequireSaddle} (default {@code false}) and
 *       {@code AllowNonOwnerSaddle} (default {@code false}) per-pet
 *       config flags for each implementer.</li>
 * </ul>
 *
 * <p>Excluded by design: Llama / TraderLlama (vanilla wears a decorative
 * carpet, not a saddle — there's no rider gating to apply).
 */
public interface PetSaddleable {
}
