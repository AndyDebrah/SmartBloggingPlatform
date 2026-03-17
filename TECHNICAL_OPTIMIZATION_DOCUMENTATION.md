# TECHNICAL_OPTIMIZATION_DOCUMENTATION

Project: SmartBloggingPlatform  
Module: Module 8 - Advanced Optimization  
Document Type: Defense-Level Technical Report  
Last Updated: 2026-03-17

## 1. Executive Summary
Document the optimization journey from baseline profiling to validated performance improvement. Explain scope, measurable outcomes, and final engineering decisions.

## 2. Optimization Goals
- Improve responsiveness for high-traffic APIs under concurrent load.
- Increase throughput while controlling CPU and memory utilization.
- Preserve correctness, consistency, and security under concurrency.
- Provide objective evidence (metrics, profiling outputs, comparative tests).

## 3. System Context and Constraints
- Existing architecture summary:
- Security constraints (JWT/auth/authorization invariants):
- Non-functional constraints (latency targets, resource ceilings):
- Backward compatibility constraints:

## 4. Baseline Metrics (Pre-Optimization)
| Metric | Endpoint/Scenario | Baseline Value | Tool | Date | Notes |
|---|---|---|---|---|---|
| P50 Latency | GET `/api/posts?page=0&size=20` | 54.07 ms | epic1_baseline_capture.ps1 | 2026-03-03 | 25 requests, 0 errors |
| P50 Latency | GET `/api/comments/post/1?page=0&size=20` | 44.60 ms | epic1_baseline_capture.ps1 | 2026-03-03 | 25 requests, 0 errors |
| P50 Latency | GET `/api/reviews/post/1/stats` | 45.39 ms | epic1_baseline_capture.ps1 | 2026-03-03 | 25 requests, 0 errors |
| P95 Latency | GET `/api/posts?page=0&size=20` | 63.34 ms | epic1_baseline_capture.ps1 | 2026-03-03 | Highest of target API scenarios |
| P95 Latency | GET `/api/comments/post/1?page=0&size=20` | 51.66 ms | epic1_baseline_capture.ps1 | 2026-03-03 |  |
| P95 Latency | GET `/api/reviews/post/1/stats` | 49.10 ms | epic1_baseline_capture.ps1 | 2026-03-03 |  |
| Throughput (req/s) | Baseline sequential script run | Not primary in Epic 1 baseline | N/A | 2026-03-03 | Measured in Epic 2+ load tests |
| CPU Usage | JVM CPU from JFR CPULoad | user 218.5%, system 57% | JFR + `jfr print` | 2026-03-03 | Includes auth setup overhead |
| Memory Usage | JVM heap used | 123.34 MB | `jcmd GC.heap_info` | 2026-03-03 | Captured near end of run |
| Thread Count/State | JVM thread dump | 33 threads captured | `jcmd Thread.print` | 2026-03-03 | See `epic-1-threaddump.txt` |

## 5. Measurement Methodology
- Workload design:
- Data volume and dataset shape:
- Warm-up strategy:
- Number of runs and averaging method:
- Environment details (machine, JVM, DB, network):
- Threats to validity and mitigation:

## 6. Epic-by-Epic Engineering Record

### 6.1 Epic 1 - Performance Bottleneck Analysis
#### 6.1.1 Concept Explanation (Profiling and Bottleneck Analysis)
What is profiling? Profiling is the process of observing how the program behaves while it runs. Instead of guessing, we measure where time and memory are spent.

Why is it important? Performance optimization without profiling is risky because engineers may optimize the wrong code path. Profiling gives evidence.

What problem does it solve? It identifies slow endpoints, expensive methods, memory-heavy allocations, and thread behavior.

What happens if we ignore it? We may spend time changing code that does not improve user-facing latency, and we may introduce regressions.

How this applies to SmartBloggingPlatform:
- We selected high-traffic baseline API scenarios:
  - post retrieval (`/api/posts`)
  - comment loading (`/api/comments/post/1`)
  - user analytics (`/api/reviews/post/1/stats`)
- We captured latency, CPU, memory, and thread data before Epic 2 changes.

Why this matters:
- Epic 1 establishes the baseline truth.
- Epic 2 to Epic 5 improvements must be compared against this baseline.

#### 6.1.2 Relevant Code Scope and Bottleneck Hypotheses
Files inspected for potential hotspots:
- [src/main/java/com/smartblog/controller/PostController.java](src/main/java/com/smartblog/controller/PostController.java)
- [src/main/java/com/smartblog/application/service/impl/PostServiceImpl.java](src/main/java/com/smartblog/application/service/impl/PostServiceImpl.java)
- [src/main/java/com/smartblog/infrastructure/repository/jpa/PostJpaRepository.java](src/main/java/com/smartblog/infrastructure/repository/jpa/PostJpaRepository.java)
- [src/main/java/com/smartblog/controller/CommentController.java](src/main/java/com/smartblog/controller/CommentController.java)
- [src/main/java/com/smartblog/application/service/impl/CommentServiceImpl.java](src/main/java/com/smartblog/application/service/impl/CommentServiceImpl.java)
- [src/main/java/com/smartblog/application/service/impl/ReviewServiceImpl.java](src/main/java/com/smartblog/application/service/impl/ReviewServiceImpl.java)

Hypotheses before measurements:
- Post listing path may be slower due paging plus DTO hydration and related-entity loading.
- Search/join paths may depend heavily on index quality.
- Repeated query patterns may create higher DB call overhead.

Measured bottleneck focus after baseline:
- Post retrieval had the highest p95 latency among target scenarios (63.34 ms), so it is prioritized for Epic 2+ optimization.

Why this matters:
- Optimization priority is now evidence-based, not intuition-based.

#### 6.1.3 Code Explanation (Before vs After)
Epic 1 did not change business endpoint behavior. It introduced reproducible profiling support and stable runtime configuration.

Change A: Runtime config stability for local profiling.

File: `src/main/resources/application.properties`  
Purpose: ensure `.env` values are consistently loaded for profiling runs.

Before:
```properties
spring.application.name=SmartBloggingPlatform
spring.profiles.active=local
server.port=8080
```

After:
```properties
spring.application.name=SmartBloggingPlatform
spring.profiles.active=local
server.port=8080
spring.config.import=optional:file:.env[.properties]
```

Line-by-line explanation:
- `spring.config.import=optional:file:.env[.properties]`
  - tells Spring to import key/value pairs from `.env`.
  - `optional` prevents startup failure if file is absent.
  - removes manual environment setup drift during repeated profiling sessions.

Performance impact:
- Not a direct latency optimization.
- Indirectly improves profiling reliability by preventing startup/config mismatch.

Security and correctness:
- No security rules changed.
- No endpoint logic changed.
- Only configuration loading path changed.

Trade-off:
- If `.env` has wrong values, app can still start with incorrect settings.
- Mitigation: keep `.env` controlled and documented.

Why this matters:
- Reproducibility is foundational for trustworthy metrics.

Change B: Automated Epic 1 baseline capture script.

File: `scripts/epic1_baseline_capture.ps1`  
Purpose: automate baseline measurement and artifact generation.

Before (manual process):
```powershell
# manual startup, manual request firing, manual JFR/thread dump exports
```

After (automated process):
```powershell
.\scripts\epic1_baseline_capture.ps1 -Profile local -Port 8085 -WarmupIterations 5 -MeasureIterations 25
```

Key implementation behavior explained:
- Loads `.env` and maps DB variables for consistent startup.
- Clears inherited `MAVEN_OPTS` to avoid noisy startup recordings.
- Auto-selects free port if requested port is occupied.
- Starts app, waits for startup confirmation, then starts JFR capture.
- Performs warm-up requests, then measured requests for 3 target scenarios.
- Exports:
  - raw latency CSV
  - JSON summary (p50/p95/avg/min/max/errors)
  - JFR execution and allocation sample extracts
  - thread dump
  - heap info snapshot
  - markdown baseline report

Why this improves engineering quality:
- Removes manual inconsistency.
- Standardizes evidence generation.
- Enables repeatable baseline for fair before/after comparison.

Security and correctness:
- Uses real auth flow to call secured endpoints.
- Does not bypass security filters.
- Does not mutate business data beyond test-auth setup and reads.

Trade-offs:
- Recorded samples can include authentication setup overhead.
- Mitigation: analysis explicitly separates auth setup noise from target endpoint ranking.

Why this matters:
- Examiners expect reproducible method, not one-off measurements.

#### 6.1.4 Output and Metrics Interpretation
Captured outputs:
- `analysis/epic-tests/epic-1/baseline-summary.json`
- `analysis/epic-tests/epic-1/baseline-latency-raw.csv`
- `analysis/epic-tests/epic-1/epic-1-baseline.jfr`
- `analysis/epic-tests/epic-1/jfr-cpu.txt`
- `analysis/epic-tests/epic-1/jfr-alloc.txt`
- `analysis/epic-tests/epic-1/epic-1-threaddump.txt`
- `analysis/epic-tests/epic-1/epic-1-heap-info.txt`
- screenshots:
  - `analysis/epic-tests/epic-1/Screenshots/epic-1-jmc-method-profiling.png`
  - `analysis/epic-tests/epic-1/Screenshots/epic-1-jmc-threads.png`
  - `analysis/epic-tests/epic-1/Screenshots/epic-1-jmc-memory-gc.png`

Metric interpretation guide with our values:
- P50 latency:
  - meaning: typical request time.
  - measured: posts 54.07 ms, comments 44.60 ms, analytics 45.39 ms.
  - implication: normal-case response is acceptable, but not enough alone for performance judgment.
- P95 latency:
  - meaning: worst-case tail for 95% of requests.
  - measured: posts 63.34 ms, comments 51.66 ms, analytics 49.10 ms.
  - implication: post retrieval tail is the highest and should be optimized first.
- CPU (JFR CPULoad):
  - meaning: JVM compute activity while workload runs.
  - measured: user 218.5%, system 57%.
  - interpretation: multi-core utilization, plus startup/auth noise in sample window.
- Memory:
  - meaning: heap consumption footprint under baseline.
  - measured: 123.34 MB (`jcmd GC.heap_info`).
  - implication: no immediate memory pressure signal in baseline.
- Thread dump:
  - meaning: thread states and roles at snapshot time.
  - measured: 33 threads.
  - implication: normal service thread footprint; no deadlock evidence in captured snapshot.

JFR event interpretation:
- Execution samples:
  - represent periodic snapshots of where CPU time is spent.
  - high sample count in one stack suggests hotspot candidate.
- Allocation samples:
  - represent sampled object allocation pressure.
  - useful for identifying memory churn sources.
- High CPU stack traces:
  - indicate methods that frequently appear during sampled execution.
  - must be interpreted with context (startup/auth noise vs steady-state endpoint handling).

Why this matters:
- Each metric answers a different question.
- Together they produce a defensible baseline narrative.

#### 6.1.5 Architecture and Flow Explanation (Epic 1 State)
Text diagram:
```text
Client/Test Script
    -> Security (JWT/Auth)
        -> Controller
            -> Service
                -> Repository/JPA
                    -> MySQL

Parallel observation path:
JFR + jcmd + latency logger -> analysis/epic-tests/epic-1 artifacts
```

Request flow before/after Epic 1:
- Before Epic 1:
  - request flow existed but measurement process was ad-hoc.
- After Epic 1:
  - same functional flow, but with standardized profiling instrumentation and artifacts.

Thread behavior before/after:
- Before: thread behavior not systematically captured.
- After: thread dump and JMC thread timeline captured and documented.

Scalability impact:
- No direct scalability improvement yet.
- Major improvement is observability readiness for safe optimization in upcoming epics.

Why this matters:
- Epic 1 is about seeing clearly before changing behavior.

#### 6.1.6 Trade-offs and Risks (Epic 1)
Risks introduced:
- Measurement contamination by startup/authentication overhead.
- Potential misinterpretation if profiler window is not aligned with measured requests.

Concurrency risks considered:
- No concurrency algorithm changes in Epic 1, so no new race-condition risk introduced.

How risks were mitigated:
- Warm-up and measured phases separated.
- Explicitly documented contaminant stacks (BCrypt during auth setup).
- Multiple evidence types used (latency, JFR, heap, thread dump), not one metric.

Behavior under extreme load:
- Not finalized in Epic 1.
- Full stress and throughput tuning are deferred to Epic 2 and Epic 3 by design.

Future improvements:
- Use separate startup phase and steady-state capture windows.
- Add sustained load test runner for throughput baseline comparability.
- Add endpoint-specific timer instrumentation (Micrometer) in later epic.

Why this matters:
- Good engineers document not only strengths, but also measurement limitations.

#### 6.1.7 Defense Preparation (Examiner Q&A)
Q: Why did you measure P95 and not only average latency?  
A: Average can hide slow tail behavior. P95 reveals near-worst user experience for most requests, which is critical for API performance quality.

Q: Why is post retrieval chosen as first optimization target?  
A: It has the highest measured p95 latency (63.34 ms) among the target baseline scenarios, so optimization impact is likely highest there.

Q: Why does CPU show values above 100%?  
A: JFR CPU can represent multi-core utilization. Over 100% means more than one core equivalent is being used by JVM activity.

Q: What is the difference between execution samples and allocation samples?  
A: Execution samples indicate where CPU time is spent. Allocation samples indicate where object creation pressure occurs.

Q: How did you preserve correctness while profiling?  
A: Endpoint logic was not modified for optimization yet. Only measurement automation/config stabilization were added.

Q: What would happen if Epic 1 were skipped?  
A: Later optimizations would lack trustworthy baseline comparison, making performance claims weak and potentially incorrect.

Common mistakes to avoid in defense:
- Do not claim direct speed improvement in Epic 1; this epic establishes baseline.
- Do not treat average latency as the only metric.
- Do not ignore measurement contamination sources; acknowledge and explain them.

#### 6.1.8 Simplified Summary
Explain like I am 5:
- We first used a stopwatch and camera before trying to make the car faster. Now we know where it is slow.

Explain like I am a junior developer:
- Epic 1 added reliable profiling and baseline capture. We measured key endpoint latencies, CPU, memory, and threads, then identified the first target endpoint based on p95.

Explain like I am a senior examiner:
- Epic 1 delivered a reproducible profiling workflow, multi-dimensional baseline metrics, and evidence-backed prioritization. It established the methodological control required for valid optimization claims in subsequent epics.

#### 6.1.9 Verification
Acceptance criteria mapping:
- AC 1.1.1: Satisfied.
- AC 1.1.2: Satisfied.
- AC 1.1.3: Satisfied.
- AC 1.2.1: Satisfied.
- AC 1.2.2: Satisfied.
- AC 1.2.3: Satisfied.

Reviewer conclusion:
- EPIC 1 COMPLETED AND VERIFIED.

### 6.2 Epic 2 - Asynchronous Programming
#### 6.2.1 Deep Concept Explanation
Synchronous handling means one servlet thread (Tomcat worker) owns the request from controller entry until response write. If service and repository calls take 150 to 300 ms, that same servlet thread is occupied the entire time.

Asynchronous handling in this epic means the controller delegates work to a separate executor thread via `CompletableFuture`, then releases the servlet thread early. The work is still blocking at DB level, but the blocking happens on executor threads, not on Tomcat request threads.

This distinction is critical: Epic 2 is thread decoupling, not non-blocking IO. The JDBC driver and repository calls remain blocking. The gain comes from reducing servlet-thread hold time under contention, which lowers request queue wait and improves tail latency.

Why this matters in a servlet app: Tomcat worker threads are finite. When all workers are busy, new requests queue. Queue time adds directly to latency. Under burst traffic, queue time dominates p95/p99 more than raw service time.

`CompletableFuture` here provides:
- Task submission to the configured executor.
- Completion signal when worker logic finishes.
- Integration with Spring MVC async response dispatch.

It does not magically parallelize DB internals. It reorganizes where waiting happens.

#### 6.2.2 Slow-Motion Execution Walkthrough
Synchronous timeline (example with `server.tomcat.threads.max=8`):
1. Request arrives; `nio-xxxx-exec-n` enters controller.
2. Controller calls service and repository directly.
3. Thread is held during DB waits and mapping work (for example 150 ms).
4. With only 8 workers, request 9 must wait in connector queue.
5. Queue wait accumulates during bursts, so p95 grows quickly.

Asynchronous timeline:
1. Request arrives; servlet thread enters controller.
2. Controller submits supplier to `epic2TaskExecutor`.
3. Servlet thread is released quickly to pick up another request.
4. `epic2-exec-*` thread runs service + repository logic.
5. When future completes, Spring performs async dispatch and writes response.

Impact:
- Queue wait on servlet workers drops because workers are not held for full business duration.
- Tail latency drops because fewer requests wait behind blocked servlet threads.
- Throughput under contention improves until executor/DB become the new bottleneck.

#### 6.2.3 Annotated Code Breakdown
Snippet A - Async controller method (from `PostController`):

```java
private CompletableFuture<ResponseEntity<ApiResponse<List<PostDTO>>>> runAsync(
        java.util.function.Supplier<ResponseEntity<ApiResponse<List<PostDTO>>>> supplier) {
    if (!isAsyncEnabled()) {
        return CompletableFuture.completedFuture(supplier.get());
    }
    return CompletableFuture.supplyAsync(supplier, epic2TaskExecutor);
}
```

Line-by-line:
- Method signature: wraps endpoint response creation in `CompletableFuture` so MVC can use async response flow.
- `Supplier<...> supplier`: captures the original synchronous business lambda.
- `if (!isAsyncEnabled())`: runtime A/B switch for verification and rollback safety.
- `completedFuture(supplier.get())`: executes inline when async is off; preserves existing behavior.
- `supplyAsync(supplier, epic2TaskExecutor)`: runs supplier on dedicated pool, not Tomcat worker.

Threading implication:
- Async off: servlet thread does all work.
- Async on: servlet thread delegates, executor thread performs work.

Performance implication:
- Reduces servlet-thread occupancy under concurrent load.

Trade-off:
- Extra async dispatch overhead can make single-request latency similar or slightly worse in no-contention scenarios.

Security/correctness:
- Response contract is unchanged; same payload type/status generation logic.

Snippet B - Executor bean (from `AsyncConfig`):

```java
@Bean("epic2TaskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix(StringUtils.hasText(threadNamePrefix) ? threadNamePrefix : "epic2-exec-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
    executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

Line-by-line:
- `@Bean("epic2TaskExecutor")`: explicit named executor used by controllers.
- `corePoolSize`: baseline worker count kept alive.
- `maxPoolSize`: upper bound when queue pressure grows.
- `queueCapacity`: bounded backlog between core and max expansion behavior.
- `threadNamePrefix`: makes logs and thread dumps diagnosable (`epic2-exec-*`).
- graceful shutdown lines: reduce dropped in-flight tasks on stop.
- `CallerRunsPolicy`: backpressure strategy; caller executes task when pool+queue are saturated.
- `initialize()`: materializes executor internals.

Threading implication:
- Moves work from container workers to managed application workers.

Performance implication:
- Predictable concurrency envelope and safer overload behavior.

Trade-off:
- Bad sizing can move bottleneck or increase context switching.

Snippet C - JWT async dispatch fix (from `JwtAuthenticationFilter`):

```java
@Override
protected boolean shouldNotFilterAsyncDispatch() {
    // Async controller methods trigger a second ASYNC dispatch.
    // Re-run JWT auth there so SecurityContext is available for completion dispatch.
    return false;
}
```

Line-by-line:
- Override defaults for `OncePerRequestFilter`.
- Returning `false` means filter executes during async dispatch too.
- Ensures `SecurityContext` exists when MVC resumes with async result.

Security implication:
- Prevents false `401` responses on async completion dispatch.

Performance implication:
- Minor extra filter pass; required for correctness.

Trade-off:
- Slight added filter work per async response, acceptable versus broken auth.

#### 6.2.4 Thread Pool Mathematical Behavior
Configured values in this project:
- `corePoolSize = 8`
- `maxPoolSize = 32`
- `queueCapacity = 300`

Submission behavior:
1. If active threads < core (8), create thread immediately.
2. Once core is full, enqueue tasks until queue reaches 300.
3. If queue is full and threads < max (32), create additional threads.
4. If queue is full and threads = max, reject task.
5. Rejection policy is `CallerRunsPolicy`, so submitting thread runs task.

Overload behavior:
- `CallerRunsPolicy` slows producers naturally by making caller pay execution cost.
- This is soft backpressure; it prevents silent task drops.

Mis-sizing effects:
- Core too low: queue fills early, p95 rises from wait time.
- Max too high: more thread contention and DB pressure, potential CPU thrash.
- Queue too large: hides overload temporarily but increases latency and memory footprint.
- Queue too small: frequent fallback to caller thread under spikes.

Defensible sizing logic:
- Start near expected parallel DB capacity.
- Keep queue bounded.
- Validate with p95 and error rate, not average alone.

#### 6.2.5 Deep Metrics Interpretation
Controlled A/B benchmark:
- Sync mode: `app.async.enabled=false`
- Async mode: `app.async.enabled=true`
- Same workload: 12 users x 10 requests x 3 endpoints
- Same servlet constraint: `server.tomcat.threads.max=8`

Results:
- Overall average: `176.53 ms -> 149.97 ms` (15.05% improvement)
- Overall p95: `480.78 ms -> 239.60 ms` (50.16% improvement)

Why p95 improved much more than average:
- Average includes all requests; many requests were never badly queued.
- p95 focuses tail requests where queueing dominates.
- Async primarily attacks queue wait on servlet workers, so tail benefits are disproportionate.

Why analytics endpoint improved less:
- This endpoint already had relatively stable execution in sync mode.
- Lower baseline queue pressure leaves less headroom for async gain.
- Endpoints with larger mapping/query variance usually gain more from queue decoupling.

What CPU likely did:
- Similar or slightly higher scheduling overhead due to extra executor and dispatch steps.
- Better latency despite that overhead because queue wait reduction outweighed dispatch cost.

What likely happens at 2x load:
- Async mode should continue outperforming until executor queue and DB pool saturate.
- After saturation, `CallerRunsPolicy` and DB contention will increase latency in both modes.
- At that stage, Epic 3/4 tuning (thread safety/data path) becomes required.

#### 6.2.6 Defensive Clarifications
- This implementation is not reactive programming.
- JDBC and repository calls are still blocking.
- Improvement comes from servlet-thread decoupling, not non-blocking DB IO.
- Gains are strongest under contention; under single-user load, difference may be small.

#### 6.2.7 Defense Question Deep Dive
Q: If DB is still blocking, why did latency improve?  
A: We reduced servlet-thread queueing. Blocking still exists, but it moved to a separate worker pool. That lowers wait time at the web entry point, especially for tail requests.

Q: Why not use WebFlux now?  
A: WebFlux gives best benefits with a non-blocking end-to-end stack. Current persistence layer is blocking JPA/JDBC. Introducing WebFlux without changing DB access adds complexity without proportional gain for this module scope.

Q: How did you handle thread safety?  
A: Epic 2 changed execution placement, not shared mutable state design. Existing transactional boundaries remain. Thread-safety hardening of shared state is addressed in Epic 3.

Q: Why these pool sizes?  
A: They were chosen to create enough worker headroom above servlet threads while remaining bounded. The choice is validated empirically by lower p95 and zero error rate in controlled A/B testing.

Q: What about `CompletableFuture` error handling?  
A: Current controller lambdas preserve existing exception-to-response behavior where present. For broader resilience, next step is explicit `.exceptionally` or centralized async exception mapping.

Q: What is the scalability limit of this design?  
A: Limit shifts from servlet workers to executor + DB capacity. Once those saturate, queueing returns. This is why Epic 2 is a decoupling step, not final scalability endpoint.

Q: What happens under extreme overload?  
A: Queue fills, then extra threads up to max are used. After max and queue are full, `CallerRunsPolicy` triggers and caller-side slowdown applies. Latency rises but tasks are not silently dropped.

#### 6.2.8 Verification
Acceptance criteria mapping:
- AC 2.1.1: Satisfied (`CompletableFuture`-based async refactor on target endpoints).
- AC 2.1.2: Satisfied (dedicated `ThreadPoolTaskExecutor` with bounded queue and rejection policy).
- AC 2.1.3: Satisfied (system remained responsive in concurrent A/B run with zero errors).
- AC 2.2.1: Satisfied (concurrent requests simulated via `scripts/epic2_async_ab_test.ps1`).
- AC 2.2.2: Satisfied (no data loss/corruption evidence; all requests completed successfully).
- AC 2.2.3: Satisfied (overall average reduced by 15.05% in controlled baseline comparison).

Reviewer conclusion:
- EPIC 2 COMPLETED AND VERIFIED.

### 6.3 Epic 3 - Concurrency and Thread Safety
#### 6.3.1 Problem Analysis and Race Condition Walkthrough
Concurrency focus for Epic 3 was token correctness and executor behavior under load:
- One-time refresh token consumption must never succeed twice.
- Shared token state must stay consistent under cleanup, revoke, and consume racing together.
- Async pool settings must be valid and stable under concurrent traffic.

Race walkthrough before the fix (two-map design):
```text
State:
  mapA: token -> expiry
  mapB: token -> username

T1 (Thread A)                      T2 (Thread B)
1. read mapA[token] != null
                                   1. read mapA[token] != null
2. not expired
                                   2. not expired
3. read+remove mapB[token] => user
                                   3. read+remove mapB[token] => user OR null (timing dependent)
4. remove mapA[token]
                                   4. remove mapA[token]
5. return user                     5. could still return stale success path in split-state timing
```

Core vulnerability:
- Token validity and token owner were split across two structures.
- Concurrent interleavings could observe partially-updated state.
- Under some interleavings, correctness depended on timing, not on atomic semantics.

Race walkthrough after fix (single-map atomic compute):
```text
State:
  map: token -> (username, expiry)

T1 (Thread A)                      T2 (Thread B)
1. compute(token, fn) enters bin lock
2. fn sees valid entry, sets winner=username, returns null (remove)
3. compute exits, returns winner
                                   1. compute(token, fn) runs after T1 update
                                   2. fn sees entry == null
                                   3. returns null winner
Result: exactly one consumer wins.
```

Why deadlock is not expected:
- No nested locking across multiple application locks.
- `ConcurrentHashMap.compute` scopes synchronization to one key/bin operation.
- No blocking waits are introduced inside these critical map operations.

#### 6.3.2 Annotated Code - InMemory Atomic Consume
File: `src/main/java/com/smartblog/auth/InMemoryRefreshTokenService.java`

```java
@Override
public String validateAndConsume(String token) {
    Instant now = Instant.now();
    AtomicReference<String> userRef = new AtomicReference<>(null);
    tokens.compute(token, (k, entry) -> {
        if (entry == null) {
            return null;
        }
        if (now.isAfter(entry.expiry())) {
            return null;
        }
        // One-shot consume: return null to remove entry atomically.
        userRef.set(entry.username());
        return null;
    });
    return userRef.get();
}
```

Line-by-line explanation:
- `Instant now = Instant.now();`
  - Captures evaluation time once so all checks in this call use one timestamp.
- `AtomicReference<String> userRef = ...`
  - Stores winner username from inside lambda to outside method scope safely.
- `tokens.compute(token, (k, entry) -> { ... })`
  - Performs read-modify-write atomically for this key.
  - No separate `get` then `remove` race window.
- `if (entry == null) return null;`
  - Key absent means token already consumed/revoked/expired.
- `if (now.isAfter(entry.expiry())) return null;`
  - Expired token is removed and treated invalid in same atomic operation.
- `userRef.set(entry.username()); return null;`
  - Success path captures username and removes token atomically by returning `null`.
- `return userRef.get();`
  - Exactly one concurrent caller receives username; others receive `null`.

Why this prevents duplicate consume:
- Winner decision and removal happen in one atomic key operation.
- Losers observe post-update state (`entry == null`) and cannot reconstruct success.

Memory visibility:
- `ConcurrentHashMap` operations establish happens-before relations for updates and subsequent reads on the same key.
- `AtomicReference` ensures safe publication from lambda execution to caller thread context.

#### 6.3.3 How `ConcurrentHashMap.compute()` Works and Why Chosen
`compute(key, remappingFunction)` semantics relevant here:
- Atomic per key: read current value, apply function, write/remove result as one logical operation.
- Contention granularity is fine-grained (bin/key-level coordination), not global-map locking.
- Prevents lost-update and check-then-act races common with `get` + `put/remove`.

Why `synchronized` was not chosen:
- A coarse synchronized block around whole map would serialize unrelated keys and reduce concurrency.
- `ConcurrentHashMap` allows high parallelism across different keys.
- Code remains simpler than introducing explicit lock objects and lock ordering rules.

Java Memory Model note:
- CHM update methods provide visibility guarantees: writes done in one successful update become visible to other threads that later read that key.
- This avoids stale reads without manual `volatile` fields for token entries.

#### 6.3.4 Annotated Code - Redis Atomic Consume
File: `src/main/java/com/smartblog/auth/RedisRefreshTokenService.java`

```java
@Override
public String validateAndConsume(String token) {
    String key = "refresh:" + token;
    // Atomic consume avoids race where two threads both read the same refresh token.
    return redis.opsForValue().getAndDelete(key);
}
```

Line-by-line explanation:
- `String key = "refresh:" + token;`
  - Namespaces refresh-token keys consistently.
- `getAndDelete(key);`
  - Single Redis operation, executed server-side atomically.
  - Returns value and removes key in one command boundary.

Protocol-level concurrency reasoning:
- Old pattern (`GET` then `DEL`) was two commands with an inter-command race window.
- With concurrent clients:
  - Client A and B could both `GET` same value before either `DEL`.
  - Both might proceed with refresh flow.
- `GETDEL` removes that window:
  - First client gets value and deletes key.
  - Second client receives null.

#### 6.3.5 Thread Pool Tuning - Deep Interpretation
Evaluated configurations:
- `c4-m16-q150`
- `c8-m32-q300`
- `c12-m48-q500`

Measured summary (all with zero errors):
- `c4-m16-q150`: avg 138.77 ms, p95 288.13 ms, CPU 27.51%, heap 157.46 MB, threads 65
- `c8-m32-q300`: avg 134.39 ms, p95 254.53 ms, CPU 32.14%, heap 140.79 MB, threads 68
- `c12-m48-q500`: avg 143.21 ms, p95 283.71 ms, CPU 32.53%, heap 156.95 MB, threads 75

Why `8/32/300` won:
- Lowest p95 and lowest average across zero-error candidates.
- Enough workers to reduce queue wait, but not so many that scheduling overhead dominates.

Why `12/48/500` degraded:
- More runnable threads increased context switching.
- More worker concurrency amplified contention on shared downstream resources (especially DB pool and DB server).
- Larger queue/worker capacity can hide overload briefly but can increase in-flight task backlog and tail variance.

CPU scheduling interpretation:
- CPU percentage increased from `c4` to `c8` due to more active work and less waiting.
- `c12` kept CPU high but latency worsened, indicating overhead/contestion rather than useful throughput gain.

#### 6.3.6 Memory and Resource Analysis
Observed heap/working-set differences:
- `c8` showed lower heap than `c4` in this run, even with slightly more threads.
- `c12` raised thread count and memory footprint significantly.

Engineering interpretation:
- Heap is influenced by allocation rate, GC timing at sample moment, queue backlog, and transient objects.
- More threads increase native stack reservation and per-thread runtime metadata.
- Larger queue and higher concurrency can increase temporary object retention during bursts.
- Therefore memory should be interpreted with latency and thread count together, not in isolation.

#### 6.3.7 What Could Still Go Wrong
- Token growth risk:
  - If cleanup cadence is too slow under extreme issuance rates, memory pressure can rise.
- Redis outage risk:
  - Refresh/revocation operations may fail or fall back poorly if Redis is unavailable.
- Extreme concurrency:
  - Even atomic token logic cannot prevent downstream bottlenecks (DB saturation, network queueing).
- Scaling limits:
  - Current design is safe and correct, but horizontal scaling and distributed token strategies may be needed at higher scale.

#### 6.3.8 Testing and Evidence
Correctness test:
- `src/test/java/com/smartblog/auth/InMemoryRefreshTokenServiceConcurrencyTest.java`
- Command: `.\mvnw.cmd -Dtest=InMemoryRefreshTokenServiceConcurrencyTest test`
- Result: pass (`2` tests, `0` failures).

Tuning and resource evidence:
- Script: `scripts/epic3_threadpool_tuning.ps1`
- Artifacts:
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-summary.json`
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-report.md`
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-raw.csv`

#### 6.3.9 Advanced Defense Q&A
Q: Why is `compute()` better than `get()` + `remove()` here?  
A: `get` + `remove` has a check-then-act race window. `compute` makes the decision and state transition atomic per key.

Q: Does `ConcurrentHashMap` lock the whole map?  
A: No. Operations coordinate at finer granularity (bin/key path), allowing higher parallelism than a global lock.

Q: Why no explicit `volatile` on token fields?  
A: CHM update/read methods provide required visibility guarantees for map entry publication and observation patterns used here.

Q: Could this design deadlock?  
A: Current logic uses no nested custom locks and no cyclic lock acquisition. Map compute operations are short, key-scoped, and non-blocking from application perspective.

Q: Why is Redis `GETDEL` critical?  
A: Because refresh consume is one-shot semantics. Two-command `GET` then `DEL` can allow double-read race; `GETDEL` makes it one atomic server-side operation.

Q: Why not use a very large thread pool to reduce wait further?  
A: Past the useful point, more threads increase scheduling and contention overhead. Measured data shows `12/48/500` had worse latency than `8/32/300`.

Q: What is the main remaining risk after Epic 3?  
A: Downstream resource saturation (DB/IO) under higher load, which is addressed by Epic 4 data-path optimization and Epic 5 observability hardening.

#### 6.3.10 Verification
Acceptance criteria mapping:
- AC 3.1.1: Satisfied (`ConcurrentHashMap`-based shared token structures and atomic operations).
- AC 3.1.2: Satisfied (concurrency test executed and passed).
- AC 3.1.3: Satisfied (no race or corruption symptoms in tests and load runs).
- AC 3.2.1: Satisfied (multiple pool configurations tested).
- AC 3.2.2: Satisfied (CPU, memory, thread, latency metrics captured per configuration).
- AC 3.2.3: Satisfied (optimal configuration chosen and justified with metrics).

Reviewer conclusion:
- EPIC 3 COMPLETED AND VERIFIED.

### 6.4 Epic 4 - Data and Algorithmic Optimization

#### 6.4.1 Epic 4 Overview
Epic 4 targets data-path efficiency in the SmartBlog backend. After Epic 2 (async request handling) and Epic 3 (thread-pool tuning), the system could handle concurrency, but each request still did more database and mapping work than necessary. Epic 4 reduces that per-request cost using caching, single-query aggregation, and index optimization.

Problem solved:
- Repeated read requests (posts, comments, review stats) were re-executing identical database queries.
- Review statistics required multiple queries and full entity hydration.
- Cold-cache performance still depended on DB query speed and indexes.

Why this epic matters:
- Epic 2 and Epic 3 improved concurrency behavior, but not the cost of each read.
- Epic 4 cuts repeated work at the service and repository layers, improving average and tail latency under load.
- The optimizations are measurable, reversible, and aligned with production-grade backend design.

#### 6.4.2 System Design and Architecture Integration
Epic 4 integrates into the existing layered architecture without altering API contracts:

Flow (unchanged externally, optimized internally):
```text
Client
  -> Controller
     -> Service (cache + aggregation logic)
        -> Repository (optimized queries + indexes)
           -> Database
```

Interaction with Epic 2 async execution:
- Controllers still use `CompletableFuture` with the Epic 2 executor.
- Epic 4 reduces work inside the async task by cutting DB round-trips and redundant hydration.
- Result: lower executor-thread occupancy and improved tail latency under concurrent load.

#### 6.4.3 Key Features Implemented
Feature A: Read-path caching (Caffeine + Spring Cache)
- What it does: Stores hot read results in memory so repeated queries return immediately.
- Why needed: Posts, comments, and review stats are frequently requested with identical parameters.
- How it improves the system: Avoids repeated SQL execution, reduces DB pool pressure, and lowers p95 latency.

Feature B: Single-query aggregation for review statistics
- What it does: Computes average rating and review count in one SQL query.
- Why needed: Previous approach required multiple queries and extra entity hydration.
- How it improves the system: Fewer DB round-trips and lower object allocation cost per request.

Feature C: Index tuning for cold-cache efficiency
- What it does: Adds indexes to common filter and sort paths (author, createdAt, deletedAt, postId).
- Why needed: Cold-cache requests still require fast DB plans.
- How it improves the system: Improves query execution time even when cache misses occur.

Feature D: Targeted post listing optimization (regression fix)
- What it does: Adds caching to post listing and removes redundant author fetching.
- Why needed: Post retrieval initially regressed in the first Epic 4 optimized run.
- How it improves the system: Reduces double hydration and adds a fast hot-path for listing.

#### 6.4.4 Code-Level Explanation (Epic 4 Components Only)

##### 6.4.4.1 Cache Configuration (`CacheConfig`)
File: `src/main/java/com/smartblog/config/CacheConfig.java`
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10));
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        cacheManager.setCacheNames(java.util.List.of(
                "postsPage",
                "postView",
                "postsByAuthor",
                "userById",
                "userByUsername",
                "tags",
                "commentsByPost",
                "reviewsByPost",
                "reviewsByUser",
                "reviewStatsByPost"));
        return cacheManager;
    }
}
```
Line-by-line purpose:
1. `@EnableCaching` activates Spring?s caching interception.
2. `initialCapacity(100)` pre-allocates cache space to reduce rehashing cost.
3. `maximumSize(10_000)` bounds memory growth and prevents unbounded caching.
4. `expireAfterWrite(10m)` limits staleness and memory residency.
5. `setCacheNames(...)` registers all caches used by Epic 4 to avoid runtime cache-name errors.

##### 6.4.4.2 Runtime Optimization Toggle (`OptimizationToggle`)
File: `src/main/java/com/smartblog/config/OptimizationToggle.java`
```java
@Component("optimizationToggle")
public class OptimizationToggle {

    @Value("${app.optimization.caching.enabled:true}")
    private boolean cachingEnabled;

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }
}
```
Line-by-line purpose:
1. `@Component("optimizationToggle")` exposes the toggle as a Spring bean.
2. `@Value(...)` reads runtime property for A/B comparisons.
3. `isCachingEnabled()` is referenced inside cache annotations to turn caching on or off without code changes.

##### 6.4.4.3 Comment Read Caching (`CommentServiceImpl`)
File: `src/main/java/com/smartblog/application/service/impl/CommentServiceImpl.java`
```java
@Transactional(readOnly = true)
@Cacheable(
    value = "commentsByPost",
    key = "#postId + '-' + #page + '-' + #size",
    condition = "@optimizationToggle.cachingEnabled"
)
public Page<CommentDTO> listForPost(long postId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Comment> commentPage = commentRepository.findByPostIdAndDeletedAtIsNull(postId, pageable);
    return commentPage.map(CommentMapper::toDTO);
}
```
Line-by-line purpose:
1. `@Transactional(readOnly = true)` optimizes DB session for read-only operations.
2. `@Cacheable` stores results keyed by postId+page+size.
3. `condition = ...` allows runtime enable/disable of caching.
4. Repository call executes only on cache miss.
5. DTO mapping preserves API contract and avoids exposing entity internals.

Write-path eviction (ensures correctness):
```java
@CacheEvict(value = "commentsByPost", allEntries = true,
            condition = "@optimizationToggle.cachingEnabled")
public boolean edit(long commentId, String content) { ... }
```
Purpose:
- All cached comment pages are cleared after edits/add/remove to avoid stale reads.

##### 6.4.4.4 Review Stats Single-Query + Cache (`ReviewServiceImpl`)
File: `src/main/java/com/smartblog/application/service/impl/ReviewServiceImpl.java`
```java
@Transactional(readOnly = true)
@Cacheable(
    value = "reviewStatsByPost",
    key = "#postId",
    condition = "@optimizationToggle.cachingEnabled"
)
public Map<String, Object> getPostRatingStats(Long postId) {
    Double averageRating;
    long reviewCount;
    if (singleQueryReviewStatsEnabled) {
        var summary = reviewRepository.findPostRatingSummary(postId)
                .orElseThrow(() -> new NotFoundException("Post not found with id: " + postId));
        averageRating = summary.getAverageRating();
        reviewCount = summary.getReviewCount();
    } else {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found with id: " + postId));
        averageRating = reviewRepository.calculateAverageRating(post);
        reviewCount = reviewRepository.countByPostAndDeletedAtIsNull(post);
    }
    Map<String, Object> stats = new HashMap<>();
    stats.put("postId", postId);
    stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
    stats.put("reviewCount", reviewCount);
    return stats;
}
```
Line-by-line purpose:
1. Cache key is only `postId` because stats are not paginated.
2. `singleQueryReviewStatsEnabled` toggles optimized vs legacy path.
3. `findPostRatingSummary(...)` executes a single aggregate SQL query.
4. Fallback path preserves rollback safety (two-query legacy path).
5. Output contract remains stable (`postId`, `averageRating`, `reviewCount`).

Write-path eviction (ensures consistency across three read models):
```java
@Caching(evict = {
    @CacheEvict(value = "reviewStatsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
    @CacheEvict(value = "reviewsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
    @CacheEvict(value = "reviewsByUser", allEntries = true, condition = "@optimizationToggle.cachingEnabled")
})
public ReviewDTO createReview(...) { ... }
```
Purpose:
- One write affects three read views; all must be cleared to avoid stale responses.

##### 6.4.4.5 Post Listing Cache + Redundant Fetch Fix (`PostServiceImpl`)
File: `src/main/java/com/smartblog/application/service/impl/PostServiceImpl.java`
```java
@Timed("posts.service.list")
@Transactional(readOnly = true)
@Cacheable(
    value = "postsPage",
    key = "#page + '-' + #size",
    condition = "@optimizationToggle.cachingEnabled"
)
public Page<PostDTO> list(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> postPage = postRepository.findByDeletedAtIsNull(pageable);
    return mapToDtoPage(postPage);
}
```
Line-by-line purpose:
1. `@Timed` supports Epic 5 metrics tracking but does not change logic.
2. `@Cacheable` stores paged list results for repeated reads.
3. Repository fetches page of posts without redundant eager author loading.
4. `mapToDtoPage` performs a single hydration query for author + tags.

Hydration step (avoids N+1 without duplicate author fetching):
```java
private Page<PostDTO> mapToDtoPage(Page<Post> postPage) {
    List<Long> ids = postPage.getContent().stream().map(Post::getId).toList();
    if (ids.isEmpty()) {
        return postPage.map(PostMapper::toDTO);
    }
    Map<Long, Post> hydratedPosts = postRepository.findWithAuthorAndTagsByIdIn(ids).stream()
            .collect(Collectors.toMap(Post::getId, Function.identity()));
    return postPage.map(post -> PostMapper.toDTO(hydratedPosts.getOrDefault(post.getId(), post)));
}
```
Line-by-line purpose:
1. Extracts IDs for the current page to scope hydration.
2. Prevents N+1 by fetching author + tags in a single query.
3. Maps DTOs from hydrated entities without re-querying.

Write-path eviction (keeps caches accurate):
```java
@Caching(evict = {
    @CacheEvict(value = "postsByAuthor", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
    @CacheEvict(value = "postsPage", allEntries = true, condition = "@optimizationToggle.cachingEnabled")
})
public long createDraft(...) { ... }
```
Purpose:
- Any post write invalidates listing caches to avoid stale data.

##### 6.4.4.6 Index Optimization (Flyway Migration)
File: `src/main/resources/db/migration/V2__performance_indexes.sql`
```sql
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_posts_published_created ON posts(published, created_at);
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_posts_deleted_at ON posts(deleted_at);
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);
```
Line-by-line purpose:
1. `author_id` index speeds author dashboards and filters.
2. `created_at` index speeds ordered listings.
3. `published + created_at` supports filtered sorted reads.
4. Comment indexes speed post and user comment queries.
5. `deleted_at` indexes speed soft-delete filtering.

#### 6.4.5 Integration with Previous Epics
- Epic 1: Provides baseline metrics used for A/B comparison.
- Epic 2: Async execution remains intact; Epic 4 reduces per-task DB work.
- Epic 3: Tuned executor configuration ensures optimized tasks run efficiently under concurrent load.

#### 6.4.6 Performance Considerations
Expected behavior:
- Cache hits reduce DB calls and lower average/p95 latency.
- Cache misses still benefit from index optimization.
- Single-query aggregation cuts DB round-trips for analytics.

Trade-offs:
- Cache eviction `allEntries=true` favors correctness over efficiency.
- Hot-key cache stampede risk exists but is bounded by short DB operations.
- Memory usage increases due to in-process cache; bounded by Caffeine limits.

#### 6.4.7 Error Handling and Edge Cases
- Missing post/user IDs throw `NotFoundException` with clear messages.
- Aggregation handles null averages by returning `0.0`.
- Cache eviction on writes prevents stale data across read models.
- A/B toggles allow rollback without code change.

#### 6.4.8 Testing Strategy
Performance testing:
- Script: `scripts/epic4_data_optimization_compare.ps1`
- Runs baseline vs optimized with identical load profile.

Unit testing:
- `ReviewServiceImplOptimizationTest`
- `Epic4CachingContractTest`

Example scenarios:
- Repeat reads for comments and review stats.
- Cache eviction after create/update/delete.
- Single-query stats correctness.

#### 6.4.9 Challenges and Solutions
Challenge 1: Post retrieval regression in initial Epic 4 run
- Cause: listing path had redundant hydration and no listing cache.
- Solution: added `postsPage` cache and removed redundant author fetch.

Challenge 2: Cache name mismatch causing runtime errors
- Cause: cache names used in annotations were not registered.
- Solution: explicit cache registration in `CacheConfig` and properties.

Challenge 3: Maintaining correctness under concurrency
- Cause: cached data could become stale after writes.
- Solution: explicit write-path eviction for all dependent caches.

#### 6.4.10 Conclusion (Defense-Focused)
Epic 4 reduces per-request database and mapping cost while preserving the async concurrency model from Epic 2. It introduces targeted caching, single-query aggregation, and index optimization to improve both average and tail latency under load. The design decisions favor correctness, measurability, and rollback safety, making the optimization defensible in a final-year backend defense.
### 6.5 Epic 5 - Final System Optimization, Stability, and Production Readiness

#### 6.5.1 Concept Explanation (Beginner Friendly)
Epic 5 is the final hardening stage.  
In simple terms: after making the system faster in Epic 2 to Epic 4, we must prove it stays fast and stable when many requests arrive at the same time.

What Epic 5 tries to achieve:
- Turn optimization into measurable, repeatable evidence.
- Confirm that improvements are not only "one lucky run".
- Prepare the project for production-like monitoring.

Why final optimization is necessary after Epic 1 to Epic 4:
- Epic 1 gave baseline truth.
- Epic 2 improved async request handling.
- Epic 3 improved thread safety and pool tuning.
- Epic 4 improved hot data paths (cache + query optimization).
- Epic 5 validates that all of these work together under stress, with metrics.

Simple defense-ready concepts:
- `System stability`: the app keeps responding correctly under load (no crashes, no error spikes, no data corruption symptoms).
- `Tail latency`: the slow requests near the end of the distribution (for example p95). Users feel this as "sometimes the app is slow."
- `Production readiness`: you can observe behavior in runtime with reliable metrics, not guesses.
- `Performance consistency under load`: response times stay predictable when concurrency increases.

One sentence for defense:
"Epic 5 turned our optimizations into production-style evidence by measuring latency, throughput, memory, and thread behavior during concurrent authenticated traffic."

#### 6.5.2 Problem Analysis
After Epic 4, performance improved overall, but we still had important concerns:

Before Epic 5:
- `post_retrieval` showed regression in the initial Epic 4 optimized run (2026-03-04); this was later resolved in a 2026-03-17 follow-up after adding listing cache and removing redundant author fetch.
- We had endpoint latency numbers, but limited unified runtime observability.
- Previous Prometheus evidence files were invalid HTML captures from a failed attempt.

Why further work was required:
- Database load can still spike under concurrent requests if cache misses occur.
- Thread usage can drift under sustained load even if average latency looks fine.
- Caching has side effects: it helps hot reads, but evictions and cache churn can affect unrelated endpoints.
- Request bottlenecks often appear in p95 before average latency shows problems.

Reasoning narrative:
- A system can look "fast on average" but still fail users during bursts.
- Production systems are judged by consistency and recoverability, not only best-case speed.
- Therefore Epic 5 focused on observability + verification, not just another micro-optimization patch.

#### 6.5.3 Implementation (Code-Level Explanation)

### Change A - Enabled Runtime Metrics Exposure for Production-Like Monitoring
File: `src/main/resources/application.properties`

```properties
# Actuator
management.endpoints.web.exposure.include=health,metrics,info,prometheus
management.endpoint.health.show-details=when-authorized
```

Line-by-line explanation:
- `management.endpoints.web.exposure.include=...prometheus`
  - Exposes actuator metrics and Prometheus scrape endpoint.
  - Needed for external dashboards and script-based metric collection.
- `management.endpoint.health.show-details=when-authorized`
  - Keeps health detail secured, preserving security posture.

Why this improves stability/readiness:
- You cannot operate a production service blindly.
- This change makes runtime behavior measurable and auditable.

### Change B - Registered Timed Aspect So `@Timed` Metrics Actually Record
File: `src/main/java/com/smartblog/config/MetricsConfig.java`

```java
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

Line-by-line explanation:
- `@Configuration`: tells Spring this class provides runtime beans.
- `timedAspect(...)`: creates the AOP component that intercepts methods annotated with `@Timed`.
- `MeterRegistry`: the central metrics sink used by Micrometer.
- `new TimedAspect(registry)`: activates timing instrumentation.

Why this improves performance analysis:
- Without `TimedAspect`, `@Timed` annotations are decorative.
- With it, endpoint/service timings become measurable and defendable.

### Change C - Timed Critical Post Endpoints
File: `src/main/java/com/smartblog/controller/PostController.java`

```java
@GetMapping
@Timed("posts.getAll")
public CompletableFuture<ResponseEntity<ApiResponse<List<PostDTO>>>> getAllPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    return runAsync(() -> {
        var posts = postService.list(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts retrieved successfully", posts.getContent(), PaginationMetadata.from(posts))
        );
    });
}

@GetMapping("/search")
@Timed("posts.search")
public CompletableFuture<ResponseEntity<ApiResponse<List<PostDTO>>>> searchPosts(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    return runAsync(() -> {
        var posts = postService.search(q, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Search results for: " + q, posts.getContent(), PaginationMetadata.from(posts))
        );
    });
}

@GetMapping("/author/{authorId}")
@Timed("posts.byAuthor")
public CompletableFuture<ResponseEntity<ApiResponse<List<PostDTO>>>> getPostsByAuthor(
        @PathVariable Long authorId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    return runAsync(() -> {
        var posts = postService.listByAuthor(authorId, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts by author retrieved", posts.getContent(), PaginationMetadata.from(posts))
        );
    });
}
```

Line-by-line explanation:
- `@Timed("posts.getAll")`, `@Timed("posts.search")`, `@Timed("posts.byAuthor")`
  - Records timing metrics per endpoint label.
- Async return type remains `CompletableFuture<...>`
  - Keeps Epic 2 async behavior while adding measurement.

Why this matters:
- We keep the same API behavior but increase observability.
- This is low-risk and high-value for production diagnostics.

### Change D - Built Reproducible Epic 5 Metrics Harness
File: `scripts/epic5_metrics_reporting.ps1`

```powershell
$beforeHttpCount = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "http.server.requests" -Statistic "COUNT"
$beforeHeapUsedBytes = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.memory.used" -Statistic "VALUE"
$beforeThreads = Get-ActuatorMetricValue -BaseUrl $baseUrl -Headers $headers -MetricName "jvm.threads.live" -Statistic "VALUE"
```

```powershell
$result = Invoke-ConcurrentLoad -Uri $endpointMap[$name] -Token $token -UsersCount $Users -ReqPerUser $RequestsPerUser
```

```powershell
$throughput = [math]::Round(($requestDelta / $durationSec), 2)
```

Line-by-line explanation:
- `Get-ActuatorMetricValue(...)`:
  - Pulls metric snapshots directly from actuator before and after load.
- `Invoke-ConcurrentLoad(...)`:
  - Generates concurrent authenticated traffic against target endpoints.
- `throughput = requestDelta / durationSec`:
  - Estimates request processing rate from measured metric deltas.

Engineering reasoning:
- This script makes the entire Epic 5 process reproducible.
- Reproducibility is critical in a defense because claims must be rerunnable.
- It also reduces manual mistakes and inconsistent test methods.

#### 6.5.4 Architecture-Level Explanation
Epic journey in architecture form:

```text
Baseline System (Epic 1 measurement truth)
    ->
Async Processing (Epic 2: servlet-thread decoupling with CompletableFuture)
    ->
Thread Safety (Epic 3: token-store concurrency correctness + pool tuning)
    ->
Data Optimization (Epic 4: caching + single-query aggregation)
    ->
Final Stability and Performance Readiness (Epic 5: unified metrics + reporting pipeline)
```

How each epic contributed:
- Epic 1 told us where we were slow.
- Epic 2 improved concurrency handling path.
- Epic 3 made concurrency behavior safe and tunable.
- Epic 4 optimized heavy read paths.
- Epic 5 proved the system is measurable, stable, and defendable as a final optimized state.

#### 6.5.5 Performance Testing and Benchmark Results
Test execution method:
- Script: `scripts/epic5_metrics_reporting.ps1`
- Consolidated benchmark report: `analysis/epic-tests/epic-5/epic-5-performance-benchmark-report.md`
- Profile: `local`
- Users: `8`
- Requests per user: `10`
- Tomcat max threads: `8`
- Endpoints:
  - `/api/posts?page=0&size=20`
  - `/api/comments/post/1?page=0&size=20`
  - `/api/reviews/post/1/stats`

Result summary table (Epic 5 run at 2026-03-05 11:57 UTC):

| Metric | Value | Interpretation |
|---|---:|---|
| Total requests | 240 | Full workload executed |
| Error count | 0 | Stable behavior under tested load |
| Overall average latency | 57.12 ms | Good central response speed |
| Overall p95 latency | 102.70 ms | Tail latency remains controlled |
| Estimated throughput | 24.33 req/s | System processes sustained concurrent traffic |
| `http.server.requests` delta | +292 | Confirms request counter growth under load |
| `jvm.memory.used` delta | +33,579,488 bytes | Expected memory growth during active workload |
| `jvm.threads.live` delta | +9 | Thread activity increased but remained stable |

What p95 means (defense-ready):
- p95 means 95% of requests are faster than this value.
- It matters because users complain about slow spikes, not averages.
- A lower p95 means fewer "randomly slow" experiences.

#### 6.5.6 Before vs After System Behavior
Before Epic 5:
- Optimization existed, but operational visibility was incomplete.
- Some cross-run comparison and monitoring evidence was fragmented.
- Invalid Prometheus artifacts reduced confidence in observability claims.

After Epic 5:
- One command generates raw metrics, summary JSON, timeline CSV, and report.
- Runtime behavior (latency, throughput, memory, threads) is measured in one workflow.
- Prometheus snapshots are now valid and reproducible.

Why improvements occurred:
- Instrumentation was connected correctly (`TimedAspect`, actuator exposure).
- Measurements were automated in a single deterministic pipeline.
- Evidence generation moved from ad-hoc to script-driven.

#### 6.5.7 Risks and Trade-Offs
Trade-offs introduced:
- Metrics collection has small runtime overhead.
- Local Caffeine cache is per-instance; cross-node consistency remains limited.
- Throughput here is estimated from metric deltas, not a full distributed load-lab setup.

Scalability limitations:
- Single-instance measurement does not fully represent multi-region production scale.
- DB can still become bottleneck under much larger traffic or write-heavy patterns.
- Cache invalidation strategy may need refinement for very high write rates.

Future improvements:
- Add distributed cache (Redis) for multi-instance coherence.
- Add long-duration soak tests and p99/SLO alert thresholds.
- Add dashboard JSON and alert rules (Grafana/Prometheus stack).
- Add per-endpoint business KPIs (not only technical metrics).

#### 6.5.8 Defense Preparation Notes
Q: Why was Epic 5 necessary?  
A: Because speed improvements without runtime proof are not production-ready. Epic 5 provided that proof.

Q: Biggest optimization improvement?  
A: Unified observability and repeatable verification: one scripted run now proves latency, throughput, memory, and thread behavior.

Q: Why these techniques?  
A: Micrometer + Actuator + Prometheus are standard Spring production tools, and script automation ensures reproducibility.

Q: What next for millions of users?  
A: Distributed cache, horizontal scaling, database read replicas/partitioning, p99 monitoring, and automated SLO-based alerting.

Q: How do you defend p95 in one sentence?  
A: p95 shows near-worst user experience; improving it means fewer users experience random slow responses.

#### 6.5.9 Verification
Acceptance criteria mapping:
- AC 5.1.1: Satisfied.  
  Evidence: `analysis/epic-tests/epic-5/epic-5-metrics-summary.json` includes latency, throughput, memory, and thread metrics.
- AC 5.1.2: Satisfied.  
  Evidence: `prom-before.txt`, `prom-after.txt`, and `epic-5-metrics-timeline.csv` provide visualization/export-ready series.
- AC 5.1.3: Satisfied.  
  Evidence: `scripts/epic5_metrics_reporting.ps1` and documented workflow in this report + protocol log.
- AC 5.2.1: Satisfied.  
  Evidence: `epic-5-metrics-report.md` and `epic-5-performance-benchmark-report.md` plus final metric tables and artifacts.
- AC 5.2.2: Satisfied.  
  Evidence: methodology and run details documented in `OPTIMIZATION_TEST_PROTOCOL_AND_LOG.md`.
- AC 5.2.3: Satisfied.  
  Evidence: authenticated concurrent endpoint test run with `240` requests and `0` errors.

Final statement:
`EPIC 5 COMPLETED AND VERIFIED.`

## 7. Code Changes (Before vs After)
Use this format per change.

### Change Item
- File:
- Purpose:
- Risk level:

Before:
```java
// paste pre-change snippet
```

After:
```java
// paste post-change snippet
```

Impact explanation:

## 8. Concurrency and Threading Deep Explanation
- Conceptual async model used in this project:
- Thread ownership model:
- Queueing model and backpressure behavior:
- Thread pool sizing formula/heuristics:
- What happens if pools are undersized/oversized:

## 9. DSA Optimization Justification
- Sorting/searching/filtering improvements:
- Hashing/indexing choices:
- Time complexity improvement table:

| Component | Before Complexity | After Complexity | Practical Effect |
|---|---|---|---|
|  |  |  |  |

## 10. Performance Trade-offs and Risk Register
| Decision | Benefit | Trade-off | Risk | Mitigation |
|---|---|---|---|---|
|  |  |  |  |  |

## 11. Architectural Diagram (Text Form)
```text
[Client] -> [Security Filter/JWT] -> [Controller] -> [Service]
                                      |              |
                                      |              +-> [Async Executor]
                                      |              +-> [Cache/Index Layer]
                                      v
                                  [Repository/DB]
```

Extend this diagram as implementation evolves.

## 12. Metrics Comparison (Before vs After)
| Metric | Baseline | After Epic 2 | After Epic 3 | After Epic 4 | Final (Epic 5) | Delta |
|---|---|---|---|---|---|---|
| P50 Latency | 45.39-54.07 ms (Epic 1 sequential endpoints) | 146.24 ms overall p50 (Epic 2 async A/B concurrent) | 118.94 ms overall p50 (best Epic 3 tuned config: 8/32/300) | 58.23-59.84 ms (endpoint p50 range from Epic 4 optimized run on 2026-03-17) | 46.98 ms (median of Epic 5 endpoint p50 values) | Final run keeps low median latency on optimized hot reads |
| P95 Latency | up to 63.34 ms (Epic 1 sequential) | 239.60 ms overall p95 (Epic 2 async mode); 480.78 ms sync baseline in same Epic 2 load | 254.53 ms overall p95 during heavier tuning matrix scenario | 102.34 ms overall p95 (Epic 4 optimized run on 2026-03-17) | 102.70 ms overall p95 | Tail latency stabilized; Epic 5 adds observability validation |
| Throughput (req/s) | Not primary in Epic 1 | Higher effective concurrency handling with 0 errors at 360 requests per mode | 0 errors at 360 requests across 3 pool configs; tuned config selected | 0 errors at 600 requests/mode (3 endpoints x 200 each) | 24.33 req/s (Actuator delta method) | Throughput now explicitly measured and reported |
| CPU Usage | user 218.5%, system 57% (startup-influenced) | Not fully isolated in Epic 2; focus was latency and stability | 27.51%-32.53% process CPU utilization across tested configs | Not isolated in Epic 4 script (focus on latency and correctness) | Prometheus/JVM CPU series exposed; dedicated CPU sampling kept in Epic 3 tuning outputs | Epic 5 focuses unified observability pipeline rather than re-running CPU micro-analysis |
| Memory Usage | 123.34 MB heap snapshot | No memory pressure or OOM observed in Epic 2 runs | Heap 140.79-157.46 MB, working set 446.71-484.80 MB across configs | No OOM or instability observed in Epic 4 final run | JVM memory used delta: +33,579,488 bytes during load | Memory trend is now instrumented and reproducible |

## 13. Testing Methodology Summary
- Tools used:
- How tests were executed:
- How outputs were interpreted:
- Confidence level and remaining uncertainty:

## 14. Lessons Learned
- Technical lessons:
- Process lessons:
- Recommended future improvements:

## 15. Update Log
- [x] 2026-03-03 Initial defense template created
- [x] Epic 1 update completed (metrics + bottleneck baseline)
- [x] Epic 2 update completed
- [x] Epic 3 update completed
- [x] Epic 4 update completed
- [x] Epic 5 update completed
