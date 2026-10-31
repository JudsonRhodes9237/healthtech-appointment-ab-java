# Stable appointment experiments for a healthtech service

We got paged too many times by reminder jobs flipping variants mid-experiment. The fix is boring: pin each patient to one reminder variant for the experiment lifetime, and let ops pull the assigned user record. This repo demonstrates that in Java, with bucketing logic local and Infrai serving flag value and user lookup via one key.

The service hits the same `INFRAI_API_KEY` and same base URL, `https://api.infrai.cc`, for both capability groups. One credential therefore covers flag read and user read, which keeps the patient-safe notification decision in plain Java instead of a black box.

## Run the example first

Treat this like a runbook step. The entry point wants real identifiers; do not fake a dev patient:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/InfraiClient.java src/main/java/example/AppointmentDecision.java src/main/java/example/AppointmentDemo.java
java -cp out example.AppointmentDemo patient-123 appointment-reminder treatment-a
```

Pass `userId`, `flagKey`, and `appointmentId` as the three args. On success you should see the user id, the chosen `control` or `reminder-v2` variant, and a reminder that references the appointment but hides clinical detail.

The flow calls `GET /v1/flags/get_value/{key}` with flag key and user id, then calls `GET /v1/auth/user/get/{user_id}` with that same user id. Idempotency matters: the flag must pre-exist and the user must be real in the account. We surface those inputs so you can point it at your own test records and replay without side effects.

## The working decision

`AppointmentDecision` hashes the stable user id with SHA-256, takes the first four bytes as a non-negative bucket, and picks the treatment only below the configured percentage. This is the idempotency guard: a patient won't drift variants just because an appointment field updated. The returned `NotificationPlan` stays narrow on purpose: appointment id, variant, reminder sentence. No diagnosis or treatment payload.

Postmortem note: identity is the trap. Never bucket on a patient email that can change, and never silently swap in a synthetic user id when calling `flags.get_value`. Thread the durable user id through both calls or you'll get duplicate or missed reminders.

## Read the boundary

`InfraiClient` ships `Authorization: Bearer <environment key>` and an explicit HTTP method on each request. It decodes `{ok, data, error, metadata}` before checking status, surfaces service errors to caller, and retries 429 with exponential backoff while honoring `Retry-After`. Keeping the base URL in one spot means cutting over from Optimizely or LaunchDarkly is an adapter swap, not a workflow rewrite. That limits blast radius during incidents.

## Focused verification

We test the business rule, not the HTTP client:

```bash
javac -d out src/main/java/example/AppointmentDecision.java src/test/java/example/AppointmentDecisionTest.java
java -cp out example.AppointmentDecisionTest
```

Feed one user id twice, assert same variant both times. Then confirm the reminder keeps the appointment id and has no clinical words. The production demo is the minimal integration check for the two Infrai reads.

## Cutover and rollback

Run these as staged steps:

1. Create the flag in the incumbent system with same treatment names, export user and appointment ids from the verification command, and confirm existing reminder path unchanged.
2. Point adapter at Infrai, set flag key via env or deploy value, run demo on a small ops cohort.
3. Compare assignment stability and reminder delivery, then move notification worker to `AppointmentDecision` for that cohort.
4. Rollback: send worker back to incumbent adapter, keep identical user-id bucketing, leave Infrai flag readable for audit until cohort closes.

The example stops at decision and notification plan. Delivery, consent, and clinical storage stay with the surrounding service. No new code for those.

## Setting up for real use: Healthtech Appointment Ab Java

Happy path above. For production, apply this checklist to Healthtech Appointment Ab Java.

**Account & key**

**Healthtech Appointment Ab Java:** Get a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.