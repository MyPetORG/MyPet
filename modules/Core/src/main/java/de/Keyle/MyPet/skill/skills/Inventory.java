/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.InventoryInfo;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Inventory extends InventoryInfo implements SkillInstance, NBTStorage, ActiveSkill {
    protected CustomInventory inv;
    protected MyPet myPet;

    public Inventory(boolean addedByInheritance) {
        super(addedByInheritance);
        inv = MyPetApi.getCompatUtil().getComapatInstance(CustomInventory.class, "util.inventory", "CustomInventory");
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
        inv.setName(myPet.getPetName());
    }

    public CustomInventory getInventory() {
        return inv;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof InventoryInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("add")) {
                rows += upgrade.getProperties().getAs("add", TagInt.class).getIntData();
                if (rows > 6) {
                    rows = 6;
                }
                inv.setSize(rows * 9);
                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Inventory.Upgrade", myPet.getOwner()), myPet.getPetName(), inv.getSize()));
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("drop")) {
                dropOnDeath = upgrade.getProperties().getAs("drop", TagByte.class).getBooleanData();
            }
        }
    }

    public String getFormattedValue() {
        return rows + " " + Translation.getString("Name.Rows", myPet.getOwner());
    }

    public void reset() {
        rows = 0;
        inv.close();
        inv.setSize(0);
    }

    public boolean activate() {
        if (rows > 0) {
            if (myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !Configuration.Skilltree.Skill.Inventory.OPEN_IN_CREATIVE && !Permissions.has(myPet.getOwner().getPlayer(), "MyPet.admin", false)) {
                myPet.getOwner().sendMessage(Translation.getString("Message.Skill.Inventory.Creative", myPet.getOwner()));
                return false;
            }
            MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Open);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                myPet.getOwner().sendMessage(Translation.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                return false;
            }
            if (!myPet.getLocation().getBlock().isLiquid()) {
                inv.setName(myPet.getPetName());
                openInventory(myPet.getOwner().getPlayer());
                return true;
            } else {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Inventory.Swimming", myPet.getOwner()), myPet.getPetName()));
                return false;
            }
        } else {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Inventory.NotAvailable", myPet.getOwner()), myPet.getPetName()));
            return false;
        }
    }

    public void openInventory(Player p) {
        inv.open(p);
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
    public SkillInstance cloneSkill() {
        Inventory newSkill = new Inventory(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}