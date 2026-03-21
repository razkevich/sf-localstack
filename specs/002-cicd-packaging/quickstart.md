# Quickstart: CI/CD Packaging and Distribution

## Prerequisites

- Docker Hub account with a public repository `razkevich/sf-localstack`
- GitHub repository secrets configured:
  - `DOCKERHUB_USERNAME`: your Docker Hub username
  - `DOCKERHUB_TOKEN`: Docker Hub access token (Settings → Security → New Access Token)

## Local Docker Build

```bash
# Build from repo root
docker build -t sf-localstack .

# Run
docker run --rm -p 8080:8080 sf-localstack

# Verify
curl http://localhost:8080/actuator/health
```

## Trigger a CI Build

Push to `main` or open a PR — the `ci.yml` workflow runs automatically.

## Publish a Release

```bash
git tag v0.1.0
git push origin v0.1.0
```

This triggers `release.yml` which:
1. Runs all tests
2. Builds and pushes multi-platform Docker image to Docker Hub
3. Publishes JAR to GitHub Packages
4. Creates a GitHub Release with the JAR attached

## Use the Published Docker Image

```bash
docker pull razkevich/sf-localstack:latest
docker run --rm -p 8080:8080 razkevich/sf-localstack:latest
```

## Use the Published JAR (GitHub Packages)

Add to `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>
  </server>
</servers>
```

Add to `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/razkevich/sf-localstack</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>co.prodly</groupId>
    <artifactId>sf-localstack</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```
