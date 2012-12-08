package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;


public class MyCreeper extends MyPet
{
    boolean isPowered = false;

    public MyCreeper(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Creeper";
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Creeper;
    }

    public void setPowered(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyCreeper) getCraftPet()).setPowered(flag);
        }
        this.isPowered = flag;
    }

    public boolean isPowered()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyCreeper) getCraftPet()).isPowered();
        }
        else
        {
            return isPowered;
        }
    }

    @Override
    public String toString()
    {
        return "MyCreeper{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ",powered=" + isPowered() + "}";
    }
}
