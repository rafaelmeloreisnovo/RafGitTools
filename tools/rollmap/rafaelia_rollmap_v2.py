#!/usr/bin/env python3
"""RAFAELIA Roll-map V2: source-preserving, read-only indexing + boolean discovery.
No network calls, no writes to ingested roots. Output DB is a disposable derived index.
"""
import argparse, collections, fnmatch, hashlib, io, json, os, pathlib, re, shlex, sqlite3, sys, tarfile, unicodedata, zipfile

TEXT_EXT={'.json','.jsonl','.txt','.md','.csv','.tsv','.yml','.yaml','.py','.c','.h','.sh','.java','.rs','.xml','.svg','.ini','.log','.toml','.gradle'}
ARCHIVE_RE=re.compile(r'(?i)\.(zip|tar|tgz|tar\.gz)$')
HEX_RE=re.compile(r'(?i)(?<![0-9a-f])[0-9a-f]{8,64}(?![0-9a-f])')
WORD_RE=re.compile(r'[^\W_]+', re.UNICODE)
H=hashlib.sha256

def canon(s):return unicodedata.normalize('NFC',str(s)).casefold()
def sha(b):return H(b).hexdigest()
def bsha(p):
 h=H()
 with open(p,'rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def jsonbytes(o):return json.dumps(o,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode('utf-8')
def put_node(c,n):
 n={**dict(id=None,source='',kind='',name='',path='',size=None,sha='',header='',meta={}),**n}
 if not n['id']:raise ValueError('node has no ID')
 c.execute('INSERT OR REPLACE INTO nodes(id,source,kind,name,path,size,sha,header,meta,sort_name,sort_path) VALUES (?,?,?,?,?,?,?,?,?,?,?)',(
  n['id'],n['source'],n['kind'],n['name'],n['path'],n['size'],n['sha'] or '',n['header'] or '',json.dumps(n['meta'],ensure_ascii=False,sort_keys=True),canon(n['name']),canon(n['path'])))

def edge(c,a,b,rel,status='OBSERVED',note=''):
 if a==b:return
 eid=sha(jsonbytes([a,rel,b,status,note]))
 c.execute('INSERT OR IGNORE INTO edges(id,src,dst,rel,status,note) VALUES (?,?,?,?,?,?)',(eid,a,b,rel,status,note))

def init(db):
 c=sqlite3.connect(db)
 c.execute('PRAGMA journal_mode=WAL')
 c.executescript('''
 CREATE TABLE IF NOT EXISTS nodes(id TEXT PRIMARY KEY,source TEXT,kind TEXT,name TEXT,path TEXT,size INTEGER,sha TEXT,header TEXT,meta TEXT,sort_name TEXT,sort_path TEXT);
 CREATE TABLE IF NOT EXISTS edges(id TEXT PRIMARY KEY,src TEXT,dst TEXT,rel TEXT,status TEXT,note TEXT);
 CREATE INDEX IF NOT EXISTS ix_node_name ON nodes(sort_name);
 CREATE INDEX IF NOT EXISTS ix_node_path ON nodes(sort_path);
 CREATE INDEX IF NOT EXISTS ix_node_size ON nodes(size);
 CREATE INDEX IF NOT EXISTS ix_node_hash ON nodes(sha);
 CREATE INDEX IF NOT EXISTS ix_edges_src ON edges(src);
 CREATE INDEX IF NOT EXISTS ix_edges_dst ON edges(dst);
 CREATE INDEX IF NOT EXISTS ix_edges_rel ON edges(rel);
 CREATE TABLE IF NOT EXISTS grams(field TEXT,gram TEXT,node_id TEXT, PRIMARY KEY(field,gram,node_id)) WITHOUT ROWID;
 CREATE INDEX IF NOT EXISTS ix_grams_node ON grams(node_id);
 ''')
 try:
  c.execute("CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts5(name,path,header,sha,meta,ident,tokenize='trigram',content='')")
 except sqlite3.OperationalError:
  pass  # fallback to ordinary postings if SQLite lacks FTS5 trigram.
 return c

def import_v1(c,fn):
 with open(fn,encoding='utf-8') as f:d=json.load(f)
 names={n['id']:n['name'] for n in d['nodes']}
 parents={e['to']:e['from'] for e in d['edges'] if e['relation'] in ('CONTAINS_OBSERVED','CONTAINS_ARCHIVE_MEMBER')}
 def fullpath(i):
  seen=set(); parts=[]; at=i
  while at in names and at not in seen:
   seen.add(at);parts.append(names[at]);at=parents.get(at)
   if not at:break
  return '/'.join(reversed(parts))
 for n in d['nodes']:
  put_node(c,dict(id=n['id'],source='DRIVE_OBSERVED' if n['id'].startswith('drive:') else 'ARCHIVE_OBSERVED',kind=n['type'],name=n['name'],path=fullpath(n['id']),size=n.get('declared_uncompressed_bytes',n.get('metadata_size_bytes')),sha=n.get('local_sha256',''),meta={'provider_id':n.get('provider_id'),'v1_ref':fn,'v1_attributes':{k:v for k,v in n.items() if k not in {'id','type','name'}}}))
 for e in d['edges']:
  edge(c,e['from'],e['to'],e['relation'],'REPORTED_VERIFIED_V1' if e.get('verified') else 'OBSERVED',e.get('evidence',''))
 return len(d['nodes']),len(d['edges'])

def peek_text(path,limit=2048):
 if limit==0 or pathlib.Path(path).suffix.lower() not in TEXT_EXT:return ''
 with open(path,'rb') as f:b=f.read(limit)
 if b'\0' in b:return ''
 return b.decode('utf-8','replace')

def peek_member(name,reader,uncompressed,limit=2048):
 if pathlib.Path(name).suffix.lower() not in TEXT_EXT or uncompressed>limit*4:return ''
 try:return reader(limit).decode('utf-8','replace')
 except Exception:return ''

def archive_scan(c,container_id,archive_bytes,archive_name,outer_path,depth,remaining):
 if depth==0:return
 # Never extract paths. Local scanners store virtual entries only. Keep compressed/member budgets.
 seen_names=collections.Counter()
 def add(member_name,n,header,reader=None):
  member_name=member_name.replace('\\','/')
  if member_name.startswith('/') or '\0' in member_name or any(p in ('.','..') for p in member_name.split('/')):
   edge(c,container_id,container_id+':REJECTED:'+sha(member_name.encode()),'UNSAFE_MEMBER_REJECTED','BLOCKED');return
  seen_names[member_name]+=1;occurrence=seen_names[member_name]
  dirs=member_name.split('/')[:-1]
  parent=container_id
  for idx,part in enumerate(dirs):
   prefix='/'.join(dirs[:idx+1]);vid='virtualdir:'+sha((container_id+'\0'+prefix).encode('utf-8'))
   put_node(c,dict(id=vid,source='CONTAINER_INDEX',kind='virtual_directory',name=part,path=outer_path+'!/'+prefix,size=None,meta={'container_id':container_id,'virtual_prefix':prefix}))
   edge(c,parent,vid,'CONTAINS_VIRTUAL_MEMBER','OBSERVED','virtual path component; no extraction')
   parent=vid
  nid='container:'+sha((container_id+'\0'+member_name+'\0'+str(occurrence)).encode())
  member_path=outer_path+'!/'+member_name+(('#occurrence='+str(occurrence)) if occurrence>1 else '')
  put_node(c,dict(id=nid,source='CONTAINER_INDEX',kind='container_member',name=member_name.split('/')[-1],path=member_path,size=n,header=header,meta={'archive':container_id,'path_in_archive':member_name,'occurrence':occurrence,'digest_unverified':True}))
  edge(c,parent,nid,'CONTAINS_VIRTUAL_MEMBER','OBSERVED','archive central directory / tar headers; no extraction')
  if occurrence>1:edge(c,container_id,nid,'DUPLICATE_MEMBER_NAME','CANDIDATE','same archive member path appeared more than once')
  return nid
 try:
  if archive_name.lower().endswith('.zip'):
   with zipfile.ZipFile(io.BytesIO(archive_bytes)) as z:
    for f in z.infolist():
     if f.is_dir():continue
     if f.file_size>remaining[0]:continue
     reader=lambda n,fn=f.filename:z.open(fn).read(n)
     hd=peek_member(f.filename,reader,f.file_size)
     nid=add(f.filename,f.file_size,hd)
     if nid and depth!=1 and ARCHIVE_RE.search(f.filename) and f.file_size<=remaining[0]:
      bb=z.read(f);remaining[0]-=len(bb)
      archive_scan(c,nid,bb,f.filename,outer_path+'!/'+f.filename,depth-1 if depth>0 else -1,remaining)
  elif ARCHIVE_RE.search(archive_name):
   with tarfile.open(fileobj=io.BytesIO(archive_bytes),mode='r:*') as t:
    for f in t:
     if not f.isfile() or f.size>remaining[0]:continue
     try:
      if pathlib.Path(f.name).suffix.lower() in TEXT_EXT and f.size<=8192:
       hd=peek_member(f.name,lambda n,fi=f:t.extractfile(fi).read(n),f.size)
      else:hd=''
      nid=add(f.name,f.size,hd)
      if nid and depth!=1 and ARCHIVE_RE.search(f.name) and f.size<=remaining[0]:
       bb=t.extractfile(f).read();remaining[0]-=len(bb)
       archive_scan(c,nid,bb,f.name,outer_path+'!/'+f.name,depth-1 if depth>0 else -1,remaining)
     except (OSError,ValueError,tarfile.TarError):continue
 except (zipfile.BadZipFile,tarfile.TarError,OSError,EOFError,MemoryError):
  edge(c,container_id,container_id+':INVALID_ARCHIVE','ARCHIVE_INDEX_FAILED','BLOCKED','container not indexed')

def add_fs_root(c,root,header_bytes=0,archive_depth=0,byte_budget=32*1024*1024,hash_content=False):
 root=os.path.abspath(root)
 if not os.path.exists(root):raise FileNotFoundError(root)
 if os.path.islink(root):raise ValueError('root symlink not traversed')
 count=0
 def add(path,parent=None):
  nonlocal count
  name=os.path.basename(path)
  islink=os.path.islink(path)
  isdir=os.path.isdir(path) if not islink else False
  ident='local:'+sha(os.fsencode(os.path.abspath(path)))
  size=None if (isdir or islink) else os.stat(path,follow_symlinks=False).st_size
  h=bsha(path) if (not isdir and not islink and hash_content) else ''
  header=peek_text(path,header_bytes) if (not isdir and not islink and header_bytes!=0) else ''
  put_node(c,dict(id=ident,source='LOCAL_FS',kind='symlink' if islink else ('directory' if isdir else 'file'),name=name,path=os.path.abspath(path),size=size,sha=h,header=header,meta={'bytes_verified':bool(h),'header_bytes_max':header_bytes,'filesystem':'local','symlinks_not_followed':True,'link_target_untrusted':os.readlink(path) if islink else None}))
  if parent:edge(c,parent,ident,'CONTAINS_PATH','VERIFIED_LOCAL','os.walk entry, no symlink traversal')
  count+=1
  if not isdir and not islink and archive_depth!=0 and ARCHIVE_RE.search(name) and size<=byte_budget:
   # Archive member names and nested routes are virtual, no unpacking on disk.
   with open(path,'rb') as f:bb=f.read()
   archive_scan(c,ident,bb,name,path,archive_depth,[byte_budget])
  return ident
 if os.path.isfile(root):add(root);return count
 add(root)
 for parent,dirs,files in os.walk(root,followlinks=False):
  links=[d for d in dirs if os.path.islink(os.path.join(parent,d))]
  dirs[:]=sorted(d for d in dirs if d not in links)
  files.sort();pid='local:'+sha(os.fsencode(os.path.abspath(parent)))
  for d in dirs:add(os.path.join(parent,d),pid)
  for d in links:add(os.path.join(parent,d),pid)
  for f in files:
   p=os.path.join(parent,f)
   if os.path.isfile(p) or os.path.islink(p):add(p,pid)
 return count

def derive_cross(c):
 # Exact-byte mirrors: both sha256 refer to complete, locally recomputed bytes
 rows=c.execute("SELECT id,sha,source FROM nodes WHERE length(sha)=64").fetchall()
 byh=collections.defaultdict(list)
 for i,h,src in rows:byh[h].append((i,src))
 for h,group in byh.items():
  for x in range(len(group)):
   for y in range(x+1,len(group)):
    a,b=group[x][0],group[y][0]
    edge(c,a,b,'SAME_CONTENT_SHA256','DIGEST_EQUALITY_SCOPED','local hash recomputed or prior V1 source-reported hash; does not establish source independence/authorship')
 # Candidate hash chunks in path, name, header, or embedded member relative path.
 # A 8+ hex fragment is an address hint, NOT proof and no chain promotion.
 digests=[(i,h) for i,h,src in rows]
 for i,meta in c.execute("SELECT id,meta FROM nodes WHERE kind IN ('directory','drive_folder','virtual_directory')"):
  try:h=json.loads(meta).get('derived_directory_snapshot_sha256')
  except (ValueError,TypeError):h=None
  if h:digests.append((i,h))
 for i,name,path,header in c.execute('SELECT id,name,path,header FROM nodes'):
  pieces=set()
  for s in [name,path,header[:2048]]:
   pieces.update(p.lower() for p in HEX_RE.findall(s))
  for p in pieces:
   for target,h in digests:
    if i!=target and p in h:
     rel='FULL_HASH_POINTER_CANDIDATE' if len(p)==64 else 'HASH_FRAGMENT_CANDIDATE'
     edge(c,i,target,rel,'CANDIDATE','substring length='+str(len(p))+'; independently verify scope/authority')
 # Header mentions exact provider IDs: the text occurrence is evidence of a reference only.
 providers=[(id,meta) for id,meta in c.execute("SELECT id,meta FROM nodes WHERE source='DRIVE_OBSERVED'")]
 tokenmap={}
 for nid,m in providers:
  try:pid=json.loads(m).get('provider_id')
  except Exception:pid=None
  if pid:tokenmap[pid]=nid
 for nid,hd in c.execute("SELECT id,header FROM nodes WHERE length(header)>0"):
  for pid,target in tokenmap.items():
   if nid!=target and pid in hd:
    edge(c,nid,target,'HEADER_REFERENCES_PROVIDER_ID','OBSERVED','literal provider id in bounded header; not endorsement')
 # Same filename in different location: a navigation candidate; no byte equivalence implied.
 basenames=collections.defaultdict(list)
 for i,n in c.execute('SELECT id,sort_name FROM nodes'):
  if len(n)>=8:basenames[n].append(i)
 for group in basenames.values():
  if 1<len(group)<=20:
   for j in range(len(group)):
    for k in range(j+1,len(group)):
     edge(c,group[j],group[k],'SAME_BASENAME_CANDIDATE','CANDIDATE','name equality without semantic or byte identity')

# Explicit nibble offset: H00=1234 / H04=abcd; path order is irrelevant.
PIECE_RE = re.compile(r'(?i)^H([0-9]{2})=([0-9a-f]{4,16})
 # This is a domain-separated manifest hash for an observation, not a native Drive directory hash.
 rows=c.execute("SELECT id,name,size,sha,kind FROM nodes").fetchall();D={r[0]:r for r in rows}
 children=collections.defaultdict(list)
 for a,b in c.execute("SELECT src,dst FROM edges WHERE rel IN ('CONTAINS_OBSERVED','CONTAINS_PATH','CONTAINS_VIRTUAL_MEMBER')"):
  if a in D and b in D:children[a].append(b)
 done={};visiting=set()
 def rec(n):
  if n in done:return done[n]
  if n in visiting:return 'CYCLE_STOP'
  visiting.add(n)
  vals=[]
  for ch in sorted(children[n]):
   ci,cn,sz,sh,kind=D[ch]
   vals.append([cn,ci,sz,sh or (rec(ch) if kind in ('directory','drive_folder','virtual_directory') else None)])
  z=sha(b'RAFAELIA/DIR-OBSERVATION/V2\0'+jsonbytes(vals))
  done[n]=z;visiting.remove(n);return z
 for i,name,sz,sh,kind in rows:
  if kind in ('directory','drive_folder','virtual_directory'):
   z=rec(i)
   md=json.loads(c.execute('SELECT meta FROM nodes WHERE id=?',(i,)).fetchone()[0]);md['derived_directory_snapshot_sha256']=z
   c.execute('UPDATE nodes SET meta=? WHERE id=?',(json.dumps(md,ensure_ascii=False,sort_keys=True),i))
 return len(done)

def index_grams(c):
 # FTS5 compact trigram index when compiled in SQLite; fallback uses postings.
 fts=bool(c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='search_fts'").fetchone())
 if fts:
  for rowid,nid,name,path,header,sh,meta in c.execute('SELECT rowid,id,name,path,header,sha,meta FROM nodes'):
   c.execute('INSERT INTO search_fts(rowid,name,path,header,sha,meta,ident) VALUES (?,?,?,?,?,?,?)',
             (rowid,canon(name),canon(path),canon(header),canon(sh),canon(meta),canon(nid)))
  return 'FTS5_TRIGRAM'
 for rowid,nid,name,path,header,sh,meta in c.execute('SELECT rowid,id,name,path,header,sha,meta FROM nodes'):
  for field,value in [('name',name),('path',path),('header',header),('sha',sh),('meta',meta),('id',nid)]:
   value=canon(value)
   if len(value)<3:continue
   words={value[i:i+3] for i in range(len(value)-2)}
   c.executemany('INSERT OR IGNORE INTO grams VALUES (?,?,?)',((field,w,nid) for w in words))
 return 'SQLITE_POSTINGS_FALLBACK'

def trigram_candidates(c,field,pattern):
 literals=re.split(r'[?*]',canon(pattern))
 seg=max(literals,key=len) if literals else ''
 if len(seg)<3:return None
 fts=bool(c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='search_fts'").fetchone())
 if fts:
  f={'id':'ident'}.get(field,field)
  if f not in {'name','path','header','sha','meta','ident'}:return None
  # FTS5 trigram quoted literal is indexed substring search. Python verifies exact wildcard semantics afterwards.
  quoted='"'+seg.replace('"','""')+'"'
  try:
   return {v[0] for v in c.execute(f'SELECT n.id FROM search_fts s JOIN nodes n ON n.rowid=s.rowid WHERE s.{f} MATCH ?',(quoted,))}
  except sqlite3.OperationalError:return None
 gs=sorted({seg[i:i+3] for i in range(len(seg)-2)})
 sizes=sorted([(c.execute('SELECT count(*) FROM grams WHERE field=? AND gram=?',(field,g)).fetchone()[0],g) for g in gs])
 if not sizes or sizes[0][0]==0:return set()
 ans=None
 for n,g in sizes:
  z={x[0] for x in c.execute('SELECT node_id FROM grams WHERE field=? AND gram=?',(field,g))}
  ans=z if ans is None else ans&z
  if not ans:break
 return ans

def build(args):
 if os.path.exists(args.out) and not args.replace:raise ValueError('output already exists; use a NEW filename or --replace (derived index only)')
 if os.path.exists(args.out):os.remove(args.out)
 c=init(args.out)
 counts=[]
 if args.graph:counts.append(('import_v1',import_v1(c,args.graph)))
 for root in args.root:counts.append((root,add_fs_root(c,root,args.header_bytes,args.archive_depth,args.archive_budget,args.hash_content)))
 dh=dir_hashes(c);derive_cross(c);derive_split_path_candidates(c);indexer=index_grams(c);c.commit()
 result={'database':args.out,'inputs':counts,'nodes':c.execute('SELECT count(*) FROM nodes').fetchone()[0],
 'edges':c.execute('SELECT count(*) FROM edges').fetchone()[0],
 'edge_statuses':dict(c.execute('SELECT status,count(*) FROM edges GROUP BY status').fetchall()),
 'relation_types':dict(c.execute('SELECT rel,count(*) FROM edges GROUP BY rel').fetchall()),
 'derived_directory_snapshot_hashes':dh,'index_backend':indexer,'indexed_records':c.execute('SELECT count(*) FROM nodes').fetchone()[0],'semantics':'NO_GLOBAL_PROOF; only listed bytes and observations; output is a derived local index',
 'write_scope':'only output DB; input roots unchanged'}
 c.close();print(json.dumps(result,ensure_ascii=False,indent=2));return result

# Boolean parser / evaluator. Conditions deliberately separate structure from proof.
def lex(s):
 x=shlex.shlex(s,posix=True,punctuation_chars='()');x.whitespace_split=True;x.commenters=''
 return list(x)
class QueryError(ValueError):pass
class Q:
 def __init__(self,tokens,c):self.t=tokens;self.at=0;self.c=c;self.universe=None
 def all(self):
  if self.universe is None:self.universe={r[0] for r in self.c.execute('SELECT id FROM nodes')}
  return self.universe
 def pop(self):
  if self.at>=len(self.t):raise QueryError('incomplete expression')
  x=self.t[self.at];self.at+=1;return x
 def parse(self):
  if not self.t:raise QueryError('empty query')
  s=self.or_()
  if self.at!=len(self.t):raise QueryError('unexpected token '+self.t[self.at])
  return s
 def or_(self):
  x=self.and_()
  while self.at<len(self.t) and self.t[self.at].upper()=='OR':
   self.pop();x|=self.and_()
  return x
 def and_(self):
  x=self.not_()
  while self.at<len(self.t) and self.t[self.at]!=')' and self.t[self.at].upper()!='OR':
   if self.t[self.at].upper()=='AND':self.pop()
   x &= self.not_()
  return x
 def not_(self):
  t=self.pop()
  if t.upper()=='NOT':return self.all()-self.not_()
  if t.upper() in ('HAS_CHILD','HAS_DESC','CHILD_OF','DESC_OF','SIBLING_OF','LINKED_TO') or t.upper().startswith('LINKED_TO:'):
   func=t.upper();relation=func.split(':',1)[1] if ':' in func else None
   if self.pop()!='(':raise QueryError('function '+func+' requires parentheses')
   subjects=self.or_()
   if self.pop()!=')':raise QueryError('unclosed function '+func)
   return self.rel_query(func.split(':')[0],subjects,relation)
  if t=='(':
   v=self.or_()
   if self.pop()!=')':raise QueryError('unclosed parenthesis')
   return v
  if t==')':raise QueryError('unexpected closing parenthesis')
  if t.upper() in ('AND','OR'):raise QueryError('missing left term')
  return self.term(t)
 def rel_query(self,func,subjects,relation=None):
  # Joins across DIFFERENT nodes: a directory may satisfy two independent child predicates.
  containment=('CONTAINS_OBSERVED','CONTAINS_PATH','CONTAINS_VIRTUAL_MEMBER','CONTAINS_ARCHIVE_MEMBER')
  upward=func in ('HAS_CHILD','HAS_DESC')
  if func=='LINKED_TO':
   out=set()
   for s in subjects:
    if relation:
     out|={x[0] for x in self.c.execute('SELECT src FROM edges WHERE dst=? AND rel=? UNION SELECT dst FROM edges WHERE src=? AND rel=?',(s,relation,s,relation))}
    else:
     out|={x[0] for x in self.c.execute('SELECT src FROM edges WHERE dst=? UNION SELECT dst FROM edges WHERE src=?',(s,s))}
   return out
  def neighbors(seed,reverse):
   output=set()
   for s in seed:
    sql=('SELECT src FROM edges WHERE dst=? AND rel IN (?,?,?,?)' if reverse else 'SELECT dst FROM edges WHERE src=? AND rel IN (?,?,?,?)')
    output|={x[0] for x in self.c.execute(sql,(s,*containment))}
   return output
  if func=='SIBLING_OF':
   parents=neighbors(subjects,True)
   return neighbors(parents,False)-subjects
  out=neighbors(subjects,upward)
  if func in ('HAS_CHILD','CHILD_OF'):return out
  seen=set(subjects)|out;frontier=out
  while frontier:
   batch=neighbors(frontier,upward)-seen
   seen|=batch;out|=batch;frontier=batch
  return out
 def term(self,term):
  m=re.match(r'^(size|depth)(>=|<=|>|<|=)(\d+)$',term,re.I)
  if m:
   fld,op,k=m.groups()
   if fld.lower()=='size':return {r[0] for r in self.c.execute('SELECT id FROM nodes WHERE size '+op+'?',(int(k),))}
   return {r[0] for r in self.c.execute('SELECT id,path FROM nodes') if op_eval(r[1].count('/'),op,int(k))}
  m=re.match(r'^([\w-]+)(:|=)(.*)$',term,re.U)
  if not m:field,pattern='any',term
  else:field,sep,pattern=m.groups();field=field.casefold()
  if not pattern:raise QueryError('empty matcher')
  if field=='word':
   pattern=canon(pattern)
   return {r[0] for r in self.c.execute('SELECT id FROM nodes WHERE sort_name LIKE ? OR sort_path LIKE ?',(f'%{pattern}%',f'%{pattern}%'))}
  if field in ('relation','rel'):
   return {r[0] for r in self.c.execute('SELECT src FROM edges WHERE rel=? UNION SELECT dst FROM edges WHERE rel=?',(pattern,pattern))}
  if field in ('status','edge_status'):
   return {r[0] for r in self.c.execute('SELECT src FROM edges WHERE status=? UNION SELECT dst FROM edges WHERE status=?',(pattern,pattern))}
  if field in ('hashfrag','hash_fragment'):
   frag=canon(pattern)
   if not re.fullmatch(r'[a-f0-9]{4,64}',frag):raise QueryError('hashfrag requires 4..64 hex characters')
   return {r[0] for r in self.c.execute('SELECT id FROM nodes WHERE lower(sha) LIKE ? OR lower(header) LIKE ? OR lower(path) LIKE ? OR lower(meta) LIKE ?',(f'%{frag}%',)*4)}
  col={'name':'sort_name','path':'sort_path','id':'id','kind':'kind','source':'source','sha':'sha','hash':'sha','header':'header','meta':'meta'}.get(field)
  if field not in ('any','name','path','id','kind','source','sha','hash','header','meta'):
   raise QueryError('unsupported field '+field)
  pat=canon(pattern)
  def hit(s):
   s=canon(s)
   if '*' in pat or '?' in pat:return fnmatch.fnmatchcase(s,pat)
   return pat in s
  if field=='any':
   # Indexed candidate union across metadata layers; exact predicate applied afterwards.
   fields=('name','path','header','meta','sha','id')
   cs=[trigram_candidates(self.c,f,pattern) for f in fields]
   universe=self.all() if any(x is None for x in cs) else set().union(*cs)
   result=set()
   for i in universe:
    row=self.c.execute('SELECT id,name,path,header,meta,sha FROM nodes WHERE id=?',(i,)).fetchone()
    if row and any(hit(v) for v in row):result.add(i)
   return result
  cs=trigram_candidates(self.c,field if field!='hash' else 'sha',pattern) if field in ('name','path','id','header','meta','sha','hash') else None
  if cs is None:return {i for i,val in self.c.execute(f'SELECT id,{col} FROM nodes') if hit(val or '')}
  result=set()
  for i in cs:
   row=self.c.execute(f'SELECT {col} FROM nodes WHERE id=?',(i,)).fetchone()
   if row and hit(row[0] or ''):result.add(i)
  return result

def op_eval(a,op,b):return {'>=':lambda:a>=b,'<=':lambda:a<=b,'>':lambda:a>b,'<':lambda:a<b,'=':lambda:a==b}[op]()

def expansion(c,seeds,depth,relations,direction):
 visited=set(seeds); frontier=set(seeds); discovered=[];d=0
 while frontier and (depth<0 or d<depth):
  batch=set()
  for cur in frontier:
   if direction in ('out','both'):
    for id,dst,rel,status in c.execute('SELECT id,dst,rel,status FROM edges WHERE src=?',(cur,)):
     if not relations or rel in relations:discovered.append((cur,dst,rel,status));batch.add(dst)
   if direction in ('in','both'):
    for id,src,rel,status in c.execute('SELECT id,src,rel,status FROM edges WHERE dst=?',(cur,)):
     if not relations or rel in relations:discovered.append((src,cur,rel,status));batch.add(src)
  frontier=batch-visited;visited|=frontier;d+=1
 return visited,discovered

def query(args):
 uri='file:'+os.path.abspath(args.db)+'?mode=ro'
 c=sqlite3.connect(uri,uri=True)
 seeds=Q(lex(args.expr),c).parse()
 target,visited_edges=expansion(c,seeds,args.expand,set(args.relation),args.direction)
 nodes=[]
 for id in target:
  row=c.execute('SELECT id,source,kind,name,path,size,sha,header,meta FROM nodes WHERE id=?',(id,)).fetchone()
  if not row:continue
  d=dict(zip(('id','source','kind','name','path','size','sha','header','meta'),row));d['meta']=json.loads(d['meta']);d['matched']=id in seeds
  if not args.show_header:d.pop('header')
  nodes.append(d)
 nodes.sort(key=lambda x:(not x['matched'],x['path'],x['id']))
 result={'expression':args.expr,'matched':len(seeds),'expanded_total':len(target),'navigated_edges':len(set(visited_edges)),
  'nodes':nodes,'edges':[dict(src=a,dst=b,relation=r,status=s) for a,b,r,s in sorted(set(visited_edges))],
  'caveat':'candidate != byte identity; snapshot directory hash != native Drive root; read-only index'}
 if args.output:
  if os.path.exists(args.output):raise ValueError('output already exists; choose new path')
  with open(args.output,'x',encoding='utf-8') as f:json.dump(result,f,ensure_ascii=False,indent=2)
 if args.format=='json':print(json.dumps(result,ensure_ascii=False,indent=2))
 else:
  print(f'MATCHED={len(seeds)} EXPANDED={len(target)} EDGES={len(set(visited_edges))}')
  for n in nodes:
   print(('★' if n['matched'] else '↳')+' '+n['path']+' ['+n['kind']+'] size='+str(n['size'])+' sha='+(n['sha'][:12] if n['sha'] else 'TOKEN_VAZIO'))
  if args.verbose:
   for a,b,r,s in sorted(set(visited_edges)):print(f'  {a} -{r}/{s}-> {b}')
 c.close();return result

def main():
 p=argparse.ArgumentParser(description='RAFAELIA Ω: tree / AND OR NOT / provenance graph index')
 sub=p.add_subparsers(dest='cmd',required=True)
 b=sub.add_parser('build');b.add_argument('--graph');b.add_argument('--root',action='append',default=[]);b.add_argument('--out',required=True);b.add_argument('--replace',action='store_true');b.add_argument('--header-bytes',type=int,default=0,help='0: no file reads for headers; -1: read full text (explicit)');b.add_argument('--hash-content',action='store_true',help='opt-in streaming bytes for SHA256');b.add_argument('--archive-depth',type=int,default=0,help='0: metadata only, -1: traverse nested containers within budget');b.add_argument('--archive-budget',type=int,default=32*1024*1024)
 q=sub.add_parser('query');q.add_argument('--db',required=True);q.add_argument('--expr',required=True);q.add_argument('--expand',type=int,default=0,help='-1 traverses all reachable links; use with care');q.add_argument('--direction',choices=('out','in','both'),default='both');q.add_argument('--relation',action='append',default=[]);q.add_argument('--format',choices=('tree','json'),default='tree');q.add_argument('--output');q.add_argument('--show-header',action='store_true');q.add_argument('--verbose',action='store_true')
 a=p.parse_args()
 try:
  if a.cmd=='build':build(a)
  else:query(a)
 except (ValueError,QueryError,sqlite3.Error,OSError) as e:print('STOPWALK: '+str(e),file=sys.stderr);return 2
 return 0
if __name__=='__main__':sys.exit(main()))

def derive_split_path_candidates(c):
    """Discover two-or-more path-segment SHA fragments; never attest identity.

    Fragment offset is a 0-based hexadecimal-character index in a 64-nibble
    digest. A match is a navigation hint, not evidence of byte equality.
    """
    targets = [(nid, digest.lower()) for nid, digest in c.execute(
        "SELECT id,sha FROM nodes WHERE length(sha)=64")]
    for nid, meta in c.execute(
            "SELECT id,meta FROM nodes WHERE kind IN ('directory','drive_folder','virtual_directory')"):
        try:
            digest = json.loads(meta).get('derived_directory_snapshot_sha256')
        except (ValueError, TypeError):
            digest = None
        if isinstance(digest, str) and re.fullmatch(r'[a-f0-9]{64}', digest.lower()):
            targets.append((nid, digest.lower()))
    for source, path in c.execute("SELECT id,path FROM nodes"):
        parts = []
        for segment in re.split(r'[!/\\]+', path):
            m = PIECE_RE.fullmatch(segment)
            if m:
                off = int(m.group(1))
                val = m.group(2).lower()
                parts.append((off, val))
        if len(parts) < 2:
            continue
        expected = {}
        contradictory = False
        for off, val in parts:
            if off + len(val) > 64:
                contradictory = True
                break
            for idx, nibble in enumerate(val):
                position = off + idx
                if position in expected and expected[position] != nibble:
                    contradictory = True
                    break
                expected[position] = nibble
            if contradictory:
                break
        if contradictory or len(expected) < 8:
            continue
        for target, digest in targets:
            if source != target and all(digest[pos] == nibble
                                       for pos, nibble in expected.items()):
                edge(c, source, target, 'HASH_PIECES_PATH_CANDIDATE', 'CANDIDATE',
                     'ordered by explicit nibble offsets; size=' + str(len(expected)) +
                     '; not a byte-integrity proof')

def dir_hashes(c):
 # This is a domain-separated manifest hash for an observation, not a native Drive directory hash.
 rows=c.execute("SELECT id,name,size,sha,kind FROM nodes").fetchall();D={r[0]:r for r in rows}
 children=collections.defaultdict(list)
 for a,b in c.execute("SELECT src,dst FROM edges WHERE rel IN ('CONTAINS_OBSERVED','CONTAINS_PATH','CONTAINS_VIRTUAL_MEMBER')"):
  if a in D and b in D:children[a].append(b)
 done={};visiting=set()
 def rec(n):
  if n in done:return done[n]
  if n in visiting:return 'CYCLE_STOP'
  visiting.add(n)
  vals=[]
  for ch in sorted(children[n]):
   ci,cn,sz,sh,kind=D[ch]
   vals.append([cn,ci,sz,sh or (rec(ch) if kind in ('directory','drive_folder','virtual_directory') else None)])
  z=sha(b'RAFAELIA/DIR-OBSERVATION/V2\0'+jsonbytes(vals))
  done[n]=z;visiting.remove(n);return z
 for i,name,sz,sh,kind in rows:
  if kind in ('directory','drive_folder','virtual_directory'):
   z=rec(i)
   md=json.loads(c.execute('SELECT meta FROM nodes WHERE id=?',(i,)).fetchone()[0]);md['derived_directory_snapshot_sha256']=z
   c.execute('UPDATE nodes SET meta=? WHERE id=?',(json.dumps(md,ensure_ascii=False,sort_keys=True),i))
 return len(done)

def index_grams(c):
 # FTS5 compact trigram index when compiled in SQLite; fallback uses postings.
 fts=bool(c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='search_fts'").fetchone())
 if fts:
  for rowid,nid,name,path,header,sh,meta in c.execute('SELECT rowid,id,name,path,header,sha,meta FROM nodes'):
   c.execute('INSERT INTO search_fts(rowid,name,path,header,sha,meta,ident) VALUES (?,?,?,?,?,?,?)',
             (rowid,canon(name),canon(path),canon(header),canon(sh),canon(meta),canon(nid)))
  return 'FTS5_TRIGRAM'
 for rowid,nid,name,path,header,sh,meta in c.execute('SELECT rowid,id,name,path,header,sha,meta FROM nodes'):
  for field,value in [('name',name),('path',path),('header',header),('sha',sh),('meta',meta),('id',nid)]:
   value=canon(value)
   if len(value)<3:continue
   words={value[i:i+3] for i in range(len(value)-2)}
   c.executemany('INSERT OR IGNORE INTO grams VALUES (?,?,?)',((field,w,nid) for w in words))
 return 'SQLITE_POSTINGS_FALLBACK'

def trigram_candidates(c,field,pattern):
 literals=re.split(r'[?*]',canon(pattern))
 seg=max(literals,key=len) if literals else ''
 if len(seg)<3:return None
 fts=bool(c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='search_fts'").fetchone())
 if fts:
  f={'id':'ident'}.get(field,field)
  if f not in {'name','path','header','sha','meta','ident'}:return None
  # FTS5 trigram quoted literal is indexed substring search. Python verifies exact wildcard semantics afterwards.
  quoted='"'+seg.replace('"','""')+'"'
  try:
   return {v[0] for v in c.execute(f'SELECT n.id FROM search_fts s JOIN nodes n ON n.rowid=s.rowid WHERE s.{f} MATCH ?',(quoted,))}
  except sqlite3.OperationalError:return None
 gs=sorted({seg[i:i+3] for i in range(len(seg)-2)})
 sizes=sorted([(c.execute('SELECT count(*) FROM grams WHERE field=? AND gram=?',(field,g)).fetchone()[0],g) for g in gs])
 if not sizes or sizes[0][0]==0:return set()
 ans=None
 for n,g in sizes:
  z={x[0] for x in c.execute('SELECT node_id FROM grams WHERE field=? AND gram=?',(field,g))}
  ans=z if ans is None else ans&z
  if not ans:break
 return ans

def build(args):
 if os.path.exists(args.out) and not args.replace:raise ValueError('output already exists; use a NEW filename or --replace (derived index only)')
 if os.path.exists(args.out):os.remove(args.out)
 c=init(args.out)
 counts=[]
 if args.graph:counts.append(('import_v1',import_v1(c,args.graph)))
 for root in args.root:counts.append((root,add_fs_root(c,root,args.header_bytes,args.archive_depth,args.archive_budget,args.hash_content)))
 dh=dir_hashes(c);derive_cross(c);indexer=index_grams(c);c.commit()
 result={'database':args.out,'inputs':counts,'nodes':c.execute('SELECT count(*) FROM nodes').fetchone()[0],
 'edges':c.execute('SELECT count(*) FROM edges').fetchone()[0],
 'edge_statuses':dict(c.execute('SELECT status,count(*) FROM edges GROUP BY status').fetchall()),
 'relation_types':dict(c.execute('SELECT rel,count(*) FROM edges GROUP BY rel').fetchall()),
 'derived_directory_snapshot_hashes':dh,'index_backend':indexer,'indexed_records':c.execute('SELECT count(*) FROM nodes').fetchone()[0],'semantics':'NO_GLOBAL_PROOF; only listed bytes and observations; output is a derived local index',
 'write_scope':'only output DB; input roots unchanged'}
 c.close();print(json.dumps(result,ensure_ascii=False,indent=2));return result

# Boolean parser / evaluator. Conditions deliberately separate structure from proof.
def lex(s):
 x=shlex.shlex(s,posix=True,punctuation_chars='()');x.whitespace_split=True;x.commenters=''
 return list(x)
class QueryError(ValueError):pass
class Q:
 def __init__(self,tokens,c):self.t=tokens;self.at=0;self.c=c;self.universe=None
 def all(self):
  if self.universe is None:self.universe={r[0] for r in self.c.execute('SELECT id FROM nodes')}
  return self.universe
 def pop(self):
  if self.at>=len(self.t):raise QueryError('incomplete expression')
  x=self.t[self.at];self.at+=1;return x
 def parse(self):
  if not self.t:raise QueryError('empty query')
  s=self.or_()
  if self.at!=len(self.t):raise QueryError('unexpected token '+self.t[self.at])
  return s
 def or_(self):
  x=self.and_()
  while self.at<len(self.t) and self.t[self.at].upper()=='OR':
   self.pop();x|=self.and_()
  return x
 def and_(self):
  x=self.not_()
  while self.at<len(self.t) and self.t[self.at]!=')' and self.t[self.at].upper()!='OR':
   if self.t[self.at].upper()=='AND':self.pop()
   x &= self.not_()
  return x
 def not_(self):
  t=self.pop()
  if t.upper()=='NOT':return self.all()-self.not_()
  if t.upper() in ('HAS_CHILD','HAS_DESC','CHILD_OF','DESC_OF','SIBLING_OF','LINKED_TO') or t.upper().startswith('LINKED_TO:'):
   func=t.upper();relation=func.split(':',1)[1] if ':' in func else None
   if self.pop()!='(':raise QueryError('function '+func+' requires parentheses')
   subjects=self.or_()
   if self.pop()!=')':raise QueryError('unclosed function '+func)
   return self.rel_query(func.split(':')[0],subjects,relation)
  if t=='(':
   v=self.or_()
   if self.pop()!=')':raise QueryError('unclosed parenthesis')
   return v
  if t==')':raise QueryError('unexpected closing parenthesis')
  if t.upper() in ('AND','OR'):raise QueryError('missing left term')
  return self.term(t)
 def rel_query(self,func,subjects,relation=None):
  # Joins across DIFFERENT nodes: a directory may satisfy two independent child predicates.
  containment=('CONTAINS_OBSERVED','CONTAINS_PATH','CONTAINS_VIRTUAL_MEMBER','CONTAINS_ARCHIVE_MEMBER')
  upward=func in ('HAS_CHILD','HAS_DESC')
  if func=='LINKED_TO':
   out=set()
   for s in subjects:
    if relation:
     out|={x[0] for x in self.c.execute('SELECT src FROM edges WHERE dst=? AND rel=? UNION SELECT dst FROM edges WHERE src=? AND rel=?',(s,relation,s,relation))}
    else:
     out|={x[0] for x in self.c.execute('SELECT src FROM edges WHERE dst=? UNION SELECT dst FROM edges WHERE src=?',(s,s))}
   return out
  def neighbors(seed,reverse):
   output=set()
   for s in seed:
    sql=('SELECT src FROM edges WHERE dst=? AND rel IN (?,?,?,?)' if reverse else 'SELECT dst FROM edges WHERE src=? AND rel IN (?,?,?,?)')
    output|={x[0] for x in self.c.execute(sql,(s,*containment))}
   return output
  if func=='SIBLING_OF':
   parents=neighbors(subjects,True)
   return neighbors(parents,False)-subjects
  out=neighbors(subjects,upward)
  if func in ('HAS_CHILD','CHILD_OF'):return out
  seen=set(subjects)|out;frontier=out
  while frontier:
   batch=neighbors(frontier,upward)-seen
   seen|=batch;out|=batch;frontier=batch
  return out
 def term(self,term):
  m=re.match(r'^(size|depth)(>=|<=|>|<|=)(\d+)$',term,re.I)
  if m:
   fld,op,k=m.groups()
   if fld.lower()=='size':return {r[0] for r in self.c.execute('SELECT id FROM nodes WHERE size '+op+'?',(int(k),))}
   return {r[0] for r in self.c.execute('SELECT id,path FROM nodes') if op_eval(r[1].count('/'),op,int(k))}
  m=re.match(r'^([\w-]+)(:|=)(.*)$',term,re.U)
  if not m:field,pattern='any',term
  else:field,sep,pattern=m.groups();field=field.casefold()
  if not pattern:raise QueryError('empty matcher')
  if field=='word':
   pattern=canon(pattern)
   return {r[0] for r in self.c.execute('SELECT id FROM nodes WHERE sort_name LIKE ? OR sort_path LIKE ?',(f'%{pattern}%',f'%{pattern}%'))}
  if field in ('relation','rel'):
   return {r[0] for r in self.c.execute('SELECT src FROM edges WHERE rel=? UNION SELECT dst FROM edges WHERE rel=?',(pattern,pattern))}
  if field in ('status','edge_status'):
   return {r[0] for r in self.c.execute('SELECT src FROM edges WHERE status=? UNION SELECT dst FROM edges WHERE status=?',(pattern,pattern))}
  if field in ('hashfrag','hash_fragment'):
   frag=canon(pattern)
   if not re.fullmatch(r'[a-f0-9]{4,64}',frag):raise QueryError('hashfrag requires 4..64 hex characters')
   return {r[0] for r in self.c.execute('SELECT id FROM nodes WHERE lower(sha) LIKE ? OR lower(header) LIKE ? OR lower(path) LIKE ? OR lower(meta) LIKE ?',(f'%{frag}%',)*4)}
  col={'name':'sort_name','path':'sort_path','id':'id','kind':'kind','source':'source','sha':'sha','hash':'sha','header':'header','meta':'meta'}.get(field)
  if field not in ('any','name','path','id','kind','source','sha','hash','header','meta'):
   raise QueryError('unsupported field '+field)
  pat=canon(pattern)
  def hit(s):
   s=canon(s)
   if '*' in pat or '?' in pat:return fnmatch.fnmatchcase(s,pat)
   return pat in s
  if field=='any':
   # Indexed candidate union across metadata layers; exact predicate applied afterwards.
   fields=('name','path','header','meta','sha','id')
   cs=[trigram_candidates(self.c,f,pattern) for f in fields]
   universe=self.all() if any(x is None for x in cs) else set().union(*cs)
   result=set()
   for i in universe:
    row=self.c.execute('SELECT id,name,path,header,meta,sha FROM nodes WHERE id=?',(i,)).fetchone()
    if row and any(hit(v) for v in row):result.add(i)
   return result
  cs=trigram_candidates(self.c,field if field!='hash' else 'sha',pattern) if field in ('name','path','id','header','meta','sha','hash') else None
  if cs is None:return {i for i,val in self.c.execute(f'SELECT id,{col} FROM nodes') if hit(val or '')}
  result=set()
  for i in cs:
   row=self.c.execute(f'SELECT {col} FROM nodes WHERE id=?',(i,)).fetchone()
   if row and hit(row[0] or ''):result.add(i)
  return result

def op_eval(a,op,b):return {'>=':lambda:a>=b,'<=':lambda:a<=b,'>':lambda:a>b,'<':lambda:a<b,'=':lambda:a==b}[op]()

def expansion(c,seeds,depth,relations,direction):
 visited=set(seeds); frontier=set(seeds); discovered=[];d=0
 while frontier and (depth<0 or d<depth):
  batch=set()
  for cur in frontier:
   if direction in ('out','both'):
    for id,dst,rel,status in c.execute('SELECT id,dst,rel,status FROM edges WHERE src=?',(cur,)):
     if not relations or rel in relations:discovered.append((cur,dst,rel,status));batch.add(dst)
   if direction in ('in','both'):
    for id,src,rel,status in c.execute('SELECT id,src,rel,status FROM edges WHERE dst=?',(cur,)):
     if not relations or rel in relations:discovered.append((src,cur,rel,status));batch.add(src)
  frontier=batch-visited;visited|=frontier;d+=1
 return visited,discovered

def query(args):
 uri='file:'+os.path.abspath(args.db)+'?mode=ro'
 c=sqlite3.connect(uri,uri=True)
 seeds=Q(lex(args.expr),c).parse()
 target,visited_edges=expansion(c,seeds,args.expand,set(args.relation),args.direction)
 nodes=[]
 for id in target:
  row=c.execute('SELECT id,source,kind,name,path,size,sha,header,meta FROM nodes WHERE id=?',(id,)).fetchone()
  if not row:continue
  d=dict(zip(('id','source','kind','name','path','size','sha','header','meta'),row));d['meta']=json.loads(d['meta']);d['matched']=id in seeds
  if not args.show_header:d.pop('header')
  nodes.append(d)
 nodes.sort(key=lambda x:(not x['matched'],x['path'],x['id']))
 result={'expression':args.expr,'matched':len(seeds),'expanded_total':len(target),'navigated_edges':len(set(visited_edges)),
  'nodes':nodes,'edges':[dict(src=a,dst=b,relation=r,status=s) for a,b,r,s in sorted(set(visited_edges))],
  'caveat':'candidate != byte identity; snapshot directory hash != native Drive root; read-only index'}
 if args.output:
  if os.path.exists(args.output):raise ValueError('output already exists; choose new path')
  with open(args.output,'x',encoding='utf-8') as f:json.dump(result,f,ensure_ascii=False,indent=2)
 if args.format=='json':print(json.dumps(result,ensure_ascii=False,indent=2))
 else:
  print(f'MATCHED={len(seeds)} EXPANDED={len(target)} EDGES={len(set(visited_edges))}')
  for n in nodes:
   print(('★' if n['matched'] else '↳')+' '+n['path']+' ['+n['kind']+'] size='+str(n['size'])+' sha='+(n['sha'][:12] if n['sha'] else 'TOKEN_VAZIO'))
  if args.verbose:
   for a,b,r,s in sorted(set(visited_edges)):print(f'  {a} -{r}/{s}-> {b}')
 c.close();return result

def main():
 p=argparse.ArgumentParser(description='RAFAELIA Ω: tree / AND OR NOT / provenance graph index')
 sub=p.add_subparsers(dest='cmd',required=True)
 b=sub.add_parser('build');b.add_argument('--graph');b.add_argument('--root',action='append',default=[]);b.add_argument('--out',required=True);b.add_argument('--replace',action='store_true');b.add_argument('--header-bytes',type=int,default=0,help='0: no file reads for headers; -1: read full text (explicit)');b.add_argument('--hash-content',action='store_true',help='opt-in streaming bytes for SHA256');b.add_argument('--archive-depth',type=int,default=0,help='0: metadata only, -1: traverse nested containers within budget');b.add_argument('--archive-budget',type=int,default=32*1024*1024)
 q=sub.add_parser('query');q.add_argument('--db',required=True);q.add_argument('--expr',required=True);q.add_argument('--expand',type=int,default=0,help='-1 traverses all reachable links; use with care');q.add_argument('--direction',choices=('out','in','both'),default='both');q.add_argument('--relation',action='append',default=[]);q.add_argument('--format',choices=('tree','json'),default='tree');q.add_argument('--output');q.add_argument('--show-header',action='store_true');q.add_argument('--verbose',action='store_true')
 a=p.parse_args()
 try:
  if a.cmd=='build':build(a)
  else:query(a)
 except (ValueError,QueryError,sqlite3.Error,OSError) as e:print('STOPWALK: '+str(e),file=sys.stderr);return 2
 return 0
if __name__=='__main__':sys.exit(main())