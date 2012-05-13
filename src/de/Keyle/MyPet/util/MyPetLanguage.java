/*
 * Copyright (C) 2011-2012 Keyle
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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.util.configuration.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class MyPetLanguage
{
    private final YamlConfiguration MWC;

    public MyPetLanguage(YamlConfiguration MWC)
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
        addString("Msg_AddLeash", "MyPet.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf.");
        addString("Msg_LvlUp", "MyPet.Message.lvlup", "%aqua%%wolfname%%white% is now Lv%lvl%");
        addString("Msg_HPinfo", "MyPet.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%");
        addString("Msg_Inventory", "MyPet.Message.inventory", "%aqua%%wolfname%%white% has now an inventory with %size%slots.");
        addString("Msg_AddControl", "MyPet.Message.addcontrol", "%aqua%%wolfname%%white% can now be controlled with a %item%");
        addString("Msg_AddPickup", "MyPet.Message.addpickup", "%aqua%%wolfname%%white% now can pickup items in a range of %range%.");
        addString("Msg_AddHPregeneration", "MyPet.Message.addhpreg", "%aqua%%wolfname%%white% regenerates now one HP every %sec%sec");
        addString("Msg_AddHP", "MyPet.Message.addhp", "%aqua%%wolfname%%white% has now a max health of %maxhealth%HP");
        addString("Msg_AddDemage", "MyPet.Message.adddemage", "%aqua%%wolfname%%white% has now %dmg% bonusdemage");
        addString("Msg_WolfIsGone", "MyPet.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . .");
        addString("Msg_DeathMessage", "MyPet.Message.deathmessage.text", "%aqua%%wolfname%%white% ");
        addString("Msg_RespawnIn", "MyPet.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec");
        addString("Msg_OnRespawn", "MyPet.Message.onrespawn", "%aqua%%wolfname%%white% respawned");
        addString("Msg_CallDead", "MyPet.Message.callwhendead", "%aqua%%wolfname%%white% is %red%dead%white% and will respawn in %gold%%time%%white% sec");
        addString("Msg_Call", "MyPet.Message.call", "%aqua%%wolfname%%white% comes to you.");
        addString("Msg_Home", "MyPet.Message.call", "%aqua%%wolfname%%white% go to home.");
        addString("Msg_CallFirst", "MyPet.Message.callfirst", "You must call %aqua%%wolfname%%white% first.");
        addString("Msg_UserDontHaveWolf", "MyPet.Message.userdonthavewolf", "%gold%%playername%%white% doesn't have a MyPet!");
        addString("Msg_DontHaveWolf", "MyPet.Message.donthavewolf", "You don't have a MyPet!");
        addString("Msg_NewName", "MyPet.Message.newname", "The name of your wolf is now: %aqua%%wolfname%");
        addString("Msg_Name", "MyPet.Message.name", "The name of your wolf is: %aqua%%wolfname%");
        addString("Msg_Release", "MyPet.Message.release", "%aqua%%wolfname%%white% is now %green%free%white% . . .");
        addString("Msg_StopAttack", "MyPet.Message.stopattack", "Your wolf should now %green%stop%white% attacking!");
        addString("Msg_InventorySwimming", "MyPet.Message.inventorywhileswimming", "You can't open the inventory while %aqua%%wolfname%%white% is swimming!");
        addString("Msg_NoInventory", "MyPet.Message.noinventory", "%aqua%%wolfname%%white% doesn't have an inventory.");
        addString("Msg_PickButNoInventory", "MyPet.Message.pickupbutnoinventory", "%aqua%%wolfname%%white% could pickup items but has no inventoy.");
        addString("Msg_NoSkill", "MyPet.Message.noskill", "%aqua%%wolfname%%white% doesn't know the skill %skill%.");
        addString("Msg_Skills", "MyPet.Message.skills", "%aqua%%wolfname%%white%'s skills: %skilltree%");
        addString("Msg_LearnedSkill", "MyPet.Message.noskill", "%aqua%%wolfname%%white% learned the skill %skill%.");
        addString("Msg_PickUpStop", "MyPet.Message.pickupstop", "%aqua%%wolfname%%white% pickup: disabled");
        addString("Msg_PickUpStart", "MyPet.Message.pickupstart", "%aqua%%wolfname%%white% pickup: activated");
        addString("Msg_BehaviorState", "MyPet.Message.behaviorstate", "%aqua%%wolfname%%white% is now in %mode% mode.");
        addString("Creeper", "MyPet.Message.deathmessage.creeper", "was killed by a Creeper.");
        addString("Zombie", "MyPet.Message.deathmessage.zombie", "was killed by a Zombie.");
        addString("Unknow", "MyPet.Message.deathmessage.unknow", "was killed by an unknown source.");
        addString("You", "MyPet.Message.deathmessage.you", "was killed by %red%YOU%white%.");
        addString("Spider", "MyPet.Message.deathmessage.spider", "was killed by a Spider.");
        addString("CaveSpider", "MyPet.Message.deathmessage.cavespider", "was killed by a Cave Spider.");
        addString("Giant", "MyPet.Message.deathmessage.giant", "was killed by a Giant.");
        addString("Slime", "MyPet.Message.deathmessage.slime", "was killed by a Slime.");
        addString("MagmaCube", "MyPet.Message.deathmessage.magmacube", "was killed by a Magma Cube.");
        addString("Ghast", "MyPet.Message.deathmessage.ghast", "was killed by a Ghast.");
        addString("Blaze", "MyPet.Message.deathmessage.blace", "was killed by a Blaze.");
        addString("EnderDragon", "MyPet.Message.deathmessage.enderdragon", "was killed by the Ender Dragon.");
        addString("Wolf", "MyPet.Message.deathmessage.wolf", "was killed by a Wolf.");
        addString("MyPet", "MyPet.Message.deathmessage.mywolf", "was killed by %wolfname% of %player%.");
        addString("OwnedWolf", "MyPet.Message.deathmessage.mywolf", "was killed by a Wolf of %player%.");
        addString("Enderman", "MyPet.Message.deathmessage.enderman", "was killed by a Enderman.");
        addString("Snowman", "MyPet.Message.deathmessage.snowman", "was killed by a Snowman.");
        addString("Player", "MyPet.Message.deathmessage.player", "was killed by %player%.");
        addString("Drowning", "MyPet.Message.deathmessage.drowning", "drowned.");
        addString("Fall", "MyPet.Message.deathmessage.fall", "died by falling down.");
        addString("Lightning", "MyPet.Message.deathmessage.lightning", "was killed by lightning.");
        addString("kvoid", "MyPet.Message.deathmessage.fire", "was killed by the VOID.");
        addString("Skeleton", "MyPet.Message.deathmessage.skeleton", "was killed by a Skeleton.");
        addString("PlayerWolf", "MyPet.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf.");
        addString("Explosion", "MyPet.Message.deathmessage.explosion", "was killed by an explosion.");
        addString("PigZombie", "MyPet.Message.deathmessage.pigzombie", "was killed by a PigZombie.");
        addString("Silverfish", "MyPet.Message.deathmessage.silverfish", "was killed by a Silverfish.");

        MWC.saveConfig();
    }
}