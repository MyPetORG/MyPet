package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.entity.Creature;

public interface MyPetEntity extends Creature
{
    public MyPet getMyPet();

    public EntityMyPet getHandle();

    public boolean canMove();

    public MyPetType getPetType();

    public MyPetPlayer getOwner();
}
