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
import de.Keyle.MyPet.api.skill.skills.Sprint;
import de.Keyle.MyPet.skill.upgrades.SprintUpgrade;
import net.kyori.adventure.text.Component;

public class SprintImpl extends AbstractSkill implements Sprint {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Sprint.class,
            UpgradeSchema.builder()
                    .bool("active").label("Active")
                    .build(), json -> new SprintUpgrade()
            .setActiveModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "active"))));

    protected UpgradeComputer<Boolean> active = new UpgradeComputer<>(false);

    public SprintImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return active.getValue();
    }

    public UpgradeComputer<Boolean> getActive() {
        return active;
    }

    @Override
    public void reset() {
        active.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.empty();
    }

    @Override
    public Component[] getUpgradeMessage() {
        if (getActive().getValue()) {
            return new Component[]{
                    upgradeMessage("Message.Skill.Sprint.Upgrade")
            };
        }
        return null;
    }

}
