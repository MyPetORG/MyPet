package de.Keyle.MyPet.entity.types.enderman;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;


public class CraftMyEnderman extends CraftMyPet
{
    public CraftMyEnderman(CraftServer server, EntityMyEnderman entityMyEnderman)
    {
        super(server, entityMyEnderman);
    }

    public short getBlockID()
    {
        return ((EntityMyEnderman) getHandle()).getBlockID();
    }

    public void setBlockID(short flag)
    {
        ((EntityMyEnderman) getHandle()).setBlockID(flag);
    }

    public short getBlockData()
    {
        return ((EntityMyEnderman) getHandle()).getBlockData();
    }

    public void setBlockData(short flag)
    {
        ((EntityMyEnderman) getHandle()).setBlockData(flag);
    }

    public boolean isScreaming()
    {
        return ((EntityMyEnderman) getHandle()).isScreaming();
    }

    public void setScreaming(boolean flag)
    {
        ((EntityMyEnderman) getHandle()).setScreaming(flag);
    }

    @Override
    public String toString()
    {
        return "CraftMyEnderman{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",BlockID=" + getBlockID() + ",BlockData=" + getBlockID() + "}";
    }


}