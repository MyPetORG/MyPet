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
import net.minecraft.server.v1_4_R1.*;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_R1.event.CraftEventFactory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SkillName("Beacon")
@SkillProperties(
        parameterNames = {"1_1", "1_3", "1_11", "1_8", "1_5", "2_1", "2_3", "2_11", "2_8", "2_5", "2_10", "duration", "range", "addset_duration", "addset_range"},
        parameterTypes = {NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Int, NBTdatatypes.Double, NBTdatatypes.String, NBTdatatypes.String})
public class Beacon extends MyPetGenericSkill
{
    public static int hungerDecreaseTime = 60;

    private TileEntityBeacon tileEntityBeacon;

    private static Map<Integer, String> buffNames = new HashMap<Integer, String>();
    private Map<Integer, Boolean> primaryActive = new HashMap<Integer, Boolean>();
    private int[] primaryBuffs = {1, 3, 11, 8, 5};
    private Map<Integer, Boolean> secundaryActive = new HashMap<Integer, Boolean>();
    private int[] secundaryBuffs = {1, 3, 11, 8, 5, 10};
    private int primaryEffectId = 0;
    private int secondaryEffectId = 0;
    private int hungerDecreaseTimer;
    private boolean active = false;
    private double range = 0;
    private int duration = 0;
    private int level = 0;
    private MyPetCustomBeaconInventory beaconInv;
    public ItemStack tributeItem;

    static
    {
        buffNames.put(1, "Speed");
        buffNames.put(3, "Haste");
        buffNames.put(5, "Strength");
        buffNames.put(8, "JumpBoost");
        buffNames.put(10, "Regeneration");
        buffNames.put(11, "Resistance");
    }

    public Beacon(boolean addedByInheritance)
    {
        super(addedByInheritance);
        beaconInv = new MyPetCustomBeaconInventory(this);
        primaryActive.put(1, false);
        primaryActive.put(3, false);
        primaryActive.put(5, false);
        primaryActive.put(8, false);
        primaryActive.put(11, false);
        secundaryActive.put(1, false);
        secundaryActive.put(3, false);
        secundaryActive.put(5, false);
        secundaryActive.put(8, false);
        secundaryActive.put(10, false);
        secundaryActive.put(11, false);
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

    public ItemStack getTributeItem()
    {
        return tributeItem;
    }

    public void setTributeItem(ItemStack itemStack)
    {
        beaconInv.setItem(0, itemStack);
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Beacon)
        {
            level = 4;
            for (int primaryBuffId : primaryBuffs)
            {
                if (upgrade.getProperties().hasKey("1_" + primaryBuffId))
                {
                    primaryActive.put(primaryBuffId, upgrade.getProperties().getBoolean("1_" + primaryBuffId));
                }
            }

            for (int secundaryBuffId : secundaryBuffs)
            {
                if (upgrade.getProperties().hasKey("2_" + secundaryBuffId))
                {
                    if (secundaryBuffId == 10)
                    {
                        secundaryActive.put(secundaryBuffId, upgrade.getProperties().getBoolean("2_" + secundaryBuffId));
                    }
                    else
                    {
                        if (primaryActive.get(secundaryBuffId))
                        {
                            secundaryActive.put(secundaryBuffId, upgrade.getProperties().getBoolean("2_" + secundaryBuffId));
                        }
                        else
                        {
                            secundaryActive.put(secundaryBuffId, false);
                        }
                    }
                }
            }
            if (upgrade.getProperties().hasKey("duration"))
            {
                if (!upgrade.getProperties().hasKey("addset_duration") || upgrade.getProperties().getString("addset_duration").equals("add"))
                {
                    duration += upgrade.getProperties().getInt("duration");
                }
                else
                {
                    duration = upgrade.getProperties().getInt("duration");
                }
            }
            if (upgrade.getProperties().hasKey("range"))
            {
                if (!upgrade.getProperties().hasKey("addset_range") || upgrade.getProperties().getString("addset_range").equals("add"))
                {
                    range += upgrade.getProperties().getDouble("range");
                }
                else
                {
                    range = upgrade.getProperties().getDouble("range");
                }
            }
            if (!quiet)
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddBeacon").replace("%range%", String.format("%1.2f", range)).replace("%duration%", "" + duration)));
                myPet.sendMessageToOwner("  " + getFormattedValue());
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        String availableBuffs = "";
        for (int primaryBuffId : primaryBuffs)
        {
            if (primaryActive.get(primaryBuffId))
            {
                if (!availableBuffs.equalsIgnoreCase(""))
                {
                    availableBuffs += ", ";
                }
                availableBuffs += MyPetLanguage.getString("Name_" + buffNames.get(primaryBuffId));
                if (secundaryActive.get(primaryBuffId))
                {
                    availableBuffs += " (II)";
                }
            }
        }
        if (secundaryActive.get(10))
        {
            if (!availableBuffs.equalsIgnoreCase(""))
            {
                availableBuffs += ", ";
            }
            availableBuffs += MyPetLanguage.getString("Name_" + buffNames.get(10));
        }
        return availableBuffs;
    }

    public void reset()
    {
        stop(true);
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        for (int i = 0 ; i < 11 ; i++)
        {
            String name = getClass().getAnnotation(SkillProperties.class).parameterNames()[i];
            if (getProperties().hasKey(name))
            {
                if (!getProperties().getBoolean(name))
                {
                    html = html.replace("name=\"" + name + "\" checked", "name=\"" + name + "\"");
                }
            }
        }
        if (getProperties().hasKey("duration"))
        {
            html = html.replace("name=\"duration\" value=\"0\"", "name=\"duration\" value=\"" + getProperties().getInt("duration") + "\"");
            if (getProperties().hasKey("addset_duration"))
            {
                if (getProperties().getString("addset_duration").equals("set"))
                {
                    html = html.replace("name=\"addset_duration\" value=\"add\" checked", "name=\"addset_duration\" value=\"add\"");
                    html = html.replace("name=\"addset_duration\" value=\"set\"", "name=\"addset_duration\" value=\"set\" checked");
                }
            }
        }
        if (getProperties().hasKey("range"))
        {
            html = html.replace("name=\"range\" value=\"0.0\"", "name=\"range\" value=\"" + String.format(Locale.ENGLISH, "%1.2f", getProperties().getDouble("range")) + "\"");
            if (getProperties().hasKey("addset_range"))
            {
                if (getProperties().getString("addset_range").equals("set"))
                {
                    html = html.replace("name=\"addset_range\" value=\"add\" checked", "name=\"addset_range\" value=\"add\"");
                    html = html.replace("name=\"addset_range\" value=\"set\"", "name=\"addset_range\" value=\"set\" checked");
                }
            }
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

    public boolean activate(boolean primary, int effectId)
    {
        if (level > 0)
        {
            if (primary)
            {
                if (primaryActive.get(effectId))
                {
                    setPrimaryEffectId(effectId);
                    return true;
                }
                else
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BeaconBuffNotActive")).replace("%buff%", MyPetLanguage.getString("Name_" + buffNames.get(effectId))));
                    return false;
                }
            }
            else
            {
                if (secundaryActive.get(effectId))
                {
                    setSecondaryEffectId(effectId);
                    return true;
                }
                else
                {
                    if (effectId != 10)
                    {
                        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BeaconImprovedBuffNotActive")).replace("%buff%", MyPetLanguage.getString("Name_" + buffNames.get(effectId))));
                    }
                    else
                    {
                        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BeaconBuffNotActive")).replace("%buff%", MyPetLanguage.getString("Name_" + buffNames.get(effectId))));
                    }
                    return false;
                }
            }
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.getName()));
        }
        return false;
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
            double range = this.range * myPet.getHungerValue() / 100.;
            for (Object entityObj : this.myPet.getCraftPet().getHandle().world.a(EntityHuman.class, myPet.getCraftPet().getHandle().boundingBox.grow(range, range, range)))
            {
                EntityHuman entityHuman = (EntityHuman) entityObj;
                entityHuman.addEffect(new MobEffect(this.primaryEffectId, duration * 20, amplification, true));

                if (level > 3 && this.primaryEffectId != this.secondaryEffectId && this.secondaryEffectId > 0)
                {
                    entityHuman.addEffect(new MobEffect(this.secondaryEffectId, duration * 20, 0, true));
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
        if (nbtTagCompound.hasKey("Item"))
        {
            setTributeItem(ItemStack.createStack(nbtTagCompound.getCompound("Item")));
        }
    }

    @Override
    public NBTTagCompound save()
    {

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setInt("Primary", this.primaryEffectId);
        nbtTagCompound.setInt("Secondary", this.secondaryEffectId);
        nbtTagCompound.setBoolean("Active", this.active);
        if (tributeItem != null)
        {
            NBTTagCompound item = new NBTTagCompound();
            tributeItem.save(item);
            nbtTagCompound.setCompound("Item", item);
        }
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
        entityPlayer.playerConnection.sendPacket(new Packet100OpenWindow(containerCounter, 7, myPet.petName + "'s - Beacon", beaconInv.getSize()));
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