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

package de.Keyle.MyWolf.util;

import org.bukkit.util.config.Configuration;

import java.util.HashMap;
import java.util.Map;

public class MyWolfLanguage
{
    private final Configuration Config;

    public MyWolfLanguage(Configuration cfg)
    {
        Config = cfg;
        Config.load();
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
        if (Config.getProperty(key) == null)
        {
            Config.setProperty(key, value);
        }
    }

    public void setStandart()
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
        setProperty("MyWolf.Message.callfirst", "You must call your wolf first.");
        setProperty("MyWolf.Message.donthavewolf", "You don't have a wolf!");
        setProperty("MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%");
        setProperty("MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%");
        setProperty("MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green$free%white% . . .");
        setProperty("MyWolf.Message.stopattack", "Your wolf should now %green%stop%white% attacking!");
        setProperty("MyWolf.Message.inventorywhileswimming", "You can't open the inventory while the wolf is swimming!");
        setProperty("MyWolf.Message.deathmessage.creeper", "was killed by a Creeper.");
        setProperty("MyWolf.Message.deathmessage.zombie", "was killed by a Zombie.");
        setProperty("MyWolf.Message.deathmessage.unknow", "was killed by an unknown source.");
        setProperty("MyWolf.Message.deathmessage.you", "was killed by %red%YOU.");
        setProperty("MyWolf.Message.deathmessage.spider", "was killed by a Spider.");
        setProperty("MyWolf.Message.deathmessage.giant", "was killed by a Giant.");
        setProperty("MyWolf.Message.deathmessage.slime", "was killed by a Slime.");
        setProperty("MyWolf.Message.deathmessage.ghast", "was killed by a Ghast.");
        setProperty("MyWolf.Message.deathmessage.wolf", "was killed by a Wolf.");
        setProperty("MyWolf.Message.deathmessage.player", "was killed by %player%.");
        setProperty("MyWolf.Message.deathmessage.drowning", "drowned.");
        setProperty("MyWolf.Message.deathmessage.fall", " died by falling down.");
        setProperty("MyWolf.Message.deathmessage.lightning", "was killed by lightning.");
        setProperty("MyWolf.Message.deathmessage.fire", "was killed by VOID.");
        setProperty("MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton.");
        setProperty("MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf.");
        setProperty("MyWolf.Message.deathmessage.explosion", "was killed by an explosion.");

        Config.save();
    }

    public void loadVariables()
    {
        LV.put("Msg_AddLeash", Config.getString("MyWolf.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf."));
        LV.put("Msg_HPinfo", Config.getString("MyWolf.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%"));
        LV.put("Msg_Inventory", Config.getString("MyWolf.Message.inventory", "%aqua%%wolfname%%white% has now an inventory with %size%slots."));
        LV.put("Msg_AddDemage", Config.getString("MyWolf.Message.adddemage", "%green%+1 attackdemage for %aqua%%wolfname%"));
        LV.put("Msg_MaxLives", Config.getString("MyWolf.Message.maxlives", "%aqua%%wolfname%%red% has reached the maximum of %maxlives% lives."));
        LV.put("Msg_AddPickup", Config.getString("MyWolf.Message.addpickup", "%aqua%%wolfname%%white% now pickup items in a range of %range%."));
        LV.put("Msg_AddHP", Config.getString("MyWolf.Message.addhp", "%aqua%%wolfname%%white% +1 maxHP for %aqua%%wolfname%"));
        LV.put("Msg_WolfIsGone", Config.getString("MyWolf.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . ."));
        LV.put("Msg_DeathMessage", Config.getString("MyWolf.Message.deathmessage.text", "%aqua%%wolfname%%white% "));
        LV.put("Msg_RespawnIn", Config.getString("MyWolf.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec"));
        LV.put("Msg_OnRespawn", Config.getString("MyWolf.Message.onrespawn", "%aqua%%wolfname%%white% respawned"));
        LV.put("Msg_CallDead", Config.getString("MyWolf.Message.callwhendead", "%aqua%%wolfname%%white% is dead! and respawns in %gold%%time%%white% sec"));
        LV.put("Msg_Call", Config.getString("MyWolf.Message.call", "%aqua%%wolfname%%white% comes to you."));
        LV.put("Msg_CallFirst", Config.getString("MyWolf.Message.callfirst", "You must call your wolf first."));
        LV.put("Msg_DontHaveWolf", Config.getString("MyWolf.Message.donthavewolf", "You don't have a wolf!"));
        LV.put("Msg_NewName", Config.getString("MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%"));
        LV.put("Msg_Name", Config.getString("MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%"));
        LV.put("Msg_Release", Config.getString("MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green$free%white% . . ."));
        LV.put("Msg_StopAttack", Config.getString("MyWolf.Message.stopattack", "%wolfname% should now %green%stop%white% attacking!"));
        LV.put("Msg_InventorySwimming", Config.getString("MyWolf.Message.inventorywhileswimming", "You can't open the inventory while the wolf is swimming!"));
        LV.put("Creeper", Config.getString("MyWolf.Message.deathmessage.creeper", "was killed by a Creeper."));
        LV.put("Zombie", Config.getString("MyWolf.Message.deathmessage.zombie", "was killed by a Zombie."));
        LV.put("Unknow", Config.getString("MyWolf.Message.deathmessage.unknow", "was killed by an unknown source."));
        LV.put("You", Config.getString("MyWolf.Message.deathmessage.you", "was killed by %red%YOU."));
        LV.put("Spider", Config.getString("MyWolf.Message.deathmessage.spider", "was killed by a Spider."));
        LV.put("Skeleton", Config.getString("MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton."));
        LV.put("Giant", Config.getString("MyWolf.Message.deathmessage.giant", "was killed by a Giant."));
        LV.put("Slime", Config.getString("MyWolf.Message.deathmessage.slime", "was killed by a Slime."));
        LV.put("Ghast", Config.getString("MyWolf.Message.deathmessage.ghast", "was killed by a Ghast."));
        LV.put("Wolf", Config.getString("MyWolf.Message.deathmessage.wolf", "was killed by a Wolf."));
        LV.put("PlayerWolf", Config.getString("MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf ."));
        LV.put("Player", Config.getString("MyWolf.Message.deathmessage.player", "was killed by %player%."));
        LV.put("Drowning", Config.getString("MyWolf.Message.deathmessage.drowning", "drowned."));
        LV.put("Explosion", Config.getString("MyWolf.Message.deathmessage.explosion", "was killed by an explosion."));
        LV.put("Fall", Config.getString("MyWolf.Message.deathmessage.fall", " died by falling down."));
        LV.put("Lightning", Config.getString("MyWolf.Message.deathmessage.lightning", "was killed by lightning."));
        LV.put("kvoid", Config.getString("MyWolf.Message.deathmessage.fire", "was killed by VOID."));
    }
}
