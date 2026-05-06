package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetAmphibiousEntity;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.entity.MyPetSunSensitive;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import org.bukkit.Material;
import org.bukkit.entity.ZombieNautilus;
import org.bukkit.inventory.ArmoredSaddledMountInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.ROTTEN_FLESH})
public class PetZombieNautilus extends MyPet implements MyPetEquipment, MyPetAmphibiousEntity, MyPetSunSensitive {

    public PetZombieNautilus(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof ZombieNautilus nautilus)) return new ItemStack[]{null, null};
        ArmoredSaddledMountInventory inv = nautilus.getInventory();
        return new ItemStack[]{inv.getArmor(), inv.getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof ZombieNautilus nautilus)) return null;
        return switch (slot.name()) {
            case "BODY" -> nautilus.getInventory().getArmor();
            case "SADDLE" -> nautilus.getInventory().getSaddle();
            default -> null;
        };
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof ZombieNautilus nautilus)) {
            super.setEquipmentBySlotName(slotName, item);
            return;
        }
        switch (slotName) {
            case "BODY" -> nautilus.getInventory().setArmor(item);
            case "SADDLE" -> nautilus.getInventory().setSaddle(item);
            default -> super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof ZombieNautilus nautilus)) return;
        ArmoredSaddledMountInventory inv = nautilus.getInventory();
        ItemStack saddle = inv.getSaddle();
        ItemStack armor = inv.getArmor();
        if (saddle != null && saddle.getType() != Material.AIR) {
            nautilus.getWorld().dropItem(nautilus.getLocation(), saddle);
            inv.setSaddle(null);
        }
        if (armor != null && armor.getType() != Material.AIR) {
            nautilus.getWorld().dropItem(nautilus.getLocation(), armor);
            inv.setArmor(null);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE", "BODY");
    }
}
