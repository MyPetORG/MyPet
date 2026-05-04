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

import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet has a skilltree assigned. {@link Source} discriminates the
 * origin of the assignment:
 *
 * <ul>
 *   <li>{@link Source#Auto} — automatic assignment (e.g., only one tree
 *       configured for the world group, or default-tree fallback).</li>
 *   <li>{@link Source#PlayerCommand} — owner ran {@code /mypet skilltree}.</li>
 *   <li>{@link Source#AdminCommand} — admin-initiated assignment via
 *       {@code /petadmin skilltree}.</li>
 *   <li>{@link Source#AdminCreation} — admin pet creation flow assigning the
 *       initial tree.</li>
 *   <li>{@link Source#BossShopPro}, {@link Source#Shop} — third-party shop
 *       integrations.</li>
 *   <li>{@link Source#Other} — fallback for any other path.</li>
 * </ul>
 *
 * <p>Fires from {@code MyPet} on every skilltree assignment and from
 * {@code CommandOptionCreate} during admin pet creation.
 *
 * <p><b>Not cancellable:</b> the assignment has already been written when this
 * event fires. To restrict who can pick which tree, gate the command's
 * permission check; to react to the change (e.g., announce, log, refund the
 * old tree's prerequisites), listen here.
 *
 * <p><b>Pet state:</b> may be live or persisted depending on the source —
 * {@code AdminCreation} typically fires while the pet is still inactive.
 */
@Getter
public class PetSelectSkilltreeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    protected final StoredMyPet pet;
    protected final Skilltree skilltree;
    private final Source source;

    public PetSelectSkilltreeEvent(StoredMyPet pet, Skilltree skilltree, Source source) {
        this.pet = pet;
        this.skilltree = skilltree;
        this.source = source;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    public enum Source {
        Auto, PlayerCommand, AdminCommand, AdminCreation, BossShopPro, Shop, Other
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}