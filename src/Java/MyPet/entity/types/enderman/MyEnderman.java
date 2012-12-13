package Java.MyPet.entity.types.enderman;


import Java.MyPet.util.MyPetPlayer;
import Java.MyPet.entity.types.MyPet;
import Java.MyPet.entity.types.MyPetType;
import net.minecraft.server.NBTTagCompound;

public class MyEnderman extends MyPet
{

    short BlockID = 0;
    short BlockData = 0;
    public boolean isScreaming = false;

    public MyEnderman(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Enderman";
    }

    public short getBlockID()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyEnderman) getCraftPet()).getBlockID();
        }
        else
        {
            return BlockID;
        }
    }

    public void setBlockID(short flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyEnderman) getCraftPet()).setBlockID(flag);
        }
        this.BlockID = flag;
    }

    public short getBlockData()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyEnderman) getCraftPet()).getBlockData();
        }
        else
        {
            return BlockData;
        }
    }

    public void setBlockData(short flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyEnderman) getCraftPet()).setBlockData(flag);
        }
        this.BlockData = flag;
    }

    public boolean isScreaming()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyEnderman) getCraftPet()).isScreaming();
        }
        else
        {
            return isScreaming;
        }
    }

    public void setScreaming(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyEnderman) getCraftPet()).setScreaming(flag);
        }
        this.isScreaming = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setShort("BlockID", getBlockID());
        info.setShort("BlockData", getBlockData());
        //info.setBoolean("Screaming", isScreaming());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setBlockID(info.getShort("BlockID"));
        setBlockData(info.getShort("BlockData"));
        //setScreaming(info.getBoolean("Screaming"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Enderman;
    }

    @Override
    public String toString()
    {
        return "MyEnderman{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ",BlockID=" + getBlockID() + ",BlockData=" + getBlockData() + "}";
    }

}
