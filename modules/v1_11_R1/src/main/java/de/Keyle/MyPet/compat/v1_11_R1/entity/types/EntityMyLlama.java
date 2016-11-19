/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_11_R1.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyLlama;
import de.Keyle.MyPet.compat.v1_11_R1.entity.EntityMyPet;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;

import java.util.UUID;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyLlama extends EntityMyPet {
    private static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Byte> saddleChestWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Optional<UUID>> ownerWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.m);
    private static final DataWatcherObject<Boolean> chestWatcher = DataWatcher.a(EntityMyMule.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Integer> bG = DataWatcher.a(EntityLlama.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> bH = DataWatcher.a(EntityLlama.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> bI = DataWatcher.a(EntityLlama.class, DataWatcherRegistry.b);

    int ageCounter = -1;

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

    protected String getLivingSound() {
        return "entity.llama.ambient";
    }


    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Item.getItemOf(Blocks.CARPET) && !getMyPet().hasDecor() && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setDecor(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Item.getItemOf(Blocks.CHEST) && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
                getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
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
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Horse.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                getMyPet().setAge(getMyPet().getAge() + 3000);
                return true;
            }
            if (itemStack.getItem() == Items.BREAD ||
                    itemStack.getItem() == Items.WHEAT ||
                    itemStack.getItem() == Items.GOLDEN_APPLE ||
                    itemStack.getItem() == Item.getItemOf(Blocks.HAY_BLOCK) ||
                    itemStack.getItem() == Items.GOLDEN_CARROT ||
                    itemStack.getItem() == Items.APPLE ||
                    itemStack.getItem() == Items.SUGAR) {
                ageCounter = 5;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(ageWatcher, false);               // age
        this.datawatcher.register(saddleChestWatcher, (byte) 0);    // saddle & chest
        this.datawatcher.register(ownerWatcher, Optional.absent()); // owner
        this.datawatcher.register(chestWatcher, false);
        this.datawatcher.register(bG, 0);                 // armor
        this.datawatcher.register(bH, 0);                 // armor
        this.datawatcher.register(bI, 0);                 // armor
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(chestWatcher, getMyPet().hasChest());
        //this.datawatcher.set(ageWatcher, getMyPet().isBaby());
        //this.datawatcher.set(armorWatcher, getHorseArmorId(getMyPet().getArmor()));
        //this.datawatcher.set(variantWatcher, getMyPet().getVariant());
        //applyVisual(8, getMyPet().hasChest());
        //applyVisual(4, getMyPet().hasSaddle());
    }

    public MyLlama getMyPet() {
        return (MyLlama) myPet;
    }
}