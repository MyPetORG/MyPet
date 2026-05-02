package de.Keyle.MyPet.api.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.Keyle.MyPet.api.skill.modifier.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeEnumModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeIntegerModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeNumberModifier;

import java.math.BigDecimal;

/**
 * Helpers for {@link UpgradeParser} implementations. Each {@code parseXxx}
 * method takes a {@link JsonElement} from an upgrade JSON node and returns the
 * corresponding modifier, or {@code null} if the element is absent or has the
 * wrong shape.
 */
public final class UpgradeParsers {

    private UpgradeParsers() {
    }

    /**
     * Looks up a key in a JSON object, ignoring case. Returns {@code null} if
     * the object is {@code null} or no key matches.
     */
    public static JsonElement get(JsonObject o, String key) {
        if (o != null) {
            for (String objectKey : o.keySet()) {
                if (objectKey.equalsIgnoreCase(key)) {
                    return o.get(objectKey);
                }
            }
        }
        return null;
    }

    public static UpgradeNumberModifier parseNumber(JsonElement modifier) {
        if (modifier instanceof JsonPrimitive p && p.isString()) {
            String s = p.getAsString();
            UpgradeNumberModifier.Type type;
            if (s.startsWith("+")) {
                type = UpgradeNumberModifier.Type.Add;
            } else if (s.startsWith("-")) {
                type = UpgradeNumberModifier.Type.Subtract;
            } else {
                return null;
            }
            BigDecimal value = new BigDecimal(s.substring(1));
            return new UpgradeNumberModifier(value, type);
        }
        return null;
    }

    public static UpgradeIntegerModifier parseInteger(JsonElement modifier) {
        if (modifier instanceof JsonPrimitive p && p.isString()) {
            String s = p.getAsString();
            UpgradeNumberModifier.Type type;
            if (s.startsWith("+")) {
                type = UpgradeNumberModifier.Type.Add;
            } else if (s.startsWith("-")) {
                type = UpgradeNumberModifier.Type.Subtract;
            } else {
                return null;
            }
            BigDecimal value = new BigDecimal(s.substring(1));
            return new UpgradeIntegerModifier(value.intValue(), type);
        }
        return null;
    }

    public static UpgradeBooleanModifier parseBoolean(JsonElement modifier) {
        if (modifier instanceof JsonPrimitive p && p.isBoolean()) {
            return p.getAsBoolean() ? UpgradeBooleanModifier.True : UpgradeBooleanModifier.False;
        }
        return null;
    }

    public static <T extends Enum<T>> UpgradeEnumModifier<T> parseEnum(JsonElement modifier, Class<T> enumClass) {
        if (modifier instanceof JsonPrimitive p && p.isString()) {
            String s = p.getAsString();
            for (T constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(s)) {
                    return new UpgradeEnumModifier<>(constant);
                }
            }
        }
        return null;
    }
}
