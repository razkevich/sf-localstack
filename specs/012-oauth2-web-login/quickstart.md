# Quickstart: OAuth2 Web Login

## Prerequisites
- sf_localstack running (`mvn -pl service spring-boot:run`)
- At least one user registered (via UI or curl)
- SF CLI installed (`sf --version`)

## Test the Flow

```bash
# 1. Register a user if none exist
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","email":"admin@test.dev","password":"admin123"}'

# 2. Login with SF CLI (opens browser)
sf org login web --instance-url http://localhost:8080 --alias sf-local

# 3. Enter credentials in the browser login page

# 4. Verify the connection
sf org display --target-org sf-local

# 5. Use the org
sf data query --target-org sf-local --query "SELECT Id, Name FROM Account"
```

## Manual Flow Testing

```bash
# Step 1: Open authorize URL in browser
open "http://localhost:8080/services/oauth2/authorize?response_type=code&client_id=test&redirect_uri=http://localhost:1717/OauthRedirect&state=test123"

# Step 2: After login, capture the code from the redirect URL
# The browser will redirect to http://localhost:1717/OauthRedirect?code=<CODE>&state=test123

# Step 3: Exchange the code for a token
curl -X POST http://localhost:8080/services/oauth2/token \
  -d "grant_type=authorization_code&code=<CODE>&redirect_uri=http://localhost:1717/OauthRedirect"
```
