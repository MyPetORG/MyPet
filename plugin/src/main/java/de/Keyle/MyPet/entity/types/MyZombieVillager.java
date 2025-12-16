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

import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import lombok.Getter;

@Getter
public class MyZombieVillager extends MyPet implements de.Keyle.MyPet.api.entity.types.MyZombieVillager {

    protected int profession = 0;
    protected MyVillager.Type type = MyVillager.Type.Plains;
    protected int tradingLevel = 1;

    public MyZombieVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Profession", new TagInt(getProfession()));
        info.getCompoundData().put("VillagerType", new TagInt(getType().ordinal()));
        info.getCompoundData().put("TradingLevel", new TagInt(getTradingLevel()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("Profession")) {
            setProfession(info.getAs("Profession", TagInt.class).getIntData());
        }
        if (info.containsKey("VillagerType")) {
            setType(MyVillager.Type.values()[info.getAs("VillagerType", TagInt.class).getIntData()]);
        }
        if (info.containsKey("TradingLevel")) {
            setTradingLevel(info.getAs("TradingLevel", TagInt.class).getIntData());
        }
    }

    public void setProfession(int value) {
        this.profession = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public void setType(MyVillager.Type value) {
        this.type = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public void setTradingLevel(int level) {
        this.tradingLevel = Math.max(1, level);
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }
}