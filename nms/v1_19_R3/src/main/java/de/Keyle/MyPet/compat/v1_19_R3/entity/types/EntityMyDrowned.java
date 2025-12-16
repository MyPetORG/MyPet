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

package de.Keyle.MyPet.compat.v1_19_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyDrowned;
import de.Keyle.MyPet.compat.v1_19_R3.entity.EntityMyAquaticPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Sound;

@EntitySize(width = 0.6F, height = 1.95F)
public class EntityMyDrowned extends EntityMyAquaticPet {

    private static final EntityDataAccessor<Boolean> BABY_WATCHER = SynchedEntityData.defineId(EntityMyDrowned.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> UNUSED_WATCHER_1 = SynchedEntityData.defineId(EntityMyDrowned.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> UNUSED_WATCHER_2 = SynchedEntityData.defineId(EntityMyDrowned.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> UNUSED_WATCHER_3 = SynchedEntityData.defineId(EntityMyDrowned.class, EntityDataSerializers.BOOLEAN);


    public EntityMyDrowned(Level world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getMyPetDeathSound() {
        return "entity.drowned.death" + (isInWater() ? "_water" : "");
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.drowned.hurt" + (isInWater() ? "_water" : "");
    }

    /**
     * Returns the default sound of the MyPet
     */
    @Override
    protected String getLivingSound() {
        return "entity.drowned.ambient" + (isInWater() ? "_water" : "");
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
            return InteractionResult.CONSUME;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && itemStack.getItem() != Items.AIR) {
            if (Configuration.MyPet.Drowned.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.getAbilities().instabuild) {
                    itemStack.shrink(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
                    }
                }
                getMyPet().setBaby(false);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(BABY_WATCHER, false);
        getEntityData().define(UNUSED_WATCHER_1, 0);
        getEntityData().define(UNUSED_WATCHER_2, false);
        getEntityData().define(UNUSED_WATCHER_3, false);
    }

    @Override
    public void updateVisuals() {
        getEntityData().set(BABY_WATCHER, getMyPet().isBaby());

        super.updateVisuals();
    }

    @Override
    public void playPetStepSound() {
        getBukkitEntity().getWorld().playSound(getBukkitEntity().getLocation(), Sound.ENTITY_DROWNED_STEP, 0.15F, 1.0F);
    }

    @Override
    public MyDrowned getMyPet() {
        return (MyDrowned) myPet;
    }
}
