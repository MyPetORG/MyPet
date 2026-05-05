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

    /**
     * Parses a JSON string element into a numeric upgrade modifier. Expects a string
     * prefixed with {@code "+"} (add) or {@code "-"} (subtract) followed by a decimal
     * number (e.g. {@code "+5"}, {@code "-2.5"}).
     *
     * @param modifier the JSON element to parse
     * @return the modifier, or {@code null} if the element is not a prefixed numeric string
     */
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

    /**
     * Parses a JSON string element into an integer upgrade modifier. Same format as
     * {@link #parseNumber(JsonElement)} but truncates the parsed value to an {@code int}.
     *
     * @param modifier the JSON element to parse
     * @return the modifier, or {@code null} if the element is not a prefixed numeric string
     */
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

    /**
     * Parses a JSON boolean primitive into a boolean upgrade modifier.
     *
     * @param modifier the JSON element to parse
     * @return {@link UpgradeBooleanModifier#True} or {@link UpgradeBooleanModifier#False},
     *         or {@code null} if the element is not a boolean primitive
     */
    public static UpgradeBooleanModifier parseBoolean(JsonElement modifier) {
        if (modifier instanceof JsonPrimitive p && p.isBoolean()) {
            return p.getAsBoolean() ? UpgradeBooleanModifier.True : UpgradeBooleanModifier.False;
        }
        return null;
    }

    /**
     * Parses a JSON string into an enum upgrade modifier by matching the string
     * (case-insensitive) against the constants of the given enum class.
     *
     * @param modifier  the JSON element to parse
     * @param enumClass the enum type to match against
     * @param <T>       the enum type
     * @return the matching modifier, or {@code null} if no constant matches or the
     *         element is not a string
     */
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
