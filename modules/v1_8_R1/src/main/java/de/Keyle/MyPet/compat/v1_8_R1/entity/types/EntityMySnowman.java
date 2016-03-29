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

package de.Keyle.MyPet.compat.v1_8_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_8_R1.entity.EntityMyPet;
import net.minecraft.server.v1_8_R1.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EntitySize(width = 0.7F, height = 1.7F)
public class EntityMySnowman extends EntityMyPet {
    Map<Location, Integer> snowMap = new HashMap<>();

    public EntityMySnowman(World world, MyPet myPet) {
        super(world, myPet);
    }

    private void addAirBlocksInBB(org.bukkit.World bukkitWorld, AxisAlignedBB axisalignedbb) {
        int minX = MathHelper.floor(axisalignedbb.a - 0.1);
        int maxX = MathHelper.floor(axisalignedbb.d + 1.1D);
        int minY = MathHelper.floor(axisalignedbb.b - 0.1);
        int maxY = MathHelper.floor(axisalignedbb.e + 1.1D);
        int minZ = MathHelper.floor(axisalignedbb.c - 0.1);
        int maxZ = MathHelper.floor(axisalignedbb.f + 1.1D);

        WorldServer world = ((CraftWorld) bukkitWorld).getHandle();

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (bukkitWorld.isChunkLoaded(x, z)) {
                    for (int y = minY - 1; y < maxY; y++) {
                        Block block = world.getType(new BlockPosition(x, y, z)).getBlock();

                        if (block == Blocks.AIR) {
                            snowMap.put(new Location(bukkitWorld, x, y, z), 10);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String getDeathSound() {
        return "step.snow";
    }

    @Override
    protected String getHurtSound() {
        return "step.snow";
    }

    protected String getLivingSound() {
        return "step.snow";
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (Configuration.MyPet.Snowman.FIX_SNOW_TRACK) {
            if (this.motX != 0D || this.motZ != 0D) {
                addAirBlocksInBB(this.world.getWorld(), this.getBoundingBox());
            }
            if (snowMap.size() > 0) {
                Iterator<Map.Entry<Location, Integer>> iter = snowMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Location, Integer> entry = iter.next();

                    int oldCounter = entry.getValue();
                    Location loc = entry.getKey();

                    if (oldCounter - 1 == 0) {
                        iter.remove();
                        if (loc.getBlock().getTypeId() == 0) {
                            byte data = loc.getBlock().getData();
                            loc.getBlock().setData((byte) 1);
                            loc.getBlock().setData(data);
                        }
                    } else {
                        snowMap.put(loc, oldCounter - 1);
                    }
                }
            }
        }
    }

    @Override
    public void playStepSound() {
        makeSound("step.snow", 0.15F, 1.0F);
    }
}