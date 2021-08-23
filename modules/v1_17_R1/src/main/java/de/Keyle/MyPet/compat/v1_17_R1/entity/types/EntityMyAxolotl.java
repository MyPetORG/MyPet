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

package de.Keyle.MyPet.compat.v1_17_R1.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyAxolotl;
import de.Keyle.MyPet.api.entity.types.MyMooshroom;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyAxolotl extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyAxolotl.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyAxolotl.class, DataWatcherRegistry.b);

    public EntityMyAxolotl(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.axolotl.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.axolotl.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.axolotl.idle_air";
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(VARIANT_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        this.getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        this.getDataWatcher().set(VARIANT_WATCHER, getMyPet().getVariant());
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.axolotl.step", 0.15F, 1.0F);
    }

    @Override
    public MyAxolotl getMyPet() {
        return (MyAxolotl) myPet;
    }
}
