-- RAFAELIA Ω | synthetic-only PostgreSQL gate falsification matrix V2
-- All values are invented test inputs; no private provider identities, real cursors or source bytes.
-- READ ONLY; runnable against local PostgreSQL, or via private SQL runner. Never publish query results with private data.
-- This is a focused falsification test of predicates, not proof of Drive snapshot isolation or B0160 recovery.
WITH fixture(case_name,page_index,request_cursor,next_cursor,is_terminal,
             stored_digest,recalc_digest,claim_allowed,provider,account,contract) AS (
 VALUES
 ('valid',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('valid',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('page_gap',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('page_gap',2,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('missing_page_zero',1,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('missing_page_zero',2,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('broken_cursor',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('broken_cursor',1,'WRONG',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('early_terminal',0,NULL,'synthetic_cursor_1',TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('early_terminal',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('two_terminal_pages',0,NULL,NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('two_terminal_pages',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('not_terminal',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('not_terminal',1,'synthetic_cursor_1','SYNTHETIC_NEXT',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('terminal_cursor_mismatch',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('terminal_cursor_mismatch',1,'synthetic_cursor_1','SYNTHETIC_NEXT',TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('null_digest',0,NULL,'synthetic_cursor_1',FALSE,NULL,'h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('null_digest',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('wrong_digest',0,NULL,'synthetic_cursor_1',FALSE,'WRONG','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('wrong_digest',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('claim_true',0,NULL,'synthetic_cursor_1',FALSE,'h','h',TRUE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('claim_true',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('provider_drift',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('provider_drift',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'OTHER','synthetic_account','synthetic_contract'),
 ('account_drift',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('account_drift',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','OTHER','synthetic_contract'),
 ('query_contract_drift',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('query_contract_drift',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','OTHER'),
 ('blank_provider',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'','synthetic_account','synthetic_contract'),
 ('blank_provider',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'','synthetic_account','synthetic_contract'),
 ('blank_account',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','   ','synthetic_contract'),
 ('blank_account',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','   ','synthetic_contract'),
 ('blank_query_contract',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account',''),
 ('blank_query_contract',1,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account',''),
 ('duplicate_page_index',0,NULL,'synthetic_cursor_1',FALSE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract'),
 ('duplicate_page_index',0,'synthetic_cursor_1',NULL,TRUE,'h','h',FALSE,'synthetic_provider','synthetic_account','synthetic_contract')
), flags AS (
 SELECT f.*,row_number() OVER(PARTITION BY case_name ORDER BY page_index) rn,
        lag(next_cursor) OVER(PARTITION BY case_name ORDER BY page_index) prior,
        max(page_index) OVER(PARTITION BY case_name) final_index
 FROM fixture f
), scores AS (
 SELECT case_name,
        coalesce(bool_or(is_terminal),FALSE) legacy_reached_terminal,
        sum((stored_digest<>recalc_digest)::int) legacy_hash_failures,
        sum((rn>1 AND request_cursor IS DISTINCT FROM prior)::int) legacy_cursor_breaks,
        count(*) pages,
        count(DISTINCT page_index) distinct_page_indices,
        min(page_index) first_index,max(page_index) last_index,
        count(*) FILTER(WHERE page_index<>rn-1) page_index_errors,
        count(*) FILTER(WHERE rn=1 AND request_cursor IS NOT NULL) origin_errors,
        count(*) FILTER(WHERE rn>1 AND request_cursor IS DISTINCT FROM prior) cursor_errors,
        count(*) FILTER(WHERE is_terminal) terminals,
        count(*) FILTER(WHERE is_terminal AND page_index<>final_index) early_terminals,
        count(*) FILTER(WHERE (next_cursor IS NULL) IS DISTINCT FROM is_terminal) terminal_cursor_errors,
        count(*) FILTER(WHERE stored_digest IS DISTINCT FROM recalc_digest) digest_errors,
        count(*) FILTER(WHERE claim_allowed IS DISTINCT FROM FALSE) unsafe_claims,
        count(*) FILTER(WHERE provider IS NULL OR btrim(provider)='' OR
                              account IS NULL OR btrim(account)='' OR
                              contract IS NULL OR btrim(contract)='') metadata_identity_errors,
        count(DISTINCT provider) provider_variants,
        count(DISTINCT account) account_variants,
        count(DISTINCT contract) contract_variants
 FROM flags GROUP BY case_name
), graded AS (
 SELECT case_name,legacy_reached_terminal,legacy_hash_failures,legacy_cursor_breaks,
        (pages=2 AND distinct_page_indices=2 AND first_index=0 AND last_index=1 AND
         page_index_errors=0 AND origin_errors=0 AND cursor_errors=0 AND
         terminals=1 AND early_terminals=0 AND terminal_cursor_errors=0 AND
         digest_errors=0 AND unsafe_claims=0 AND metadata_identity_errors=0 AND
         provider_variants=1 AND account_variants=1 AND contract_variants=1) strict_accept
 FROM scores
), scope_fixture(scenario,capture_id,provider,account_id,query_contract) AS (
 VALUES
 ('valid_scope','A','p','a','q'), ('valid_scope','B','p','a','q'),
 ('provider_drift_scope','A','p','a','q'), ('provider_drift_scope','B','OTHER','a','q'),
 ('account_drift_scope','A','p','a','q'), ('account_drift_scope','B','p','OTHER','q'),
 ('contract_drift_scope','A','p','a','q'), ('contract_drift_scope','B','p','a','OTHER'),
 ('blank_account_scope','A','p','','q'), ('blank_account_scope','B','p','','q'),
 ('missing_b_scope','A','p','a','q')
), scope_results AS (
 SELECT scenario,
        (count(DISTINCT capture_id)=2 AND count(DISTINCT provider)=1 AND
         count(DISTINCT account_id)=1 AND count(DISTINCT query_contract)=1 AND
         count(*) FILTER (WHERE provider IS NULL OR btrim(provider)='' OR
                                account_id IS NULL OR btrim(account_id)='' OR
                                query_contract IS NULL OR btrim(query_contract)='')=0) same_scope
 FROM scope_fixture GROUP BY scenario
), scope_matrix AS (
 SELECT (count(*)=6 AND bool_and((scenario='valid_scope')=same_scope)) passed
 FROM scope_results
)
SELECT case_name,(case_name='valid') expected_accept,strict_accept,
       (strict_accept=(case_name='valid')) as expectation_met,
       legacy_reached_terminal,legacy_hash_failures,legacy_cursor_breaks,
       (count(*) OVER()=18 AND bool_and(strict_accept=(case_name='valid')) OVER() AND
        (SELECT passed FROM scope_matrix)) AS entire_matrix_pass
FROM graded ORDER BY case_name;