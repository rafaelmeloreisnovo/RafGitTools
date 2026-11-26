-- RAFAELIA Ω | C13 | terminal folder-only coverage audit v1 | 2026-11-21
-- Additive, private, read-only, security_invoker. No copied private identities.
-- Join scopes are explicit. Absence from B0154/projection is NOT deletion or loss.
-- Does NOT establish all-file census, snapshot isolation, source-byte SHA, or B0160.
create view raf_private.drive_folder_terminal_scope_coverage_v1
with (security_invoker = true) as
with source_scope as (
  select min(account_profile_id) account_id,
         count(distinct account_profile_id) account_variants,
         count(distinct query_contract) contract_variants,
         count(distinct provider) provider_variants
  from raf_private.drive_folder_capture_pages_v1
  where capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'
),
source_ids as materialized (
  select distinct x.item->>'id' id
  from raf_private.drive_folder_capture_pages_v1 p
  cross join lateral jsonb_array_elements(p.folder_ids_and_metadata) x(item)
  where p.capture_id='RAF_DRIVE_FOLDERS_20261121_PASS_A_V1'
),
history_ids as materialized (
  select distinct provider_file_id id
  from raf_private.inventory_identity_membership_v1
  where snapshot_key='B0154_HISTORICAL_UNION_V1'
    and account_id=(select account_id from source_scope where account_variants=1)
),
projection_ids as materialized (
  select distinct source_file_id id
  from raf_private.drive_metadata_index_v1
  where account_alias='oficial' and mime_type='application/vnd.google-apps.folder'
),
counted as (
  select count(*) captured,
    count(*) filter(where h.id is not null and p.id is not null) both,
    count(*) filter(where h.id is not null and p.id is null) only_history,
    count(*) filter(where h.id is null and p.id is not null) only_projection,
    count(*) filter(where h.id is null and p.id is null) outside_both
  from source_ids s left join history_ids h using(id) left join projection_ids p using(id)
),
quality as (
  select count(*) filtered_audits,
     count(*) filter(where pages=132 and unique_ids=60687
         and duplicate_positions=0 and digest_mismatch=0 and broken_chain=0
         and terminal_pages=1 and scoped_state='TERMINAL_CAPTURE_SCOPED'
         and claim_allowed=false) clean_terminal_audits
  from raf_private.drive_folder_capture_scoped_quality_v1
  where capture_id in ('RAF_DRIVE_FOLDERS_20261121_PASS_A_V1','RAF_DRIVE_FOLDERS_20261121_PASS_B_V1')
)
select c.*,
  (select count(*) from history_ids) history_total,
  (select count(*) from projection_ids) projection_total,
  s.account_variants,s.contract_variants,s.provider_variants,
  q.filtered_audits,q.clean_terminal_audits,
  c.captured=(c.both+c.only_history+c.only_projection+c.outside_both) conservation_ok,
  (c.both+c.only_history)=(select count(*) from history_ids) historic_covered,
  (c.both+c.only_projection)=(select count(*) from projection_ids) projection_covered,
  (s.account_variants=1 and s.contract_variants=1 and s.provider_variants=1
   and q.filtered_audits=2 and q.clean_terminal_audits=2
   and c.captured=c.both+c.only_history+c.only_projection+c.outside_both
   and c.both+c.only_history=(select count(*) from history_ids)
   and c.both+c.only_projection=(select count(*) from projection_ids)) scoped_acceptance,
  'NOT_PROVEN'::text provider_snapshot_isolation,
  'TOKEN_VAZIO'::text source_content_sha256,
  'TOKEN_VAZIO'::text historic_60687_position_identity_equivalence,
  false::boolean claim_allowed
from counted c cross join source_scope s cross join quality q;
comment on view raf_private.drive_folder_terminal_scope_coverage_v1 is 'RAFAELIA C13. Read-only folder-only source coverage; absence in a partial index is NOT lost content. claim_allowed=false.';