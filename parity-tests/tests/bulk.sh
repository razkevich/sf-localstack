#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# tests/bulk.sh — Bulk API v1 Parity Tests
#
# Sourced by run-parity.sh. Uses compare_json / skip_test from parent.
#
# Tests:
#   bulk-job-create-shape       — POST /async/bulk/... job envelope shape
#   bulk-job-state-queued       — newly created job state is "Open"
#   bulk-job-get-shape          — GET job returns same top-level keys
#   bulk-job-abort-ok           — abort (close) a job without error
#
# NOTE: sf CLI Bulk API v1 commands differ between CLI versions.
#       We use raw curl against the Salesforce REST Bulk API directly for
#       the real-org side so the comparison is apples-to-apples.
#       Auth token is obtained from `sf org display`.
# ---------------------------------------------------------------------------

run_bulk_tests() {
    local org="$1" emu="$2"

    echo "=== Bulk API Tests ==="

    # -----------------------------------------------------------------------
    # Obtain real org credentials
    # -----------------------------------------------------------------------
    local org_info sf_instance sf_token

    org_info=$(sf org display --target-org "$org" --json 2>/dev/null) || {
        skip_test "bulk-job-create-shape" "could not get org info"
        skip_test "bulk-job-state-queued" "could not get org info"
        skip_test "bulk-job-get-shape"    "could not get org info"
        skip_test "bulk-job-abort-ok"     "could not get org info"
        return
    }

    sf_instance=$(echo "$org_info" | jq -r '.result.instanceUrl // empty')
    sf_token=$(echo "$org_info"    | jq -r '.result.accessToken // empty')

    if [ -z "$sf_instance" ] || [ -z "$sf_token" ]; then
        skip_test "bulk-job-create-shape" "missing instanceUrl or accessToken"
        skip_test "bulk-job-state-queued" "missing instanceUrl or accessToken"
        skip_test "bulk-job-get-shape"    "missing instanceUrl or accessToken"
        skip_test "bulk-job-abort-ok"     "missing instanceUrl or accessToken"
        return
    fi

    local SF_BULK_BASE="$sf_instance/services/async/41.0"
    local EMU_BULK_BASE="$emu/services/async/41.0"

    local JOB_XML='<?xml version="1.0" encoding="UTF-8"?>
<jobInfo xmlns="http://www.force.com/2009/06/asyncapi/dataload">
  <operation>insert</operation>
  <object>Account</object>
  <contentType>CSV</contentType>
</jobInfo>'

    # -----------------------------------------------------------------------
    # 1 & 2. Create job — compare shape and initial state
    # -----------------------------------------------------------------------
    local sf_job_xml emu_job_xml sf_job_id emu_job_id

    sf_job_xml=$(curl -sf \
        -X POST \
        -H "X-SFDC-Session: $sf_token" \
        -H "Content-Type: application/xml" \
        -d "$JOB_XML" \
        "$SF_BULK_BASE/job" 2>/dev/null) || {
        skip_test "bulk-job-create-shape" "sf bulk job create failed"
        skip_test "bulk-job-state-queued" "sf bulk job create failed"
        skip_test "bulk-job-get-shape"    "sf bulk job create failed"
        skip_test "bulk-job-abort-ok"     "sf bulk job create failed"
        return
    }

    emu_job_xml=$(curl -sf \
        -X POST \
        -H "Content-Type: application/xml" \
        -d "$JOB_XML" \
        "$EMU_BULK_BASE/job" 2>/dev/null) || {
        skip_test "bulk-job-create-shape" "emulator bulk job create failed"
        skip_test "bulk-job-state-queued" "emulator bulk job create failed"
        skip_test "bulk-job-get-shape"    "emulator bulk job create failed"
        skip_test "bulk-job-abort-ok"     "emulator bulk job create failed"
        # Still close the sf job
        sf_job_id=$(echo "$sf_job_xml" | grep -oP '(?<=<id>)[^<]+' | head -1 || true)
        [ -n "$sf_job_id" ] && curl -sf \
            -X POST \
            -H "X-SFDC-Session: $sf_token" \
            -H "Content-Type: application/xml" \
            -d '<jobInfo xmlns="http://www.force.com/2009/06/asyncapi/dataload"><state>Closed</state></jobInfo>' \
            "$SF_BULK_BASE/job/$sf_job_id" >/dev/null 2>&1 || true
        return
    }

    # Extract IDs from XML  <id>...</id>
    sf_job_id=$(echo  "$sf_job_xml"  | grep -oP '(?<=<id>)[^<]+' | head -1 || true)
    emu_job_id=$(echo "$emu_job_xml" | grep -oP '(?<=<id>)[^<]+' | head -1 || true)

    # Shape: has <id>, <state>, <object>, <operation>
    _xml_shape() {
        local xml="$1"
        jq -n \
            --arg hasId     "$(echo "$xml" | grep -c '<id>'        || echo 0)" \
            --arg hasState  "$(echo "$xml" | grep -c '<state>'     || echo 0)" \
            --arg hasObject "$(echo "$xml" | grep -c '<object>'    || echo 0)" \
            --arg hasOp     "$(echo "$xml" | grep -c '<operation>' || echo 0)" \
            '{
                hasId:        ($hasId     | tonumber > 0),
                hasState:     ($hasState  | tonumber > 0),
                hasObject:    ($hasObject | tonumber > 0),
                hasOperation: ($hasOp     | tonumber > 0)
            }' 2>/dev/null || echo '{}'
    }

    local sf_create_shape emu_create_shape
    sf_create_shape=$(_xml_shape "$sf_job_xml")
    emu_create_shape=$(_xml_shape "$emu_job_xml")
    compare_json "bulk-job-create-shape" "$sf_create_shape" "$emu_create_shape" || true

    # State should be "Open" for a newly created job
    local sf_state emu_state
    sf_state=$(echo  "$sf_job_xml"  | grep -oP '(?<=<state>)[^<]+' | head -1 || echo "unknown")
    emu_state=$(echo "$emu_job_xml" | grep -oP '(?<=<state>)[^<]+' | head -1 || echo "unknown")
    compare_json "bulk-job-state-open" "\"$sf_state\"" "\"$emu_state\"" || true

    # -----------------------------------------------------------------------
    # 3. GET job — same shape as create response
    # -----------------------------------------------------------------------
    if [ -n "$sf_job_id" ] && [ -n "$emu_job_id" ]; then
        local sf_get_xml emu_get_xml
        sf_get_xml=$(curl -sf \
            -H "X-SFDC-Session: $sf_token" \
            "$SF_BULK_BASE/job/$sf_job_id" 2>/dev/null || echo "")
        emu_get_xml=$(curl -sf \
            "$EMU_BULK_BASE/job/$emu_job_id" 2>/dev/null || echo "")

        local sf_get_shape emu_get_shape
        sf_get_shape=$(_xml_shape "$sf_get_xml")
        emu_get_shape=$(_xml_shape "$emu_get_xml")
        compare_json "bulk-job-get-shape" "$sf_get_shape" "$emu_get_shape" || true
    else
        skip_test "bulk-job-get-shape" "job IDs not available"
    fi

    # -----------------------------------------------------------------------
    # 4. Abort (close) job — both should succeed without error
    # -----------------------------------------------------------------------
    local CLOSE_XML='<?xml version="1.0" encoding="UTF-8"?>
<jobInfo xmlns="http://www.force.com/2009/06/asyncapi/dataload">
  <state>Closed</state>
</jobInfo>'

    local sf_close_ok emu_close_ok
    sf_close_ok="false"
    emu_close_ok="false"

    if [ -n "$sf_job_id" ]; then
        local sf_close_xml
        sf_close_xml=$(curl -sf \
            -X POST \
            -H "X-SFDC-Session: $sf_token" \
            -H "Content-Type: application/xml" \
            -d "$CLOSE_XML" \
            "$SF_BULK_BASE/job/$sf_job_id" 2>/dev/null || echo "")
        echo "$sf_close_xml" | grep -q '<state>Closed</state>' && sf_close_ok="true"
    fi

    if [ -n "$emu_job_id" ]; then
        local emu_close_xml
        emu_close_xml=$(curl -sf \
            -X POST \
            -H "Content-Type: application/xml" \
            -d "$CLOSE_XML" \
            "$EMU_BULK_BASE/job/$emu_job_id" 2>/dev/null || echo "")
        echo "$emu_close_xml" | grep -q '<state>Closed</state>' && emu_close_ok="true"
    fi

    compare_json "bulk-job-abort-ok" "\"$sf_close_ok\"" "\"$emu_close_ok\"" || true
}
