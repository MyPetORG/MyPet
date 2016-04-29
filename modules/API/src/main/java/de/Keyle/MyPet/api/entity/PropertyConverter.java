/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
 * MyPet is licensed under the GNU Lesser General public static License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General public static License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General public static License for more details.
 *
 * You should have received a copy of the GNU General public static License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.types.MyRabbit;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagList;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PropertyConverter {
    private static Random random = new Random();
    
    public static TagCompound convertEntity(LivingEntity entity) {
        TagCompound properties = new TagCompound();
        switch (entity.getType().name()) {
            case "OCELOT":
                convertOcelot((Ocelot) entity, properties);
                break;
            case "WOLF":
                convertWolf((Wolf) entity, properties);
                break;
            case "SHEEP":
                convertSheep((Sheep) entity, properties);
                break;
            case "VILLAGER":
                convertVillager((Villager) entity, properties);
                break;
            case "PIG":
                convertPig((Pig) entity, properties);
                break;
            case "MAGMA_CUBE":
            case "SLIME":
                convertSlime((Slime) entity, properties);
                break;
            case "CREEPER":
                convertCreeper((Creeper) entity, properties);
                break;
            case "HORSE":
                convertHorse((Horse) entity, properties);
                break;
            case "ZOMBIE":
            case "PIG_ZOMBIE":
                convertZombie((Zombie) entity, properties);
                if(Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                    convertEquipable(entity, properties);
                }
                break;
            case "ENDERMAN":
                convertEnderman((Enderman) entity, properties);
                break;
            case "SKELETON":
                convertSkeleton((Skeleton) entity, properties);
                if(Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                    convertEquipable(entity, properties);    
                }
                break;
            case "GUARDIAN":
                convertGuardian((Guardian) entity, properties);
                break;
            case "RABBIT":
                convertRabbit((Rabbit) entity, properties);
                break;
        }
        
        if (entity instanceof Ageable) {
            convertAgable((Ageable) entity, properties);
        }

        return properties;
    }

    public static void convertRabbit(Rabbit rabbit, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagByte(MyRabbit.RabbitType.getTypeByBukkitEnum(rabbit.getRabbitType()).getId()));
    }

    public static void convertGuardian(Guardian guardian, TagCompound properties) {
        properties.getCompoundData().put("Elder", new TagByte(guardian.isElder()));
    }

    public static void convertEquipable(LivingEntity entity, TagCompound properties) {
        List<TagCompound> equipmentList = new ArrayList<>();
        if (random.nextFloat() <= entity.getEquipment().getChestplateDropChance()) {
            ItemStack itemStack = entity.getEquipment().getChestplate();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(itemStack);
                item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Chestplate.getSlotId()));
                equipmentList.add(item);
            }
        }
        if (random.nextFloat() <= entity.getEquipment().getHelmetDropChance()) {
            ItemStack itemStack = entity.getEquipment().getHelmet();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(itemStack);
                item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Helmet.getSlotId()));
                equipmentList.add(item);
            }
        }
        if (random.nextFloat() <= entity.getEquipment().getLeggingsDropChance()) {
            ItemStack itemStack = entity.getEquipment().getLeggings();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(itemStack);
                item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Leggins.getSlotId()));
                equipmentList.add(item);
            }
        }
        if (random.nextFloat() <= entity.getEquipment().getBootsDropChance()) {
            ItemStack itemStack = entity.getEquipment().getBoots();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(itemStack);
                item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Boots.getSlotId()));
                equipmentList.add(item);
            }
        }
        properties.getCompoundData().put("Equipment", new TagList(equipmentList));
    }

    public static void convertAgable(Ageable ageable, TagCompound properties) {
        properties.getCompoundData().put("Baby", new TagByte(!ageable.isAdult()));
    }

    public static void convertSkeleton(Skeleton skeleton, TagCompound properties) {
        properties.getCompoundData().put("Wither", new TagByte(skeleton.getSkeletonType() == Skeleton.SkeletonType.WITHER));
    }

    public static void convertEnderman(Enderman enderman, TagCompound properties) {
        if (enderman.getCarriedMaterial().getItemType() != Material.AIR) {
            ItemStack block = enderman.getCarriedMaterial().toItemStack(1);
            properties.getCompoundData().put("Block", MyPetApi.getPlatformHelper().itemStackToCompund(block));
        }
    }

    public static void convertZombie(Zombie zombie, TagCompound properties) {
        properties.getCompoundData().put("Baby", new TagByte(zombie.isBaby()));
        if (MyPetApi.getCompatUtil().getMinecraftVersion() >= 19) {
            if (zombie.isVillager()) {
                properties.getCompoundData().put("Profession", new TagInt(zombie.getVillagerProfession().ordinal() + 1));
            }
        } else {
            properties.getCompoundData().put("Villager", new TagByte(zombie.isVillager()));
        }
    }

    public static void convertCreeper(Creeper creeper, TagCompound properties) {
        properties.getCompoundData().put("Powered", new TagByte(creeper.isPowered()));
    }

    public static void convertHorse(Horse horse, TagCompound properties) {
        byte type = (byte) horse.getVariant().ordinal();
        int style = horse.getStyle().ordinal();
        int color = horse.getColor().ordinal();
        int variant = color & 255 | style << 8;

        if (horse.getInventory().getArmor() != null) {
            TagCompound armor = MyPetApi.getPlatformHelper().itemStackToCompund(horse.getInventory().getArmor());
            properties.getCompoundData().put("Armor", armor);
        }
        if (horse.getInventory().getSaddle() != null) {
            TagCompound saddle = MyPetApi.getPlatformHelper().itemStackToCompund(horse.getInventory().getSaddle());
            properties.getCompoundData().put("Saddle", saddle);
        }

        properties.getCompoundData().put("Type", new TagByte(type));
        properties.getCompoundData().put("Variant", new TagInt(variant));
        properties.getCompoundData().put("Chest", new TagByte(horse.isCarryingChest()));
        properties.getCompoundData().put("Age", new TagInt(horse.getAge()));

        if (horse.isCarryingChest()) {
            ItemStack[] contents = horse.getInventory().getContents();
            for (int i = 2; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null) {
                    horse.getWorld().dropItem(horse.getLocation(), item);
                }
            }
        }
    }

    public static void convertSlime(Slime slime, TagCompound properties) {
        properties.getCompoundData().put("Size", new TagInt(slime.getSize()));
    }

    public static void convertPig(Pig pig, TagCompound properties) {
        properties.getCompoundData().put("Saddle", new TagByte(pig.hasSaddle()));
    }

    public static void convertVillager(Villager villager, TagCompound properties) {
        properties.getCompoundData().put("Profession", new TagInt(villager.getProfession().getId()));

        TagCompound villagerTag = MyPetApi.getPlatformHelper().entityToTag(villager);
        String[] allowedTags = {"Riches", "Career", "CareerLevel", "Willing", "Inventory", "Offers"};
        Set<String> keys = new HashSet<>(villagerTag.getCompoundData().keySet());
        for (String key : keys) {
            if (Arrays.binarySearch(allowedTags, key) > -1) {
                continue;
            }
            villagerTag.remove(key);
        }
        properties.getCompoundData().put("OriginalData", villagerTag);
    }

    public static void convertSheep(Sheep sheep, TagCompound properties) {
        properties.getCompoundData().put("Color", new TagInt(sheep.getColor().getDyeData()));
        properties.getCompoundData().put("Sheared", new TagByte(sheep.isSheared()));    
    }

    public static void convertOcelot(Ocelot ocelot, TagCompound properties) {
        properties.getCompoundData().put("CatType", new TagInt(ocelot.getCatType().getId()));
        properties.getCompoundData().put("Sitting", new TagByte(ocelot.isSitting()));    
    }
    
    public static void convertWolf(Wolf wolf, TagCompound properties) {
        properties.getCompoundData().put("Sitting", new TagByte(wolf.isSitting()));
        properties.getCompoundData().put("Tamed", new TagByte(wolf.isTamed()));
        properties.getCompoundData().put("CollarColor", new TagByte(wolf.getCollarColor().getWoolData()));
    }
}