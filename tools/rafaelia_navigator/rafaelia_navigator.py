#!/usr/bin/env python3
"""RAFAELIA Navigator Private V1 — stdlib-only local indexer/query tool."""
from __future__ import annotations
import argparse,csv,hashlib,json,re,sqlite3,sys,tempfile,subprocess
from datetime import datetime,timezone
from pathlib import Path

CHUNK=1<<20
SCHEMA='RAFAELIA_NAVIGATOR_V1'

def now(): return datetime.now(timezone.utc).isoformat().replace('+00:00','Z')
def htext(s): return hashlib.sha256(s.encode('utf-8','replace')).hexdigest()
def hfile(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(CHUNK),b''): h.update(b)
 return h.hexdigest()
def norm(s): return re.sub(r'\s+',' ',s).strip()
def scalar(v): return str(v) if isinstance(v,(str,int,float,bool)) else None

def jarray(path):
 dec=json.JSONDecoder(); buf=''; pos=0; eof=False
 with path.open('r',encoding='utf-8-sig',errors='replace') as f:
  def more():
   nonlocal buf,pos,eof
   if eof:return False
   c=f.read(CHUNK)
   if not c:eof=True;return False
   buf=buf[pos:]+c;pos=0;return True
  if not more(): raise ValueError('empty json')
  while pos>=len(buf) or buf[pos].isspace():
   if pos<len(buf):pos+=1
   elif not more():raise ValueError('empty json')
  if buf[pos]!='[':raise ValueError('top-level is not array')
  pos+=1
  while True:
   while True:
    while pos<len(buf) and (buf[pos].isspace() or buf[pos]==','):pos+=1
    if pos<len(buf):break
    if not more():raise ValueError('truncated array')
   if buf[pos]==']':return
   while True:
    try:v,end=dec.raw_decode(buf,pos);break
    except json.JSONDecodeError:
     if not more():raise ValueError('invalid/truncated array')
   yield v;pos=end

def records(path):
 with path.open('r',encoding='utf-8-sig',errors='replace') as f: pre=f.read(4096).lstrip()
 if pre.startswith('['):yield from jarray(path)
 else:
  with path.open('r',encoding='utf-8-sig',errors='replace') as f:yield json.load(f)

def text_of(msg):
 c=msg.get('content'); typ='unknown'; out=[]
 if isinstance(c,dict):
  typ=str(c.get('content_type') or c.get('type') or 'unknown'); p=c.get('parts')
  if isinstance(p,list):
   for x in p:
    if isinstance(x,str):out.append(x)
    elif x is not None:out.append(json.dumps(x,ensure_ascii=False,sort_keys=True))
  elif isinstance(c.get('text'),str):out.append(c['text'])
 elif isinstance(c,str):typ='text';out=[c]
 return norm('\n'.join(out)),typ

def role(msg):
 a=msg.get('author');return str(a.get('role') or 'unknown') if isinstance(a,dict) else 'unknown'
def assets(v):
 out=[]
 if isinstance(v,dict):
  for k,x in v.items():
   if str(k).lower() in {'asset_pointer','file_id','asset_id','upload_id'} and isinstance(x,str):out.append(x)
   out+=assets(x)
 elif isinstance(v,list):
  for x in v:out+=assets(x)
 return list(dict.fromkeys(out))
def deep(v,names):
 names={x.lower() for x in names};stack=[v]
 while stack:
  x=stack.pop()
  if isinstance(x,dict):
   for k,y in x.items():
    if str(k).lower() in names and scalar(y):return scalar(y)
    if isinstance(y,(dict,list)):stack.append(y)
  elif isinstance(x,list):stack+=x
 return None
def alltext(v,limit=128000):
 out=[];n=0;stack=[v]
 while stack and n<limit:
  x=stack.pop()
  if isinstance(x,str):
   x=norm(x)
   if x:out.append(x);n+=len(x)
  elif isinstance(x,dict):stack+=reversed(list(x.values()))
  elif isinstance(x,list):stack+=reversed(x)
 return norm('\n'.join(out))[:limit]

class Seg:
 def __init__(self,d,prefix,maxn):self.d=d;self.prefix=prefix;self.maxn=maxn;self.i=0;self.n=0;self.f=None
 def put(self,x):
  if self.f is None or self.n>=self.maxn:
   self.close();self.i+=1;self.n=0;self.f=(self.d/f'{self.prefix}-{self.i:05d}.jsonl.txt').open('x',encoding='utf-8')
  self.f.write(json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n');self.n+=1
 def close(self):
  if self.f:self.f.close();self.f=None

class Build:
 def __init__(self,src,out,maxfiles=None,seg=5000):
  self.src=src;self.out=out;self.idx=out/'DRIVE_SEARCH_INDEX';self.rec=out/'RECEIPTS';self.idx.mkdir(parents=True,exist_ok=True);self.rec.mkdir(parents=True,exist_ok=True)
  self.db=out/'RAFAELIA_NAVIGATOR.sqlite3';self.cp=self.rec/'CHECKPOINTS.jsonl';self.c=sqlite3.connect(self.db);self.c.execute('pragma journal_mode=wal');self.fts=True;self.maxfiles=maxfiles
  self.msgseg=Seg(self.idx,'MESSAGES',seg);self.codseg=Seg(self.idx,'CODEX',seg);self.srcseg=Seg(self.idx,'SOURCES',seg);self.count={'files':0,'conversations':0,'messages':0,'codex':0,'assets':0,'duplicates':0,'errors':0};self.schema()
 def schema(self):
  self.c.executescript('''
 create table if not exists source_files(path text primary key,sha256 text,bytes integer,mtime_ns integer,kind text,status text,processed_at text,records integer,error text);
 create table if not exists conversations(conversation_id text primary key,title_hash text,create_time text,update_time text,source_path text,source_pointer text,structural_hash text,privacy_class text,epistemic_state text,claim_allowed integer);
 create table if not exists messages(message_id text primary key,conversation_id text,node_id text,parent_id text,role text,create_time text,content_type text,text text,text_hash text,source_path text,source_pointer text,asset_refs_json text,error_json text,privacy_class text,epistemic_state text,claim_allowed integer);
 create index if not exists msg_conv on messages(conversation_id);create index if not exists msg_hash on messages(text_hash);
 create table if not exists codex_records(record_id text primary key,task text,repository text,branch text,commit_sha text,pr text,path text,diff_hash text,text text,text_hash text,source_path text,source_pointer text,privacy_class text,epistemic_state text,claim_allowed integer);
 create table if not exists assets(asset_key text primary key,original_name text,stored_name text,conversation_id text,message_id text,source_path text,source_pointer text,claim_allowed integer);''')
  try:
   self.c.execute('create virtual table if not exists messages_fts using fts5(message_id unindexed,conversation_id unindexed,role,text)');self.c.execute('create virtual table if not exists codex_fts using fts5(record_id unindexed,repository,path,text)')
  except sqlite3.OperationalError:self.fts=False
  self.c.commit()
 def files(self):
  p=[]
  for pat in ('conversations-*.json','codex-*.json','conversation_asset_file_names.json','export_manifest.json'):p+=list(self.src.rglob(pat))
  p=sorted(set(p),key=lambda x:x.relative_to(self.src).as_posix());return p[:self.maxfiles] if self.maxfiles else p
 def checkpoint(self,x):
  prev='0'*64
  if self.cp.exists():
   lines=[z for z in self.cp.read_text(encoding='utf-8').splitlines() if z.strip()]
   if lines:prev=json.loads(lines[-1]).get('event_hash',prev)
  x={'schema':SCHEMA,'timestamp_utc':now(),**x,'predecessor_event_hash':prev};x['event_hash']=htext(json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(',',':')))
  with self.cp.open('a',encoding='utf-8') as f:f.write(json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
 def conv(self,p,rel):
  n=0
  for ci,v in enumerate(records(p)):
   if not isinstance(v,dict):continue
   n+=1;cid=str(v.get('id') or v.get('conversation_id') or htext(f'{rel}:{ci}'));mp=v.get('mapping') if isinstance(v.get('mapping'),dict) else {};sh=htext('\n'.join(sorted(f'{k}>{x.get("parent")}' for k,x in mp.items() if isinstance(x,dict))))
   self.c.execute('insert or replace into conversations values(?,?,?,?,?,?,?,?,?,0)',(cid,htext(str(v.get('title') or '')),scalar(v.get('create_time')),scalar(v.get('update_time')),rel,f'{rel}#conversation[{ci}]',sh,'PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED'));self.count['conversations']+=1
   for nid,node in mp.items():
    if not isinstance(node,dict) or not isinstance(node.get('message'),dict):continue
    m=node['message'];mid=str(m.get('id') or nid);txt,typ=text_of(m);th=htext(txt);ar=assets(m);ptr=f'{rel}#conversation[{ci}].mapping[{json.dumps(str(nid))}]';err=m.get('status') if m.get('status') not in (None,'finished_successfully') else None
    self.c.execute('insert or replace into messages values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)',(mid,cid,str(nid),scalar(node.get('parent')),role(m),scalar(m.get('create_time')),typ,txt,th,rel,ptr,json.dumps(ar,ensure_ascii=False),json.dumps(err,ensure_ascii=False) if err is not None else None,'PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED'))
    if self.fts:self.c.execute('delete from messages_fts where message_id=?',(mid,));self.c.execute('insert into messages_fts values(?,?,?,?)',(mid,cid,role(m),txt))
    if self.c.execute('select count(*) from messages where text_hash=?',(th,)).fetchone()[0]>1:self.count['duplicates']+=1
    self.msgseg.put({'kind':'message','conversation_id':cid,'message_id':mid,'node_id':str(nid),'parent_id':scalar(node.get('parent')),'role':role(m),'create_time':scalar(m.get('create_time')),'content_type':typ,'text':txt,'text_hash':th,'asset_refs':ar,'source_pointer':ptr,'source_path':rel,'privacy_class':'PRIVATE_DEFAULT_DENY','epistemic_state':'SOURCE_OBSERVED','target_adapter':['GAIA_L1_L3','RMRALPHA'],'claim_allowed':False});self.count['messages']+=1
  return n
 def codex(self,p,rel):
  n=0
  for i,v in enumerate(records(p)):
   if not isinstance(v,dict):continue
   n+=1;txt=alltext(v);rid=str(v.get('id') or v.get('task_id') or htext(f'{rel}:{i}:{txt[:256]}'));repo=deep(v,['repository','repo','repository_full_name']);br=deep(v,['branch','head_ref','base_ref']);com=deep(v,['commit','commit_sha','sha']);pr=deep(v,['pr','pull_request','pull_request_number']);path=deep(v,['path','file_path','filepath']);diff=deep(v,['diff','patch']);task=deep(v,['task','title','prompt','instruction']);ptr=f'{rel}#record[{i}]';th=htext(txt);dh=htext(diff) if diff else None
   self.c.execute('insert or replace into codex_records values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)',(rid,task,repo,br,com,pr,path,dh,txt,th,rel,ptr,'PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED'))
   if self.fts:self.c.execute('delete from codex_fts where record_id=?',(rid,));self.c.execute('insert into codex_fts values(?,?,?,?)',(rid,repo or '',path or '',txt))
   self.codseg.put({'kind':'codex','record_id':rid,'task':task,'repository':repo,'branch':br,'commit':com,'pr':pr,'path':path,'diff_hash':dh,'text':txt,'text_hash':th,'source_pointer':ptr,'source_path':rel,'privacy_class':'PRIVATE_DEFAULT_DENY','epistemic_state':'SOURCE_OBSERVED','target_adapter':['RMRALPHA','RMRIA_HOLD'],'claim_allowed':False});self.count['codex']+=1
  return n
 def assetmanifest(self,p,rel):
  n=0
  for top in records(p):
   it=top.items() if isinstance(top,dict) else enumerate(top) if isinstance(top,list) else []
   for k,v in it:
    n+=1;stored=scalar(v) if not isinstance(v,(dict,list)) else deep(v,['file_name','stored_name','name']);orig=deep(v,['original_name','display_name','filename']) if isinstance(v,(dict,list)) else None;cid=deep(v,['conversation_id']) if isinstance(v,(dict,list)) else None;mid=deep(v,['message_id']) if isinstance(v,(dict,list)) else None
    self.c.execute('insert or replace into assets values(?,?,?,?,?,?,?,0)',(str(k),orig,stored,cid,mid,rel,f'{rel}#{json.dumps(str(k))}'));self.count['assets']+=1
  return n
 def run(self):
  fs=self.files()
  if not fs:print('no eligible files',file=sys.stderr);return 2
  for i,p in enumerate(fs,1):
   rel=p.relative_to(self.src).as_posix();st=p.stat();dig=hfile(p);old=self.c.execute('select sha256,bytes,mtime_ns,status from source_files where path=?',(rel,)).fetchone()
   if old and old==(dig,st.st_size,st.st_mtime_ns,'COMPLETE'):
    self.checkpoint({'event':'SKIP_UNCHANGED','cursor_initial':rel,'cursor_final':rel,'source_hash':dig});print(f'SKIP {i}/{len(fs)} {rel}');continue
   kind='conversation' if p.name.startswith('conversations-') else 'codex' if p.name.startswith('codex-') else 'asset_manifest' if p.name=='conversation_asset_file_names.json' else 'manifest'
   try:
    with self.c:
     n=self.conv(p,rel) if kind=='conversation' else self.codex(p,rel) if kind=='codex' else self.assetmanifest(p,rel) if kind=='asset_manifest' else sum(1 for _ in records(p))
     self.c.execute("insert into source_files values(?,?,?,?,?,'COMPLETE',?,?,null) on conflict(path) do update set sha256=excluded.sha256,bytes=excluded.bytes,mtime_ns=excluded.mtime_ns,kind=excluded.kind,status='COMPLETE',processed_at=excluded.processed_at,records=excluded.records,error=null",(rel,dig,st.st_size,st.st_mtime_ns,kind,now(),n))
    self.count['files']+=1;self.srcseg.put({'path':rel,'kind':kind,'bytes':st.st_size,'sha256':dig,'records':n,'privacy_class':'PRIVATE_DEFAULT_DENY','claim_allowed':False});self.checkpoint({'event':'FILE_COMPLETE','cursor_initial':f'{rel}#record[0]','cursor_final':f'{rel}#record[{max(0,n-1)}]','source_hash':dig,'records':n,'bytes':st.st_size});print(f'CHECKPOINT {i}/{len(fs)} COMPLETE {rel} records={n}')
   except Exception as e:
    self.count['errors']+=1;self.c.execute("insert into source_files values(?,?,?,?,?,'BLOCKED',?,0,?) on conflict(path) do update set status='BLOCKED',processed_at=excluded.processed_at,error=excluded.error",(rel,dig,st.st_size,st.st_mtime_ns,kind,now(),f'{type(e).__name__}: {e}'));self.c.commit();self.checkpoint({'event':'FILE_BLOCKED','cursor_initial':f'{rel}#record[0]','cursor_final':f'{rel}#record[0]','source_hash':dig,'error':f'{type(e).__name__}: {e}'});print(f'BLOCKED {rel}: {e}',file=sys.stderr)
  self.msgseg.close();self.codseg.close();self.srcseg.close();self.outputs();self.c.execute('pragma wal_checkpoint(truncate)');self.c.close();print('RAFAELIA_NAVIGATOR_PASS');[print(f'{k}={v}') for k,v in self.count.items()];return 0 if not self.count['errors'] else 4
 def outputs(self):
  rows=self.c.execute('select path,kind,bytes,sha256,records,status,error from source_files order by path').fetchall();cov=self.out/'COVERAGE.csv'
  with cov.open('w',encoding='utf-8',newline='') as f:
   w=csv.writer(f);w.writerow(['path','kind','bytes','sha256','records','status','error','source','hash','index','graph','memory','receipt']);[w.writerow([*r,'F_OK','F_OK','F_OK' if r[5]=='COMPLETE' else 'TOKEN_VAZIO','TOKEN_VAZIO','TOKEN_VAZIO','F_OK']) for r in rows]
  man={'schema':SCHEMA,'created_at':now(),'source_root':str(self.src),'privacy_class':'PRIVATE_DEFAULT_DENY','claim_allowed':False,'training_executed':False,'fts5':self.fts,'counts':self.count,'artifacts':{'database':self.db.name,'coverage':cov.name,'checkpoints':str(self.cp.relative_to(self.out))},'adapters':{'GAIA_phi':['L1_raw','L2_parsed','L3_indexed'],'Rafaelia_Private':['RMRALPHA'],'RMRIA':'HOLD'},'F_ok':'structural sources indexed with lineage','F_gap':'asset bytes and graph/memory reconciliation remain separate','F_next':'publish compact segments privately and validate retrieval round-trip'}
  (self.out/'MANIFEST.json').write_text(json.dumps(man,ensure_ascii=False,indent=2,sort_keys=True)+'\n',encoding='utf-8');(self.out/'NAVIGATOR_ROOT.md').write_text('# RAFAELIA Navigator Private\n\nquestion → compact index → source_pointer → original verification\n',encoding='utf-8')

def query(db,q,kind='all',limit=20):
 c=sqlite3.connect(db);out=[]
 if kind in ('message','all'):
  try:rs=c.execute("select m.message_id,m.conversation_id,m.role,m.create_time,m.source_pointer,snippet(messages_fts,3,'[',']','…',16) from messages_fts join messages m using(message_id) where messages_fts match ? limit ?",(q,limit))
  except sqlite3.OperationalError:rs=c.execute('select message_id,conversation_id,role,create_time,source_pointer,text from messages where text like ? limit ?',(f'%{q}%',limit))
  out+=[{'kind':'message','message_id':r[0],'conversation_id':r[1],'role':r[2],'create_time':r[3],'source_pointer':r[4],'snippet':r[5]} for r in rs]
 if kind in ('codex','all') and len(out)<limit:
  n=limit-len(out)
  try:rs=c.execute("select x.record_id,x.repository,x.branch,x.commit_sha,x.pr,x.path,x.source_pointer,snippet(codex_fts,3,'[',']','…',16) from codex_fts join codex_records x using(record_id) where codex_fts match ? limit ?",(q,n))
  except sqlite3.OperationalError:rs=c.execute('select record_id,repository,branch,commit_sha,pr,path,source_pointer,text from codex_records where text like ? limit ?',(f'%{q}%',n))
  out+=[{'kind':'codex','record_id':r[0],'repository':r[1],'branch':r[2],'commit':r[3],'pr':r[4],'path':r[5],'source_pointer':r[6],'snippet':r[7]} for r in rs]
 c.close();print(json.dumps(out,ensure_ascii=False,indent=2));return 0

def selftest():
 with tempfile.TemporaryDirectory() as td:
  b=Path(td);s=b/'NOVOexport';o=b/'out';s.mkdir();(s/'conversations-000.json').write_text(json.dumps([{'id':'c1','title':'private','mapping':{'n1':{'parent':None,'message':{'id':'m1','author':{'role':'user'},'content':{'content_type':'text','parts':['Termux ARMv7']}}},'n2':{'parent':'n1','message':{'id':'m2','author':{'role':'assistant'},'content':{'content_type':'text','parts':['deterministic']}}}}}]),encoding='utf-8');(s/'codex-000.json').write_text(json.dumps([{'id':'t1','repository':'r/x','branch':'main','commit_sha':'abc','path':'a.c','text':'RafPolimata'}]),encoding='utf-8');(s/'conversation_asset_file_names.json').write_text(json.dumps({'a1':{'file_name':'f.dat','conversation_id':'c1','message_id':'m1'}}),encoding='utf-8')
  assert Build(s,o,seg=1).run()==0;c=sqlite3.connect(o/'RAFAELIA_NAVIGATOR.sqlite3');assert c.execute('select count(*) from messages').fetchone()[0]==2;assert c.execute("select parent_id from messages where message_id='m2'").fetchone()[0]=='n1';assert c.execute('select count(*) from codex_records').fetchone()[0]==1;assert c.execute('select count(*) from assets').fetchone()[0]==1;c.close();assert Build(s,o).run()==0;assert 'SKIP_UNCHANGED' in (o/'RECEIPTS'/'CHECKPOINTS.jsonl').read_text();print('SELFTEST_PASS');return 0

def main():
 p=argparse.ArgumentParser();sp=p.add_subparsers(dest='cmd',required=True);b=sp.add_parser('build');b.add_argument('source',type=Path);b.add_argument('output',type=Path);b.add_argument('--max-files',type=int);b.add_argument('--segment-records',type=int,default=5000);q=sp.add_parser('query');q.add_argument('database',type=Path);q.add_argument('query');q.add_argument('--kind',choices=['message','codex','all'],default='all');q.add_argument('--limit',type=int,default=20);sp.add_parser('selftest');a=p.parse_args()
 if a.cmd=='build':a.output.mkdir(parents=True,exist_ok=True);return Build(a.source.expanduser().resolve(),a.output.expanduser().resolve(),a.max_files,max(1,a.segment_records)).run()
 if a.cmd=='query':return query(a.database,a.query,a.kind,a.limit)
 return selftest()
if __name__=='__main__':raise SystemExit(main())
