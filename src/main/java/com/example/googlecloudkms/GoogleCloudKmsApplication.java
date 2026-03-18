package com.example.googlecloudkms;

import com.google.cloud.kms.v1.*;
import com.google.protobuf.ByteString;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class GoogleCloudKmsApplication {

    public static void main(String[] args) throws Exception {
        // ====== Configuration ======
        String projectId = "project-30cdd8b7-4ff7-4d16-a0b";  // Your GCP project ID
        String locationId = "global";                           // KMS location
        String keyRingId = "my-key-ring";
        String keyId = "test-asymmetric-key";
        String message = "hello world";                         // Message to sign

        try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {

            // ====== 1️⃣ Create KeyRing if it doesn't exist ======
            KeyRingName keyRingName = KeyRingName.of(projectId, locationId, keyRingId);
            try {
                client.getKeyRing(keyRingName);
                System.out.println("KeyRing exists: " + keyRingId);
            } catch (Exception e) {
                client.createKeyRing(LocationName.of(projectId, locationId), keyRingId, KeyRing.newBuilder().build());
                System.out.println("Created KeyRing: " + keyRingId);
            }

            // ====== 2️⃣ Create CryptoKey if it doesn't exist ======
            CryptoKey cryptoKey;
            try {
                cryptoKey = client.getCryptoKey(CryptoKeyName.of(projectId, locationId, keyRingId, keyId));
                System.out.println("Key exists: " + keyId);
            } catch (Exception e) {
                cryptoKey = CryptoKey.newBuilder()
                        .setPurpose(CryptoKey.CryptoKeyPurpose.ASYMMETRIC_SIGN)
                        .setVersionTemplate(
                                CryptoKeyVersionTemplate.newBuilder()
                                        .setAlgorithm(CryptoKeyVersion.CryptoKeyVersionAlgorithm.RSA_SIGN_PKCS1_2048_SHA256)
                                        .build()
                        )
                        .build();
                cryptoKey = client.createCryptoKey(keyRingName, keyId, cryptoKey);
                System.out.println("Created key: " + cryptoKey.getName());
            }

            // ====== 3️⃣ Get first key version ======
            CryptoKeyVersionName versionName = CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, "1");

            // ====== 4️⃣ Fetch public key and save to file ======
            com.google.cloud.kms.v1.PublicKey kmsPublicKey = client.getPublicKey(versionName);
            Files.write(Paths.get("pubkey.pem"), kmsPublicKey.getPem().getBytes());
            System.out.println("Saved public key to pubkey.pem");

            // ====== 5️⃣ Sign the message ======
            byte[] messageBytes = message.getBytes();
            Digest digest = Digest.newBuilder()
                    .setSha256(ByteString.copyFrom(java.security.MessageDigest.getInstance("SHA-256").digest(messageBytes)))
                    .build();

            AsymmetricSignResponse signResponse = client.asymmetricSign(versionName, digest);
            byte[] signatureBytes = signResponse.getSignature().toByteArray();
            Files.write(Paths.get("sig.bin"), signatureBytes);
            System.out.println("Saved signature to sig.bin");
            System.out.println("Signature (base64): " + Base64.getEncoder().encodeToString(signatureBytes));

            // ====== 6️⃣ Verify signature locally using SHA256withRSA ======
            String pubKeyPem = new String(Files.readAllBytes(Paths.get("pubkey.pem")))
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyPem);
            PublicKey rsaPubKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            Signature sig = Signature.getInstance("SHA256withRSA"); // Correct for KMS
            sig.initVerify(rsaPubKey);
            sig.update(messageBytes); // Original message
            boolean verified = sig.verify(signatureBytes);

            System.out.println("Signature verified: " + verified);
        }
    }
}