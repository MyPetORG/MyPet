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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetLavaEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.entity.leashing.WildAngerCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.Warden;

@ShopInfo
@DefaultInfo(food = {Material.BONE}, flySpeed = 0.6608D)
public class PetWarden extends PetImpl implements PetLavaEntity {

    public static final WildAngerCheck<Warden> ANGER_CHECK =
            new WildAngerCheck<>(Warden.class, warden -> warden.getAngerLevel() != Warden.AngerLevel.CALM);

    /**
     * Strips Warden brain behaviors that autonomously target and attack
     * entities (including the owner). Vanilla Warden uses Brain not Goals,
     * so {@code PetGoalInstaller}'s {@code removeAllGoals} sweep leaves all
     * of vibration-driven anger management, sniff/roar/sonic-boom, and the
     * emerge/dig spawn-and-despawn animations intact.
     *
     * <p>Load-bearing entry: {@code SetRoarTarget} is the only behavior
     * that copies {@code AngerManagement} state into a memory the FIGHT
     * activity can act on ({@code ROAR_TARGET} → {@code Roar} →
     * {@code ATTACK_TARGET}). Stripping it breaks the targeting chain at
     * its root. The FIGHT-activity strips ({@code Roar}, {@code SonicBoom},
     * {@code MeleeAttack}) are belt-and-suspenders against any future
     * Mojang behavior that writes {@code ATTACK_TARGET} directly.
     *
     * <p>{@code Emerging} and {@code Digging} are quality-of-life strips:
     * without them, the pet would play a multi-second emergence animation
     * on spawn and periodically burrow into the ground when idle.
     */
    public static final PetBrainBehaviorRemoval BRAIN_BEHAVIOR_REMOVAL = new PetBrainBehaviorRemoval(
            "Warden",
            "SetRoarTarget",
            "Roar",
            "SonicBoom",
            "MeleeAttack",
            "Emerging",
            "Digging"
    );

    public PetWarden(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
