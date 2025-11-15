#!/bin/bash

###############################################################################
# Import Sample Jobs
# Loads sample job definitions into the scheduler
###############################################################################

set -e

API_URL="${API_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"

# Get auth token
echo "Authenticating..."
TOKEN=$(curl -sf -X POST "$API_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" \
    2>/dev/null | jq -r '.token // empty')

if [ -z "$TOKEN" ]; then
    echo "WARNING: Authentication failed or not required, proceeding without token"
    TOKEN=""
fi

# Import each job
echo "Importing sample jobs..."
jq -c '.[]' tests/sample-jobs.json | while read job; do
    NAME=$(echo "$job" | jq -r '.name')
    echo "Creating job: $NAME"

    RESPONSE=$(curl -sf -X POST "$API_URL/api/v1/jobs" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$job" 2>/dev/null)

    if [ $? -eq 0 ]; then
        JOB_ID=$(echo "$RESPONSE" | jq -r '.data.id // .id // empty')
        echo "✓ Created: $NAME (ID: $JOB_ID)"
    else
        echo "✗ Failed to create: $NAME"
    fi
done

echo ""
echo "Sample jobs imported successfully!"
echo "View jobs at: $API_URL/swagger-ui.html"
