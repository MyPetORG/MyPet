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

import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorHelpers;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetLavaEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

@ShopInfo(displayName = "Magma Cube", options = {"size:2"})
@DefaultInfo(food = {Material.REDSTONE})
public class PetMagmaCube extends PetImpl implements PetLavaEntity {

    public static final ConfigKey<Boolean> CAN_HURT_PLAYERS_ON_CONTACT = ConfigKey.bool("MagmaCube", "CanHurtPlayersOnContact", false);

    /**
     * Gates passive cube-mob contact damage to non-owner players. Owner is
     * universally protected. Deliberate attacks (via
     * {@code PetMeleeAttackGoal}'s current target) bypass the gate. Otherwise
     * the per-type {@link #CAN_HURT_PLAYERS_ON_CONTACT} flag decides.
     *
     * <p>Vanilla {@code Slime#playerTouch(Player)} fires automatically when a
     * cube-mob is in contact with a Player — without this gate, slime/magma
     * pets damage their owner and other players just by hopping near them.
     */
    public static final PetBehavior<EntityDamageByEntityEvent> CUBE_CONTACT_DAMAGE_GATE =
            PetBehaviorHelpers.onPetDamages("MagmaCube", (event, pet, mob) -> {
                if (!(event.getEntity() instanceof Player victim)) return;
                // Deliberate attacks via PetMeleeAttackGoal target the pet's
                // current target. Don't gate those.
                if (pet.getPetTarget() == victim) return;
                // Owner is universally protected.
                Player owner = pet.getOwner() != null ? pet.getOwner().getPlayer() : null;
                if (owner != null && owner.equals(victim)) {
                    event.setCancelled(true);
                    return;
                }
                if (!CAN_HURT_PLAYERS_ON_CONTACT.get()) {
                    event.setCancelled(true);
                }
            });

    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            PetCreationOptions.sizeSpec(MagmaCube.class, 8, MagmaCube::setSize)
    );

    public PetMagmaCube(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
