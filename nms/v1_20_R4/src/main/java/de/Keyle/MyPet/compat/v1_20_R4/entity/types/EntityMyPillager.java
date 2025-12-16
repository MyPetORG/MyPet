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

package de.Keyle.MyPet.compat.v1_20_R4.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPillager;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPet;
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
public class EntityMyPillager extends EntityMyPet {

    protected static final EntityDataAccessor<Boolean> RAID_WATCHER = SynchedEntityData.defineId(EntityMyPillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CROSSBOW_WATCHER = SynchedEntityData.defineId(EntityMyPillager.class, EntityDataSerializers.BOOLEAN);

    public EntityMyPillager(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.pillager.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.pillager.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.pillager.ambient";
    }

    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        // Special handling for banners - must be checked BEFORE super call
        // since banners aren't standard equipment and super's sitting toggle would consume the interaction
        org.bukkit.entity.Player player = getOwner().getPlayer();
        org.bukkit.inventory.ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (getOwner().equals(entityhuman) && Tag.ITEMS_BANNERS.isTagged(heldItem.getType())
                && player.isSneaking() && canEquip()) {
            // Drop existing banner if present
            org.bukkit.inventory.ItemStack currentBanner = getMyPet().getEquipment(EquipmentSlot.HEAD);
            if (currentBanner != null && currentBanner.getType() != Material.AIR
                    && player.getGameMode() != GameMode.CREATIVE) {
                Item dropped = getBukkitEntity().getWorld().dropItem(
                        getBukkitEntity().getLocation().add(0, 1, 0), currentBanner);
                dropped.setPickupDelay(10);
            }
            // Equip new banner
            getMyPet().setEquipment(EquipmentSlot.HEAD, heldItem.clone());
            if (player.getGameMode() != GameMode.CREATIVE) {
                heldItem.setAmount(heldItem.getAmount() - 1);
            }
            return InteractionResult.CONSUME;
        }

        return super.handlePlayerInteraction(entityhuman, enumhand, itemStack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAID_WATCHER, false);
        builder.define(CROSSBOW_WATCHER, false);
    }

    @Override
    public MyPillager getMyPet() {
        return (MyPillager) myPet;
    }
}
