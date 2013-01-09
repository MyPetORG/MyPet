/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skills.beacon.ContainerBeacon;
import de.Keyle.MyPet.skill.skills.beacon.MyPetCustomBeaconInventory;
import de.Keyle.MyPet.skill.skills.beacon.TileEntityBeacon;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.*;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_6.event.CraftEventFactory;
import org.bukkit.entity.Player;

@SkillName("Beacon")
@SkillProperties(
        parameterNames = {"Add_1", "Add_2", "Add_3", "Add_4", "Add_5", "Add_6", "Add_7", "Add_8", "level"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.Int})
public class Beacon extends MyPetGenericSkill
{
    public static int hungerDecreaseTime = 60;

    private TileEntityBeacon tileEntityBeacon;

    private int primaryEffectId = 0;
    private int secondaryEffectId = 0;
    private int hungerDecreaseTimer;
    private boolean active = false;
    private double range = 5;
    private int level = 0;
    private MyPetCustomBeaconInventory beaconInv;

    public Beacon(boolean addedByInheritance)
    {
        super(addedByInheritance);
        beaconInv = new MyPetCustomBeaconInventory();
    }

    @Override
    public boolean isActive()
    {
        return level > 0;
    }

    public int getLevel()
    {
        return level;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Beacon)
        {
            if (upgrade.getProperties().hasKey("level"))
            {
                level = upgrade.getProperties().getInt("level");
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return MyPetLanguage.getString("Name_Tier") + ": " + level;
    }

    public void reset()
    {
        stop(true);
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        for (int i = 1 ; i <= 8 ; i++)
        {
            if (getProperties().hasKey("Add_" + i))
            {
                int buffId = getProperties().getInt("Add_" + i);
                html = html.replace(" onselect=\"Add_" + i + "_" + buffId + "\"", " selected");
                html = html.replaceAll("\\sonselect=\"Add_" + i + "_\\d\"", "");
            }
        }
        if (getProperties().hasKey("level"))
        {
            int level = getProperties().getInt("level");
            html = html.replace(" onselect=\"level_" + level + "\"", " selected");
            html = html.replaceAll("\\sonselect=\"level_\\d\"", "");
        }

        return html;
    }

    @Override
    public void activate()
    {
        if (level > 0)
        {
            openBeacon(myPet.getOwner().getPlayer());
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.getName()));
        }
    }

    public void activate(Player player)
    {
        if (level > 0)
        {
            openBeacon(player);
        }
        else
        {
            player.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.getName()));
        }
    }

    @Override
    public void schedule()
    {
        if (myPet.status == PetState.Here && level > 0 && this.active && this.primaryEffectId > 0)
        {
            byte amplification = 0;

            if (this.primaryEffectId == this.secondaryEffectId)
            {
                amplification = 1;
            }
            double range = this.range * myPet.getHungerValue() / 100;
            for (Object entityObj : this.myPet.getCraftPet().getHandle().world.a(EntityHuman.class, myPet.getCraftPet().getHandle().boundingBox.grow(range, range, range)))
            {
                EntityHuman entityHuman = (EntityHuman) entityObj;
                entityHuman.addEffect(new MobEffect(this.primaryEffectId, 180, amplification, true));

                if (level > 3 && this.primaryEffectId != this.secondaryEffectId && this.secondaryEffectId > 0)
                {
                    entityHuman.addEffect(new MobEffect(this.secondaryEffectId, 180, 0, true));
                }
            }

            if (hungerDecreaseTime > 0 && hungerDecreaseTimer-- < 0)
            {
                myPet.setHungerValue(myPet.getHungerValue() - 1);
                hungerDecreaseTimer = hungerDecreaseTime;
            }
        }
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        if (nbtTagCompound.hasKey("Primary"))
        {
            this.primaryEffectId = nbtTagCompound.getInt("Primary");
        }
        if (nbtTagCompound.hasKey("Secondary"))
        {
            this.secondaryEffectId = nbtTagCompound.getInt("Secondary");
        }

        if (nbtTagCompound.hasKey("Active"))
        {
            this.active = nbtTagCompound.getBoolean("Active");
        }
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setInt("Primary", this.primaryEffectId);
        nbtTagCompound.setInt("Secondary", this.secondaryEffectId);
        nbtTagCompound.setBoolean("Active", this.active);
        return nbtTagCompound;
    }

    public void openBeacon(Player p)
    {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();

        if (tileEntityBeacon == null)
        {
            tileEntityBeacon = new TileEntityBeacon(this);
        }
        Container container = CraftEventFactory.callInventoryOpenEvent(entityPlayer, new ContainerBeacon(entityPlayer.inventory, beaconInv, tileEntityBeacon, this));
        if (container == null)
        {
            return;
        }

        int containerCounter = entityPlayer.nextContainerCounter();
        entityPlayer.playerConnection.sendPacket(new Packet100OpenWindow(containerCounter, 7, this.getName(), beaconInv.getSize()));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.windowId = containerCounter;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }


    public int getPrimaryEffectId()
    {
        return primaryEffectId;
    }

    public void setPrimaryEffectId(int effectId)
    {
        if (effectId > 0)
        {
            this.primaryEffectId = effectId;
            active = true;
            hungerDecreaseTimer = hungerDecreaseTime;
        }
        else
        {
            this.primaryEffectId = 0;
            if (secondaryEffectId == 0)
            {
                active = false;
            }
        }
    }

    public int getSecondaryEffectId()
    {
        return secondaryEffectId;
    }

    public void setSecondaryEffectId(int effectId)
    {
        if (effectId > 0)
        {
            this.secondaryEffectId = effectId;
            active = true;
            hungerDecreaseTimer = hungerDecreaseTime;
        }
        else
        {
            this.secondaryEffectId = 0;
            if (primaryEffectId == 0)
            {
                active = false;
            }
        }
    }

    public void stop(boolean reset)
    {
        this.active = false;
        if (reset)
        {
            primaryEffectId = 0;
            secondaryEffectId = 0;
        }
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Beacon(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}