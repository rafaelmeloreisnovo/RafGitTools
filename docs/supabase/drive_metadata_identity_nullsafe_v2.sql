-- RAFAELIA Ω | additive, scoped NULL-aware metadata identity projection.
-- IMPORTANT: REVIEWED SCHEMA PROPOSAL; NOT a deployed migration or data receipt.
-- Never replace/drop V1, overwrite original observations or expose private schema.
-- Deploy only with reviewed authority/ACL, versioned migration, and readback.
-- The existing V1 COUNT(DISTINCT source_parent_id) omits SQL NULL.
-- ROW(value, value IS NULL) treats a NULL observation as one explicit variant.
create view raf_private.drive_metadata_identity_nullsafe_v2
with (security_invoker = true)
as
with parent_audit as (
  select account_alias, source_file_id,
         count(*)::bigint as source_observations,
         count(distinct row(source_parent_id, source_parent_id is null))::bigint
           as parent_variants_including_null,
         count(distinct row(source_root_id, source_root_id is null))::bigint
           as root_variants_including_null,
         count(*) filter(where source_parent_id is null)::bigint
           as parent_null_observations,
         count(*) filter(where source_parent_id is not null and btrim(source_parent_id) = '')::bigint
           as parent_blank_observations,
         count(*) filter(where source_parent_id is not null and btrim(source_parent_id) <> '')::bigint
           as parent_value_observations,
         count(*) filter(where source_parent_id is not null and source_parent_id = source_root_id)::bigint
           as parent_equal_root_observations
    from raf_private.drive_metadata_index_v1
   group by account_alias, source_file_id
)
select v.account_alias, v.source_file_id,
       v.representative_observation_pk, v.observation_count,
       v.root_variants as root_variants_excluding_null_v1,
       a.root_variants_including_null,
       v.parent_variants as parent_variants_excluding_null_v1,
       a.parent_variants_including_null,
       a.parent_null_observations, a.parent_blank_observations,
       a.parent_value_observations, a.parent_equal_root_observations,
       (a.parent_variants_including_null > v.parent_variants) as parent_legacy_undercount,
       v.metadata_fingerprint_variants,
       case
         when a.parent_null_observations > 0 and a.parent_value_observations > 0
           then 'NULL_AND_VALUE_OBSERVED'
         when a.parent_null_observations > 0
           then 'NULL_OBSERVED_NO_VALUE'
         when a.parent_blank_observations > 0
           then 'BLANK_PARENT_OBSERVED'
         else 'NONBLANK_PARENT_OBSERVED'
       end::text as parent_evidence_class,
       case
         when v.metadata_fingerprint_variants > 1 and a.parent_null_observations > 0
           and a.parent_value_observations > 0
           then 'FINGERPRINT_AND_PARENT_NULL_DIFFERENCE_NOT_CAUSAL'
         when v.metadata_fingerprint_variants > 1
           then 'FINGERPRINT_DIFFERENCE_CAUSE_TOKEN_VAZIO'
         else 'NO_OBSERVED_FINGERPRINT_VARIANT'
       end::text as fingerprint_evidence_class,
       'TOKEN_VAZIO'::text as content_equivalence,
       false::boolean as claim_allowed
from raf_private.drive_metadata_identity_projection_v1 v
join parent_audit a
  on a.account_alias = v.account_alias and a.source_file_id = v.source_file_id
where a.source_observations = v.observation_count;

comment on view raf_private.drive_metadata_identity_nullsafe_v2 is
'RAFAELIA Ω additive NULL-aware parent variant diagnostics. Identity is not byte equivalence; no global completeness or B0160 promotion.';
