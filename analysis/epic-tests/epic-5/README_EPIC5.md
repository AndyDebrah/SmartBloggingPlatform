Epic 5 - Metrics & Reporting (Quick Start)

Goal
- Capture and report latency, throughput, memory, and thread metrics with reproducible artifacts.

What was added
- Spring Boot Actuator + Micrometer Prometheus metrics exposure.
- `MetricsConfig` registering `TimedAspect` so `@Timed` annotations work.
- Prometheus endpoint exposure in profile properties.
- End-to-end Epic 5 harness: `scripts/epic5_metrics_reporting.ps1`.

Quick validation steps
1. Run Epic 5 harness:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\epic5_metrics_reporting.ps1 -Profile local -Port 8140 -Users 8 -RequestsPerUser 10 -TomcatMaxThreads 8
```

2. Review generated report:

```powershell
Get-Content .\analysis\epic-tests\epic-5\epic-5-metrics-report.md
```

3. Confirm Prometheus snapshot content:

```powershell
Get-Content .\analysis\epic-tests\epic-5\prom-after.txt -TotalCount 40
```

Primary outputs
- `analysis/epic-tests/epic-5/epic-5-metrics-summary.json`
- `analysis/epic-tests/epic-5/epic-5-metrics-report.md`
- `analysis/epic-tests/epic-5/epic-5-metrics-raw.csv`
- `analysis/epic-tests/epic-5/epic-5-metrics-timeline.csv`
- `analysis/epic-tests/epic-5/prom-before.txt`
- `analysis/epic-tests/epic-5/prom-after.txt`
