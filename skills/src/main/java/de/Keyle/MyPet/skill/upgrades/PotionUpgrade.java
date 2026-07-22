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

package de.Keyle.MyPet.skill.upgrades;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.modifier.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.skills.Potion;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@ToString
@SkillName("Potion")
public class PotionUpgrade implements Upgrade<Potion> {

    /** Potions this level grants into the arsenal (added on apply, removed on invert). */
    private final List<Potion.Entry> entries = new ArrayList<>();

    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier materializeModifier = null;

    public PotionUpgrade addEntry(Potion.Entry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public void apply(Potion skill) {
        for (Potion.Entry entry : entries) {
            skill.addEntry(entry);
        }
        skill.getMaterialize().addUpgrade(materializeModifier);
    }

    @Override
    public void invert(Potion skill) {
        for (Potion.Entry entry : entries) {
            skill.removeEntry(entry);
        }
        skill.getMaterialize().removeUpgrade(materializeModifier);
    }
}
