/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.entity.MyPet;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Mob;

import java.lang.reflect.Method;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetNaturalDrop;
import de.Keyle.MyPet.api.entity.ShopInfo;
import java.util.Set;
import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.SPIDER_EYE})
public class MyArmadillo extends MyPet implements MyPetBaby, MyPetNaturalDrop {

    // Paper 1.21.5+ exposes Armadillo#getState() returning an Armadillo.State enum
    // ({IDLE, ROLLING, SCARED, UNROLLING}). v4 compiles against 1.21.4 where the
    // method does not exist yet but supports running on 1.21.5–1.21.11, so the
    // lookup is performed reflectively once and cached.
    // TODO Replace reflection when MyPet minimum MC version reaches 1.21.5
    private static volatile Method getStateMethod;
    private static volatile boolean stateLookupDone;

    public MyArmadillo(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public boolean canMove() {
        if (!super.canMove()) {
            return false;
        }
        Mob entity = getBukkitEntity();
        if (entity instanceof Armadillo armadillo) {
            return !isCowering(armadillo);
        }
        return true;
    }

    private static boolean isCowering(Armadillo armadillo) {
        Method method = resolveGetState(armadillo);
        if (method == null) {
            return false;
        }
        try {
            Object state = method.invoke(armadillo);
            return state != null && !"IDLE".equals(state.toString());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Method resolveGetState(Armadillo armadillo) {
        if (stateLookupDone) {
            return getStateMethod;
        }
        synchronized (MyArmadillo.class) {
            if (!stateLookupDone) {
                try {
                    getStateMethod = armadillo.getClass().getMethod("getState");
                } catch (NoSuchMethodException ignored) {
                    getStateMethod = null;
                }
                stateLookupDone = true;
            }
        }
        return getStateMethod;
    }


    @Override
    public Set<Material> naturalDropMaterials() {
        return Set.of(Material.ARMADILLO_SCUTE);
    }

    @Override
    public boolean isNaturalDropSuppressed() {
        return !Configuration.MyPet.Armadillo.CAN_SHED_SCUTE;
    }
}
