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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.*;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Material.AIR;

@Getter
public class MyEnderman extends MyPet implements de.Keyle.MyPet.api.entity.types.MyEnderman {

    protected boolean screaming = false;
    protected boolean permaScreaming = false;
    protected ItemStack block = null;

    public MyEnderman(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setBlock(ItemStack block) {
        if (block != null) {
            this.block = block.clone();
            this.block.setAmount(1);

            if (status == PetState.Here) {
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
            }
        } else {
            this.block = null;
            if (status == PetState.Here) {
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
            }
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        if (block != null && block.getType() != AIR) {
            info.getCompoundData().put("Block", MyPetApi.getPlatformHelper().itemStackToCompund(block));
        }
        info.getCompoundData().put("Screaming", new TagByte(permaScreaming));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("BlockName")) {
            ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
            String id = info.getAs("BlockName", TagString.class).getStringData();
            MaterialHolder materialHolder = itemDatabase.getByID(id);
            if (materialHolder != null) {
                Material material = materialHolder.getMaterial();
                if (material != null) {
                    setBlock(new ItemStack(material, 1));
                }
            }
        } else if (info.containsKey("BlockID")) {
            int id;
            byte data = 0;
            if (info.containsKeyAs("BlockID", TagShort.class)) {
                id = info.getAs("BlockID", TagShort.class).getShortData();
            } else {
                id = info.getAs("BlockID", TagInt.class).getIntData();
            }
            if (info.containsKeyAs("BlockData", TagShort.class)) {
                data = (byte) info.getAs("BlockData", TagShort.class).getShortData();
            } else if (info.containsKeyAs("BlockData", TagInt.class)) {
                data = (byte) info.getAs("BlockData", TagInt.class).getIntData();
            }

            ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
            MaterialHolder materialHolder = itemDatabase.getByLegacyId(id, data);
            if (materialHolder != null) {
                Material material = materialHolder.getMaterial();
                setBlock(new ItemStack(material, 1));
            }
        } else if (info.containsKey("Block")) {
            TagCompound itemStackCompund = info.getAs("Block", TagCompound.class);
            try {
                ItemStack block = MyPetApi.getPlatformHelper().compundToItemStack(itemStackCompund);
                setBlock(block);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Block item from pet data!");
            }
        }
        if (info.containsKey("Screaming")) {
            setPermaScreaming(info.getAs("Screaming", TagByte.class).getBooleanData());
        }
    }

    // Custom logic - Lombok would return field directly but we need to combine both flags
    public boolean isScreaming() {
        return screaming || permaScreaming;
    }

    public void setScreaming(boolean flag) {
        if (!flag && permaScreaming)
            return;

        this.screaming = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public void setPermaScreaming(boolean flag) {
        this.permaScreaming = flag;
        this.setScreaming(flag);
    }

    public boolean hasBlock() {
        return block != null;
    }
}
