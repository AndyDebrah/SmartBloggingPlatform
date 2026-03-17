# Epic 5 Performance Benchmark Report

Generated: 2026-03-05 11:57 UTC  
Profile: `local`  
Script: `scripts/epic5_metrics_reporting.ps1`

## 1. Workload Configuration

- Users: `8`
- Requests per user: `10`
- Tomcat max threads: `8`
- Endpoints:
  - `GET /api/posts?page=0&size=20`
  - `GET /api/comments/post/1?page=0&size=20`
  - `GET /api/reviews/post/1/stats`

Total planned endpoint calls: `8 * 10 * 3 = 240`

## 2. Endpoint Latency Results

| Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |
|---|---:|---:|---:|---:|---:|---:|
| post_retrieval | 80 | 0 | 87.45 | 85.21 | 124.64 | 166.39 |
| comment_loading | 80 | 0 | 37.44 | 37.54 | 50.59 | 54.21 |
| user_analytics | 80 | 0 | 46.47 | 46.98 | 61.13 | 67.85 |

Overall:
- Total requests: `240`
- Error count: `0`
- Overall average latency: `57.12 ms`
- Overall p95 latency: `102.70 ms`
- Throughput estimate: `24.33 req/s`

## 3. Runtime Metrics (Actuator)

| Metric | Before | After | Delta |
|---|---:|---:|---:|
| http.server.requests (COUNT) | 3 | 295 | +292 |
| jvm.memory.used (bytes) | 283,669,120 | 317,248,608 | +33,579,488 |
| jvm.threads.live | 31 | 40 | +9 |

Interpretation:
- `http.server.requests` increasing confirms the app processed load during the test window.
- Memory increased as expected under traffic and object allocation pressure.
- Live thread growth remained controlled (no abnormal thread explosion observed).

## 4. Comparison Against Epic 4 Optimized Baseline

Reference (Epic 4 optimized):
- Overall avg latency: `92.20 ms`
- Overall p95 latency: `160.12 ms`

Epic 5 run:
- Overall avg latency: `57.12 ms` (`38.05%` lower than Epic 4 optimized)
- Overall p95 latency: `102.70 ms` (`35.86%` lower than Epic 4 optimized)

## 5. Stability Assessment

- No endpoint errors (`0/240` failures).
- No security flow break observed (authenticated benchmark run completed).
- Prometheus snapshots captured before and after load:
  - `analysis/epic-tests/epic-5/prom-before.txt`
  - `analysis/epic-tests/epic-5/prom-after.txt`

## 6. Artifact Index

- Raw latency CSV: `analysis/epic-tests/epic-5/epic-5-metrics-raw.csv`
- Timeline CSV: `analysis/epic-tests/epic-5/epic-5-metrics-timeline.csv`
- Summary JSON: `analysis/epic-tests/epic-5/epic-5-metrics-summary.json`
- Metrics report: `analysis/epic-tests/epic-5/epic-5-metrics-report.md`
- Benchmark report (this file): `analysis/epic-tests/epic-5/epic-5-performance-benchmark-report.md`
