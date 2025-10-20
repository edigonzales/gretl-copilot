# GRETL RAG Starter

This bundle includes:
- `ingest_gretl.java` – single-file **JBang** ingester using **OpenAI embeddings** and a hardcoded whitelist (`https://gretl.app/reference.html` + anchors).
- `docker-compose.yml` – Postgres with **pgvector** and Adminer.
- `initdb/01_init.sql` – schema for pages/chunks/properties/examples.
- `initdb/02_readonly_user.sql` – creates a **read-only** user `gretl_ro` (password `change_me_ro`, change before prod).

## Quick start

```bash
# 1) Start Postgres + Adminer
docker compose up -d

# Adminer at http://localhost:8080
# System: PostgreSQL, Server: pgvector, User: gretl, Pass: gretl, DB: gretl_rag

# 2) Run the ingester (requires Java + JBang)
#    https://www.jbang.dev/download/
export OPENAI_API_KEY=sk-...

# Optional overrides (these match docker-compose)
export JDBC_URL=jdbc:postgresql://localhost:5432/gretl_rag
export JDBC_USER=gretl
export JDBC_PASS=gretl
export EMB_DIM=1536
export OPENAI_EMBED_MODEL=text-embedding-3-small

jbang ingest_gretl.java
```

## Exporting data as INSERT statements

You can dump the ingested data as plain `INSERT` commands for use in
pipelines or Codex-style demos. With the compose stack running:

```bash
# Open a shell inside the Postgres container (optional helper)
docker compose exec pgvector bash

# Inside the container: dump data-only SQL with INSERT statements
pg_dump -U gretl --schema=rag --data-only --inserts gretl_rag > /tmp/rag_inserts.sql

# Exit the container when done
exit

# Copy the dump file back to the host
docker compose cp gretl-rag-pg:/tmp/rag_inserts.sql ./rag_inserts.sql
```

For non-interactive environments (CI/CD, pipelines), you can skip the shell
and stream the dump directly:

```bash
docker compose exec -T pgvector \
  pg_dump -U gretl --schema=rag --data-only --inserts gretl_rag > rag_inserts.sql
```

## Switching to a larger embedding model

If you choose `text-embedding-3-large` (~3072 dims):

1. Update SQL vector dims:
   ```sql
   ALTER TABLE doc_chunks    ALTER COLUMN embedding TYPE vector(3072);
   ALTER TABLE task_examples ALTER COLUMN embedding TYPE vector(3072);
   ```
2. Update env:
   ```bash
   export EMB_DIM=3072
   export OPENAI_EMBED_MODEL=text-embedding-3-large
   ```

Re-embed existing rows for best results.

## Notes
- The whitelist is in `ingest_gretl.java` (`ALLOWLIST`). By default only `reference.html` is crawled.
- For production, **change the read-only password** in `initdb/02_readonly_user.sql` before first start.
- Create IVFFLAT indexes after ingest (tune `lists` for your dataset size).
