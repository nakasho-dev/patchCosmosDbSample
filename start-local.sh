#!/bin/bash

# Local Development Startup Script for CosmosDB Migration
echo "=============================================================="
echo "CosmosDB Hour Setting Migration - Local Development Mode"
echo "=============================================================="
echo ""

# Set local profile
export SPRING_PROFILES_ACTIVE=local

# Check if CosmosDB connection info is provided
if [ -z "$COSMOS_DB_URI" ]; then
    echo "⚠️  Using default CosmosDB Emulator settings"
    echo "   URI: https://localhost:8081"
    echo "   Make sure CosmosDB Emulator is running on your machine"
    echo ""
    echo "💡 To use actual CosmosDB, set these environment variables:"
    echo "   export COSMOS_DB_URI=your-cosmos-uri"
    echo "   export COSMOS_DB_KEY=your-cosmos-key"
    echo "   export COSMOS_DB_DATABASE=your-database"
    echo "   export COSMOS_DB_CONTAINER=your-container"
else
    echo "✅ Using provided CosmosDB settings"
    echo "   URI: $COSMOS_DB_URI"
    echo "   Database: ${COSMOS_DB_DATABASE:-testdb}"
    echo "   Container: ${COSMOS_DB_CONTAINER:-users}"
fi

echo ""
echo "🚀 Starting Spring Boot application..."
echo "   Application will be available at: http://localhost:8080"
echo ""
echo "📋 Available API endpoints:"
echo "   POST http://localhost:8080/api/batch/migrate-hour-settings"
echo "        → Start the hour setting migration job"
echo ""
echo "   GET  http://localhost:8080/api/batch/status/{jobId}"
echo "        → Check specific job status"
echo ""
echo "   GET  http://localhost:8080/api/batch/jobs"
echo "        → List all jobs and their executions"
echo ""
echo "=============================================================="
echo ""

# Start the application
./gradlew bootRun
