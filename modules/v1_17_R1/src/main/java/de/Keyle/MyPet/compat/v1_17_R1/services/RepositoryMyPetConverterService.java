/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_17_R1.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.service.Load;
import de.keyle.knbt.TagCompound;

@Load(Load.State.AfterHooks)
public class RepositoryMyPetConverterService extends de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService {

    public void v1_17_R1(StoredMyPet pet) {
        TagCompound info = pet.getInfo();

        switch (pet.getPetType()) {
            case PigZombie:
                pet.setPetType(MyPetType.ZombifiedPiglin);
        }

        pet.setInfo(info);
    }
}
