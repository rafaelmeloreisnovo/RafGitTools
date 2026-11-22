-- Read-only private execution gate. Run only through authorized Supabase SQL session.
-- No private metadata, secrets or source contents are emitted.
with source_counts as (
  select count(*)::bigint as total from raf_private.drive_metadata_index_v1
), projection as (
  select count(*)::bigint as identities,
         coalesce(sum(observation_count),0)::bigint as represented,
         coalesce(sum(observation_count-1),0)::bigint as repeated_excess,
         count(*) filter(where observation_count>1)::bigint as repeated_groups,
         count(*) filter(where content_equivalence <> 'TOKEN_VAZIO' or claim_allowed)::bigint as unearned_promotions,
         count(*) filter(where root_variants>1 and identity_observation_class <> 'MULTI_ROOT_OBSERVATION')::bigint as class_errors,
         count(*) filter(where observation_count=1 and identity_observation_class <> 'SINGLE_OBSERVATION')::bigint as singleton_errors
    from raf_private.drive_metadata_identity_projection_v1
)
select (s.total=p.represented) as preservation_pass,
       (p.unearned_promotions=0) as claim_gate_pass,
       (p.class_errors=0 and p.singleton_errors=0) as classification_pass,
       (p.represented-p.identities=p.repeated_excess) as count_invariant_pass,
       (coalesce(c.reloptions @> ARRAY['security_invoker=true'],false)) as view_security_invoker_pass,
       not has_schema_privilege('anon','raf_private','USAGE') as anon_boundary_pass,
       s.total as original_observations,
       p.identities as projected_provider_identities,
       p.repeated_groups, p.repeated_excess
from source_counts s cross join projection p
join pg_catalog.pg_class c on c.oid='raf_private.drive_metadata_identity_projection_v1'::regclass;
