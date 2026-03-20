# Quickstart: Usable Salesforce Integration-Test Emulator

## Goals

- Start the Spring Boot emulator and dashboard on `http://localhost:8080`.
- Build and validate the app feature-by-feature, with backend and frontend moving together.
- Use `POST /reset` and `dev20` parity checks to verify supported behavior while the app grows.

## Local Development

### Backend

```bash
sdk env install
mvn -pl service spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Local URLs

- Service base URL: `http://localhost:8080`
- Dashboard URL: `http://localhost:5173` during Vite dev, or `http://localhost:8080/` when using built assets
- API version focus: `v60.0`

## Reset-First Workflow

1. Start the backend and frontend locally.
2. Call `POST /reset` before an independent verification run.
3. Exercise the active feature slice through API calls and the dashboard.
4. Inspect request and response details in the dashboard.
5. Call `POST /reset` again before the next independent run if the previous flow mutated state.

## Baseline Verification Commands

```bash
curl -X POST http://localhost:8080/reset
curl "http://localhost:8080/services/data/v60.0/query?q=SELECT%20Id,%20Name%20FROM%20Account"
curl http://localhost:8080/services/data/
```

## `dev20` Parity Workflow

1. Reset the local emulator and run the supported local workflow for the active feature.
2. Run the equivalent request sequence against `dev20`.
3. Compare:
   - request path and method
   - status code
   - top-level response shape
   - key fields
   - supported error envelope shape
4. If parity requires temporary Salesforce records, prefix them clearly and clean them up immediately after the check.
5. Record accepted deltas before marking the feature slice complete.

## Current Non-Goals

- Docker packaging
- CI automation
- deployment workflows

Those concerns are intentionally deferred until the core app behavior is complete.
