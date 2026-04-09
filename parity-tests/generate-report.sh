#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# generate-report.sh — Gap Report Generator
#
# Sourced by run-parity.sh after all tests have run.
# Reads PASS, FAIL, SKIP, RESULTS arrays from the parent scope.
# Writes a markdown report to SUMMARY_FILE.
# ---------------------------------------------------------------------------

generate_gap_report() {
    local out_file="$1"
    local ts="$2"

    local total=$((PASS + FAIL + SKIP))

    # -----------------------------------------------------------------------
    # Severity heuristics (based on test name prefix)
    # -----------------------------------------------------------------------
    _severity() {
        local name="$1"
        case "$name" in
            error-*)        echo "Critical" ;;  # wrong error shapes break CLI/tools
            rest-create-*|rest-get-*) echo "Critical" ;;
            describe-*)     echo "Major" ;;
            query-*)        echo "Major" ;;
            bulk-*)         echo "Major" ;;
            rest-*)         echo "Minor" ;;
            *)              echo "Minor" ;;
        esac
    }

    # -----------------------------------------------------------------------
    # Write report
    # -----------------------------------------------------------------------
    cat > "$out_file" <<EOF
# sf_localstack API Parity Gap Report

**Generated:** ${ts}
**Org alias:** ${ORG_ALIAS}
**Emulator:** ${EMULATOR}

## Summary

| Result | Count |
|--------|-------|
| Pass   | ${PASS} |
| Fail   | ${FAIL} |
| Skip   | ${SKIP} |
| **Total** | **${total}** |

EOF

    if [ "$FAIL" -eq 0 ]; then
        echo "All tests passed. No gaps detected." >> "$out_file"
    else
        cat >> "$out_file" <<'EOF'
## Failures

EOF
        local critical=0 major=0 minor=0

        for entry in "${RESULTS[@]}"; do
            local status name diff_path
            status=$(echo "$entry"    | cut -d'|' -f1)
            name=$(echo "$entry"      | cut -d'|' -f2)
            diff_path=$(echo "$entry" | cut -d'|' -f3)

            if [ "$status" = "FAIL" ]; then
                local sev
                sev=$(_severity "$name")
                case "$sev" in
                    Critical) critical=$((critical + 1)) ;;
                    Major)    major=$((major + 1)) ;;
                    Minor)    minor=$((minor + 1)) ;;
                esac

                echo "### [$sev] $name" >> "$out_file"
                echo "" >> "$out_file"
                if [ -n "$diff_path" ] && [ -f "$diff_path" ]; then
                    echo "**Diff:** \`$diff_path\`" >> "$out_file"
                    echo "" >> "$out_file"
                    echo '```json' >> "$out_file"
                    # Show the diff inline (first 40 lines to keep report readable)
                    jq '.' "$diff_path" 2>/dev/null | head -40 >> "$out_file" || cat "$diff_path" | head -40 >> "$out_file"
                    local line_count
                    line_count=$(jq '.' "$diff_path" 2>/dev/null | wc -l || wc -l < "$diff_path")
                    [ "$line_count" -gt 40 ] && echo "... (truncated — see full diff at $diff_path)" >> "$out_file"
                    echo '```' >> "$out_file"
                fi
                echo "" >> "$out_file"
            fi
        done

        cat >> "$out_file" <<EOF

## Severity Breakdown

| Severity | Count | Impact |
|----------|-------|--------|
| Critical | $critical | Breaks sf CLI integration or fundamental API contract |
| Major    | $major | Functional gap — feature works but responses differ |
| Minor    | $minor | Cosmetic difference, unlikely to affect real usage |

EOF
    fi

    # -----------------------------------------------------------------------
    # Skipped tests (useful for tracking coverage)
    # -----------------------------------------------------------------------
    if [ "$SKIP" -gt 0 ]; then
        echo "## Skipped Tests" >> "$out_file"
        echo "" >> "$out_file"
        echo "| Test | Reason |" >> "$out_file"
        echo "|------|--------|" >> "$out_file"
        for entry in "${RESULTS[@]}"; do
            local status name reason
            status=$(echo "$entry" | cut -d'|' -f1)
            name=$(echo "$entry"   | cut -d'|' -f2)
            reason=$(echo "$entry" | cut -d'|' -f3)
            if [ "$status" = "SKIP" ]; then
                echo "| $name | ${reason:-—} |" >> "$out_file"
            fi
        done
        echo "" >> "$out_file"
    fi

    # -----------------------------------------------------------------------
    # Passed tests (for completeness)
    # -----------------------------------------------------------------------
    if [ "$PASS" -gt 0 ]; then
        echo "## Passed Tests" >> "$out_file"
        echo "" >> "$out_file"
        for entry in "${RESULTS[@]}"; do
            local status name
            status=$(echo "$entry" | cut -d'|' -f1)
            name=$(echo "$entry"   | cut -d'|' -f2)
            [ "$status" = "PASS" ] && echo "- $name" >> "$out_file"
        done
        echo "" >> "$out_file"
    fi

    echo "---" >> "$out_file"
    echo "_Report generated by parity-tests/run-parity.sh_" >> "$out_file"
}
