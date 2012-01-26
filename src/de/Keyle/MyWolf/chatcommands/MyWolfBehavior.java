/*
* Copyright (C) 2011-2012 Keyle
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

package de.Keyle.MyWolf.chatcommands;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.BehaviorState;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfBehavior implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                MyWolf Wolf = ConfigBuffer.mWolves.get(player.getName());

                if (Wolf.Status == WolfState.Despawned)
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_CallFirst")));
                    return true;
                }
                else if (MyWolfUtil.hasSkill(Wolf.Abilities, "Behavior"))
                {
                    if (args.length > 0 && (args[0].equalsIgnoreCase("Raid") || args[0].equalsIgnoreCase("Friendly") || args[0].equalsIgnoreCase("Aggressive") || args[0].equalsIgnoreCase("Normal")))
                    {
                        if (args[0].equalsIgnoreCase("Raid"))
                        {
                            ConfigBuffer.RegisteredSkills.get("Behavior").run(Wolf, BehaviorState.Raid);
                        }
                        else if (args[0].equalsIgnoreCase("Friendly"))
                        {
                            ConfigBuffer.RegisteredSkills.get("Behavior").run(Wolf, BehaviorState.Friendly);
                        }
                        else if (args[0].equalsIgnoreCase("Aggressive"))
                        {
                            ConfigBuffer.RegisteredSkills.get("Behavior").run(Wolf, BehaviorState.Aggressive);
                        }
                        else if (args[0].equalsIgnoreCase("Normal"))
                        {
                            ConfigBuffer.RegisteredSkills.get("Behavior").run(Wolf, BehaviorState.Normal);
                        }
                    }
                    else if (args.length > 0)
                    {
                        return false;
                    }
                    else
                    {
                        ConfigBuffer.RegisteredSkills.get("Behavior").run(Wolf, null);
                    }
                    sender.sendMessage("Your wolf is now in " + Wolf.Behavior.toString() + " mode");
                }
                else
                {
                    sender.sendMessage("Your wolf can't switch behavior mode!");
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}
