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

package de.Keyle.MyPet.skill.skilltree;

import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.UpgradeParser;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;

import static de.Keyle.MyPet.api.util.configuration.Try.tryToLoad;

/** Reads the {@code Skills.<name>.Upgrades.<levelRule>} section and dispatches each upgrade to the skill's {@link UpgradeParser} SPI. */
public final class SkillUpgradeParser {

    private SkillUpgradeParser() {
    }

    public static void apply(SkilltreeJsonReader reader, Skilltree skilltree) {
        tryToLoad("Skills", () -> reader.optObject("Skills").ifPresent(skillsObject -> {
            for (String skillName : skillsObject.keySet()) {
                JsonObject skillObject = skillsObject.getAsJsonObject(skillName);

                tryToLoad("Skills." + skillName + ".Upgrades", () ->
                        new SkilltreeJsonReader(skillObject).optObject("Upgrades").ifPresent(upgradesObject -> {
                        for (String levelRuleString : upgradesObject.keySet()) {
                            tryToLoad("Skills." + skillName + ".Upgrades." + levelRuleString, () -> {
                                LevelRule levelRule = LevelRuleParser.parse(levelRuleString);

                                JsonObject upgradeObject = upgradesObject.getAsJsonObject(levelRuleString);
                                tryToLoad("Skills." + skillName + ".Upgrades." + levelRuleString + ".Upgrade", () -> {
                                    Upgrade<?> upgrade = loadUpgrade(skillName, upgradeObject);
                                    if (upgrade != null) {
                                        skilltree.addUpgrade(levelRule, upgrade);
                                    } else {
                                        MyPetApi.getLogger().warning("Unknown skill '" + skillName + "' in skilltree '" + skilltree.getName() + "' - skipping upgrade");
                                    }
                                });
                            });
                        }
                    }));
            }
        }));
    }

    private static Upgrade<?> loadUpgrade(String skillName, JsonObject upgradeObject) {
        UpgradeParser<?> parser = MyPetApi.getSkillManager().getUpgradeParser(skillName);
        if (parser == null) {
            return null;
        }
        return parser.parse(upgradeObject);
    }
}
