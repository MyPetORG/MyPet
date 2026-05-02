package de.Keyle.MyPet.api.skill;

import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

@FunctionalInterface
public interface UpgradeParser<S extends Skill> {

    /**
     * Parses an {@link Upgrade} for skill {@code S} from a skilltree JSON node.
     *
     * <p>Called by the skilltree loader for each {@code Skills.<name>.Upgrades.<level>}
     * block in an {@code .st.json} file. The {@code upgradeJson} object is the JSON
     * object directly under the level rule (e.g. {@code {"damage": "+5"}}).
     *
     * <p>Return {@code null} if the JSON cannot be parsed into a valid upgrade —
     * the loader will log and skip the entry. Returning an upgrade with no
     * modifiers set (e.g. all fields absent from the JSON) is also valid; the
     * loader will install it as a no-op upgrade for that level.
     */
    Upgrade<S> parse(JsonObject upgradeJson);
}
