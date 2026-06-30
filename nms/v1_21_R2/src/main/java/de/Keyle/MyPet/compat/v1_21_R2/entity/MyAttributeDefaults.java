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

package de.Keyle.MyPet.compat.v1_21_R2.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.HashMap;

public class MyAttributeDefaults {

    private static final HashMap<EntityType<? extends LivingEntity>, AttributeSupplier> defaultAttribute = new HashMap<>();


    public static AttributeSupplier getAttribute(EntityType<?> types) {
        AttributeSupplier supplier = defaultAttribute.get(types);
        // The map only holds custom mypet_* types (filled at registration). For the underlying vanilla
        // type, resolve the supplier from vanilla via DefaultAttributes instead of a hardcoded list,
        // so new/renamed mobs are covered and we never build a null-supplier AttributeMap (which
        // NPEs in AttributeMap.getInstance when equipment attribute modifiers are applied).
        if (supplier == null && DefaultAttributes.hasSupplier(types)) {
        	supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) types);
        }
        if (supplier == null) {
        	supplier = Mob.createMobAttributes().build();
        }
        return supplier;
    }

    public static void registerCustomEntityType(EntityType<? extends LivingEntity> customType, EntityType<? extends LivingEntity> rootType) {
        defaultAttribute.put(customType, getAttribute(rootType));
    }

}