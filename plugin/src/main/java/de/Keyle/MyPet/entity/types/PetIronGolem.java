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

import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetAttributes;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

@ShopInfo(displayName = "Iron Golem")
@DefaultInfo(food = {Material.IRON_INGOT}, leashFlags = {"UserCreated"}, fallbackIconMaterial = "SKELETON_SPAWN_EGG", fallbackIconGlow = true)
public class PetIronGolem extends PetImpl {

    public static final ConfigKey<Boolean> CAN_TOSS_UP = ConfigKey.bool("IronGolem", "CanTossUp", true);


    public PetIronGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Applies vanilla IronGolem toss-up after a successful melee hit.
     * Vanilla applies this inside {@code IronGolem#doHurtTarget}, but
     * MyPet's melee path calls {@code target.damage(...)} directly to
     * bypass attacker-specific knockback, so we re-apply the upward
     * impulse here scaled by the victim's KNOCKBACK_RESISTANCE.
     */
    @Override
    public void onMeleeHitLanded(LivingEntity target) {
        if (!CAN_TOSS_UP.get()) return;
        double knockbackResistance = 0.0;
        AttributeInstance attribute = target.getAttribute(PetAttributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            knockbackResistance = attribute.getValue();
        }
        double scale = Math.max(0.0, 1.0 - knockbackResistance);
        if (scale <= 0.0) return;
        Vector velocity = target.getVelocity();
        target.setVelocity(velocity.setY(velocity.getY() + 0.4 * scale));
    }
}
