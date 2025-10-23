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

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;
import org.bukkit.entity.Panda;

public class MyPanda extends MyPet implements de.Keyle.MyPet.api.entity.types.MyPanda {

    protected boolean isBaby = false;
    protected Panda.Gene mainGene = Panda.Gene.NORMAL;
    protected Panda.Gene hiddenGene = Panda.Gene.NORMAL;

    public MyPanda(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Panda.Gene getMainGene() {
        return mainGene;
    }

    @Override
    public void setMainGene(Panda.Gene gene) {
        this.mainGene = gene;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public Panda.Gene getHiddenGene() {
        return hiddenGene;
    }

    @Override
    public void setHiddenGene(Panda.Gene gene) {
        this.hiddenGene = gene;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        this.isBaby = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        info.getCompoundData().put("MainGene", new TagInt(getMainGene().ordinal()));
        info.getCompoundData().put("HiddenGene", new TagInt(getHiddenGene().ordinal()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.containsKey("MainGene")) {
            setMainGene(Panda.Gene.values()[info.getAs("MainGene", TagInt.class).getIntData()]);
        }
        if (info.containsKey("HiddenGene")) {
            setHiddenGene(Panda.Gene.values()[info.getAs("HiddenGene", TagInt.class).getIntData()]);
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Panda;
    }

    @Override
    public String toString() {
        return "MyPanda{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", baby=" + isBaby() + "}";
    }
}