# Implementation Plan: CI/CD Packaging and Distribution

**Branch**: `002-cicd-packaging` | **Date**: 2026-03-20 | **Spec**: [spec.md](./spec.md)

## Summary

Add a production-ready Dockerfile and GitHub Actions CI/CD workflows to package and publish sf-localstack. On every push to `main`, run the full test suite and publish a `main-<sha>` Docker image to Docker Hub. On every semver tag (`v*.*.*`), additionally publish a versioned multi-platform image (`linux/amd64`, `linux/arm64`) as both `vX.Y.Z` and `latest`, publish the Spring Boot fat JAR to GitHub Packages Maven registry, and create a GitHub Release with the JAR attached.

## Technical Context

**Language/Version**: Java 21, YAML (GitHub Actions), Dockerfile  
**Primary Dependencies**: Spring Boot Maven plugin (`spring-boot:repackage`), `docker/build-push-action`, `docker/setup-buildx-action`, QEMU for arm64 emulation  
**Storage**: N/A — build artifacts only; GitHub Actions cache for Maven `.m2`  
**Testing**: Existing Maven test suite (`mvn test`); no new tests added (CI workflow validates the existing suite)  
**Target Platform**: GitHub Actions `ubuntu-latest` (free tier); Docker images targeting `linux/amd64` + `linux/arm64`  
**Project Type**: CI/CD pipeline + container packaging (infrastructure, not application code)  
**Performance Goals**: CI completes in < 10 min; release workflow in < 20 min  
**Constraints**: GitHub Actions free tier (public repo = unlimited minutes); Docker Hub free tier (public image, unlimited pulls)  
**Scale/Scope**: Single service module (`service/`); frontend static assets bundled into JAR via Maven build

## Constitution Check

*This feature is packaging/infrastructure — it adds no Salesforce API surface and has no parity verification requirement. Constitution principles I, II (emulator behavior), and VII (parity) are explicitly not applicable. The applicable principles are:*

- **API Fidelity**: N/A — no Salesforce surface touched
- **Test-First**: Existing test suite runs in CI before any publish step; no new emulator behavior added
- **Runtime Reproducibility**: Docker image tags include git SHA for exact reproducibility; `latest` updated only on stable semver tags
- **Dependency Surface**: New tooling limited to Docker/GitHub Actions ecosystem (no new Java dependencies); Spring Boot Maven plugin already present
- **Observability**: GitHub Actions workflow logs provide full build/publish audit trail; no emulator observability changes needed
- **Scope Control**: Strictly packaging — no REST, Bulk, or Metadata behavior changes
- **Parity Verification**: N/A

**Constitution compliance**: ✅ PASS (packaging feature, Salesforce surface gates waived per spec)

## Project Structure

### Documentation (this feature)

```text
specs/002-cicd-packaging/
├── plan.md
├── research.md
├── quickstart.md
└── tasks.md
```

### Source Code (repository root)

```text
sf-localstack/
├── Dockerfile                          # NEW — multi-stage build
├── .github/
│   └── workflows/
│       ├── ci.yml                      # NEW — test + build on push/PR
│       └── release.yml                 # NEW — publish on tag
├── service/
│   └── pom.xml                         # MODIFY — add distributionManagement for GitHub Packages
└── pom.xml                             # MODIFY — add distributionManagement (root)
```

**Structure Decision**: Workflows live in `.github/workflows/` (GitHub standard). Dockerfile at repo root for `docker build .` ergonomics. No new Maven modules introduced.

## Feature Iteration Strategy

### Feature 0: Dockerfile

- **Scope**: Multi-stage Dockerfile — build stage (Maven + Node), runtime stage (JRE 21 slim)
- **Frontend Scope**: None — frontend assets compiled in build stage and included in fat JAR via `frontend-maven-plugin` or `maven-resources-plugin`
- **Tests First**: Local `docker build` + `docker run` health check
- **Integration Verification**: `docker build -t sf-localstack . && docker run --rm -p 8080:8080 sf-localstack` → `curl http://localhost:8080/actuator/health`
- **Parity Verification**: N/A

### Feature 1: CI Workflow (push/PR)

- **Scope**: `.github/workflows/ci.yml` — checkout, Java 21, Maven test, Docker build + push `main-<sha>` tag
- **Frontend Scope**: None
- **Tests First**: Push to a test branch and verify Actions tab
- **Integration Verification**: Push commit → observe green workflow in GitHub Actions
- **Parity Verification**: N/A

### Feature 2: Release Workflow (tag-triggered)

- **Scope**: `.github/workflows/release.yml` — multi-platform Docker build/push, JAR publish to GitHub Packages, GitHub Release creation
- **Frontend Scope**: None
- **Tests First**: Test with a pre-release tag `v0.1.0-test` before a real release
- **Integration Verification**: Push tag → verify Docker Hub, GitHub Packages, GitHub Releases
- **Parity Verification**: N/A

### Feature 3: README Updates

- **Scope**: Update `README.md` Quick Start section with Docker pull command and Maven dependency snippet
- **Frontend Scope**: None
- **Tests First**: N/A (documentation)
- **Integration Verification**: Follow updated README on a clean machine
- **Parity Verification**: N/A

## Salesforce Parity Verification

N/A — this feature adds no Salesforce API surface.

## Complexity Tracking

No constitution violations.
