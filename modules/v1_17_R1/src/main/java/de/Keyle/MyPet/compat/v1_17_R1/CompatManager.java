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

package de.Keyle.MyPet.compat.v1_17_R1;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_17_R1.services.EggIconService;
import de.Keyle.MyPet.compat.v1_17_R1.services.EntityConverterService;
import de.Keyle.MyPet.compat.v1_17_R1.services.RepositoryMyPetConverterService;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import java.lang.reflect.Method;

@Compat("v1_17_R1")
public class CompatManager extends de.Keyle.MyPet.api.util.CompatManager implements Listener {

    public static Method ENTITY_LIVING_broadcastItemBreak = ReflectionUtil.getMethod(EntityLiving.class, "d", EnumHand.class);

    @Override
	public void init() {
        MyPetApi.getServiceManager().registerService(EggIconService.class);
        MyPetApi.getServiceManager().registerService(EntityConverterService.class);
        MyPetApi.getServiceManager().registerService(RepositoryMyPetConverterService.class);
    }

    @Override
	public void enable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, MyPetApi.getPlugin());
    }

    @EventHandler()
    public void on(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.isGliding()) {
                Player player = (Player) event.getEntity();
                if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                    MyPet pet = MyPetApi.getMyPetManager().getMyPet(player);
                    if (pet.getStatus() == MyPet.PetState.Here) {
                        pet.removePet(true);
                    }
                }
            }
        }
    }
}
