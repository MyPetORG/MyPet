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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Toolbox;
import de.Keyle.MyPet.skill.upgrades.ToolboxUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Toolbox Pet Skill
 * <p>
 * Holds one boolean {@link UpgradeComputer} per {@link Toolbox.Station} so skilltree
 * authors can unlock workstations independently across levels. The skill is stateless;
 * opening the station views is handled by the plugin's GUI layer.
 */
public class ToolboxImpl extends AbstractSkill implements Toolbox {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Toolbox.class,
            UpgradeSchema.builder()
                    .bool("crafting").label("Crafting Table")
                    .bool("anvil").label("Anvil")
                    .bool("grindstone").label("Grindstone")
                    .bool("smithing").label("Smithing Table")
                    .bool("stonecutter").label("Stonecutter")
                    .bool("loom").label("Loom")
                    .bool("cartography").label("Cartography Table")
                    .build(), json -> {
                ToolboxUpgrade upgrade = new ToolboxUpgrade();
                for (Toolbox.Station station : Toolbox.Station.values()) {
                    upgrade.setStationModifier(station,
                            UpgradeParsers.parseBoolean(UpgradeParsers.get(json, station.getUpgradeKey())));
                }
                return upgrade;
            });

    protected final Map<Station, UpgradeComputer<Boolean>> stations = new EnumMap<>(Station.class);

    public ToolboxImpl(Pet pet) {
        super(pet);
        for (Station station : Station.values()) {
            stations.put(station, new UpgradeComputer<>(false));
        }
    }

    @Override
    public UpgradeComputer<Boolean> getStation(Station station) {
        return stations.get(station);
    }

    /** Active as soon as at least one station has been unlocked. */
    @Override
    public boolean isActive() {
        for (UpgradeComputer<Boolean> station : stations.values()) {
            if (station.getValue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset() {
        stations.values().forEach(UpgradeComputer::removeAllUpgrades);
    }

    /** Comma-separated list of the unlocked stations' vanilla display names. */
    @Override
    public Component toPrettyComponent(String locale) {
        Component result = Component.empty();
        boolean first = true;
        for (Station station : getUnlockedStations()) {
            if (!first) {
                result = result.append(Component.text(", ").color(NamedTextColor.GRAY));
            }
            result = result.append(Component.translatable(station.getTranslationKey()).color(NamedTextColor.GOLD));
            first = false;
        }
        return result;
    }

    @Override
    public Component[] getUpgradeMessage() {
        List<Station> unlocked = getUnlockedStations();
        if (unlocked.isEmpty()) {
            return null;
        }
        return new Component[]{
                upgradeMessage("Message.Skill.Toolbox.Upgrade", toPrettyComponent(null))
        };
    }
}
