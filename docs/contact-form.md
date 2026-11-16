# Local Kafka contact flow

Angular contact form → Spring Boot API → Kafka → Notification Service worker → fixed SMTP inbox.

Changes are confined to this Angular copy. No Git repository was present here at inspection. Nothing was committed, pushed, or deployed. The sibling `Personal-website` static site and existing DRMS Notification Service were not modified. Existing contact CSS and layout are retained; draft copying is replaced by email submission.

## Start

Requires Docker Desktop with Linux containers, a Node version supported by this Angular project, and Java 21/Maven for backend tests outside Docker.

From the Angular project root:

```powershell
# Preserve any existing local configuration.
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build -d
npm.cmd start
```

Open http://localhost:4200 and the local test inbox at http://localhost:8025. The ignored `.env` created in this session contains Mailpit defaults, with no Gmail credentials. Mailpit never sends messages to a real inbox. Allow the API and worker a few seconds to start after Kafka becomes healthy.

The image from `backend/Dockerfile` is reused for both services: `api` runs `--spring.profiles.active=api`; `worker` runs `--spring.profiles.active=worker` without an HTTP server.

```powershell
docker compose logs --tail 100 api worker
docker compose stop worker
docker compose start worker
docker compose stop api
docker compose start api
docker compose down
```

Kafka data survives `down` in a named volume. Do not add `--volumes` if queued messages should survive. This is a single-broker local setup. Messages expire after seven days and DLT records after fourteen days.

## Configuration

Edit `.env` using `.env.example`; never commit credentials. All SMTP configuration stays on the backend.

| Variable | Local default / value to supply |
| --- | --- |
| `MAIL_HOST`, `MAIL_PORT` | `mailpit`, `1025`; Gmail: `smtp.gmail.com`, `587` |
| `MAIL_USERNAME` | Empty locally; authenticated Gmail address for Gmail |
| `MAIL_PASSWORD` | Empty locally; Gmail app password for Gmail |
| `MAIL_FROM` | Test address locally; must equal `MAIL_USERNAME` when SMTP authentication is enabled |
| `MAIL_TO` | Test inbox locally; your fixed Gmail recipient for real delivery |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | `false` locally; both `true` for Gmail |
| `CONTACT_ALLOWED_ORIGINS` | Comma-separated exact origins; includes localhost and 127.0.0.1 port 4200. Append your actual deployed origin with no path/trailing slash. No deployed origin was supplied in this session. |
| `CONTACT_RATE_LIMIT` | 5 requests per remote IP per minute, including invalid requests |
| `CONTACT_MAIL_RETRIES` | 3 retries after the initial SMTP attempt |
| `CONTACT_MAIL_RETRY_DELAY_MS` | 2000 ms |
| `KAFKA_BOOTSTRAP_SERVERS` | Compose sets `kafka:9092`; JVM processes outside Compose use `localhost:9092` |
| `KAFKA_CONTACT_TOPIC` | `portfolio.contact.submitted.v1`; DLT appends `.DLT` |
| `KAFKA_GROUP_ID` | `portfolio-contact-mail-v1`; preserve across worker restarts |
| `PORT` | Optional API port inside container, default 8080; update port mapping if changed |

`src/environments/environment.ts` is local development configuration and uses `http://localhost:8080`. Production builds use `CONTACT_API_URL` to generate a separate environment file. Set it to the hosted backend's HTTPS origin (without `/api/contact`) in Vercel build settings or your terminal before `npm.cmd run build`. The build rejects missing URLs and localhost to prevent broken public deployments. See [the launch guide](deployment.md). This is public browser configuration; never place credentials in it. CORS is configured separately on the API.

For Gmail, use an app password where supported by your account. See [Google app password instructions](https://support.google.com/accounts/answer/185833) and [Google SMTP documentation](https://developers.google.com/workspace/gmail/imap/imap-smtp). After changing `.env`, recreate the worker with `docker compose up -d --force-recreate worker`. Verify receipt in the actual Gmail inbox by submission ID; Mailpit success does not verify Gmail delivery.

## Verify

```powershell
npm.cmd run build:local
npm.cmd run test:contact
# Strict Angular TypeScript/template checking without bundling:
node_modules/.bin/ngc.cmd -p tsconfig.app.json --noEmit
mvn.cmd -f backend/pom.xml test
# Default Mailpit stack must be running; requires PowerShell 7:
pwsh -File scripts/verify-contact.ps1
```

The PowerShell script checks a valid message in Mailpit, an invalid request returning 400, a message accepted while the worker is stopped, and delivery after restarting the worker. It restarts the worker in a `finally` block. Run with Mailpit configuration; this is not a Gmail test. Wait a minute if earlier submissions exhausted the rate limit.

Backend integration tests use an embedded Kafka broker and a real loopback SMTP test receiver. They cover validation/lengths/header injection, request body limits, CORS, rate limiting, waiting for broker acknowledgment, broker failures, stopped-listener queue draining, and bounded SMTP retries followed by DLT recovery. The Compose script additionally exercises a separately stopped worker container.

Manual requests:

```powershell
Invoke-WebRequest http://localhost:8080/api/contact -Method Post -ContentType application/json -Body '{"name":"Visitor","email":"visitor@example.test","message":"Hello"}'
# Expect 202 with a unique submissionId.
Invoke-WebRequest http://localhost:8080/api/contact -Method Post -ContentType application/json -Body '{"name":"","email":"bad","message":""}'
# Expect 400 (PowerShell reports the HTTP error).
```

Inspect DLT records locally when needed:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic portfolio.contact.submitted.v1.DLT --from-beginning --max-messages 1
```

DLT records contain visitor data and failure headers. Fix the cause before deliberately replaying to the original topic; automatic DLT replay is not enabled.

## Delivery guarantees

The API validates a trimmed name (1–120 characters), email (valid, up to 254), and message (1–5000); name/email reject CR/LF. Request bodies are capped at 32 KiB, including streamed bodies. Invalid inputs return 400, oversized bodies 413, rate limiting 429 with `Retry-After: 60`, and unconfirmed Kafka acceptance 503. CORS allows configured exact origins only.

Each event has `eventType=ContactMessageSubmitted`, schema version 1, a UUID submission ID, timestamp, and visitor fields. The producer uses `acks=all` and idempotence. The API returns 202 only after the Kafka send future confirms acknowledgment. A 202 means queued, not delivered.

The worker uses the fixed server-side recipient and authenticated sender; the visitor email is only `Reply-To`. `JavaMailSender.send` is synchronous. The offset commits only after SMTP acceptance. Transient failures retry a bounded number of times; exhaustion publishes the original record to `.DLT`. The recovered offset commits only after DLT publication succeeds. Authentication failures and malformed events go directly to DLT. DLT publication failure leaves the record uncommitted for another recovery attempt. Restarting a worker may restart the retry budget for an uncommitted record.

**Duplicate-email risk:** a crash after SMTP acceptance but before Kafka offset commit can redeliver the event and send another email. SMTP and Kafka do not share a transaction: this is at-least-once processing, not exactly-once email delivery. IDs appear in subjects and bodies for correlation. An API acknowledgment timeout may also occur after Kafka accepted an event; manually resubmitting generates another ID and may send another email. SMTP acceptance does not prove inbox placement.

Rate limiting uses bounded in-memory state (10,000 IP windows), applies to one API instance, and resets on restart. Client-supplied forwarded-IP headers are ignored. A future proxy deployment needs trusted proxy handling and a review of distributed rate limiting. CORS alone is not spam prevention. Jenkins, deployment, durable deduplication, and distributed limiting remain follow-up work.

## Notification Service reuse

Inspected the existing service at `C:/Users/LENOVO/Desktop/Microservices/Device_Repair_and_Management_System/notification-service`, including `Service/NotificationService.java` and its RabbitMQ consumer. Adapted its `SimpleMailMessage`/`JavaMailSender` construction into local `NotificationService.sendSingleEmail`. This flow uses synchronous dispatch and propagates exceptions so SMTP failures cannot be acknowledged as successful. Its DRMS database, recipients, Eureka discovery, and RabbitMQ routing are unnecessary here.

Offset recovery follows [Spring Kafka exception handling](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html): manual immediate acknowledgment, commit recovered records, and fail recovery when DLT publication fails.

## Verification status in this session

- PASS: three Angular form behavior tests: pending guarding, exact 202 message, preserved input after network/429/503 failures, and unexpected success handling.
- PASS: strict TypeScript and Angular template checking (`ngc --noEmit`).
- PASS: Compose configuration validation with local Mailpit defaults.
- BLOCKED: Angular production bundling. esbuild reports `Cannot read directory ../../../../..: Access is denied` resolving existing source files/styles in this restricted Windows session.
- BLOCKED: production Maven build/tests. Maven Central connections are denied and required Spring Boot 3.5.11 dependencies are absent from the cache. An auxiliary attempt with cached versions also encountered Java filesystem errors; it does not count as a successful production build or test run.
- NOT VERIFIED: live valid/invalid API requests, Kafka-to-SMTP delivery, worker stop/restart queue draining, and retry/DLT runtime behavior. Docker engine access is denied; backend integration tests could not run.
- NOT VERIFIED: real Gmail delivery. No Gmail credentials were configured or read.

## Files changed or added

Modified: `.gitignore`, `README.md`, `package.json`, `src/main.ts`, `src/app/components/contact/contact.component.ts`, `src/app/components/contact/contact.component.html`.

Added: `src/environments/environment.ts`, `compose.yaml`, `.env.example`, `docs/contact-form.md`, `scripts/contact-form.test.mjs`, `scripts/verify-contact.ps1`, `backend/pom.xml`, `backend/Dockerfile`, `backend/.dockerignore`, `backend/src/main/resources/application.yml`.

Added under `backend/src/main/java/com/portfolio/contact/`: `ContactApplication.java`, `ContactRequest.java`, `ContactMessageSubmitted.java`, `ContactController.java`, `ApiConfiguration.java`, `ContactGuard.java`, `KafkaConfiguration.java`, `NotificationService.java`, `ContactWorker.java`, `WorkerConfiguration.java`.

Added under `backend/src/test/java/com/portfolio/contact/`: `ContactApiTest.java`, `ContactFlowTest.java`.

Ignored local artifacts: `.env` (Mailpit defaults), `verification-output/`, `backend/target/`. Angular build commands may refresh ignored `.angular/` and `dist/` output.
