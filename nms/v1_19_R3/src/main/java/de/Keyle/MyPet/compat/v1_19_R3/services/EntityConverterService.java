/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_19_R3.services;

import com.google.common.collect.Sets;
import com.mojang.serialization.Dynamic;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import org.bukkit.inventory.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.types.*;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_19_R3.util.inventory.ItemStackNBTConverter;
import net.kyori.adventure.nbt.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.trading.MerchantOffers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftTropicalFish;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftVillagerZombie;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EntityConverterService extends de.Keyle.MyPet.api.util.service.types.EntityConverterService {

    @Override
    public CompoundBinaryTag convertEntity(LivingEntity entity) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
        switch (entity.getType()) {
            case WOLF:
                convertWolf((Wolf) entity, builder);
                break;
            case SHEEP:
                convertSheep((Sheep) entity, builder);
                break;
            case VILLAGER:
                convertVillager((Villager) entity, builder);
                break;
            case PIG:
                convertPig((Pig) entity, builder);
                break;
            case MAGMA_CUBE:
            case SLIME:
                convertSlime((Slime) entity, builder);
                break;
            case CREEPER:
                convertCreeper((Creeper) entity, builder);
                break;
            case HORSE:
                convertHorse((Horse) entity, builder);
                break;
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
                convertSaddledHorse((AbstractHorse) entity, builder);
                break;
            case MULE:
            case DONKEY:
                convertChestedHorse((ChestedHorse) entity, builder);
                break;
            case ZOMBIE_VILLAGER:
                convertZombieVillager((ZombieVillager) entity, builder);
            case HUSK:
            case ZOMBIE:
            case ZOMBIFIED_PIGLIN:
            case DROWNED:
                convertZombie((Zombie) entity, builder);
                if (Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                    convertEquipable(entity, builder);
                }
                break;
            case ENDERMAN:
                convertEnderman((Enderman) entity, builder);
                break;
            case RABBIT:
                convertRabbit((Rabbit) entity, builder);
                break;
            case LLAMA:
                convertLlama((Llama) entity, builder);
                break;
            case AXOLOTL:
                convertAxolotl((Axolotl) entity, builder);
                break;
            case GOAT:
                convertGoat((Goat) entity, builder);
                break;
            case PARROT:
                convertParrot((Parrot) entity, builder);
                break;
            case TROPICAL_FISH:
                convertTropicalFish((TropicalFish) entity, builder);
                break;
            case PUFFERFISH:
                convertPufferFish((PufferFish) entity, builder);
                break;
            case PHANTOM:
                convertPhantom((Phantom) entity, builder);
                break;
            case CAT:
                convertCat((Cat) entity, builder);
                break;
            case MUSHROOM_COW:
                convertMushroomCow((MushroomCow) entity, builder);
                break;
            case FOX:
                convertFox((Fox) entity, builder);
                break;
            case FROG:
                convertFrog((Frog) entity, builder);
                break;
            case PANDA:
                convertPanda((Panda) entity, builder);
                break;
            case WANDERING_TRADER:
                convertWanderingTrader((WanderingTrader) entity, builder);
                break;
            case BEE:
                convertBee((Bee) entity, builder);
                break;
            case TRADER_LLAMA:
                convertTraderLlama((TraderLlama) entity, builder);
                break;
        }

        if (entity instanceof Ageable) {
            convertAgable((Ageable) entity, builder);
        }

        return builder.build();
    }

    @Override
    public void convertEntity(MyPet myPet, LivingEntity normalEntity) {
        if (myPet instanceof MyCreeper creeper) {
            if (creeper.isPowered()) {
                ((Creeper) normalEntity).setPowered(true);
            }
        } else if (myPet instanceof MyGoat goat) {
            if (goat.isScreaming()) {
                ((Goat) normalEntity).setScreaming(true);
            }
            if (!goat.hasLeftHorn()) {
                ((Goat) normalEntity).setLeftHorn(false);
            }
            if (!goat.hasRightHorn()) {
                ((Goat) normalEntity).setRightHorn(false);
            }
        } else if (myPet instanceof MyEnderman enderman) {
            if (enderman.hasBlock()) {
                ((Enderman) normalEntity).setCarriedBlock(enderman.getBlock().getType().createBlockData());
            }
        } else if (myPet instanceof MyIronGolem) {
            ((IronGolem) normalEntity).setPlayerCreated(true);
        } else if (myPet instanceof MyMagmaCube magmaCube) {
            ((MagmaCube) normalEntity).setSize(magmaCube.getSize());
        } else if (myPet instanceof MyPig pig) {
            ((Pig) normalEntity).setSaddle(pig.hasSaddle());
        } else if (myPet instanceof MySheep sheep) {
            ((Sheep) normalEntity).setSheared(sheep.isSheared());
            ((Sheep) normalEntity).setColor(sheep.getColor());
        } else if (myPet instanceof MyVillager villagerPet) {
            Villager villagerEntity = ((Villager) normalEntity);

            Villager.Profession profession = Villager.Profession.values()[villagerPet.getProfession()];
            Villager.Type type = Villager.Type.values()[villagerPet.getType().ordinal()];
            villagerEntity.setVillagerType(type);
            villagerEntity.setProfession(profession);
            villagerEntity.setVillagerLevel(villagerPet.getLevel());

            if (villagerPet.hasOriginalData()) {
                CompoundBinaryTag villagerTag = villagerPet.getOriginalData();

                net.minecraft.world.entity.npc.Villager entityVillager = ((CraftVillager) villagerEntity).getHandle();

                try {
                    if (villagerTag.keySet().contains("Offers")) {
                        CompoundBinaryTag offersTag = villagerTag.getCompound("Offers");
                        CompoundTag vanillaNBT = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(offersTag);
                        entityVillager.setOffers(new MerchantOffers(vanillaNBT));
                    }
                    if (villagerTag.keySet().contains("Inventory")) {
                        ListBinaryTag inventoryTag = villagerTag.getList("Inventory");
                        ListTag vanillaNBT = (ListTag) ItemStackNBTConverter.compoundToVanillaCompound(inventoryTag);
                        for (int i = 0; i < vanillaNBT.size(); ++i) {
                            net.minecraft.world.item.ItemStack itemstack = net.minecraft.world.item.ItemStack.of(vanillaNBT.getCompound(i));
                            ItemStack item = CraftItemStack.asCraftMirror(itemstack);
                            if (!itemstack.isEmpty()) {
                                Villager vill = ((Villager) Bukkit.getServer().getEntity(normalEntity.getUniqueId()));
                                vill.getInventory().addItem(item);
                            }
                        }
                    }
                    if (villagerTag.keySet().contains("FoodLevel")) {
                        byte foodLevel = villagerTag.getByte("FoodLevel");
                        ReflectionUtil.setFieldValue("cn", entityVillager, foodLevel);        // Field: foodLevel
                    }
                    if (villagerTag.keySet().contains("Gossips")) {
                        ListBinaryTag inventoryTag = villagerTag.getList("Gossips");
                        ListTag vanillaNBT = (ListTag) ItemStackNBTConverter.compoundToVanillaCompound(inventoryTag);
                        entityVillager.getGossips().update(new Dynamic<>(NbtOps.INSTANCE, vanillaNBT));
                    }
                    if (villagerTag.keySet().contains("LastRestock")) {
                        long lastRestock = villagerTag.getLong("LastRestock");
                        ReflectionUtil.setFieldValue("cs", entityVillager, lastRestock);    //Field: lastRestockGameTime
                    }
                    if (villagerTag.keySet().contains("LastGossipDecay")) {
                        long lastGossipDecay = villagerTag.getLong("LastGossipDecay");
                        ReflectionUtil.setFieldValue("cq", entityVillager, lastGossipDecay);    //Field: lastGossipDecayTime
                    }
                    if (villagerTag.keySet().contains("RestocksToday")) {
                        int restocksToday = villagerTag.getInt("RestocksToday");
                        ReflectionUtil.setFieldValue("ct", entityVillager, restocksToday);        //Field: numberOfRestocksToday
                    }
                    ReflectionUtil.setFieldValue("cv", entityVillager, true); // Field: AssignProfessionWhenSpawned
                } catch (Exception e) {
                    ErrorUtil.report(e);
                }
                if (villagerTag.keySet().contains("Xp")) {
                    int xp = villagerTag.getInt("Xp");
                    entityVillager.setVillagerXp(xp);
                }
            }
        } else if (myPet instanceof MySlime slime) {
            ((Slime) normalEntity).setSize(slime.getSize());
        } else if (myPet instanceof MyZombieVillager zombieVillager) {
            Villager.Profession profession = Villager.Profession.values()[zombieVillager.getProfession()];
            net.minecraft.world.entity.monster.ZombieVillager nmsEntity = ((CraftVillagerZombie) normalEntity).getHandle();
            nmsEntity.setVillagerData(nmsEntity.getVillagerData()
                    .setType(BuiltInRegistries.VILLAGER_TYPE.get(new ResourceLocation(zombieVillager.getType().name().toLowerCase(Locale.ROOT))))
                    .setLevel(zombieVillager.getTradingLevel())
                    .setProfession(BuiltInRegistries.VILLAGER_PROFESSION.get(new ResourceLocation(profession.name().toLowerCase(Locale.ROOT)))));
        } else if (myPet instanceof MyWitherSkeleton) {
            normalEntity.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
        } else if (myPet instanceof MySkeleton) {
            normalEntity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
        } else if (myPet instanceof MyHorse horse) {
            Horse.Style style = Horse.Style.values()[(horse.getVariant() >>> 8)];
            Horse.Color color = Horse.Color.values()[(horse.getVariant() & 0xFF)];

            ((Horse) normalEntity).setColor(color);
            ((Horse) normalEntity).setStyle(style);

            if (horse.hasSaddle()) {
                ((Horse) normalEntity).getInventory().setSaddle(horse.getSaddle().clone());
            }
            if (horse.hasArmor()) {
                ((Horse) normalEntity).getInventory().setArmor(horse.getArmor().clone());
            }
            ((Horse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MySkeletonHorse skeletonHorse) {
            if (skeletonHorse.hasSaddle()) {
                ((SkeletonHorse) normalEntity).getInventory().setSaddle(skeletonHorse.getSaddle().clone());
            }
            ((SkeletonHorse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyZombieHorse zombieHorse) {
            if (zombieHorse.hasSaddle()) {
                ((ZombieHorse) normalEntity).getInventory().setSaddle(zombieHorse.getSaddle().clone());
            }
            ((ZombieHorse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyDonkey donkey) {
            if (donkey.hasSaddle()) {
                ((Donkey) normalEntity).getInventory().setSaddle(donkey.getSaddle().clone());
            }
            if (donkey.hasChest()) {
                ((Donkey) normalEntity).setCarryingChest(true);
            }
            ((Donkey) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyMule mule) {
            if (mule.hasSaddle()) {
                ((Mule) normalEntity).getInventory().setSaddle(mule.getSaddle().clone());
            }
            if (mule.hasChest()) {
                ((Mule) normalEntity).setCarryingChest(true);
            }
            ((Mule) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyLlama llama) {
            ((Llama) normalEntity).setColor(Llama.Color.values()[Math.max(0, Math.min(3, llama.getVariant()))]);
            ((Llama) normalEntity).setCarryingChest(llama.hasChest());

            if (llama.hasDecor()) {
                ((Llama) normalEntity).getInventory().setDecor(llama.getDecor());
            }
            ((Llama) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyTraderLlama traderLlama) {
            ((TraderLlama) normalEntity).setColor(TraderLlama.Color.values()[Math.max(0, Math.min(3, traderLlama.getVariant()))]);
            ((TraderLlama) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyRabbit rabbit) {
            ((Rabbit) normalEntity).setRabbitType(rabbit.getVariant().getBukkitType());
        } else if (myPet instanceof MyParrot parrot) {
            ((Parrot) normalEntity).setVariant(Parrot.Variant.values()[parrot.getVariant()]);
        } else if (myPet instanceof MyAxolotl axolotl) {
            ((Axolotl) normalEntity).setVariant(Axolotl.Variant.values()[axolotl.getVariant()]);
        } else if (myPet instanceof MyTropicalFish tropicalFish) {
            ((CraftTropicalFish) normalEntity).getHandle().setPackedVariant(tropicalFish.getVariant());
        } else if (myPet instanceof MyPufferfish pufferfish) {
            ((PufferFish) normalEntity).setPuffState(pufferfish.getPuffState().ordinal());
        } else if (myPet instanceof MyPhantom phantom) {
            ((Phantom) normalEntity).setSize(phantom.getSize());
        } else if (myPet instanceof MyCat cat) {
            ((Cat) normalEntity).setCatType(cat.getCatType());
            ((Cat) normalEntity).setCollarColor(cat.getCollarColor());
        } else if (myPet instanceof MyMooshroom mooshroom) {
            ((MushroomCow) normalEntity).setVariant(MushroomCow.Variant.values()[mooshroom.getType().ordinal()]);
        } else if (myPet instanceof MyPanda panda) {
            ((Panda) normalEntity).setMainGene(panda.getMainGene());
            ((Panda) normalEntity).setHiddenGene(panda.getHiddenGene());
        } else if (myPet instanceof MyWanderingTrader traderPet) {
            if (traderPet.hasOriginalData()) {
                CompoundBinaryTag villagerTag = MyPetApi.getPlatformHelper().entityToTag(normalEntity);
                CompoundBinaryTag.Builder mergedBuilder = CompoundBinaryTag.builder();
                mergedBuilder.put(villagerTag);
                mergedBuilder.put(traderPet.getOriginalData());
                MyPetApi.getPlatformHelper().applyTagToEntity(mergedBuilder.build(), normalEntity);
            }
        } else if (myPet instanceof MyBee bee) {
            ((Bee) normalEntity).setHasNectar(bee.hasNectar());
            ((Bee) normalEntity).setHasStung(bee.hasStung());
        } else if (myPet instanceof MyFox fox) {
            ((Fox) normalEntity).setFoxType(fox.getFoxType());
        } else if (myPet instanceof MyFrog frog) {
            ((Frog) normalEntity).setVariant(Frog.Variant.values()[frog.getFrogVariant()]);
        }

        if (myPet instanceof MyPetBaby baby && normalEntity instanceof Ageable ageable) {
            if (baby.isBaby()) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }
    }

    private void convertLlama(Llama llama, CompoundBinaryTag.Builder builder) {
        builder.putInt("Variant", llama.getColor().ordinal());
        if (llama.getInventory().getDecor() != null && llama.getInventory().getDecor().getType() != Material.AIR) {
            builder.put("Decor", MyPetApi.getPlatformHelper().itemStackToCompound(llama.getInventory().getDecor()));
        }
        if (llama.isCarryingChest()) {
            builder.put("Chest", MyPetApi.getPlatformHelper().itemStackToCompound(new ItemStack(Material.CHEST)));
        }
    }

    private void convertTraderLlama(TraderLlama tLlama, CompoundBinaryTag.Builder builder) {
        builder.putInt("Variant", tLlama.getColor().ordinal());
    }

    private void convertAxolotl(Axolotl axolotl, CompoundBinaryTag.Builder builder) {
        builder.putInt("Variant", axolotl.getVariant().ordinal());
    }

    private void convertParrot(Parrot parrot, CompoundBinaryTag.Builder builder) {
        builder.putInt("Variant", parrot.getVariant().ordinal());
    }

    public void convertRabbit(Rabbit rabbit, CompoundBinaryTag.Builder builder) {
        builder.putByte("Variant", MyRabbit.RabbitType.getTypeByBukkitEnum(rabbit.getRabbitType()).getId());
    }

    public void convertEquipable(LivingEntity entity, CompoundBinaryTag.Builder builder) {
        List<CompoundBinaryTag> equipmentList = new ArrayList<>();
        if (random.nextFloat() <= entity.getEquipment().getChestplateDropChance()) {
            ItemStack itemStack = entity.getEquipment().getChestplate();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(itemStack);
                item = item.putString("Slot", EquipmentSlot.CHEST.name());
                equipmentList.add(item);
            }
        }
        if (random.nextFloat() <= entity.getEquipment().getHelmetDropChance()) {
            ItemStack itemStack = entity.getEquipment().getHelmet();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(itemStack);
                item = item.putString("Slot", EquipmentSlot.HEAD.name());
                equipmentList.add(item);
            }
        }
        if (random.nextFloat() <= entity.getEquipment().getLeggingsDropChance()) {
            ItemStack itemStack = entity.getEquipment().getLeggings();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(itemStack);
                item = item.putString("Slot", EquipmentSlot.LEGS.name());
                equipmentList.add(item);
            }
        }
        if (random.nextFloat() <= entity.getEquipment().getBootsDropChance()) {
            ItemStack itemStack = entity.getEquipment().getBoots();
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(itemStack);
                item = item.putString("Slot", EquipmentSlot.FEET.name());
                equipmentList.add(item);
            }
        }
        builder.put("Equipment", ListBinaryTag.from(equipmentList));
    }

    public void convertAgable(Ageable ageable, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Baby", !ageable.isAdult());
    }

    public void convertEnderman(Enderman enderman, CompoundBinaryTag.Builder builder) {
        if (enderman.getCarriedBlock() != null) {
            ItemStack block = enderman.getCarriedMaterial().toItemStack(1);
            builder.put("Block", MyPetApi.getPlatformHelper().itemStackToCompound(block));
        }
    }

    public void convertZombieVillager(ZombieVillager zombie, CompoundBinaryTag.Builder builder) {
        builder.putInt("Profession", zombie.getVillagerProfession().ordinal());

        CompoundBinaryTag villagerTag = MyPetApi.getPlatformHelper().entityToTag(zombie);
        Set<String> allowedTags = Sets.newHashSet("VillagerData");
        CompoundBinaryTag.Builder filteredBuilder = CompoundBinaryTag.builder();
        for (String key : villagerTag.keySet()) {
            if (allowedTags.contains(key)) {
                filteredBuilder.put(key, villagerTag.get(key));
            }
        }
        builder.put("VillagerData", filteredBuilder.build());
    }

    public void convertZombie(Zombie zombie, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Baby", zombie.isBaby());
    }

    public void convertCreeper(Creeper creeper, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Powered", creeper.isPowered());
    }

    public void convertHorse(Horse horse, CompoundBinaryTag.Builder builder) {
        int style = horse.getStyle().ordinal();
        int color = horse.getColor().ordinal();
        int variant = color & 255 | style << 8;
        builder.putInt("Variant", variant);

        // Write equipment using unified Equipment list format
        List<CompoundBinaryTag> equipmentList = new ArrayList<>();
        if (horse.getInventory().getArmor() != null && horse.getInventory().getArmor().getType() != Material.AIR) {
            CompoundBinaryTag armor = MyPetApi.getPlatformHelper().itemStackToCompound(horse.getInventory().getArmor());
            armor = armor.putString("Slot", "BODY");
            equipmentList.add(armor);
        }
        if (horse.getInventory().getSaddle() != null && horse.getInventory().getSaddle().getType() != Material.AIR) {
            CompoundBinaryTag saddle = MyPetApi.getPlatformHelper().itemStackToCompound(horse.getInventory().getSaddle());
            saddle = saddle.putString("Slot", "SADDLE");
            equipmentList.add(saddle);
        }
        if (!equipmentList.isEmpty()) {
            builder.put("Equipment", ListBinaryTag.from(equipmentList));
        }

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

    public void convertSaddledHorse(AbstractHorse horse, CompoundBinaryTag.Builder builder) {
        // Write equipment using unified Equipment list format
        if (horse.getInventory().getSaddle() != null && horse.getInventory().getSaddle().getType() != Material.AIR) {
            List<CompoundBinaryTag> equipmentList = new ArrayList<>();
            CompoundBinaryTag saddle = MyPetApi.getPlatformHelper().itemStackToCompound(horse.getInventory().getSaddle());
            saddle = saddle.putString("Slot", "SADDLE");
            equipmentList.add(saddle);
            builder.put("Equipment", ListBinaryTag.from(equipmentList));
        }
    }

    public void convertChestedHorse(ChestedHorse horse, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Chest", horse.isCarryingChest());

        // Write saddle using unified Equipment list format
        if (horse.getInventory().getSaddle() != null && horse.getInventory().getSaddle().getType() != Material.AIR) {
            List<CompoundBinaryTag> equipmentList = new ArrayList<>();
            CompoundBinaryTag saddle = MyPetApi.getPlatformHelper().itemStackToCompound(horse.getInventory().getSaddle());
            saddle = saddle.putString("Slot", "SADDLE");
            equipmentList.add(saddle);
            builder.put("Equipment", ListBinaryTag.from(equipmentList));
        }

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

    public void convertSlime(Slime slime, CompoundBinaryTag.Builder builder) {
        builder.putInt("Size", slime.getSize());
    }

    public void convertPig(Pig pig, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Saddle", pig.hasSaddle());
    }

    public void convertVillager(Villager villager, CompoundBinaryTag.Builder builder) {
        int profession = villager.getProfession().ordinal();
        builder.putInt("Profession", profession);
        int type = villager.getVillagerType().ordinal();
        builder.putInt("VillagerType", type);
        int level = villager.getVillagerLevel();
        builder.putInt("VillagerLevel", level);

        CompoundBinaryTag villagerTag = MyPetApi.getPlatformHelper().entityToTag(villager);
        Set<String> allowedTags = Sets.newHashSet(
                "RestocksToday",
                "FoodLevel",
                "Gossips",
                "Offers",
                "LastRestock",
                "Inventory",
                "Xp"
        );
        CompoundBinaryTag.Builder filteredBuilder = CompoundBinaryTag.builder();
        for (String key : villagerTag.keySet()) {
            if (allowedTags.contains(key)) {
                filteredBuilder.put(key, villagerTag.get(key));
            }
        }
        builder.put("OriginalData", filteredBuilder.build());
    }

    public void convertWanderingTrader(WanderingTrader wanderingTrader, CompoundBinaryTag.Builder builder) {
        CompoundBinaryTag traderTag = MyPetApi.getPlatformHelper().entityToTag(wanderingTrader);
        Set<String> allowedTags = Sets.newHashSet("DespawnDelay", "WanderTarget", "Offers", "Inventory");
        CompoundBinaryTag.Builder filteredBuilder = CompoundBinaryTag.builder();
        for (String key : traderTag.keySet()) {
            if (allowedTags.contains(key)) {
                filteredBuilder.put(key, traderTag.get(key));
            }
        }
        builder.put("OriginalData", filteredBuilder.build());
    }

    public void convertSheep(Sheep sheep, CompoundBinaryTag.Builder builder) {
        builder.putInt("Color", sheep.getColor().getDyeData());
        builder.putBoolean("Sheared", sheep.isSheared());
    }

    public void convertWolf(Wolf wolf, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Tamed", wolf.isTamed());
        builder.putByte("CollarColor", (byte) wolf.getCollarColor().ordinal());
    }

    public void convertTropicalFish(TropicalFish tropicalFish, CompoundBinaryTag.Builder builder) {
        CraftTropicalFish fish = (CraftTropicalFish) tropicalFish;
        builder.putInt("Variant", fish.getHandle().getPackedVariant());
    }

    public void convertPufferFish(PufferFish pufferFish, CompoundBinaryTag.Builder builder) {
        builder.putInt("PuffState", Util.clamp(pufferFish.getPuffState(), 0, 2));
    }

    public void convertPhantom(Phantom phantom, CompoundBinaryTag.Builder builder) {
        builder.putInt("Size", phantom.getSize());
    }

    public void convertCat(Cat cat, CompoundBinaryTag.Builder builder) {
        builder.putInt("CollarColor", cat.getCollarColor().ordinal());
        builder.putInt("CatType", cat.getCatType().ordinal());
    }

    public void convertMushroomCow(MushroomCow mushroomCow, CompoundBinaryTag.Builder builder) {
        builder.putInt("CowType", mushroomCow.getVariant().ordinal());
    }

    public void convertFox(Fox fox, CompoundBinaryTag.Builder builder) {
        builder.putInt("FoxType", fox.getFoxType().ordinal());
    }

    public void convertFrog(Frog frog, CompoundBinaryTag.Builder builder) {
        builder.putInt("FrogType", frog.getVariant().ordinal());
    }

    public void convertPanda(Panda panda, CompoundBinaryTag.Builder builder) {
        builder.putInt("MainGene", panda.getMainGene().ordinal());
        builder.putInt("HiddenGene", panda.getHiddenGene().ordinal());
    }

    public void convertBee(Bee bee, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("Angry", bee.getAnger() > 1);
        builder.putBoolean("HasStung", bee.hasStung());
        builder.putBoolean("HasNectar", bee.hasNectar());
    }

    public void convertGoat(Goat goat, CompoundBinaryTag.Builder builder) {
        builder.putBoolean("screaming", goat.isScreaming());
        builder.putBoolean("LeftHorn", goat.hasLeftHorn());
        builder.putBoolean("RightHorn", goat.hasRightHorn());
    }
}
