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

package de.Keyle.MyPet.compat.v1_13_R1.services;

import com.mojang.datafixers.DataFixTypes;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.compat.v1_13_R1.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagShort;
import de.keyle.knbt.TagString;
import net.minecraft.server.v1_13_R1.GameProfileSerializer;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import net.minecraft.server.v1_13_R1.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;

@Load(Load.State.AfterHooks)
public class RepositoryMyPetConverterService extends de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService {

    public void v1_13_R1(StoredMyPet pet) {
        TagCompound skills = pet.getSkillInfo();

        if (skills.containsKey("Inventory")) {
            TagCompound invTag = skills.getAs("Inventory", TagCompound.class);
            skills.remove("Inventory");
            skills.put("Backpack", invTag);

            TagList items = invTag.getAs("Items", TagList.class);
            for (TagCompound item : items.getListAs(TagCompound.class)) {
                updateItemId(item);
            }
            try {
                invTag.put("Items", updateItemstacks(items));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        pet.setSkills(skills);

        TagCompound info = pet.getInfo();

        switch (pet.getPetType()) {
            case Horse:
                if (info.containsKeyAs("Armor", TagCompound.class)) {
                    TagCompound itemTag = info.get("Armor");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Armor", itemTag);
                }
            case Donkey:
            case Mule:
                if (info.containsKeyAs("Chest", TagCompound.class)) {
                    TagCompound itemTag = info.get("Chest");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Chest", itemTag);
                }
            case SkeletonHorse:
            case ZombieHorse:
            case Pig:
                if (info.containsKeyAs("Saddle", TagCompound.class)) {
                    TagCompound itemTag = info.get("Saddle");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Saddle", itemTag);
                }
                break;
            case Enderman:
                if (info.containsKeyAs("Block", TagCompound.class)) {
                    TagCompound itemTag = info.get("Block");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Block", itemTag);
                }
                break;
            case IronGolem:
                if (info.containsKeyAs("Flower", TagCompound.class)) {
                    TagCompound itemTag = info.get("Flower");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Flower", itemTag);
                }
                break;
            case Llama:
                if (info.containsKeyAs("Chest", TagCompound.class)) {
                    TagCompound itemTag = info.get("Chest");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Chest", itemTag);
                }
                if (info.containsKeyAs("Decor", TagCompound.class)) {
                    TagCompound itemTag = info.get("Decor");
                    updateItemId(itemTag);
                    itemTag = updateItemstack(itemTag);
                    info.put("Decor", itemTag);
                }
                break;
        }

        pet.setInfo(info);
    }

    public TagCompound updateItemstack(TagCompound item) {
        TagList chestList = new TagList();
        chestList.addTag(item);
        chestList = updateItemstacks(chestList);
        return chestList.getTag(0);
    }


    public TagList updateItemstacks(TagList items) {
        NBTTagCompound fakePlayer = new NBTTagCompound();
        fakePlayer.set("Inventory", ItemStackNBTConverter.compoundToVanillaCompound(items));

        World w = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
        NBTTagCompound updatedFakePlayer = GameProfileSerializer.a(w.getDataManager().i(), DataFixTypes.PLAYER, fakePlayer, 1343);

        return (TagList) ItemStackNBTConverter.vanillaCompoundToCompound(updatedFakePlayer.get("Inventory"));
    }

    public void updateItemId(TagCompound item) {
        short damage = item.getAs("Damage", TagShort.class).getShortData();
        int id = item.getAs("id", TagShort.class).getShortData();

        ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
        MaterialHolder materialHolder = itemDatabase.getByLegacyId(id, damage);
        if (materialHolder == null) {
            materialHolder = itemDatabase.getByLegacyId(id);
        }
        if (materialHolder != null) {
            item.put("id", new TagString("minecraft:" + materialHolder.getLegacyName().getName()));
        }
    }
}
