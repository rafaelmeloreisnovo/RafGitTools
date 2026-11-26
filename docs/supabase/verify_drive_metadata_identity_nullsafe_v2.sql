-- READ-ONLY validation AFTER authorized view deployment; never use as migration.
-- Live counts are environment-specific; do not hardcode private measurements.
with source_totals as (
  select count(*)::bigint as source_rows
    from raf_private.drive_metadata_index_v1
), v1 as (
  select count(*)::bigint as identities,
         coalesce(sum(observation_count),0)::bigint as observations
    from raf_private.drive_metadata_identity_projection_v1
), v2 as (
  select count(*)::bigint as identities,
         coalesce(sum(observation_count),0)::bigint as observations,
         count(*) filter(where parent_legacy_undercount)::bigint as parent_null_undercounts,
         count(*) filter(where parent_legacy_undercount and metadata_fingerprint_variants > 1)::bigint
           as fingerprint_parent_intersections,
         count(*) filter(where parent_variants_including_null < parent_variants_excluding_null_v1)::bigint
           as monotonicity_failures,
         count(*) filter(where content_equivalence <> 'TOKEN_VAZIO' or claim_allowed)::bigint
           as unsafe_promotions
    from raf_private.drive_metadata_identity_nullsafe_v2
)
select (s.source_rows=v1.observations and v1.observations=v2.observations) as source_preserved,
       (v1.identities=v2.identities) as identity_preserved,
       (v2.monotonicity_failures=0) as parent_null_monotonicity_pass,
       (v2.unsafe_promotions=0) as claim_gate_pass,
       coalesce(c.reloptions @> ARRAY['security_invoker=true'],false) as security_invoker_pass,
       (not has_schema_privilege('anon','raf_private','USAGE')) as anon_private_schema_pass,
       v1.identities, s.source_rows, v2.parent_null_undercounts,
       v2.fingerprint_parent_intersections
  from source_totals s cross join v1 cross join v2
  join pg_catalog.pg_class c on c.oid='raf_private.drive_metadata_identity_nullsafe_v2'::regclass;

-- Standalone adversarial fixture, independent of deployment or private rows.
with fixture(grp,parent) as (values
  ('NULL_TO_VALUE',null::text),('NULL_TO_VALUE','root-A'),
  ('ALL_NULL',null::text),('ALL_NULL',null::text),
  ('TWO_VALUES','root-A'),('TWO_VALUES','root-B'),
  ('STABLE','root-A'),('STABLE','root-A'),
  ('BLANK_NULL',null::text),('BLANK_NULL','')
), measured as (
  select grp, count(distinct parent) legacy,
         count(distinct row(parent,parent is null)) nullaware
    from fixture group by grp
)
select grp,legacy,nullaware,
       case grp
         when 'NULL_TO_VALUE' then legacy=1 and nullaware=2
         when 'ALL_NULL' then legacy=0 and nullaware=1
         when 'TWO_VALUES' then legacy=2 and nullaware=2
         when 'STABLE' then legacy=1 and nullaware=1
         when 'BLANK_NULL' then legacy=1 and nullaware=2
         else false
       end as scoped_assertion_pass
  from measured order by grp;
