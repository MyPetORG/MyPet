package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.entity.Creature;

public interface MyPetEntity extends Creature {
    public MyPet getMyPet();

    public EntityMyPet getHandle();

    public boolean canMove();

    public MyPetType getPetType();

    public MyPetPlayer getOwner();
}
