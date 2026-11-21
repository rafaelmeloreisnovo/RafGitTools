-- RAFAELIA Ω | READ-ONLY private Drive folder capture verification V1.
-- Execute only in authorized Supabase private session. No private IDs are returned.
-- This is a scoped observation, NOT proof of stable provider snapshot or B0160 promotion.
-- Replace capture labels below only after reviewing exact account/scope in private receipts.

with selected as (
  select *
    from raf_private.drive_folder_capture_pages_v1
   where capture_id in ('RAF_DRIVE_FOLDERS_20261121_PASS_A_V1',
                        'RAF_DRIVE_FOLDERS_20261121_PASS_B_V1')
), links as (
  select p.*,
         lag(p.next_cursor) over(partition by p.capture_id order by p.page_index) predecessor_cursor,
         lag(p.observed_at) over(partition by p.capture_id order by p.page_index) predecessor_observed,
         row_number() over(partition by p.capture_id order by p.page_index) as page_rank,
         encode(sha256(convert_to(p.folder_ids_and_metadata::text,'UTF8')),'hex') as recalculated_hash
    from selected p
), identities as (
  select p.capture_id, e->>'id' as file_id
    from selected p
    cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) e
), idcounts as (
  select capture_id,count(*)::bigint as positions,count(distinct file_id)::bigint as distinct_ids
    from identities group by capture_id
)
select l.capture_id,count(*)::bigint as persisted_pages,
       sum(l.provider_positions)::bigint as persisted_positions,
       i.distinct_ids, i.positions-i.distinct_ids as cross_page_duplicate_positions,
       min(l.page_index) as first_page,max(l.page_index) as last_page,
       bool_or(l.is_terminal) as reached_terminal,
       sum((l.page_manifest_sha256<>l.recalculated_hash)::int)::bigint as page_hash_failures,
       sum((l.page_rank>1 and l.request_cursor is distinct from l.predecessor_cursor)::int)::bigint
         as cursor_chain_breaks,
       sum((l.page_rank>1 and l.requested_at<l.predecessor_observed)::int)::bigint
         as nonmonotonic_requests,
       bool_and(l.claim_allowed=false) as all_claims_blocked
  from links l join idcounts i using(capture_id)
 group by l.capture_id,i.distinct_ids,i.positions
 order by l.capture_id;

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
