-- RAFAELIA FIAT_LUX Evidence Database Federation V1
-- Logical database planes inside RAFAELIA_NAVIGATOR.sqlite3.
-- Policy: local-first, append-only ledgers, claim_allowed=false by default.
-- SQLite-compatible; no external extensions required.

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS schema_meta (
  schema_id TEXT PRIMARY KEY,
  version INTEGER NOT NULL,
  created_at TEXT NOT NULL,
  source_authority TEXT NOT NULL,
  state TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);

CREATE TABLE IF NOT EXISTS "databaseroot" (
  event_id TEXT PRIMARY KEY,
  root_id TEXT NOT NULL,
  provider TEXT NOT NULL,
  locator TEXT NOT NULL,
  role TEXT NOT NULL,
  authority TEXT NOT NULL,
  predecessor_event_id TEXT,
  epistemic_state TEXT NOT NULL,
  source_id TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1)),
  payload_json TEXT
);
CREATE INDEX IF NOT EXISTS idx_databaseroot_root ON "databaseroot"(root_id, created_at);

CREATE TABLE IF NOT EXISTS "databaseStarthere" (
  event_id TEXT PRIMARY KEY,
  route_id TEXT NOT NULL,
  route_key TEXT NOT NULL,
  route_order INTEGER NOT NULL,
  target_kind TEXT NOT NULL,
  target_ref TEXT NOT NULL,
  purpose TEXT NOT NULL,
  predecessor_event_id TEXT,
  source_id TEXT,
  created_at TEXT NOT NULL,
  epistemic_state TEXT NOT NULL DEFAULT 'OBSERVED_SOURCE_TEXT',
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_starthere_key ON "databaseStarthere"(route_key, route_order);

CREATE TABLE IF NOT EXISTS "database_∅" (
  event_id TEXT PRIMARY KEY,
  gap_id TEXT NOT NULL,
  subject_id TEXT NOT NULL,
  priority TEXT NOT NULL CHECK (priority IN ('P0','P1','P2','P3','UNSET')),
  state TEXT NOT NULL,
  authority TEXT,
  evidence_required TEXT,
  uncertainty TEXT,
  falsifier TEXT,
  next_action TEXT,
  predecessor_event_id TEXT,
  source_ref TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_empty_gap ON "database_∅"(gap_id, created_at);
CREATE INDEX IF NOT EXISTS idx_empty_priority ON "database_∅"(priority, created_at);

CREATE TABLE IF NOT EXISTS database_evidencias (
  evidence_event_id TEXT PRIMARY KEY,
  evidence_id TEXT NOT NULL,
  subject_id TEXT NOT NULL,
  evidence_type TEXT NOT NULL,
  source_id TEXT NOT NULL,
  locator TEXT NOT NULL,
  observed_at TEXT NOT NULL,
  independent_origin INTEGER NOT NULL DEFAULT 0 CHECK (independent_origin IN (0,1)),
  digest_algorithm TEXT,
  digest TEXT,
  bytes INTEGER,
  metric_name TEXT,
  metric_value TEXT,
  uncertainty TEXT,
  epistemic_state TEXT NOT NULL,
  predecessor_event_id TEXT,
  payload_json TEXT,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_evidence_subject ON database_evidencias(subject_id, observed_at);
CREATE INDEX IF NOT EXISTS idx_evidence_id ON database_evidencias(evidence_id, observed_at);

CREATE TABLE IF NOT EXISTS database_gates (
  gate_event_id TEXT PRIMARY KEY,
  gate_id TEXT NOT NULL,
  subject_id TEXT NOT NULL,
  gate_kind TEXT NOT NULL,
  required_evidence_json TEXT NOT NULL,
  falsifier TEXT,
  current_state TEXT NOT NULL,
  decision TEXT NOT NULL,
  decision_reason TEXT,
  evidence_ids_json TEXT,
  predecessor_event_id TEXT,
  source_ref TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_gate_id ON database_gates(gate_id, created_at);
CREATE INDEX IF NOT EXISTS idx_gate_subject ON database_gates(subject_id, created_at);

CREATE TABLE IF NOT EXISTS database_receipts (
  receipt_event_id TEXT PRIMARY KEY,
  receipt_id TEXT NOT NULL,
  subject_id TEXT NOT NULL,
  result_state TEXT NOT NULL,
  evidence_ids_json TEXT NOT NULL,
  gate_ids_json TEXT,
  input_scope TEXT,
  output_scope TEXT,
  predecessor_receipt_id TEXT,
  source_ref TEXT NOT NULL,
  integrity_algorithm TEXT,
  integrity_digest TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1)),
  payload_json TEXT
);
CREATE INDEX IF NOT EXISTS idx_receipt_id ON database_receipts(receipt_id, created_at);
CREATE INDEX IF NOT EXISTS idx_receipt_subject ON database_receipts(subject_id, created_at);

CREATE TABLE IF NOT EXISTS database_invariants (
  invariant_event_id TEXT PRIMARY KEY,
  invariant_id TEXT NOT NULL,
  scope TEXT NOT NULL,
  statement TEXT NOT NULL,
  severity TEXT NOT NULL,
  enforcement TEXT NOT NULL,
  falsifier TEXT,
  source_ref TEXT,
  predecessor_event_id TEXT,
  created_at TEXT NOT NULL,
  epistemic_state TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_invariant_id ON database_invariants(invariant_id, created_at);

CREATE TABLE IF NOT EXISTS database_memory_axes (
  memory_event_id TEXT PRIMARY KEY,
  object_id TEXT NOT NULL,
  axis TEXT NOT NULL CHECK (axis IN ('L','O','T')),
  relation_type TEXT NOT NULL,
  source_ref TEXT NOT NULL,
  target_ref TEXT,
  evidence_id TEXT,
  gap_id TEXT,
  predecessor_event_id TEXT,
  created_at TEXT NOT NULL,
  payload_json TEXT,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_memory_object_axis ON database_memory_axes(object_id, axis, created_at);

CREATE TABLE IF NOT EXISTS database_routes (
  route_event_id TEXT PRIMARY KEY,
  route_id TEXT NOT NULL,
  object_id TEXT NOT NULL,
  ordinal INTEGER NOT NULL,
  node_kind TEXT NOT NULL,
  node_ref TEXT NOT NULL,
  relation TEXT NOT NULL,
  next_ref TEXT,
  gate_id TEXT,
  evidence_id TEXT,
  gap_id TEXT,
  predecessor_event_id TEXT,
  created_at TEXT NOT NULL,
  state TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1)),
  UNIQUE(route_id, ordinal, route_event_id)
);
CREATE INDEX IF NOT EXISTS idx_routes_route ON database_routes(route_id, ordinal);
CREATE INDEX IF NOT EXISTS idx_routes_object ON database_routes(object_id, created_at);

CREATE TABLE IF NOT EXISTS database_hot_pathway (
  pathway_event_id TEXT PRIMARY KEY,
  pathway_id TEXT NOT NULL,
  object_id TEXT NOT NULL,
  ordinal INTEGER NOT NULL,
  slot INTEGER CHECK (slot BETWEEN 0 AND 8),
  node_ref TEXT NOT NULL,
  condition_expr TEXT,
  next_ref TEXT,
  binding_id TEXT,
  gate_id TEXT,
  evidence_id TEXT,
  state TEXT NOT NULL,
  predecessor_event_id TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_hot_path ON database_hot_pathway(pathway_id, ordinal);

CREATE TABLE IF NOT EXISTS database_one_hot_binding (
  binding_event_id TEXT PRIMARY KEY,
  binding_id TEXT NOT NULL,
  slot INTEGER NOT NULL CHECK (slot BETWEEN 0 AND 8),
  bank INTEGER NOT NULL CHECK (bank BETWEEN 0 AND 2),
  semantic_role TEXT NOT NULL,
  runtime_target TEXT,
  bind_state TEXT NOT NULL,
  source_ref TEXT,
  evidence_id TEXT,
  gate_id TEXT,
  predecessor_event_id TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1)),
  CHECK (slot >= bank * 3 AND slot <= bank * 3 + 2)
);
CREATE INDEX IF NOT EXISTS idx_binding_slot ON database_one_hot_binding(binding_id, slot, created_at);

CREATE TABLE IF NOT EXISTS "databaseSTEPStoDo&done" (
  step_event_id TEXT PRIMARY KEY,
  step_id TEXT NOT NULL,
  roadmap_id TEXT,
  subject_id TEXT NOT NULL,
  state TEXT NOT NULL CHECK (state IN ('TODO','DOING','DONE','BLOCKED','TOKEN_VAZIO','SUPERSEDED')),
  urgency TEXT NOT NULL CHECK (urgency IN ('P0','P1','P2','P3','UNSET')),
  action TEXT NOT NULL,
  gate_id TEXT,
  evidence_id TEXT,
  receipt_id TEXT,
  gap_id TEXT,
  predecessor_event_id TEXT,
  source_ref TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1)),
  payload_json TEXT
);
CREATE INDEX IF NOT EXISTS idx_steps_step ON "databaseSTEPStoDo&done"(step_id, created_at);
CREATE INDEX IF NOT EXISTS idx_steps_urgency ON "databaseSTEPStoDo&done"(urgency, created_at);

CREATE TABLE IF NOT EXISTS roadmapDatabase (
  roadmap_event_id TEXT PRIMARY KEY,
  roadmap_id TEXT NOT NULL,
  item_id TEXT NOT NULL,
  ordinal INTEGER NOT NULL,
  priority TEXT NOT NULL CHECK (priority IN ('P0','P1','P2','P3','UNSET')),
  state TEXT NOT NULL,
  title TEXT NOT NULL,
  objective TEXT NOT NULL,
  dependency_ids_json TEXT,
  gate_ids_json TEXT,
  evidence_ids_json TEXT,
  gap_ids_json TEXT,
  next_action TEXT,
  predecessor_event_id TEXT,
  source_ref TEXT,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_roadmap ON roadmapDatabase(roadmap_id, ordinal, created_at);

CREATE TABLE IF NOT EXISTS atlas_urgency_queue (
  urgency_event_id TEXT PRIMARY KEY,
  urgency_id TEXT NOT NULL,
  priority TEXT NOT NULL CHECK (priority IN ('P0','P1','P2','P3','UNSET')),
  state TEXT NOT NULL,
  scope_json TEXT,
  authority TEXT,
  evidence_json TEXT,
  uncertainty TEXT,
  falsifier TEXT,
  closure_gate_json TEXT,
  next_action TEXT,
  predecessor_event_id TEXT,
  source_ref TEXT NOT NULL,
  created_at TEXT NOT NULL,
  claim_allowed INTEGER NOT NULL DEFAULT 0 CHECK (claim_allowed IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_atlas_urgency ON atlas_urgency_queue(priority, urgency_id, created_at);

-- Latest-state views preserve append-only history while exposing current routing state.
CREATE VIEW IF NOT EXISTS v_current_gaps AS
SELECT g.* FROM "database_∅" g
WHERE g.rowid = (SELECT MAX(g2.rowid) FROM "database_∅" g2 WHERE g2.gap_id = g.gap_id);

CREATE VIEW IF NOT EXISTS v_current_steps AS
SELECT s.* FROM "databaseSTEPStoDo&done" s
WHERE s.rowid = (SELECT MAX(s2.rowid) FROM "databaseSTEPStoDo&done" s2 WHERE s2.step_id = s.step_id);

CREATE VIEW IF NOT EXISTS v_current_roadmap AS
SELECT r.* FROM roadmapDatabase r
WHERE r.rowid = (SELECT MAX(r2.rowid) FROM roadmapDatabase r2 WHERE r2.roadmap_id = r.roadmap_id AND r2.item_id = r.item_id);

CREATE VIEW IF NOT EXISTS v_current_urgency AS
SELECT u.* FROM atlas_urgency_queue u
WHERE u.rowid = (SELECT MAX(u2.rowid) FROM atlas_urgency_queue u2 WHERE u2.urgency_id = u.urgency_id);

CREATE VIEW IF NOT EXISTS v_one_hot_walk AS
SELECT
  binding_id,
  slot,
  bank,
  semantic_role,
  runtime_target,
  bind_state,
  ((slot + 1) % 9) AS next_slot,
  ((slot + 8) % 9) AS prev_slot,
  ((slot + 3) % 9) AS bank_next_slot,
  evidence_id,
  gate_id,
  created_at
FROM database_one_hot_binding;

-- Append-only enforcement. State transitions are new events; rows are never rewritten.
CREATE TRIGGER IF NOT EXISTS ao_databaseroot_u BEFORE UPDATE ON "databaseroot" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_databaseroot_d BEFORE DELETE ON "databaseroot" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_starthere_u BEFORE UPDATE ON "databaseStarthere" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_starthere_d BEFORE DELETE ON "databaseStarthere" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_empty_u BEFORE UPDATE ON "database_∅" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_empty_d BEFORE DELETE ON "database_∅" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_evidence_u BEFORE UPDATE ON database_evidencias BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_evidence_d BEFORE DELETE ON database_evidencias BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_gates_u BEFORE UPDATE ON database_gates BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_gates_d BEFORE DELETE ON database_gates BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_receipts_u BEFORE UPDATE ON database_receipts BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_receipts_d BEFORE DELETE ON database_receipts BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_invariants_u BEFORE UPDATE ON database_invariants BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_invariants_d BEFORE DELETE ON database_invariants BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_memory_u BEFORE UPDATE ON database_memory_axes BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_memory_d BEFORE DELETE ON database_memory_axes BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_routes_u BEFORE UPDATE ON database_routes BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_routes_d BEFORE DELETE ON database_routes BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_hot_u BEFORE UPDATE ON database_hot_pathway BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_hot_d BEFORE DELETE ON database_hot_pathway BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_binding_u BEFORE UPDATE ON database_one_hot_binding BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_binding_d BEFORE DELETE ON database_one_hot_binding BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_steps_u BEFORE UPDATE ON "databaseSTEPStoDo&done" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_steps_d BEFORE DELETE ON "databaseSTEPStoDo&done" BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_roadmap_u BEFORE UPDATE ON roadmapDatabase BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_roadmap_d BEFORE DELETE ON roadmapDatabase BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_urgency_u BEFORE UPDATE ON atlas_urgency_queue BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
CREATE TRIGGER IF NOT EXISTS ao_urgency_d BEFORE DELETE ON atlas_urgency_queue BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
