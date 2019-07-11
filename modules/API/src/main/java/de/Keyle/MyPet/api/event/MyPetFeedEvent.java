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

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.inventory.ItemStack;

public class MyPetFeedEvent extends MyPetInteractEvent {

    public enum Result {
        Heal, Eat
    }

    private double saturation;
    private Result result;

    public MyPetFeedEvent(MyPet myPet, ItemStack item, double saturation, Result result) {
        super(myPet, item);
        this.saturation = saturation;
        this.result = result;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public double getSaturation() {
        return saturation;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}