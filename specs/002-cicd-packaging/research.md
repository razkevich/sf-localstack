# Research: CI/CD Packaging and Distribution

## Decision 1: Docker Registry — Docker Hub

**Decision**: Publish to Docker Hub (`razkevich/sf-localstack`)  
**Rationale**: Docker Hub is the default registry for `docker pull`; no registry URL prefix needed in commands. Free tier supports unlimited pulls for public images.  
**Alternatives considered**: GitHub Container Registry (ghcr.io) — requires `ghcr.io/razkevich/sf-localstack` prefix, less discoverable for new users.

---

## Decision 2: JAR Registry — GitHub Packages (Maven)

**Decision**: Publish fat JAR to GitHub Packages Maven registry (`maven.pkg.github.com/razkevich/sf-localstack`)  
**Rationale**: Free for public repos, zero manual approval process (unlike Maven Central). Authentication uses `GITHUB_TOKEN` already available in Actions. Artifact is immediately accessible after publish.  
**Alternatives considered**: Maven Central — requires Sonatype OSSRH account, GPG signing, manual review; not practical for rapid iteration. JitPack — auto-builds from source, not suitable for pre-built JARs.

---

## Decision 3: Multi-platform Build — QEMU + Buildx

**Decision**: Use `docker/setup-qemu-action` + `docker/setup-buildx-action` + `docker/build-push-action` with `platforms: linux/amd64,linux/arm64`  
**Rationale**: Standard GitHub Actions approach. QEMU emulation builds arm64 on amd64 runners (free tier). Build time ~10-15 min for arm64 — acceptable for release workflow.  
**Alternatives considered**: Native arm64 runners — GitHub charges for arm64 hosted runners; not free tier.

---

## Decision 4: Dockerfile Strategy — Multi-stage

**Decision**: Two-stage Dockerfile: (1) `maven:3.9-eclipse-temurin-21` build stage for Maven + Node build, (2) `eclipse-temurin:21-jre-jammy` runtime stage with only the fat JAR.  
**Rationale**: Reduces final image size by ~400 MB by excluding Maven, Node.js, and build caches. Final image ~250 MB.  
**Alternatives considered**: Single-stage — simple but bloated (~800 MB); Google Distroless — smaller but complicates debugging.

---

## Decision 5: Frontend Assets — Bundled into JAR

**Decision**: Compile the React frontend as part of the Maven build using `frontend-maven-plugin`, output static assets to `service/src/main/resources/static/`, and include them in the fat JAR.  
**Rationale**: Single artifact to deploy. Spring Boot serves static assets automatically from `classpath:/static/`.  
**Alternatives considered**: Separate nginx container for frontend — requires Docker Compose, more complex deployment; out of scope for initial packaging.

---

## Decision 6: Version Strategy — Git Tag as Source of Truth

**Decision**: Extract version from git tag (`github.ref_name` → strip `v` prefix) in the release workflow. Do NOT rely on `pom.xml` version.  
**Rationale**: Avoids version drift between pom.xml and git tags. Release workflow sets `--build-arg VERSION=${{ github.ref_name }}` and labels the image with it.  
**Alternatives considered**: Maven release plugin — complex, requires pom.xml mutations, unnecessary for this workflow.

---

## Decision 7: GitHub Actions Secrets Required

| Secret Name | Value | Where to Set |
|---|---|---|
| `DOCKERHUB_USERNAME` | `razkevich` | Repo → Settings → Secrets |
| `DOCKERHUB_TOKEN` | Docker Hub access token | Repo → Settings → Secrets |

`GITHUB_TOKEN` is auto-provided by GitHub Actions — no manual setup needed for GitHub Packages.
