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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.util.configuration.YAML_Configuration;

import java.util.HashMap;
import java.util.Map;

public class MyPetLanguage
{
    private final YAML_Configuration yamlConfiguration;
    private boolean save = false;

    public MyPetLanguage(YAML_Configuration yamlConfiguration)
    {
        this.yamlConfiguration = yamlConfiguration;
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
        if (yamlConfiguration.getConfig().contains(node) && !yamlConfiguration.getConfig().isConfigurationSection(node))
        {
            LV.put(name, yamlConfiguration.getConfig().getString(node, def));
        }
        else
        {
            yamlConfiguration.getConfig().set(node, def);
            LV.put(name, def);
            save = true;
        }
    }

    public void load()
    {
        // -- Messages -- -----------------------------------------------------------------------------------------------------------------------------------
        // --  A  --
        addString("Msg_AddBeacon", "MyPet.Message.addbeacon", "%aqua%%petname%%reset% can now now use these buffs (range: %range% - duration: %duration%sec):");
        addString("Msg_AddBehavior", "MyPet.Message.addbehavior", "%aqua%%petname%%reset% can now now use these behavior modes:");
        addString("Msg_AddControl", "MyPet.Message.addcontrol", "%aqua%%petname%%reset% can now be controlled with a %item%.");
        addString("Msg_AddDamage", "MyPet.Message.adddamage", "%aqua%%petname%%reset% has now %dmg% bonusdamage.");
        addString("Msg_AddHP", "MyPet.Message.addhp", "%aqua%%petname%%reset% has now a max health of %maxhealth%HP.");
        addString("Msg_AddHPregeneration", "MyPet.Message.addhpreg", "%aqua%%petname%%reset% regenerates now %hp%HP every %sec%sec.");
        addString("Msg_AddLeash", "MyPet.Message.addleash", "%green%You take your pet on the leash, he'll be a good pet.");
        addString("Msg_AddPickup", "MyPet.Message.addpickup", "%aqua%%petname%%reset% now can pickup items in a range of %range%.");
        addString("Msg_AddRide", "MyPet.Message.addride", "%aqua%%petname%%reset% can now be ridden.");
        addString("Msg_AlreadyAway", "MyPet.Message.alreadyaway", "%aqua%%petname%%reset% is not here.");
        addString("Msg_AutomaticSkilltreeAssignment", "MyPet.Message.automaticskilltreeassignment", "Skilltrees will be assigned automatically!");
        addString("Msg_AvailableSkilltrees", "MyPet.Message.availableskilltrees", "Available skilltrees for %aqua%%petname%%reset%:");
        // --  B  --
        addString("Msg_BeaconCreative", "MyPet.Message.beaconwhileincreative", "You can't open the beacon window while you are in creative mode.");
        addString("Msg_BeaconBuffNotActive", "MyPet.Message.beaconbuffnotactive", "You %red%can't%reset% use the %gold%%buff%%reset% buff.");
        addString("Msg_BeaconImprovedBuffNotActive", "MyPet.Message.beaconimprovedbuffnotactive", "You %red%can't%reset% use the improved %gold%%buff%%reset% buff.");
        addString("Msg_BehaviorState", "MyPet.Message.behaviorstate", "%aqua%%petname%%reset% is now in %mode% mode.");
        // --  C  --
        addString("Msg_Call", "MyPet.Message.call", "%aqua%%petname%%reset% comes to you.");
        addString("Msg_CallDead", "MyPet.Message.callwhendead", "%aqua%%petname%%reset% is %red%dead%reset% and will respawn in %gold%%time%%reset% sec.");
        addString("Msg_CallFirst", "MyPet.Message.callfirst", "You must call %aqua%%petname%%reset% first.");
        addString("Msg_CantFindSkilltree", "MyPet.Message.cantfindskilltree", "There is not skilltree available that is labeled %name%.");
        addString("Msg_CantUse", "MyPet.Message.cantuse", "%red%You can not use this now.");
        addString("Msg_Cmd_petinfo", "MyPet.Message.petinfo", " [player] | Display info about a MyPet  (alias: /pinfo)");
        addString("Msg_Cmd_petname", "MyPet.Message.petname", " <new pet name> | Set the name of your pet");
        addString("Msg_Cmd_petrelease", "MyPet.Message.petrelease", " <petname> | Release your pet");
        addString("Msg_Cmd_petstop", "MyPet.Message.petstop", " | MyPet stopps attacking  (alias: /ps or /pets)");
        addString("Msg_Cmd_petcall", "MyPet.Message.petcall", " | Call your pet  (alias: /pc or /petc)");
        addString("Msg_Cmd_petsendaway", "MyPet.Message.petsendaway", " | Sends your pet away  (alias: /psa or /petsa)");
        addString("Msg_Cmd_petskill", "MyPet.Message.petskill", " | Shows the skill-levels");
        addString("Msg_Cmd_petchooseskilltree", "MyPet.Message.petchooseskilltree", " | Shows and chooses skilltrees  (alias: /pcst or /petcst)");
        addString("Msg_Cmd_petinventory", "MyPet.Message.petinventory", " | Opens the inventory of the pet  (alias: /pi or /peti)");
        addString("Msg_Cmd_petpickup", "MyPet.Message.petpickup", " | Toggle pickup on/off  (alias: /pp or /petp)");
        addString("Msg_Cmd_petbehavior", "MyPet.Message.petbehavior", " | Toggles the behaivior  (alias: /pb or /petb)");
        addString("Msg_Cmd_moreinfo", "MyPet.Message.moreinfo", "For more info read the command page on: ");
        addString("Msg_Cmd_petadmin", "MyPet.Message.petadmin", " [PlayerName] name/exp/respawn [Value]");
        addString("Msg_ControlAggroFarm", "MyPet.Message.controlaggrofarm", "You can't control %aqua%%petname%%reset% when in %mode% mode!");
        // --  D  --
        addString("Msg_DeathMessage", "MyPet.Message.deathmessage", "%aqua%%petname%%reset% was killed by: ");
        addString("Msg_Despawn", "MyPet.Message.despawn", "%aqua%%petname%%reset% despawned.");
        addString("Msg_DontHavePet", "MyPet.Message.donthavepet", "You don't have a MyPet!");
        // --  F  --
        addString("Msg_FireChance", "MyPet.Message.firechance", "%aqua%%petname%%reset% has now a chance of %chance%% to set enemies on fire for %duration%sec.");
        // --  H  --
        addString("Msg_Home", "MyPet.Message.call", "%aqua%%petname%%reset% go to home.");
        addString("Msg_HPinfo", "MyPet.Message.hpinfo", "%aqua%%petname%%reset% HP:%hp%");
        // --  I  --
        addString("Msg_Inventory", "MyPet.Message.inventory", "%aqua%%petname%%reset% has now an inventory with %size%slots.");
        addString("Msg_InventoryCreative", "MyPet.Message.inventorywhileincreative", "You can't open the inventory while you are in creative mode.");
        addString("Msg_InventorySwimming", "MyPet.Message.inventorywhileswimming", "You can't open the inventory while %aqua%%petname%%reset% is swimming!");
        // --  K  --
        addString("Msg_KnockbackChance", "MyPet.Message.knockbackchance", "%aqua%%petname%%reset% has now a chance of %chance%% to knock his target back.");
        // --  L  --
        addString("Msg_LearnedSkill", "MyPet.Message.learnskill", "%aqua%%petname%%reset% learned the skill %skill%.");
        addString("Msg_LightningChance", "MyPet.Message.lightningchance", "%aqua%%petname%%reset% has now a chance of %chance%% to shoot a lightning at his enemies.");
        addString("Msg_LvlUp", "MyPet.Message.lvlup", "%aqua%%petname%%reset% is now Lv%lvl%");
        // --  N  --
        addString("Msg_Name", "MyPet.Message.name", "The name of your pet is: %aqua%%petname%");
        addString("Msg_NewName", "MyPet.Message.newname", "The name of your pet is now: %aqua%%petname%");
        addString("Msg_NoInventory", "MyPet.Message.noinventory", "%aqua%%petname%%reset% doesn't have an inventory.");
        addString("Msg_NoSkill", "MyPet.Message.noskill", "%aqua%%petname%%reset% doesn't know the skill %skill%.");
        // --  O  --
        addString("Msg_OnlyChooseSkilltreeOnce", "MyPet.Message.onlychooseskilltreeonce", "You can change the skilltree of %aqua%%petname%%reset% only once!");
        addString("Msg_OnRespawn", "MyPet.Message.onrespawn", "%aqua%%petname%%reset% respawned!");
        // --  P  --
        addString("Msg_PetIsGone", "MyPet.Message.petisgone", "%aqua%%petname%%reset% is %red%gone%reset% and will never come back . . .");
        addString("Msg_PickButNoInventory", "MyPet.Message.pickupbutnoinventory", "%aqua%%petname%%reset% could pickup items but has no inventoy.");
        addString("Msg_PickUpStart", "MyPet.Message.pickupstart", "%aqua%%petname%%reset% pickup: activated");
        addString("Msg_PickUpStop", "MyPet.Message.pickupstop", "%aqua%%petname%%reset% pickup: disabled");
        addString("Msg_PoisonChance", "MyPet.Message.poisonchance", "%aqua%%petname%%reset% has now a chance of %chance%% to poison his target for %duration%sec.");
        // --  R  --
        addString("Msg_Release", "MyPet.Message.release", "%aqua%%petname%%reset% is now %green%free%reset% . . .");
        addString("Msg_RespawnAuto", "MyPet.Message.respawnauto", "Auto respawn: %gold%%status%%reset%.");
        addString("Msg_RespawnAutoMin", "MyPet.Message.respawnautomin", "Set minimal auto respawn time to %gold%%time%%reset%.");
        addString("Msg_RespawnIn", "MyPet.Message.respawnin", "%aqua%%petname%%reset% will respawn in %gold%%time%%reset% sec.");
        addString("Msg_RespawnNoMoney", "MyPet.Message.respawnnomoney", "You need %red%%costs%%reset% to let respawn %aqua%%petname%%reset%!");
        addString("Msg_RespawnPaid", "MyPet.Message.respawnpaid", "Respawn fee (%gold%%costs%%reset%) for %aqua%%petname%%reset% paid.");
        addString("Msg_RespawnShow", "MyPet.Message.respawnshow", "Respawn fee for %aqua%%petname%%reset%: %gold%%costs%%reset% (%color%auto%reset%)");
        // --  S  --
        addString("Msg_SendAway", "MyPet.Message.sendaway", "You sent %aqua%%petname%%reset% away.");
        addString("Msg_Skills", "MyPet.Message.skills", "%aqua%%petname%%reset%'s skills: %skilltree%");
        addString("Msg_SkilltreeNotSwitched", "MyPet.Message.skilltreesnotwitched", "Skilltree hasn't changed!");
        addString("Msg_SkilltreeSwitchedTo", "MyPet.Message.skilltreeswitchedto", "You have selected the %aqua%%name%%reset% skilltree.");
        addString("Msg_SlowChance", "MyPet.Message.slowchance", "%aqua%%petname%%reset% has now a chance of %chance%% to slow target down for %duration%sec.");
        addString("Msg_SpawnNoSpace", "MyPet.Message.spawnnospace", "%aqua%%petname%%reset% can not come to you because there is not enough space.");
        addString("Msg_SpawnPrevent", "MyPet.Message.spawnprevent", "Something prevents %aqua%%petname%%reset% from coming to you.");
        addString("Msg_StopAttack", "MyPet.Message.stopattack", "Your pet should now %green%stop%reset% attacking!");
        // --  T  --
        addString("Msg_ThornsChance", "MyPet.Message.thornschance", "%aqua%%petname%%reset% has now a chance of %chance%% to reflect damage.");
        // --  U  --
        addString("Msg_UserDontHavePet", "MyPet.Message.userdonthavepet", "%gold%%playername%%reset% doesn't have a MyPet!");
        // --  W  --
        addString("Msg_WitherChance", "MyPet.Message.witherchance", "%aqua%%petname%%reset% has now a chance of %chance%% to wither his target for %duration%sec.");

        // -- Names -- --------------------------------------------------------------------------------------------------------------------------------------
        // --  A  --
        addString("Name_Aggressive", "MyPet.Name.Aggressive", "Aggressive");
        addString("Name_ARROW", "MyPet.Name.Arrow", "Arrow");
        // --  B  --
        addString("Name_Bat", "MyPet.Name.Bat", "Bat");
        addString("Name_Blaze", "MyPet.Name.Blaze", "Blaze");
        addString("Name_Blocks", "MyPet.Name.Blocks", "Block(s)");
        // --  C  --
        addString("Name_CaveSpider", "MyPet.Name.CaveSpider", "Cave Spider");
        addString("Name_Chicken", "MyPet.Name.Chicken", "Chicken");
        addString("Name_CONTACT", "MyPet.Name.Contact", "Contact");
        addString("Name_Cow", "MyPet.Name.Cow", "Cow");
        addString("Name_Creeper", "MyPet.Name.Creeper", "Creeper");
        // --  D  --
        addString("Name_Damage", "MyPet.Name.Damage", "Damage");
        addString("Name_Dead", "MyPet.Name.Dead", "Dead");
        addString("Name_Disabled", "MyPet.Name.Disabled", "Disabled");
        addString("Name_DROWNING", "MyPet.Name.Drowning", "Drowning");
        // --  E  --
        addString("Name_EGG", "MyPet.Name.Egg", "Egg");
        addString("Name_Enabled", "MyPet.Name.Enabled", "Enabled");
        addString("Name_EnderDragon", "MyPet.Name.EnderDragon", "Ender Dragon");
        addString("Name_Enderman", "MyPet.Name.Enderman", "Enderman");
        addString("Name_Exp", "MyPet.Name.Exp", "Exp");
        // --  F  --
        addString("Name_FALL", "MyPet.Name.Fall", "Fall");
        addString("Name_FALLING_BLOCK", "MyPet.Name.FallingBlock", "Falling Block");
        addString("Name_Farm", "MyPet.Name.Farm", "Farm");
        addString("Name_FIRE", "MyPet.Name.Fire", "Fire");
        addString("Name_FIREBALL", "MyPet.Name.Fireball", "Fireball");
        addString("Name_Fireworks", "MyPet.Name.Fireworks", "Fireworks");
        addString("Name_FISHING_HOOK", "MyPet.Name.FishingHook", "Fishing Hook");
        addString("Name_FIRE_TICK", "MyPet.Name.Fire", "Fire");
        addString("Name_Friendly", "MyPet.Name.Friendly", "Friendly");
        // --  G  --
        addString("Name_Ghast", "MyPet.Name.Ghast", "Ghast");
        // --  H  --
        addString("Name_Haste", "MyPet.Name.Haste", "Haste");
        addString("Name_Help", "MyPet.Name.Help", "Help");
        addString("Name_HP", "MyPet.Name.HP", "HP");
        addString("Name_Hunger", "MyPet.Name.Hunger", "Hunger");
        // --  J  --
        addString("Name_JumpBoost", "MyPet.Name.JumpBoost", "Jump Boost");
        // --  L  --
        addString("Name_LAVA", "MyPet.Name.Lava", "Lava");
        addString("Name_LavaSlime", "MyPet.Name.MagmaCube", "Magma Cube");
        addString("Name_Level", "MyPet.Name.Level", "Level");
        addString("Name_LIGHTNING", "MyPet.Name.Lightning", "Lightning");
        // --  M  --
        addString("Name_MAGIC", "MyPet.Name.Magic", "Magic");
        addString("Name_Modes", "MyPet.Name.Modes", "Modes");
        addString("Name_Mooshroom", "MyPet.Name.Mooshroom", "Mooshroom");
        // --  N  --
        addString("Name_Normal", "MyPet.Name.Normal", "Normal");
        // --  O  --
        addString("Name_Owner", "MyPet.Name.Owner", "Owner");
        addString("Name_Ozelot", "MyPet.Name.Ocelot", "Ocelot");
        // --  P  --
        addString("Name_Pig", "MyPet.Name.Pig", "Pig");
        addString("Name_PigZombie", "MyPet.Name.ZombiePigman", "Zombie Pigman");
        addString("Name_POISON", "MyPet.Name.Poison", "Poison");
        addString("Name_PrimedTnT", "MyPet.Name.TnT", "TnT");
        // --  R  --
        addString("Name_Raid", "MyPet.Name.Raid", "Raid");
        addString("Name_Range", "MyPet.Name.Range", "Range");
        addString("Name_Regeneration", "MyPet.Name.Regeneration", "Regeneration");
        addString("Name_Resistance", "MyPet.Name.Resistance", "Resistance");
        addString("Name_Rows", "MyPet.Name.Rows", "Row(s)");
        // --  S  --
        addString("Name_Sheep", "MyPet.Name.Sheep", "Sheep");
        addString("Name_Silverfish", "MyPet.Name.Silverfish", "Silverfish");
        addString("Name_Skeleton", "MyPet.Name.Skeleton", "Skeleton");
        addString("Name_Slime", "MyPet.Name.Slime", "Slime");
        addString("Name_SMALL_FIREBALL", "MyPet.Name.Fireball", "Fireball");
        addString("Name_SNOWBALL", "MyPet.Name.Snowball", "Snowball");
        addString("Name_SnowMan", "MyPet.Name.SnowMan", "Snowman");
        addString("Name_Speed", "MyPet.Name.Speed", "Speed");
        addString("Name_Spider", "MyPet.Name.Spider", "Spider");
        addString("Name_SPLASH_POTION", "MyPet.Name.SplashPotion", "Splash Potion");
        addString("Name_Squid", "MyPet.Name.Squid", "Squid");
        addString("Name_Strength", "MyPet.Name.Strength", "Strength");
        // --  T  --
        addString("Name_Tier", "MyPet.Name.Tier", "Tier");
        // --  U  --
        addString("Name_Unknow", "MyPet.Name.Unknow", "Unknow");
        // --  V  --
        addString("Name_Villager", "MyPet.Name.Villager", "Villager");
        addString("Name_VillagerGolem", "MyPet.Name.IronGolem", "Iron Golem");
        addString("Name_VOID", "MyPet.Name.Void", "The Void");
        // --  W  --
        addString("Name_Witch", "MyPet.Name.Witch", "Witch");
        addString("Name_WITHER", "MyPet.Name.Wither", "Wither");
        addString("Name_WitherBoss", "MyPet.Name.Wither", "Wither");
        addString("Name_WITHER_SKULL", "MyPet.Name.WitherSkull", "Witherskull");
        addString("Name_Wolf", "MyPet.Name.Wolf", "Wolf");
        // --  Y  --
        addString("Name_You", "MyPet.Name.You", "You");
        // --  Z  --
        addString("Name_Zombie", "MyPet.Name.Zombie", "Zombie");

        if (save)
        {
            yamlConfiguration.saveConfig();
            save = false;
            MyPetUtil.getDebugLogger().info("Added new values to language config");
        }
    }
}