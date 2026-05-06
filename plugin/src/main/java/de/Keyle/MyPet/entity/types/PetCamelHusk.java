package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import org.bukkit.Material;
import org.bukkit.entity.CamelHusk;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.CACTUS})
public class PetCamelHusk extends MyPet implements MyPetBaby, MyPetEquipment {

    public PetCamelHusk(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof CamelHusk camel)) return new ItemStack[]{null};
        return new ItemStack[]{camel.getInventory().getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof CamelHusk camel)) return null;
        if ("SADDLE".equals(slot.name())) return camel.getInventory().getSaddle();
        return null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof CamelHusk camel)) {
            super.setEquipmentBySlotName(slotName, item);
            return;
        }
        if ("SADDLE".equals(slotName)) {
            camel.getInventory().setSaddle(item);
        } else {
            super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof CamelHusk camel)) return;
        var inv = camel.getInventory();
        ItemStack saddle = inv.getSaddle();
        if (saddle != null && saddle.getType() != Material.AIR) {
            camel.getWorld().dropItem(camel.getLocation(), saddle);
            inv.setSaddle(null);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE");
    }
}
