# Epic 1 Baseline Report

Generated: 2026-03-04 08:37:08Z

| Scenario | Requests | Errors | Avg (ms) | p50 (ms) | p95 (ms) | Max (ms) |
|---|---:|---:|---:|---:|---:|---:|
| post_retrieval | 25 | 0 | 57.19 | 54.5 | 76.3 | 83.19 |
| comment_loading | 25 | 0 | 46.91 | 46.66 | 51.43 | 53.73 |
| user_analytics | 25 | 0 | 48.85 | 48.38 | 52.31 | 55.51 |

JFR CPU/Memory Snapshot:
- Avg JVM user CPU (%): 754.4
- Avg JVM system CPU (%): 99
- Peak heap used (MB): 
- Heap used from jcmd GC.heap_info (MB): 105.77

Artifacts:
- analysis\epic-tests\epic-1\epic-1-baseline.jfr
- analysis\epic-tests\epic-1\epic-1-app.log
- analysis\epic-tests\epic-1\epic-1-app.err.log
- analysis\epic-tests\epic-1\jfr-cpu.txt
- analysis\epic-tests\epic-1\jfr-alloc.txt
- analysis\epic-tests\epic-1\epic-1-threaddump.txt
- analysis\epic-tests\epic-1\epic-1-heap-info.txt
- analysis\epic-tests\epic-1\baseline-latency-raw.csv
- analysis\epic-tests\epic-1\baseline-summary.json
