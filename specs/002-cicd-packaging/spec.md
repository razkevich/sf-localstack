# Feature Specification: CI/CD Packaging and Distribution

**Feature Branch**: `002-cicd-packaging`
**Created**: 2026-03-20
**Status**: Draft
**Input**: Package sf-localstack to Docker and JAR with GitHub Actions CI/CD, publishing to Docker Hub and GitHub Packages.

## Compatibility Context

- **Salesforce Surface**: N/A — this feature is about packaging and distribution, not API emulation
- **Compatibility Target**: Docker Engine 24+, Java 21+, Maven 3.9+, GitHub Actions (ubuntu-latest)
- **In-Scope Operations**:
  - Multi-platform Docker image build (linux/amd64, linux/arm64)
  - Spring Boot executable JAR (fat jar) build
  - Publish Docker image to Docker Hub (`razkevich/sf-localstack`)
  - Publish JAR to GitHub Packages Maven registry
  - GitHub Release creation with JAR artifact on git tag push
- **Out-of-Scope Operations**:
  - Maven Central publishing (requires manual approval process)
  - Helm chart or Kubernetes manifests
  - Frontend (React) container or separate image
  - Multi-module image builds (service + frontend combined image handled via single Dockerfile with embedded static assets)
- **API Shape Commitments**: N/A
- **Frontend Scope**: No dashboard changes — README updates only
- **Test Isolation Plan**: CI workflows run against the test suite before any publish step; publish only on success
- **Runtime Reproducibility Controls**: Image tags include git SHA for exact reproducibility; `latest` and semver tags updated on release
- **Parity Verification Plan**: N/A — no Salesforce API surface involved

## Feature Iterations

### Feature 0 - Dockerfile and Local Build (Priority: P1)

Add a production-ready Dockerfile that builds a self-contained sf-localstack image.
The image runs the Spring Boot JAR, exposes port 8080, and includes the frontend static assets served by the backend.

**Why this priority**: Foundation required before any CI workflow can publish an image.

**Independent Test**: Run `docker build -t sf-localstack .` locally; start with `docker run -p 8080:8080 sf-localstack`; health check responds at `GET /actuator/health`.

**Acceptance Scenarios**:

1. **Given** the repo root, **When** `docker build -t sf-localstack .` runs, **Then** the image builds without error in under 5 minutes on a standard laptop
2. **Given** a built image, **When** `docker run -p 8080:8080 sf-localstack`, **Then** `GET /actuator/health` returns `{"status":"UP"}` within 15 seconds
3. **Given** a running container, **When** `POST /reset` is called, **Then** response is 200 OK

**Frontend Deliverables**: None — static frontend assets are bundled into the JAR via Maven build

**Parity Check**: N/A

---

### Feature 1 - CI Workflow: Build and Test on Push (Priority: P1)

GitHub Actions workflow that triggers on every push to `main` and every pull request.
Runs the full Maven test suite (Java 21) and the frontend build. Fails fast on test failures.

**Why this priority**: Must be in place before any publish workflow to ensure only passing code is published.

**Independent Test**: Push a commit to `main` or open a PR; verify the Actions tab shows all jobs green.

**Acceptance Scenarios**:

1. **Given** a push to `main`, **When** the CI workflow runs, **Then** all Maven tests pass and the workflow reports success
2. **Given** a PR with a broken test, **When** the CI workflow runs, **Then** the workflow fails and blocks merge
3. **Given** a push to `main` with passing tests, **When** CI completes, **Then** the Docker image is built and pushed to Docker Hub tagged with `main-<sha>`

**Frontend Deliverables**: None

**Parity Check**: N/A

---

### Feature 2 - Release Workflow: Tag-Triggered Publish (Priority: P2)

GitHub Actions workflow triggered by semver git tags (`v*.*.*`).
Builds and pushes multi-platform Docker image (`linux/amd64`, `linux/arm64`) to Docker Hub tagged as `v1.2.3` and `latest`.
Publishes the Spring Boot fat JAR to GitHub Packages Maven registry.
Creates a GitHub Release with the JAR attached as a downloadable asset and auto-generated changelog.

**Why this priority**: Core distribution goal — enables users to `docker pull razkevich/sf-localstack` or add a Maven dependency.

**Independent Test**: Push a tag `v0.1.0`; verify Docker Hub has the new tag; verify GitHub Packages shows the artifact; verify GitHub Releases page has the release with attached JAR.

**Acceptance Scenarios**:

1. **Given** a git tag `v0.1.0` is pushed, **When** the release workflow runs, **Then** Docker Hub shows image `razkevich/sf-localstack:0.1.0` and `razkevich/sf-localstack:latest`
2. **Given** a tag push, **When** the workflow completes, **Then** GitHub Packages Maven registry shows `co.prodly:sf-localstack:0.1.0`
3. **Given** a tag push, **When** the workflow completes, **Then** a GitHub Release `v0.1.0` exists with `sf-localstack-0.1.0.jar` attached
4. **Given** a multi-platform build, **When** pulled on both `linux/amd64` and `linux/arm64`, **Then** the container starts successfully on both architectures

**Frontend Deliverables**: None

**Parity Check**: N/A

---

### Feature 3 - README Usage Instructions (Priority: P3)

Update README.md with accurate Docker and Maven usage instructions based on the published artifacts.

**Why this priority**: Users need clear onboarding instructions to actually use the published packages.

**Independent Test**: Follow README instructions on a clean machine; service starts without reading any source code.

**Acceptance Scenarios**:

1. **Given** a user reads the README, **When** they copy the `docker run` command, **Then** sf-localstack starts locally with no additional setup
2. **Given** a Java project, **When** the user adds the GitHub Packages Maven dependency from the README, **Then** the dependency resolves and the project compiles

**Frontend Deliverables**: None

**Parity Check**: N/A

---

### Edge Cases

- What happens when a tag is pushed but tests fail? → Publish step is skipped; release is not created
- What happens when Docker Hub credentials are missing or expired? → Workflow fails with a clear error; no partial push occurs
- What happens when the multi-platform build takes too long on GitHub free tier? → Use `docker/build-push-action` with QEMU emulation; arm64 build is expected to take ~10 min on free runners
- What happens when a pre-release tag (`v1.0.0-beta.1`) is pushed? → Create a GitHub pre-release; do NOT update `latest` Docker tag
- What happens when the version in `pom.xml` doesn't match the git tag? → Workflow extracts version from the tag, not pom.xml, to avoid sync issues

## Requirements

### Functional Requirements

- **FR-001**: The CI workflow MUST run Maven tests before any publish step and abort publish on test failure
- **FR-002**: The Dockerfile MUST produce a runnable image without requiring Java installed on the host
- **FR-003**: The Docker image MUST support both `linux/amd64` and `linux/arm64` platforms
- **FR-004**: The release workflow MUST be triggered exclusively by semver git tags (`v*.*.*`)
- **FR-005**: Docker Hub image tags MUST include both the exact version (`vX.Y.Z`) and `latest` (on stable releases only)
- **FR-006**: The JAR MUST be published to GitHub Packages under the `co.prodly` group ID
- **FR-007**: A GitHub Release MUST be created for every tag with the JAR as a downloadable asset
- **FR-008**: All credentials (Docker Hub token, GitHub token) MUST be stored as GitHub Actions secrets, never hardcoded
- **FR-009**: The build MUST complete within GitHub Actions free tier limits (2000 min/month for public repos = unlimited)

### Key Entities

- **Docker Image**: Multi-platform OCI image containing the Spring Boot fat JAR and exposed on port 8080
- **Fat JAR**: Self-contained executable JAR built by `spring-boot:repackage`, includes all dependencies
- **GitHub Release**: Versioned release artifact with changelog and attached JAR binary
- **GitHub Actions Workflow**: YAML-defined automation triggered by push/tag events

## Success Criteria

### Measurable Outcomes

- **SC-001**: A fresh `docker pull razkevich/sf-localstack && docker run -p 8080:8080 razkevich/sf-localstack` results in a running service with a healthy `/actuator/health` endpoint within 30 seconds
- **SC-002**: The Docker image size is under 400 MB (using multi-stage build to exclude build tools)
- **SC-003**: CI completes (test + build) in under 10 minutes on every push to `main`
- **SC-004**: The release workflow runs end-to-end (test + multi-platform build + publish + release) in under 20 minutes
- **SC-005**: 100% of pushes to `main` trigger the CI workflow with no manual intervention
- **SC-006**: Every semver tag push produces a corresponding Docker Hub tag, GitHub Package, and GitHub Release — verified by inspecting each registry after a tag push
