# Epic 3 Thread Pool Tuning Report

Generated: 2026-03-04 13:47:03Z
Load profile: users=12, requestsPerUser=10, tomcatMaxThreads=8

| Config | Core | Max | Queue | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | CPU % | Heap MB | WorkingSet MB | Threads |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| c4-m16-q150 | 4 | 16 | 150 | 360 | 0 | 138.77 | 121.15 | 288.13 | 27.51 | 157.46 | 446.71 | 65 |
| c8-m32-q300 | 8 | 32 | 300 | 360 | 0 | 134.39 | 118.94 | 254.53 | 32.14 | 140.79 | 472.28 | 68 |
| c12-m48-q500 | 12 | 48 | 500 | 360 | 0 | 143.21 | 121.34 | 283.71 | 32.53 | 156.95 | 484.8 | 75 |

## Selected Optimal Configuration
- Config: c8-m32-q300
- Core/Max/Queue: 8/32/300
- Selection rule: lowest error count -> lowest p95 -> lowest avg.
