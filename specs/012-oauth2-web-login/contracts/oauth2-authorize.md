# Contract: OAuth2 Authorization Code Flow

## GET /services/oauth2/authorize

**Purpose**: Start the OAuth2 authorization code flow. Returns an HTML login page.

**Request**:
```
GET /services/oauth2/authorize?response_type=code&client_id=PlatformCLI&redirect_uri=http://localhost:1717/OauthRedirect&state=abc123
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| response_type | Yes | Must be `code` |
| client_id | Yes | Client identifier (accepted but not validated for MVP) |
| redirect_uri | Yes | Where to redirect after successful login |
| state | No | Opaque value passed through to redirect |

**Response (200)**: HTML page with login form containing hidden fields for all OAuth params.

**Error (400)**: `response_type` missing or not `code`:
```json
{"error": "unsupported_response_type", "error_description": "Only response_type=code is supported"}
```

---

## POST /services/oauth2/authorize

**Purpose**: Process login credentials, redirect with authorization code.

**Request**:
```
POST /services/oauth2/authorize
Content-Type: application/x-www-form-urlencoded

username=admin&password=secret&response_type=code&client_id=PlatformCLI&redirect_uri=http://localhost:1717/OauthRedirect&state=abc123
```

**Success Response (302)**:
```
Location: http://localhost:1717/OauthRedirect?code=<uuid>&state=abc123
```

**Invalid Credentials (200)**: Re-renders login page with error message.

---

## POST /services/oauth2/token (authorization_code grant)

**Purpose**: Exchange authorization code for access token.

**Request**:
```
POST /services/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=<uuid>&redirect_uri=http://localhost:1717/OauthRedirect
```

**Success Response (200)**:
```json
{
  "access_token": "00D000000000001!<jwt>",
  "instance_url": "http://164.92.219.185",
  "id": "http://164.92.219.185/id/00D000000000001AAA/<userId>",
  "token_type": "Bearer",
  "issued_at": "1776000000000",
  "signature": "jwt"
}
```

**Error Responses (400)**:
```json
{"error": "invalid_grant", "error_description": "authorization code is invalid, expired, or already used"}
{"error": "invalid_grant", "error_description": "redirect_uri mismatch"}
{"error": "invalid_request", "error_description": "code is required"}
```
