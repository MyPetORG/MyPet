/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.MyPetAISit;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.ItemStack;
import net.minecraft.server.v1_6_R2.MathHelper;
import net.minecraft.server.v1_6_R2.World;
import org.bukkit.DyeColor;
import org.bukkit.Material;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyWolf extends EntityMyPet
{
    public static int GROW_UP_ITEM = Material.POTION.getId();
    protected boolean shaking;
    protected boolean isWet;
    protected float shakeCounter;
    private MyPetAISit sitPathfinder;

    public EntityMyWolf(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void applySitting(boolean sitting)
    {
        int i = this.datawatcher.getByte(16);
        if (sitting)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyWolf) myPet).isSitting = sitting;
    }

    public boolean canMove()
    {
        return !isSitting();
    }

    public DyeColor getCollarColor()
    {
        return ((MyWolf) myPet).collarColor;
    }

    public void setCollarColor(byte color)
    {
        this.datawatcher.watch(20, color);
        ((MyWolf) myPet).collarColor = DyeColor.getByWoolData(color);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound()
    {
        return "mob.wolf.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound()
    {
        return "mob.wolf.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String getLivingSound()
    {
        return !playIdleSound() ? null : (this.random.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "mob.wolf.whine" : "mob.wolf.panting") : "mob.wolf.bark");
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean handlePlayerInteraction(EntityHuman entityhuman)
    {
        if (super.handlePlayerInteraction(entityhuman))
        {
            return true;
        }
        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman))
        {
            if (itemStack != null && canUseItem())
            {
                if (itemStack.id == 351 && itemStack.getData() != ((MyWolf) myPet).getCollarColor().getDyeData() && getOwner().getPlayer().isSneaking())
                {
                    if (itemStack.getData() <= 15)
                    {
                        setCollarColor(DyeColor.getByDyeData((byte) itemStack.getData()));
                        if (!entityhuman.abilities.canInstantlyBuild)
                        {
                            if (--itemStack.count <= 0)
                            {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        return true;
                    }
                }
                else if (itemStack.id == GROW_UP_ITEM && getOwner().getPlayer().isSneaking())
                {
                    if (isBaby())
                    {
                        if (!entityhuman.abilities.canInstantlyBuild)
                        {
                            if (--itemStack.count <= 0)
                            {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        this.setBaby(false);
                        return true;
                    }
                }
            }
            this.sitPathfinder.toogleSitting();
            return true;
        }
        return false;
    }

    protected void initDatawatcher()
    {
        super.initDatawatcher();
        this.datawatcher.a(12, new Integer(0));         // age
        this.datawatcher.a(16, new Byte((byte) 0));     // tamed/angry/sitting
        this.datawatcher.a(17, "");                     // wolf owner name
        this.datawatcher.a(18, new Float(getHealth())); // tail height
        this.datawatcher.a(19, new Byte((byte) 0));     // N/A
        this.datawatcher.a(20, new Byte((byte) 14));    // collar color
    }

    public boolean isAngry()
    {
        return ((MyWolf) myPet).isAngry;
    }

    public void setAngry(boolean flag)
    {
        byte b0 = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (b0 | 0x2));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & 0xFFFFFFFD));
        }
        ((MyWolf) myPet).isAngry = flag;
    }

    public boolean isBaby()
    {
        return ((MyWolf) myPet).isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyWolf) myPet).isBaby = flag;
    }

    public boolean isSitting()
    {
        return ((MyWolf) myPet).isSitting;
    }

    public void setSitting(boolean sitting)
    {
        this.sitPathfinder.setSitting(sitting);
    }

    public boolean isTamed()
    {
        return ((MyWolf) myPet).isTamed;
    }

    public void setTamed(boolean flag)
    {
        int i = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (i | 0x4));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFB));
        }
        ((MyWolf) myPet).isTamed = flag;
    }

    @Override
    public void onLivingUpdate()
    {
        super.onLivingUpdate();
        if ((!this.world.isStatic) && (this.isWet) && (!this.shaking) && (!bM()) && (this.onGround)) // bM -> has pathentity
        {
            this.shaking = true;
            this.shakeCounter = 0.0F;
            this.world.broadcastEntityEffect(this, (byte) 8);
        }

        if (F()) // F() -> is in water
        {
            this.isWet = true;
            this.shaking = false;
            this.shakeCounter = 0.0F;
        }
        else if ((this.isWet || this.shaking) && this.shaking)
        {
            if (this.shakeCounter == 0.0F)
            {
                makeSound("mob.wolf.shake", aZ(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            this.shakeCounter += 0.05F;
            if (this.shakeCounter - 0.05F >= 2.0F)
            {
                this.isWet = false;
                this.shaking = false;
                this.shakeCounter = 0.0F;
            }

            if (this.shakeCounter > 0.4F)
            {
                float locY = (float) this.boundingBox.b;
                int i = (int) (MathHelper.sin((this.shakeCounter - 0.4F) * 3.141593F) * 7.0F);

                for (int j = 0 ; j < i ; j++)
                {
                    float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;
                    float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;

                    this.world.addParticle("splash", this.locX + offsetX, locY + 0.8F, this.locZ + offsetZ, this.motX, this.motY, this.motZ);
                }
            }
        }

        float tailHeight = 25.F * getHealth() / getMaxHealth();
        if (this.datawatcher.getFloat(18) != tailHeight)
        {
            this.datawatcher.watch(18, tailHeight); // update tail height
        }
    }

    @Override
    public void playStepSound()
    {
        makeSound("mob.wolf.step", 0.15F, 1.0F);
    }

    public void setCollarColor(DyeColor color)
    {
        setCollarColor(color.getWoolData());
    }

    public void setHealth(int i)
    {
        super.setHealth(i);
        this.bj();
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.sitPathfinder = new MyPetAISit(this);

            super.setMyPet(myPet);

            this.setBaby(((MyWolf) myPet).isBaby());
            this.setSitting(((MyWolf) myPet).isSitting());
            this.setTamed(((MyWolf) myPet).isTamed());
            this.setCollarColor(((MyWolf) myPet).getCollarColor());
        }
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        petPathfinderSelector.addGoal("Sit", 2, sitPathfinder);
    }
}