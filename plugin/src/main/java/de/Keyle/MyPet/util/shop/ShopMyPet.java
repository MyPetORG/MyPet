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
import de.Keyle.MyPet.api.entity.PersistedMyPet;
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

/**
 * Mutable shop-template for the pet shop UI. Carries presentation state
 * ({@link IconMenuItem}, price, position) plus the seed data needed to mint a
 * {@link PersistedMyPet} when a player checks out.
 *
 * <p>Previously implemented {@code StoredMyPet} so that
 * {@code MyPetManager.getInactiveMyPetFromMyPet(this)} would reflectively copy
 * its fields into a {@code PersistedMyPet}. That polymorphism was a hack — the
 * shop-template's "fields" are partly computed (e.g. health = type's start HP)
 * and partly mutable (icon, price), neither of which fits the immutable
 * {@code StoredMyPet} contract. The {@link #toPersisted(MyPetPlayer)} factory
 * replaces that round-trip with a direct construction of the record, which let
 * {@code StoredMyPet} become a sealed interface (audit finding #3).</p>
 */
public class ShopMyPet {

    protected double price = 0;
    protected IconMenuItem icon;
    protected String name;
    protected int position = -1;

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

    public MyPetType getPetType() {
        return petType;
    }

    private String resolvePetName(MyPetPlayer buyer) {
        if (petName != null && !petName.isEmpty()) {
            return petName;
        }
        return Locale.getString("Name." + petType.name(), buyer);
    }

    public Component getDisplayName() {
        if (petName != null && !petName.isEmpty()) {
            return Util.SANITIZED_MINIMESSAGE.deserialize(petName);
        }
        return Component.text("MyPet");
    }

    /**
     * Mints a {@link PersistedMyPet} for the given buyer using this template's
     * seed data. Health is left to the record's compact constructor (which
     * seeds it from the pet type's start HP via the builder).
     */
    public PersistedMyPet toPersisted(MyPetPlayer buyer) {
        return PersistedMyPet.builder(buyer)
                .petType(petType)
                .petName(resolvePetName(buyer))
                .worldGroup(worldGroup)
                .exp(exp)
                .skilltree(skilltree)
                .info(NBTextendetInfo)
                .build();
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
