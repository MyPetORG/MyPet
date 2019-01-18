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

package de.Keyle.MyPet.compat.v1_13_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyTurtle;
import de.Keyle.MyPet.compat.v1_13_R1.entity.EntityMyPet;
import net.minecraft.server.v1_13_R1.*;

@EntitySize(width = 1.2F, height = 0.4F)
public class EntityMyTurtle extends EntityMyPet {

    private static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<BlockPosition> homePosWatcher = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.l);
    private static final DataWatcherObject<Boolean> hasEggWatcher = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Boolean> unusedWatcher1 = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<BlockPosition> travelPosWatcher = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.l);
    private static final DataWatcherObject<Boolean> unusedWatcher2 = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Boolean> unusedWatcher3 = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);

    public EntityMyTurtle(World world, MyPet myPet) {
        super(EntityTypes.TURTLE, world, myPet);
        this.datawatcher.register(homePosWatcher, BlockPosition.ZERO);
        this.datawatcher.register(hasEggWatcher, false);
        this.datawatcher.register(travelPosWatcher, BlockPosition.ZERO);
        this.datawatcher.register(unusedWatcher2, false);
        this.datawatcher.register(unusedWatcher3, false);
        this.datawatcher.register(unusedWatcher1, false);
    }

    @Override
    protected String getDeathSound() {
        return "entity.turtle.death" + (isBaby() ? "_baby" : "");
    }

    @Override
    protected String getHurtSound() {
        return "entity.turtle.hurt" + (isBaby() ? "_baby" : "");
    }

    protected String getLivingSound() {
        return "entity.turtle.ambient_land";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (Configuration.MyPet.Cow.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
        this.datawatcher.register(ageWatcher, false);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());
    }

    public MyTurtle getMyPet() {
        return (MyTurtle) myPet;
    }
}