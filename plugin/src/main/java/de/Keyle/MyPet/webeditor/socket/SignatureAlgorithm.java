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

package de.Keyle.MyPet.webeditor.socket;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ECDSA P-256 signing for the web-editor session protocol — the server-side
 * counterpart of the browser's Web Crypto helpers.
 *
 * <p>Two compatibility details make this interoperate with the browser:
 * <ul>
 *   <li><b>Signature format:</b> we use {@code SHA256withECDSAinP1363Format} so
 *       signatures are raw {@code r‖s} (IEEE P1363), exactly what the Web Crypto
 *       {@code ECDSA} sign/verify produces and consumes. The JDK default
 *       ({@code SHA256withECDSA}) is DER-encoded ASN.1 and would NOT verify
 *       against browser-produced signatures.</li>
 *   <li><b>Public-key encoding:</b> base64 of the X.509 {@code SubjectPublicKeyInfo}
 *       (SPKI) — the same bytes the browser's {@code exportKey("spki")} yields.</li>
 * </ul>
 *
 * <p>The plugin generates a fresh keypair per session. Public keys are exchanged
 * as base64 SPKI strings; messages are signed/verified as UTF-8.
 */
public final class SignatureAlgorithm {

    private static final String KEY_ALGORITHM = "EC";
    private static final String CURVE = "secp256r1"; // a.k.a. P-256 / prime256v1
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSAinP1363Format";

    private SignatureAlgorithm() {
    }

    /** Generate a fresh ECDSA P-256 keypair. */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(new ECGenParameterSpec(CURVE));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate ECDSA P-256 keypair", e);
        }
    }

    /** Export a public key as base64 SPKI (matches the browser's exportKey("spki")). */
    public static String exportPublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /** Import a base64 SPKI public key (as sent by the browser in {@code hello}). */
    public static PublicKey importPublicKey(String base64Spki) throws GeneralSecurityException {
        byte[] der = Base64.getDecoder().decode(base64Spki);
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
    }

    /** Sign a UTF-8 message, returning a base64 IEEE-P1363 signature. */
    public static String sign(PrivateKey privateKey, String message) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign message", e);
        }
    }

    /** Verify a base64 IEEE-P1363 signature over a UTF-8 message. Never throws. */
    public static boolean verify(PublicKey publicKey, String message, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
