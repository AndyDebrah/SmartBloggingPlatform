# Epic 4 Data Optimization Comparison Report

Generated: 2026-03-17 11:36:30Z

| Mode | Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) |
|---|---|---:|---:|---:|---:|---:|
| baseline | post_retrieval | 200 | 0 | 87.62 | 84.47 | 134.49 |
| baseline | comment_loading | 200 | 0 | 82.12 | 68.39 | 175.04 |
| baseline | user_analytics | 200 | 0 | 99.3 | 99.14 | 118.53 |
| optimized | post_retrieval | 200 | 0 | 65.15 | 59.36 | 118.93 |
| optimized | comment_loading | 200 | 0 | 68.02 | 59.84 | 100.7 |
| optimized | user_analytics | 200 | 0 | 63.11 | 58.23 | 93.57 |

## Optimized vs Baseline (Average)
| Endpoint | Baseline Avg (ms) | Optimized Avg (ms) | Delta (ms) | Improvement % |
|---|---:|---:|---:|---:|
| post_retrieval | 87.62 | 65.15 | -22.47 | 25.64 |
| comment_loading | 82.12 | 68.02 | -14.1 | 17.17 |
| user_analytics | 99.3 | 63.11 | -36.19 | 36.45 |

## Overall
| Baseline Avg (ms) | Optimized Avg (ms) | Delta (ms) | Improvement % | Baseline P95 (ms) | Optimized P95 (ms) |
|---:|---:|---:|---:|---:|---:|
| 89.68 | 65.43 | -24.25 | 27.04 | 134.49 | 102.34 |
