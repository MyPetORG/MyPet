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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.skills.Inventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.EntityItem;
import net.minecraft.server.ItemStack;
import net.minecraft.server.World;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class CommandRelease implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player owner = (Player) sender;
            if (MyPetList.hasMyWolf(owner))
            {
                MyWolf MWolf = MyPetList.getMyWolf(owner);

                if (!MyPetPermissions.has(owner, "MyPet.user.release"))
                {
                    return true;
                }
                if (MWolf.Status == PetState.Despawned)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")));
                    return true;
                }
                if (args.length < 1)
                {
                    return false;
                }
                String name = "";
                for (String arg : args)
                {
                    name += arg + " ";
                }
                name = name.substring(0, name.length() - 1);
                if (MWolf.Name.equalsIgnoreCase(name))
                {
                    if (MWolf.skillSystem.hasSkill("Inventory") && MWolf.skillSystem.getSkill("Inventory").getLevel() > 0)
                    {
                        World world = MWolf.Wolf.getHandle().world;
                        Location loc = MWolf.getLocation();
                        for (ItemStack is : ((Inventory) MWolf.skillSystem.getSkill("Inventory")).inv.getContents())
                        {
                            if (is != null)
                            {
                                EntityItem entity = new EntityItem(world, loc.getX(), loc.getY(), loc.getZ(), is);
                                entity.pickupDelay = 10;
                                world.addEntity(entity);
                            }
                        }
                    }
                    MWolf.getLocation().getWorld().spawnCreature(MWolf.getLocation(), EntityType.WOLF);
                    MWolf.removeWolf();


                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Release")).replace("%wolfname%", MWolf.Name));
                    MyPetList.removeMyWolf(MWolf);
                    MyPetPlugin.getPlugin().saveWolves(MyPetPlugin.NBTWolvesFile);
                    return true;
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Name")).replace("%wolfname%", MWolf.Name));
                    return false;
                }
            }
            else
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return false;
    }
}