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

package de.Keyle.MyPet.util.hooks;

import com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil;
import com.github.intellectualsites.plotsquared.plot.flag.Flags;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;

@PluginHookName("PlotSquared")
public class PlotSquaredHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook {

    protected boolean isV4 = false;

    @Override
    public String getActivationMessage() {
        return !isV4 ? " (Legacy)" : "";
    }

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_PlotSquared) {
            Plugin plugin = MyPetApi.getPluginHookManager().getPluginInstance("PlotSquared").get();
            if (plugin.getDescription().getVersion().startsWith("4")) {
                isV4 = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        if (!isV4) {
            return canHurtLegacy(attacker, defender);
        }
        try {
            Location dloc = BukkitUtil.getLocation(attacker);
            Location vloc = BukkitUtil.getLocation(defender);
            PlotArea dArea = dloc.getPlotArea();
            PlotArea vArea = dArea != null && dArea.contains(vloc.getX(), vloc.getZ()) ? dArea : vloc.getPlotArea();
            if (dArea == null && vArea == null) {
                return true;
            }

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
                // Prioritize plots for close to seamless pvp zones
                if (defender.getTicksLived() > attacker.getTicksLived()) {
                    if (dplot == null || !(defender instanceof Player)) {
                        plot = vplot == null ? dplot : vplot;
                    } else {
                        plot = dplot;
                    }
                } else if (dplot == null || !(defender instanceof Player)) {
                    plot = vplot == null ? dplot : vplot;
                } else if (vplot == null) {
                    plot = dplot;
                } else {
                    plot = vplot;
                }
                stub = plot.hasOwner() ? "other" : "unowned";
            }

            PlotPlayer plotPlayer = BukkitUtil.getPlayer(attacker);
            if (defender instanceof Hanging) { // hanging
                if (plot != null && (plot.getFlag(Flags.HANGING_BREAK, false) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.destroy." + stub);
            } else if (defender.getEntityId() == 30) {
                if (plot != null && (plot.getFlag(Flags.MISC_BREAK, false) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.destroy." + stub);
            } else if (defender instanceof Monster || defender instanceof EnderDragon) { // defender is monster
                if (plot != null && (plot.getFlag(Flags.HOSTILE_ATTACK, false) || plot.getFlag(Flags.PVE, false) ||
                        plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.pve." + stub);
            } else if (defender instanceof Tameable) { // defender is tameable
                if (plot != null && (plot.getFlag(Flags.TAMED_ATTACK, false) || plot.getFlag(Flags.PVE, false) ||
                        plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.pve." + stub);
            } else if (defender instanceof Player) {
                if (plot != null) {
                    return !Flags.PVP.isFalse(plot) || Permissions.hasPermission(plotPlayer, "plots.admin.pvp." + stub);
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.pvp." + stub);
            } else if (defender instanceof Creature) { // defender is animal
                if (plot != null && (plot.getFlag(Flags.ANIMAL_ATTACK, false) || plot.getFlag(Flags.PVE, false) ||
                        plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.pve." + stub);
            } else { // defender is something else
                if (plot != null && (plot.getFlag(Flags.PVE, false) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return Permissions.hasPermission(plotPlayer, "plots.admin.pve." + stub);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    public boolean canHurtLegacy(Player attacker, Entity defender) {
        try {
            com.intellectualcrafters.plot.object.Location dloc = com.plotsquared.bukkit.util.BukkitUtil.getLocation(attacker);
            com.intellectualcrafters.plot.object.Location vloc = com.plotsquared.bukkit.util.BukkitUtil.getLocation(defender);
            com.intellectualcrafters.plot.object.PlotArea dArea = dloc.getPlotArea();
            com.intellectualcrafters.plot.object.PlotArea vArea = dArea != null && dArea.contains(vloc.getX(), vloc.getZ()) ? dArea : vloc.getPlotArea();
            if (dArea != null || vArea != null) {
                com.intellectualcrafters.plot.object.Plot dplot = dArea != null ? dArea.getPlot(dloc) : null;
                com.intellectualcrafters.plot.object.Plot vplot = vArea != null ? vArea.getPlot(vloc) : null;
                com.intellectualcrafters.plot.object.Plot plot;
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
                        plot = vplot == null ? dplot : vplot;
                    } else if (vplot == null) {
                        plot = dplot;
                    } else {
                        plot = vplot;
                    }
                    stub = plot.hasOwner() ? "other" : "unowned";
                }

                PlotPlayer plotPlayer1 = BukkitUtil.getPlayer(attacker);
                if (defender instanceof Hanging) {
                    if (plot != null && (plot.getFlag(com.intellectualcrafters.plot.flag.Flags.HANGING_BREAK, Boolean.FALSE) ||
                            plot.isAdded(plotPlayer1.getUUID()))) {
                        return true;
                    }

                    if (!Permissions.hasPermission(plotPlayer1, "plots.admin.destroy." + stub)) {
                        return false;
                    }
                } else if (defender.getEntityId() == 30) {
                    if (plot != null && (plot.getFlag(com.intellectualcrafters.plot.flag.Flags.MISC_BREAK, Boolean.FALSE) ||
                            plot.isAdded(plotPlayer1.getUUID()))) {
                        return true;
                    }
                    if (!Permissions.hasPermission(plotPlayer1, "plots.admin.destroy." + stub)) {
                        return false;
                    }
                } else if (!(defender instanceof Monster) && !(defender instanceof EnderDragon)) {
                    if (defender instanceof Tameable) {
                        if (plot != null && (plot.getFlag(com.intellectualcrafters.plot.flag.Flags.TAMED_ATTACK, Boolean.FALSE) ||
                                plot.getFlag(com.intellectualcrafters.plot.flag.Flags.PVE, Boolean.FALSE) ||
                                plot.isAdded(plotPlayer1.getUUID()))) {
                            return true;
                        }
                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                            return false;
                        }
                    } else if (defender instanceof Player) {
                        if (plot != null) {
                            return !(com.intellectualcrafters.plot.flag.Flags.PVP.isFalse(plot) &&
                                    !Permissions.hasPermission(plotPlayer1, "plots.admin.pvp." + stub));
                        }
                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pvp." + stub)) {
                            return false;
                        }
                    } else if (!(defender instanceof Creature)) {
                        if (defender instanceof Vehicle) {
                            return true;
                        }
                        if (plot != null && (plot.getFlag(com.intellectualcrafters.plot.flag.Flags.PVE, Boolean.FALSE) ||
                                plot.isAdded(plotPlayer1.getUUID()))) {
                            return true;
                        }
                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                            return false;
                        }
                    } else {
                        if (plot != null && (plot.getFlag(com.intellectualcrafters.plot.flag.Flags.ANIMAL_ATTACK, Boolean.FALSE) ||
                                plot.getFlag(com.intellectualcrafters.plot.flag.Flags.PVE, Boolean.FALSE) ||
                                plot.isAdded(plotPlayer1.getUUID()))) {
                            return true;
                        }
                        if (!Permissions.hasPermission(plotPlayer1, "plots.admin.pve." + stub)) {
                            return false;
                        }
                    }
                } else {
                    if (plot != null && (plot.getFlag(com.intellectualcrafters.plot.flag.Flags.HOSTILE_ATTACK, Boolean.FALSE) ||
                            plot.getFlag(com.intellectualcrafters.plot.flag.Flags.PVE, Boolean.FALSE) ||
                            plot.isAdded(plotPlayer1.getUUID()))) {
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