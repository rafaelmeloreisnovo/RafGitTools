-- RAFAELIA PIRC-2: scoped, additive, PRIVATE metadata identity projection.
-- Source: Supabase Rafcodephi, migration rafaelia_private_drive_identity_projection_v1_20261111.
-- Status: deployed in Supabase and readback-tested on 2026-11-11.
-- Never apply automatically from public CI; no service-role keys, corpus bytes or private IDs.
-- First installation only: this CREATE VIEW intentionally fails on pre-existing name.
create view raf_private.drive_metadata_identity_projection_v1
with (security_invoker = true)
as
with grouped as (
    select account_alias, source_file_id,
           count(*)::bigint as observation_count,
           count(distinct source_root_id)::bigint as root_variants,
           count(distinct version_fingerprint_sha256)::bigint as metadata_fingerprint_variants,
           count(distinct provider_modified_at)::bigint as provider_modification_variants,
           count(distinct source_name)::bigint as name_variants,
           count(distinct source_parent_id)::bigint as parent_variants,
           count(*) filter (where nullif(btrim(source_root_id),'') is null)::bigint as missing_root_observations,
           count(*) filter (where source_content_sha256 ~* '^[0-9a-f]{64}$')::bigint as content_hash_observations,
           count(distinct lower(source_content_sha256)) filter (where source_content_sha256 ~* '^[0-9a-f]{64}$')::bigint as content_hash_variants
    from raf_private.drive_metadata_index_v1
    group by account_alias, source_file_id
), ranked as (
    select item_pk, account_alias, source_file_id,
           row_number() over (
               partition by account_alias, source_file_id
               order by provider_modified_at desc nulls last,
                        observed_at desc nulls last,
                        item_pk desc
           ) as representative_rank
    from raf_private.drive_metadata_index_v1
)
select g.account_alias, g.source_file_id,
       r.item_pk as representative_observation_pk,
       g.observation_count, g.root_variants,
       g.metadata_fingerprint_variants, g.provider_modification_variants,
       g.name_variants, g.parent_variants, g.missing_root_observations,
       g.content_hash_observations, g.content_hash_variants,
       case
           when g.missing_root_observations > 0 then 'ROOT_TOKEN_VAZIO'
           when g.observation_count = 1 then 'SINGLE_OBSERVATION'
           when g.root_variants > 1 then 'MULTI_ROOT_OBSERVATION'
           else 'REOBSERVED_SAME_ROOT'
       end as identity_observation_class,
       'TOKEN_VAZIO'::text as content_equivalence,
       false as claim_allowed
from grouped g
join ranked r
  on r.account_alias = g.account_alias
 and r.source_file_id = g.source_file_id
 and r.representative_rank = 1;
comment on view raf_private.drive_metadata_identity_projection_v1 is
'RAFAELIA PIRC-2: read-only identity projection; never delete source observations. Same provider account+file ID is not a byte-content claim. content_equivalence TOKEN_VAZIO until independently verified.';
