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

package de.Keyle.MyPet.compat.v1_9_R2.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyHorse;
import de.Keyle.MyPet.compat.v1_9_R2.entity.EntityMyPet;
import net.minecraft.server.v1_9_R2.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;

import java.util.UUID;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyHorse extends EntityMyPet implements IJumpable {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Byte> SADDLE_CHEST_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Integer> TYPE_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.m);
    private static final DataWatcherObject<Integer> ARMOR_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.b);

    int soundCounter = 0;
    int rearCounter = -1;

    public EntityMyHorse(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Possible visual horse effects:
     * 4 saddle
     * 8 chest
     * 32 head down
     * 64 rear
     * 128 mouth open
     */
    private void applyVisual(int value, boolean flag) {
        int i = this.datawatcher.get(SADDLE_CHEST_WATCHER);
        if (flag) {
            this.datawatcher.set(SADDLE_CHEST_WATCHER, (byte) (i | value));
        } else {
            this.datawatcher.set(SADDLE_CHEST_WATCHER, (byte) (i & (~value)));
        }
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            flag = super.attack(entity);
            if (flag) {
                applyVisual(64, true);
                rearCounter = 10;
                if (getMyPet().getHorseType() == 0) {
                    this.makeSound("entity.horse.angry", 1.0F, 1.0F);
                } else if (getMyPet().getHorseType() == 2 || getMyPet().getHorseType() == 1) {
                    this.makeSound("entity.donkey.angry", 1.0F, 1.0F);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        int horseType = getMyPet().getHorseType();
        if (horseType == 3) {
            return "entity.zombie_horse.death";
        }
        if (horseType == 4) {
            return "entity.skeleton_horse.death";
        }
        if ((horseType == 1) || (horseType == 2)) {
            return "entity.donkey.death";
        }
        return "entity.horse.death";
    }

    @Override
    protected String getHurtSound() {
        int horseType = ((MyHorse) myPet).getHorseType();
        if (horseType == 3) {
            return "entity.zombie_horse.hurt";
        }
        if (horseType == 4) {
            return "entity.skeleton_horse.hurt";
        }
        if ((horseType == 1) || (horseType == 2)) {
            return "entity.donkey.hurt";
        }
        return "entity.horse.hurt";
    }

    protected String getLivingSound() {
        int horseType = ((MyHorse) myPet).getHorseType();
        if (horseType == 3) {
            return "entity.zombie_horse.ambient";
        }
        if (horseType == 4) {
            return "entity.skeleton_horse.ambient";
        }
        if ((horseType == 1) || (horseType == 2)) {
            return "entity.donkey.ambient";
        }
        return "entity.horse.ambient";
    }


    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Item.getItemOf(Blocks.CHEST) && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
                getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (getHorseArmorId(CraftItemStack.asBukkitCopy(itemStack)) > 0 && !getMyPet().hasArmor() && getMyPet().getHorseType() == 0 && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setArmor(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getMyPet().hasArmor()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getArmor()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasChest()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getChest()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasSaddle()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setChest(null);
                getMyPet().setSaddle(null);
                getMyPet().setArmor(null);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Horse.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                getMyPet().setBaby(false);
                return true;
            }
        }
        return false;
    }

    private int getHorseArmorId(org.bukkit.inventory.ItemStack itemstack) {
        if (itemstack == null) {
            return 0;
        }
        Material item = itemstack.getType();

        return item == Material.DIAMOND_BARDING ? 3 : item == Material.GOLD_BARDING ? 2 : item == Material.IRON_BARDING ? 1 : 0;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(AGE_WATCHER, false);
        this.datawatcher.register(SADDLE_CHEST_WATCHER, (byte) 0);    // saddle & chest
        this.datawatcher.register(TYPE_WATCHER, 0);                  // horse type
        this.datawatcher.register(VARIANT_WATCHER, 0);               // variant
        this.datawatcher.register(OWNER_WATCHER, Optional.absent()); // owner
        this.datawatcher.register(ARMOR_WATCHER, 0);                 // armor
    }

    protected void initAttributes() {
        super.initAttributes();
        this.getAttributeMap().b(EntityHorse.attributeJumpStrength);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(AGE_WATCHER, getMyPet().isBaby());
        this.datawatcher.set(ARMOR_WATCHER, getHorseArmorId(getMyPet().getArmor()));
        this.datawatcher.set(TYPE_WATCHER, (int) getMyPet().getHorseType());
        this.datawatcher.set(VARIANT_WATCHER, getMyPet().getVariant());
        applyVisual(8, getMyPet().hasChest());
        applyVisual(4, getMyPet().hasSaddle());
    }

    public void onLivingUpdate() {
        boolean oldRiding = hasRider;
        super.onLivingUpdate();
        if (!hasRider) {
            if (rearCounter > -1 && rearCounter-- == 0) {
                applyVisual(64, false);
                rearCounter = -1;
            }
        }
        if (oldRiding != hasRider) {
            if (hasRider) {
                applyVisual(4, true);
            } else {
                applyVisual(4, getMyPet().hasSaddle());
            }
        }
    }

    @Override
    public void playStepSound(BlockPosition pos, Block block) {
        SoundEffectType soundeffecttype = block.w();
        if (this.world.getType(pos) == Blocks.SNOW) {
            soundeffecttype = Blocks.SNOW_LAYER.w();
        }
        if (!block.getBlockData().getMaterial().isLiquid()) {
            int horseType = getMyPet().getHorseType();
            if ((this.isVehicle()) && (horseType != 1) && (horseType != 2)) {
                this.soundCounter += 1;
                if ((this.soundCounter > 5) && (this.soundCounter % 3 == 0)) {
                    a(SoundEffects.ct, soundeffecttype.a() * 0.15F, soundeffecttype.b());
                    if ((horseType == 0) && (this.random.nextInt(10) == 0)) {
                        a(SoundEffects.cq, soundeffecttype.a() * 0.6F, soundeffecttype.b());
                    }
                } else if (this.soundCounter <= 5) {
                    a(SoundEffects.cz, soundeffecttype.a() * 0.15F, soundeffecttype.b());
                }
            } else if (soundeffecttype == SoundEffectType.a) {
                a(SoundEffects.cz, soundeffecttype.a() * 0.15F, soundeffecttype.b());
            } else {
                a(SoundEffects.cy, soundeffecttype.a() * 0.15F, soundeffecttype.b());
            }
        }
    }

    public MyHorse getMyPet() {
        return (MyHorse) myPet;
    }

    @Override
    public boolean b() {
        return true;
    }

    @Override
    public void b(int i) {
        this.jumpPower = i;
    }

    @Override
    public void r_() {
    }
}