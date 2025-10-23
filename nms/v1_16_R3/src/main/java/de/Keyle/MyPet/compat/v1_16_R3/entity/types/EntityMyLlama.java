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

package de.Keyle.MyPet.compat.v1_16_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyLlama;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.compat.v1_16_R3.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.9F, height = 1.87F)
public class EntityMyLlama extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Byte> SADDLE_CHEST_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Boolean> CHEST_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> STRENGTH_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> COLOR_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyLlama.class, DataWatcherRegistry.b);

    public EntityMyLlama(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.llama.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.llama.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.llama.ambient";
    }

    @Override
    public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
            return EnumInteractionResult.CONSUME;
        }

        if (itemStack != null && canUseItem()) {
            if (TagsItem.CARPETS.isTagged(itemStack.getItem()) && !getMyPet().hasDecor() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setDecor(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                return EnumInteractionResult.CONSUME;
            } else if (itemStack.getItem() == Blocks.CHEST.getItem() && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
                getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                return EnumInteractionResult.CONSUME;
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getMyPet().hasChest()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getChest()));
                    entityitem.pickupDelay = 10;
                    entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasDecor()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getDecor()));
                    entityitem.pickupDelay = 10;
                    entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                    this.world.addEntity(entityitem);
                }

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setChest(null);
                getMyPet().setDecor(null);
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    try {
                        itemStack.damage(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastItemBreak(enumhand));
                    } catch (Error e) {
                        // TODO REMOVE
                        itemStack.damage(1, entityhuman, (entityhuman1) -> {
                            try {
                                ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                ex.printStackTrace();
                            }
                        });
                    }
                }

                return EnumInteractionResult.CONSUME;
            } else if (Configuration.MyPet.Llama.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                getMyPet().setBaby(false);
                return EnumInteractionResult.CONSUME;
            }
        }
        return EnumInteractionResult.PASS;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(SADDLE_CHEST_WATCHER, (byte) 0);    // saddle & chest
        getDataWatcher().register(OWNER_WATCHER, Optional.empty()); // owner
        getDataWatcher().register(CHEST_WATCHER, true);
        getDataWatcher().register(STRENGTH_WATCHER, 0);
        getDataWatcher().register(COLOR_WATCHER, 0);
        getDataWatcher().register(VARIANT_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(CHEST_WATCHER, getMyPet().hasChest());
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        if (getMyPet().hasDecor()) {
            ItemStack is = CraftItemStack.asNMSCopy(getMyPet().getDecor());
            Block block = Block.asBlock(is.getItem());
            int color = block instanceof BlockCarpet ? ((BlockCarpet) block).c().getColorIndex() : 0;
            getDataWatcher().set(COLOR_WATCHER, color);
        } else {
            getDataWatcher().set(COLOR_WATCHER, -1);
        }
        getDataWatcher().set(VARIANT_WATCHER, getMyPet().getVariant());
    }

    @Override
    public MyLlama getMyPet() {
        return (MyLlama) myPet;
    }
}