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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
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
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner))
            {
                MyPet myPet = MyPetList.getMyPet(petOwner);

                if (myPet.status == PetState.Despawned)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")).replace("%petname%", myPet.petName));
                    return true;
                }
                else if (myPet.getSkills().hasSkill("Behavior"))
                {
                    Behavior behaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
                    if (args.length == 1)
                    {
                        if ((args[0].equalsIgnoreCase("friendly") || args[0].equalsIgnoreCase("friend")) && BehaviorState.Friendly.isActive())
                        {
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Friendly);
                        }
                        else if ((args[0].equalsIgnoreCase("aggressive") || args[0].equalsIgnoreCase("aggro")) && BehaviorState.Aggressive.isActive())
                        {
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Aggressive);
                        }
                        else if (args[0].equalsIgnoreCase("farm") && BehaviorState.Farm.isActive())
                        {
                            behaviorSkill.activateBehavior(BehaviorState.Farm);
                        }
                        else if (args[0].equalsIgnoreCase("raid") && BehaviorState.Raid.isActive())
                        {
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Raid);
                        }
                        else if (args[0].equalsIgnoreCase("normal"))
                        {
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Normal);
                        }
                        else
                        {
                            behaviorSkill.activate();
                            return false;
                        }
                    }
                    else
                    {
                        behaviorSkill.activate();
                    }
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
            }
        }
        return true;
    }
}