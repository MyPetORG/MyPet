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
import de.Keyle.MyPet.api.skill.skills.Armor;
import de.Keyle.MyPet.skill.upgrades.ArmorUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ArmorImpl extends AbstractSkill implements Armor {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Armor.class,
            UpgradeSchema.builder()
                    .integer("armor").label("Armor").cumulative()
                    .integer("toughness").label("Toughness").cumulative()
                    .build(), json -> new ArmorUpgrade()
            .setArmorModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "armor")))
            .setToughnessModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "toughness"))));

    protected UpgradeComputer<Integer> armor = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> toughness = new UpgradeComputer<>(0);

    public ArmorImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return armor.getValue() > 0 || toughness.getValue() > 0;
    }

    @Override
    public void reset() {
        armor.removeAllUpgrades();
        toughness.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text("+"))
                .append(Component.text(armor.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text(" Armor | +"))
                .append(Component.text(toughness.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text(" Toughness"))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Armor.Upgrade", armor.getValue(), toughness.getValue())
        };
    }

    public UpgradeComputer<Integer> getArmor() {
        return armor;
    }

    public UpgradeComputer<Integer> getToughness() {
        return toughness;
    }

}
