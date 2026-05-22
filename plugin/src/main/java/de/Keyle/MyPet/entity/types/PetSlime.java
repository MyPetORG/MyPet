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
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

@ShopInfo(options = {"size:2"})
@DefaultInfo(food = {Material.SUGAR}, flySpeed = 1.5419D)
public class PetSlime extends PetImpl {

    public static final ConfigKey<Boolean> CAN_HURT_PLAYERS_ON_CONTACT = ConfigKey.bool("Slime", "CanHurtPlayersOnContact", false);

    /** See {@code PetMagmaCube.CUBE_CONTACT_DAMAGE_GATE} — same logic, Slime variant. */
    public static final PetBehavior<EntityDamageByEntityEvent> CUBE_CONTACT_DAMAGE_GATE =
            PetBehaviorHelpers.onPetDamages("Slime", (event, pet, mob) -> {
                if (!(event.getEntity() instanceof Player victim)) return;
                if (pet.getPetTarget() == victim) return;
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
            PetCreationOptions.sizeSpec(Slime.class, 8, Slime::setSize)
    );

    public PetSlime(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
