-- RAFAELIA Ω | TERMINAL FOLDER CAPTURE (SCOPED) | 2026-11-21
-- READ ONLY; run on authorized private Supabase connection. No private IDs exported.
-- 132 pages, 60687 distinct accessible FOLDER IDs PER capture.
-- Never equate these digests with original file-byte SHA-256.
-- No global Drive snapshot isolation / incompleteSearch proof / B0160 promotion.

-- A. Independent structural/ACL gates: both captures must be terminal and clean.
select capture_id,pages,provider_positions,unique_ids,duplicate_positions,
 digest_mismatch,broken_chain,terminal_pages,json_null_parents,
 absent_parent_key,invalid_parent_type,missing_mod_time,scoped_state,
 snapshot_isolation,claim_allowed
from raf_private.drive_folder_capture_scoped_quality_v1
order by capture_id;

-- B. Set equality and scoped selected-metadata equivalence.
with a as (
 select x.item->>'id' id,x.item->'parent_ids' parents,
        x.item->>'created_time' created_at,x.item->>'modified_time' modified_at
 from raf_private.drive_folder_capture_pages_v1 p
 cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) x(item)
 where p.capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'
), b as (
 select x.item->>'id' id,x.item->'parent_ids' parents,
        x.item->>'created_time' created_at,x.item->>'modified_time' modified_at
 from raf_private.drive_folder_capture_pages_v1 p
 cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) x(item)
 where p.capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_B_V1'
)
select count(*) filter(where a.id is not null) n_a,
       count(*) filter(where b.id is not null) n_b,
       count(*) filter(where a.id is not null and b.id is not null) intersection,
       count(*) filter(where a.id is null or b.id is null) symmetric_difference,
       count(*) filter(where a.id is not null and b.id is not null
                        and (a.parents,a.created_at,a.modified_at)
                        is distinct from (b.parents,b.created_at,b.modified_at))
                        selected_metadata_differences
from a full outer join b using(id);

-- C. Canonical ID-set, selected metadata, and ordered-page roots.
-- Length-prefixed sorted IDs and C collation remove row-order ambiguity.
with items as (
 select p.capture_id,x.item->>'id' id,x.item->'parent_ids' parents,
 x.item->>'created_time' created_at,x.item->>'modified_time' modified_at
 from raf_private.drive_folder_capture_pages_v1 p
 cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) x(item)
), sets as (
 select capture_id,count(*) n,
 encode(sha256(convert_to(
    string_agg(length(id)::text||':'||id,E'\n' order by id collate "C"),
    'UTF8')),'hex') identity_manifest_sha256,
 encode(sha256(convert_to(
    string_agg(jsonb_build_object('id',id,'parents',parents,
     'created',created_at,'modified',modified_at)::text,E'\n'
     order by id collate "C"),'UTF8')),'hex') selected_metadata_manifest_sha256
 from items group by capture_id
), pages as (
 select capture_id,count(*) page_count,
 encode(sha256(convert_to(
  string_agg(page_index::text||':'||page_manifest_sha256,E'\n'
   order by page_index),'UTF8')),'hex') ordered_page_digest_root
 from raf_private.drive_folder_capture_pages_v1 group by capture_id
)
select sets.*,pages.page_count,pages.ordered_page_digest_root
from sets join pages using(capture_id) order by capture_id;

-- D. Known historical folders and metadata-projection folder coverage.
with capture as (
 select distinct x.item->>'id' id
 from raf_private.drive_folder_capture_pages_v1 p
 cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) x(item)
 where p.capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'
), b0154 as (
 select distinct provider_file_id id
 from raf_private.inventory_identity_membership_v1
 where snapshot_key='B0154_HISTORICAL_UNION_V1'
   and account_id='100638035397025079961'
), projection as (
 select distinct source_file_id id
 from raf_private.drive_metadata_index_v1
 where account_alias='oficial' and mime_type='application/vnd.google-apps.folder'
)
select (select count(*) from capture) captured_folder_ids,
 (select count(*) from b0154) historical_folders,
 (select count(*) from b0154 join capture using(id)) historical_found,
 (select count(*) from projection) projection_folder_ids,
 (select count(*) from projection join capture using(id)) projection_found;

-- E. Preserve source-field absence separately from independent files.get null.
select capture_id,
 count(*) filter(where item->>'parent_listing_state'=
     'TOKEN_VAZIO_PARENT_FIELD_ABSENT') listing_parent_absent,
 count(*) filter(where item->>'parent_resolution_state'='PROVIDER_EXPLICIT_NULL'
     and item->>'parent_resolution_source'='Google_Drive.get_file_metadata'
     and jsonb_typeof(item->'parent_ids')='null') verified_get_null,
 count(*) filter(where jsonb_typeof(item->'parent_ids')='null') all_null_parents,
 count(*) filter(where not (item ? 'parent_ids')) persisted_parent_field_absent
from (
 select p.capture_id,x.item
 from raf_private.drive_folder_capture_pages_v1 p
 cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) x(item)
) r
group by capture_id order by capture_id;

-- F. ACL, source immutability and writer gate (current readback).
select (select count(*) from raf_private.drive_folder_capture_pages_v1) stored_pages,
 (select count(*) from raf_private.drive_folder_capture_scoped_quality_v1
  where claim_allowed) promoted_claims,
 (select lease_expires_at<=clock_timestamp()
  from raf_private.global_writer_leases_v1
  where resource_key='RAFAELIA:INVENTORY:DRIVE_GITHUB') writer_inactive,
 not has_schema_privilege('anon','raf_private','USAGE') anonymous_schema_closed,
 (select reloptions @> ARRAY['security_invoker=true']
 from pg_class where oid='raf_private.drive_folder_capture_scoped_quality_v1'::regclass)
 security_invoker_view;
