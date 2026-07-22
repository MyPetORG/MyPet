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

package de.Keyle.MyPet.api.skill.skills;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryView;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill that turns the pet into a set of portable utility workstations (crafting table,
 * anvil, grindstone, etc.). Each {@link Station} is unlocked independently through the
 * skilltree; the owner opens an unlocked station as a virtual view — no placed block needed.
 *
 * <p>Furnace-type blocks are deliberately not offered: their menus need a real block
 * entity to smelt, so they cannot be opened as virtual views.
 */
@SkillName(value = "Toolbox", translationNode = "Name.Skill.Toolbox")
public interface Toolbox extends Skill {

    /** Returns the upgrade computer controlling whether the given station is unlocked. */
    UpgradeComputer<Boolean> getStation(Station station);

    /** Returns all currently unlocked stations, in {@link Station} declaration order. */
    default List<Station> getUnlockedStations() {
        List<Station> unlocked = new ArrayList<>();
        for (Station station : Station.values()) {
            if (getStation(station).getValue()) {
                unlocked.add(station);
            }
        }
        return unlocked;
    }

    /**
     * Enumerates the workstations the Toolbox skill can unlock. Each entry maps its
     * skilltree upgrade key to a GUI icon and the vanilla block translation key.
     */
    enum Station {
        CRAFTING_TABLE("crafting", Material.CRAFTING_TABLE, "block.minecraft.crafting_table"),
        ANVIL("anvil", Material.ANVIL, "block.minecraft.anvil"),
        GRINDSTONE("grindstone", Material.GRINDSTONE, "block.minecraft.grindstone"),
        SMITHING_TABLE("smithing", Material.SMITHING_TABLE, "block.minecraft.smithing_table"),
        STONECUTTER("stonecutter", Material.STONECUTTER, "block.minecraft.stonecutter"),
        LOOM("loom", Material.LOOM, "block.minecraft.loom"),
        CARTOGRAPHY_TABLE("cartography", Material.CARTOGRAPHY_TABLE, "block.minecraft.cartography_table");

        @Getter
        private final String upgradeKey;
        @Getter
        private final Material icon;
        @Getter
        private final String translationKey;

        Station(String upgradeKey, Material icon, String translationKey) {
            this.upgradeKey = upgradeKey;
            this.icon = icon;
            this.translationKey = translationKey;
        }

        /** Opens this station as a virtual view for the given player (no block required). */
        public InventoryView open(HumanEntity viewer) {
            return switch (this) {
                case CRAFTING_TABLE -> viewer.openWorkbench(null, true);
                case ANVIL -> viewer.openAnvil(null, true);
                case GRINDSTONE -> viewer.openGrindstone(null, true);
                case SMITHING_TABLE -> viewer.openSmithingTable(null, true);
                case STONECUTTER -> viewer.openStonecutter(null, true);
                case LOOM -> viewer.openLoom(null, true);
                case CARTOGRAPHY_TABLE -> viewer.openCartographyTable(null, true);
            };
        }
    }
}
