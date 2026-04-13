# Data Model: OAuth2 Authorization Code Flow

## Authorization Code

Temporary, single-use credential that maps a successful login to a token exchange.

| Field | Type | Description |
|-------|------|-------------|
| code | String (UUID) | The authorization code value, used as the map key |
| userId | String (UUID) | The authenticated user's ID |
| redirectUri | String | The redirect_uri provided during authorization (must match on exchange) |
| createdAt | Instant | When the code was generated |
| consumed | boolean | Whether the code has already been exchanged |

**Lifecycle**:
- Created: On successful credential submission at `/services/oauth2/authorize`
- Consumed: On successful token exchange at `/services/oauth2/token` with `grant_type=authorization_code`
- Expired: 5 minutes after creation (cleanup via scheduled task or lazy eviction)
- Single-use: Once consumed, any replay attempt returns `invalid_grant`

**Storage**: In-memory ConcurrentHashMap. Codes are ephemeral and don't need to survive restarts.

## Relationships

```
User (existing) <--- userId --- AuthorizationCode (new, in-memory)
                                       |
                                       v
                              JWT Access Token (generated on exchange)
```

The authorization code is a bridge: it holds the user identity from the login step until the CLI exchanges it for a JWT. The JWT itself contains all user claims (userId, username, role) — no separate token-to-user table is needed.
