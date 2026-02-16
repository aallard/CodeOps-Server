#!/bin/bash
# ============================================
# CodeOps Seed Data Loader
# ============================================
# Loads test data into PostgreSQL and publishes
# sample events to Kafka for Analytics
#
# Usage:
#   ./seed-codeops.sh          # Load all seed data
#   ./seed-codeops.sh reset    # Clear and reload
#   ./seed-codeops.sh kafka    # Only publish Kafka events

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
GITHUB_DIR="${GITHUB_DIR:-$HOME/Documents/Github}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

info() { echo -e "${BLUE}info: $1${NC}"; }
success() { echo -e "${GREEN}done: $1${NC}"; }
warn() { echo -e "${YELLOW}warn: $1${NC}"; }
error() { echo -e "${RED}error: $1${NC}"; exit 1; }

# Check if PostgreSQL is running
check_postgres() {
    if ! docker exec codeops-db pg_isready -U codeops -d codeops >/dev/null 2>&1; then
        error "PostgreSQL is not running. Start infrastructure first: ./start-infra.sh"
    fi
}

# Check if Kafka is running
check_kafka() {
    if ! docker exec codeops-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; then
        error "Kafka is not running. Start infrastructure first: ./start-infra.sh"
    fi
}

# Load SQL seed data
load_sql() {
    info "Loading PostgreSQL seed data..."

    if [ ! -f "$SCRIPT_DIR/seed-data.sql" ]; then
        error "seed-data.sql not found in $SCRIPT_DIR"
    fi

    docker exec -i codeops-db psql -U codeops -d codeops < "$SCRIPT_DIR/seed-data.sql"

    success "PostgreSQL seed data loaded"
}

# Clear existing data
clear_data() {
    warn "Clearing existing data..."

    docker exec -i codeops-db psql -U codeops -d codeops << 'EOF'
SET search_path TO public;

-- Clear tables (order matters for FK constraints)
TRUNCATE TABLE decision_comments CASCADE;
TRUNCATE TABLE decision_votes CASCADE;
TRUNCATE TABLE stakeholder_responses CASCADE;
TRUNCATE TABLE key_results CASCADE;
TRUNCATE TABLE decisions CASCADE;
TRUNCATE TABLE stakeholders CASCADE;
TRUNCATE TABLE hypotheses CASCADE;
TRUNCATE TABLE outcomes CASCADE;
TRUNCATE TABLE decision_queues CASCADE;
TRUNCATE TABLE team_members CASCADE;
TRUNCATE TABLE teams CASCADE;
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE role_permissions CASCADE;
TRUNCATE TABLE roles CASCADE;
TRUNCATE TABLE permissions CASCADE;
TRUNCATE TABLE tenants CASCADE;

-- Clear analytics schema (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'analytics') THEN
        TRUNCATE TABLE analytics.metric_snapshots CASCADE;
        TRUNCATE TABLE analytics.decision_cycle_log CASCADE;
        TRUNCATE TABLE analytics.reports CASCADE;
    END IF;
END $$;

SELECT 'Data cleared' as status;
EOF

    success "Existing data cleared"
}

# Publish Kafka events for Analytics
publish_kafka_events() {
    info "Publishing sample Kafka events for Analytics..."

    # Decision Resolved Events (simulating historical decisions)
    for i in {1..10}; do
        TENANT_ID="11111111-1111-1111-1111-111111111111"
        DECISION_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
        CREATED_DAYS_AGO=$((RANDOM % 30 + 1))
        RESOLVED_DAYS_AGO=$((CREATED_DAYS_AGO - RANDOM % 3))
        CYCLE_HOURS=$((RANDOM % 72 + 4))
        PRIORITY_OPTIONS=("LOW" "MEDIUM" "HIGH" "CRITICAL")
        PRIORITY=${PRIORITY_OPTIONS[$RANDOM % 4]}
        ESCALATED=$((RANDOM % 5 == 0))  # 20% escalation rate

        EVENT=$(cat <<EOF
{
  "tenantId": "$TENANT_ID",
  "decisionId": "$DECISION_ID",
  "title": "Sample Decision $i",
  "priority": "$PRIORITY",
  "decisionType": "TECHNICAL",
  "resolvedBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003",
  "stakeholderId": "88888888-8888-8888-8888-888888888801",
  "wasEscalated": $ESCALATED,
  "createdAt": "$(date -v-${CREATED_DAYS_AGO}d -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -d "$CREATED_DAYS_AGO days ago" -u +%Y-%m-%dT%H:%M:%SZ)",
  "resolvedAt": "$(date -v-${RESOLVED_DAYS_AGO}d -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -d "$RESOLVED_DAYS_AGO days ago" -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)

        echo "$EVENT" | docker exec -i codeops-kafka kafka-console-producer \
            --bootstrap-server localhost:9092 \
            --topic codeops.core.decision.resolved 2>/dev/null
    done

    success "Published 10 decision.resolved events"

    # Outcome Validated Events
    for i in {1..5}; do
        TENANT_ID="11111111-1111-1111-1111-111111111111"
        OUTCOME_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
        CREATED_DAYS_AGO=$((RANDOM % 60 + 10))
        VALIDATED_DAYS_AGO=$((RANDOM % 10))

        EVENT=$(cat <<EOF
{
  "tenantId": "$TENANT_ID",
  "outcomeId": "$OUTCOME_ID",
  "title": "Sample Outcome $i",
  "validatedBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001",
  "createdAt": "$(date -v-${CREATED_DAYS_AGO}d -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -d "$CREATED_DAYS_AGO days ago" -u +%Y-%m-%dT%H:%M:%SZ)",
  "validatedAt": "$(date -v-${VALIDATED_DAYS_AGO}d -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -d "$VALIDATED_DAYS_AGO days ago" -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)

        echo "$EVENT" | docker exec -i codeops-kafka kafka-console-producer \
            --bootstrap-server localhost:9092 \
            --topic codeops.core.outcome.validated 2>/dev/null
    done

    success "Published 5 outcome.validated events"

    # Hypothesis Concluded Events
    for i in {1..8}; do
        TENANT_ID="11111111-1111-1111-1111-111111111111"
        HYPOTHESIS_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
        OUTCOME_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
        CREATED_DAYS_AGO=$((RANDOM % 45 + 5))
        CONCLUDED_DAYS_AGO=$((RANDOM % 5))
        RESULT_OPTIONS=("VALIDATED" "INVALIDATED")
        RESULT=${RESULT_OPTIONS[$RANDOM % 2]}

        EVENT=$(cat <<EOF
{
  "tenantId": "$TENANT_ID",
  "hypothesisId": "$HYPOTHESIS_ID",
  "outcomeId": "$OUTCOME_ID",
  "result": "$RESULT",
  "createdAt": "$(date -v-${CREATED_DAYS_AGO}d -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -d "$CREATED_DAYS_AGO days ago" -u +%Y-%m-%dT%H:%M:%SZ)",
  "concludedAt": "$(date -v-${CONCLUDED_DAYS_AGO}d -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -d "$CONCLUDED_DAYS_AGO days ago" -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)

        echo "$EVENT" | docker exec -i codeops-kafka kafka-console-producer \
            --bootstrap-server localhost:9092 \
            --topic codeops.core.hypothesis.concluded 2>/dev/null
    done

    success "Published 8 hypothesis.concluded events"
}

# Verify data loaded
verify_data() {
    info "Verifying seed data..."

    docker exec -i codeops-db psql -U codeops -d codeops << 'EOF'
SET search_path TO public;

SELECT 'Data Verification' as report;
SELECT '=================' as separator;
SELECT 'Tenants:' as entity, COUNT(*)::text as count FROM tenants
UNION ALL
SELECT 'Permissions:', COUNT(*)::text FROM permissions
UNION ALL
SELECT 'Roles:', COUNT(*)::text FROM roles
UNION ALL
SELECT 'Users:', COUNT(*)::text FROM users
UNION ALL
SELECT 'Teams:', COUNT(*)::text FROM teams
UNION ALL
SELECT 'Decision Queues:', COUNT(*)::text FROM decision_queues
UNION ALL
SELECT 'Stakeholders:', COUNT(*)::text FROM stakeholders
UNION ALL
SELECT 'Outcomes:', COUNT(*)::text FROM outcomes
UNION ALL
SELECT 'Key Results:', COUNT(*)::text FROM key_results
UNION ALL
SELECT 'Hypotheses:', COUNT(*)::text FROM hypotheses
UNION ALL
SELECT 'Decisions:', COUNT(*)::text FROM decisions
UNION ALL
SELECT 'Votes:', COUNT(*)::text FROM decision_votes
UNION ALL
SELECT 'Comments:', COUNT(*)::text FROM decision_comments;
EOF
}

# Show test credentials
show_credentials() {
    echo ""
    echo "============================================================"
    echo "                    TEST CREDENTIALS                        "
    echo "============================================================"
    echo "  ACME CORPORATION (Tenant 1)                               "
    echo "  -----------------------------------------                 "
    echo "  alice@acme.com    | password123 | Tenant Owner            "
    echo "  bob@acme.com      | password123 | Tenant Admin            "
    echo "  carol@acme.com    | password123 | Manager                 "
    echo "  david@acme.com    | password123 | Team Lead               "
    echo "  emma@acme.com     | password123 | Contributor             "
    echo "  frank@acme.com    | password123 | Contributor             "
    echo "============================================================"
    echo "  TECHSTART INC (Tenant 2)                                  "
    echo "  -----------------------------------------                 "
    echo "  grace@techstart.io| password123 | Tenant Owner            "
    echo "  henry@techstart.io| password123 | Manager                 "
    echo "  ivy@techstart.io  | password123 | Team Lead               "
    echo "  jack@techstart.io | password123 | Contributor             "
    echo "============================================================"
    echo ""
}

# Main
main() {
    echo ""
    echo "CodeOps Seed Data Loader"
    echo "========================================"
    echo ""

    case "${1:-all}" in
        all)
            check_postgres
            load_sql
            verify_data
            check_kafka
            publish_kafka_events
            show_credentials
            success "All seed data loaded successfully!"
            ;;
        reset)
            check_postgres
            clear_data
            load_sql
            verify_data
            check_kafka
            publish_kafka_events
            show_credentials
            success "Data reset and reloaded successfully!"
            ;;
        kafka)
            check_kafka
            publish_kafka_events
            success "Kafka events published!"
            ;;
        sql)
            check_postgres
            load_sql
            verify_data
            show_credentials
            success "SQL seed data loaded!"
            ;;
        clear)
            check_postgres
            clear_data
            success "All data cleared!"
            ;;
        verify)
            check_postgres
            verify_data
            show_credentials
            ;;
        *)
            echo "Usage: $0 [all|reset|kafka|sql|clear|verify]"
            echo ""
            echo "Commands:"
            echo "  all     - Load SQL + Kafka events (default)"
            echo "  reset   - Clear all data and reload"
            echo "  kafka   - Only publish Kafka events"
            echo "  sql     - Only load SQL seed data"
            echo "  clear   - Clear all data (no reload)"
            echo "  verify  - Check current data counts"
            exit 1
            ;;
    esac
}

main "$@"
