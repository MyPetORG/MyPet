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

import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a pet has a skilltree assigned. {@link Source} discriminates the
 * origin of the assignment:
 *
 * <ul>
 *   <li>{@link Source#AUTO} — automatic assignment (e.g., only one tree
 *       configured for the world group, or default-tree fallback).</li>
 *   <li>{@link Source#PLAYER_COMMAND} — owner ran {@code /mypet skilltree}.</li>
 *   <li>{@link Source#ADMIN_COMMAND} — admin-initiated assignment via
 *       {@code /petadmin skilltree}.</li>
 *   <li>{@link Source#ADMIN_CREATION} — admin pet creation flow assigning the
 *       initial tree.</li>
 *   <li>{@link Source#OTHER} — fallback for any other path.</li>
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
    private final StoredPet pet;
    private final Skilltree skilltree;
    private final Source source;

    public PetSelectSkilltreeEvent(StoredPet pet, Skilltree skilltree, Source source) {
        this.pet = pet;
        this.skilltree = skilltree;
        this.source = source;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public enum Source {
        AUTO, PLAYER_COMMAND, ADMIN_COMMAND, ADMIN_CREATION, SHOP, OTHER
    }
}