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

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a pet deals melee damage to a target. Listeners may modify the
 * damage amount or cancel the strike entirely.
 *
 * <p>Fires from {@code PetSkillTriggerListener} after the Damage / Bleed / Fire /
 * Knockback / Lightning / Stomp / Wither chain has computed final damage but
 * before the target's {@code damage()} call. {@code getDamage()} reflects the
 * already-computed total — base attack damage plus skill modifiers.
 *
 * <p><b>Cancellable:</b> cancellation aborts both the damage application and any
 * on-hit skills queued for the strike — see {@link PetOnHitSkillEvent}, which
 * is dispatched per on-hit skill in the same flow.
 *
 * <p><b>Mutable damage:</b> {@code setDamage(d)} clamps to {@code max(0, d)} —
 * negative values can't heal the target through this hook. Setting damage to
 * {@code 0} does NOT cancel the swing; the entity damage call still runs (which
 * triggers vanilla animation). To skip the strike entirely, cancel.
 *
 * <p><b>Pet state:</b> live pet, owner online (the firing path requires the
 * owner to be tracking the pet entity).
 */
public class PetDamageEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    protected final MyPet myPet;
    protected final Entity target;
    protected double damage;
    protected boolean cancelled;

    public PetDamageEvent(MyPet myPet, Entity target, double damage) {
        this.myPet = myPet;
        this.target = target;
        this.damage = damage;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public Player getPlayer() {
        return myPet.getOwner().getPlayer();
    }

    public Entity getTarget() {
        return target;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = Math.max(0, damage);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}