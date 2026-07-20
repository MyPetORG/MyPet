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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Discovers and registers MyPet's built-in skills by scanning this package for
 * concrete {@link Skill} implementations — adding a skill class is the whole
 * registration step.
 */
public final class BuiltInSkills {

    private static final String SKILLS_PACKAGE_PATH = "de/Keyle/MyPet/skill/skills/";

    private BuiltInSkills() {
    }

    public static void register() {
        List<Class<? extends Skill>> discovered = discoverSkillClasses();
        if (discovered.isEmpty()) {
            throw new IllegalStateException("No built-in skill classes discovered in " + SKILLS_PACKAGE_PATH
                    + " — plugin jar scan failed");
        }
        for (Class<? extends Skill> skill : discovered) {
            MyPetApi.getSkillManager().registerSkill(skill);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Skill>> discoverSkillClasses() {
        List<Class<? extends Skill>> classes = new ArrayList<>();
        try {
            File jarFile = new File(MyPetApi.getPlugin().getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) {
                throw new IllegalStateException("MyPet is not running from a jar; cannot scan for built-in skills");
            }
            ClassLoader classLoader = MyPetApi.getPlugin().getClass().getClassLoader();
            List<String> classNames = new ArrayList<>();
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> jarEntries = jar.entries();
                while (jarEntries.hasMoreElements()) {
                    String name = jarEntries.nextElement().getName();
                    if (!name.startsWith(SKILLS_PACKAGE_PATH) || !name.endsWith(".class") || name.contains("$")) {
                        continue;
                    }
                    classNames.add(name.replace('/', '.').replace(".class", ""));
                }
            }
            Collections.sort(classNames);
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className, false, classLoader);
                if (Skill.class.isAssignableFrom(clazz)
                        && !clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers())) {
                    classes.add((Class<? extends Skill>) clazz);
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan for built-in skills", e);
        }
        return classes;
    }
}
