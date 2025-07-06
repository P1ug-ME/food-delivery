#!/usr/bin/env python3
"""
Load Test for TTB Spark Order Service - 10,000+ orders/day validation

Usage:
  python3 scripts/load_test.py --quick        # 100 orders in 1 minute
  python3 scripts/load_test.py --daily        # 1000 orders in 1 hour  
  python3 scripts/load_test.py --burst --orders 500 --concurrent 25
"""

import asyncio
import aiohttp
import argparse
import json
import random
import time
import statistics
from datetime import datetime
from typing import List, Dict

class OrderLoadTester:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.results: List[Dict] = []
    
    def generate_order_data(self, customer_id: int) -> Dict:
        """Generate realistic order data"""
        menu_items = ["Pad Thai", "Tom Yum Goong", "Green Curry", "Mango Sticky Rice"]
        
        return {
            "customerId": customer_id,
            "merchantId": random.randint(1, 100),
            "paymentMethod": random.choice(["CREDIT_CARD", "BANK_TRANSFER"]),
            "deliveryAddress": f"Building {customer_id}, Bangkok 10110",
            "items": [{
                "menuItemId": random.randint(1, 50),
                "menuItemName": random.choice(menu_items),
                "quantity": random.randint(1, 3),
                "unitPrice": round(random.uniform(80, 350), 2)
            }]
        }
    
    async def create_order(self, session: aiohttp.ClientSession, customer_id: int) -> Dict:
        """Create a single order and measure performance"""
        order_data = self.generate_order_data(customer_id)
        start_time = time.time()
        
        try:
            async with session.post(
                f"{self.base_url}/api/orders",
                json=order_data,
                timeout=aiohttp.ClientTimeout(total=30)
            ) as response:
                end_time = time.time()
                response_time = (end_time - start_time) * 1000
                
                result = {
                    "timestamp": datetime.now().isoformat(),
                    "status_code": response.status,
                    "response_time_ms": response_time,
                    "success": response.status == 201,
                    "customer_id": customer_id
                }
                
                if response.status == 201:
                    data = await response.json()
                    result["order_number"] = data.get("orderNumber")
                else:
                    error_text = await response.text()
                    result["error"] = error_text[:200]
                
                return result
                
        except Exception as e:
            return {
                "timestamp": datetime.now().isoformat(),
                "status_code": 0,
                "response_time_ms": (time.time() - start_time) * 1000,
                "success": False,
                "customer_id": customer_id,
                "error": str(e)[:200]
            }
    
    async def run_load_test(self, duration_seconds: int, target_rps: float):
        """Run load test for specified duration"""
        print(f"🚀 Load Test Starting:")
        print(f"   Duration: {duration_seconds}s ({duration_seconds/60:.1f} min)")
        print(f"   Target Rate: {target_rps} req/s")
        print(f"   Expected Total: {int(duration_seconds * target_rps)} orders")
        print()
        
        connector = aiohttp.TCPConnector(limit=100)
        async with aiohttp.ClientSession(connector=connector) as session:
            start_time = time.time()
            end_time = start_time + duration_seconds
            request_count = 0
            last_report = start_time
            
            while time.time() < end_time:
                batch_start = time.time()
                
                # Calculate batch size for this second
                batch_size = int(target_rps)
                if random.random() < (target_rps - batch_size):
                    batch_size += 1
                
                # Create batch of requests
                tasks = []
                for _ in range(batch_size):
                    customer_id = random.randint(1, 10000)
                    task = asyncio.create_task(self.create_order(session, customer_id))
                    tasks.append(task)
                    request_count += 1
                
                # Execute batch
                batch_results = await asyncio.gather(*tasks, return_exceptions=True)
                
                # Store results
                for result in batch_results:
                    if isinstance(result, dict):
                        self.results.append(result)
                
                # Progress report every 30 seconds
                current_time = time.time()
                if current_time - last_report >= 30:
                    elapsed = current_time - start_time
                    remaining = end_time - current_time
                    current_rate = request_count / elapsed
                    successful = len([r for r in self.results if r.get("success", False)])
                    success_rate = (successful / len(self.results) * 100) if self.results else 0
                    
                    print(f"⏱️  {elapsed:.0f}s elapsed, {remaining:.0f}s remaining")
                    print(f"📊 {request_count} sent, {len(self.results)} completed")
                    print(f"📈 Rate: {current_rate:.2f} req/s, Success: {success_rate:.1f}%")
                    print()
                    last_report = current_time
                
                # Rate limiting
                batch_duration = time.time() - batch_start
                sleep_time = max(0, 1.0 - batch_duration)
                if sleep_time > 0:
                    await asyncio.sleep(sleep_time)
        
        self.analyze_results()
    
    def analyze_results(self):
        """Analyze and report test results"""
        if not self.results:
            print("❌ No results to analyze")
            return
        
        successful = [r for r in self.results if r.get("success", False)]
        failed = [r for r in self.results if not r.get("success", False)]
        response_times = [r["response_time_ms"] for r in successful]
        
        print("\n" + "="*70)
        print("📊 PERFORMANCE TEST RESULTS")
        print("="*70)
        
        # Volume metrics
        total = len(self.results)
        success_count = len(successful)
        success_rate = (success_count / total * 100) if total > 0 else 0
        
        print(f"📈 VOLUME:")
        print(f"   Total Requests: {total:,}")
        print(f"   Successful: {success_count:,}")
        print(f"   Failed: {len(failed):,}")
        print(f"   Success Rate: {success_rate:.2f}%")
        
        # Response time metrics
        if response_times:
            response_times.sort()
            avg = statistics.mean(response_times)
            median = statistics.median(response_times)
            p95 = response_times[int(len(response_times) * 0.95)] if len(response_times) > 20 else response_times[-1]
            
            print(f"\n⏱️  RESPONSE TIMES:")
            print(f"   Average: {avg:.2f}ms")
            print(f"   Median: {median:.2f}ms")
            print(f"   95th Percentile: {p95:.2f}ms")
            print(f"   Min: {min(response_times):.2f}ms")
            print(f"   Max: {max(response_times):.2f}ms")
        
        # Throughput calculation
        if self.results:
            timestamps = [datetime.fromisoformat(r["timestamp"]) for r in self.results]
            if len(timestamps) > 1:
                duration = (max(timestamps) - min(timestamps)).total_seconds()
                throughput = total / duration if duration > 0 else 0
                daily_capacity = throughput * 86400
                
                print(f"\n🚀 THROUGHPUT:")
                print(f"   Test Duration: {duration:.2f}s")
                print(f"   Actual Rate: {throughput:.2f} req/s")
                print(f"   Daily Capacity: {daily_capacity:.0f} orders/day")
        
        # Validation against 10,000+ orders/day requirement
        print(f"\n🎯 VALIDATION (10,000+ orders/day):")
        
        if 'daily_capacity' in locals():
            capacity_ok = "✅" if daily_capacity >= 10000 else "❌"
            print(f"   {capacity_ok} Daily Capacity: {daily_capacity:.0f}/day (need: 10,000+)")
        
        if response_times:
            avg_ok = "✅" if avg < 300 else "❌"
            p95_ok = "✅" if p95 < 500 else "❌"
            print(f"   {avg_ok} Avg Response: {avg:.0f}ms (target: <300ms)")
            print(f"   {p95_ok} P95 Response: {p95:.0f}ms (target: <500ms)")
        
        success_ok = "✅" if success_rate > 99.9 else "❌"
        print(f"   {success_ok} Success Rate: {success_rate:.2f}% (target: >99.9%)")
        
        # Save results
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = f"load_test_results_{timestamp}.json"
        
        with open(filename, 'w') as f:
            json.dump({
                "summary": {
                    "total_requests": total,
                    "success_rate": success_rate,
                    "avg_response_ms": avg if response_times else None,
                    "p95_response_ms": p95 if response_times else None,
                    "daily_capacity": daily_capacity if 'daily_capacity' in locals() else None
                },
                "results": self.results
            }, f, indent=2)
        
        print(f"\n📄 Results saved to: {filename}")

def main():
    parser = argparse.ArgumentParser(description='TTB Spark Order Service Load Test')
    parser.add_argument('--url', default='http://localhost:8080',
                       help='Service URL (default: http://localhost:8080)')
    
    test_group = parser.add_mutually_exclusive_group(required=True)
    test_group.add_argument('--quick', action='store_true',
                           help='Quick test: 100 orders in 1 minute')
    test_group.add_argument('--daily', action='store_true',
                           help='Daily simulation: 1000 orders in 1 hour')
    test_group.add_argument('--burst', action='store_true',
                           help='Burst test: custom orders/duration')
    
    parser.add_argument('--orders', type=int, default=500,
                       help='Orders for burst test (default: 500)')
    parser.add_argument('--duration', type=int, default=300,
                       help='Duration for burst test in seconds (default: 300)')
    
    args = parser.parse_args()
    
    tester = OrderLoadTester(args.url)
    
    print("🍜 TTB Spark Order Service Load Test")
    print("="*50)
    print(f"Target: {args.url}")
    print(f"Start: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    try:
        if args.quick:
            print("🚀 Quick Test: 100 orders in 1 minute")
            asyncio.run(tester.run_load_test(60, 1.67))
        
        elif args.daily:
            print("📅 Daily Simulation: 1000 orders in 1 hour")
            asyncio.run(tester.run_load_test(3600, 0.28))
        
        elif args.burst:
            rps = args.orders / args.duration
            print(f"💥 Burst Test: {args.orders} orders in {args.duration}s ({rps:.2f} req/s)")
            asyncio.run(tester.run_load_test(args.duration, rps))
        
    except KeyboardInterrupt:
        print("\n⚠️  Test interrupted")
        if tester.results:
            tester.analyze_results()
    except Exception as e:
        print(f"\n❌ Test failed: {e}")

if __name__ == "__main__":
    main()
