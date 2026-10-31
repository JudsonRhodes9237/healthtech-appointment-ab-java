package example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class AppointmentDecision {
    public record NotificationPlan(String appointmentId, String userId, String variant, String message) {}

    public NotificationPlan plan(String userId, String appointmentId, String flagValue, int treatmentPercent) {
        if (userId == null || userId.isBlank() || appointmentId == null || appointmentId.isBlank()) {
            throw new IllegalArgumentException("userId and appointmentId are required");
        }
        int bucket = bucket(userId);
        boolean treatment = "on".equalsIgnoreCase(flagValue) && bucket < treatmentPercent;
        String variant = treatment ? "reminder-v2" : "control";
        String message = "Appointment " + appointmentId + " reminder: please review your scheduled visit.";
        return new NotificationPlan(appointmentId, userId, variant, message);
    }

    private int bucket(String userId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(userId.getBytes(StandardCharsets.UTF_8));
            int value = ((digest[0] & 255) << 24) | ((digest[1] & 255) << 16) | ((digest[2] & 255) << 8) | (digest[3] & 255);
            return (value & Integer.MAX_VALUE) % 100;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}
