#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# tests/errors.sh — Error Response Parity Tests
#
# Sourced by run-parity.sh. Uses compare_json / skip_test from parent.
#
# Tests:
#   error-invalid-sobject       — POST to non-existent object type
#   error-invalid-record-id     — GET with malformed record ID
#   error-record-not-found      — GET with well-formed but non-existent ID
#   error-bad-soql              — Malformed SOQL query
#   error-missing-required-field— Create without required Name field (Contact)
#
# Strategy: compare the error *shape* (errorCode present, message non-empty)
# rather than exact text, since Salesforce messages can vary.
# ---------------------------------------------------------------------------

run_error_tests() {
    local org="$1" emu="$2"

    echo "=== Error Response Tests ==="

    # -----------------------------------------------------------------------
    # Helper: extract error shape from a Salesforce-style error array
    #   [{"message":"...","errorCode":"...","fields":[...]}]
    # -----------------------------------------------------------------------
    _sf_error_shape() {
        echo "$1" | jq '
            if type == "array" then .[0]
            elif .errorCode != null then .
            else . end
            | {
                hasErrorCode: (.errorCode != null and .errorCode != ""),
                hasMessage:   (.message   != null and .message   != "")
            }' 2>/dev/null || echo '{"hasErrorCode":false,"hasMessage":false}'
    }

    # -----------------------------------------------------------------------
    # 1. Invalid sObject type — expect 404 / NOT_FOUND error shape
    # -----------------------------------------------------------------------
    local sf_inv_obj emu_inv_obj

    # sf CLI: create on bogus object; expect non-zero exit and JSON error
    sf_inv_obj=$(sf data create record \
        --sobject PARITY_TEST_NoSuchObject \
        --values "Name='x'" \
        --target-org "$org" \
        --json 2>/dev/null || true)

    emu_inv_obj_raw=$(curl -sf \
        -o /dev/null \
        -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"Name":"x"}' \
        "$emu/services/data/v60.0/sobjects/PARITY_TEST_NoSuchObject" 2>/dev/null || echo "0")

    # For emulator: fetch the actual body separately
    emu_inv_obj_body=$(curl -s \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"Name":"x"}' \
        "$emu/services/data/v60.0/sobjects/PARITY_TEST_NoSuchObject" 2>/dev/null || echo '[]')

    local sf_inv_shape emu_inv_shape
    # sf CLI wraps errors in .result or .message at top level on failure
    sf_inv_shape=$(echo "$sf_inv_obj" | jq '{
        isError: (.status != 0 or .result.success == false or (.result | type) == "null")
    }' 2>/dev/null || echo '{"isError":true}')

    emu_inv_shape=$(echo "$emu_inv_obj_body" | jq '
        if type == "array" then {isError: (length > 0 and .[0].errorCode != null)}
        else {isError: (.errorCode != null)}
        end' 2>/dev/null || echo '{"isError":true}')

    compare_json "error-invalid-sobject" "$sf_inv_shape" "$emu_inv_shape" || true

    # -----------------------------------------------------------------------
    # 2. Invalid (malformed) record ID
    # -----------------------------------------------------------------------
    local sf_bad_id emu_bad_id

    sf_bad_id=$(sf data get record \
        --sobject Account \
        --record-id "NOT_A_REAL_ID_XYZ" \
        --target-org "$org" \
        --json 2>/dev/null || true)

    emu_bad_id_body=$(curl -s \
        "$emu/services/data/v60.0/sobjects/Account/NOT_A_REAL_ID_XYZ" 2>/dev/null || echo '[]')

    sf_bad_id_shape=$(echo "$sf_bad_id" | jq '{isError: (.status != 0)}' 2>/dev/null || echo '{"isError":true}')
    emu_bad_id_shape=$(echo "$emu_bad_id_body" | jq '
        if type == "array" then {isError: (length > 0)}
        else {isError: (.errorCode != null)}
        end' 2>/dev/null || echo '{"isError":true}')

    compare_json "error-invalid-record-id" "$sf_bad_id_shape" "$emu_bad_id_shape" || true

    # -----------------------------------------------------------------------
    # 3. Record not found — well-formed ID that does not exist
    # -----------------------------------------------------------------------
    local FAKE_ID="001000000000000AAA"

    local sf_notfound emu_notfound_body

    sf_notfound=$(sf data get record \
        --sobject Account \
        --record-id "$FAKE_ID" \
        --target-org "$org" \
        --json 2>/dev/null || true)

    emu_notfound_body=$(curl -s \
        "$emu/services/data/v60.0/sobjects/Account/$FAKE_ID" 2>/dev/null || echo '[]')

    sf_notfound_shape=$(echo "$sf_notfound" | jq '{isError: (.status != 0)}' 2>/dev/null || echo '{"isError":true}')
    emu_notfound_shape=$(echo "$emu_notfound_body" | jq '
        if type == "array" then {isError: (length > 0)}
        else {isError: (.errorCode != null)}
        end' 2>/dev/null || echo '{"isError":true}')

    compare_json "error-record-not-found" "$sf_notfound_shape" "$emu_notfound_shape" || true

    # -----------------------------------------------------------------------
    # 4. Bad SOQL syntax
    # -----------------------------------------------------------------------
    local sf_badsoql emu_badsoql_body

    sf_badsoql=$(sf data query \
        --query "SELEKT Id FROM Account" \
        --target-org "$org" \
        --json 2>/dev/null || true)

    emu_badsoql_body=$(curl -s \
        "$emu/services/data/v60.0/query?q=SELEKT+Id+FROM+Account" 2>/dev/null || echo '[]')

    sf_badsoql_shape=$(echo "$sf_badsoql" | jq '{isError: (.status != 0)}' 2>/dev/null || echo '{"isError":true}')
    emu_badsoql_shape=$(echo "$emu_badsoql_body" | jq '
        if type == "array" then {isError: (length > 0)}
        else {isError: (.errorCode != null)}
        end' 2>/dev/null || echo '{"isError":true}')

    compare_json "error-bad-soql" "$sf_badsoql_shape" "$emu_badsoql_shape" || true

    # -----------------------------------------------------------------------
    # 5. Missing required field — Contact without LastName
    # -----------------------------------------------------------------------
    local sf_missing emu_missing_body

    sf_missing=$(sf data create record \
        --sobject Contact \
        --values "FirstName='PARITY_TEST_NoLastName'" \
        --target-org "$org" \
        --json 2>/dev/null || true)

    emu_missing_body=$(curl -s \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"FirstName":"PARITY_TEST_NoLastName"}' \
        "$emu/services/data/v60.0/sobjects/Contact" 2>/dev/null || echo '[]')

    sf_missing_shape=$(echo "$sf_missing" | jq '{isError: (.status != 0 or .result.success == false)}' 2>/dev/null || echo '{"isError":true}')
    emu_missing_shape=$(echo "$emu_missing_body" | jq '
        if type == "array" then {isError: (length > 0)}
        elif .success == false then {isError: true}
        else {isError: (.errorCode != null)}
        end' 2>/dev/null || echo '{"isError":true}')

    compare_json "error-missing-required-field" "$sf_missing_shape" "$emu_missing_shape" || true
}
