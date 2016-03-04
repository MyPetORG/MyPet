/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_9_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.compat.v1_9_R1.entity.EntityMyPet;
import net.minecraft.server.v1_9_R1.*;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyVillager extends EntityMyPet {
    private static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityMyVillager.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Integer> professionWatcher = DataWatcher.a(EntityMyVillager.class, DataWatcherRegistry.b);

    public EntityMyVillager(World world, MyPet myPet) {
        super(world, myPet);
    }

    protected String getDeathSound() {
        return "entity.villager.death";
    }

    protected String getHurtSound() {
        return "entity.villager.hurt";
    }

    protected String getLivingSound() {
        return "entity.villager.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (Configuration.MyPet.Villager.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(ageWatcher, false); // age
        this.datawatcher.register(professionWatcher, 0); // profession
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());
        this.datawatcher.set(professionWatcher, getMyPet().getProfession());
    }

    public MyVillager getMyPet() {
        return (MyVillager) myPet;
    }
}