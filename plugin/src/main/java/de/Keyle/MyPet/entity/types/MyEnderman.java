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
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
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
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        if (block != null && block.getType() != AIR) {
            info = info.put("Block", MyPetApi.getPlatformHelper().itemStackToCompound(block));
        }
        return info.putBoolean("Screaming", permaScreaming);
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("BlockName")) {
            ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
            String id = info.getString("BlockName");
            MaterialHolder materialHolder = itemDatabase.getByID(id);
            if (materialHolder != null) {
                Material material = materialHolder.getMaterial();
                if (material != null) {
                    setBlock(new ItemStack(material, 1));
                }
            }
        } else if (info.keySet().contains("BlockID")) {
            int id = info.getInt("BlockID");
            byte data = 0;
            if (info.keySet().contains("BlockData")) {
                data = (byte) info.getInt("BlockData");
            }

            ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
            MaterialHolder materialHolder = itemDatabase.getByLegacyId(id, data);
            if (materialHolder != null) {
                Material material = materialHolder.getMaterial();
                setBlock(new ItemStack(material, 1));
            }
        } else if (info.keySet().contains("Block")) {
            CompoundBinaryTag itemStackCompund = info.getCompound("Block");
            try {
                ItemStack block = MyPetApi.getPlatformHelper().compoundToItemStack(itemStackCompund);
                setBlock(block);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Block item from pet data!");
            }
        }
        if (info.keySet().contains("Screaming")) {
            setPermaScreaming(info.getBoolean("Screaming"));
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
