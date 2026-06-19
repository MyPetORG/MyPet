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

package de.Keyle.MyPet.webeditor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent trust store for browser public keys, keyed by the owning player's
 * UUID — so each admin has their own set of trusted browsers.
 *
 * <p>We store only the SHA-256 fingerprint of each trusted key, never the key
 * itself. A browser proves trust once via {@code /mypet editor trust <code>};
 * after that its key fingerprint is remembered and future sessions are accepted
 * without re-authorising.
 *
 * <p>Trust logic and fingerprinting are pure (no I/O, no Gson) and thread-safe.
 * Disk persistence is intentionally kept outside this class: {@link #exportState()}
 * / {@link #importState(Map)} round-trip a plain {@code Map<String,List<String>>}
 * that the plugin serialises to {@code plugins/MyPet/editor-keys.json} with Gson.
 */
public final class WebEditorKeystore {

    /** A pending, not-yet-confirmed trust attempt, indexed by nonce. */
    public record PendingAttempt(UUID owner, String publicKeyBase64) {
    }

    /** owner UUID → set of trusted key fingerprints (base64 SHA-256). */
    private final Map<UUID, Set<String>> trusted = new ConcurrentHashMap<>();

    /** nonce → pending attempt awaiting an in-game `trust` confirmation. */
    private final Map<String, PendingAttempt> attempts = new ConcurrentHashMap<>();

    /** SHA-256 fingerprint (base64) of a base64 SPKI public key. */
    public static String fingerprint(String publicKeyBase64) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Base64.getDecoder().decode(publicKeyBase64));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** True if this player has already trusted the given browser key. */
    public boolean isTrusted(UUID owner, String publicKeyBase64) {
        Set<String> fingerprints = trusted.get(owner);
        return fingerprints != null && fingerprints.contains(fingerprint(publicKeyBase64));
    }

    /** Record a connection awaiting trust, indexed by its hello nonce. */
    public void beginTrustAttempt(String nonce, UUID owner, String publicKeyBase64) {
        attempts.put(nonce, new PendingAttempt(owner, publicKeyBase64));
    }

    /**
     * Confirm a pending attempt by nonce (the admin ran {@code trust <nonce>}).
     * Persists the key fingerprint as trusted and returns the owning UUID, or
     * empty if the nonce is unknown/expired.
     */
    public Optional<UUID> confirmTrust(String nonce) {
        PendingAttempt attempt = attempts.remove(nonce);
        if (attempt == null) {
            return Optional.empty();
        }
        trusted.computeIfAbsent(attempt.owner(), k -> ConcurrentHashMap.newKeySet())
                .add(fingerprint(attempt.publicKeyBase64()));
        return Optional.of(attempt.owner());
    }

    /** Drop a pending attempt (e.g. the socket closed before trust). */
    public void cancelAttempt(String nonce) {
        attempts.remove(nonce);
    }

    /** Snapshot of trusted fingerprints as a plain map (UUID string → hashes) for persistence. */
    public Map<String, List<String>> exportState() {
        Map<String, List<String>> out = new ConcurrentHashMap<>();
        trusted.forEach((owner, fingerprints) -> out.put(owner.toString(), new ArrayList<>(fingerprints)));
        return out;
    }

    /** Replace the trusted set from a persisted plain map. Pending attempts are not persisted. */
    public void importState(Map<String, List<String>> state) {
        trusted.clear();
        state.forEach((owner, fingerprints) -> {
            Set<String> set = ConcurrentHashMap.newKeySet();
            set.addAll(fingerprints);
            trusted.put(UUID.fromString(owner), set);
        });
    }
}
