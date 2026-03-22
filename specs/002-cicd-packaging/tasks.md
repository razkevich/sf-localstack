# Tasks: CI/CD Packaging and Distribution

**Input**: Design documents from `/specs/002-cicd-packaging/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, quickstart.md ✅

**Organization**: Tasks grouped by feature slice. Each slice is independently deployable and verifiable.

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Feature]**: Feature slice (F0, F1, F2, F3)

---

## Phase 1: Setup

**Purpose**: Configure secrets and verify build prerequisites before writing any workflow files.

- [x] T001 Create Docker Hub access token and add `DOCKERHUB_USERNAME` + `DOCKERHUB_TOKEN` secrets to the GitHub repo (Settings → Secrets and variables → Actions)
- [x] T002 Verify `service/pom.xml` has `<groupId>co.prodly</groupId>` and `<artifactId>sf-localstack</artifactId>` — update if needed
- [x] T003 Add `distributionManagement` block to `service/pom.xml` pointing to GitHub Packages Maven registry (`https://maven.pkg.github.com/razkevich/sf-localstack`)

**Checkpoint**: Secrets configured; pom.xml has correct coordinates and distributionManagement.

---

## Feature 0: Dockerfile (Priority: P1) 🎯

**Goal**: Multi-stage Dockerfile that produces a runnable sf-localstack image from repo root.

**Independent Test**: `docker build -t sf-localstack . && docker run --rm -p 8080:8080 sf-localstack` → `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`

### Implementation

- [x] T004 [F0] Create `Dockerfile` at repo root — multi-stage: stage 1 `maven:3.9-eclipse-temurin-21` builds the fat JAR with `mvn -pl service package -DskipTests`; stage 2 `eclipse-temurin:21-jre-jammy` copies only the JAR, sets `ENTRYPOINT ["java","-jar","/app/sf-localstack.jar"]`, exposes port 8080
- [x] T005 [P] [F0] Create `.dockerignore` at repo root excluding `target/`, `node_modules/`, `.git/`, `*.md`, `specs/`, `tmp/`

### Verification

- [x] T006 [F0] Run `docker build -t sf-localstack .` locally and confirm successful build; run container and verify `GET /actuator/health` returns 200

---

## Feature 1: CI Workflow — Build and Test on Push (Priority: P1)

**Goal**: GitHub Actions workflow that runs tests and builds a Docker image on every push to `main` and every PR.

**Independent Test**: Push a commit to `main` → observe green workflow in Actions tab; verify Docker Hub has a new `main-<sha>` image tag.

### Implementation

- [x] T007 [F1] Create `.github/workflows/ci.yml` with:
  - Trigger: `push` to `main`, `pull_request` to `main`
  - Jobs: `test` (Java 21, `mvn -pl service test`), `docker` (depends on `test`, builds and pushes `razkevich/sf-localstack:main-${{ github.sha }}` using `docker/build-push-action`)
  - Maven `.m2` cache via `actions/cache`
  - Docker Hub login using `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` secrets

### Verification

- [x] T008 [F1] Push a commit to `main` on the feature branch (via PR) and confirm: Actions workflow passes, Docker Hub shows new `main-<sha>` tag

---

## Feature 2: Release Workflow — Tag-Triggered Publish (Priority: P2)

**Goal**: On every `v*.*.*` tag push, build multi-platform Docker image, publish JAR to GitHub Packages, and create a GitHub Release.

**Independent Test**: Push tag `v0.1.0` → verify Docker Hub has `0.1.0` and `latest` tags; GitHub Packages shows `co.prodly:sf-localstack:0.1.0`; GitHub Releases page has `v0.1.0` with attached JAR.

### Implementation

- [x] T009 [F2] Create `.github/workflows/release.yml` with:
  - Trigger: `push` to tags matching `v*.*.*`
  - Job 1 `test`: Java 21, `mvn -pl service test`
  - Job 2 `docker` (depends on `test`): `docker/setup-qemu-action`, `docker/setup-buildx-action`, `docker/build-push-action` with `platforms: linux/amd64,linux/arm64`; tags `razkevich/sf-localstack:${{ github.ref_name }}` and `razkevich/sf-localstack:latest` (skip `latest` if tag contains `-` pre-release marker)
  - Job 3 `publish-jar` (depends on `test`): `mvn -pl service deploy -DskipTests` with `GITHUB_TOKEN` as server password for GitHub Packages
  - Job 4 `release` (depends on `docker` and `publish-jar`): `softprops/action-gh-release` to create GitHub Release with JAR artifact attached and auto-generated release notes
- [x] T010 [P] [F2] Add `<server>` entry with `<id>github</id>` to Maven settings in the workflow (`echo` a `settings.xml` to `~/.m2/settings.xml`) using `GITHUB_TOKEN`

### Verification

- [x] T011 [F2] Push tag `v0.1.0` → confirm Docker Hub has `0.1.0` + `latest` tags (multi-platform); GitHub Packages shows the JAR; GitHub Releases page shows `v0.1.0` with JAR attached

---

## Feature 3: README Updates (Priority: P3)

**Goal**: Update README.md Quick Start section with accurate Docker pull and Maven dependency instructions based on published artifacts.

**Independent Test**: Follow the updated README on a clean machine and reach a running service without reading source code.

### Implementation

- [x] T012 [P] [F3] Update `README.md` Quick Start section:
  - Replace source-based run instructions with `docker pull razkevich/sf-localstack:latest && docker run -p 8080:8080 razkevich/sf-localstack`
  - Add GitHub Packages Maven snippet (repository + dependency XML)
  - Add SF CLI connect command using the running container's URL
  - Keep source-based build instructions in a collapsible `<details>` section

---

## Phase Final: Polish

- [x] T013 [P] Add `CHANGELOG.md` with initial `v0.1.0` entry describing the initial release
- [x] T014 [P] Add GitHub Actions workflow status badge to README.md header (`[![CI](https://github.com/razkevich/sf-localstack/actions/workflows/ci.yml/badge.svg)](...)`)

---

## Dependency Graph

```
T001 → T003
T002 → T003
T003 → T010, T009 (job: publish-jar)
T004 → T006, T007 (job: docker), T009 (job: docker)
T005 → T004
T007 → T008
T009 → T011
T012 → (after T009 verifies image names)
```

## Parallel Opportunities

- T005 (`.dockerignore`) can be written alongside T004 (`Dockerfile`)
- T010 (Maven settings) can be authored alongside T009 (workflow file)
- T012 (README), T013 (CHANGELOG), T014 (badge) are all independent and can run in parallel
- CI (T007) and release (T009) workflows are fully independent files

## Implementation Strategy

**MVP** (Features 0 + 1): Dockerfile + CI workflow. Proves the image builds and tests pass in CI.
**Release** (Feature 2): Tag workflow. Adds publish once CI is green.
**Polish** (Feature 3): README + badge. Documentation follows working automation.
