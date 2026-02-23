# Module 6 Performance Report

Date: 2026-02-23  
Project: SmartBloggingPlatform

## Environment
- JVM: OpenJDK 21.0.9
- Spring Boot: 3.2.2
- Test profile DB: H2 (in-memory, transactional tests)
- MySQL CLI availability: not available in this environment (`mysql` command missing), so live MySQL reruns were not possible in this session

## Artifact Sources
- Historical MySQL "before" timings: `analysis/module6/before/*.txt`
- Current measured H2 timings (captured this session): `analysis/module6/after/h2_benchmark_2026-02-23.txt`
- Query plan artifacts already present: `analysis/module6/before/explain*.json`

## Historical MySQL Baseline (Before)
Values below are arithmetic means from files in `analysis/module6/before`.

- Full-text search (`bench_fulltext.txt`): **13849.64 ms**
- LIKE fallback (`bench_like.txt`): **8240.96 ms**
- By author (`bench_by_author.txt`): **9919.42 ms**
- By tag (`bench_by_tag.txt`): **16157.61 ms**
- Aggregation (`bench_aggregation.txt`): **2321.50 ms**

## Current Measured Results (H2, 2026-02-23)
Extracted from `analysis/module6/after/h2_benchmark_2026-02-23.txt`:

- `listByAuthor` cached read path: **cold 138 ms**, **warm 3.8 ms** (97.2% faster warm)
- `findByUsername` cached read path: **cold 28 ms**, **warm 0.2 ms** (99.3% faster warm)
- `getView(postId)` cached read path: **cold 47 ms**, **warm 0.2 ms** (99.6% faster warm)
- Paged `listByAuthor`: **cold 114 ms**, **warm 0.2 ms**
- After write eviction (`publish`), next `listByAuthor`: **29.5 ms** (cache miss expected; confirms eviction path)

## Code-Level Performance Fixes Applied
- Removed dead cache helper pattern and moved eviction to actual write methods:
  - `src/main/java/com/smartblog/application/service/impl/UserServiceImpl.java`
  - `src/main/java/com/smartblog/application/service/impl/PostServiceImpl.java`
- N+1 mitigation:
  - Added `@EntityGraph` for comment/review parent lookups:
    - `src/main/java/com/smartblog/infrastructure/repository/jpa/CommentJpaRepository.java`
    - `src/main/java/com/smartblog/infrastructure/repository/jpa/ReviewJpaRepository.java`
  - For posts, avoided collection-fetch pagination issue by using:
    - paged query with author fetch
    - one bulk hydration query for `author + tags` by page ids
    - files:
      - `src/main/java/com/smartblog/infrastructure/repository/jpa/PostJpaRepository.java`
      - `src/main/java/com/smartblog/application/service/impl/PostServiceImpl.java`

## Security and Data Integrity Fixes (Related to Supervisor Feedback)
- Passwords now hashed with BCrypt on creation:
  - `src/main/java/com/smartblog/application/service/impl/UserServiceImpl.java`
  - `src/main/java/com/smartblog/graphql/UserGraphQLController.java`
- Authentication now validates BCrypt hashes:
  - `src/main/java/com/smartblog/application/service/impl/UserServiceImpl.java`
- Comment auditing fix (`createdAt` auto-population):
  - `src/main/java/com/smartblog/core/model/Comment.java`

## Limitations
- MySQL "after" rerun metrics and fresh `EXPLAIN ANALYZE` captures were not generated in this session because the MySQL CLI is not installed in this environment.
- H2 timings are valid for regression/caching verification but should not be treated as production-equivalent MySQL latencies.

## Recommended Follow-up (MySQL Staging)
1. Install/enable MySQL CLI and run `scripts/mysql/run_benchmarks.ps1` for each target query.
2. Save fresh outputs to `analysis/module6/after/`:
   - `bench_*.txt`
   - `explain_*.json`
   - `explain_analyze_*.json`
3. Add a direct before/after comparison table using the same dataset and instance size.
