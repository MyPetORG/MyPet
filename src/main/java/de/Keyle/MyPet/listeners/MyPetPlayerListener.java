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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.IMyPet;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Behavior.BehaviorState;
import de.Keyle.MyPet.skill.skills.implementation.Control;
import de.Keyle.MyPet.skill.skills.implementation.Inventory;
import de.Keyle.MyPet.skill.skills.implementation.inventory.MyPetCustomInventory;
import de.Keyle.MyPet.util.*;
import net.minecraft.server.v1_5_R2.EntityItem;
import net.minecraft.server.v1_5_R2.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_5_R2.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class MyPetPlayerListener implements Listener
{
    private final int[] ControllIgnoreBlocks = {6, 27, 28, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 90, 92, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 116, 117, 118, 119};

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if ((event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && event.getPlayer().getItemInHand().getType() == Control.ITEM && MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.getStatus() == PetState.Here && myPet.getCraftPet().canMove())
            {
                if (myPet.getSkills().isSkillActive("Control"))
                {
                    if (myPet.getSkills().isSkillActive("Behavior"))
                    {
                        Behavior behavior = (Behavior) myPet.getSkills().getSkill("Behavior");
                        if (behavior.getBehavior() == BehaviorState.Aggressive || behavior.getBehavior() == BehaviorState.Farm)
                        {
                            event.getPlayer().sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_ControlAggroFarm").replace("%petname%", myPet.petName).replace("%mode%", "" + behavior.getBehavior().name())));
                            return;
                        }
                    }
                    if (myPet.getSkills().isSkillActive("Ride"))
                    {
                        if (myPet.getCraftPet().getHandle().hasRider())
                        {
                            return;
                        }
                    }
                    if (!MyPetPermissions.hasExtended(event.getPlayer(), "MyPet.user.extended.Control"))
                    {
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CantUse")));
                        return;
                    }
                    Block block = event.getPlayer().getTargetBlock(null, 100);
                    if (block != null && block.getType() != Material.AIR)
                    {
                        for (int i : ControllIgnoreBlocks)
                        {
                            if (block.getTypeId() == i)
                            {
                                block = block.getRelative(BlockFace.DOWN);
                                break;
                            }
                        }
                        ((Control) myPet.getSkills().getSkill("Control")).setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetPlayerJoin(final PlayerJoinEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getPlayer()))
        {
            MyPetPlayer joinedPlayer = MyPetPlayer.getMyPetPlayer(event.getPlayer());

            if (!joinedPlayer.hasMyPet() && joinedPlayer.hasInactiveMyPets())
            {
                IMyPet myPet = MyPetList.getLastActiveMyPet(joinedPlayer);
                if (!(joinedPlayer.hasLastActiveMyPet() && joinedPlayer.getLastActiveMyPetUUID() == null))
                {
                    if (joinedPlayer.getLastActiveMyPetUUID() == null)
                    {
                        if (joinedPlayer.hasInactiveMyPets())
                        {
                            MyPetList.setMyPetActive(joinedPlayer.getInactiveMyPets()[0]);
                        }
                    }
                    else if (myPet != null && myPet instanceof InactiveMyPet)
                    {
                        MyPetList.setMyPetActive((InactiveMyPet) myPet);
                    }
                }
            }
            if (joinedPlayer.hasMyPet())
            {
                MyPet myPet = MyPetList.getMyPet(event.getPlayer());
                if (myPet.getStatus() == PetState.Dead)
                {
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime)));
                }
                else if (myPet.getLocation().getWorld() == event.getPlayer().getLocation().getWorld() && myPet.getLocation().distance(event.getPlayer().getLocation()) < 75)
                {
                    myPet.createPet();
                }
                else
                {
                    myPet.status = PetState.Despawned;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.getSkills().isSkillActive("Behavior"))
            {
                Behavior behavior = (Behavior) myPet.getSkills().getSkill("Behavior");
                if (behavior.getBehavior() != BehaviorState.Normal && behavior.getBehavior() != BehaviorState.Friendly)
                {
                    behavior.setBehavior(BehaviorState.Normal);
                }
            }
            myPet.removePet();
            MyPetPlugin.getPlugin().savePets(false);
        }
    }

    @EventHandler
    public void onMyPetPlayerChangeWorld(final PlayerChangedWorldEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getPlayer().getName()))
        {
            final MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (myPetPlayer.hasMyPet())
            {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here)
                {
                    myPet.removePet();
                    myPet.setLocation(event.getPlayer().getLocation());

                    MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable()
                    {
                        public void run()
                        {
                            if (myPetPlayer.hasMyPet())
                            {
                                MyPet runMyPet = myPetPlayer.getMyPet();
                                switch (runMyPet.createPet())
                                {
                                    case Canceled:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SpawnPrevent")).replace("%petname%", runMyPet.petName));
                                        break;
                                    case NoSpace:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SpawnNoSpace")).replace("%petname%", runMyPet.petName));
                                        break;
                                    case Dead:
                                        if (runMyPet != myPet)
                                        {
                                            runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", runMyPet.petName).replace("%time%", "" + runMyPet.respawnTime));
                                        }
                                        break;
                                    case Success:
                                        if (runMyPet != myPet)
                                        {
                                            runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", runMyPet.petName));
                                        }
                                        break;
                                }
                            }
                        }
                    }, 25L);
                }
            }
        }
    }

    @EventHandler
    public void onMyPetPlayerTeleport(final PlayerTeleportEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getPlayer().getName()))
        {
            MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (myPetPlayer.hasMyPet())
            {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here && event.getTo().getWorld() == myPet.getLocation().getWorld() && (event.getPlayer().getLocation().distance(event.getTo()) > 30))
                {
                    myPet.removePet();
                    myPet.setLocation(event.getTo());

                    MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable()
                    {
                        public void run()
                        {
                            if (myPet.status == PetState.Despawned)
                            {
                                switch (myPet.createPet())
                                {
                                    case Canceled:
                                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SpawnPrevent")).replace("%petname%", myPet.petName));
                                        break;
                                    case NoSpace:
                                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SpawnNoSpace")).replace("%petname%", myPet.petName));
                                        break;
                                }
                            }
                        }
                    }, 25L);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getEntity().getPlayer().getName()))
        {
            MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(event.getEntity().getPlayer().getName());
            if (myPetPlayer.hasMyPet())
            {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here && MyPetConfiguration.DROP_PET_INVENTORY_AFTER_PLAYER_DEATH)
                {
                    World world = ((CraftWorld) event.getEntity().getLocation().getWorld()).getHandle();
                    Location petLocation = event.getEntity().getLocation();
                    MyPetCustomInventory inv = ((Inventory) myPet.getSkills().getSkill("Inventory")).inv;
                    for (int i = 0 ; i < inv.getSize() ; i++)
                    {
                        net.minecraft.server.v1_5_R2.ItemStack is = inv.splitWithoutUpdate(i);
                        if (is != null)
                        {
                            is = is.cloneItemStack();
                            EntityItem itemEntity = new EntityItem(world, petLocation.getX(), petLocation.getY(), petLocation.getZ(), is);
                            itemEntity.pickupDelay = 10;
                            world.addEntity(itemEntity);
                        }
                    }
                }
            }
        }
    }
}