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

package de.Keyle.MyPet.compat.v1_19_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyIllusioner;
import de.Keyle.MyPet.compat.v1_19_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EquipmentSlot;

@EntitySize(width = 0.6F, height = 1.95F)
public class EntityMyIllusioner extends EntityMyPet {

    protected static final EntityDataAccessor<Boolean> RAID_WATCHER = SynchedEntityData.defineId(EntityMyIllusioner.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Byte> SPELL_WATCHER = SynchedEntityData.defineId(EntityMyIllusioner.class, EntityDataSerializers.BYTE);

    public EntityMyIllusioner(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.illusioner.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.illusioner.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.illusioner.ambient";
    }

    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        org.bukkit.entity.Player player = getOwner().getPlayer();
        org.bukkit.inventory.ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (getOwner().equals(entityhuman) && Tag.ITEMS_BANNERS.isTagged(heldItem.getType())
                && player.isSneaking() && canEquip()) {
            org.bukkit.inventory.ItemStack currentBanner = getMyPet().getEquipment(EquipmentSlot.HEAD);
            if (currentBanner != null && currentBanner.getType() != Material.AIR
                    && player.getGameMode() != GameMode.CREATIVE) {
                Item dropped = getBukkitEntity().getWorld().dropItem(
                        getBukkitEntity().getLocation().add(0, 1, 0), currentBanner);
                dropped.setPickupDelay(10);
            }
            getMyPet().setEquipment(EquipmentSlot.HEAD, heldItem.clone());
            if (player.getGameMode() != GameMode.CREATIVE) {
                heldItem.setAmount(heldItem.getAmount() - 1);
            }
            return InteractionResult.CONSUME;
        }

        return super.handlePlayerInteraction(entityhuman, enumhand, itemStack);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(RAID_WATCHER, false);
        getEntityData().define(SPELL_WATCHER, (byte) 0);
    }

    @Override
    public void updateVisuals() {
        super.updateVisuals();

        org.bukkit.inventory.ItemStack itemInSlot = getMyPet().getEquipment(EquipmentSlot.HAND);
        boolean hasItem = itemInSlot != null && !itemInSlot.getType().isAir();
        if (hasItem != this.isAggressive()) {
            this.setAggressive(hasItem);
        }
    }

    @Override
    public MyIllusioner getMyPet() {
        return (MyIllusioner) myPet;
    }
}
