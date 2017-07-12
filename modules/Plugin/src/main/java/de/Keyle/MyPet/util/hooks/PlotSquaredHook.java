/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.util.hooks;

import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.bukkit.listeners.PlayerEvents;
import com.plotsquared.bukkit.util.BukkitUtil;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.*;

@PluginHookName("PlotSquared")
public class PlotSquaredHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook {

    protected PlayerEvents playerEvents;

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_PlotSquared) {
            playerEvents = new PlayerEvents();
            return true;
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            com.intellectualcrafters.plot.object.Location dloc = BukkitUtil.getLocation(attacker);
            com.intellectualcrafters.plot.object.Location vloc = BukkitUtil.getLocation(defender);
            PlotArea dArea = dloc.getPlotArea();
            PlotArea vArea = dArea != null && dArea.contains(vloc.getX(), vloc.getZ()) ? dArea : vloc.getPlotArea();
            if (dArea != null || vArea != null) {
                Plot dplot = dArea != null ? dArea.getPlot(dloc) : null;
                Plot vplot = vArea != null ? vArea.getPlot(vloc) : null;
                Plot plot;
                String stub;
                if (dplot == null && vplot == null) {
                    if (dArea == null) {
                        return true;
                    }

                    plot = null;
                    stub = "road";
                } else {
                    if (defender.getTicksLived() > attacker.getTicksLived()) {
                        if (dplot != null && defender instanceof Player) {
                            plot = dplot;
                        } else if (vplot == null) {
                            plot = dplot;
                        } else {
                            plot = vplot;
                        }
                    } else if (dplot != null && defender instanceof Player) {
                        if (vplot == null) {
                            plot = dplot;
                        } else {
                            plot = vplot;
                        }
                    } else if (vplot == null) {
                        plot = dplot;
                    } else {
                        plot = vplot;
                    }

                    if (plot.hasOwner()) {
                        stub = "other";
                    } else {
                        stub = "unowned";
                    }
                }

                PlotPlayer plotPlayer1 = BukkitUtil.getPlayer(attacker);
                if (defender instanceof Hanging) {
                    if (plot != null && (plot.getFlag(Flags.HANGING_BREAK, Boolean.FALSE) || plot.isAdded(plotPlayer1.getUUID()))) {
                        return true;
                    }

                    if (!Permissions.hasPermission(plotPlayer1, "plots.admin.destroy." + stub)) {
                        return false;
                    }
                } else if (defender.getEntityId() == 30) {
                    if (plot != null && (plot.getFlag(Flags.MISC_BREAK, Boolean.FALSE) || plot.isAdded(plotPlayer1.getUUID()))) {
                        return true;
                    }

                    if (!Permissions.hasPermission(plotPlayer1, "plots.admin.destroy." + stub)) {
                        return false;
                    }
                } else if (!(defender instanceof Monster) && !(defender instanceof EnderDragon)) {
                    if (defender instanceof Tameable) {
                        if (plot != null && (plot.getFlag(Flags.TAMED_ATTACK, Boolean.FALSE) || plot.getFlag(Flags.PVE, Boolean.FALSE) || plot.isAdded(plotPlayer1.getUUID()))) {
                            return true;
                        }

                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                            return false;
                        }
                    } else if (defender instanceof Player) {
                        if (plot != null) {
                            return !(Flags.PVP.isFalse(plot) && !Permissions.hasPermission(plotPlayer1, "plots.admin.pvp." + stub));
                        }

                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pvp." + stub)) {
                            return false;
                        }
                    } else if (!(defender instanceof Creature)) {
                        if (defender instanceof Vehicle) {
                            return true;
                        }

                        if (plot != null && (plot.getFlag(Flags.PVE, Boolean.FALSE) || plot.isAdded(plotPlayer1.getUUID()))) {
                            return true;
                        }

                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                            return false;
                        }
                    } else {
                        if (plot != null && (plot.getFlag(Flags.ANIMAL_ATTACK, Boolean.FALSE) || plot.getFlag(Flags.PVE, Boolean.FALSE) || plot.isAdded(plotPlayer1.getUUID()))) {
                            return true;
                        }

                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                            return false;
                        }
                    }
                } else {
                    if (plot != null && (plot.getFlag(Flags.HOSTILE_ATTACK, Boolean.FALSE) || plot.getFlag(Flags.PVE, Boolean.FALSE) || plot.isAdded(plotPlayer1.getUUID()))) {
                        return true;
                    }

                    if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        return canHurt(attacker, (Entity) defender);
    }
}