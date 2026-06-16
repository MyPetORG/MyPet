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

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a skilltree level-rule string (e.g. {@code "%5>3"}, {@code "1,2,3"}) into a {@link LevelRule}. */
public final class LevelRuleParser {

    private static final Pattern LEVEL_RULE_REGEX = Pattern.compile("(?:%(\\d+))|(?:<(\\d+))|(?:>(\\d+))");

    private LevelRuleParser() {
    }

    public static LevelRule parse(String levelRuleString) {
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
            levelRule = new LevelRule.Dynamic(modulo, min, max);
        } else {
            String[] levelStrings = levelRuleString.split(",");
            Set<Integer> levels = new HashSet<>();
            for (String levelString : levelStrings) {
                if (Util.isInt(levelString.trim())) {
                    levels.add(Integer.parseInt(levelString.trim()));
                }
            }
            levelRule = new LevelRule.Static(levels);
        }
        return levelRule;
    }
}
