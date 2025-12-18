/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.NotImplemented;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EggIconService;
import de.Keyle.MyPet.commands.admin.CommandOptionCreate;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShopMyPet implements StoredMyPet {

    protected double price = 0;
    protected IconMenuItem icon;
    protected String name;
    protected int position = -1;

    protected UUID uuid = null;
    protected MyPetPlayer petOwner = null;
    protected String petName = "";
    protected String worldGroup = "";
    protected double exp = 0;
    protected MyPetType petType = MyPetType.Wolf;
    protected Skilltree skilltree = null;
    protected CompoundBinaryTag NBTSkills = CompoundBinaryTag.empty();
    protected CompoundBinaryTag NBTextendetInfo = CompoundBinaryTag.empty();

    public ShopMyPet(String name) {
        this.name = name;
        this.icon = new IconMenuItem();
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public IconMenuItem getIcon() {
        IconMenuItem icon = this.icon.clone();
        Optional<EggIconService> egg = MyPetApi.getServiceManager().getService(EggIconService.class);
        egg.ifPresent(eggIconService -> eggIconService.updateIcon(petType, icon));
        icon.setTitle(ChatColor.AQUA + Colorizer.setColors(getPetName()));

        return icon;
    }

    public void setIcon(IconMenuItem icon) {
        this.icon = icon;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getExp() {
        return exp;
    }

    public void setExp(double exp) {
        this.exp = exp;
    }

    public double getHealth() {
        return MyPetApi.getMyPetInfo().getStartHP(getPetType());
    }

    @NotImplemented
    public void setHealth(double health) {
    }

    @Override
    public double getSaturation() {
        return 100;
    }

    @NotImplemented
    public void setSaturation(double value) {
    }

    public CompoundBinaryTag getInfo() {
        return NBTextendetInfo;
    }

    public void setInfo(CompoundBinaryTag info) {
        NBTextendetInfo = info != null ? info : CompoundBinaryTag.empty();
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    public void setOwner(MyPetPlayer owner) {
        petOwner = owner;
    }

    public String getPetName() {
        if (petName != null) {
            return Colorizer.setColors(petName);
        }
        if (petOwner != null) {
            return Colorizer.setColors(Translation.getString("Name." + petType.name(), petOwner));
        }
        return "MyPet";
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public MyPetType getPetType() {
        return petType;
    }

    public void setPetType(MyPetType petType) {
        this.petType = petType;
    }

    public boolean wantsToRespawn() {
        return true;
    }

    @NotImplemented
    public void setWantsToRespawn(boolean wantsToRespawn) {
    }

    public int getRespawnTime() {
        return 0;
    }

    @NotImplemented
    public void setRespawnTime(int respawnTime) {
    }

    public Skilltree getSkilltree() {
        return skilltree;
    }

    public boolean setSkilltree(Skilltree skilltree) {
        this.skilltree = skilltree;
        MyPetSelectSkilltreeEvent selectEvent = new MyPetSelectSkilltreeEvent(this, skilltree, MyPetSelectSkilltreeEvent.Source.Shop);
        Bukkit.getServer().getPluginManager().callEvent(selectEvent);
        return true;
    }

    public CompoundBinaryTag getSkillInfo() {
        return NBTSkills;
    }

    public void setSkills(CompoundBinaryTag skills) {
        NBTSkills = skills != null ? skills : CompoundBinaryTag.empty();
    }

    public UUID getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    @NotImplemented
    public void setUUID(UUID uuid) {
    }

    @Override
    public String getWorldGroup() {
        return worldGroup;
    }

    public void setWorldGroup(String worldGroup) {
        if (worldGroup != null) {
            this.worldGroup = worldGroup;
        }
    }

    @Override
    public long getLastUsed() {
        return System.currentTimeMillis();
    }

    @NotImplemented
    public void setLastUsed(long lastUsed) {
    }

    public void load(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        price = config.getDouble("Price", 0);
        position = config.getInt("Position", -1);
        petType = MyPetType.byName(config.getString("PetType", "Pig"));
        exp = config.getDouble("EXP");
        petName = config.getString("Name", null);
        Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(config.getString("Skilltree", null));
        if (skilltree != null && skilltree.getMobTypes().contains(petType)) {
            this.skilltree = skilltree;
        }
        for (String line : config.getStringList("Description")) {
            icon.addLoreLine(ChatColor.RESET + Colorizer.setColors(line));
        }
        List<String> options = config.getStringList("Options");
        if (options != null && !options.isEmpty()) {
            CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
            String[] optionsArray = options.toArray(new String[0]);
            CommandOptionCreate.createInfo(petType, optionsArray, builder);
            this.NBTextendetInfo = builder.build();
        }
    }

    @Override
    public String toString() {
        return "ShopMyPet{type=" + getPetType().name() + ", exp=" + getExp() + ", worldgroup=" + worldGroup + (skilltree != null ? ", skilltree=" + skilltree.getName() : "") + "}";
    }
}