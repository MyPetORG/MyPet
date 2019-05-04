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

package de.Keyle.MyPet.compat.v1_13_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyLlama;
import de.Keyle.MyPet.compat.v1_13_R2.entity.EntityMyPet;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

import java.util.Optional;
import java.util.UUID;

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
        super(EntityTypes.LLAMA, world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.llama.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.llama.hurt";
    }

    protected String getLivingSound() {
        return "entity.llama.ambient";
    }


    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (itemStack != null && canUseItem()) {
            if (TagsItem.CARPETS.isTagged(itemStack.getItem()) && !getMyPet().hasDecor() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setDecor(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Blocks.CHEST.getItem() && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
                getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getMyPet().hasChest()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getChest()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasDecor()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getDecor()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setChest(null);
                getMyPet().setDecor(null);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Llama.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                getMyPet().setBaby(false);
                return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(AGE_WATCHER, false);
        this.datawatcher.register(SADDLE_CHEST_WATCHER, (byte) 0);    // saddle & chest
        this.datawatcher.register(OWNER_WATCHER, Optional.empty()); // owner
        this.datawatcher.register(CHEST_WATCHER, true);
        this.datawatcher.register(STRENGTH_WATCHER, 0);
        this.datawatcher.register(COLOR_WATCHER, 0);
        this.datawatcher.register(VARIANT_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(CHEST_WATCHER, getMyPet().hasChest());
        this.datawatcher.set(AGE_WATCHER, getMyPet().isBaby());
        if (getMyPet().hasDecor()) {
            ItemStack is = CraftItemStack.asNMSCopy(getMyPet().getDecor());
            Block block = Block.asBlock(is.getItem());
            int color = block instanceof BlockCarpet ? ((BlockCarpet) block).d().getColorIndex() : 0;
            this.datawatcher.set(COLOR_WATCHER, color);
        } else {
            this.datawatcher.set(COLOR_WATCHER, -1);
        }
        this.datawatcher.set(VARIANT_WATCHER, getMyPet().getVariant());
    }

    public MyLlama getMyPet() {
        return (MyLlama) myPet;
    }
}