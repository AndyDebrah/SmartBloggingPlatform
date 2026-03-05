# Epic 4 Data Optimization Comparison Report

Generated: 2026-03-04 14:35:33Z

| Mode | Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) |
|---|---|---:|---:|---:|---:|---:|
| baseline | post_retrieval | 200 | 0 | 113.2 | 109.6 | 155.54 |
| baseline | comment_loading | 200 | 0 | 108.62 | 92.43 | 223.97 |
| baseline | user_analytics | 200 | 0 | 137.19 | 134.96 | 197.03 |
| optimized | post_retrieval | 200 | 0 | 140.52 | 122.66 | 201.25 |
| optimized | comment_loading | 200 | 0 | 71.61 | 67.95 | 118.21 |
| optimized | user_analytics | 200 | 0 | 64.46 | 60.84 | 103.98 |

## Optimized vs Baseline (Average)
| Endpoint | Baseline Avg (ms) | Optimized Avg (ms) | Delta (ms) | Improvement % |
|---|---:|---:|---:|---:|
| post_retrieval | 113.2 | 140.52 | 27.32 | -24.13 |
| comment_loading | 108.62 | 71.61 | -37.01 | 34.07 |
| user_analytics | 137.19 | 64.46 | -72.73 | 53.01 |

## Overall
| Baseline Avg (ms) | Optimized Avg (ms) | Delta (ms) | Improvement % | Baseline P95 (ms) | Optimized P95 (ms) |
|---:|---:|---:|---:|---:|---:|
| 119.67 | 92.2 | -27.47 | 22.95 | 188.13 | 160.12 |
