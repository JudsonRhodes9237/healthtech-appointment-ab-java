package example;

public final class AppointmentDecisionTest {
    public static void main(String[] args) {
        AppointmentDecision decision = new AppointmentDecision();
        AppointmentDecision.NotificationPlan first = decision.plan("patient-123", "visit-7", "on", 50);
        AppointmentDecision.NotificationPlan second = decision.plan("patient-123", "visit-8", "on", 50);
        if (!first.variant().equals(second.variant())) throw new AssertionError("assignment moved for the same user");
        if (!first.message().contains("visit-7") || first.message().toLowerCase().contains("diagnosis")) {
            throw new AssertionError("notification is not patient-safe");
        }
        System.out.println("AppointmentDecisionTest passed: stable assignment and safe reminder");
    }
}
