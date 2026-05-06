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

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.ShopInfo;

@Getter
@ShopInfo
@DefaultInfo(food = {Material.SOUL_SAND})
public class PetEnderman extends PetImpl {

    /**
     * Pet-only override: vanilla {@link Enderman} screaming is AI-driven and
     * does not persist to NBT. When this flag is set, the live entity is
     * force-screamed on each {@code updateVisuals} pass.
     */
    protected boolean permaScreaming = false;

    public PetEnderman(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setPermaScreaming(boolean flag) {
        this.permaScreaming = flag;
        if (status == PetState.Here && getBukkitEntity() instanceof Enderman enderman) {
            enderman.setScreaming(flag);
        }
    }

    @Override
    public void updateVisuals() {
        super.updateVisuals();
        if (permaScreaming && getBukkitEntity() instanceof Enderman enderman) {
            enderman.setScreaming(true);
        }
    }

}
