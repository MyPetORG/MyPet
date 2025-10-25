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

package de.Keyle.MyPet.api.player;

import com.google.common.collect.BiMap;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import de.keyle.knbt.TagBase;
import de.keyle.knbt.TagCompound;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public interface MyPetPlayer extends Scheduler, NBTStorage {

    String getName();

    boolean hasCustomData();

    // Custom Data -----------------------------------------------------------------

    void setAutoRespawnEnabled(boolean flag);

    boolean hasAutoRespawnEnabled();

    void setAutoRespawnMin(int value);

    int getAutoRespawnMin();

    float getPetLivingSoundVolume();

    void setPetLivingSoundVolume(float volume);

    boolean isHealthBarActive();

    void setHealthBarActive(boolean showHealthBar);

    boolean isCaptureHelperActive();

    void setCaptureHelperActive(boolean captureHelperMode);

    void setMyPetForWorldGroup(String worldGroup, UUID myPetUUID);

    void setMyPetForWorldGroup(WorldGroup worldGroup, UUID myPetUUID);

    UUID getMyPetForWorldGroup(String worldGroup);

    UUID getMyPetForWorldGroup(WorldGroup worldGroup);

    BiMap<String, UUID> getMyPetsForWorldGroups();

    String getWorldGroupForMyPet(UUID petUUID);

    boolean hasMyPetInWorldGroup(String worldGroup);

    boolean hasMyPetInWorldGroup(WorldGroup worldGroup);

    void setExtendedInfo(TagCompound compound);

    void addExtendedInfo(String key, TagBase tag);

    Optional<TagBase> getExtendedInfo(String key);

    TagCompound getExtendedInfo();

    // -----------------------------------------------------------------------------

    boolean isOnline();

    UUID getPlayerUUID();

    UUID getInternalUUID();

    UUID getOfflineUUID();

    UUID getMojangUUID();

    String getLanguage();

    boolean isMyPetAdmin();

    boolean hasMyPet();

    MyPet getMyPet();

    Player getPlayer();

    void sendMessage(String message);

    boolean sendMessage(String message, int cooldown);

    DonateCheck.DonationRank getDonationRank();

    void checkForDonation();
}