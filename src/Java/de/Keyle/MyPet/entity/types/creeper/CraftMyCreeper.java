package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.v1_4_5.CraftServer;


public class CraftMyCreeper extends CraftMyPet
{
    public CraftMyCreeper(CraftServer server, EntityMyCreeper entityMyCreeper)
    {
        super(server, entityMyCreeper);
    }

    public boolean isPowered()
    {
        return ((EntityMyCreeper) getHandle()).isPowered();
    }

    public void setPowered(boolean sheared)
    {
        ((EntityMyCreeper) getHandle()).setPowered(sheared);
    }

    @Override
    public String toString()
    {
        return "CraftMyCreeper{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",powered=" + isPowered() + "}";
    }
}