/*
* Copyright (C) 2011 Keyle
*
* This file is part of MyWolf.
*
* MyWolf is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* MyWolf is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
*/

package de.Keyle.MyWolf;

import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.getspout.spout.inventory.CustomMCInventory;
import org.getspout.spoutapi.SpoutManager;

import java.util.HashMap;
import java.util.Map;

public class MyWolf
{
    public String Name = "Wolf";
    public final String Owner;
    private int ID;
    public int HealthMax = 6;
    private int HealthNow = HealthMax;
    public int Lives = 5;
    public Wolf Wolf;
    public int RespawnTime = 0;

    private int Timer = -1;

    private int SitTimer = 15;
    private boolean isSitting = false;
    public boolean isPickup = false;

    //public GenericTexture hpbar = new GenericTexture("http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/hpbar.png");

    public static enum BehaviorState
    {
        Normal, Friendly, Aggressive, Raid
    }

    public static enum WolfState
    {
        Dead, Despawned, Here
    }

    public BehaviorState Behavior = BehaviorState.Normal;
    public WolfState Status = WolfState.Despawned;

    public CustomMCInventory inv;

    private Location Location;

    public final Map<String, Boolean> Abilities = new HashMap<String, Boolean>();
    public final MyWolfExperience Experience;

    public MyWolf(String Owner)
    {
        this.Owner = Owner;
        this.inv = new CustomMCInventory(0, Owner);
        Experience = new MyWolfExperience(MyWolfConfig.ExpFactor, this);
    }

    /*
    public void updateHPbar(int HP)
    {
        if(getOwner() != null)
        {
            double p = (HP<0?0:HP) * 100 / HealthMax;
            hpbar.setWidth(SpoutCraftPlayer.getPlayer(getOwner()).getMainScreen().getHealthBar().getWidth()* ((Double)p).intValue() /100);
            hpbar.render();
            hpbar.setDirty(true);
        }
    }
    public void updateHPbar()
    {
        if(getOwner() != null)
        {
            double p = (HealthNow<0?0:HealthNow) * 100 / HealthMax;
            hpbar.setWidth(SpoutCraftPlayer.getPlayer(getOwner()).getMainScreen().getHealthBar().getWidth()* ((Double)p).intValue() /100);
            hpbar.render();
            hpbar.setDirty(true);
        }
    }
    */
    public void SetName(String Name)
    {
        this.Name = Name;
        inv.setName(Name + "\'s Inventory (" + inv.getSize() + ")");
        String NameColor;
        if (MyWolfConfig.NameColor >= 0 && MyWolfConfig.NameColor <= 0xf)
        {
            NameColor = "§" + MyWolfConfig.NameColor;
        }
        else
        {
            if (getHealth() > HealthMax / 3 * 2)
            {
                NameColor = "" + ChatColor.GREEN;
            }
            else if (getHealth() > HealthMax / 3)
            {
                NameColor = "" + ChatColor.YELLOW;
            }
            else
            {
                NameColor = "" + ChatColor.RED;
            }
        }
        if (Status == WolfState.Here)
        {
            SpoutManager.getAppearanceManager().setGlobalTitle(Wolf, NameColor + Name);
        }
    }

    public void SetName()
    {
        String NameColor;
        if (MyWolfConfig.NameColor >= 0 && MyWolfConfig.NameColor <= 0xf)
        {
            NameColor = "§" + MyWolfConfig.NameColor;
        }
        else
        {
            if (getHealth() > HealthMax / 3 * 2)
            {
                NameColor = "" + ChatColor.GREEN;
            }
            else if (getHealth() > HealthMax / 3)
            {
                NameColor = "" + ChatColor.YELLOW;
            }
            else
            {
                NameColor = "" + ChatColor.RED;
            }
        }
        if (Status == WolfState.Here)
        {
            SpoutManager.getAppearanceManager().setGlobalTitle(Wolf, NameColor + this.Name);
        }
    }

    public void SetName(int HP)
    {
        String NameColor;
        if (MyWolfConfig.NameColor >= 0 && MyWolfConfig.NameColor <= 0xf)
        {
            NameColor = "§" + MyWolfConfig.NameColor;
        }
        else
        {
            if (HP > HealthMax / 3 * 2)
            {
                NameColor = "" + ChatColor.GREEN;
            }
            else if (HP > HealthMax / 3)
            {
                NameColor = "" + ChatColor.YELLOW;
            }
            else
            {
                NameColor = "" + ChatColor.RED;
            }
        }
        if (Status == WolfState.Here)
        {
            SpoutManager.getAppearanceManager().setGlobalTitle(Wolf, NameColor + this.Name);
        }
    }

    public void OpenInventory()
    {
        if (MyWolfUtil.hasSkill(Abilities, "Inventory"))
        {
            EntityPlayer eh = ((CraftPlayer) getOwner()).getHandle();
            eh.a(inv);
        }
    }

    public void removeWolf()
    {
        StopTimer();
        isSitting = Wolf.isSitting();
        HealthNow = Wolf.getHealth();
        Location = Wolf.getLocation();
        Status = WolfState.Despawned;
        Wolf.remove();
    }

    void RespawnWolf()
    {
        if (Status != WolfState.Here)
        {
            HealthNow = HealthMax;
            Location = getOwner().getLocation();
            getOwner().sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_OnRespawn")).replace("%wolfname%", Name));
            createWolf(false);
            RespawnTime = 0;
        }
    }

    public void createWolf(boolean sitting)
    {
        if (Status == WolfState.Here || getOwner() == null)
        {
        }
        else
        {
            if (RespawnTime <= 0)
            {
                Wolf = (Wolf) MyWolfPlugin.Plugin.getServer().getWorld(Location.getWorld().getName()).spawnCreature(Location, CreatureType.WOLF);
                Wolf.setOwner(getOwner());
                Wolf.setSitting(sitting);
                Location = Wolf.getLocation();
                Wolf.setHealth(HealthNow);
                ID = Wolf.getEntityId();

                Status = WolfState.Here;
                SetName();
                //updateHPbar();
            }
            Timer();
        }
    }

    public void createWolf(Wolf wolf)
    {
        Wolf = wolf;
        ID = Wolf.getEntityId();
        Location = Wolf.getLocation();
        Status = WolfState.Here;
        SetName();
        //updateHPbar();
        Timer();
    }

    public void setHealth(int d)
    {
        if (d > HealthMax)
        {
            HealthNow = HealthMax;
        }
        else
        {
            HealthNow = d;
        }
        if (Status == WolfState.Here)
        {
            if (d > 20)
            {
                Wolf.setHealth(20);
            }
            else
            {
                Wolf.setHealth(HealthNow);
            }
        }
        SetName();
        //updateHPbar();
    }

    public int getHealth()
    {

        if (Status == WolfState.Here)
        {
            return Wolf.getHealth();
        }
        else
        {
            return HealthNow;
        }
    }

    public int getID()
    {
        if (Status == WolfState.Here)
        {
            return Wolf.getEntityId();
        }
        else
        {
            return ID;
        }
    }

    public Location getLocation()
    {
        if (Status == WolfState.Here)
        {
            return Wolf.getLocation();
        }
        else
        {
            return Location;
        }
    }

    public void setLocation(Location loc)
    {
        this.Location = loc;
        if (Status == WolfState.Here)
        {
            Wolf.teleport(loc);
        }
    }

    public boolean isSitting()
    {
        if (Status == WolfState.Here)
        {
            return Wolf.isSitting();
        }
        else
        {
            return isSitting;
        }
    }

    public void setSitting(boolean sitting)
    {
        if (Status == WolfState.Here)
        {
            Wolf.setSitting(sitting);
            this.isSitting = sitting;
        }
        else
        {
            this.isSitting = sitting;
        }
    }

    public void ResetSitTimer()
    {
        if(MyWolfConfig.SitdownTime > 0)
        {
            SitTimer = MyWolfConfig.SitdownTime;
        }
    }

    public void StopTimer()
    {
        if (Timer != -1)
        {
            MyWolfPlugin.Plugin.getServer().getScheduler().cancelTask(Timer);
            Timer = -1;
        }
    }

    public void Timer()
    {
        if (Status != WolfState.Despawned)
        {
            if (Timer != -1)
            {
                StopTimer();
            }
            Timer = MyWolfPlugin.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWolfPlugin.Plugin, new Runnable()
            {
                public void run()
                {
                    if (Status == WolfState.Despawned || getOwner() == null)
                    {
                        StopTimer();
                    }
                    else
                    {
                        if (Status == WolfState.Here)
                        {
                            SitTimer--;
                            if (MyWolfConfig.SitdownTime > 0 && SitTimer <= 0)
                            {
                                Wolf.setSitting(true);
                            }
                            if (isPickup)
                            {
                                for (Entity e : Wolf.getNearbyEntities(MyWolfConfig.PickupRange, MyWolfConfig.PickupRange, MyWolfConfig.PickupRange))
                                {
                                    if (e instanceof Item)
                                    {
                                        Item item = (Item) e;

                                        PlayerPickupItemEvent ppievent = new PlayerPickupItemEvent(getOwner(), item, item.getItemStack().getAmount());
                                        MyWolfUtil.getServer().getPluginManager().callEvent(ppievent);

                                        if (ppievent.isCancelled())
                                        {
                                            continue;
                                        }

                                        int ItemID = item.getItemStack().getTypeId();
                                        int ItemDuarbility = item.getItemStack().getDurability();
                                        int ItemAmount = item.getItemStack().getAmount();
                                        int ItemMaxStack = item.getItemStack().getMaxStackSize();

                                        for (int i = 0 ; i < inv.getSize() ; i++)
                                        {
                                            if (inv.getItem(i) != null && inv.getItem(i).id == ItemID && inv.getItem(i).damage == ItemDuarbility && inv.getItem(i).count < ItemMaxStack)
                                            {
                                                if (ItemAmount >= ItemMaxStack - inv.getItem(i).count)
                                                {
                                                    ItemAmount = ItemAmount - (ItemMaxStack - inv.getItem(i).count);
                                                    inv.getItem(i).count = ItemMaxStack;
                                                }
                                                else
                                                {
                                                    inv.getItem(i).count += ItemAmount;
                                                    ItemAmount = 0;
                                                    break;
                                                }
                                            }
                                        }
                                        for (int i = 0 ; i < inv.getSize() ; i++)
                                        {
                                            if (ItemAmount <= 0)
                                            {
                                                break;
                                            }
                                            if (inv.getItem(i) == null)
                                            {
                                                if (ItemAmount <= ItemMaxStack)
                                                {
                                                    inv.setItem(i, new ItemStack(ItemID, ItemAmount, ItemDuarbility));
                                                    ItemAmount = 0;
                                                }
                                                else
                                                {
                                                    inv.setItem(i, new ItemStack(ItemID, ItemMaxStack, ItemDuarbility));
                                                    ItemAmount -= ItemMaxStack;
                                                }
                                            }
                                        }

                                        if (ItemAmount == 0)
                                        {
                                            e.remove();
                                        }
                                        else
                                        {
                                            item.getItemStack().setAmount(ItemAmount);
                                        }
                                    }
                                }
                            }
                            if (Behavior == BehaviorState.Aggressive)
                            {
                                if (Wolf.getTarget() == null)
                                {
                                    for (Entity e : Wolf.getNearbyEntities(10, 10, 10))
                                    {
                                        if (MyWolfUtil.getCreatureType(e) != null)
                                        {
                                            Wolf.setTarget((LivingEntity) e);
                                        }
                                    }
                                }
                            }
                        }
                        if (Status == WolfState.Dead)
                        {
                            RespawnTime--;
                            if (RespawnTime <= 0)
                            {
                                RespawnWolf();
                            }
                        }
                    }
                }
            }, 0L, 20L);
        }
    }

    public Player getOwner()
    {
        for (Player p : MyWolfPlugin.Plugin.getServer().getOnlinePlayers())
        {
            if (p.getName().equals(Owner) && !MyWolfUtil.isNPC(p))
            {
                return p;
            }
        }
        return null;
    }

    public void sendMessageToOwner(String Text)
    {
        if (getOwner() != null)
        {
            getOwner().sendMessage(Text);
        }
    }
}
