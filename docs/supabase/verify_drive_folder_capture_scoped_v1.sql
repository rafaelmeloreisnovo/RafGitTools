-- RAFAELIA Ω | READ-ONLY Drive folder verifier V1, hardened scoped gate V2.
-- Diagnostics remain compatible; reached_terminal/page counts ALONE ARE NOT PASS.
-- Require strict_scoped_gate AND both_capture_gate. No global, provider or byte claim.
-- Private Supabase only; never log provider IDs, cursor tokens, secrets or private records.
-- B0159 remains canonical; B0160 stays BLOCKED_HTTP_400. No DDL/DML.

WITH expected_capture(capture_id) AS (
  VALUES ('RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'),
         ('RAF_DRIVE_FOLDERS_20261121_PASS_B_V1')
), selected AS (
  SELECT p.* FROM raf_private.drive_folder_capture_pages_v1 p
  JOIN expected_capture e USING(capture_id)
), links AS (
  SELECT p.*,
         lag(p.next_cursor) OVER(PARTITION BY p.capture_id ORDER BY p.page_index) predecessor_cursor,
         lag(p.observed_at) OVER(PARTITION BY p.capture_id ORDER BY p.page_index) predecessor_observed,
         row_number() OVER(PARTITION BY p.capture_id ORDER BY p.page_index) page_rank,
         max(p.page_index) OVER(PARTITION BY p.capture_id) max_page,
         encode(sha256(convert_to(p.folder_ids_and_metadata::text,'UTF8')),'hex') recalculated_hash
  FROM selected p
), identities AS (
  SELECT p.capture_id,x.item->>'id' file_id FROM selected p
  CROSS JOIN LATERAL jsonb_array_elements(p.folder_ids_and_metadata) x(item)
), idcounts AS (
  SELECT capture_id,count(*)::bigint positions,count(DISTINCT file_id)::bigint distinct_ids
  FROM identities GROUP BY capture_id
), per_capture AS (
 SELECT e.capture_id,
        count(l.page_index)::bigint persisted_pages,
        sum(l.provider_positions)::bigint persisted_positions,
        i.distinct_ids,
        (i.positions-i.distinct_ids)::bigint cross_page_duplicate_positions,
        min(l.page_index) first_page,max(l.page_index) last_page,
        coalesce(bool_or(l.is_terminal),false) reached_terminal,
        count(*) FILTER(WHERE l.page_manifest_sha256 IS DISTINCT FROM l.recalculated_hash)::bigint page_hash_failures,
        count(*) FILTER(WHERE l.page_rank>1 AND (l.request_cursor IS DISTINCT FROM l.predecessor_cursor
                  OR l.page_index<>l.page_rank-1))::bigint cursor_chain_breaks,
        count(*) FILTER(WHERE l.page_rank>1 AND l.requested_at<l.predecessor_observed)::bigint nonmonotonic_requests,
        bool_and(l.claim_allowed IS FALSE) FILTER(WHERE l.page_index IS NOT NULL) all_claims_blocked,
        count(*) FILTER(WHERE l.page_index<>l.page_rank-1 OR
                  (l.page_rank=1 AND l.request_cursor IS NOT NULL))::bigint index_or_origin_errors,
        count(*) FILTER(WHERE l.is_terminal)::bigint terminal_pages,
        count(*) FILTER(WHERE l.is_terminal AND l.page_index<>l.max_page)::bigint early_terminal_pages,
        count(*) FILTER(WHERE (l.next_cursor IS NULL) IS DISTINCT FROM l.is_terminal)::bigint terminal_cursor_errors,
        count(*) FILTER(WHERE l.provider_positions IS DISTINCT FROM
                  jsonb_array_length(l.folder_ids_and_metadata))::bigint page_position_errors,
        count(DISTINCT l.provider)::bigint provider_variants,
        count(DISTINCT l.account_profile_id)::bigint account_variants,
        count(DISTINCT l.query_contract)::bigint query_variants
 FROM expected_capture e
 LEFT JOIN links l ON l.capture_id=e.capture_id
 LEFT JOIN idcounts i ON i.capture_id=e.capture_id
 GROUP BY e.capture_id,i.positions,i.distinct_ids
), validated AS (
 SELECT c.*,q.scoped_state,q.snapshot_isolation,q.source_content_sha256,
        coalesce(
          c.persisted_pages=132 AND c.persisted_positions=60687 AND
          c.distinct_ids=60687 AND c.cross_page_duplicate_positions=0 AND
          c.first_page=0 AND c.last_page=131 AND
          c.index_or_origin_errors=0 AND c.cursor_chain_breaks=0 AND
          c.nonmonotonic_requests=0 AND c.page_hash_failures=0 AND
          c.terminal_pages=1 AND c.early_terminal_pages=0 AND
          c.terminal_cursor_errors=0 AND c.page_position_errors=0 AND
          c.provider_variants=1 AND c.account_variants=1 AND c.query_variants=1 AND
          c.all_claims_blocked IS TRUE AND
          q.capture_id IS NOT NULL AND q.scoped_state='TERMINAL_CAPTURE_SCOPED' AND
          q.pages=132 AND q.unique_ids=60687 AND q.duplicate_positions=0 AND
          q.broken_chain=0 AND q.digest_mismatch=0 AND q.invalid_ids=0 AND
          q.invalid_parent_type=0 AND q.missing_mod_time=0 AND q.claim_allowed IS FALSE AND
          q.snapshot_isolation='NOT_PROVEN' AND q.source_content_sha256='TOKEN_VAZIO',
          FALSE
        ) strict_scoped_gate
 FROM per_capture c
 LEFT JOIN raf_private.drive_folder_capture_scoped_quality_v1 q ON q.capture_id=c.capture_id
)
SELECT v.*,
       (count(*) OVER()=2 AND bool_and(v.strict_scoped_gate) OVER() IS TRUE) both_capture_gate
FROM validated v ORDER BY v.capture_id;

-- Exact set differences, provider modified/created times, parent sets and page movement.
with source as (
  select p.capture_id,p.page_index,e->>'id' as file_id,
         e->>'modified_time' as modified_time,
         e->>'created_time' as created_time,
         e->'parent_ids' as parent_ids
    from raf_private.drive_folder_capture_pages_v1 p
    cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) e
   where p.capture_id in ('RAF_DRIVE_FOLDERS_20261121_PASS_A_V1',
                          'RAF_DRIVE_FOLDERS_20261121_PASS_B_V1')
), a as (
  select * from source where capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'
), b as (
  select * from source where capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_B_V1'
)
select (select count(*) from a) as a_positions,
       (select count(*) from b) as b_positions,
       (select count(*) from a where not exists(select 1 from b where b.file_id=a.file_id)) as only_a,
       (select count(*) from b where not exists(select 1 from a where a.file_id=b.file_id)) as only_b,
       (select count(*) from a join b using(file_id)) as matched_ids,
       (select count(*) from a join b using(file_id)
          where a.modified_time is distinct from b.modified_time) as modified_time_drift,
       (select count(*) from a join b using(file_id)
          where a.created_time is distinct from b.created_time) as created_time_drift,
       (select count(*) from a join b using(file_id)
          where a.parent_ids is distinct from b.parent_ids) as parent_drift,
       (select count(*) from a join b using(file_id)
          where a.page_index is distinct from b.page_index) as moved_across_pages,
       false as global_claim_allowed;

-- Historical B0154 (folders only) versus the current *prefix*, exact provider identity.
with a as (
  select e->>'id' as file_id from raf_private.drive_folder_capture_pages_v1 p
  cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) e
  where p.capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'
), b as (
  select provider_file_id as file_id
    from raf_private.inventory_identity_membership_v1
   where snapshot_key='B0154_HISTORICAL_UNION_V1'
)
select (select count(*) from b) baseline_folders,
       (select count(*) from a join b using(file_id)) baseline_found_in_prefix,
       (select count(*) from b where not exists(select 1 from a where a.file_id=b.file_id)) baseline_not_seen_in_prefix,
       false as global_claim_allowed;
