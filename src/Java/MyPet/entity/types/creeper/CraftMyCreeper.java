package Java.MyPet.entity.types.creeper;

import Java.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.CraftServer;


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
