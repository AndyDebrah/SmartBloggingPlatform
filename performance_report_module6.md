# Module 6 — Performance Report (template)

This file is a template to collect EXPLAIN results and before/after timings for Module 6 query optimizations.

How to run (Windows CMD / PowerShell)

1) Run EXPLAIN for each query in `scripts/mysql/explain_queries.sql` using the MySQL client. Example:

```bash
mysql -u <user> -p -D <database> -e "EXPLAIN FORMAT=JSON SELECT ..." > explain_query1.json
```

2) Record wall-clock timings using a client or small script. Example using the MySQL client (measure externally) or use a small Java program to execute and measure time.

Template sections

- Environment
  - MySQL version: 
  - Dataset size (rows): 
  - Instance size / hardware: 

- Query: Full-text search
  - EXPLAIN (attached): `explain_fulltext.json`
  - Time before (ms): 
  - Time after (ms): 
  - Indexes applied: 

H2 (test profile) measured samples
 - `listByAuthor` (paged): COLD = 155 ms, WARM = 4.4 ms (approx)
 - `findByUsername`: COLD = 19 ms, WARM = 0.1 ms (approx)
 - `getView` (single post): COLD = 17 ms, WARM = 0.1 ms (approx)

These H2 numbers were collected via the `CachingIntegrationTest` and `EvictionAndPagedBenchmarkTest` in the test profile. Use these as local reference; final production numbers must be collected against MySQL staging (see below).

- Query: Title/content LIKE fallback
  - EXPLAIN (attached): 
  - Time before (ms): 
  - Time after (ms): 
  - Notes: 

- Query: Find by author id
  - EXPLAIN: 
  - Time before (ms): 
  - Time after (ms): 

- Query: Find by tag
  - EXPLAIN: 
  - Time before (ms): 
  - Time after (ms): 

- Recommendations / index changes applied
  - Migration file(s) added: `src/main/resources/db/migration/V7__add_fulltext_and_indexes.sql`
  - Additional migration(s) proposed: 

---

## Reproducible measurement strategy (practical guide)

Purpose: record BEFORE and AFTER execution times and plan metadata for the key queries used by Module 6 so we can show measurable improvement after index changes.

Overview (steps)
1. Prepare environment metadata (MySQL version, instance size, dataset row counts). Record these in the `Environment` section above.
2. For each query below run:
   - `EXPLAIN FORMAT=JSON` (optimizer estimate)
   - `EXPLAIN ANALYZE FORMAT=JSON` (actual runtime & rows — MySQL 8+)
   - Repeated measured runs of `SELECT SQL_NO_CACHE ...` to capture wall-clock execution times (warm and cold cache measurements).
3. Save all JSON outputs and measured timings as files and fill the template section above.

Important notes:
- `EXPLAIN ANALYZE` actually executes the query and returns runtime stats — use it only on staging data.
- For cold-cache measurements you must clear the buffer pool (restart MySQL) or run on a freshly started instance; otherwise treat the first measured run as the cold run.
- Use `SQL_NO_CACHE` to avoid MySQL query cache (if present) and measure raw execution time.

SQL templates (replace placeholders with values)

- Full-text search (MATCH...AGAINST)

EXPLAIN (estimates):
```
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
WHERE MATCH(p.title, p.content) AGAINST('keyword' IN NATURAL LANGUAGE MODE)
LIMIT 10;
```

EXPLAIN ANALYZE (actual runtime, MySQL 8+):
```
EXPLAIN ANALYZE FORMAT=JSON
SELECT p.*
FROM post p
WHERE MATCH(p.title, p.content) AGAINST('keyword' IN NATURAL LANGUAGE MODE)
LIMIT 10;
```

Measured run (no cache):
```
SELECT SQL_NO_CACHE p.id FROM post p
WHERE MATCH(p.title, p.content) AGAINST('keyword' IN NATURAL LANGUAGE MODE)
LIMIT 10;
```

- LIKE fallback (title/content)

EXPLAIN FORMAT=JSON
```
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
WHERE p.title LIKE CONCAT('%', 'keyword', '%') OR p.content LIKE CONCAT('%', 'keyword', '%')
LIMIT 10;
```

EXPLAIN ANALYZE FORMAT=JSON
```
EXPLAIN ANALYZE FORMAT=JSON
SELECT p.*
FROM post p
WHERE p.title LIKE CONCAT('%', 'keyword', '%') OR p.content LIKE CONCAT('%', 'keyword', '%')
LIMIT 10;
```

Measured run (no cache):
```
SELECT SQL_NO_CACHE p.id FROM post p
WHERE p.title LIKE CONCAT('%', 'keyword', '%') OR p.content LIKE CONCAT('%', 'keyword', '%')
LIMIT 10;
```

- Find by author

EXPLAIN FORMAT=JSON
```
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
WHERE p.author_id = 123 AND p.deleted_at IS NULL
ORDER BY p.created_at DESC
LIMIT 10;
```

EXPLAIN ANALYZE FORMAT=JSON
```
EXPLAIN ANALYZE FORMAT=JSON
SELECT p.*
FROM post p
WHERE p.author_id = 123 AND p.deleted_at IS NULL
ORDER BY p.created_at DESC
LIMIT 10;
```

Measured run (no cache):
```
SELECT SQL_NO_CACHE p.id FROM post p
WHERE p.author_id = 123 AND p.deleted_at IS NULL
ORDER BY p.created_at DESC
LIMIT 10;
```

- Find by tag (join)

EXPLAIN FORMAT=JSON
```
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
JOIN post_tags pt ON p.id = pt.post_id
JOIN tag t ON pt.tag_id = t.id
WHERE t.name = 'sometag' AND p.deleted_at IS NULL
LIMIT 10;
```

EXPLAIN ANALYZE FORMAT=JSON
```
EXPLAIN ANALYZE FORMAT=JSON
SELECT p.*
FROM post p
JOIN post_tags pt ON p.id = pt.post_id
JOIN tag t ON pt.tag_id = t.id
WHERE t.name = 'sometag' AND p.deleted_at IS NULL
LIMIT 10;
```

Measured run (no cache):
```
SELECT SQL_NO_CACHE p.id
FROM post p
JOIN post_tags pt ON p.id = pt.post_id
JOIN tag t ON pt.tag_id = t.id
WHERE t.name = 'sometag' AND p.deleted_at IS NULL
LIMIT 10;
```

- Aggregation (top-rated posts)

EXPLAIN FORMAT=JSON
```
EXPLAIN FORMAT=JSON
SELECT p.id, AVG(r.rating) as avg_rating
FROM post p
LEFT JOIN review r ON r.post_id = p.id
GROUP BY p.id
ORDER BY avg_rating DESC
LIMIT 10;
```

EXPLAIN ANALYZE FORMAT=JSON
```
EXPLAIN ANALYZE FORMAT=JSON
SELECT p.id, AVG(r.rating) as avg_rating
FROM post p
LEFT JOIN review r ON r.post_id = p.id
GROUP BY p.id
ORDER BY avg_rating DESC
LIMIT 10;
```

Measured run (no cache):
```
SELECT SQL_NO_CACHE p.id, AVG(r.rating)
FROM post p
LEFT JOIN review r ON r.post_id = p.id
GROUP BY p.id
ORDER BY AVG(r.rating) DESC
LIMIT 10;
```

Windows CMD / PowerShell commands (exact)

- Capture EXPLAIN (optimizer estimate) to JSON file (CMD):
```
mysql -u <user> -p -D <database> -e "EXPLAIN FORMAT=JSON SELECT ..." > explain_query.json
```

- Capture EXPLAIN ANALYZE (actual runtime; MySQL 8+) to JSON (CMD):
```
mysql -u <user> -p -D <database> -e "EXPLAIN ANALYZE FORMAT=JSON SELECT ..." > explain_analyze_query.json
```

- Measure execution time (repeat N runs and compute average) using PowerShell (CMD-compatible):
Replace `<user>`, `<pass>`, `<db>` and the SQL between the quotes. This runs the `mysql` client N times and prints per-run ms and average.
```
powershell -Command "$runs=1..5; $results=@(); foreach($i in $runs){ $t=Measure-Command { & 'mysql' -u '<user>' -p'<pass>' -D '<db>' -e \"SELECT SQL_NO_CACHE p.id FROM post p WHERE ... LIMIT 10;\" }; $ms=[math]::Round($t.TotalMilliseconds,2); Write-Output \"Run $i: $ms ms\"; $results+=$ms }; Write-Output 'Average ms: ' + [math]::Round(($results | Measure-Object -Average).Average,2)"
```

Notes on the PowerShell one-liner:
- It is invoked from CMD but runs PowerShell to measure wall-clock time. Replace `<pass>` inline only for non-production testing; prefer being prompted for password (omit `-p'<pass>'`) and enter when asked.
- For warm-cache timings, run the query several times and report the median of later runs. For cold-cache, restart the MySQL server between runs.

Parsing JSON EXPLAIN outputs (Windows)
- If you have `jq` installed (WSL or Windows builds), you can extract fields, e.g.:
```
jq '.[] | select(.query_block != null) | .query_block' explain_analyze_query.json
```
- With PowerShell you can parse JSON:
```
powershell -Command "(Get-Content explain_analyze_query.json | ConvertFrom-Json) | Select-Object -Property *"
```
- Look for fields such as `rows_examined`, `filtered`, `execution_time`, and details under `query_block` / `table` nodes. `EXPLAIN ANALYZE` provides actual timing and rows processed.

Structured result entry (copy this block into `performance_report_module6.md` for each query tested)

- Environment
  - MySQL version: <fill>
  - Dataset size (rows): <fill>
  - Instance hardware: <fill>

- Query tested: `<paste SQL>`
- Index state: `before` — list indexes present (fill), `after` — list indexes applied (fill)
- EXPLAIN (estimate): `explain_query_before.json` (attach)
- EXPLAIN ANALYZE (actual): `explain_analyze_query_before.json` (attach)
- Execution times (ms):
  - Cold (first run after restart): <ms>
  - Warm (median of N runs): <ms>
  - After index applied (median of N runs): <ms>
- Rows examined (from EXPLAIN / ANALYZE JSON): <value>
- Plan summary: <short human-readable summary — e.g., "index lookup on author_id; no filesort" or "full table scan; filesort" >
- Observed improvement: <percent change, e.g., 72% faster>

Suggested additional indexes (apply if EXPLAIN shows full table scans / filesort / temporary):

- Full-text: `ALTER TABLE post ADD FULLTEXT INDEX ft_post_title_content (title, content);` (MySQL InnoDB fulltext index)
- Author filter + ordering: `CREATE INDEX idx_post_author_created ON post(author_id, deleted_at, created_at DESC);` — helps `WHERE author_id = ? AND deleted_at IS NULL ORDER BY created_at DESC` queries.
- Tag join: ensure `post_tags` has `INDEX(post_id, tag_id)` and `tag` has `INDEX(name)`.
- Aggregation support: add an index on `review(post_id, rating)` or ensure `review.post_id` is indexed to speed joins/aggregations.
- Covering/index-on-filter: if many queries filter on `deleted_at IS NULL`, consider a composite index including `deleted_at` where appropriate.

How to apply index migration (example Flyway migration snippet)
```
-- V8__add_recommended_indexes.sql
ALTER TABLE post ADD FULLTEXT INDEX ft_post_title_content (title, content);
CREATE INDEX idx_post_author_created ON post(author_id, deleted_at, created_at);
CREATE INDEX idx_tag_name ON tag(name);
CREATE INDEX idx_post_tags_post_tag ON post_tags(post_id, tag_id);
CREATE INDEX idx_review_post_rating ON review(post_id, rating);
```

After applying migrations, re-run the EXPLAIN/ANALYZE and measured runs and fill the structured result entry above.

Added artifacts
- `scripts/mysql/explain_queries.sql` — EXPLAIN templates (already present)
- `scripts/mysql/run_benchmarks.ps1` — PowerShell script to run the measured queries repeatedly and compute median/average timings (uses `mysql` CLI). Run this on Windows where the `mysql` client is available.

Example: run the PowerShell benchmark (will prompt for MySQL password):

```powershell
./scripts/mysql/run_benchmarks.ps1 -User myuser -Database smart_blog -Query "SELECT SQL_NO_CACHE p.id FROM post p WHERE p.author_id = 123 AND p.deleted_at IS NULL ORDER BY p.created_at DESC LIMIT 10;" -Runs 5
```

Collect the `EXPLAIN ANALYZE` JSON outputs and measured timings and paste them into this report. Once the MySQL numbers are present, update the `Record query timings` task in the project's TODO list.

Closing

Follow the reproducible steps for each key query, save the JSON files and timing outputs in a folder (e.g., `analysis/module6/`) and then paste the structured entries into `performance_report_module6.md`. That will complete the requirement: "Query execution times recorded and improved where applicable." 

