# Epic 2 Concurrent Comparison Report

Generated: 2026-03-04 10:46:38Z

| Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |
|---|---:|---:|---:|---:|---:|---:|
| post_retrieval | 9 | 0 | 1079.57 | 130.01 | 3475.82 | 3475.82 |
| comment_loading | 9 | 0 | 851.63 | 91.76 | 2686.43 | 2686.43 |
| user_analytics | 9 | 0 | 792.42 | 83.48 | 2515.88 | 2515.88 |

## Comparison vs Epic 1 Baseline
| Endpoint | Baseline Avg (ms) | Epic 2 Avg (ms) | Delta (ms) | Improvement % |
|---|---:|---:|---:|---:|
| post_retrieval | 57.19 | 1079.57 | 1022.38 | -1787.69 |
| comment_loading | 46.91 | 851.63 | 804.72 | -1715.46 |
| user_analytics | 48.85 | 792.42 | 743.57 | -1522.15 |
