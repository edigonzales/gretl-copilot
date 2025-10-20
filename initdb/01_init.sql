CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS rag;

-- Core tables (lives in the dedicated rag schema)
CREATE TABLE IF NOT EXISTS rag.pages (
  id BIGSERIAL PRIMARY KEY,
  url TEXT UNIQUE NOT NULL,
  title TEXT,
  fetched_at TIMESTAMPTZ DEFAULT now(),
  raw_md TEXT
);

CREATE TABLE IF NOT EXISTS rag.doc_chunks (
  id BIGSERIAL PRIMARY KEY,
  page_id BIGINT REFERENCES rag.pages(id) ON DELETE CASCADE,
  task_name TEXT,
  section_type TEXT,         -- 'task' | 'parameters' | 'example' | 'note'
  url TEXT,
  anchor TEXT,
  heading TEXT,
  content_text TEXT NOT NULL,
  content_md TEXT,
  embedding vector(1536)     -- match EMB_DIM in your ingester (1536 for text-embedding-3-small)
);

CREATE TABLE IF NOT EXISTS rag.task_properties (
  id BIGSERIAL PRIMARY KEY,
  task_name TEXT,
  property_name TEXT,
  type TEXT,
  required BOOLEAN,
  default_value TEXT,
  description TEXT,
  enum_values TEXT[]
);

CREATE TABLE IF NOT EXISTS rag.task_examples (
  id BIGSERIAL PRIMARY KEY,
  task_name TEXT,
  title TEXT,
  code_md TEXT,
  explanation TEXT,
  embedding vector(1536)
);

-- Supporting indexes
CREATE INDEX IF NOT EXISTS idx_doc_chunks_task_section ON rag.doc_chunks (task_name, section_type);
CREATE INDEX IF NOT EXISTS idx_task_props ON rag.task_properties (task_name, property_name);

-- Vector indexes (create after data is loaded; lists depends on dataset size)
-- CREATE INDEX idx_doc_chunks_embed ON rag.doc_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
-- CREATE INDEX idx_task_examples_embed ON rag.task_examples USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
