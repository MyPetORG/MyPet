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

import de.Keyle.MyWolf.util.configuration.MyWolfYamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class MyWolfLanguage
{
    private final MyWolfYamlConfiguration MWC;

    public MyWolfLanguage(MyWolfYamlConfiguration MWC)
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

    public void addString(String name, String node, String def)
    {
        if (MWC.getConfig().contains(node))
        {
            LV.put(name, MWC.getConfig().getString(node, def));
        }
        else
        {
            MWC.getConfig().set(node, def);
            LV.put(name, def);
        }
    }

    public void loadVariables()
    {
        addString("Msg_AddLeash", "MyWolf.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf.");
        addString("Msg_LvlUp", "MyWolf.Message.lvlup", "%aqua%%wolfname%%white% is now Lv%lvl%");
        addString("Msg_HPinfo", "MyWolf.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%");
        addString("Msg_Inventory", "MyWolf.Message.inventory", "%aqua%%wolfname%%white% has now an inventory with %size%slots.");
        addString("Msg_AddControl", "MyWolf.Message.addcontrol", "%aqua%%wolfname%%white% can now be controlled with a %item%");
        addString("Msg_AddPickup", "MyWolf.Message.addpickup", "%aqua%%wolfname%%white% now can pickup items in a range of %range%.");
        addString("Msg_AddHPregeneration", "MyWolf.Message.addhpreg", "%aqua%%wolfname%%white% regenerates now one HP every %sec%sec");
        addString("Msg_AddHP", "MyWolf.Message.addhp", "%aqua%%wolfname%%white% has now a max health of %maxhealth%HP");
        addString("Msg_AddDemage", "MyWolf.Message.adddemage", "%aqua%%wolfname%%white% has now %dmg% bonusdemage");
        addString("Msg_WolfIsGone", "MyWolf.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . .");
        addString("Msg_DeathMessage", "MyWolf.Message.deathmessage.text", "%aqua%%wolfname%%white% ");
        addString("Msg_RespawnIn", "MyWolf.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec");
        addString("Msg_OnRespawn", "MyWolf.Message.onrespawn", "%aqua%%wolfname%%white% respawned");
        addString("Msg_CallDead", "MyWolf.Message.callwhendead", "%aqua%%wolfname%%white% is %red%dead%white% and will respawn in %gold%%time%%white% sec");
        addString("Msg_Call", "MyWolf.Message.call", "%aqua%%wolfname%%white% comes to you.");
        addString("Msg_Home", "MyWolf.Message.call", "%aqua%%wolfname%%white% go to home.");
        addString("Msg_CallFirst", "MyWolf.Message.callfirst", "You must call %aqua%%wolfname%%white% first.");
        addString("Msg_UserDontHaveWolf", "MyWolf.Message.userdonthavewolf", "%gold%%playername%%white% doesn't have a MyWolf!");
        addString("Msg_DontHaveWolf", "MyWolf.Message.donthavewolf", "You don't have a MyWolf!");
        addString("Msg_NewName", "MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%");
        addString("Msg_Name", "MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%");
        addString("Msg_Release", "MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green%free%white% . . .");
        addString("Msg_StopAttack", "MyWolf.Message.stopattack", "Your wolf should now %green%stop%white% attacking!");
        addString("Msg_InventorySwimming", "MyWolf.Message.inventorywhileswimming", "You can't open the inventory while %aqua%%wolfname%%white% is swimming!");
        addString("Msg_NoInventory", "MyWolf.Message.noinventory", "%aqua%%wolfname%%white% doesn't have an inventory.");
        addString("Msg_PickButNoInventory", "MyWolf.Message.pickupbutnoinventory", "%aqua%%wolfname%%white% could pickup items but has no inventoy.");
        addString("Msg_NoSkill", "MyWolf.Message.noskill", "%aqua%%wolfname%%white% doesn't know the skill %skill%.");
        addString("Msg_LearnedSkill", "MyWolf.Message.noskill", "%aqua%%wolfname%%white% learned the skill %skill%.");
        addString("Msg_PickUpStop", "MyWolf.Message.pickupstop", "%aqua%%wolfname%%white% pickup: disabled");
        addString("Msg_PickUpStart", "MyWolf.Message.pickupstart", "%aqua%%wolfname%%white% pickup: activated");
        addString("Msg_BehaviorState", "MyWolf.Message.behaviorstate", "%aqua%%wolfname%%white% is now in %mode% mode.");
        addString("Creeper", "MyWolf.Message.deathmessage.creeper", "was killed by a Creeper.");
        addString("Zombie", "MyWolf.Message.deathmessage.zombie", "was killed by a Zombie.");
        addString("Unknow", "MyWolf.Message.deathmessage.unknow", "was killed by an unknown source.");
        addString("You", "MyWolf.Message.deathmessage.you", "was killed by %red%YOU%white%.");
        addString("Spider", "MyWolf.Message.deathmessage.spider", "was killed by a Spider.");
        addString("CaveSpider", "MyWolf.Message.deathmessage.cavespider", "was killed by a Cave Spider.");
        addString("Giant", "MyWolf.Message.deathmessage.giant", "was killed by a Giant.");
        addString("Slime", "MyWolf.Message.deathmessage.slime", "was killed by a Slime.");
        addString("MagmaCube", "MyWolf.Message.deathmessage.magmacube", "was killed by a Magma Cube.");
        addString("Ghast", "MyWolf.Message.deathmessage.ghast", "was killed by a Ghast.");
        addString("Blaze", "MyWolf.Message.deathmessage.blace", "was killed by a Blaze.");
        addString("EnderDragon", "MyWolf.Message.deathmessage.enderdragon", "was killed by the Ender Dragon.");
        addString("Wolf", "MyWolf.Message.deathmessage.wolf", "was killed by a Wolf.");
        addString("MyWolf", "MyWolf.Message.deathmessage.mywolf", "was killed by %wolfname% of %player%.");
        addString("OwnedWolf", "MyWolf.Message.deathmessage.mywolf", "was killed by a Wolf of %player%.");
        addString("Enderman", "MyWolf.Message.deathmessage.enderman", "was killed by a Enderman.");
        addString("Snowman", "MyWolf.Message.deathmessage.snowman", "was killed by a Snowman.");
        addString("Player", "MyWolf.Message.deathmessage.player", "was killed by %player%.");
        addString("Drowning", "MyWolf.Message.deathmessage.drowning", "drowned.");
        addString("Fall", "MyWolf.Message.deathmessage.fall", "died by falling down.");
        addString("Lightning", "MyWolf.Message.deathmessage.lightning", "was killed by lightning.");
        addString("kvoid", "MyWolf.Message.deathmessage.fire", "was killed by the VOID.");
        addString("Skeleton", "MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton.");
        addString("PlayerWolf", "MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf.");
        addString("Explosion", "MyWolf.Message.deathmessage.explosion", "was killed by an explosion.");
        addString("PigZombie", "MyWolf.Message.deathmessage.pigzombie", "was killed by a PigZombie.");
        addString("Silverfish", "MyWolf.Message.deathmessage.silverfish", "was killed by a Silverfish.");

        MWC.saveConfig();
    }
}
