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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.entity.Panda;

public class MyPanda extends MyPet implements de.Keyle.MyPet.api.entity.types.MyPanda {

    /**
     * Gene storage by name (e.g. "NORMAL", "LAZY", "WORRIED") — drift-safe
     * across Paper updates that reorder or extend {@code Panda.Gene}.
     */
    protected String mainGeneName = Panda.Gene.NORMAL.name();
    protected String hiddenGeneName = Panda.Gene.NORMAL.name();

    public MyPanda(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Panda.Gene getMainGene() {
        try {
            return Panda.Gene.valueOf(mainGeneName);
        } catch (Throwable ignored) {
            return Panda.Gene.NORMAL;
        }
    }

    @Override
    public Panda.Gene getHiddenGene() {
        try {
            return Panda.Gene.valueOf(hiddenGeneName);
        } catch (Throwable ignored) {
            return Panda.Gene.NORMAL;
        }
    }

    @Override
    public void setMainGene(Panda.Gene gene) {
        if (gene != null) {
            this.mainGeneName = gene.name();
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public void setHiddenGene(Panda.Gene gene) {
        if (gene != null) {
            this.hiddenGeneName = gene.name();
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("MainGeneName", mainGeneName);
        info = info.putString("HiddenGeneName", hiddenGeneName);
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("MainGeneName")) {
            String name = info.getString("MainGeneName");
            if (name != null && !name.isEmpty()) {
                try {
                    Panda.Gene.valueOf(name);
                    this.mainGeneName = name;
                } catch (Throwable ignored) {
                }
            }
        } else if (info.keySet().contains("MainGene")) {
            try {
                int ord = info.getInt("MainGene");
                Panda.Gene[] values = Panda.Gene.values();
                if (ord >= 0 && ord < values.length) {
                    this.mainGeneName = values[ord].name();
                }
            } catch (Throwable ignored) {
            }
        }
        if (info.keySet().contains("HiddenGeneName")) {
            String name = info.getString("HiddenGeneName");
            if (name != null && !name.isEmpty()) {
                try {
                    Panda.Gene.valueOf(name);
                    this.hiddenGeneName = name;
                } catch (Throwable ignored) {
                }
            }
        } else if (info.keySet().contains("HiddenGene")) {
            try {
                int ord = info.getInt("HiddenGene");
                Panda.Gene[] values = Panda.Gene.values();
                if (ord >= 0 && ord < values.length) {
                    this.hiddenGeneName = values[ord].name();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
