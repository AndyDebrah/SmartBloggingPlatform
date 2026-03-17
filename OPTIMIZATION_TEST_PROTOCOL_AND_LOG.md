# OPTIMIZATION_TEST_PROTOCOL_AND_LOG

Purpose: step-by-step testing protocol and execution log for each epic. This file is mandatory evidence for verification and mark-done decisions.

## Global Test Rules
- Execute tests in the same environment where possible.
- Capture timestamp, commit id, and dataset version for each run.
- Record both expected and actual outputs.
- Explain any deviation and whether it blocks epic closure.
- No epic can be closed without a completed test log section.

## Test Environment Snapshot
- Date:
- Commit ID:
- Java version:
- Spring profile:
- DB version:
- Machine specs:
- Dataset seed file(s):

---

## Epic 1 Test Protocol and Log - Performance Bottleneck Analysis

### Test Steps
1. Start backend with profiling enabled (JFR/VisualVM/JProfiler).
2. Execute baseline API scenarios for post retrieval, comment loading, analytics.
3. Capture CPU, memory, thread states, and latency metrics.
4. Identify top bottleneck methods/endpoints from profiler.
5. Save profiler screenshots and raw exports.

### Standard Command (Project-Specific)
```powershell
.\scripts\epic1_baseline_capture.ps1 -Profile local -Port 8085 -WarmupIterations 5 -MeasureIterations 25
```
Generated outputs:
- `analysis/epic-tests/epic-1/baseline-summary.json`
- `analysis/epic-tests/epic-1/epic-1-baseline-report.md`
- `analysis/epic-tests/epic-1/baseline-latency-raw.csv`
- `analysis/epic-tests/epic-1/epic-1-baseline.jfr`
- `analysis/epic-tests/epic-1/epic-1-threaddump.txt`

### Expected Output
- Baseline latency values for each selected endpoint.
- Baseline CPU/memory/thread utilization values.
- At least one clearly identified bottleneck area with evidence.

### Output Interpretation Guide
- High CPU hotspot means compute-bound bottleneck.
- Long blocked/waiting thread time can indicate lock or IO contention.
- High latency with low CPU may indicate DB/network wait or serialization overhead.

### Execution Log
- Run ID: EPIC1-BASELINE-20260303-1628Z
- Timestamp: 2026-03-03 16:28 UTC
- Commands/collection executed:
  - `.\scripts\epic1_baseline_capture.ps1 -Profile local -Port 8085 -WarmupIterations 5 -MeasureIterations 25`
- Expected output summary:
  - Baseline latency values for post retrieval, comment loading, and user analytics.
  - CPU, memory, and thread evidence captured.
  - Clear bottleneck prioritization candidate.
- Actual output summary:
  - App started on auto-selected port `8087` (8085 was occupied).
  - Latency baseline captured with 25/25 success for each scenario and 0 errors:
    - post_retrieval: avg 55.00 ms, p50 54.07 ms, p95 63.34 ms
    - comment_loading: avg 45.78 ms, p50 44.60 ms, p95 51.66 ms
    - user_analytics: avg 45.28 ms, p50 45.39 ms, p95 49.10 ms
  - CPU and memory captured:
    - JFR CPU: user 218.5%, system 57%
    - Heap used (jcmd): 123.34 MB
  - Thread dump captured (`33` threads listed).
- Deviation analysis:
  - JFR sampling includes auth/register BCrypt cost at startup; this is non-target overhead for Epic 1 endpoint bottleneck ranking.
  - No functional errors occurred during measured API calls.
- Evidence paths:
  - `analysis/epic-tests/epic-1/baseline-summary.json`
  - `analysis/epic-tests/epic-1/epic-1-baseline-report.md`
  - `analysis/epic-tests/epic-1/baseline-latency-raw.csv`
  - `analysis/epic-tests/epic-1/epic-1-baseline.jfr`
  - `analysis/epic-tests/epic-1/jfr-cpu.txt`
  - `analysis/epic-tests/epic-1/jfr-alloc.txt`
  - `analysis/epic-tests/epic-1/epic-1-threaddump.txt`
  - `analysis/epic-tests/epic-1/epic-1-heap-info.txt`
  - `analysis/epic-tests/epic-1/Screenshots/epic-1-jmc-method-profiling.png`
  - `analysis/epic-tests/epic-1/Screenshots/epic-1-jmc-threads.png`
  - `analysis/epic-tests/epic-1/Screenshots/epic-1-jmc-memory-gc.png`
- Pass/Fail: Pass

---

## Epic 2 Test Protocol and Log - Asynchronous Programming

### Test Steps
1. Trigger long-running endpoints under normal load and record latency.
2. Execute concurrent requests (Postman runner/JMeter).
3. Verify response responsiveness under concurrent load.
4. Confirm no data loss/corruption.
5. Compare average and percentile latency with Epic 1 baseline.

### Expected Output
- Reduced average or percentile response time vs baseline.
- Stable responses under concurrent requests.
- No integrity issues in persisted data.

### Output Interpretation Guide
- Lower p95 latency indicates improved tail behavior.
- Increased throughput with stable error rate indicates successful async decoupling.
- Any data mismatch is a release blocker.

### Execution Log
- Run ID: EPIC2-ASYNC-AB-20260304-1108Z
- Timestamp: 2026-03-04 11:08 UTC
- Load profile (users, duration, ramp):
  - users: 12
  - requests per user: 10
  - target endpoints: post retrieval, comment loading, review analytics
  - server constraint: `server.tomcat.threads.max=8` (forces contention to observe async behavior)
- Commands executed:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\epic2_async_ab_test.ps1 -Profile local -Port 8110 -Users 12 -RequestsPerUser 10 -TomcatMaxThreads 8`
- Expected output summary:
  - Async mode should remain stable under concurrency (no 5xx, no auth failures).
  - Async mode should reduce overall average and tail latency versus sync mode under same load.
- Actual output summary:
  - Stability: 360/360 success in sync mode and 360/360 success in async mode.
  - Endpoint avg improvements (async vs sync):
    - post_retrieval: 221.86 ms -> 184.70 ms (16.75% improvement)
    - comment_loading: 154.86 ms -> 117.42 ms (24.18% improvement)
    - user_analytics: 152.86 ms -> 147.79 ms (3.32% improvement)
  - Overall:
    - average latency: 176.53 ms -> 149.97 ms (15.05% improvement)
    - p95 latency: 480.78 ms -> 239.60 ms
- Deviation analysis:
  - First implementation attempt with `Start-Job` produced inflated latency due harness overhead.
  - A runspace-based harness replaced it and produced stable reproducible values.
  - Process cleanup was corrected to terminate full process tree after each mode run.
- Evidence paths:
  - `scripts/epic2_async_ab_test.ps1`
  - `analysis/epic-tests/epic-2/epic-2-async-ab-summary.json`
  - `analysis/epic-tests/epic-2/epic-2-async-ab-report.md`
  - `analysis/epic-tests/epic-2/epic-2-async-ab-raw.csv`
  - `analysis/epic-tests/epic-2/epic-2-sync-app-20260304110735.log`
  - `analysis/epic-tests/epic-2/epic-2-async-app-20260304110803.log`
- Pass/Fail: Pass

---

## Epic 3 Test Protocol and Log - Concurrency and Thread Safety

### Test Steps
1. Execute simultaneous create/update/read flows for posts/comments.
2. Run repeated concurrent test loops to detect race conditions.
3. Monitor logs for concurrency exceptions and inconsistent states.
4. Validate correctness of final stored data set.
5. Compare CPU/memory impact of thread-safe structures.

### Expected Output
- No race-condition symptoms.
- No data corruption or lost updates.
- Acceptable resource overhead after synchronization/thread-safe structure usage.

### Output Interpretation Guide
- Rare intermittent failures still count as failures.
- Consistent data integrity across repeated runs is required for pass.
- Elevated resource usage is acceptable only if justified and documented.

### Execution Log
- Run ID: EPIC3-CONCURRENCY-TUNING-20260304-1347Z
- Timestamp: 2026-03-04 13:47 UTC
- Concurrency scenario:
  - Thread-safety race test:
    - `.\mvnw.cmd -Dtest=InMemoryRefreshTokenServiceConcurrencyTest test`
    - 24 concurrent callers attempt to consume the same refresh token.
  - Thread-pool tuning matrix:
    - `powershell -ExecutionPolicy Bypass -File .\scripts\epic3_threadpool_tuning.ps1 -Profile local -StartPort 8120 -Users 12 -RequestsPerUser 10 -TomcatMaxThreads 8 -ConfigMatrix "4:16:150;8:32:300;12:48:500"`
- Expected output summary:
  - No duplicate refresh-token consumption under concurrency.
  - No errors under concurrent endpoint load across candidate pool configurations.
  - Quantitative CPU/memory/thread metrics collected per configuration.
- Actual output summary:
  - Concurrency test: passed (`2` tests, `0` failures).
  - Tuning matrix completed for all 3 configs with `0` errors in each run.
  - Best configuration by rule (lowest errors -> lowest p95 -> lowest avg): `8/32/300`.
  - Key metrics:
    - c4-m16-q150: avg 138.77 ms, p95 288.13 ms, CPU 27.51%, heap 157.46 MB
    - c8-m32-q300: avg 134.39 ms, p95 254.53 ms, CPU 32.14%, heap 140.79 MB
    - c12-m48-q500: avg 143.21 ms, p95 283.71 ms, CPU 32.53%, heap 156.95 MB
- Deviation analysis:
  - Initial script attempts had PowerShell reserved-variable naming collisions (`$PID/$Pid`) and were corrected.
  - Final run completed cleanly and produced reproducible artifacts.
- Evidence paths:
  - `src/test/java/com/smartblog/auth/InMemoryRefreshTokenServiceConcurrencyTest.java`
  - `scripts/epic3_threadpool_tuning.ps1`
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-summary.json`
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-report.md`
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-raw.csv`
- Pass/Fail: Pass

---

## Epic 4 Test Protocol and Log - Data and Algorithmic Optimization

### Test Steps
1. Execute benchmark scenarios for optimized endpoints.
2. Compare before/after execution times for sorting/search/filter logic.
3. Validate cache/index correctness under reads and updates.
4. Measure latency improvements and hit ratio (if cache used).
5. Confirm functional equivalence of response content.

### Expected Output
- Measurable reduction in endpoint/query latency.
- Correct cache/index behavior without stale critical data.
- Demonstrable complexity/performance improvement with evidence.

### Output Interpretation Guide
- Improvement must be consistent across multiple runs.
- Faster but incorrect results are a hard fail.
- Cache gains without invalidation correctness are not accepted.

### Execution Log
- Run ID: EPIC4-DATA-OPT-20260304-1435Z
- Timestamp: 2026-03-04 14:35 UTC
- Benchmark scenario:
  - Baseline mode:
    - `app.optimization.caching.enabled=false`
    - `app.optimization.reviewStats.singleQuery=false`
  - Optimized mode:
    - `app.optimization.caching.enabled=true`
    - `app.optimization.reviewStats.singleQuery=true`
  - Same load in both modes: users `10`, requests per user `20`, `server.tomcat.threads.max=8`
- Command executed:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\epic4_data_optimization_compare.ps1 -Profile local -Port 8130 -Users 10 -RequestsPerUser 20 -TomcatMaxThreads 8`
- Expected output summary:
  - Improved read latency on optimized data paths.
  - No errors and no correctness regression.
  - Before/after comparison table for defense evidence.
- Actual output summary:
  - 0 errors in baseline and optimized runs.
  - Overall average latency improved: `119.67 ms -> 92.20 ms` (`22.95%`).
  - Overall p95 improved: `188.13 ms -> 160.12 ms`.
  - Endpoint improvements:
    - comment_loading avg: `108.62 ms -> 71.61 ms` (`34.07%`)
    - user_analytics avg: `137.19 ms -> 64.46 ms` (`53.01%`)
  - Note: post_retrieval regressed (`113.2 ms -> 140.52 ms`) and is flagged for future tuning.
- Deviation analysis:
  - First Epic 4 run failed with `500` due missing cache names in `CacheConfig`; fixed by adding caches and runtime caching toggle.
  - Final run is the accepted evidence set.
- Evidence paths:
  - `scripts/epic4_data_optimization_compare.ps1`
  - `analysis/epic-tests/epic-4/epic-4-optimization-summary.json`
  - `analysis/epic-tests/epic-4/epic-4-optimization-report.md`
  - `analysis/epic-tests/epic-4/epic-4-optimization-raw.csv`
  - `src/test/java/com/smartblog/application/service/ReviewServiceImplOptimizationTest.java`
  - `src/test/java/com/smartblog/config/Epic4CachingContractTest.java`
  - `src/test/java/com/smartblog/config/AsyncConfigTest.java`
  - Test command:
    - `.\mvnw.cmd -q "-Dtest=AsyncConfigTest,ReviewServiceImplOptimizationTest,Epic4CachingContractTest" test`
- Pass/Fail: Pass

### Follow-up Run (Post Retrieval Regression Fix)
- Run ID: EPIC4-POST-RETRIEVAL-FIX-20260317-1136Z
- Timestamp: 2026-03-17 11:36 UTC
- Change summary before run:
  - Added caching for post listing (`postsPage`) with eviction on post writes.
  - Removed redundant author fetch on `findByDeletedAtIsNull` to avoid duplicate hydration.
  - Preserved N+1 avoidance via `findWithAuthorAndTagsByIdIn` hydration query.
- Command executed:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\epic4_data_optimization_compare.ps1 -Profile local -Port 8130 -Users 10 -RequestsPerUser 20 -TomcatMaxThreads 8`
- Actual output summary:
  - 0 errors in baseline and optimized runs.
  - Overall average latency improved: `89.68 ms -> 65.43 ms` (`27.04%`).
  - Overall p95 improved: `134.49 ms -> 102.34 ms`.
  - Endpoint improvements:
    - post_retrieval avg: `87.62 ms -> 65.15 ms` (`25.64%`)
    - comment_loading avg: `82.12 ms -> 68.02 ms` (`17.17%`)
    - user_analytics avg: `99.30 ms -> 63.11 ms` (`36.45%`)
  - Regression status:
    - post_retrieval regression resolved as of this run.
- Evidence paths:
  - `analysis/epic-tests/epic-4/epic-4-optimization-summary.json`
  - `analysis/epic-tests/epic-4/epic-4-optimization-report.md`
  - `analysis/epic-tests/epic-4/epic-4-optimization-raw.csv`
  - `analysis/epic-tests/epic-4/epic-4-baseline-app-20260317113527.log`
  - `analysis/epic-tests/epic-4/epic-4-optimized-app-20260317113558.log`
- Pass/Fail: Pass

---

## Epic 5 Test Protocol and Log - Metrics Collection and Reporting

### Test Steps
1. Validate metric capture for latency, throughput, memory, and thread behavior.
2. Run stress scenarios and confirm metrics update in near real-time.
3. Export/report final before-vs-after comparison artifacts.
4. Confirm documentation references all evidence correctly.
5. Review total optimization impact and residual risks.

### Expected Output
- Reliable metrics stream across test scenarios.
- Complete final report with screenshots/tables/comparisons.
- Traceable methodology from baseline to final outcome.

### Output Interpretation Guide
- Missing or inconsistent metric series makes conclusions unreliable.
- Final evidence must be reproducible from logged steps.
- If methodology is unclear, epic remains blocked.

### Execution Log
- Run ID: EPIC5-METRICS-REPORTING-20260305-1157Z
- Timestamp: 2026-03-05 11:57 UTC
- Scenario executed:
  - Optimized runtime mode:
    - `app.async.enabled=true`
    - `app.optimization.caching.enabled=true`
    - `app.optimization.reviewStats.singleQuery=true`
  - Load profile:
    - users: `8`
    - requests per user: `10`
    - target endpoints: `post_retrieval`, `comment_loading`, `user_analytics`
  - Command:
    - `powershell -ExecutionPolicy Bypass -File .\scripts\epic5_metrics_reporting.ps1 -Profile local -Port 8140 -Users 8 -RequestsPerUser 10 -TomcatMaxThreads 8`
- Expected output summary:
  - Latency + throughput + memory + thread metrics are captured with reproducible artifacts.
  - Prometheus snapshot before/after load is captured.
  - Final report links methodology and evidence paths.
- Actual output summary:
  - Requests: `240`, errors: `0`.
  - Endpoint averages:
    - post_retrieval: `87.45 ms`
    - comment_loading: `37.44 ms`
    - user_analytics: `46.47 ms`
  - Overall:
    - average latency: `57.12 ms`
    - p95 latency: `102.70 ms`
    - estimated throughput: `24.33 req/s`
  - Runtime metrics delta (Actuator):
    - `http.server.requests`: `+292`
    - `jvm.memory.used`: `+33,579,488 bytes`
    - `jvm.threads.live`: `+9`
  - Prometheus scrape snapshots captured successfully before and after load.
- Deviation analysis:
  - Existing historical `prom-before.txt` and `prom-after.txt` in Epic 5 folder were invalid OAuth HTML responses from a previous attempt.
  - They were replaced by valid Prometheus scrape outputs via authenticated `/actuator/prometheus`.
- Evidence paths:
  - `scripts/epic5_metrics_reporting.ps1`
  - `analysis/epic-tests/epic-5/epic-5-metrics-summary.json`
  - `analysis/epic-tests/epic-5/epic-5-metrics-report.md`
  - `analysis/epic-tests/epic-5/epic-5-metrics-raw.csv`
  - `analysis/epic-tests/epic-5/epic-5-metrics-timeline.csv`
  - `analysis/epic-tests/epic-5/prom-before.txt`
  - `analysis/epic-tests/epic-5/prom-after.txt`
  - `analysis/epic-tests/epic-5/epic-5-app-20260305115703.log`
  - `analysis/epic-tests/epic-5/epic-5-app-20260305115703.err.log`
- Pass/Fail: Pass

---

## Reviewer Decision Ledger
| Epic | Test Log Complete | Critical Issues | Decision | Notes |
|---|---|---|---|---|
| Epic 1 | Yes | None | Approved | EPIC 1 COMPLETED AND VERIFIED |
| Epic 2 | Yes | None (after harness correction) | Approved | EPIC 2 COMPLETED AND VERIFIED |
| Epic 3 | Yes | None (after script variable-name fixes) | Approved | EPIC 3 COMPLETED AND VERIFIED |
| Epic 4 | Yes | None (after cache-registration fix) | Approved | EPIC 4 COMPLETED AND VERIFIED |
| Epic 5 | Yes | None | Approved | EPIC 5 COMPLETED AND VERIFIED |
