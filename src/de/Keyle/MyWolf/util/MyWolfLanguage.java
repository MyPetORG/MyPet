/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
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

package de.Keyle.MyWolf.util;

import java.util.HashMap;
import java.util.Map;

public class MyWolfLanguage
{
    private final MyWolfConfiguration MWC;

    public MyWolfLanguage(MyWolfConfiguration MWC)
    {
        this.MWC = MWC;
    }

    private static final Map<String, String> LV = new HashMap<String, String>();

    public static String getString(String Variable)
    {
        if (LV.containsKey(Variable))
        {
            return LV.get(Variable);
        }
        return Variable;
    }

    public void setProperty(String key, Object value)
    {
        if (MWC.Config.get(key) == null)
        {
            MWC.Config.set(key, value);
        }
    }

    public void setDefault()
    {
        setProperty("MyWolf.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf.");
        setProperty("MyWolf.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%");
        setProperty("MyWolf.Message.inventory", "%aqua%%wolfname%%white% has now an inventory with %size%slots.");
        setProperty("MyWolf.Message.addlive", "%green%+1 life for %aqua%%wolfname%");
        setProperty("MyWolf.Message.maxlives", "%aqua%%wolfname%%red% has reached the maximum of %maxlives% lives.");
        setProperty("MyWolf.Message.addpickup", "%aqua%%wolfname%%white% now can pickup items in a range of %range%.");
        setProperty("MyWolf.Message.addhp", "%aqua%%wolfname%%white% +1 maxHP for %aqua%%wolfname%");
        setProperty("MyWolf.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . .");
        setProperty("MyWolf.Message.deathmessage.text", "%aqua%%wolfname%%white% ");
        setProperty("MyWolf.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec");
        setProperty("MyWolf.Message.onrespawn", "%aqua%%wolfname%%white% respawned");
        setProperty("MyWolf.Message.callwhendead", "%aqua%%wolfname%%white% is dead! and respawns in %gold%%time%%white% sec");
        setProperty("MyWolf.Message.call", "%aqua%%wolfname%%white% comes to you.");
        setProperty("MyWolf.Message.call", "%aqua%%wolfname%%white% go to home.");
        setProperty("MyWolf.Message.callfirst", "You must call your wolf first.");
        setProperty("MyWolf.Message.userdonthavewolf", "This user doesn't have a MyWolf!");
        setProperty("MyWolf.Message.donthavewolf", "You don't have a MyWolf!");
        setProperty("MyWolf.Message.otherdonthavewolf", "%aqua%%playername%%white% don't have a MyWolf!");
        setProperty("MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%");
        setProperty("MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%");
        setProperty("MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green%free%white% . . .");
        setProperty("MyWolf.Message.stopattack", "Your wolf should now %green%stop%white% attacking!");
        setProperty("MyWolf.Message.inventorywhileswimming", "You can't open the inventory while the wolf is swimming!");
        setProperty("MyWolf.Message.deathmessage.creeper", "was killed by a Creeper.");
        setProperty("MyWolf.Message.deathmessage.zombie", "was killed by a Zombie.");
        setProperty("MyWolf.Message.deathmessage.unknow", "was killed by an unknown source.");
        setProperty("MyWolf.Message.deathmessage.you", "was killed by %red%YOU.");
        setProperty("MyWolf.Message.deathmessage.spider", "was killed by a Spider.");
        setProperty("MyWolf.Message.deathmessage.cavespider", "was killed by a Cave Spider.");
        setProperty("MyWolf.Message.deathmessage.giant", "was killed by a Giant.");
        setProperty("MyWolf.Message.deathmessage.slime", "was killed by a Slime.");
        setProperty("MyWolf.Message.deathmessage.ghast", "was killed by a Ghast.");
        setProperty("MyWolf.Message.deathmessage.wolf", "was killed by a Wolf.");
        setProperty("MyWolf.Message.deathmessage.enderman", "was killed by a Enderman.");
        setProperty("MyWolf.Message.deathmessage.player", "was killed by %player%.");
        setProperty("MyWolf.Message.deathmessage.drowning", "drowned.");
        setProperty("MyWolf.Message.deathmessage.fall", " died by falling down.");
        setProperty("MyWolf.Message.deathmessage.lightning", "was killed by lightning.");
        setProperty("MyWolf.Message.deathmessage.fire", "was killed by VOID.");
        setProperty("MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton.");
        setProperty("MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf.");
        setProperty("MyWolf.Message.deathmessage.explosion", "was killed by an explosion.");
        setProperty("MyWolf.Message.deathmessage.pigzombie", "was killed by a PigZombie.");
        setProperty("MyWolf.Message.deathmessage.silverfish", "was killed by a Silverfish.");

        MWC.saveConfig();
    }

    public void loadVariables()
    {
        LV.put("Msg_AddLeash", MWC.Config.getString("MyWolf.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf."));
        LV.put("Msg_HPinfo", MWC.Config.getString("MyWolf.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%"));
        LV.put("Msg_Inventory", MWC.Config.getString("MyWolf.Message.inventory", "%aqua%%wolfname%%white% has now an inventory with %size%slots."));
        LV.put("Msg_AddDemage", MWC.Config.getString("MyWolf.Message.adddemage", "%green%+1 attackdemage for %aqua%%wolfname%"));
        LV.put("Msg_MaxLives", MWC.Config.getString("MyWolf.Message.maxlives", "%aqua%%wolfname%%red% has reached the maximum of %maxlives% lives."));
        LV.put("Msg_AddPickup", MWC.Config.getString("MyWolf.Message.addpickup", "%aqua%%wolfname%%white% now pickup items in a range of %range%."));
        LV.put("Msg_AddHP", MWC.Config.getString("MyWolf.Message.addhp", "%aqua%%wolfname%%white% +1 maxHP for %aqua%%wolfname%"));
        LV.put("Msg_WolfIsGone", MWC.Config.getString("MyWolf.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . ."));
        LV.put("Msg_DeathMessage", MWC.Config.getString("MyWolf.Message.deathmessage.text", "%aqua%%wolfname%%white% "));
        LV.put("Msg_RespawnIn", MWC.Config.getString("MyWolf.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec"));
        LV.put("Msg_OnRespawn", MWC.Config.getString("MyWolf.Message.onrespawn", "%aqua%%wolfname%%white% respawned"));
        LV.put("Msg_CallDead", MWC.Config.getString("MyWolf.Message.callwhendead", "%aqua%%wolfname%%white% is dead! and respawns in %gold%%time%%white% sec"));
        LV.put("Msg_Call", MWC.Config.getString("MyWolf.Message.call", "%aqua%%wolfname%%white% comes to you."));
        LV.put("Msg_Home", MWC.Config.getString("MyWolf.Message.home", "%aqua%%wolfname%%white% go to home."));
        LV.put("Msg_CallFirst", MWC.Config.getString("MyWolf.Message.callfirst", "You must call your wolf first."));
        LV.put("Msg_DontHaveWolf", MWC.Config.getString("MyWolf.Message.donthavewolf", "You don't have a MyWolf!"));
        LV.put("Msg_OtherDontHaveWolf", MWC.Config.getString("MyWolf.Message.otherdonthavewolf", "%aqua%%playername%%white% don't have a MyWolf!"));
        LV.put("Msg_UserDontHaveWolf", MWC.Config.getString("MyWolf.Message.userdonthavewolf", "This user doesn't have a MyWolf!"));
        LV.put("Msg_NewName", MWC.Config.getString("MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%"));
        LV.put("Msg_Name", MWC.Config.getString("MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%"));
        LV.put("Msg_Release", MWC.Config.getString("MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green$free%white% . . ."));
        LV.put("Msg_StopAttack", MWC.Config.getString("MyWolf.Message.stopattack", "%wolfname% should now %green%stop%white% attacking!"));
        LV.put("Msg_InventorySwimming", MWC.Config.getString("MyWolf.Message.inventorywhileswimming", "You can't open the inventory while the wolf is swimming!"));
        LV.put("Msg_NoInventory", MWC.Config.getString("MyWolf.Message.noinventory", "%wolfname% doesn't have an inventory."));
        LV.put("Creeper", MWC.Config.getString("MyWolf.Message.deathmessage.creeper", "was killed by a Creeper."));
        LV.put("Zombie", MWC.Config.getString("MyWolf.Message.deathmessage.zombie", "was killed by a Zombie."));
        LV.put("PigZombie", MWC.Config.getString("MyWolf.Message.deathmessage.pigzombie", "was killed by a Pig Zombie."));
        LV.put("Unknow", MWC.Config.getString("MyWolf.Message.deathmessage.unknow", "was killed by an unknown source."));
        LV.put("You", MWC.Config.getString("MyWolf.Message.deathmessage.you", "was killed by %red%YOU."));
        LV.put("Spider", MWC.Config.getString("MyWolf.Message.deathmessage.spider", "was killed by a Spider."));
        LV.put("CaveSpider", MWC.Config.getString("MyWolf.Message.deathmessage.cavespider", "was killed by a Cave Spider."));
        LV.put("Enderman", MWC.Config.getString("MyWolf.Message.deathmessage.enderman", "was killed by a Enderman."));
        LV.put("Skeleton", MWC.Config.getString("MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton."));
        LV.put("Silverfish", MWC.Config.getString("MyWolf.Message.deathmessage.silverfish", "was killed by a Silverfish."));
        LV.put("Giant", MWC.Config.getString("MyWolf.Message.deathmessage.giant", "was killed by a Giant."));
        LV.put("Slime", MWC.Config.getString("MyWolf.Message.deathmessage.slime", "was killed by a Slime."));
        LV.put("Ghast", MWC.Config.getString("MyWolf.Message.deathmessage.ghast", "was killed by a Ghast."));
        LV.put("Wolf", MWC.Config.getString("MyWolf.Message.deathmessage.wolf", "was killed by a Wolf."));
        LV.put("PlayerWolf", MWC.Config.getString("MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf ."));
        LV.put("Player", MWC.Config.getString("MyWolf.Message.deathmessage.player", "was killed by %player%."));
        LV.put("Drowning", MWC.Config.getString("MyWolf.Message.deathmessage.drowning", "drowned."));
        LV.put("Explosion", MWC.Config.getString("MyWolf.Message.deathmessage.explosion", "was killed by an explosion."));
        LV.put("Fall", MWC.Config.getString("MyWolf.Message.deathmessage.fall", " died by falling down."));
        LV.put("Lightning", MWC.Config.getString("MyWolf.Message.deathmessage.lightning", "was killed by lightning."));
        LV.put("kvoid", MWC.Config.getString("MyWolf.Message.deathmessage.fire", "was killed by the VOID."));
    }
}
