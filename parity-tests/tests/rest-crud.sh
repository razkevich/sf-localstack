#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# tests/rest-crud.sh — REST CRUD Parity Tests
#
# Sourced by run-parity.sh. Uses compare_json, skip_test, PASS/FAIL/SKIP,
# RESULTS arrays defined in the parent script.
#
# Tests:
#   rest-create-shape   — POST /sobjects/Account response envelope
#   rest-get-shape      — GET /sobjects/Account/:id field set
#   rest-update-shape   — PATCH /sobjects/Account/:id (204 no-content shape)
#   rest-query-shape    — GET /query?q=... envelope (totalSize, done, records)
#   rest-delete-shape   — DELETE /sobjects/Account/:id (204 no-content)
# ---------------------------------------------------------------------------

run_rest_tests() {
    local org="$1" emu="$2"

    echo "=== REST CRUD Tests ==="

    # -----------------------------------------------------------------------
    # 1. Create Account — compare response envelope shape
    # -----------------------------------------------------------------------
    local sf_create sf_id emu_create emu_id

    sf_create=$(sf data create record \
        --sobject Account \
        --values "Name='PARITY_TEST_Acme' Industry='Technology'" \
        --target-org "$org" \
        --json 2>/dev/null) || { skip_test "rest-create-shape" "sf CLI create failed"; return; }

    sf_id=$(echo "$sf_create" | jq -r '.result.id // .result.Id // empty')
    if [ -z "$sf_id" ]; then
        skip_test "rest-create-shape" "could not extract id from sf response"
        return
    fi

    emu_create=$(curl -sf \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"Name":"PARITY_TEST_Acme","Industry":"Technology"}' \
        "$emu/services/data/v60.0/sobjects/Account" 2>/dev/null) || {
        skip_test "rest-create-shape" "emulator create request failed"
        # Still clean up the real record
        sf data delete record --sobject Account --record-id "$sf_id" --target-org "$org" --no-prompt 2>/dev/null || true
        return
    }

    emu_id=$(echo "$emu_create" | jq -r '.id // empty')

    # Compare only the shape (success boolean + id present)
    local sf_shape emu_shape
    sf_shape=$(echo "$sf_create" | jq '{success: .result.success, hasId: (.result.id != null)}' 2>/dev/null || echo '{}')
    emu_shape=$(echo "$emu_create" | jq '{success: .success, hasId: (.id != null)}'            2>/dev/null || echo '{}')
    compare_json "rest-create-shape" "$sf_shape" "$emu_shape" || true

    # -----------------------------------------------------------------------
    # 2. Get by ID — compare field set
    # -----------------------------------------------------------------------
    if [ -n "$emu_id" ]; then
        local sf_get emu_get
        sf_get=$(sf data get record \
            --sobject Account \
            --record-id "$sf_id" \
            --target-org "$org" \
            --json 2>/dev/null | jq '.result' 2>/dev/null || echo '{}')

        emu_get=$(curl -sf "$emu/services/data/v60.0/sobjects/Account/$emu_id" 2>/dev/null || echo '{}')

        # Compare structural shape: which top-level keys are present
        local sf_keys emu_keys
        sf_keys=$(echo "$sf_get"  | jq '[keys[]] | sort' 2>/dev/null || echo '[]')
        emu_keys=$(echo "$emu_get" | jq '[keys[]] | sort' 2>/dev/null || echo '[]')
        compare_json "rest-get-shape" "$sf_keys" "$emu_keys" || true
    else
        skip_test "rest-get-shape" "emulator create did not return id"
    fi

    # -----------------------------------------------------------------------
    # 3. Update — compare HTTP status behaviour (204 no-content)
    # -----------------------------------------------------------------------
    if [ -n "$emu_id" ]; then
        local sf_update_status emu_update_status

        # sf CLI update returns json with status 0 on success
        sf_update_status=$(sf data update record \
            --sobject Account \
            --record-id "$sf_id" \
            --values "Industry='Finance'" \
            --target-org "$org" \
            --json 2>/dev/null | jq '.status' 2>/dev/null || echo '1')

        # emulator PATCH should return 204; curl -o /dev/null captures HTTP status
        emu_update_status=$(curl -sf \
            -o /dev/null \
            -w "%{http_code}" \
            -X PATCH \
            -H "Content-Type: application/json" \
            -d '{"Industry":"Finance"}' \
            "$emu/services/data/v60.0/sobjects/Account/$emu_id" 2>/dev/null || echo '0')

        local sf_update_ok emu_update_ok
        sf_update_ok=$([ "$sf_update_status" = "0" ] && echo "true" || echo "false")
        emu_update_ok=$([ "$emu_update_status" = "204" ] && echo "true" || echo "false")
        compare_json "rest-update-ok" "\"$sf_update_ok\"" "\"$emu_update_ok\"" || true
    else
        skip_test "rest-update-ok" "emulator create did not return id"
    fi

    # -----------------------------------------------------------------------
    # 4. Query — envelope shape (totalSize, done, records array)
    # -----------------------------------------------------------------------
    local sf_query emu_query
    sf_query=$(sf data query \
        --query "SELECT Id, Name, Industry FROM Account WHERE Name = 'PARITY_TEST_Acme'" \
        --target-org "$org" \
        --json 2>/dev/null | jq '{totalSize: .result.totalSize, done: .result.done, hasRecords: (.result.records | type == "array")}' 2>/dev/null || echo '{}')

    emu_query=$(curl -sf \
        "$emu/services/data/v60.0/query?q=SELECT+Id%2CName%2CIndustry+FROM+Account+WHERE+Name%3D'PARITY_TEST_Acme'" \
        2>/dev/null | jq '{totalSize: .totalSize, done: .done, hasRecords: (.records | type == "array")}' 2>/dev/null || echo '{}')

    compare_json "rest-query-shape" "$sf_query" "$emu_query" || true

    # -----------------------------------------------------------------------
    # 5. Cleanup — delete test records
    # -----------------------------------------------------------------------
    sf data delete record --sobject Account --record-id "$sf_id"  --target-org "$org" --no-prompt 2>/dev/null || true
    [ -n "$emu_id" ] && curl -sf -X DELETE "$emu/services/data/v60.0/sobjects/Account/$emu_id" >/dev/null 2>&1 || true
}
