-- RAFAELIA Ω | private, additive, read-only capture quality V1.
-- SOURCE IS A PUBLIC SCHEMA CONTRACT, NOT PRIVATE PROVIDER IDENTITIES.
-- PostgreSQL: avoid regex repetition counts above 255; validate length separately.
-- Full inventory, provider snapshot isolation and byte content equivalence are NOT proved.
-- Never drop/replace the original immutable page tables or CURRENT_STATE B0159.
create view raf_private.drive_folder_capture_scoped_quality_v1
with (security_invoker = true)
as
WITH p AS (
 SELECT capture_id,page_index,provider_positions,folder_ids_and_metadata,is_terminal,
 request_cursor,next_cursor,observed_at,requested_at,provider,account_profile_id,query_contract,page_manifest_sha256,
 lag(page_index) over(partition by capture_id order by page_index) prev_page,
 lag(next_cursor) over(partition by capture_id order by page_index) prev_cursor,
 lag(observed_at) over(partition by capture_id order by page_index) prev_observed
 FROM raf_private.drive_folder_capture_pages_v1
), pg AS (
 SELECT capture_id,count(*) pages,min(page_index) first_page,max(page_index) last_page,
 sum(provider_positions) provider_positions,count(*) filter(where is_terminal) terminal_pages,
 count(*) filter(where is_terminal and page_index<(select max(q.page_index) from raf_private.drive_folder_capture_pages_v1 q where q.capture_id=p.capture_id)) early_terminal,
 count(*) filter(where provider_positions<>jsonb_array_length(folder_ids_and_metadata)) count_mismatch,
 count(*) filter(where encode(sha256(convert_to(folder_ids_and_metadata::text,'UTF8')),'hex') is distinct from page_manifest_sha256) digest_mismatch,
 count(*) filter(where page_index>0 and (prev_page is distinct from page_index-1 or prev_cursor is distinct from request_cursor or prev_observed>requested_at)) broken_chain,
 count(*) filter(where page_index=0 and request_cursor is not null) wrong_start,
 count(*) filter(where (next_cursor is null) is distinct from is_terminal) wrong_terminal,
 count(distinct provider) provider_variants,count(distinct account_profile_id) account_variants,count(distinct query_contract) contract_variants
 FROM p GROUP BY capture_id
), ids AS (
 SELECT p.capture_id,x.item->>'id' id,x.item item
 FROM raf_private.drive_folder_capture_pages_v1 p
 CROSS JOIN LATERAL jsonb_array_elements(p.folder_ids_and_metadata) x(item)
), iv AS (
 SELECT capture_id,count(*) actual_positions,count(distinct id) unique_ids,
 count(*) filter(where id is null or length(id) not between 10 and 256 or id !~ '^[A-Za-z0-9_-]+$') invalid_ids,
 count(*) filter(where jsonb_typeof(item->'parent_ids')='null') json_null_parents,
 count(*) filter(where not (item ? 'parent_ids')) absent_parent_key,
 count(*) filter(where jsonb_typeof(item->'parent_ids') not in ('null','array') or item->'parent_ids' is null) invalid_parent_type,
 count(*) filter(where jsonb_typeof(item->'modified_time') is distinct from 'string') missing_mod_time
 FROM ids GROUP BY capture_id
)
SELECT pg.capture_id,pg.pages,pg.provider_positions,iv.actual_positions,iv.unique_ids,
 iv.actual_positions-iv.unique_ids duplicate_positions,pg.terminal_pages,pg.digest_mismatch,pg.broken_chain,
 iv.invalid_ids,iv.json_null_parents,iv.absent_parent_key,iv.invalid_parent_type,iv.missing_mod_time,
 CASE WHEN pg.first_page<>0 or pg.last_page<>pg.pages-1 or pg.terminal_pages>1 or pg.early_terminal>0 or pg.count_mismatch>0 or pg.digest_mismatch>0 or pg.broken_chain>0 or pg.wrong_start>0 or pg.wrong_terminal>0 or pg.provider_variants<>1 or pg.account_variants<>1 or pg.contract_variants<>1 or pg.provider_positions<>iv.actual_positions or iv.actual_positions<>iv.unique_ids or iv.invalid_ids>0 or iv.invalid_parent_type>0 or iv.missing_mod_time>0 THEN 'BLOCKED_SCOPED_INVARIANT'
 WHEN pg.terminal_pages=0 THEN 'CONSISTENT_PREFIX_NONTERMINAL'
 ELSE 'TERMINAL_CAPTURE_SCOPED' END scoped_state,
 'NOT_PROVEN'::text snapshot_isolation,
 'TOKEN_VAZIO'::text source_content_sha256,
 false::boolean claim_allowed
FROM pg JOIN iv USING(capture_id);
comment on view raf_private.drive_folder_capture_scoped_quality_v1 is 'RAFAELIA scoped, NULL-aware, private inventory prefix auditor. Does not prove global completeness or content hashes.';