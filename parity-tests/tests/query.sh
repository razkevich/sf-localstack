#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# tests/query.sh — SOQL Query Parity Tests
#
# Sourced by run-parity.sh. Uses compare_json / skip_test from parent.
#
# Tests:
#   query-select-basic        — SELECT Id, Name FROM Account LIMIT 5
#   query-where-equals        — WHERE Industry = 'Technology'
#   query-where-like          — WHERE Name LIKE 'PARITY_TEST_%'
#   query-order-by-asc        — ORDER BY Name ASC
#   query-limit               — LIMIT 1 returns exactly 1 record (or 0 if empty)
#   query-count               — SELECT COUNT() FROM Account
#   query-envelope            — totalSize + done + records present in both
# ---------------------------------------------------------------------------

run_query_tests() {
    local org="$1" emu="$2"

    echo "=== Query Tests ==="

    # Seed one record into both systems for predictable queries
    local sf_seed_id emu_seed_id
    sf_seed_id=$(sf data create record \
        --sobject Account \
        --values "Name='PARITY_TEST_QuerySeed' Industry='Technology' AnnualRevenue=1000000" \
        --target-org "$org" \
        --json 2>/dev/null | jq -r '.result.id // empty') || true

    emu_seed_id=$(curl -sf \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"Name":"PARITY_TEST_QuerySeed","Industry":"Technology","AnnualRevenue":1000000}' \
        "$emu/services/data/v60.0/sobjects/Account" 2>/dev/null | jq -r '.id // empty') || true

    # -----------------------------------------------------------------------
    # Helper: run the same SOQL against both sides, compare envelope shape
    # -----------------------------------------------------------------------
    _compare_query_envelope() {
        local test_name="$1"
        local soql="$2"
        local encoded
        encoded=$(python3 -c "import urllib.parse, sys; print(urllib.parse.quote(sys.argv[1]))" "$soql" 2>/dev/null \
                  || printf '%s' "$soql" | sed 's/ /+/g; s/=/%3D/g; s/,/%2C/g; s/(/%28/g; s/)/%29/g; s/'\''/%27/g')

        local sf_res emu_res

        sf_res=$(sf data query \
            --query "$soql" \
            --target-org "$org" \
            --json 2>/dev/null \
            | jq '{totalSize: .result.totalSize, done: .result.done, hasRecords: (.result.records | type == "array")}' \
            2>/dev/null || echo '{}')

        emu_res=$(curl -sf "$emu/services/data/v60.0/query?q=${encoded}" 2>/dev/null \
            | jq '{totalSize: .totalSize, done: .done, hasRecords: (.records | type == "array")}' \
            2>/dev/null || echo '{}')

        compare_json "$test_name" "$sf_res" "$emu_res" || true
    }

    # -----------------------------------------------------------------------
    # 1. Basic SELECT with LIMIT
    # -----------------------------------------------------------------------
    _compare_query_envelope "query-select-basic" \
        "SELECT Id, Name FROM Account LIMIT 5"

    # -----------------------------------------------------------------------
    # 2. WHERE equals
    # -----------------------------------------------------------------------
    _compare_query_envelope "query-where-equals" \
        "SELECT Id, Name, Industry FROM Account WHERE Industry = 'Technology'"

    # -----------------------------------------------------------------------
    # 3. WHERE LIKE
    # -----------------------------------------------------------------------
    _compare_query_envelope "query-where-like" \
        "SELECT Id, Name FROM Account WHERE Name LIKE 'PARITY_TEST_%'"

    # -----------------------------------------------------------------------
    # 4. ORDER BY ASC  — verify both return same totalSize direction (envelope only)
    # -----------------------------------------------------------------------
    _compare_query_envelope "query-order-by-asc" \
        "SELECT Id, Name FROM Account ORDER BY Name ASC LIMIT 10"

    # -----------------------------------------------------------------------
    # 5. LIMIT 1 — totalSize is an integer, records array length <= 1
    # -----------------------------------------------------------------------
    local sf_lim emu_lim
    sf_lim=$(sf data query \
        --query "SELECT Id, Name FROM Account LIMIT 1" \
        --target-org "$org" \
        --json 2>/dev/null \
        | jq '{recordCount: (.result.records | length), totalSizeIsInt: (.result.totalSize | type == "number")}' \
        2>/dev/null || echo '{}')

    emu_lim=$(curl -sf "$emu/services/data/v60.0/query?q=SELECT+Id%2CName+FROM+Account+LIMIT+1" 2>/dev/null \
        | jq '{recordCount: (.records | length), totalSizeIsInt: (.totalSize | type == "number")}' \
        2>/dev/null || echo '{}')

    compare_json "query-limit-one" "$sf_lim" "$emu_lim" || true

    # -----------------------------------------------------------------------
    # 6. COUNT aggregate
    # -----------------------------------------------------------------------
    local sf_count emu_count
    sf_count=$(sf data query \
        --query "SELECT COUNT() FROM Account" \
        --target-org "$org" \
        --json 2>/dev/null \
        | jq '{totalSize: .result.totalSize, done: .result.done}' \
        2>/dev/null || echo '{}')

    emu_count=$(curl -sf "$emu/services/data/v60.0/query?q=SELECT+COUNT()+FROM+Account" 2>/dev/null \
        | jq '{totalSize: .totalSize, done: .done}' \
        2>/dev/null || echo '{}')

    # Only compare shape (is totalSize a number and done a bool) — not the actual count
    local sf_count_shape emu_count_shape
    sf_count_shape=$(echo "$sf_count"  | jq '{totalSizeIsNum: (.totalSize | type == "number"), doneIsBool: (.done | type == "boolean")}' 2>/dev/null || echo '{}')
    emu_count_shape=$(echo "$emu_count" | jq '{totalSizeIsNum: (.totalSize | type == "number"), doneIsBool: (.done | type == "boolean")}' 2>/dev/null || echo '{}')
    compare_json "query-count-shape" "$sf_count_shape" "$emu_count_shape" || true

    # -----------------------------------------------------------------------
    # Cleanup
    # -----------------------------------------------------------------------
    [ -n "$sf_seed_id"  ] && sf data delete record --sobject Account --record-id "$sf_seed_id"  --target-org "$org" --no-prompt 2>/dev/null || true
    [ -n "$emu_seed_id" ] && curl -sf -X DELETE "$emu/services/data/v60.0/sobjects/Account/$emu_seed_id" >/dev/null 2>&1 || true
}
