package de.Keyle.MyPet.compat.v1_21_R1.entity.types;

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyBogged;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;

import java.util.Arrays;

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
    public void updateVisuals() {
    }

    @Override
    public MyBogged getMyPet() {
        return (MyBogged) myPet;
    }

    public void setPetEquipment(EquipmentSlot slot, ItemStack itemStack) {
        ((ServerLevel) this.level()).getChunkSource().broadcastAndSend(this, new ClientboundSetEquipmentPacket(getId(), Arrays.asList(new Pair<>(net.minecraft.world.entity.EquipmentSlot.values()[slot.get19Slot()], itemStack))));
    }

    @Override
    public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot vanillaSlot) {
        return super.getItemBySlot(vanillaSlot);
    }
}
