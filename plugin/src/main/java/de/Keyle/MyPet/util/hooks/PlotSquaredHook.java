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

package de.Keyle.MyPet.util.hooks;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.MountInsideHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.*;

@PluginHookName("PlotSquared")
public class PlotSquaredHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook, MountInsideHook {

    @Override
    public boolean onEnable() {
        try {
            Class.forName("com.plotsquared.core.plot.Plot");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            Location dloc = BukkitUtil.adapt(attacker.getLocation());
            Location vloc = BukkitUtil.adapt(defender.getLocation());

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

            PlotPlayer<?> plotPlayer = BukkitUtil.adapt(attacker);

            if (defender instanceof Hanging) {
                if (plot != null && (plot.getFlag(HangingBreakFlag.class) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return plotPlayer.hasPermission("plots.admin.destroy." + stub);
            } else if (defender.getEntityId() == 30) {
                if (plot != null && (plot.getFlag(MiscBreakFlag.class) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return plotPlayer.hasPermission("plots.admin.destroy." + stub);
            } else if (defender instanceof Monster || defender instanceof EnderDragon) {
                if (plot != null && (plot.getFlag(HostileAttackFlag.class) || plot.getFlag(PveFlag.class) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return plotPlayer.hasPermission("plots.admin.pve." + stub);
            } else if (defender instanceof Tameable) {
                if (plot != null && (plot.getFlag(TamedAttackFlag.class) || plot.getFlag(PveFlag.class) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return plotPlayer.hasPermission("plots.admin.pve." + stub);
            } else if (defender instanceof Player) {
                if (plot != null) {
                    return plot.getFlag(PvpFlag.class) || plotPlayer.hasPermission("plots.admin.pvp." + stub);
                }
                return plotPlayer.hasPermission("plots.admin.pvp." + stub);
            } else if (defender instanceof Creature) {
                if (plot != null && (plot.getFlag(AnimalAttackFlag.class) || plot.getFlag(PveFlag.class) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return plotPlayer.hasPermission("plots.admin.pve." + stub);
            } else {
                if (plot != null && (plot.getFlag(PveFlag.class) || plot.isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                return plotPlayer.hasPermission("plots.admin.pve." + stub);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean playerCanMount(MyPetPlayer player, Entity pet) {
        try {
            Location loc = BukkitUtil.adapt(pet.getLocation());
            PlotArea area = loc.getPlotArea();

            if (area == null) {
                return true;
            }

            Plot plot = area.getPlot(loc);

            if (plot == null) {
                return true;
            }

            return plot.isAdded(player.getUniqueId());
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        return canHurt(attacker, (Entity) defender);
    }
}
