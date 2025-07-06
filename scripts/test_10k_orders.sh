#!/bin/bash

# TTB Spark Order Service - 10,000+ Orders/Day Test Script
# This script validates the performance requirement

set -e

SERVICE_URL="http://localhost:8080"
PYTHON_SCRIPT="scripts/load_test.py"

echo "🍜 TTB Spark Food Delivery - Performance Validation"
echo "=================================================="
echo "Testing requirement: 10,000+ orders per day"
echo "Service URL: $SERVICE_URL"
echo ""

# Check if service is running
echo "🔍 Checking if service is running..."
if curl -f -s $SERVICE_URL/actuator/health > /dev/null; then
    echo "✅ Service is running"
else
    echo "❌ Service is not running at $SERVICE_URL"
    echo "Please start the service with: ./gradlew bootRun"
    exit 1
fi

echo ""
echo "📋 Available Tests:"
echo "1. Quick Validation (100 orders in 1 minute)"
echo "2. Daily Simulation (1000 orders in 1 hour)" 
echo "3. Burst Test (500 orders in 5 minutes)"
echo "4. All Tests (recommended)"
echo ""

read -p "Select test (1-4): " choice

case $choice in
    1)
        echo "🚀 Running Quick Validation Test..."
        python3 $PYTHON_SCRIPT --quick --url $SERVICE_URL
        ;;
    2)
        echo "📅 Running Daily Simulation Test..."
        echo "⚠️  This will take 1 hour to complete"
        read -p "Continue? (y/N): " confirm
        if [[ $confirm == [yY] ]]; then
            python3 $PYTHON_SCRIPT --daily --url $SERVICE_URL
        else
            echo "Test cancelled"
        fi
        ;;
    3)
        echo "💥 Running Burst Test..."
        python3 $PYTHON_SCRIPT --burst --orders 500 --duration 300 --url $SERVICE_URL
        ;;
    4)
        echo "🏃 Running All Tests..."
        echo ""
        
        echo "=== TEST 1: Quick Validation ==="
        python3 $PYTHON_SCRIPT --quick --url $SERVICE_URL
        echo ""
        
        echo "=== TEST 2: Burst Test ==="
        python3 $PYTHON_SCRIPT --burst --orders 500 --duration 300 --url $SERVICE_URL
        echo ""
        
        echo "📊 Summary: Both tests completed successfully!"
        echo "💡 For full daily simulation, run: $0 and select option 2"
        ;;
    *)
        echo "Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""
echo "✅ Performance testing completed!"
echo ""
echo "📈 Results Analysis:"
echo "- Check the generated JSON files for detailed metrics"
echo "- Daily capacity should be >= 10,000 orders/day"
echo "- Average response time should be < 300ms"
echo "- Success rate should be > 99.9%"
echo ""
echo "🐳 For production-like testing with PostgreSQL:"
echo "   docker-compose up -d"
echo "   $0"
