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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.intervaltree.DoubleInterval;
import de.Keyle.MyPet.api.util.intervaltree.Interval;
import de.Keyle.MyPet.api.util.intervaltree.IntervalTree;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@ServiceName("ExperienceCache")
@Load(Load.State.OnLoad)
public class ExperienceCache implements ServiceContainer {

    String calculator = null;
    long version = 0;
    JSONObject expMap = new JSONObject();
    Map<String, Map<MyPetType, IntervalTree<Double, Integer>>> intervalMap = new HashMap<>();

    File cacheFile = new File(MyPetApi.getPlugin().getDataFolder(), "exp.cache");

    public double getExp(String worldGroup, MyPetType type, int level) throws LevelNotCalculatedException {
        if (this.expMap.containsKey(worldGroup)) {
            JSONObject typeMap = (JSONObject) this.expMap.get(worldGroup);
            if (typeMap.containsKey(type.name())) {
                JSONObject expMap = (JSONObject) typeMap.get(type.name());
                if (expMap.containsKey(level)) {
                    return (double) expMap.get(level);
                }
            }
        }
        throw new LevelNotCalculatedException(type, level);
    }

    public int getLevel(String worldGroup, MyPetType type, double exp) {
        if (this.intervalMap.containsKey(worldGroup)) {
            Map<MyPetType, IntervalTree<Double, Integer>> typeIntervalMap = this.intervalMap.get(worldGroup);
            if (typeIntervalMap.containsKey(type)) {
                IntervalTree<Double, Integer> tree = new IntervalTree<>();
                for (Interval<Double, Integer> i : tree.query(exp)) {
                    if (i.getStart() != exp) {
                        return i.getValue();
                    }
                }
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public void insertExp(String worldGroup, MyPetType type, int level, double exp) {
        if (level < 1) {
            return;
        }
        if (worldGroup.isEmpty()) {
            return;
        }
        if (!expMap.containsKey(worldGroup)) {
            expMap.put(worldGroup, new JSONObject());
            intervalMap.put(worldGroup, new HashMap<>());
        }
        JSONObject typeMap = (JSONObject) this.expMap.get(worldGroup);
        Map<MyPetType, IntervalTree<Double, Integer>> typeIntervalMap = intervalMap.get(worldGroup);
        if (!typeMap.containsKey(type.name())) {
            typeMap.put(type.name(), new JSONObject());
            typeIntervalMap.put(type, new IntervalTree<>());
        }
        JSONObject expMap = (JSONObject) typeMap.get(type.name());
        IntervalTree<Double, Integer> tree = typeIntervalMap.get(type);

        expMap.put(level, exp);
        if (expMap.containsKey(level - 1)) {
            DoubleInterval<Integer> interval = (DoubleInterval<Integer>) new DoubleInterval(level - 1).builder()
                    .greaterEqual((Double) expMap.get(level - 1))
                    .less(exp).build();
            tree.add(interval);
        }
        if (expMap.containsKey(level + 1)) {
            DoubleInterval<Integer> interval = (DoubleInterval<Integer>) new DoubleInterval(level).builder()
                    .greaterEqual(exp)
                    .less((Double) expMap.get(level + 1)).build();
            tree.add(interval);
        }
    }

    @Override
    public boolean onEnable() {
        if (cacheFile.exists()) {
            load();
        }
        return true;
    }

    @Override
    public void onDisable() {
        save();
        version = 0;
        calculator = null;
        expMap.clear();
        intervalMap.clear();
    }

    public void checkVersion(ExperienceCalculator calculator) {
        long version = calculator.getVersion();
        String identifier = calculator.getIdentifier();
        if (version != this.version || !identifier.equals(this.calculator)) {
            expMap.clear();
            this.version = version;
            this.calculator = identifier;
            MyPetApi.getLogger().info("Current Exp-Cache is invalid, it will be recalculated.");
            save();
        }
    }

    @SuppressWarnings("unchecked")
    protected void save() {
        try (OutputStreamWriter oos = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(cacheFile)))) {
            JSONObject cacheObject = new JSONObject();
            cacheObject.put("expMap", expMap);
            cacheObject.put("version", version);
            cacheObject.put("calculator", calculator);
            cacheObject.writeJSONString(oos);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void load() {
        try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(cacheFile)), StandardCharsets.UTF_8)) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONObject) {
                JSONObject cacheObject = (JSONObject) obj;
                this.expMap = (JSONObject) cacheObject.get("expMap");
                this.version = (long) cacheObject.get("version");
                this.calculator = cacheObject.get("calculator").toString();
                loadIntervals();
            }
        } catch (ParseException | IOException e) {
            cacheFile.delete();
            version = 0;
            calculator = null;
            expMap.clear();
            intervalMap.clear();
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadIntervals() {
        for (Object worldGroup : this.expMap.keySet()) {
            Map<MyPetType, IntervalTree<Double, Integer>> typeIntervalMap = new HashMap<>();
            intervalMap.put(worldGroup.toString(), typeIntervalMap);
            JSONObject typeMap = (JSONObject) this.expMap.get(worldGroup);
            for (Object typeObject : typeMap.keySet()) {
                MyPetType type = MyPetType.byName(typeObject.toString());
                IntervalTree<Double, Integer> tree = new IntervalTree<>();
                typeIntervalMap.put(type, tree);
                JSONObject expMap = (JSONObject) typeMap.get(type.name());
                for (Object levelObject : expMap.keySet()) {
                    int level = Integer.parseInt(levelObject.toString());
                    double exp = (double) expMap.get("" + level);
                    if (expMap.containsKey("" + (level - 1))) {
                        DoubleInterval<Integer> interval = (DoubleInterval<Integer>) new DoubleInterval(level - 1).builder().greaterEqual((Double) expMap.get("" + (level - 1))).less(exp).build();
                        if (tree.query(interval).size() == 0) {
                            tree.add(interval);
                        }
                    }
                    if (expMap.containsKey("" + (level + 1))) {
                        DoubleInterval<Integer> interval = (DoubleInterval<Integer>) new DoubleInterval(level).builder().greaterEqual(exp).less((Double) expMap.get("" + (level + 1))).build();
                        if (tree.query(interval).size() == 0) {
                            tree.add(interval);
                        }
                    }
                }
            }
        }
    }

    public class LevelNotCalculatedException extends Exception {

        @Getter
        private final MyPetType type;
        @Getter
        private final int level;

        public LevelNotCalculatedException(MyPetType type, int level) {
            super("Exp for " + type + " at level " + level + " not yet calculated!");
            this.type = type;
            this.level = level;
        }
    }
}
