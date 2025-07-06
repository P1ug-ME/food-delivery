# How to Test 10,000+ Orders/Day Requirement

This guide explains how to validate that the TTB Spark Order Service can handle **10,000+ transactions per day** as required.

## 🎯 Performance Requirements

To support 10,000+ orders/day, the system must achieve:

| Metric | Target | Reasoning |
|--------|--------|-----------|
| **Daily Capacity** | ≥ 10,000 orders/day | Core requirement |
| **Peak Throughput** | ≥ 1.5 req/s | Handle lunch/dinner rush (3x average) |
| **Average Response Time** | < 300ms | Good user experience |
| **95th Percentile Response** | < 500ms | Acceptable for slower requests |
| **Success Rate** | > 99.9% | High reliability |

### Calculation Breakdown:
- **10,000 orders/day** = 416 orders/hour = 6.9 orders/minute = **0.12 orders/second**
- **Peak hours (3x average)**: 0.35 orders/second
- **Flash sales (10x average)**: 1.2 orders/second

## 🖥️ Windows & Linux/Mac Quick Reference

### Prerequisites
- **Python 3.7+** (Install from [python.org](https://www.python.org/downloads/))
- **aiohttp** package

#### Install aiohttp (Windows PowerShell/cmd)
```powershell
pip install aiohttp
```

#### Start the Order Service
- **With Docker Compose (recommended, cross-platform):**
```powershell
docker-compose up -d
```
- **With Gradle (if Java/Gradle installed):**
```powershell
./gradlew bootRun   # Linux/Mac
# OR
.\gradlew.bat bootRun   # Windows
```

## 🚀 Running the 10K Orders Test

### Option 1: Run the Python Load Test Script Directly (Recommended for Windows)

You can run the load test directly with Python on any OS:

#### Quick Validation (100 orders in 1 minute)
```powershell
python scripts/load_test.py --quick
```

#### Daily Simulation (1000 orders in 1 hour)
```powershell
python scripts/load_test.py --daily
```

#### Custom Burst Test
```powershell
python scripts/load_test.py --burst --orders 500 --duration 300
```

> **Note:** You can use `python3` or `python` depending on your system.

### Option 2: Interactive Test Menu (Linux/Mac Bash Only)
If you are on Linux/Mac, you can use the interactive script:
```bash
chmod +x scripts/test_10k_orders.sh
./scripts/test_10k_orders.sh
```
*On Windows, use Option 1 above.*

## 📊 Test Scenarios

### 1. Quick Validation Test (1 minute)
**Purpose**: Verify basic performance and functionality
- **Duration**: 60 seconds
- **Rate**: 1.67 requests/second
- **Total**: ~100 orders
- **Validates**: Response time, error rate, basic throughput

```powershell
python scripts/load_test.py --quick
```

**Expected Results**:
- ✅ Success Rate: > 99%
- ✅ Average Response: < 300ms
- ✅ All orders processed successfully

### 2. Daily Volume Simulation (1 hour)
**Purpose**: Simulate actual daily usage pattern
- **Duration**: 3600 seconds (1 hour)
- **Rate**: 0.28 requests/second  
- **Total**: ~1000 orders (10% of daily target)
- **Validates**: Sustained performance, memory leaks, database performance

```powershell
python scripts/load_test.py --daily
```

**Expected Results**:
- ✅ Daily Capacity: > 10,000 orders/day equivalent
- ✅ Success Rate: > 99.9%
- ✅ Stable performance throughout test

### 3. Peak Load Test (5 minutes)
**Purpose**: Test system under peak restaurant hours
- **Duration**: 300 seconds
- **Rate**: 1.67 requests/second
- **Total**: ~500 orders
- **Validates**: Peak hour performance, resource usage

```powershell
python scripts/load_test.py --burst --orders 500 --duration 300
```

**Expected Results**:
- ✅ Handle peak load without degradation
- ✅ P95 Response Time: < 500ms
- ✅ No timeouts or connection errors

### 4. Stress Test (Optional)
**Purpose**: Find system breaking point
- **Duration**: Variable
- **Rate**: Gradually increase until failure
- **Validates**: Maximum capacity, failure modes

```powershell
python scripts/load_test.py --burst --orders 100 --duration 60   # 1.67 req/s
python scripts/load_test.py --burst --orders 200 --duration 60   # 3.33 req/s  
python scripts/load_test.py --burst --orders 300 --duration 60   # 5.0 req/s
```

## 📝 Notes for Windows Users
- All Python-based tests work on Windows, Mac, and Linux.
- Use `python` instead of `python3` if that's how your system is set up.
- If you see errors about missing packages, run `pip install aiohttp`.
- You can use PowerShell or Command Prompt for all commands above.
- For Docker Compose, use `docker-compose` as shown above.

## 📈 Understanding Results

### Key Metrics to Monitor

#### 1. Daily Capacity Calculation
```
Daily Capacity = (Successful Requests / Test Duration) × 86400 seconds

Example:
- Test: 500 successful requests in 300 seconds
- Throughput: 500/300 = 1.67 req/s
- Daily Capacity: 1.67 × 86400 = 144,288 orders/day ✅
```

#### 2. Response Time Analysis
```
Average Response Time: Mean of all successful requests
95th Percentile: 95% of requests faster than this time
99th Percentile: 99% of requests faster than this time
```

#### 3. Success Rate
```
Success Rate = (Successful Requests / Total Requests) × 100%

Target: > 99.9% (less than 1 error per 1000 requests)
```

### Sample Success Output
```
📊 PERFORMANCE TEST RESULTS
==================================================
📈 VOLUME:
   Total Requests: 1,000
   Successful: 999
   Failed: 1
   Success Rate: 99.90%

⏱️  RESPONSE TIMES:
   Average: 156.23ms
   Median: 142.15ms
   95th Percentile: 287.45ms
   Min: 89.12ms
   Max: 456.78ms

🚀 THROUGHPUT:
   Test Duration: 300.45s
   Actual Rate: 3.33 req/s
   Daily Capacity: 287,712 orders/day

🎯 VALIDATION (10,000+ orders/day):
   ✅ Daily Capacity: 287,712/day (need: 10,000+)
   ✅ Avg Response: 156ms (target: <300ms)
   ✅ P95 Response: 287ms (target: <500ms)
   ✅ Success Rate: 99.90% (target: >99.9%)
```

## 🐳 Production-Like Testing

### With PostgreSQL Database
```powershell
docker-compose up -d
```

## 🔧 Troubleshooting

### Common Issues and Solutions

#### 1. Service Not Responding
```bash
# Check if service is running
curl http://localhost:8080/actuator/health

# If not running, start it
./gradlew bootRun
```

#### 2. Connection Errors
```bash
# Increase connection limits
ulimit -n 4096

# Check database connections
docker exec ttb-postgres psql -U orderuser -d orderdb -c "SELECT count(*) FROM pg_stat_activity;"
```

#### 3. High Response Times
- Check database performance
- Monitor memory usage
- Verify no resource constraints

#### 4. Low Success Rate  
- Check application logs
- Verify database connectivity
- Check for validation errors

### Performance Optimization Tips

1. **Database Tuning**:
   ```sql
   # Check slow queries
   SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;
   ```

2. **JVM Tuning**:
   ```bash
   # Set appropriate heap size
   export JAVA_OPTS="-Xmx1g -Xms512m"
   ./gradlew bootRun
   ```

3. **Connection Pool**:
   ```yaml
   # application.yml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
   ```

## 📋 Test Report Template

### Performance Test Report
```
Date: [Test Date]
Duration: [Test Duration]
Environment: [Local/Docker/Production]

REQUIREMENTS VALIDATION:
✅/❌ Daily Capacity: [X] orders/day (target: 10,000+)
✅/❌ Average Response: [X]ms (target: <300ms)  
✅/❌ P95 Response: [X]ms (target: <500ms)
✅/❌ Success Rate: [X]% (target: >99.9%)

CONCLUSION:
[System meets/does not meet the 10,000+ orders/day requirement]

RECOMMENDATIONS:
- [Any optimization recommendations]
- [Infrastructure scaling suggestions]
```

## 🎯 Success Criteria

The system **PASSES** the 10,000+ orders/day requirement when:

- ✅ **Daily Capacity** ≥ 10,000 orders/day (extrapolated from test results)
- ✅ **Average Response Time** < 300ms
- ✅ **95th Percentile Response Time** < 500ms  
- ✅ **Success Rate** > 99.9%
- ✅ **No memory leaks** during sustained testing
- ✅ **Stable performance** throughout test duration

If all criteria are met, the TTB Spark Order Service successfully satisfies the performance requirements! 🎉

*This guide now supports both Windows and Linux/Mac users for all test scenarios.*
