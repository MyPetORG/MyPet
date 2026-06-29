package de.Keyle.MyPet.compat.v26_1_R1.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

public final class HandSlot {
    private HandSlot() {}

    public static EquipmentSlot getSlotForHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }
}