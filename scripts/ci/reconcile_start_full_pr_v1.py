"""Reconcile provider metadata for the reviewed START full PR lane.
Not an authentication adapter. No lifecycle, status, claim or skip authority.
Only the pinned workflow layout is supported; unknown metadata stays missing.
"""
import re

REPO = 'rafaelmeloreisnovo/RafGitTools'
WORKFLOW_ID = 352645907
WORKFLOW_BLOB = 'ed04ceb0b3dc95d0d36314f827bbba3edeea4e02'
STAGES = {
    'plan': ('00 · Plan / roteamento',),
    'topology': ('01 · Workflow topology / START owner + scoped workflows',),
    'coherence': ('02 · Coherence / anti-regression',),
    'python_tests': ('03 · Python / deterministic tests',),
    'federation': ('04 · Federation / master-index + provenance',),
    'docs': ('05 · Documentation / claim boundary',),
    'docs_integrity': ('05Z · Docs Integrity / source references (scoped)',),
    'semchaos_scoped': ('10S · SEMCHAOS typed-token scoped gate',),
    'android': ('06 · Android / test + lint + devDebug APK',),
    'security': ('07 · CodeQL / java-kotlin', '07 · CodeQL / actions'),
    'provider_actions': ('08A · Provider actions / exact cross-repo read-only',),
    'private_ci_bridge': ('08B · Private CI bridge / exact-SHA secretless replay',),
    'provider_environments': ('08P · Provider environments / main enforcement',),
    'release': ('08 · Signed release',),
    'public_data_custody': ('Public data custody / PLECT public-money lane',),
    'promotion_hold_pat_agent': ('09Z · PAT_AGENTS / P0 title hold only (owner manual)',),
}
REQUIRED = frozenset(('android','security','docs','docs_integrity','semchaos_scoped','federation'))
CORE = frozenset(('plan','topology','coherence','python_tests'))
EXTRA = {
    '10Z · RAFAELIA Atlas standalone · exact source': 'success',
    '10 · BLAKE3 triple APK (RMR / fork / official; dual ABI)': 'skipped',
    '08Q · Mapa PAT_ENV / read-only provider authority': 'skipped',
    '09Y · PAT_AGENTS / identity and org preflight (read-only)': 'skipped',
    '09X · Repository factory / offline plan only': 'skipped',
    '09 · Final receipt / fechamento': 'success',
}
EXPECTED = {j: ('success' if k in REQUIRED|CORE else 'skipped') for k, names in STAGES.items() for j in names} | EXTRA


def reconcile(context, run, jobs, artifact):
    """All inputs are independent provider metadata, never receipt self-declarations.

    This scoped crosscheck does not establish that the caller authenticated them.
    Incomplete pagination or unknown attempt/SHA stays a blocker. It deliberately
    does not produce attestations for the lifecycle reuse gate.
    """
    def out(state, reasons=(), **fields):
        return dict(state=state, reasons=sorted(set(reasons)), skip_allowed=False,
                    claim_allowed=False, check_status_write_allowed=False,
                    lifecycle_binding_ready=False, **fields)
    if not all(isinstance(x,dict) for x in (context,run,artifact)) or not isinstance(jobs,list):
        return out('REJECTED', ['INVALID_INPUT'])
    if (context.get('repository') != REPO or context.get('workflow_blob') != WORKFLOW_BLOB
            or context.get('event') != 'pull_request' or context.get('mode') != 'full'
            or context.get('base_ref') not in ('main','develop')):
        return out('REJECTED',['UNREVIEWED_SCOPE'])
    sha=context.get('execution_sha'); attempt=context.get('attempt'); rid=run.get('id')
    if (not isinstance(sha,str) or re.fullmatch('[0-9a-f]{40}',sha) is None
            or type(attempt) is not int or attempt<=0 or type(rid) is not int or rid<=0
            or type(run.get('workflow_id')) is not int or run['workflow_id']!=WORKFLOW_ID
            or run.get('status')!='completed' or run.get('conclusion')!='success'):
        return out('REJECTED',['INVALID_RUN'])
    if len(jobs)!=len(EXPECTED):
        return out('REJECTED',['JOB_COUNT_INCOMPLETE_OR_EXTRA'])
    missing=[]; by_name={}; ids=set()
    for field, expected in (('run_attempt',attempt),('head_sha',sha),('event','pull_request')):
        if field not in run or run[field] is None: missing.append('RUN:'+field)
        elif type(run[field]) is not type(expected) or run[field]!=expected: return out('REJECTED',['RUN_CONTEXT:'+field])
    for job in jobs:
        if not isinstance(job,dict) or not isinstance(job.get('name'),str): return out('REJECTED',['INVALID_JOB'])
        n=job['name']; jid=job.get('id')
        if n not in EXPECTED or n in by_name or type(jid) is not int or jid<=0 or jid in ids:
            return out('REJECTED',['JOB_IDENTITY_DUPLICATE_OR_UNKNOWN'])
        if type(job.get('run_id')) is not int or job['run_id']!=rid or job.get('status')!='completed' or job.get('conclusion')!=EXPECTED[n]:
            return out('REJECTED',['JOB_TERMINAL_MISMATCH:'+n])
        for field, expected in (('run_attempt',attempt),('head_sha',sha)):
            if field not in job or job[field] is None: missing.append('JOB:'+field+':'+n)
            elif type(job[field]) is not type(expected) or job[field]!=expected: return out('REJECTED',['JOB_CONTEXT:'+field+':'+n])
        by_name[n]=job;ids.add(jid)
    ar=artifact.get('workflow_run'); digest=artifact.get('digest')
    if (type(artifact.get('id')) is not int or artifact['id']<=0 or artifact.get('expired') is not False
            or not isinstance(digest,str) or re.fullmatch('sha256:[0-9a-f]{64}',digest) is None
            or not isinstance(ar,dict) or type(ar.get('id')) is not int or ar['id']!=rid or ar.get('head_sha')!=sha
            or ar.get('repository_id')!=1131213900 or ar.get('head_repository_id')!=1131213900):
        return out('REJECTED',['ARTIFACT_CONTEXT'])
    if artifact.get('name')!='start-09-receipt-'+str(rid): return out('REJECTED',['ARTIFACT_NAME_SCOPE'])
    if 'run_attempt' not in artifact or artifact['run_attempt'] is None: missing.append('ARTIFACT:run_attempt')
    elif type(artifact['run_attempt']) is not int or artifact['run_attempt']!=attempt: return out('REJECTED',['ARTIFACT_ATTEMPT'])
    if context.get('pagination_complete') is not True: missing.append('JOB_PAGINATION_NOT_PROVEN')
    stages={k: ('SUCCESS' if all(by_name[j]['conclusion']=='success' for j in names) else 'TOKEN_VAZIO_NOT_SELECTED') for k,names in STAGES.items()}
    return out('OBSERVATION_ONLY', missing+['AUTHENTICATED_ADAPTER_NOT_ESTABLISHED','LIFECYCLE_AND_CLOSURE_NOT_PROVEN'],
               expected_required_gates=dict.fromkeys(sorted(REQUIRED),True), expected_stages=stages,
               run_id=rid, final_job_id=by_name['09 · Final receipt / fechamento']['id'], artifact_id=artifact['id'],
               zip_sha256=digest[7:], metadata_complete=not missing)
