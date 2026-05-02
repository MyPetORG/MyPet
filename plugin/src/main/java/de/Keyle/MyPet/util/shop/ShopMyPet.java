/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.commands.admin.CommandOptionCreate;
import de.Keyle.MyPet.services.EggIconService;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shop-template implementation of {@link StoredMyPet}. Mutable (in contrast to
 * the immutable {@code PersistedMyPet} record) because shop entries carry
 * mutable presentation state ({@link IconMenuItem}, price, position) and
 * because several read-side accessors return computed values
 * (e.g. {@link #getHealth} returns the type's start HP, {@link #getLastUsed}
 * returns the current time) rather than stored fields. Records cannot model
 * either of those cleanly, so this class stays a class.
 *
 * <p>{@link #setOwner} is the only mutator: it's invoked during shop checkout
 * so the cloned {@link StoredMyPet} produced by
 * {@code MyPetManager#getInactiveMyPetFromMyPet(this)} picks up the buyer.
 */
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
    protected MyPetType petType = MyPetType.byName("Wolf");
    protected Skilltree skilltree = null;
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
        icon.setTitle(getDisplayName());

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

    @Override
    public double getExp() {
        return exp;
    }

    @Override
    public double getHealth() {
        return MyPetApi.getMyPetInfo().getStartHP(getPetType());
    }

    @Override
    public double getSaturation() {
        return 100;
    }

    @Override
    public CompoundBinaryTag getInfo() {
        // Shop pets carry no entity snapshot — shop visual config is currently
        // dropped on activation regardless (the legacy curated NBT in
        // NBTextendetInfo isn't a snapshot). Returning an empty compound makes
        // that explicit; activated pets spawn with their type's default visuals.
        return CompoundBinaryTag.empty();
    }

    @Override
    public MyPetPlayer getOwner() {
        return petOwner;
    }

    /**
     * Set during the shop purchase flow ({@code PetShop}) so that the cloned
     * {@code PersistedMyPet} produced by
     * {@code MyPetManager#getInactiveMyPetFromMyPet(this)} picks up the buyer
     * as its owner.
     */
    public void setOwner(MyPetPlayer owner) {
        petOwner = owner;
    }

    @Override
    public String getPetName() {
        if (petName != null) {
            return petName;
        }
        if (petOwner != null) {
            return Locale.getString("Name." + petType.name(), petOwner);
        }
        return "MyPet";
    }

    @Override
    public Component getDisplayName() {
        return Util.SANITIZED_MINIMESSAGE.deserialize(getPetName());
    }

    @Override
    public MyPetType getPetType() {
        return petType;
    }

    @Override
    public boolean wantsToRespawn() {
        return true;
    }

    @Override
    public int getRespawnTime() {
        return 0;
    }

    @Override
    public Skilltree getSkilltree() {
        return skilltree;
    }

    @Override
    public CompoundBinaryTag getSkillInfo() {
        return CompoundBinaryTag.empty();
    }

    @Override
    public UUID getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    @Override
    public String getWorldGroup() {
        return worldGroup;
    }

    @Override
    public long getLastUsed() {
        return System.currentTimeMillis();
    }

    public void load(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        price = config.getDouble("Price", 0);
        position = config.getInt("Position", -1);
        MyPetType type = MyPetType.byNameOrNull(config.getString("PetType", "Pig"));
        if (type == null) return;
        petType = type;
        exp = config.getDouble("EXP");
        petName = config.getString("Name", null);
        Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(config.getString("Skilltree", null));
        if (skilltree != null && skilltree.getMobTypes().contains(petType)) {
            this.skilltree = skilltree;
        }
        for (String line : config.getStringList("Description")) {
            icon.addLoreLine(Util.SANITIZED_MINIMESSAGE.deserialize(line));
        }
        List<String> options = config.getStringList("Options");
        if (options != null && !options.isEmpty()) {
            CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
            String[] optionsArray = options.toArray(new String[0]);
            CommandOptionCreate.createInfo(petType, optionsArray, builder);
            this.NBTextendetInfo = builder.build();
        }
    }

}
