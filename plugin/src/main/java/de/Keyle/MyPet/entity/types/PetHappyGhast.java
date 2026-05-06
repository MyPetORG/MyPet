package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetFlyingEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.GHAST_TEAR}, leashFlags = {"Tamed"})
public class PetHappyGhast extends MyPet implements MyPetFlyingEntity, MyPetBaby {

    public PetHappyGhast(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public double getYSpawnOffset() {
        return 4;
    }
}
