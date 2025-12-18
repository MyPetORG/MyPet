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
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.entity.Panda;

@Getter
public class MyPanda extends MyPet implements de.Keyle.MyPet.api.entity.types.MyPanda {

    protected Panda.Gene mainGene = Panda.Gene.NORMAL;
    protected Panda.Gene hiddenGene = Panda.Gene.NORMAL;

    public MyPanda(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public void setMainGene(Panda.Gene gene) {
        this.mainGene = gene;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public void setHiddenGene(Panda.Gene gene) {
        this.hiddenGene = gene;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putInt("MainGene", getMainGene().ordinal());
        info = info.putInt("HiddenGene", getHiddenGene().ordinal());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("MainGene")) {
            setMainGene(Panda.Gene.values()[info.getInt("MainGene")]);
        }
        if (info.keySet().contains("HiddenGene")) {
            setHiddenGene(Panda.Gene.values()[info.getInt("HiddenGene")]);
        }
    }
}