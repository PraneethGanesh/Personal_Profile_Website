# Contact form launch

The Angular website and contact backend have separate runtimes:

`Visitor → Vercel Angular site → HTTPS Spring Boot API → Kafka → worker → SMTP inbox`

The current Docker Compose stack is a local development setup. Deploying the Angular files to Vercel does not start this stack. The backend, broker, and worker must stay online independently of your laptop.

## 1. Verify the local flow

Open Docker Desktop with Linux containers. Run these commands in your own terminal from the project root:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build -d
docker compose ps
npm.cmd start
```

Visit http://localhost:4200, submit the contact form, and inspect http://localhost:8025. With the default configuration, messages are captured in Mailpit; they do not reach a real inbox.

For automated queue/delivery checks, use PowerShell 7 in a second terminal:

```powershell
pwsh -File scripts/verify-contact.ps1
```

The script verifies a Mailpit email, rejects an invalid request, pauses the worker, queues a message, restarts the worker, and verifies delivery. If it fails, inspect `docker compose logs --tail 100 api worker kafka` locally. Do not share credentials or visitor message content from logs.

## 2. Configure real email

Set SMTP configuration in the ignored `.env` locally, or in your backend host's secret settings when deployed. Never put SMTP credentials in Angular, `public`, or Vercel's frontend configuration.

For Gmail accounts that support app passwords, enable 2-Step Verification and generate an [app password](https://support.google.com/accounts/answer/185833). Use it in place of your account password:

```dotenv
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-account@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-account@gmail.com
MAIL_TO=your-recipient@gmail.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
```

Replace every example value locally. `MAIL_FROM` must match `MAIL_USERNAME` when SMTP authentication is enabled. Other providers can use their SMTP host, port, and credentials.

Recreate the worker after changing local settings:

```powershell
docker compose up -d --force-recreate worker
```

Submit once through the form and confirm receipt in the real inbox (including spam). A `202` response confirms Kafka acceptance only. Mailpit checks do not verify real SMTP delivery.

## 3. Host the backend

Choose a host that supports the continuously running API and worker, plus persistent Kafka storage, or connect them to a separately hosted broker. The API must have a public HTTPS origin. Keep Kafka and SMTP credentials private.

The API and worker use the same `backend/Dockerfile`, with separate runtime arguments:

- API: `--spring.profiles.active=api`
- Worker: `--spring.profiles.active=worker`

Provide the backend environment variables from `.env.example`, replacing `KAFKA_BOOTSTRAP_SERVERS` with the reachable broker address if services do not share the Compose network. An authenticated hosted broker will also require its TLS/SASL configuration; the current local broker uses plaintext on its private network.

Set `CONTACT_ALLOWED_ORIGINS` on the API to exact website origins, separated by commas, such as `https://your-project.vercel.app,https://your-domain.com`. No paths, trailing slashes, or wildcard preview domains. Add specific preview origins only if they should be able to submit.

The current limiter uses the direct remote IP and ignores forwarded headers. Behind a hosting proxy this may apply one shared five-per-minute limit to all visitors. Configure trusted proxy IP handling for the selected host or enforce per-client limits at its edge before launch. Do not blindly trust arbitrary `X-Forwarded-For` headers.

## 4. Configure Vercel

Import the Angular project directory containing `package.json` and `vercel.json`.

In Vercel project settings, add a **build environment variable**:

```text
CONTACT_API_URL=https://your-backend.example.com
```

Use the real backend origin, without `/api/contact`. This URL is public browser configuration. Set it for Production and any Preview environments you want to use, then redeploy after changing it.

`vercel.json` selects `npm run build` and `dist/portfolio/browser`. Production builds generate a separate Angular configuration and fail early if the backend URL is missing, uses HTTP, or contains credentials. Local `npm start` continues to use `http://localhost:8080`.

To build production locally:

```powershell
$env:CONTACT_API_URL = 'https://your-real-backend.example.com'
npm.cmd run build
```

For a local development bundle without a hosted API, use `npm.cmd run build:local`.

## 5. Verify the public site

From the actual deployed domain, submit a message and confirm all of the following:

- The browser's Network tab shows an HTTPS request to the hosted API, not localhost.
- The API returns `202` with a submission ID and the form clears.
- The email arrives at the configured real inbox with the same submission ID.
- A second visitor network can submit successfully, including through the hosting proxy.

Do not declare real delivery working until that inbox check succeeds. Keep the backend, worker, and Kafka running after your laptop shuts down.

References: [Vercel build settings](https://vercel.com/docs/builds/configure-a-build), [Vercel environment variables](https://vercel.com/docs/environment-variables).

## Verification in the restricted editing session

- Passed: six frontend/configuration tests, strict Angular template/type checking, and Docker Compose configuration validation.
- Confirmed: production configuration generation and rejection of a missing backend URL. The temporary example URL was removed after checks.
- Blocked: Docker daemon access is denied, so the running Kafka/SMTP flow could not be exercised here.
- Blocked: Maven's required Spring Boot parent is absent from the offline cache. Backend tests have not passed here.
- Blocked: the production bundler cannot read ancestor directories in this Windows session. A complete production build has not passed here.
- Not verified: real inbox delivery, hosted API routing/CORS, or public website deployment.
