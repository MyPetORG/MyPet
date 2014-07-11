/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.inventory.CustomInventory;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.InventoryInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.support.Permissions;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.PacketPlayOutNamedSoundEffect;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Inventory extends InventoryInfo implements ISkillInstance, ISkillStorage, ISkillActive {
    public CustomInventory inv = new CustomInventory("Pet's Inventory", 0);
    public static boolean OPEN_IN_CREATIVEMODE = true;
    public static boolean DROP_WHEN_OWNER_DIES = true;
    private MyPet myPet;

    public Inventory(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
        inv.setName(myPet.getPetName());
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof InventoryInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("add")) {
                rows += upgrade.getProperties().getAs("add", TagInt.class).getIntData();
                if (rows > 6) {
                    rows = 6;
                }
                inv.setSize(rows * 9);
                if (!quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Inventory.Upgrade", myPet.getOwner()), myPet.getPetName(), inv.getSize()));
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("drop")) {
                dropOnDeath = upgrade.getProperties().getAs("drop", TagByte.class).getBooleanData();
            }
        }
    }

    public String getFormattedValue() {
        return rows + " " + Locales.getString("Name.Rows", myPet.getOwner());
    }

    public void reset() {
        rows = 0;
        inv.close();
        inv.setSize(0);
    }

    public boolean activate() {
        if (rows > 0) {
            if (myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !OPEN_IN_CREATIVEMODE && !Permissions.has(myPet.getOwner().getPlayer(), "MyPet.admin", false)) {
                myPet.sendMessageToOwner(Locales.getString("Message.Skill.Inventory.Creative", myPet.getOwner()));
                return false;
            }
            if (myPet.getOwner().isInExternalGames()) {
                myPet.sendMessageToOwner(Locales.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                return false;
            }
            if (!myPet.getLocation().getBlock().isLiquid()) {
                inv.setName(myPet.getPetName());
                openInventory(myPet.getOwner().getPlayer());
                return true;
            } else {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Inventory.Swimming", myPet.getOwner()), myPet.getPetName()));
                return false;
            }
        } else {
            myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Inventory.NotAvailable", myPet.getOwner()), myPet.getPetName()));
            return false;
        }
    }

    public void openInventory(Player p) {
        EntityPlayer eh = ((CraftPlayer) p).getHandle();
        PacketPlayOutNamedSoundEffect packet = new PacketPlayOutNamedSoundEffect("mob.horse.leather", p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), 1.0F, 1.0F);
        eh.playerConnection.sendPacket(packet);
        eh.openContainer(inv);
    }

    public void closeInventory() {
        inv.close();
    }

    public void load(TagCompound compound) {
        inv.load(compound);
    }

    public TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        inv.save(nbtTagCompound);
        return nbtTagCompound;
    }

    public boolean isActive() {
        return rows > 0;
    }

    public boolean dropOnDeath() {
        return dropOnDeath;
    }

    @Override
    public ISkillInstance cloneSkill() {
        Inventory newSkill = new Inventory(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}