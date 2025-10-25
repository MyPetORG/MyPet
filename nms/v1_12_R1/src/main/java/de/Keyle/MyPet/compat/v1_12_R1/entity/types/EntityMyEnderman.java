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

package de.Keyle.MyPet.compat.v1_12_R1.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyEnderman;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;

@EntitySize(width = 0.6F, height = 2.55F)
public class EntityMyEnderman extends EntityMyPet {

    private static final DataWatcherObject<Optional<IBlockData>> BLOCK_WATCHER = DataWatcher.a(EntityMyEnderman.class, DataWatcherRegistry.g);
    private static final DataWatcherObject<Boolean> SCREAMING_WATCHER = DataWatcher.a(EntityMyEnderman.class, DataWatcherRegistry.h);

    public EntityMyEnderman(World world, MyPet myPet) {
        super(world, myPet);
    }

    public int getBlockData() {
        return getMyPet().getBlock() != null ? getMyPet().getBlock().getData().getData() : 0;
    }

    public int getBlockID() {
        return getMyPet().getBlock() != null ? getMyPet().getBlock().getTypeId() : 0;
    }

    @Override
    protected String getDeathSound() {
        return "entity.endermen.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.endermen.hurt";
    }

    @Override
    protected String getLivingSound() {
        return getMyPet().isScreaming() ? "entity.endermen.scream" : "entity.endermen.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SHEARS && getMyPet().hasBlock() && getOwner().getPlayer().isSneaking()) {
                EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getBlock()));
                entityitem.pickupDelay = 10;
                entityitem.motY += (double) (this.random.nextFloat() * 0.05F);

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setBlock(null);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (getMyPet().getBlock() == null && Util.isBetween(1, 255, Item.getId(itemStack.getItem())) && getOwner().getPlayer().isSneaking()) {
                getMyPet().setBlock(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(BLOCK_WATCHER, Optional.absent());  // block data
        this.datawatcher.register(SCREAMING_WATCHER, false);          // face(angry)
    }

    @Override
    @SuppressWarnings("Guava")
    public void updateVisuals() {
        IBlockData data = CraftMagicNumbers.getBlock(getBlockID()).fromLegacyData(getBlockData());
        this.datawatcher.set(BLOCK_WATCHER, Optional.fromNullable(data));
        this.datawatcher.set(SCREAMING_WATCHER, getMyPet().isScreaming());
    }

    protected void doMyPetTick() {
        super.doMyPetTick();
        BehaviorImpl skill = getMyPet().getSkills().get(BehaviorImpl.class);
        Behavior.BehaviorMode behavior = skill.getBehavior();
        if (behavior == Behavior.BehaviorMode.Aggressive) {
                if (!getMyPet().isScreaming()) {
                    getMyPet().setScreaming(true);
                }
            } else {
                if (getMyPet().isScreaming()) {
                    getMyPet().setScreaming(false);
                }
            }
    }

    public MyEnderman getMyPet() {
        return (MyEnderman) myPet;
    }
}