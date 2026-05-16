/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.services.EggIconService;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Mutable shop-template for the pet shop UI. Carries presentation state
 * ({@link IconMenuItem}, price, position) plus the seed data needed to mint a
 * {@link PersistedPet} when a player checks out.
 *
 * <p>Previously implemented {@code StoredPet} so that the manager's
 * {@code StoredPet → PersistedPet} converter (now {@code PetManager.snapshot})
 * would reflectively copy its fields into a {@code PersistedPet}. That polymorphism was a hack — the
 * shop-template's "fields" are partly computed (e.g. health = type's start HP)
 * and partly mutable (icon, price), neither of which fits the immutable
 * {@code StoredPet} contract. The {@link #toPersisted(MyPetPlayer)} factory
 * replaces that round-trip with a direct construction of the record, which let
 * {@code StoredPet} become a sealed interface.</p>
 */
public class ShopPet {

    protected double price = 0;
    protected IconMenuItem icon;
    protected String name;
    protected int position = -1;

    protected String petName = "";
    protected String worldGroup = "";
    protected double exp = 0;
    protected PetType petType = PetType.byName("Wolf");
    protected Skilltree skilltree = null;
    /**
     * Option strings parsed from {@code Options:} in pet-shops.yml — applied to
     * a detached Bukkit mob at {@link #toPersisted(MyPetPlayer) checkout} time,
     * not at config load (worlds may not yet be loaded). See Cluster L in
     * {@code docs/pet-type-issue-tracker.md} for the design.
     */
    protected String[] options = new String[0];

    public ShopPet(String name) {
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
        IconMenuItem icon = this.icon.copy();
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

    public PetType getPetType() {
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
     * Mints a {@link PersistedPet} for the given buyer using this template's
     * seed data. Health is left to the record's compact constructor (which
     * seeds it from the pet type's start HP via the builder).
     *
     * <p>Per-type options are applied here, not at YAML load: each purchase
     * obtains a detached Bukkit mob via {@code World#createEntity} in the
     * buyer's world, runs {@link CommandOptionCreate#applyOptions} against it,
     * captures vanilla NBT via {@link PetEntitySnapshot#capture}, and stores
     * the captured compound on {@code PersistedPet.info}. The detached mob
     * never enters the world (no {@code CreatureSpawnEvent}, no spawn packet,
     * no Folia region scheduling) and is GC'd after capture.
     */
    public PersistedPet toPersisted(MyPetPlayer buyer) {
        return PersistedPet.builder(buyer)
                .petType(petType)
                .petName(resolvePetName(buyer))
                .worldGroup(worldGroup)
                .exp(exp)
                .skilltree(skilltree)
                .info(materializeInfo(buyer))
                .build();
    }

    /**
     * Builds the vanilla-NBT envelope for {@link PersistedPet#info()} via
     * {@link PetEntitySnapshot#captureForOptions} in the buyer's world. Returns
     * an empty compound on any failure (offline buyer, unknown Bukkit class,
     * etc.) — the pet is still purchasable, just without per-type options.
     */
    private CompoundBinaryTag materializeInfo(MyPetPlayer buyer) {
        Player player = buyer.getPlayer();
        if (player == null) {
            if (options.length > 0) {
                MyPetApi.getLogger().warning("ShopPet: buyer " + buyer.getName()
                        + " is offline at checkout — " + petType.name()
                        + " purchased without per-type options.");
            }
            return CompoundBinaryTag.empty();
        }
        return PetEntitySnapshot.captureForOptions(petType, options,
                player.getWorld(), player.getLocation());
    }

    public void load(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        price = config.getDouble("Price", 0);
        position = config.getInt("Position", -1);
        PetType type = PetType.byNameOrNull(config.getString("PetType", "Pig"));
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
        List<String> rawOptions = config.getStringList("Options");
        if (rawOptions != null && !rawOptions.isEmpty()) {
            this.options = rawOptions.toArray(new String[0]);
        }
    }

}
