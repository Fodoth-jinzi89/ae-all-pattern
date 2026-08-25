package io.github.langqi99.aeallpattern.recipe;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Stable identity used to diff virtual patterns across recipe reloads. */
public record RecipeFingerprint(
        String adapterId,
        String recipeId,
        String normalizedInputs,
        String normalizedOutputs,
        int schemaVersion) {

    public RecipeFingerprint {
        adapterId = requireText(adapterId, "adapterId");
        recipeId = requireText(recipeId, "recipeId");
        normalizedInputs = Objects.requireNonNull(normalizedInputs, "normalizedInputs");
        normalizedOutputs = Objects.requireNonNull(normalizedOutputs, "normalizedOutputs");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
    }

    public String stableKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateField(digest, adapterId);
            updateField(digest, recipeId);
            updateField(digest, normalizedInputs);
            updateField(digest, normalizedOutputs);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(schemaVersion).array());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }

    private static void updateField(MessageDigest digest, String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(encoded.length).array());
        digest.update(encoded);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
