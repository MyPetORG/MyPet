/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.UpgradeEnumModifier;
import de.Keyle.MyPet.api.skill.UpgradeNumberModifier;
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.DynamicLevelRule;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.StaticLevelRule;
import de.Keyle.MyPet.api.skill.upgrades.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Error in " + skilltreeFile.getName() + " -> Skilltree not loaded.");
                MyPetApi.getLogger().warning(e.getMessage());
            }
        }
    }

    public static void loadSkilltree(JSONObject skilltreeObject) {
        if (!containsKey(skilltreeObject, "ID")) {
            return;
        }

        Skilltree skilltree;
        String skilltreeID = get(skilltreeObject, "ID").toString();

        //if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeID)) {
        //    return;
        //}

        skilltree = new Skilltree(skilltreeID);

        if (containsKey(skilltreeObject, "Name")) {
            skilltree.setDisplayName(get(skilltreeObject, "Name").toString());
        }
        if (containsKey(skilltreeObject, "Permission")) {
            skilltree.setPermission(get(skilltreeObject, "Permission").toString());
        }
        if (containsKey(skilltreeObject, "Display")) {
            skilltree.setDisplayName(get(skilltreeObject, "Display").toString());
        }
        if (containsKey(skilltreeObject, "MaxLevel")) {
            skilltree.setMaxLevel(((Number) get(skilltreeObject, "MaxLevel")).intValue());
        }
        if (containsKey(skilltreeObject, "RequiredLevel")) {
            skilltree.setRequiredLevel(((Number) get(skilltreeObject, "RequiredLevel")).intValue());
        }
        if (containsKey(skilltreeObject, "Order")) {
            skilltree.setOrder(((Number) get(skilltreeObject, "Order")).intValue());
        }
        if (containsKey(skilltreeObject, "MobTypes")) {
            JSONArray mobTypeArray = (JSONArray) get(skilltreeObject, "MobTypes");
            Set<MyPetType> mobTypes = new HashSet<>();
            for (Object o : mobTypeArray) {
                MyPetType mobType = MyPetType.byName(o.toString());
                if (mobType != null) {
                    mobTypes.add(mobType);
                }
            }
            skilltree.setMobTypes(mobTypes);
        }
        /*
        if (containsKey(skilltreeObject, "IconItem")) {
            skillTree.setIconItem(loadIcon((JSONObject) skilltreeObject.get("IconItem")));
        }
        */
        if (containsKey(skilltreeObject, "Description")) {
            JSONArray descriptionArray = (JSONArray) get(skilltreeObject, "Description");
            for (Object lvl_object : descriptionArray) {
                skilltree.addDescriptionLine(String.valueOf(lvl_object));
            }
        }
        if (containsKey(skilltreeObject, "Skills")) {
            JSONObject skillsObject = (JSONObject) get(skilltreeObject, "Skills");
            for (Object oo : skillsObject.keySet()) {
                JSONObject skillObject = (JSONObject) skillsObject.get(oo);
                String skillName = oo.toString();

                if (containsKey(skillObject, "Upgrades")) {
                    JSONObject upgradesObject = (JSONObject) get(skillObject, "Upgrades");

                    for (Object ooo : upgradesObject.keySet()) {
                        String levelRuleString = ooo.toString();
                        LevelRule levelRule;
                        if (levelRuleString.contains("%")) {
                            int modulo = 1;
                            int min = 0;
                            int max = 0;
                            Matcher matcher = LEVEL_RULE_REGEX.matcher(levelRuleString);
                            while (matcher.find()) {
                                if (matcher.group(0).startsWith("%")) {
                                    modulo = Integer.parseInt(matcher.group(1));
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

                        JSONObject upgradeObject = (JSONObject) upgradesObject.get(ooo);
                        Upgrade upgrade = loadUpgrade(skillName, upgradeObject);

                        skilltree.addUpgrade(levelRule, upgrade);
                    }
                }
            }
        }

        MyPetApi.getSkilltreeManager().registerSkilltree(skilltree);
    }

    private static Upgrade loadUpgrade(String skillName, JSONObject upgradeObject) {
        Upgrade upgrade = null;
        switch (skillName.toLowerCase()) {
            case "backpack": {

                upgrade = new BackpackUpgrade()
                        .setRowsModifier(parseNumberModifier(get(upgradeObject, "rows")))
                        .setDropOnDeathModifier(parseBooleanModifier(get(upgradeObject, "drop")));
                break;
            }
            case "beacon": {
                JSONObject buffsObject = (JSONObject) get(upgradeObject, "buffs");
                upgrade = new BeaconUpgrade()
                        .setRangeModifier(parseNumberModifier(get(upgradeObject, "range")))
                        .setDurationModifier(parseNumberModifier(get(upgradeObject, "duration")))
                        .setNumberOfBuffsModifier(parseNumberModifier(get(upgradeObject, "count")))
                        .setAbsorptionModifier(parseNumberModifier(get(buffsObject, "absorption")))
                        .setFireResistanceModifier(parseNumberModifier(get(buffsObject, "fireresistance")))
                        .setHasteModifier(parseNumberModifier(get(buffsObject, "haste")))
                        .setLuckModifier(parseBooleanModifier(get(buffsObject, "luck")))
                        .setNightVisionModifier(parseBooleanModifier(get(buffsObject, "nightvision")))
                        .setResistanceModifier(parseNumberModifier(get(buffsObject, "resistance")))
                        .setSpeedModifier(parseNumberModifier(get(buffsObject, "speed")))
                        .setStrengthModifier(parseNumberModifier(get(buffsObject, "strength")))
                        .setWaterBreathingModifier(parseBooleanModifier(get(buffsObject, "waterbreathing")));
                break;
            }
            case "behavior": {
                upgrade = new BehaviorUpgrade()
                        .setAggroModifier(parseBooleanModifier(get(upgradeObject, "aggro")))
                        .setDuelModifier(parseBooleanModifier(get(upgradeObject, "duel")))
                        .setFarmModifier(parseBooleanModifier(get(upgradeObject, "farm")))
                        .setFriendModifier(parseBooleanModifier(get(upgradeObject, "friend")))
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
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseNumberModifier(get(upgradeObject, "duration")));
                break;
            }
            case "heal": {
                upgrade = new HealUpgrade()
                        .setIncreaseHpByModifier(parseNumberModifier(get(upgradeObject, "health")))
                        .setRegenTimeyModifier(parseNumberModifier(get(upgradeObject, "timer")));
                break;
            }
            case "knockback": {
                upgrade = new KnockbackUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")));
                break;
            }
            case "life": {
                upgrade = new LifeUpgrade()
                        .setExtraLifeModifier(parseNumberModifier(get(upgradeObject, "health")));
                break;
            }
            case "lightning": {
                upgrade = new LightningUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
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
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseNumberModifier(get(upgradeObject, "duration")));
                break;
            }
            case "ranged": {
                upgrade = new RangedUpgrade()
                        .setDamageModifier(parseNumberModifier(get(upgradeObject, "damage")))
                        .setRateOfFireModifier(parseNumberModifier(get(upgradeObject, "rate")))
                        .setProjectileModifier(parseEnumModifier(get(upgradeObject, "projectile"), Ranged.Projectile.class));
                break;
            }
            case "ride": {
                upgrade = new RideUpgrade()
                        .setSpeedIncreaseModifier(parseNumberModifier(get(upgradeObject, "speed")))
                        .setJumpHeightModifier(parseNumberModifier(get(upgradeObject, "jumpheight")))
                        .setFlyLimitModifier(parseNumberModifier(get(upgradeObject, "flylimit")))
                        .setFlyRegenRateModifier(parseNumberModifier(get(upgradeObject, "flyregenrate")))
                        .setCanFlyModifier(parseBooleanModifier(get(upgradeObject, "canfly")));
                break;
            }
            case "shield": {
                upgrade = new ShieldUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setRedirectedDamageModifier(parseNumberModifier(get(upgradeObject, "redirect")));
                break;
            }
            case "slow": {
                upgrade = new SlowUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseNumberModifier(get(upgradeObject, "duration")));
                break;
            }
            case "sprint": {
                upgrade = new SprintUpgrade()
                        .setActiveModifier(parseBooleanModifier(get(upgradeObject, "active")));
                break;
            }
            case "stomp": {
                upgrade = new StompUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setDamageModifier(parseNumberModifier(get(upgradeObject, "damage")));
                break;
            }
            case "thorns": {
                upgrade = new ThornsUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setReflectedDamageModifier(parseNumberModifier(get(upgradeObject, "reflection")));
                break;
            }
            case "wither": {
                upgrade = new WitherUpgrade()
                        .setChanceModifier(parseNumberModifier(get(upgradeObject, "chance")))
                        .setDurationModifier(parseNumberModifier(get(upgradeObject, "duration")));
                break;
            }
        }
        return upgrade;
    }

    private static JSONObject loadJsonObject(File jsonFile) throws IOException, ParseException {
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONObject) {
                return (JSONObject) obj;
            }
        }
        return null;
    }

    private static Object get(JSONObject o, String key) {
        if (o != null) {
            for (Object keyObject : o.keySet()) {
                if (keyObject.toString().equalsIgnoreCase(key)) {
                    return o.get(keyObject);
                }
            }
        }
        return null;
    }

    private static boolean containsKey(JSONObject o, String key) {
        for (Object keyObject : o.keySet()) {
            if (keyObject.toString().equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static UpgradeNumberModifier parseNumberModifier(Object modifierObject) {
        if (modifierObject instanceof String) {
            String modifierString = modifierObject.toString();
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

    private static UpgradeBooleanModifier parseBooleanModifier(Object modifierObject) {
        if (modifierObject instanceof Boolean) {
            if ((Boolean) modifierObject) {
                return UpgradeBooleanModifier.True;
            } else {
                return UpgradeBooleanModifier.False;
            }
        }
        return UpgradeBooleanModifier.DontChange;
    }

    private static <T extends Enum> UpgradeEnumModifier<T> parseEnumModifier(Object modifierObject, Class<T> e) {
        if (modifierObject instanceof String) {
            String modifierString = modifierObject.toString();
            for (T c : e.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(modifierString)) {
                    return new UpgradeEnumModifier<>(c);
                }
            }
        }
        return null;
    }
}