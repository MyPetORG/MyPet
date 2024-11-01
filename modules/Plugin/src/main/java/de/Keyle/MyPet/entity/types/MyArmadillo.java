package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import net.md_5.bungee.api.ChatColor;

public class MyArmadillo extends MyPet implements de.Keyle.MyPet.api.entity.types.MyArmadillo {

    boolean isBaby = false;

    protected MyArmadillo(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public boolean isBaby() {
        return isBaby;
    }

    @Override
    public void setBaby(boolean flag) {
        this.isBaby = flag;
        if(status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Armadillo;
    }
    @Override
    public String toString() {
        return "MyArmadillo{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", saddle=" + ", baby=" + isBaby() + "}";
    }
}
