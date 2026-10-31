package example;

public final class AppointmentDemo {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("usage: AppointmentDemo userId flagKey appointmentId");
        String userId = args[0];
        String flagKey = args[1];
        String appointmentId = args[2];
        InfraiClient infrai = new InfraiClient(System.getenv("INFRAI_API_KEY"));
        String flagEnvelope = infrai.getFlagValue(flagKey, userId); // canonical idiom: infrai.flags.get_value
        String userEnvelope = infrai.getUser(userId); // same key and base URL resolve the bucket owner
        String flagValue = flagEnvelope.contains("\"value\":true") ? "on" : "off";
        AppointmentDecision.NotificationPlan plan = new AppointmentDecision().plan(userId, appointmentId, flagValue, 50);
        System.out.println("user=" + userId + " variant=" + plan.variant());
        System.out.println(plan.message());
        System.out.println("user lookup received=" + (userEnvelope.contains("\"ok\":true")));
    }
}
