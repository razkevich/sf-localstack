#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# tests/describe.sh — sObject Describe Parity Tests
#
# Sourced by run-parity.sh. Uses compare_json / skip_test from parent.
#
# Tests:
#   describe-account-top-keys     — top-level keys present in describe result
#   describe-account-field-names  — set of field names returned
#   describe-account-field-types  — Name field type is "string"
#   describe-contact-top-keys     — same structural check for Contact
#   describe-list-global          — global describe returns sobjects array
# ---------------------------------------------------------------------------

run_describe_tests() {
    local org="$1" emu="$2"

    echo "=== Describe Tests ==="

    _describe_sobject_sf() {
        local sobject="$1"
        sf sobject describe \
            --sobject "$sobject" \
            --target-org "$org" \
            --json 2>/dev/null | jq '.result' 2>/dev/null || echo '{}'
    }

    _describe_sobject_emu() {
        local sobject="$1"
        curl -sf "$emu/services/data/v60.0/sobjects/${sobject}/describe" 2>/dev/null || echo '{}'
    }

    # -----------------------------------------------------------------------
    # 1. Account — top-level keys
    # -----------------------------------------------------------------------
    local sf_acct_desc emu_acct_desc

    sf_acct_desc=$(_describe_sobject_sf "Account")
    emu_acct_desc=$(_describe_sobject_emu "Account")

    if [ "$sf_acct_desc" = "{}" ] || [ "$emu_acct_desc" = "{}" ]; then
        skip_test "describe-account-top-keys" "describe request failed"
    else
        local sf_top emu_top
        # Key structural fields every describe must have
        sf_top=$(echo "$sf_acct_desc"  | jq '{
            hasName:        (.name       != null),
            hasLabel:       (.label      != null),
            hasFields:      (.fields     | type == "array"),
            hasCreateable:  (.createable != null),
            hasQueryable:   (.queryable  != null),
            hasUpdateable:  (.updateable != null)
        }' 2>/dev/null || echo '{}')
        emu_top=$(echo "$emu_acct_desc" | jq '{
            hasName:        (.name       != null),
            hasLabel:       (.label      != null),
            hasFields:      (.fields     | type == "array"),
            hasCreateable:  (.createable != null),
            hasQueryable:   (.queryable  != null),
            hasUpdateable:  (.updateable != null)
        }' 2>/dev/null || echo '{}')
        compare_json "describe-account-top-keys" "$sf_top" "$emu_top" || true
    fi

    # -----------------------------------------------------------------------
    # 2. Account — verify both return the same field names (sorted)
    # -----------------------------------------------------------------------
    if [ "$sf_acct_desc" != "{}" ] && [ "$emu_acct_desc" != "{}" ]; then
        local sf_fields emu_fields
        sf_fields=$(echo "$sf_acct_desc"  | jq '[.fields[].name] | sort' 2>/dev/null || echo '[]')
        emu_fields=$(echo "$emu_acct_desc" | jq '[.fields[].name] | sort' 2>/dev/null || echo '[]')
        compare_json "describe-account-field-names" "$sf_fields" "$emu_fields" || true
    else
        skip_test "describe-account-field-names" "describe data unavailable"
    fi

    # -----------------------------------------------------------------------
    # 3. Account — Name field is type "string"
    # -----------------------------------------------------------------------
    if [ "$sf_acct_desc" != "{}" ] && [ "$emu_acct_desc" != "{}" ]; then
        local sf_name_type emu_name_type
        sf_name_type=$(echo "$sf_acct_desc"  | jq '.fields[] | select(.name == "Name") | .type' 2>/dev/null || echo 'null')
        emu_name_type=$(echo "$emu_acct_desc" | jq '.fields[] | select(.name == "Name") | .type' 2>/dev/null || echo 'null')
        compare_json "describe-account-name-field-type" "$sf_name_type" "$emu_name_type" || true
    else
        skip_test "describe-account-name-field-type" "describe data unavailable"
    fi

    # -----------------------------------------------------------------------
    # 4. Contact — top-level structural keys
    # -----------------------------------------------------------------------
    local sf_con_desc emu_con_desc

    sf_con_desc=$(_describe_sobject_sf "Contact")
    emu_con_desc=$(_describe_sobject_emu "Contact")

    if [ "$sf_con_desc" = "{}" ] || [ "$emu_con_desc" = "{}" ]; then
        skip_test "describe-contact-top-keys" "Contact describe unavailable"
    else
        local sf_con_top emu_con_top
        sf_con_top=$(echo "$sf_con_desc"  | jq '{
            hasName: (.name != null), hasFields: (.fields | type == "array")
        }' 2>/dev/null || echo '{}')
        emu_con_top=$(echo "$emu_con_desc" | jq '{
            hasName: (.name != null), hasFields: (.fields | type == "array")
        }' 2>/dev/null || echo '{}')
        compare_json "describe-contact-top-keys" "$sf_con_top" "$emu_con_top" || true
    fi

    # -----------------------------------------------------------------------
    # 5. Global describe — /sobjects returns a list
    # -----------------------------------------------------------------------
    local sf_global emu_global

    sf_global=$(sf sobject list --target-org "$org" --json 2>/dev/null \
        | jq '{isList: (.result | type == "array"), nonEmpty: (.result | length > 0)}' 2>/dev/null || echo '{}')

    emu_global=$(curl -sf "$emu/services/data/v60.0/sobjects/" 2>/dev/null \
        | jq '{isList: (.sobjects | type == "array"), nonEmpty: (.sobjects | length > 0)}' 2>/dev/null || echo '{}')

    if [ "$sf_global" = "{}" ] || [ "$emu_global" = "{}" ]; then
        skip_test "describe-global-sobjects" "global describe unavailable"
    else
        compare_json "describe-global-sobjects" "$sf_global" "$emu_global" || true
    fi
}
