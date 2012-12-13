package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.World;


public class EntityMyCreeper extends EntityMyPet
{
    public EntityMyCreeper(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/creeper.png";
        this.setPathfinder();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyCreeper(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setPowered(((MyCreeper) myPet).isPowered());
        }
    }

    public void setPowered(boolean powered)
    {
        if (!powered)
        {
            this.datawatcher.watch(17, (byte) 0);
        }
        else
        {
            this.datawatcher.watch(17, (byte) 1);
        }
        ((MyCreeper) myPet).isPowered = powered;
    }

    public boolean isPowered()
    {
        return this.datawatcher.getByte(17) == 1;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) -1)); // N/A
        this.datawatcher.a(17, new Byte((byte) 0));  // powered
    }

    @Override
    protected String aY()
    {
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.creeper.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.creeper.death";
    }
}
