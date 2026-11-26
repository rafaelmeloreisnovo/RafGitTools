-- RAFAELIA Ω | private append-only Drive folder page manifests V1
-- For authorized scoped captures only. No content bytes, public ACL, overwrite, or checkpoint promotion.
-- Review current authority, lease/fencing, version, producer, credentials and rollback before migration.
create table raf_private.drive_folder_capture_pages_v1 (
  capture_id text not null check (capture_id ~ '^RAF_DRIVE_FOLDERS_[A-Za-z0-9_]{8,100}$'),
  page_index integer not null check (page_index >= 0),
  provider text not null default 'google_drive' check (provider='google_drive'),
  account_profile_id text not null check (account_profile_id ~ '^[0-9]{15,25}$'),
  query_contract text not null,
  requested_at timestamptz not null,
  observed_at timestamptz not null check (observed_at >= requested_at),
  request_cursor text,
  next_cursor text,
  is_terminal boolean not null check (is_terminal = (next_cursor is null)),
  provider_positions integer not null check (provider_positions between 0 and 1000),
  folder_ids_and_metadata jsonb not null check (jsonb_typeof(folder_ids_and_metadata)='array'),
  page_manifest_sha256 text not null check (page_manifest_sha256 ~ '^[0-9a-f]{64}$'),
  lease_holder_id text not null,
  fencing_token bigint not null check (fencing_token > 0),
  claim_allowed boolean not null default false check (claim_allowed=false),
  recorded_at timestamptz not null default clock_timestamp(),
  primary key(capture_id,page_index),
  constraint capture_count_eq_json check(provider_positions=jsonb_array_length(folder_ids_and_metadata)),
  constraint capture_first_cursor check(page_index <> 0 or request_cursor is null)
);
alter table raf_private.drive_folder_capture_pages_v1 enable row level security;
alter table raf_private.drive_folder_capture_pages_v1 force row level security;
revoke all on raf_private.drive_folder_capture_pages_v1 from public, anon, authenticated;

create function raf_private.drive_folder_capture_pages_guard_v1()
returns trigger
language plpgsql
set search_path to ''
as $$
declare prev raf_private.drive_folder_capture_pages_v1%rowtype;
begin
  if not raf_private.global_writer_lease_validate_v1(
     'RAFAELIA:INVENTORY:DRIVE_GITHUB',new.lease_holder_id,new.fencing_token
  ) then
     raise exception 'CAPTURE_LEASE_FENCING_INVALID' using errcode='42501';
  end if;
  if encode(sha256(convert_to(new.folder_ids_and_metadata::text,'UTF8')),'hex')
     is distinct from new.page_manifest_sha256 then
     raise exception 'CAPTURE_PAGE_DIGEST_MISMATCH' using errcode='22023';
  end if;
  if new.page_index > 0 then
    select * into prev
      from raf_private.drive_folder_capture_pages_v1
     where capture_id=new.capture_id and page_index=new.page_index-1;
    if not found then
      raise exception 'CAPTURE_PREDECESSOR_MISSING' using errcode='22023';
    end if;
    if prev.is_terminal or prev.next_cursor is distinct from new.request_cursor
       or prev.query_contract is distinct from new.query_contract
       or prev.account_profile_id is distinct from new.account_profile_id
       or prev.provider is distinct from new.provider then
      raise exception 'CAPTURE_CURSOR_OR_SCOPE_CHAIN_MISMATCH' using errcode='22023';
    end if;
    if prev.observed_at > new.requested_at then
      raise exception 'CAPTURE_NONMONOTONIC_TIME' using errcode='22023';
    end if;
  end if;
  return new;
end
$$;
create trigger drive_folder_capture_insert_guard_v1
before insert on raf_private.drive_folder_capture_pages_v1
for each row execute function raf_private.drive_folder_capture_pages_guard_v1();

-- Use an existing guarded append-only function; preserving original page rows is essential.
create trigger drive_folder_capture_immutable_v1
before update or delete on raf_private.drive_folder_capture_pages_v1
for each row execute function raf_private.deny_omega_catalog_mutation_v1();
create trigger drive_folder_capture_no_truncate_v1
before truncate on raf_private.drive_folder_capture_pages_v1
for each statement execute function raf_private.deny_omega_catalog_mutation_v1();

comment on table raf_private.drive_folder_capture_pages_v1 is
'RAFAELIA Ω provider-folder metadata manifest custody, private append-only, fenced, cursor-chained. Never source byte hash or global completeness claim.';
