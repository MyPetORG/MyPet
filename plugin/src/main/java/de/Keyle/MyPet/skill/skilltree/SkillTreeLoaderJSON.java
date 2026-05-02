/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import com.google.gson.*;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.InvalidSkilltreeException;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.UpgradeParser;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;
import de.Keyle.MyPet.skill.skilltree.levelrule.DynamicLevelRule;
import de.Keyle.MyPet.skill.skilltree.levelrule.StaticLevelRule;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.Keyle.MyPet.api.skill.UpgradeParsers.get;
import static de.Keyle.MyPet.api.util.configuration.Try.tryToLoad;

public class SkillTreeLoaderJSON {

    final static Pattern LEVEL_RULE_REGEX = Pattern.compile("(?:%(\\d+))|(?:<(\\d+))|(?:>(\\d+))");

    public static void loadSkilltrees(File skilltreePath) {
        File[] skilltreeFiles = skilltreePath.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".st.json"));
        if (skilltreeFiles != null) {
            for (File skilltreeFile : skilltreeFiles) {
                loadSkilltree(skilltreeFile);
            }
        }
    }

    public static void loadSkilltree(File skilltreeFile) {
        if (skilltreeFile.exists()) {
            try {
                loadSkilltree(loadJsonObject(skilltreeFile));
            } catch (InvalidSkilltreeException | JsonSyntaxException e) {
                MyPetApi.getLogger().warning("Error in " + skilltreeFile.getName() + " -> Skilltree not loaded.");
                MyPetApi.getLogger().warning(e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    public static void loadSkilltree(JsonObject skilltreeObject) {
        if (!containsKey(skilltreeObject, "ID")) {
            return;
        }

        Skilltree skilltree;
        String skilltreeID = get(skilltreeObject, "ID").getAsString();

        if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeID)) {
            return;
        }

        skilltree = new Skilltree(skilltreeID);

        tryToLoad("Name", () -> {
            if (containsKey(skilltreeObject, "Name")) {
                skilltree.setDisplayName(get(skilltreeObject, "Name").getAsString());
            }
        });
        tryToLoad("Permission", () -> {
            if (containsKey(skilltreeObject, "Permission")) {
                String permission = get(skilltreeObject, "Permission").getAsString();
                Settings settings = new Settings("Permission");
                settings.load(permission);
                skilltree.addRequirementSettings(settings);
                //TODO warnung zum aktualisieren
            }
        });
        tryToLoad("Display", () -> {
            if (containsKey(skilltreeObject, "Display")) {
                skilltree.setDisplayName(get(skilltreeObject, "Display").getAsString());
            }
        });
        tryToLoad("MaxLevel", () -> {
            if (containsKey(skilltreeObject, "MaxLevel")) {
                skilltree.setMaxLevel((get(skilltreeObject, "MaxLevel").getAsInt()));
            }
        });
        tryToLoad("RequiredLevel", () -> {
            if (containsKey(skilltreeObject, "RequiredLevel")) {
                skilltree.setRequiredLevel((get(skilltreeObject, "RequiredLevel").getAsInt()));
            }
        });
        tryToLoad("Order", () -> {
            if (containsKey(skilltreeObject, "Order")) {
                skilltree.setOrder((get(skilltreeObject, "Order").getAsInt()));
            }
        });
        tryToLoad("Weight", () -> {
            if (containsKey(skilltreeObject, "Weight")) {
                skilltree.setWeight((get(skilltreeObject, "Weight").getAsDouble()));
            }
        });

        tryToLoad("MobTypes", () -> {
            if (containsKey(skilltreeObject, "MobTypes")) {
                List<MyPetType> availableTypes = MyPetType.all();
                JsonArray mobTypeArray = get(skilltreeObject, "MobTypes").getAsJsonArray();
                Set<MyPetType> mobTypes = new HashSet<>();
                if (mobTypeArray.isEmpty()) {
                    mobTypes.addAll(availableTypes);
                } else {
                    boolean allNegative = true;
                    for (JsonElement o : mobTypeArray) {
                        String type = o.getAsString();
                        if (!type.startsWith("-")) {
                            allNegative = false;
                            break;
                        }
                    }
                    if (allNegative) {
                        mobTypes.addAll(availableTypes);
                    }
                    mobTypeArray.forEach(jsonElement -> {
                        String type = jsonElement.getAsString();
                        if (type.equals("*")) {
                            mobTypes.addAll(availableTypes);
                        } else {
                            boolean negative = false;
                            if (type.startsWith("-")) {
                                type = type.substring(1);
                                negative = true;
                            }
                            MyPetType mobType = MyPetType.byNameOrNull(type);
                            if (mobType == null) {
                                MyPetApi.getLogger().warning("Skilltree '" + skilltreeID + "': Unknown mob type '" + type + "' - skipping (not a valid MyPet type or not available in this Minecraft version)");
                            } else if (mobType.checkMinecraftVersion()) {
                                if (negative) {
                                    mobTypes.remove(mobType);
                                } else {
                                    mobTypes.add(mobType);
                                }
                            }
                        }
                    });
                }
                skilltree.setMobTypes(mobTypes);
            }
        });
        tryToLoad("Icon", () -> {
            if (containsKey(skilltreeObject, "Icon")) {
                JsonObject iconObject = get(skilltreeObject, "Icon").getAsJsonObject();
                SkilltreeIcon icon = new SkilltreeIcon();
                tryToLoad("Icon.Material", () -> {
                    if (containsKey(iconObject, "Material")) {
                        icon.setMaterial(get(iconObject, "Material").getAsString());
                    }
                });
                tryToLoad("Icon.Glowing", () -> {
                    if (containsKey(iconObject, "Glowing")) {
                        icon.setGlowing(get(iconObject, "Glowing").getAsBoolean());
                    }
                });
                skilltree.setIcon(icon);
            }
        });
        tryToLoad("Inheritance", () -> {
            if (containsKey(skilltreeObject, "Inheritance")) {
                JsonObject inheritanceObject = get(skilltreeObject, "Inheritance").getAsJsonObject();
                if (containsKey(inheritanceObject, "Skilltree")) {
                    skilltree.setInheritedSkilltreeName(get(inheritanceObject, "Skilltree").getAsString());
                }
            }
        });
        tryToLoad("Description", () -> {
            if (containsKey(skilltreeObject, "Description")) {
                JsonArray descriptionArray = get(skilltreeObject, "Description").getAsJsonArray();
                descriptionArray.forEach(jsonElement -> skilltree.addDescriptionLine(jsonElement.getAsString()));
            }
        });
        tryToLoad("Notifications", () -> {
            if (containsKey(skilltreeObject, "Notifications")) {
                JsonObject notificationsObject = get(skilltreeObject, "Notifications").getAsJsonObject();
                for (String levelRuleString : notificationsObject.keySet()) {
                    tryToLoad("Notification." + levelRuleString, () -> {
                        LevelRule levelRule = loadLevelRule(levelRuleString);
                        String message = notificationsObject.get(levelRuleString).getAsString();
                        skilltree.addNotification(levelRule, message);
                    });
                }
            }
        });
        tryToLoad("Requirements", () -> {
            if (containsKey(skilltreeObject, "Requirements")) {
                JsonArray requirementsArray = get(skilltreeObject, "Requirements").getAsJsonArray();
                requirementsArray.forEach(jsonElement -> {
                    boolean hasParameter = jsonElement.getAsString().contains(":");
                    String[] data = jsonElement.getAsString().split(":", 2);
                    Settings settings = new Settings(data[0]);
                    if (hasParameter) {
                        tryToLoad("Requirement." + jsonElement.getAsString(), () -> settings.load(data[1]));
                    }
                    skilltree.addRequirementSettings(settings);
                });
            }
        });
        tryToLoad("Skills", () -> {
            if (containsKey(skilltreeObject, "Skills")) {
                JsonObject skillsObject = get(skilltreeObject, "Skills").getAsJsonObject();
                for (String skillName : skillsObject.keySet()) {
                    JsonObject skillObject = skillsObject.getAsJsonObject(skillName);

                    tryToLoad("Skills." + skillName + ".Upgrades", () -> {
                        if (containsKey(skillObject, "Upgrades")) {
                            JsonObject upgradesObject = get(skillObject, "Upgrades").getAsJsonObject();

                            for (String levelRuleString : upgradesObject.keySet()) {
                                tryToLoad("Skills." + skillName + ".Upgrades." + levelRuleString, () -> {
                                    LevelRule levelRule = loadLevelRule(levelRuleString);

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
                        }
                    });
                }
            }
        });

        MyPetApi.getSkilltreeManager().registerSkilltree(skilltree);
    }

    private static LevelRule loadLevelRule(String levelRuleString) {
        LevelRule levelRule;
        if (levelRuleString.contains("%")) {
            int modulo = 1;
            int min = 0;
            int max = 0;
            Matcher matcher = LEVEL_RULE_REGEX.matcher(levelRuleString);
            while (matcher.find()) {
                if (matcher.group(0).startsWith("%")) {
                    modulo = Math.max(1, Integer.parseInt(matcher.group(1)));
                } else if (matcher.group(0).startsWith(">")) {
                    min = Integer.parseInt(matcher.group(3));
                } else if (matcher.group(0).startsWith("<")) {
                    max = Integer.parseInt(matcher.group(2));
                }
            }
            levelRule = new DynamicLevelRule(modulo, min, max);
        } else {
            String[] levelStrings = levelRuleString.split(",");
            List<Integer> levels = new ArrayList<>();
            for (String levelString : levelStrings) {
                if (Util.isInt(levelString.trim())) {
                    levels.add(Integer.parseInt(levelString.trim()));
                }
            }
            levelRule = new StaticLevelRule(levels);
        }
        return levelRule;
    }

    private static Upgrade<?> loadUpgrade(String skillName, JsonObject upgradeObject) {
        UpgradeParser<?> parser = MyPetApi.getSkillManager().getUpgradeParser(skillName);
        if (parser == null) {
            return null;
        }
        return parser.parse(upgradeObject);
    }

    private static JsonObject loadJsonObject(File jsonFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(jsonFile.toPath()), StandardCharsets.UTF_8))) {
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    private static boolean containsKey(JsonObject o, String key) {
        if (o != null) {
            for (String keyObject : o.keySet()) {
                if (keyObject.equalsIgnoreCase(key)) {
                    return true;
                }
            }
        }
        return false;
    }

}