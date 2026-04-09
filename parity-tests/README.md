# API Parity Tests

Compares sf_localstack responses against real Salesforce orgs to identify gaps.

## Prerequisites
- `sf` CLI installed and authenticated to target org
- sf_localstack running locally on port 8080
- `jq` installed (`brew install jq`)

## Usage
```bash
# Run all parity tests against dev20 org
./run-parity.sh dev20

# Run all parity tests against rca org
./run-parity.sh rca

# Run specific test category
./run-parity.sh dev20 rest
./run-parity.sh dev20 bulk
./run-parity.sh dev20 describe
./run-parity.sh dev20 errors
./run-parity.sh dev20 query
```

## Test Categories
| Category  | What it tests |
|-----------|---------------|
| `rest`    | CRUD via REST API — create, get, update, delete record shape |
| `describe`| sObject describe — field names, types, key structural elements |
| `query`   | SOQL — SELECT, WHERE operators, ORDER BY, LIMIT, totalSize |
| `errors`  | Error response shape — invalid object, bad SOQL, missing field, bad ID |
| `bulk`    | Bulk v1 job create/status shape |

## Reports
Results are saved to `parity-tests/reports/` as:
- `<test>-<timestamp>.diff.json` — side-by-side JSON diff for each failing test
- `gap-report-<timestamp>.md` — summary with pass/fail counts and severity ratings

## Conventions
- All test records use the `PARITY_TEST_` prefix for easy identification and cleanup
- Test records are deleted from the real org after each test run
- Tests continue on individual failures (set -euo pipefail applies at script level,
  individual test errors are caught and reported)
- JSON normalization strips dynamic fields (Id, timestamps, URL slugs) before comparison
