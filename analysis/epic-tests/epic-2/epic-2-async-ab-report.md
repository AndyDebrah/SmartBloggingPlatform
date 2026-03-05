# Epic 2 Async A/B Comparison Report

Generated: 2026-03-04 11:08:31Z
Load profile: users=12, requestsPerUser=10, tomcatMaxThreads=8

## Per-Mode Metrics
| Mode | Endpoint | Requests | Errors | Avg (ms) | P50 (ms) | P95 (ms) | Max (ms) |
|---|---|---:|---:|---:|---:|---:|---:|
| sync | post_retrieval | 120 | 0 | 221.86 | 176.68 | 499.97 | 546.38 |
| sync | comment_loading | 120 | 0 | 154.86 | 106.26 | 531.08 | 557.39 |
| sync | user_analytics | 120 | 0 | 152.86 | 152 | 208.49 | 238.06 |
| async | post_retrieval | 120 | 0 | 184.7 | 167.03 | 300.57 | 336.43 |
| async | comment_loading | 120 | 0 | 117.42 | 121.09 | 163.05 | 199.18 |
| async | user_analytics | 120 | 0 | 147.79 | 148.23 | 184.21 | 199.24 |

## Async vs Sync (Average Latency)
| Endpoint | Sync Avg (ms) | Async Avg (ms) | Delta (ms) | Improvement % |
|---|---:|---:|---:|---:|
| post_retrieval | 221.86 | 184.7 | -37.16 | 16.75 |
| comment_loading | 154.86 | 117.42 | -37.44 | 24.18 |
| user_analytics | 152.86 | 147.79 | -5.07 | 3.32 |

## Overall Async vs Sync
| Scope | Sync Avg (ms) | Async Avg (ms) | Delta (ms) | Improvement % | Sync P95 (ms) | Async P95 (ms) |
|---|---:|---:|---:|---:|---:|---:|
| All endpoints | 176.53 | 149.97 | -26.56 | 15.05 | 480.78 | 239.6 |
