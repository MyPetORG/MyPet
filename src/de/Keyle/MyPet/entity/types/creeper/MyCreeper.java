package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;


public class MyCreeper extends MyPet
{
    boolean isPowered = false;

    public MyCreeper(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Creeper";
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
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setBoolean("Powered", isPowered());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setPowered(info.getBoolean("Powered"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Creeper;
    }

    @Override
    public String toString()
    {
        return "MyCreeper{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ",powered=" + isPowered() + "}";
    }
}
