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

package de.Keyle.MyPet.compat.v1_21_R2.services;

import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.types.*;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_21_R2.util.VariantConverter;
import de.Keyle.MyPet.compat.v1_21_R2.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.trading.MerchantOffers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R2.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftTropicalFish;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftVillagerZombie;
import org.bukkit.craftbukkit.v1_21_R2.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.*;

@Compat("v1_21_R2")
public class EntityConverterService extends de.Keyle.MyPet.api.util.service.types.EntityConverterService {

    public final Registry<VillagerType> VILLAGER_TYPE_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.VILLAGER_TYPE);
    public final Registry<VillagerProfession> VILLAGER_PROFESSION_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.VILLAGER_PROFESSION);

    @Override
    public TagCompound convertEntity(LivingEntity entity) {
        TagCompound properties = new TagCompound();
        switch (entity.getType()) {
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
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
                convertSaddledHorse((AbstractHorse) entity, properties);
                break;
            case MULE:
            case DONKEY:
                convertChestedHorse((ChestedHorse) entity, properties);
                break;
            case ZOMBIE_VILLAGER:
                convertZombieVillager((ZombieVillager) entity, properties);
            case HUSK:
            case ZOMBIE:
            case ZOMBIFIED_PIGLIN:
            case DROWNED:
                convertZombie((Zombie) entity, properties);
                if (Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                    convertEquipable(entity, properties);
                }
                break;
            case ENDERMAN:
                convertEnderman((Enderman) entity, properties);
                break;
            case RABBIT:
                convertRabbit((Rabbit) entity, properties);
                break;
            case LLAMA:
                convertLlama((Llama) entity, properties);
                break;
            case AXOLOTL:
                convertAxolotl((Axolotl) entity, properties);
                break;
            case GOAT:
                convertGoat((Goat) entity, properties);
                break;
            case PARROT:
                convertParrot((Parrot) entity, properties);
                break;
            case TROPICAL_FISH:
                convertTropicalFish((TropicalFish) entity, properties);
                break;
            case PUFFERFISH:
                convertPufferFish((PufferFish) entity, properties);
                break;
            case PHANTOM:
                convertPhantom((Phantom) entity, properties);
                break;
            case CAT:
                convertCat((Cat) entity, properties);
                break;
            case MOOSHROOM:
                convertMushroomCow((MushroomCow) entity, properties);
                break;
            case FOX:
                convertFox((Fox) entity, properties);
                break;
            case FROG:
                convertFrog((Frog) entity, properties);
                break;
            case PANDA:
                convertPanda((Panda) entity, properties);
                break;
            case WANDERING_TRADER:
                convertWanderingTrader((WanderingTrader) entity, properties);
                break;
            case BEE:
                convertBee((Bee) entity, properties);
                break;
            case TRADER_LLAMA:
                convertTraderLlama((TraderLlama) entity, properties);
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
        } else if (myPet instanceof MyGoat) {
        	if (((MyGoat) myPet).isScreaming()) {
                ((Goat) normalEntity).setScreaming(true);
            }
            if(!((MyGoat) myPet).hasLeftHorn()) {
                ((Goat) normalEntity).setLeftHorn(false);
            }
            if(!((MyGoat) myPet).hasRightHorn()) {
                ((Goat) normalEntity).setRightHorn(false);
            }
        } else if (myPet instanceof MyEnderman) {
            if (((MyEnderman) myPet).hasBlock()) {
                ((Enderman) normalEntity).setCarriedMaterial(((MyEnderman) myPet).getBlock().getData());
            }
        } else if (myPet instanceof MyIronGolem) {
            ((IronGolem) normalEntity).setPlayerCreated(true);
        } else if (myPet instanceof MyMagmaCube) {
            ((MagmaCube) normalEntity).setSize(((MyMagmaCube) myPet).getSize());
        } else if (myPet instanceof MyPig) {
            ((Pig) normalEntity).setSaddle(((MyPig) myPet).hasSaddle());
        } else if (myPet instanceof MySheep) {
            ((Sheep) normalEntity).setSheared(((MySheep) myPet).isSheared());
            ((Sheep) normalEntity).setColor(((MySheep) myPet).getColor());
        } else if (myPet instanceof MyVillager) {
            MyVillager villagerPet = (MyVillager) myPet;
            Villager villagerEntity = ((Villager) normalEntity);
            net.minecraft.world.entity.npc.Villager entityVillager = ((CraftVillager) villagerEntity).getHandle();

            VillagerProfession profession = VILLAGER_PROFESSION_REGISTRY.byId(villagerPet.getProfession());
            VillagerType type = VILLAGER_TYPE_REGISTRY.byId(villagerPet.getType().ordinal());

            VillagerData villagerData = new VillagerData(type, profession, villagerPet.getVillagerLevel());
            entityVillager.setVillagerData(villagerData);

            if (villagerPet.hasOriginalData()) {
                TagCompound villagerTag = villagerPet.getOriginalData();

                try {
                    if (villagerTag.containsKey("Offers")) {
                        TagCompound offersTag = villagerTag.get("Offers");
                        CompoundTag vanillaNBT = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(offersTag);
                        DataResult<MerchantOffers> dataresult = MerchantOffers.CODEC.parse(entityVillager.registryAccess().createSerializationContext(NbtOps.INSTANCE), vanillaNBT);
                        if(dataresult.hasResultOrPartial() && dataresult.resultOrPartial().isPresent()) {
                            entityVillager.setOffers(dataresult.resultOrPartial().get());
                        }
                    }
                    if (villagerTag.containsKey("Inventory")) {
                        TagList inventoryTag = villagerTag.get("Inventory");
                        ListTag vanillaNBT = (ListTag) ItemStackNBTConverter.compoundToVanillaCompound(inventoryTag);
                        for (int i = 0; i < vanillaNBT.size(); ++i) {
                            net.minecraft.world.item.ItemStack itemstack = ItemStackNBTConverter.vanillaCompoundToItemStack(vanillaNBT.getCompound(i));;
                            ItemStack item = CraftItemStack.asCraftMirror(itemstack);
                            if (!itemstack.isEmpty()) {
                            	Villager vill = ((Villager) Bukkit.getServer().getEntity(normalEntity.getUniqueId()));
                            	vill.getInventory().addItem(item);
                            }
                        }
                    }
                    if (villagerTag.containsKey("FoodLevel")) {
                        byte foodLevel = villagerTag.getAs("FoodLevel", TagByte.class).getByteData();
                        ReflectionUtil.setFieldValue("cs", entityVillager, foodLevel);		// Field: foodLevel
                    }
                    if (villagerTag.containsKey("Gossips")) {
                        TagList inventoryTag = villagerTag.get("Gossips");
                        ListTag vanillaNBT = (ListTag) ItemStackNBTConverter.compoundToVanillaCompound(inventoryTag);
                        //This might be useful for later/following versions
                        //((GossipContainer) ReflectionUtil.getFieldValue(net.minecraft.world.entity.npc.Villager.class, entityVillager, "cu")) //Field: gossips
                        entityVillager.getGossips().update(new Dynamic<>(NbtOps.INSTANCE, vanillaNBT));
                    }
                    if (villagerTag.containsKey("LastRestock")) {
                    	long lastRestock = villagerTag.getAs("LastRestock", TagLong.class).getLongData();
                        ReflectionUtil.setFieldValue("cx", entityVillager, lastRestock);	//Field: lastRestock(Game)Time
                    }
                    if (villagerTag.containsKey("LastGossipDecay")) {
                        long lastGossipDecay = villagerTag.getAs("LastGossipDecay", TagLong.class).getLongData();
                        ReflectionUtil.setFieldValue("cv", entityVillager, lastGossipDecay);	//Field: lastGossipDecayTime
                    }
                    if (villagerTag.containsKey("RestocksToday")) {
                        int restocksToday = villagerTag.getAs("RestocksToday", TagInt.class).getIntData();
                        ReflectionUtil.setFieldValue("cy", entityVillager, restocksToday);		//Field: numberOfRestocksToday or restocksToday
                    }
                    ReflectionUtil.setFieldValue("cC", entityVillager, true); // Field: AssignProfessionWhenSpawned (natural?)
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (villagerTag.containsKey("Xp")) {
                    int xp = villagerTag.getAs("Xp", TagInt.class).getIntData();
                    entityVillager.setVillagerXp(xp);
                }

                //Have to refresh brain, otherwise will not store new job site in memories
                entityVillager.refreshBrain(entityVillager.level().getMinecraftWorld());
            }
        } else if (myPet instanceof MySlime) {
            ((Slime) normalEntity).setSize(((MySlime) myPet).getSize());
        } else if (myPet instanceof MyZombieVillager) {
            Villager.Profession profession = Villager.Profession.values()[((MyZombieVillager) myPet).getProfession()];
            net.minecraft.world.entity.monster.ZombieVillager nmsEntity = ((CraftVillagerZombie) normalEntity).getHandle();
            nmsEntity.setVillagerData(nmsEntity.getVillagerData()
                    .setType(BuiltInRegistries.VILLAGER_TYPE.get(ResourceLocation.tryParse(((MyZombieVillager) myPet).getType().name().toLowerCase(Locale.ROOT))).get().value())
                    .setLevel(((MyZombieVillager) myPet).getTradingLevel())
                    .setProfession(BuiltInRegistries.VILLAGER_PROFESSION.get(ResourceLocation.tryParse(profession.name().toLowerCase(Locale.ROOT))).get().value()));
        } else if (myPet instanceof MyWitherSkeleton) {
            normalEntity.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
        } else if (myPet instanceof MySkeleton) {
            normalEntity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
        } else if (myPet instanceof MyHorse) {
            Horse.Style style = Horse.Style.values()[(((MyHorse) myPet).getVariant() >>> 8)];
            Horse.Color color = Horse.Color.values()[(((MyHorse) myPet).getVariant() & 0xFF)];

            ((Horse) normalEntity).setColor(color);
            ((Horse) normalEntity).setStyle(style);

            if (((MyHorse) myPet).hasSaddle()) {
                ((Horse) normalEntity).getInventory().setSaddle(((MyHorse) myPet).getSaddle().clone());
            }
            if (((MyHorse) myPet).hasArmor()) {
                ((Horse) normalEntity).getInventory().setArmor(((MyHorse) myPet).getArmor().clone());
            }
            ((Horse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MySkeletonHorse) {
            ((SkeletonHorse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyZombieHorse) {
            ((ZombieHorse) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyLlama) {
            ((Llama) normalEntity).setColor(Llama.Color.values()[Math.max(0, Math.min(3, ((MyLlama) myPet).getVariant()))]);
            ((Llama) normalEntity).setCarryingChest(((MyLlama) myPet).hasChest());

            if (((MyLlama) myPet).hasDecor()) {
                ((Llama) normalEntity).getInventory().setDecor(((MyLlama) myPet).getDecor());
            }
            ((Llama) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyTraderLlama) {
            ((TraderLlama) normalEntity).setColor(TraderLlama.Color.values()[Math.max(0, Math.min(3, ((MyTraderLlama) myPet).getVariant()))]);
            ((TraderLlama) normalEntity).setOwner(myPet.getOwner().getPlayer());
        } else if (myPet instanceof MyRabbit) {
            ((Rabbit) normalEntity).setRabbitType(((MyRabbit) myPet).getVariant().getBukkitType());
        } else if (myPet instanceof MyParrot) {
            ((Parrot) normalEntity).setVariant(Parrot.Variant.values()[((MyParrot) myPet).getVariant()]);
        } else if (myPet instanceof MyAxolotl) {
            ((Axolotl) normalEntity).setVariant(Axolotl.Variant.values()[((MyAxolotl) myPet).getVariant()]);
        } else if (myPet instanceof MyTropicalFish) {
            ((CraftTropicalFish) normalEntity).getHandle().setPackedVariant(((MyTropicalFish) myPet).getVariant());
        } else if (myPet instanceof MyPufferfish) {
            ((PufferFish) normalEntity).setPuffState(((MyPufferfish) myPet).getPuffState().ordinal());
        } else if (myPet instanceof MyPhantom) {
            ((Phantom) normalEntity).setSize(((MyPhantom) myPet).getSize());
        } else if (myPet instanceof MyCat) {
            ((Cat) normalEntity).setCatType(((MyCat) myPet).getCatType());
            ((Cat) normalEntity).setCollarColor(((MyCat) myPet).getCollarColor());
        } else if (myPet instanceof MyMooshroom) {
            ((MushroomCow) normalEntity).setVariant(MushroomCow.Variant.values()[((MyMooshroom) myPet).getType().ordinal()]);
        } else if (myPet instanceof MyPanda) {
            ((Panda) normalEntity).setMainGene(((MyPanda) myPet).getMainGene());
            ((Panda) normalEntity).setHiddenGene(((MyPanda) myPet).getHiddenGene());
        } else if (myPet instanceof WanderingTrader) {
            MyWanderingTrader traderPet = (MyWanderingTrader) myPet;
            if (traderPet.hasOriginalData()) {
                TagCompound villagerTag = MyPetApi.getPlatformHelper().entityToTag(normalEntity);
                for (String key : traderPet.getOriginalData().getCompoundData().keySet()) {
                    villagerTag.put(key, traderPet.getOriginalData().get(key));
                }
                MyPetApi.getPlatformHelper().applyTagToEntity(villagerTag, normalEntity);
            }
        } else if (myPet instanceof MyBee) {
            ((Bee) normalEntity).setHasNectar(((MyBee) myPet).hasNectar());
            ((Bee) normalEntity).setHasStung(((MyBee) myPet).hasStung());
        } else if (myPet instanceof MyFox) {
            ((Fox) normalEntity).setFoxType(((MyFox) myPet).getFoxType());
        }else if (myPet instanceof MyFrog) {
            ((Frog) normalEntity).setVariant(VariantConverter.getBukkitFrogVariant(((MyFrog) myPet).getFrogVariant()));
        }else if (myPet instanceof MyWolf) {
            Method getVariant = ReflectionUtil.getMethod(Wolf.Variant.class, "getVariant", String.class);
            try {
                Wolf.Variant leVariant = (Wolf.Variant) getVariant.invoke(null, ((MyWolf)myPet).getVariant());
                ((Wolf) normalEntity).setVariant(leVariant);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (myPet instanceof MyPetBaby && normalEntity instanceof Ageable) {
            if (((MyPetBaby) myPet).isBaby()) {
                ((Ageable) normalEntity).setBaby();
            } else {
                ((Ageable) normalEntity).setAdult();
            }
        }
    }

    private void convertLlama(Llama llama, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagInt(llama.getColor().ordinal()));
        if (llama.getInventory().getDecor() != null && llama.getInventory().getDecor().getType() != Material.AIR) {
            properties.getCompoundData().put("Decor", MyPetApi.getPlatformHelper().itemStackToCompund(llama.getInventory().getDecor()));
        }
        if (llama.isCarryingChest()) {
            properties.getCompoundData().put("Chest", MyPetApi.getPlatformHelper().itemStackToCompund(new ItemStack(Material.CHEST)));
        }
    }

    private void convertTraderLlama(TraderLlama tLlama, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagInt(tLlama.getColor().ordinal()));
    }
    
    private void convertAxolotl(Axolotl axolotl, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagInt(axolotl.getVariant().ordinal()));
    }

    private void convertParrot(Parrot parrot, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagInt(parrot.getVariant().ordinal()));
    }

    public void convertRabbit(Rabbit rabbit, TagCompound properties) {
        properties.getCompoundData().put("Variant", new TagByte(MyRabbit.RabbitType.getTypeByBukkitEnum(rabbit.getRabbitType()).getId()));
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

    public void convertEnderman(Enderman enderman, TagCompound properties) {
        if (enderman.getCarriedBlock() != null) {
            ItemStack block = enderman.getCarriedMaterial().toItemStack(1);
            properties.getCompoundData().put("Block", MyPetApi.getPlatformHelper().itemStackToCompund(block));
        }
    }

    public void convertZombieVillager(ZombieVillager zombie, TagCompound properties) {
        properties.getCompoundData().put("Profession", new TagInt(zombie.getVillagerProfession().ordinal()));

        TagCompound villagerTag = MyPetApi.getPlatformHelper().entityToTag(zombie);
        Set<String> allowedTags = Sets.newHashSet("VillagerData");
        Set<String> keys = new HashSet<>(villagerTag.getCompoundData().keySet());
        for (String key : keys) {
            if (allowedTags.contains(key)) {
                continue;
            }
            villagerTag.remove(key);
        }
        properties.getCompoundData().put("VillagerData", villagerTag);
    }

    public void convertZombie(Zombie zombie, TagCompound properties) {
        properties.getCompoundData().put("Baby", new TagByte(zombie.isBaby()));
    }

    public void convertCreeper(Creeper creeper, TagCompound properties) {
        properties.getCompoundData().put("Powered", new TagByte(creeper.isPowered()));
    }

    public void convertHorse(Horse horse, TagCompound properties) {
        int style = horse.getStyle().ordinal();
        int color = horse.getColor().ordinal();
        int variant = color & 255 | style << 8;

        if (horse.getInventory().getArmor() != null) {
            TagCompound armor = MyPetApi.getPlatformHelper().itemStackToCompund(horse.getInventory().getArmor());
            properties.getCompoundData().put("Armor", armor);
        }

        properties.getCompoundData().put("Variant", new TagInt(variant));

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

    public void convertSaddledHorse(AbstractHorse horse, TagCompound properties) {
        if (horse.getInventory() instanceof HorseInventory) {
            if (horse.getInventory().getSaddle() != null) {
                TagCompound saddle = MyPetApi.getPlatformHelper().itemStackToCompund(horse.getInventory().getSaddle());
                properties.getCompoundData().put("Saddle", saddle);
            }
        }
    }

    public void convertChestedHorse(ChestedHorse horse, TagCompound properties) {
        properties.getCompoundData().put("Chest", new TagByte(horse.isCarryingChest()));
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
        net.minecraft.world.entity.npc.Villager npcVillager = ((CraftVillager) villager).getHandle();
        VillagerData villagerData = npcVillager.getVillagerData();

        int profession = VILLAGER_PROFESSION_REGISTRY.getId(villagerData.getProfession());
        properties.getCompoundData().put("Profession", new TagInt(profession));
        int type = VILLAGER_TYPE_REGISTRY.getId(villagerData.getType());
        properties.getCompoundData().put("VillagerType", new TagInt(type));
        int level = villager.getVillagerLevel();
        properties.getCompoundData().put("VillagerLevel", new TagInt(level));

        TagCompound villagerTag = MyPetApi.getPlatformHelper().entityToTag(villager);
        Set<String> allowedTags = Sets.newHashSet(
                "RestocksToday",
                "FoodLevel",
                "Gossips",
                "Offers",
                "LastRestock",
                "Inventory",
                "Xp"
        );
        Set<String> keys = new HashSet<>(villagerTag.getCompoundData().keySet());
        for (String key : keys) {
            if (allowedTags.contains(key)) {
                continue;
            }
            villagerTag.remove(key);
        }
        properties.getCompoundData().put("OriginalData", villagerTag);
    }

    public void convertWanderingTrader(WanderingTrader wanderingTrader, TagCompound properties) {
        TagCompound traderTag = MyPetApi.getPlatformHelper().entityToTag(wanderingTrader);
        Set<String> allowedTags = Sets.newHashSet("DespawnDelay", "WanderTarget", "Offers", "Inventory");
        Set<String> keys = new HashSet<>(traderTag.getCompoundData().keySet());
        for (String key : keys) {
            if (allowedTags.contains(key)) {
                continue;
            }
            traderTag.remove(key);
        }
        properties.getCompoundData().put("OriginalData", traderTag);
    }

    public void convertSheep(Sheep sheep, TagCompound properties) {
        properties.getCompoundData().put("Color", new TagInt(sheep.getColor().getDyeData()));
        properties.getCompoundData().put("Sheared", new TagByte(sheep.isSheared()));
    }

    public void convertWolf(Wolf wolf, TagCompound properties) {
        properties.getCompoundData().put("Tamed", new TagByte(wolf.isTamed()));
        properties.getCompoundData().put("CollarColor", new TagByte(wolf.getCollarColor().ordinal()));
        properties.getCompoundData().put("Variant", new TagString(wolf.getVariant().getKey().getKey()));
    }

    public void convertTropicalFish(TropicalFish tropicalFish, TagCompound properties) {
        CraftTropicalFish fish = (CraftTropicalFish) tropicalFish;
        properties.getCompoundData().put("Variant", new TagInt(fish.getHandle().getPackedVariant()));
    }

    public void convertPufferFish(PufferFish pufferFish, TagCompound properties) {
        properties.getCompoundData().put("PuffState", new TagInt(Util.clamp(pufferFish.getPuffState(), 0, 2)));
    }

    public void convertPhantom(Phantom phantom, TagCompound properties) {
        properties.getCompoundData().put("Size", new TagInt(phantom.getSize()));
    }

    public void convertCat(Cat cat, TagCompound properties) {
        properties.getCompoundData().put("CollarColor", new TagInt(cat.getCollarColor().ordinal()));
        properties.getCompoundData().put("CatType", new TagInt(cat.getCatType().ordinal()));
    }

    public void convertMushroomCow(MushroomCow mushroomCow, TagCompound properties) {
        properties.getCompoundData().put("CowType", new TagInt(mushroomCow.getVariant().ordinal()));
    }

    public void convertFox(Fox fox, TagCompound properties) {
        properties.getCompoundData().put("FoxType", new TagInt(fox.getFoxType().ordinal()));
    }

    public void convertFrog(Frog frog, TagCompound properties) {
        properties.getCompoundData().put("FrogType", new TagInt(frog.getVariant().ordinal()));
    }

    public void convertPanda(Panda panda, TagCompound properties) {
        properties.getCompoundData().put("MainGene", new TagInt(panda.getMainGene().ordinal()));
        properties.getCompoundData().put("HiddenGene", new TagInt(panda.getHiddenGene().ordinal()));
    }

    public void convertBee(Bee bee, TagCompound properties) {
        properties.getCompoundData().put("Angry", new TagByte(bee.getAnger()>1));
        properties.getCompoundData().put("HasStung", new TagByte(bee.hasStung()));
        properties.getCompoundData().put("HasNectar", new TagByte(bee.hasNectar()));
    }

    public void convertGoat(Goat goat, TagCompound properties) {
        properties.getCompoundData().put("screaming", new TagByte(goat.isScreaming()));
        properties.getCompoundData().put("LeftHorn", new TagByte(goat.hasLeftHorn()));
        properties.getCompoundData().put("RightHorn", new TagByte(goat.hasRightHorn()));
    }
}
