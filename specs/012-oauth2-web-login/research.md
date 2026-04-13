# Research: OAuth2 Authorization Code Flow

## How SF CLI Web Login Works

**Decision**: Implement standard OAuth2 authorization code flow with SF CLI as the client.

**Flow**:
1. User runs `sf org login web --instance-url http://164.92.219.185`
2. SF CLI starts a local HTTP server on `http://localhost:1717`
3. SF CLI opens browser to: `GET http://164.92.219.185/services/oauth2/authorize?response_type=code&client_id=PlatformCLI&redirect_uri=http://localhost:1717/OauthRedirect&state=<random>&prompt=login`
4. Browser shows login page, user enters credentials
5. Server validates credentials, generates authorization code
6. Server redirects browser to: `http://localhost:1717/OauthRedirect?code=<auth_code>&state=<state>`
7. SF CLI's local server receives the callback, extracts the code
8. SF CLI calls: `POST http://164.92.219.185/services/oauth2/token` with `grant_type=authorization_code&code=<code>&redirect_uri=http://localhost:1717/OauthRedirect`
9. Server exchanges code for JWT, returns Salesforce-compatible token response
10. SF CLI stores the token and calls `/services/oauth2/userinfo` and `/services/data/v*/sobjects/User/<userId>` to get the username

**Rationale**: This matches the documented Salesforce OAuth2 Web Server Flow and is what SF CLI implements.

**Alternatives considered**: 
- Device flow: Not supported by SF CLI's `login web` command
- Implicit grant: Deprecated in OAuth2.1, not used by SF CLI

## Authorization Code Storage

**Decision**: Use ConcurrentHashMap with scheduled cleanup, same pattern as BulkJobService.

**Rationale**: Simple, no new dependencies, sufficient for MVP scale. Codes are short-lived (5 min) and the map is small.

**Alternatives considered**:
- Database storage: Overkill for ephemeral codes
- Redis/external cache: Adds dependency, violates Principle IV

## Login Page Rendering

**Decision**: Return inline HTML string from the controller method. No Thymeleaf or template engine.

**Rationale**: Single HTML page with inline CSS. Adding a template engine would violate Minimal Dependency Surface (Principle IV). The page is simple enough that a String builder or text block works well.

**Alternatives considered**:
- Thymeleaf: New dependency for a single page
- Static HTML file served from resources: Harder to inject dynamic error messages and hidden form fields
- React SPA page: Won't work in OAuth redirect context (needs server-rendered form submission)

## Token Mapping

**Decision**: The authorization code maps to a user ID. When exchanged, the server looks up the user and generates a JWT containing the user's identity (userId, username, role). The JWT is the access token — it IS the mapping between token and user.

**Rationale**: Same as existing password grant. The JWT contains all user identity claims. No separate token-to-user mapping table needed.
