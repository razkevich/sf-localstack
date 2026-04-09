#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# run-parity.sh — API Parity Test Runner
#
# Usage:  ./run-parity.sh <org-alias> [category]
#
# Runs the same operations against a real Salesforce org and the local
# sf_localstack emulator, then compares JSON responses.
# ---------------------------------------------------------------------------

ORG_ALIAS="${1:?Usage: $0 <org-alias> [category]}"
CATEGORY="${2:-all}"
EMULATOR="${EMULATOR_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/reports"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
SUMMARY_FILE="$REPORT_DIR/gap-report-${TIMESTAMP}.md"

mkdir -p "$REPORT_DIR"

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
check_prereqs() {
    local missing=0

    command -v sf >/dev/null 2>&1 || { echo "ERROR: sf CLI not found — install from https://developer.salesforce.com/tools/sfdxcli"; missing=1; }
    command -v jq >/dev/null 2>&1 || { echo "ERROR: jq not found — brew install jq"; missing=1; }

    if ! curl -sf "$EMULATOR/services/data/" >/dev/null 2>&1; then
        echo "ERROR: sf_localstack not reachable at $EMULATOR"
        echo "       Start it with: cd service && mvn spring-boot:run"
        missing=1
    fi

    if ! sf org display --target-org "$ORG_ALIAS" >/dev/null 2>&1; then
        echo "ERROR: org alias '$ORG_ALIAS' not found — run: sf org login web --alias $ORG_ALIAS"
        missing=1
    fi

    [ "$missing" -eq 0 ] || exit 1
}

# ---------------------------------------------------------------------------
# Shared state (accumulated by each test file via sourced functions)
# ---------------------------------------------------------------------------
PASS=0
FAIL=0
SKIP=0
RESULTS=()

# ---------------------------------------------------------------------------
# compare_json <name> <sf_response> <emu_response>
#
# Normalizes both responses by removing dynamic/org-specific fields, then
# does a string comparison. On mismatch writes a diff JSON to REPORT_DIR.
# ---------------------------------------------------------------------------
compare_json() {
    local name="$1"
    local sf_response="$2"
    local emu_response="$3"
    local diff_file="$REPORT_DIR/${name}-${TIMESTAMP}.diff.json"

    # Strip fields that legitimately differ between orgs/environments
    local STRIP='del(.. | .Id?, .id?, .CreatedById?, .LastModifiedById?,
                          .CreatedDate?, .LastModifiedDate?, .SystemModstamp?,
                          .attributes?.url?, .nextRecordsUrl?)'

    local sf_norm emu_norm
    sf_norm=$(echo "$sf_response"  | jq --sort-keys "$STRIP" 2>/dev/null || echo "$sf_response")
    emu_norm=$(echo "$emu_response" | jq --sort-keys "$STRIP" 2>/dev/null || echo "$emu_response")

    if [ "$sf_norm" = "$emu_norm" ]; then
        PASS=$((PASS + 1))
        RESULTS+=("PASS|$name|")
        echo "  PASS  $name"
        return 0
    else
        FAIL=$((FAIL + 1))
        jq -n \
            --argjson sf  "$(echo "$sf_norm"  | jq '.' 2>/dev/null || echo 'null')" \
            --argjson emu "$(echo "$emu_norm" | jq '.' 2>/dev/null || echo 'null')" \
            '{salesforce: $sf, emulator: $emu}' > "$diff_file" 2>/dev/null || true
        RESULTS+=("FAIL|$name|$diff_file")
        echo "  FAIL  $name  (diff -> $diff_file)"
        return 1
    fi
}

# skip_test — call when a test cannot run (missing object type, etc.)
skip_test() {
    local name="$1" reason="${2:-}"
    SKIP=$((SKIP + 1))
    RESULTS+=("SKIP|$name|$reason")
    echo "  SKIP  $name${reason:+  ($reason)}"
}

# ---------------------------------------------------------------------------
# Load test modules
# ---------------------------------------------------------------------------
# shellcheck source=tests/rest-crud.sh
source "$SCRIPT_DIR/tests/rest-crud.sh"
# shellcheck source=tests/describe.sh
source "$SCRIPT_DIR/tests/describe.sh"
# shellcheck source=tests/query.sh
source "$SCRIPT_DIR/tests/query.sh"
# shellcheck source=tests/errors.sh
source "$SCRIPT_DIR/tests/errors.sh"
# shellcheck source=tests/bulk.sh
source "$SCRIPT_DIR/tests/bulk.sh"

# ---------------------------------------------------------------------------
# Run selected categories
# ---------------------------------------------------------------------------
echo ""
echo "=================================================="
echo "  sf_localstack API Parity Tests"
echo "  Org:      $ORG_ALIAS"
echo "  Emulator: $EMULATOR"
echo "  Category: $CATEGORY"
echo "  Started:  $(date)"
echo "=================================================="
echo ""

run_category() {
    local cat="$1"
    case "$cat" in
        rest)     run_rest_tests     "$ORG_ALIAS" "$EMULATOR" ;;
        describe) run_describe_tests "$ORG_ALIAS" "$EMULATOR" ;;
        query)    run_query_tests    "$ORG_ALIAS" "$EMULATOR" ;;
        errors)   run_error_tests    "$ORG_ALIAS" "$EMULATOR" ;;
        bulk)     run_bulk_tests     "$ORG_ALIAS" "$EMULATOR" ;;
        *)        echo "ERROR: unknown category '$cat'"; exit 1 ;;
    esac
}

if [ "$CATEGORY" = "all" ]; then
    for cat in rest describe query errors bulk; do
        run_category "$cat"
        echo ""
    done
else
    run_category "$CATEGORY"
fi

# ---------------------------------------------------------------------------
# Generate gap report
# ---------------------------------------------------------------------------
source "$SCRIPT_DIR/generate-report.sh"
generate_gap_report "$SUMMARY_FILE" "$TIMESTAMP"

# ---------------------------------------------------------------------------
# Final summary
# ---------------------------------------------------------------------------
TOTAL=$((PASS + FAIL + SKIP))
echo ""
echo "=================================================="
echo "  Results: $PASS/$TOTAL passed  |  $FAIL failed  |  $SKIP skipped"
echo "  Report:  $SUMMARY_FILE"
echo "=================================================="
echo ""

[ "$FAIL" -eq 0 ]
