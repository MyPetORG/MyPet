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

import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;

import static de.Keyle.MyPet.api.util.configuration.Try.tryToLoad;

/** Reads the fixed metadata envelope (everything except {@code Skills}) of a skilltree JSON onto a {@link Skilltree}. */
public final class SkilltreeMetadataParser {

    private SkilltreeMetadataParser() {
    }

    public static void apply(SkilltreeJsonReader reader, Skilltree skilltree) {
        tryToLoad("Name", () -> reader.optString("Name").ifPresent(skilltree::setDisplayName));
        tryToLoad("Permission", () -> reader.optString("Permission").ifPresent(permission -> {
            Settings settings = new Settings("Permission");
            settings.load(permission);
            skilltree.addRequirementSettings(settings);
        }));
        tryToLoad("Display", () -> reader.optString("Display").ifPresent(skilltree::setDisplayName));
        tryToLoad("MaxLevel", () -> reader.optInt("MaxLevel").ifPresent(skilltree::setMaxLevel));
        tryToLoad("RequiredLevel", () -> reader.optInt("RequiredLevel").ifPresent(skilltree::setRequiredLevel));
        tryToLoad("Order", () -> reader.optInt("Order").ifPresent(skilltree::setOrder));
        tryToLoad("Weight", () -> reader.optDouble("Weight").ifPresent(skilltree::setWeight));
        tryToLoad("MobTypes", () -> reader.optArray("MobTypes").ifPresent(arr ->
                skilltree.setMobTypes(MobTypeParser.parse(arr, skilltree.getName()))));
        tryToLoad("Icon", () -> reader.optObject("Icon").ifPresent(iconObject -> {
            SkilltreeJsonReader iconReader = new SkilltreeJsonReader(iconObject);
            SkilltreeIcon icon = new SkilltreeIcon();
            tryToLoad("Icon.Material", () -> iconReader.optString("Material").ifPresent(icon::setMaterial));
            tryToLoad("Icon.Glowing", () -> iconReader.optBool("Glowing").ifPresent(icon::setGlowing));
            skilltree.setIcon(icon);
        }));
        tryToLoad("Inheritance", () -> reader.optObject("Inheritance").ifPresent(inheritanceObject -> {
            SkilltreeJsonReader inheritanceReader = new SkilltreeJsonReader(inheritanceObject);
            inheritanceReader.optString("Skilltree").ifPresent(skilltree::setInheritedSkilltreeName);
        }));
        tryToLoad("Description", () -> reader.optArray("Description").ifPresent(descriptionArray ->
                descriptionArray.forEach(jsonElement -> skilltree.addDescriptionLine(jsonElement.getAsString()))));
        tryToLoad("Notifications", () -> reader.optObject("Notifications").ifPresent(notificationsObject -> {
            for (String levelRuleString : notificationsObject.keySet()) {
                tryToLoad("Notification." + levelRuleString, () -> {
                    LevelRule levelRule = LevelRuleParser.parse(levelRuleString);
                    String message = notificationsObject.get(levelRuleString).getAsString();
                    skilltree.addNotification(levelRule, message);
                });
            }
        }));
        tryToLoad("Requirements", () -> reader.optArray("Requirements").ifPresent(requirementsArray ->
                requirementsArray.forEach(jsonElement -> {
                    boolean hasParameter = jsonElement.getAsString().contains(":");
                    String[] data = jsonElement.getAsString().split(":", 2);
                    Settings settings = new Settings(data[0]);
                    if (hasParameter) {
                        tryToLoad("Requirement." + jsonElement.getAsString(), () -> settings.load(data[1]));
                    }
                    skilltree.addRequirementSettings(settings);
                })));
    }
}
