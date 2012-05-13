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

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBehavior implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player owner = (Player) sender;
            if (MyPetList.hasMyWolf(owner))
            {
                MyWolf MWolf = MyPetList.getMyWolf(owner);

                if (MWolf.Status == PetState.Despawned)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")));
                    return true;
                }
                else if (MWolf.skillSystem.hasSkill("Behavior"))
                {
                    Behavior Skill = (Behavior) MWolf.skillSystem.getSkill("Behavior");
                    if (args.length == 1)
                    {
                        if (args[0].equalsIgnoreCase("Friendly") || args[0].equalsIgnoreCase("Fri"))
                        {
                            Skill.activateBehavior(Behavior.BehaviorState.Friendly);
                        }
                        else if (args[0].equalsIgnoreCase("Aggressive") || args[0].equalsIgnoreCase("Agg"))
                        {
                            Skill.activateBehavior(Behavior.BehaviorState.Aggressive);
                        }
                        /*
                        else if (args[0].equalsIgnoreCase("Raid") || args[0].equalsIgnoreCase("Rai"))
                        {
                            Skill.activateBehavior(Behavior.BehaviorState.Raid);
                        }
                        */
                        else
                        {
                            Skill.activateBehavior(Behavior.BehaviorState.Normal);
                        }
                    }
                    else
                    {
                        Skill.activate();
                    }
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}