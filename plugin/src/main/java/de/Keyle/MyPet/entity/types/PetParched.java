package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.BONE})
public class PetParched extends MyPet implements MyPetEquipment {

    public PetParched(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
