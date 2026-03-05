# Epic 5 Metrics Collection and Reporting

Generated: 2026-03-05 11:57:33Z

## Endpoint Latency Summary
| Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |
|---|---:|---:|---:|---:|---:|---:|
| post_retrieval | 80 | 0 | 87.45 | 85.21 | 124.64 | 166.39 |
| comment_loading | 80 | 0 | 37.44 | 37.54 | 50.59 | 54.21 |
| user_analytics | 80 | 0 | 46.47 | 46.98 | 61.13 | 67.85 |

## Runtime Metrics (Actuator)
| Metric | Before | After | Delta |
|---|---:|---:|---:|
| http.server.requests COUNT | 3 | 295 | 292 |
| jvm.memory.used VALUE (bytes) | 283669120 | 317248608 | 33579488 |
| jvm.threads.live VALUE | 31 | 40 | 9 |

## Throughput
- Estimated throughput (request delta / sampling window): **24.33 req/s**

## Cross-Epic Average Latency Comparison
| Stage | Overall Avg Latency (ms) |
|---|---:|
| Epic 1 baseline | 50.98 |
| Epic 2 async mode | 149.97 |
| Epic 3 best tuned config | 134.39 |
| Epic 4 optimized mode | 92.2 |
| Epic 5 current run | 57.12 |

## Evidence Artifacts
- Raw latency: analysis\epic-tests\epic-5\epic-5-metrics-raw.csv
- Summary JSON: analysis\epic-tests\epic-5\epic-5-metrics-summary.json
- Timeline CSV: analysis\epic-tests\epic-5\epic-5-metrics-timeline.csv
- Prometheus before: analysis\epic-tests\epic-5\prom-before.txt
- Prometheus after: analysis\epic-tests\epic-5\prom-after.txt
- App logs: analysis\epic-tests\epic-5\epic-5-app-20260305115703.log, analysis\epic-tests\epic-5\epic-5-app-20260305115703.err.log
