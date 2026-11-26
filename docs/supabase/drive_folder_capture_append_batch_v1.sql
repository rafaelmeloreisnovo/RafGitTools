-- RAFAELIA Ω | private bounded atomic page ingest V1, additive; no automatic cursor advance
-- Only the existing fenced, append-only private table is modified by this function.
create function raf_private.drive_folder_capture_append_batch_v1(
  p_holder_id text,
  p_fencing_token bigint,
  p_pages jsonb
)
returns table(pages_added integer, positions_added bigint)
language plpgsql
security invoker
set search_path to ''
as $$
declare
  n integer;
  idx integer;
  p jsonb;
  stored_count integer;
  total_count bigint := 0;
  items jsonb;
begin
  if p_holder_id is null or length(btrim(p_holder_id))=0
     or p_fencing_token is null
     or jsonb_typeof(p_pages) is distinct from 'array' then
    raise exception 'BATCH_INPUT_INVALID' using errcode='22023';
  end if;
  n:=jsonb_array_length(p_pages);
  if n<1 or n>14 then
    raise exception 'BATCH_SIZE_OUT_OF_BOUNDS' using errcode='22023';
  end if;
  if not raf_private.global_writer_lease_validate_v1(
    'RAFAELIA:INVENTORY:DRIVE_GITHUB',p_holder_id,p_fencing_token) then
    raise exception 'BATCH_WRITER_FENCING_INVALID' using errcode='42501';
  end if;
  for idx in 0..n-1 loop
    p:=p_pages->idx;
    items:=p->'items';
    if jsonb_typeof(p) is distinct from 'object'
       or jsonb_typeof(items) is distinct from 'array'
       or not (p ? 'request_cursor') or not (p ? 'next_cursor')
       or not (p ? 'is_terminal') or jsonb_typeof(p->'is_terminal') is distinct from 'boolean'
       or not (p ? 'page_index') or jsonb_typeof(p->'page_index') is distinct from 'number'
       or not (p ? 'observed_at') or not (p ? 'requested_at')
    then raise exception 'BATCH_PAGE_SCHEMA_INVALID' using errcode='22023';
    end if;
    if jsonb_array_length(items)>1000 then
      raise exception 'BATCH_PAGE_TOO_LARGE' using errcode='22023';
    end if;
    insert into raf_private.drive_folder_capture_pages_v1 (
      capture_id,page_index,provider,account_profile_id,query_contract,
      requested_at,observed_at,request_cursor,next_cursor,is_terminal,
      provider_positions,folder_ids_and_metadata,page_manifest_sha256,
      lease_holder_id,fencing_token,claim_allowed
    )
    values (
      p->>'capture_id', (p->>'page_index')::integer,'google_drive',
      p->>'account_profile_id',p->>'query_contract',
      (p->>'requested_at')::timestamptz,(p->>'observed_at')::timestamptz,
      p->>'request_cursor',p->>'next_cursor',(p->>'is_terminal')::boolean,
      jsonb_array_length(items),items,
      pg_catalog.encode(pg_catalog.sha256(pg_catalog.convert_to(items::text,'UTF8')),'hex'),
      p_holder_id,p_fencing_token,false
    ) returning provider_positions into stored_count;
    total_count := total_count + stored_count;
  end loop;
  return query select n,total_count;
end
$$;
revoke all on function raf_private.drive_folder_capture_append_batch_v1(text,bigint,jsonb) from public,anon,authenticated;
comment on function raf_private.drive_folder_capture_append_batch_v1(text,bigint,jsonb) is
'RAFAELIA private fenced, bounded, transactional appends; no overwrite, source-byte proof or checkpoint promotion.';
