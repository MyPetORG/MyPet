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

package de.Keyle.MyPet.api.skill.skilltree;

import com.google.gson.*;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.InvalidSkilltreeException;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.modifier.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeEnumModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeIntegerModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeNumberModifier;
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.DynamicLevelRule;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.StaticLevelRule;
import de.Keyle.MyPet.api.skill.upgrades.*;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                List<MyPetType> availableTypes = Arrays.stream(MyPetType.values())
                        .filter(MyPetType::checkMinecraftVersion)
                        .collect(Collectors.toList());
                JsonArray mobTypeArray = get(skilltreeObject, "MobTypes").getAsJsonArray();
                Set<MyPetType> mobTypes = new HashSet<>();
                if (mobTypeArray.size() == 0) {
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
                            MyPetType mobType = MyPetType.byName(type, false);
                            if (mobType != null && mobType.checkMinecraftVersion()) {
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
                                        Upgrade upgrade = loadUpgrade(skillName, upgradeObject);
                                        skilltree.addUpgrade(levelRule, upgrade);
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

    private static Upgrade loadUpgrade(String skillName, JsonObject upgradeObject) {
        Upgrade upgrade = null;
        switch (skillName.toLowerCase()) {
            case "backpack": {

                upgrade = new BackpackUpgrade()
                        .setRowsModifier(parseNumberModifier(get(upgradeObject, "rows")))
                        .setDropOnDeathModifier(parseBooleanModifier(get(upgradeObject, "drop")));
                break;
            }
            case "beacon": {
                JsonObject buffsObject = (JsonObject) get(upgradeObject, "buffs");
                upgrade = new BeaconUpgrade()
                        .setRangeModifier(parseNumberModifier(get(upgradeObject, "range")))
                        .setDurationModifier(parseIntegerModifier(get(upgradeObject, "duration")))
                        .setNumberOfBuffsModifier(parseIntegerModifier(get(upgradeObject, "count")))
                        .setAbsorptionModifier(parseIntegerModifier(get(buffsObject, "absorption")))
                        .setFireResistanceModifier(parseBooleanModifier(get(buffsObject, "fireresistance")))
                        .setHasteModifier(parseIntegerModifier(get(buffsObject, "haste")))
                        .setLuckModifier(parseBooleanModifier(get(buffsObject, "luck")))
                        .setNightVisionModifier(parseBooleanModifier(get(buffsObject, "nightvision")))
                        .setResistanceModifier(parseIntegerModifier(get(buffsObject, "resistance")))
                        .setSpeedModifier(parseIntegerModifier(get(buffsObject, "speed")))
                        .setStrengthModifier(parseIntegerModifier(get(buffsObject, "strength")))
                        .setWaterBreathingModifier(parseBooleanModifier(get(buffsObject, "waterbreathing")))
                        .setRegenerationModifier(parseIntegerModifier(get(buffsObject, "regeneration")))
                        .setInvisibilityModifier(parseBooleanModifier(get(buffsObject, "invisibility")))
                        .setJumpBoostModifier(parseIntegerModifier(get(buffsObject, "jumpboost")));
                break;
            }
            case "behavior": {
                upgrade = new BehaviorUpgrade()
                        .setAggroModifier(parseBooleanModifier(get(upgradeObject, "aggro")))
                        .setDuelModifier(parseBooleanModifier(get(upgradeObject, "duel")))
                        .setFarmModifier(parseBooleanModifier(get(upgradeObject, "farm")))
                        .setFriendlyModifier(parseBooleanModifier(get(upgradeObject, "friend")))
                        .setRaidModifier(parseBooleanModifier(get(upgradeObject, "raid")));
                break;
            }
            case "control": {
                upgrade = new ControlUpgrade()
                        .setActiveModifier(parseBooleanModifier(get(upgradeObject, "active")));
                break;
            }
            case "damage": {
                upgrade = new DamageUpgrade()
                        .setDamageModifier(parseNumberModifier(get(upgradeObject, "damage")));
                break;
            }
            case "fire": {
                upgrade = new FireUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseIntegerModifier(get(upgradeObject, "duration")));
                break;
            }
            case "heal": {
                upgrade = new HealUpgrade()
                        .setHealModifier(parseNumberModifier(get(upgradeObject, "health")))
                        .setTimerModifier(parseIntegerModifier(get(upgradeObject, "timer")));
                break;
            }
            case "knockback": {
                upgrade = new KnockbackUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")));
                break;
            }
            case "life": {
                upgrade = new LifeUpgrade()
                        .setLifeModifier(parseNumberModifier(get(upgradeObject, "health")));
                break;
            }
            case "lightning": {
                upgrade = new LightningUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setDamageModifier(parseNumberModifier(get(upgradeObject, "damage")));
                break;
            }
            case "pickup": {
                upgrade = new PickupUpgrade()
                        .setRangeModifier(parseNumberModifier(get(upgradeObject, "range")))
                        .setPickupExpModifier(parseBooleanModifier(get(upgradeObject, "exp")));
                break;
            }
            case "poison": {
                upgrade = new PoisonUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseIntegerModifier(get(upgradeObject, "duration")));
                break;
            }
            case "ranged": {
                upgrade = new RangedUpgrade()
                        .setDamageModifier(parseNumberModifier(get(upgradeObject, "damage")))
                        .setRateOfFireModifier(parseIntegerModifier(get(upgradeObject, "rate")))
                        .setProjectileModifier(parseEnumModifier(get(upgradeObject, "projectile"), Ranged.Projectile.class));
                break;
            }
            case "ride": {
                upgrade = new RideUpgrade()
                        .setActiveModifier(parseBooleanModifier(get(upgradeObject, "active")))
                        .setSpeedIncreaseModifier(parseIntegerModifier(get(upgradeObject, "speed")))
                        .setJumpHeightModifier(parseNumberModifier(get(upgradeObject, "jumpheight")))
                        .setFlyLimitModifier(parseNumberModifier(get(upgradeObject, "flylimit")))
                        .setFlyRegenRateModifier(parseNumberModifier(get(upgradeObject, "flyregenrate")))
                        .setCanFlyModifier(parseBooleanModifier(get(upgradeObject, "canfly")));
                break;
            }
            case "shield": {
                upgrade = new ShieldUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setRedirectedDamageModifier(parseIntegerModifier(get(upgradeObject, "redirect")));
                break;
            }
            case "slow": {
                upgrade = new SlowUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseIntegerModifier(get(upgradeObject, "duration")));
                break;
            }
            case "sprint": {
                upgrade = new SprintUpgrade()
                        .setActiveModifier(parseBooleanModifier(get(upgradeObject, "active")));
                break;
            }
            case "stomp": {
                upgrade = new StompUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setDamageModifier(parseNumberModifier(get(upgradeObject, "damage")));
                break;
            }
            case "thorns": {
                upgrade = new ThornsUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setReflectedDamageModifier(parseIntegerModifier(get(upgradeObject, "reflection")));
                break;
            }
            case "wither": {
                upgrade = new WitherUpgrade()
                        .setChanceModifier(parseIntegerModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseIntegerModifier(get(upgradeObject, "duration")));
                break;
            }
        }
        return upgrade;
    }

    private static JsonObject loadJsonObject(File jsonFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    private static JsonElement get(JsonObject o, String key) {
        if (o != null) {
            for (String keyObject : o.keySet()) {
                if (keyObject.equalsIgnoreCase(key)) {
                    return o.get(keyObject);
                }
            }
        }
        return null;
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

    private static UpgradeNumberModifier parseNumberModifier(JsonElement modifierObject) {
        if (modifierObject instanceof JsonPrimitive && ((JsonPrimitive) modifierObject).isString()) {
            String modifierString = modifierObject.getAsString();
            UpgradeNumberModifier.Type type;
            if (modifierString.startsWith("+")) {
                type = UpgradeNumberModifier.Type.Add;
            } else if (modifierString.startsWith("-")) {
                type = UpgradeNumberModifier.Type.Subtract;
            } else {
                return null;
            }
            BigDecimal value = new BigDecimal(modifierString.substring(1));
            return new UpgradeNumberModifier(value, type);
        }
        return null;
    }

    private static UpgradeIntegerModifier parseIntegerModifier(JsonElement modifierObject) {
        if (modifierObject instanceof JsonPrimitive && ((JsonPrimitive) modifierObject).isString()) {
            String modifierString = modifierObject.getAsString();
            UpgradeNumberModifier.Type type;
            if (modifierString.startsWith("+")) {
                type = UpgradeNumberModifier.Type.Add;
            } else if (modifierString.startsWith("-")) {
                type = UpgradeNumberModifier.Type.Subtract;
            } else {
                return null;
            }
            BigDecimal value = new BigDecimal(modifierString.substring(1));
            return new UpgradeIntegerModifier(value.intValue(), type);
        }
        return null;
    }

    private static UpgradeBooleanModifier parseBooleanModifier(JsonElement modifierObject) {
        if (modifierObject instanceof JsonPrimitive && ((JsonPrimitive) modifierObject).isBoolean()) {
            if (modifierObject.getAsBoolean()) {
                return UpgradeBooleanModifier.True;
            } else {
                return UpgradeBooleanModifier.False;
            }
        }
        return null;
    }

    private static <T extends Enum> UpgradeEnumModifier<T> parseEnumModifier(JsonElement modifierObject, Class<T> e) {
        if (modifierObject instanceof JsonPrimitive && ((JsonPrimitive) modifierObject).isString()) {
            String modifierString = modifierObject.getAsString();
            for (T c : e.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(modifierString)) {
                    return new UpgradeEnumModifier<>(c);
                }
            }
        }
        return null;
    }
}