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

package de.Keyle.MyPet.compat.v1_8_R3.services;

import com.google.common.collect.Sets;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.types.*;
import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagList;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Compat("v1_8_R3")
public class EntityConverterService extends de.Keyle.MyPet.api.util.service.types.EntityConverterService {

    public TagCompound convertEntity(LivingEntity entity) {
        TagCompound properties = new TagCompound();
        switch (entity.getType()) {
            case OCELOT:
                convertOcelot((Ocelot) entity, properties);
                break;
            case WOLF:
                convertWolf((Wolf) entity, properties);
                break;
            case SHEEP:
                convertSheep((Sheep) entity, properties);
                break;
            case VILLAGER:
                convertVillager((Villager) entity, properties);
                break;
            case PIG:
                convertPig((Pig) entity, properties);
                break;
            case MAGMA_CUBE:
            case SLIME:
                convertSlime((Slime) entity, properties);
                break;
            case CREEPER:
                convertCreeper((Creeper) entity, properties);
                break;
            case HORSE:
                convertHorse((Horse) entity, properties);
                break;
            case PIG_ZOMBIE:
                convertZombie((Zombie) entity, properties);
                if (Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                    convertEquipable(entity, properties);
                }
                break;
            case ENDERMAN:
                convertEnderman((Enderman) entity, properties);
                break;
            case SKELETON:
                convertSkeleton((Skeleton) entity, properties);
                if (Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                    convertEquipable(entity, properties);
                }
                break;
            case GUARDIAN:
                convertGuardian((Guardian) entity, properties);
                break;
            case RABBIT:
                convertRabbit((Rabbit) entity, properties);
                break;
        }

        if (entity instanceof Ageable) {
            convertAgable((Ageable) entity, properties);
        }

        return properties;
    }

    @Override
    public void convertEntity(MyPet myPet, LivingEntity normalEntity) {
        if (myPet instanceof MyCreeper) {
            if (((MyCreeper) myPet).isPowered()) {
                ((Creeper) normalEntity).setPowered(true);
            }
        } else if (myPet instanceof MyEnderman) {
            if (((MyEnderman) myPet).hasBlock()) {
                MaterialData materialData = new MaterialData(((MyEnderman) myPet).getBlock().getType(), ((MyEnderman) myPet).getBlock().getData().getData());
                ((Enderman) normalEntity).setCarriedMaterial(materialData);
            }
        } else if (myPet instanceof MyIronGolem) {
            ((IronGolem) normalEntity).setPlayerCreated(true);
        } else if (myPet instanceof MyMagmaCube) {
            ((MagmaCube) normalEntity).setSize(((MyMagmaCube) myPet).getSize());
        } else if (myPet instanceof MyOcelot) {
            ((Ocelot) normalEntity).setCatType(Ocelot.Type.WILD_OCELOT);
            ((Ocelot) normalEntity).setTamed(false);
        } else if (myPet instanceof MyPig) {
            ((Pig) normalEntity).setSaddle(((MyPig) myPet).hasSaddle());
        } else if (myPet instanceof MySheep) {
            ((Sheep) normalEntity).setSheared(((MySheep) myPet).isSheared());
            ((Sheep) normalEntity).setColor(((MySheep) myPet).getColor());
        } else if (myPet instanceof MyVillager) {
            MyVillager villagerPet = (MyVillager) myPet;
            Villager.Profession profession = Villager.Profession.values()[villagerPet.getProfession()];
            ((Villager) normalEntity).setProfession(profession);
            if (villagerPet.hasOriginalData()) {
                TagCompound villagerTag = MyPetApi.getPlatformHelper().entityToTag(normalEntity);
                for (String key : villagerPet.getOriginalData().getCompoundData().keySet()) {
                    villagerTag.put(key, villagerPet.getOriginalData().get(key));
                }
                MyPetApi.getPlatformHelper().applyTagToEntity(villagerTag, normalEntity);
            }
        } else if (myPet instanceof MyWolf) {
            ((Wolf) normalEntity).setTamed(false);
        } else if (myPet instanceof MySlime) {
            ((Slime) normalEntity).setSize(((MySlime) myPet).getSize());
        } else if (myPet instanceof MyZombie) {
            ((Zombie) normalEntity).setVillager(((MyZombie) myPet).isVillager());
        } else if (myPet instanceof MySkeleton) {
            ((Skeleton) normalEntity).setSkeletonType(Skeleton.SkeletonType.values()[((MySkeleton) myPet).getType()]);
            if (((MySkeleton) myPet).isWither()) {
                normalEntity.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
            } else {
                normalEntity.getEquipment().setItemInHand(new ItemStack(Material.BOW));
            }
        } else if (myPet instanceof MyPigZombie) {
            normalEntity.getEquipment().setItemInHand(new ItemStack(Material.GOLD_SWORD));
        } else if (myPet instanceof MyHorse) {
            Horse.Variant type = Horse.Variant.values()[((MyHorse) myPet).getHorseType()];
            Horse.Style style = Horse.Style.values()[(((MyHorse) myPet).getVariant() >>> 8)];
            Horse.Color color = Horse.Color.values()[(((MyHorse) myPet).getVariant() & 0xFF)];

            ((Horse) normalEntity).setVariant(type);
            ((Horse) normalEntity).setColor(color);
            ((Horse) normalEntity).setStyle(style);
            ((Horse) normalEntity).setCarryingChest(((MyHorse) myPet).hasChest());

            if (((MyHorse) myPet).hasSaddle()) {
                ((Horse) normalEntity).getInventory().setSaddle(((MyHorse) myPet).getSaddle().clone());
            }
            if (((MyHorse) myPet).hasArmor()) {
                ((Horse) normalEntity).getInventory().setArmor(((MyHorse) myPet).getArmor().clone());
            }
            ((Horse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyRabbit) {
            ((Rabbit) normalEntity).setRabbitType(((MyRabbit) myPet).getVariant().getBukkitType());
        } else if (myPet instanceof MyGuardian) {
            ((Guardian) normalEntity).setElder(((MyGuardian) myPet).isElder());
        }

        if (myPet instanceof MyPetBaby && normalEntity instanceof Ageable) {
            if (((MyPetBaby) myPet).isBaby()) {
                ((Ageable) normalEntity).setBaby();
            } else {
                ((Ageable) normalEntity).setAdult();
            }
        }
    }

    public void convertRabbit(Rabbit rabbit, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagByte(MyRabbit.RabbitType.getTypeByBukkitEnum(rabbit.getRabbitType()).getId()));
    }

    public void convertGuardian(Guardian guardian, TagCompound properties) {
        properties.getCompoundData().put("Elder", new TagByte(guardian.isElder()));
    }

    public void convertEquipable(LivingEntity entity, TagCompound properties) {
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

    public void convertAgable(Ageable ageable, TagCompound properties) {
        properties.getCompoundData().put("Baby", new TagByte(!ageable.isAdult()));
    }

    public void convertSkeleton(Skeleton skeleton, TagCompound properties) {
        properties.getCompoundData().put("Type", new TagInt(skeleton.getSkeletonType().ordinal()));
    }

    public void convertEnderman(Enderman enderman, TagCompound properties) {
        if (enderman.getCarriedMaterial().getItemType() != Material.AIR) {
            ItemStack block = enderman.getCarriedMaterial().toItemStack(1);
            properties.getCompoundData().put("Block", MyPetApi.getPlatformHelper().itemStackToCompund(block));
        }
    }

    public void convertZombie(Zombie zombie, TagCompound properties) {
        properties.getCompoundData().put("Baby", new TagByte(zombie.isBaby()));
        if (zombie.isVillager()) {
            properties.getCompoundData().put("Type", new TagInt(1));

        }
    }

    public void convertCreeper(Creeper creeper, TagCompound properties) {
        properties.getCompoundData().put("Powered", new TagByte(creeper.isPowered()));
    }

    public void convertHorse(Horse horse, TagCompound properties) {
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

    public void convertSlime(Slime slime, TagCompound properties) {
        properties.getCompoundData().put("Size", new TagInt(slime.getSize()));
    }

    public void convertPig(Pig pig, TagCompound properties) {
        properties.getCompoundData().put("Saddle", new TagByte(pig.hasSaddle()));
    }

    public void convertVillager(Villager villager, TagCompound properties) {
        int profession = villager.getProfession().ordinal();
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.10") >= 0) {
            profession--;
        }
        properties.getCompoundData().put("Profession", new TagInt(profession));

        TagCompound villagerTag = MyPetApi.getPlatformHelper().entityToTag(villager);
        Set<String> allowedTags = Sets.newHashSet("Riches", "Career", "CareerLevel", "Willing", "Inventory", "Offers");
        Set<String> keys = new HashSet<>(villagerTag.getCompoundData().keySet());
        for (String key : keys) {
            if (allowedTags.contains(key)) {
                continue;
            }
            villagerTag.remove(key);
        }
        properties.getCompoundData().put("OriginalData", villagerTag);
    }

    public void convertSheep(Sheep sheep, TagCompound properties) {
        properties.getCompoundData().put("Color", new TagInt(sheep.getColor().getDyeData()));
        properties.getCompoundData().put("Sheared", new TagByte(sheep.isSheared()));
    }

    public void convertOcelot(Ocelot ocelot, TagCompound properties) {
        properties.getCompoundData().put("CatType", new TagInt(ocelot.getCatType().getId()));
        properties.getCompoundData().put("Sitting", new TagByte(ocelot.isSitting()));
    }

    public void convertWolf(Wolf wolf, TagCompound properties) {
        properties.getCompoundData().put("Sitting", new TagByte(wolf.isSitting()));
        properties.getCompoundData().put("Tamed", new TagByte(wolf.isTamed()));
        properties.getCompoundData().put("CollarColor", new TagByte(wolf.getCollarColor().getWoolData()));
    }
}