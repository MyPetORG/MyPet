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

package de.Keyle.MyPet.util.sound;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;

/**
 * Library-agnostic sound-packet logic. Returns the multiplier each
 * adapter should apply to the packet's volume field. {@code 1f} means
 * "leave alone." {@code 0f} means "cancel the packet entirely."
 *
 * <p>Applies to ALL sounds emitted by a marked pet — ambient, hurt,
 * death, step, eat, etc. — not just "living" sounds.
 */
public final class SoundPacketInterceptor {

    /** @return the volume multiplier in [0f, 1f]. */
    public float computeMultiplier(SoundPacketContext ctx) {
        if (ctx.pet() == null) {
            return 1f;
        }
        MyPetPlayer source = switch (PetSoundService.MODE) {
            case PER_OWNER -> ctx.pet().getOwner();
            case PER_PLAYER -> {
                if (ctx.recipient() == null) yield null;
                yield MyPetApi.getPlayerManager().getMyPetPlayer(ctx.recipient());
            }
        };
        if (source == null) {
            return 1f;
        }
        float multiplier = source.getPetVolume();
        if (multiplier < 0f) return 0f;
        if (multiplier > 1f) return 1f;
        return multiplier;
    }
}
