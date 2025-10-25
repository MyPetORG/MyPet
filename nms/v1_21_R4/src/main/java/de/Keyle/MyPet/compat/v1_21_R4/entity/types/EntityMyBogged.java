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

package de.Keyle.MyPet.compat.v1_21_R4.entity.types;

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyBogged;
import de.Keyle.MyPet.compat.v1_21_R4.entity.EntityMyPet;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyBogged extends EntityMyPet {

	public EntityMyBogged(Level world, MyPet myPet) {
		super(world, myPet);
	}
	@Override
	protected String getMyPetDeathSound() {
		return "entity.bogged.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.bogged.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.bogged.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.bogged.step", 0.15F, 1.0F);
	}

	@Override
	public MyBogged getMyPet() {
		return (MyBogged) myPet;
	}

	public void setPetEquipment(EquipmentSlot slot, ItemStack itemStack) {
		((ServerLevel) this.level()).getChunkSource().broadcastAndSend(this, new ClientboundSetEquipmentPacket(getId(), List.of(new Pair<>(net.minecraft.world.entity.EquipmentSlot.values()[slot.get19Slot()], itemStack))));
	}

	@Override
	public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot vanillaSlot) {
		return super.getItemBySlot(vanillaSlot);
	}
}
