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

/*
 * This file is part of mypet-api_main
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api_main is licensed under the GNU Lesser General Public License.
 *
 * mypet-api_main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api_main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.animation;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.location.LocationHolder;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public abstract class Animation {
    protected int framesPerTick = 1;
    protected int frame = 0;
    protected int length;
    protected int loops = 0;
    protected int tickRate = 1;
    protected LocationHolder locationHolder;
    ScheduledTask task = null;

    public Animation(int length, LocationHolder locationHolder) {
        this.length = length;
        this.locationHolder = locationHolder;
    }

    public abstract void tick(int frame, Location location);

    public void reset() {
        frame = 0;
    }

    public int getFramesPerTick() {
        return framesPerTick;
    }

    public void setFramesPerTick(int framesPerTick) {
        this.framesPerTick = Math.max(1, framesPerTick);
    }

    public void once() {
        if (!running()) {
            task = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(MyPetApi.getPlugin(), t -> {
                if (locationHolder.isValid()) {
                    Location loc = locationHolder.getLocation();
                    Bukkit.getServer().getRegionScheduler().execute(MyPetApi.getPlugin(), loc, () -> {
                        for (int i = 0; i < framesPerTick; i++) {
                            tick(frame, loc);
                            if (++frame >= length) {
                                stop();
                                break;
                            }
                        }
                    });
                } else {
                    stop();
                }
            }, 1L, tickRate);
        }
    }

    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
            task = null;
            onStop();
        }
    }

    public boolean running() {
        return task != null;
    }

    public void loop() {
        if (!running()) {
            task = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(MyPetApi.getPlugin(), t -> {
                if (locationHolder.isValid()) {
                    Location loc = locationHolder.getLocation();
                    Bukkit.getServer().getRegionScheduler().execute(MyPetApi.getPlugin(), loc, () -> {
                        for (int i = 0; i < framesPerTick; i++) {
                            tick(frame, loc);
                            if (++frame >= length) {
                                reset();
                                break;
                            }
                        }
                    });
                } else {
                    stop();
                }
            }, 1L, tickRate);
        }
    }

    public void loop(int quantity) {
        if (!running()) {
            this.loops = quantity;
            task = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(MyPetApi.getPlugin(), t -> {
                if (locationHolder.isValid()) {
                    Location loc = locationHolder.getLocation();
                    Bukkit.getServer().getRegionScheduler().execute(MyPetApi.getPlugin(), loc, () -> {
                        for (int i = 0; i < framesPerTick; i++) {
                            tick(frame, loc);
                            if (++frame >= length) {
                                if (--Animation.this.loops > 0) {
                                    reset();
                                } else {
                                    stop();
                                }
                                break;
                            }
                        }
                    });
                } else {
                    stop();
                }
            }, 1L, tickRate);
        }
    }

    public void onStop() {
    }
}