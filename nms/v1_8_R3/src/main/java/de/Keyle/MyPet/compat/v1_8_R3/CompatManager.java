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

package de.Keyle.MyPet.compat.v1_8_R3;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_8_R3.services.EggIconService;
import de.Keyle.MyPet.compat.v1_8_R3.services.EntityConverterService;

@Compat("v1_8_R3")
public class CompatManager extends de.Keyle.MyPet.api.util.CompatManager {
    public void init() {
        MyPetApi.getServiceManager().registerService(EggIconService.class);
        MyPetApi.getServiceManager().registerService(EntityConverterService.class);
    }
}
