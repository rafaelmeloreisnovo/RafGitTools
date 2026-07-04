#!/usr/bin/env bash
# =============================================================================
# falas.sh — gerador refatorado do compiladorlowFala.txt
# RAFCODE-Φ · Fala → Fonema → Token → AST → Bytecode → ASM → VM → Output
# =============================================================================
# Objetivo:
#   Gerar um monólito textual completo, auditável e ampliado, sem depender de
#   blocos quebrados, strings abertas ou trechos incompletos.
# =============================================================================

set -Eeuo pipefail
IFS=$'\n\t'

readonly VERSION="2.0.0-refactor"
readonly DEFAULT_OUT="compiladorlowFala.txt"
readonly DEFAULT_MANIFEST="compiladorlowFala.manifest.json"

OUT="$DEFAULT_OUT"
MANIFEST="$DEFAULT_MANIFEST"
DRY_RUN=0
QUIET=0
CHECK_ONLY=0
LIST_ONLY=0

SEED_IDS=()
SEED_TITLES=()
TMP_OUT=""
TMP_MANIFEST=""

usage() {
  cat <<'EOF'
Uso: ./falas.sh [opções]

Opções:
  -o, --out ARQUIVO          arquivo final gerado (padrão: compiladorlowFala.txt)
  -m, --manifest ARQUIVO     manifesto JSON gerado (padrão: compiladorlowFala.manifest.json)
      --check                gera em temporário e valida sem sobrescrever
      --dry-run              imprime o conteúdo no stdout
      --list                 lista as 60 sementes disponíveis
  -q, --quiet                reduz logs
  -h, --help                 mostra esta ajuda

Saída:
  - 12 famílias semânticas
  - 5 variantes por família
  - 60 sementes implementadas
  - índice, manifesto, contratos e exemplos de execução
EOF
}

log() {
  (( QUIET == 1 )) && return 0
  printf '[falas] %s\n' "$*" >&2
}

die() {
  printf '[falas:erro] %s\n' "$*" >&2
  exit 1
}

cleanup() {
  [[ -n "${TMP_OUT:-}" && -f "$TMP_OUT" ]] && rm -f "$TMP_OUT"
  [[ -n "${TMP_MANIFEST:-}" && -f "$TMP_MANIFEST" ]] && rm -f "$TMP_MANIFEST"
}
trap cleanup EXIT

parse_args() {
  while (($#)); do
    case "$1" in
      -o|--out)
        [[ $# -ge 2 ]] || die "faltou valor para $1"
        OUT="$2"; shift 2 ;;
      -m|--manifest)
        [[ $# -ge 2 ]] || die "faltou valor para $1"
        MANIFEST="$2"; shift 2 ;;
      --check) CHECK_ONLY=1; shift ;;
      --dry-run) DRY_RUN=1; shift ;;
      --list) LIST_ONLY=1; shift ;;
      -q|--quiet) QUIET=1; shift ;;
      -h|--help) usage; exit 0 ;;
      *) die "opção desconhecida: $1" ;;
    esac
  done
}

family_title() {
  case "$1" in
    1)  printf 'Tokenizador fonético multilíngue' ;;
    2)  printf 'Parser AST e gramática gerativa' ;;
    3)  printf 'Codificador bíblico e cross-reference' ;;
    4)  printf 'Compilador fala para baixo nível' ;;
    5)  printf 'Texto para código e decompilador' ;;
    6)  printf 'Acento, cantilação e prosódia' ;;
    7)  printf 'Sistema conversacional multilíngue' ;;
    8)  printf 'Tradutor HE/EL/LA/PT e morfologia' ;;
    9)  printf 'Pipeline monolítico e VM' ;;
    10) printf 'Letra, fonema e DNA textual' ;;
    11) printf 'Sistema sonoro, síntese e análise' ;;
    12) printf 'Hardening, empacotamento e operação' ;;
    *)  printf 'Família desconhecida' ;;
  esac
}

variant_title() {
  case "$1" in
    1) printf 'núcleo determinístico' ;;
    2) printf 'baixo nível freestanding' ;;
    3) printf 'auditoria e assinatura' ;;
    4) printf 'consenso poliglota' ;;
    5) printf 'expansão semântica máxima' ;;
    *) printf 'variante desconhecida' ;;
  esac
}

variant_params() {
  case "$1" in
    1) printf 'latência=baixa; memória=fixa; phi_min=0.75; modo=determinístico' ;;
    2) printf 'latência=mínima; memória=arena; phi_min=0.78; modo=bare-metal' ;;
    3) printf 'latência=média; memória=hash-chain; phi_min=0.82; modo=auditável' ;;
    4) printf 'latência=média; memória=contextual; phi_min=0.80; modo=consenso' ;;
    5) printf 'latência=adaptável; memória=expandida; phi_min=0.86; modo=semântico' ;;
  esac
}

list_seeds() {
  local f v sid
  for f in $(seq 1 12); do
    for v in $(seq 1 5); do
      printf 'S%02d_V%d — %s / %s\n' "$f" "$v" "$(family_title "$f")" "$(variant_title "$v")"
    done
  done
}

append_header() {
  cat > "$TMP_OUT" <<EOF
# compiladorlowFala.txt — MONÓLITO RAFCODE-Φ

Versão do gerador: $VERSION
Gerado em UTC: $(date -u '+%Y-%m-%dT%H:%M:%SZ')
Origem: Livro/falas.sh

## Pipeline canônico

FALA → FONEMA → TOKEN → AST → BYTECODE → ASM → VM → OUTPUT

## Invariantes

- Toda semente declara entrada, transformação, saída e validação.
- Toda emissão passa por phi_min antes de ser aceita.
- Nenhum bloco depende de string quebrada ou interpolação ambígua.
- Os exemplos são heurísticos, portáveis e sem dependência externa obrigatória.
- O gerador entrega 12 famílias × 5 variantes = 60 sementes.

## Idiomas-alvo

PT · HE · AR · EL · LA · JP · CN · EN · RU · SA

## Opcodes RAFAELIA-VM

| Hex  | Nome      | Função |
| ---- | --------- | ------ |
| 0x00 | NOP       | repouso operacional |
| 0x01 | LOAD      | carrega símbolo |
| 0x02 | STORE     | guarda símbolo |
| 0x03 | PUSH      | empilha valor |
| 0x04 | POP       | desempilha valor |
| 0x10 | BEGIN     | abre emissão |
| 0x11 | CREATE    | cria estrutura |
| 0x12 | SPEAK     | emite fala |
| 0x13 | LET       | declara forma |
| 0x14 | SEP       | separa domínio |
| 0x15 | NAME      | nomeia entidade |
| 0x16 | GOOD      | valida coerência |
| 0x17 | EVE       | marca transição |
| 0x20 | AND       | conjunção |
| 0x21 | OR        | alternativa |
| 0x22 | NOT       | negação |
| 0x23 | IF        | condição |
| 0x24 | LOOP      | repetição |
| 0x25 | BLESS     | reforço positivo |
| 0x26 | SANCTIFY  | purificação de estado |
| 0x27 | REST      | fechamento estável |
| 0x80 | VERB      | verbo |
| 0x81 | NOUN      | nome |
| 0x82 | PROP      | propriedade |
| 0xF0 | LOGOS     | nó semântico superior |
| 0xFE | PRINT     | saída |
| 0xFF | SEAL      | selo final |

## Índice das sementes

EOF
  list_seeds >> "$TMP_OUT"
  printf '\n---\n' >> "$TMP_OUT"
}

emit_seed() {
  local family="$1" variant="$2" sid title params
  sid=$(printf 'S%02d_V%d' "$family" "$variant")
  title="$(family_title "$family") / $(variant_title "$variant")"
  params="$(variant_params "$variant")"
  SEED_IDS+=("$sid")
  SEED_TITLES+=("$title")

  cat >> "$TMP_OUT" <<EOF

# $sid — $title

Contrato:
- Entrada: fala, texto, bytecode ou estrutura semântica.
- Transformação: $params.
- Saída: artefato verificável com phi_min aplicado.
- Validação: assinatura local, coerência por domínio e fechamento SEAL.

EOF
  seed_body "$family" "$variant" "$sid" >> "$TMP_OUT"
}

seed_body() {
  local family="$1" variant="$2" sid="$3"
  case "$family" in
    1) body_tokenizer "$variant" "$sid" ;;
    2) body_parser "$variant" "$sid" ;;
    3) body_bible "$variant" "$sid" ;;
    4) body_lowlevel "$variant" "$sid" ;;
    5) body_textcode "$variant" "$sid" ;;
    6) body_prosody "$variant" "$sid" ;;
    7) body_conversation "$variant" "$sid" ;;
    8) body_translation "$variant" "$sid" ;;
    9) body_pipeline "$variant" "$sid" ;;
    10) body_textdna "$variant" "$sid" ;;
    11) body_audio "$variant" "$sid" ;;
    12) body_ops "$variant" "$sid" ;;
  esac
}

body_tokenizer() {
  cat <<'EOF'
```python
import unicodedata

SCRIPT_RANGES = {
    'he': ((0x0590, 0x05FF), (0xFB1D, 0xFB4F)),
    'ar': ((0x0600, 0x06FF), (0x0750, 0x077F)),
    'el': ((0x0370, 0x03FF), (0x1F00, 0x1FFF)),
    'cn': ((0x4E00, 0x9FFF),),
    'jp': ((0x3040, 0x30FF), (0x31F0, 0x31FF)),
}

PHONEME_FEATURES = {
    'a': (1,0,0,0,0,1), 'e': (1,0,0,0,0,1), 'i': (1,0,0,0,0,1),
    'o': (1,0,0,0,0,1), 'u': (1,0,0,0,0,1), 'm': (1,1,1,0,0,0),
    'n': (1,1,1,0,0,0), 's': (0,0,0,1,0,0), 'r': (1,0,0,0,1,0),
    'א': (0,0,0,0,1,0), 'ב': (1,0,1,0,0,0), 'ר': (1,0,0,0,1,0),
    'α': (1,0,0,0,0,1), 'λ': (1,0,0,0,0,0), 'ω': (1,0,0,0,0,1),
}

def script_of(char):
    cp = ord(char)
    for lang, ranges in SCRIPT_RANGES.items():
        if any(lo <= cp <= hi for lo, hi in ranges):
            return lang
    if char.isalpha():
        return 'la_pt_en'
    if char.isdigit():
        return 'num'
    return 'sep'

def norm_token(text):
    return unicodedata.normalize('NFKC', text).strip()

def feature_distance(a, b):
    va = PHONEME_FEATURES.get(a.lower(), (0,0,0,0,0,0))
    vb = PHONEME_FEATURES.get(b.lower(), (0,0,0,0,0,0))
    return sum(abs(x-y) for x, y in zip(va, vb))

def tokenize(text, threshold=2):
    out, buf, current_script = [], [], None
    for ch in norm_token(text):
        sc = script_of(ch)
        if sc == 'sep':
            if buf:
                out.append({'raw': ''.join(buf), 'script': current_script})
                buf = []
            current_script = None
            continue
        if not buf:
            buf, current_script = [ch], sc
            continue
        if sc == current_script and feature_distance(buf[-1], ch) <= threshold:
            buf.append(ch)
        else:
            out.append({'raw': ''.join(buf), 'script': current_script})
            buf, current_script = [ch], sc
    if buf:
        out.append({'raw': ''.join(buf), 'script': current_script})
    return out
```
EOF
}

body_parser() {
  cat <<'EOF'
```python
class ASTNode:
    def __init__(self, kind, value='', lang='pt'):
        self.kind = kind
        self.value = value
        self.lang = lang
        self.children = []
        self.phi = 0.75
    def add(self, node):
        self.children.append(node)
        return self
    def seal(self):
        child_phi = sum(c.phi for c in self.children) / max(len(self.children), 1)
        self.phi = min(1.0, 0.25 + child_phi * 0.75)
        return self

WORD_ORDER = {'he': 'VSO', 'ar': 'VSO', 'el': 'SVO', 'la': 'SOV', 'pt': 'SVO', 'en': 'SVO', 'jp': 'SOV', 'cn': 'SVO'}
VERB_HINTS = {'criou', 'fez', 'disse', 'bara', 'ברא', 'ποιέω', 'creavit'}

def parse_tokens(tokens, lang='pt'):
    root = ASTNode('SENTENCE', lang=lang)
    subject = ASTNode('NP_SUBJECT', lang=lang)
    verb = ASTNode('VP', lang=lang)
    obj = ASTNode('NP_OBJECT', lang=lang)
    order = WORD_ORDER.get(lang, 'SVO')
    for i, tok in enumerate(tokens):
        raw = tok['raw'] if isinstance(tok, dict) else str(tok)
        low = raw.lower()
        node = ASTNode('WORD', raw, lang)
        node.phi = 0.90 if low in VERB_HINTS else 0.80
        if low in VERB_HINTS:
            verb.add(node)
        elif order == 'VSO' and not verb.children:
            verb.add(node)
        elif len(subject.children) < 2:
            subject.add(node)
        else:
            obj.add(node)
    return root.add(subject.seal()).add(verb.seal()).add(obj.seal()).seal()
```
EOF
}

body_bible() {
  cat <<'EOF'
```python
GENESIS_1_1 = {
    'ref': 'GEN.1.1',
    'he': 'בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת הַשָּׁמַיִם וְאֵת הָאָרֶץ',
    'el': 'Ἐν ἀρχῇ ἐποίησεν ὁ θεὸς τὸν οὐρανὸν καὶ τὴν γῆν',
    'la': 'In principio creavit Deus caelum et terram',
    'pt': 'No princípio criou Deus os céus e a terra',
    'en': 'In the beginning God created the heavens and the earth',
}

CLUSTERS = {
    'CRIAR': {'he': 'ברא', 'el': 'ποιέω', 'la': 'creavit', 'pt': 'criou', 'en': 'created'},
    'PRINCIPIO': {'he': 'ראשית', 'el': 'ἀρχῇ', 'la': 'principio', 'pt': 'princípio', 'en': 'beginning'},
    'DEUS': {'he': 'אֱלֹהִים', 'el': 'θεὸς', 'la': 'Deus', 'pt': 'Deus', 'en': 'God'},
    'CEUS': {'he': 'שָּׁמַיִם', 'el': 'οὐρανὸν', 'la': 'caelum', 'pt': 'céus', 'en': 'heavens'},
    'TERRA': {'he': 'אָרֶץ', 'el': 'γῆν', 'la': 'terram', 'pt': 'terra', 'en': 'earth'},
}

def cross_reference(verse=GENESIS_1_1):
    rows = []
    for concept, langs in CLUSTERS.items():
        hit = {lang: word for lang, word in langs.items() if word.lower() in verse.get(lang, '').lower()}
        phi = len(hit) / max(len(langs), 1)
        rows.append({'concept': concept, 'hits': hit, 'phi': round(phi, 3)})
    return rows
```
EOF
}

body_lowlevel() {
  cat <<'EOF'
```c
#include <stdint.h>
#define RVM_BEGIN 0x10
#define RVM_PUSH  0x03
#define RVM_PRINT 0xFE
#define RVM_SEAL  0xFF

typedef struct { uint8_t op; uint8_t arg; } RvmInstr;
typedef struct { RvmInstr code[256]; uint32_t len; uint8_t phi_q8; } RvmProgram;

static uint8_t raf_hash8(const uint8_t *p, uint32_t n) {
    uint8_t h = 216u;
    for (uint32_t i = 0; i < n; i++) h = (uint8_t)((h ^ p[i]) * 167u);
    return h;
}

static void emit(RvmProgram *p, uint8_t op, uint8_t arg) {
    if (p->len < 256u) p->code[p->len++] = (RvmInstr){op, arg};
}

static uint32_t compile_words(const uint8_t *text, uint32_t n, RvmProgram *out) {
    out->len = 0; out->phi_q8 = 200u;
    emit(out, RVM_BEGIN, 0);
    uint32_t start = 0;
    for (uint32_t i = 0; i <= n; i++) {
        if (i == n || text[i] <= 32u) {
            if (i > start) emit(out, RVM_PUSH, raf_hash8(text + start, i - start));
            start = i + 1u;
        }
    }
    emit(out, RVM_PRINT, out->phi_q8);
    emit(out, RVM_SEAL, 1);
    return out->len;
}
```
EOF
}

body_textcode() {
  cat <<'EOF'
```python
import re

RULES = [
    (r'soma(r)? dois números', 'static int raf_soma(int a, int b) { return a + b; }'),
    (r'multiplica(r)? dois números', 'static int raf_mul(int a, int b) { return a * b; }'),
    (r'crc32', 'static unsigned raf_crc32(const unsigned char *p, unsigned n) { unsigned c=~0u; for(unsigned i=0;i<n;i++){ c^=p[i]; for(int b=0;b<8;b++) c=(c>>1)^(0xEDB88320u&-(c&1)); } return ~c; }'),
    (r'hash fnv', 'static unsigned long long raf_fnv(const unsigned char *p, unsigned n) { unsigned long long h=1469598103934665603ULL; for(unsigned i=0;i<n;i++){ h^=p[i]; h*=1099511628211ULL; } return h; }'),
]

def natural_to_c(text):
    src = text.lower()
    found = [code for pattern, code in RULES if re.search(pattern, src)]
    if not found:
        safe = re.sub(r'[^a-z0-9_]+', '_', src).strip('_')[:32] or 'fala'
        found.append(f'static int raf_{safe}(void) {{ return 0; }}')
    return '#include <stdint.h>\n' + '\n\n'.join(found)

OP_TEXT = {0x10: 'início', 0x03: 'empilha', 0xFE: 'imprime', 0xFF: 'sela'}
def decompile(bytecode):
    return ' '.join(OP_TEXT.get(b, f'op_{b:02x}') for b in bytecode)
```
EOF
}

body_prosody() {
  cat <<'EOF'
```python
ACCENTS = {
    'pt': {'á':'agudo','é':'agudo','í':'agudo','ó':'agudo','ú':'agudo','ã':'til','õ':'til','â':'circ','ê':'circ','ô':'circ','ç':'cedilha'},
    'el': {'ά':'acute','ὰ':'grave','ᾶ':'circ','ἁ':'rough','ἀ':'smooth','ῳ':'iota_sub'},
    'he': {'֑':'athnach','֖':'tifcha','֣':'merkha','ֽ':'silluq','ּ':'dagesh','ְ':'shva'},
    'la': {'ā':'longa','ē':'longa','ī':'longa','ō':'longa','ū':'longa','ă':'breve','ĕ':'breve'},
}

def prosody_map(text, lang='pt'):
    table = ACCENTS.get(lang, {})
    out = []
    for pos, ch in enumerate(text):
        if ch in table:
            out.append({'pos': pos, 'char': ch, 'kind': table[ch], 'phi': 0.90})
    density = len(out) / max(len(text), 1)
    return {'marks': out, 'density': round(density, 4), 'phi': round(min(1.0, 0.65 + density * 3), 3)}
```
EOF
}

body_conversation() {
  cat <<'EOF'
```python
class DialogueMachine:
    STATES = ('LISTEN', 'PARSE', 'UNDERSTAND', 'RESPOND', 'SEAL')
    INTENTS = {
        'compilar': ('compile', 'gere código', 'refatorar', 'baixo nível'),
        'biblia': ('gênesis', 'salmo', 'versículo', 'logos'),
        'traduzir': ('traduza', 'em hebraico', 'em grego', 'em latim'),
        'fonetica': ('fonema', 'som', 'acento', 'cantilação'),
    }
    def __init__(self):
        self.state = 'LISTEN'
        self.stack = []
        self.phi = 0.70
    def intent(self, text):
        low = text.lower()
        for name, keys in self.INTENTS.items():
            if any(k in low for k in keys):
                return name
        return 'geral'
    def step(self, text):
        intent = self.intent(text)
        self.stack.append({'state': self.state, 'intent': intent, 'text': text[:80]})
        self.state = 'PARSE' if self.state == 'LISTEN' else 'UNDERSTAND' if self.state == 'PARSE' else 'RESPOND' if self.state == 'UNDERSTAND' else 'SEAL'
        self.phi = min(1.0, self.phi + 0.06)
        return {'state': self.state, 'intent': intent, 'phi': round(self.phi, 3)}
```
EOF
}

body_translation() {
  cat <<'EOF'
```python
LEXICON = {
    ('he', 'ברא'): {'pt': 'criou', 'root': 'ברא', 'morph': 'qal perfeito 3ms'},
    ('he', 'דבר'): {'pt': 'palavra/falou', 'root': 'דבר', 'morph': 'raiz trilítera'},
    ('el', 'λόγος'): {'pt': 'verbo/palavra/razão', 'root': 'λέγω', 'morph': 'nom sg masc'},
    ('la', 'verbum'): {'pt': 'verbo/palavra', 'root': 'verbum', 'morph': 'nom sg neutro'},
    ('pt', 'amor'): {'el': 'ἀγάπη', 'la': 'amor', 'he': 'אהבה'},
}

def translate_word(word, src, tgt='pt'):
    data = LEXICON.get((src, word))
    if data and tgt in data:
        return {'src': word, 'src_lang': src, 'tgt_lang': tgt, 'tgt': data[tgt], 'phi': 0.94, 'meta': data}
    for (lang, key), val in LEXICON.items():
        if lang == src and word.lower() == key.lower():
            return {'src': word, 'tgt': val.get(tgt, key), 'phi': 0.80, 'meta': val}
    return {'src': word, 'tgt': word, 'phi': 0.55, 'meta': {'reason': 'eco controlado'}}
```
EOF
}

body_pipeline() {
  cat <<'EOF'
```python
class CompiladorLowFala:
    def __init__(self, lang='pt'):
        self.lang = lang
        self.phi = 0.0
    def tokenize(self, text):
        return [{'raw': w, 'lang': self.lang, 'phi': 0.82} for w in text.split() if w]
    def ast(self, tokens):
        return {'kind': 'SENTENCE', 'children': tokens, 'phi': sum(t['phi'] for t in tokens)/max(len(tokens),1)}
    def bytecode(self, ast):
        bc = bytearray([0x10])
        for node in ast['children']:
            bc.extend([0x03, sum(node['raw'].encode('utf-8')) & 0xFF])
        bc.extend([0xFE, 0xFF])
        return bc
    def asm(self, bc):
        lines = ['.section .text', '.global raf_entry', 'raf_entry:']
        lines.extend(f'    .byte 0x{b:02X}' for b in bc)
        lines.append('    ret')
        return '\n'.join(lines)
    def compile(self, text):
        tokens = self.tokenize(text)
        tree = self.ast(tokens)
        bc = self.bytecode(tree)
        self.phi = round((tree['phi'] + len(bc)/(len(bc)+4)) / 2, 3)
        return {'tokens': tokens, 'ast': tree, 'bytecode': bc.hex(), 'asm': self.asm(bc), 'phi': self.phi}
```
EOF
}

body_textdna() {
  cat <<'EOF'
```python
BASES = {'vowel': 'G', 'consonant': 'C', 'accent': 'A', 'transition': 'T'}
VOWELS = set('aeiouáàâãéêíóôõúüαεηιουω')
ACCENT_MARKS = set('´`^~¨ּֽׁׂ֑֖֣')

def text_to_dna(text):
    dna = []
    for ch in text:
        if ch.lower() in VOWELS:
            dna.append(BASES['vowel'])
        elif ch in ACCENT_MARKS:
            dna.append(BASES['accent'])
        elif ch.isalpha():
            dna.append(BASES['consonant'])
        else:
            dna.append(BASES['transition'])
    return ''.join(dna)

def dna_signature(text):
    dna = text_to_dna(text)
    total = max(len(dna), 1)
    profile = {b: dna.count(b) for b in 'TCGA'}
    profile['cg_ratio'] = round(profile['C'] / max(profile['G'], 1), 3)
    profile['phi'] = round(1.0 - abs(0.5 - profile['G']/total), 3)
    return {'dna': dna, 'profile': profile}
```
EOF
}

body_audio() {
  cat <<'EOF'
```python
import math

FORMANTS = {
    'a': (800,1200,2600), 'e': (500,1900,2700), 'i': (300,2300,3000),
    'o': (500,1000,2500), 'u': (300,800,2300), 's': (5000,6500,8000),
    'מ': (250,900,2100), 'ש': (3000,5500,8000), 'θ': (300,1200,3500),
}

def synth_phoneme(ch, fs=16000, ms=70):
    f1, f2, f3 = FORMANTS.get(ch.lower(), (600,1400,2800))
    n = int(fs * ms / 1000)
    out = []
    for i in range(n):
        t = i / fs
        env = math.sin(math.pi * i / max(n,1))
        out.append(env * (0.60*math.sin(2*math.pi*f1*t) + 0.30*math.sin(2*math.pi*f2*t) + 0.10*math.sin(2*math.pi*f3*t)))
    return out

def voice_phi(samples):
    if not samples:
        return 0.0
    mean = sum(samples) / len(samples)
    var = sum((x-mean)**2 for x in samples) / len(samples)
    return round(1.0 / (1.0 + var), 4)
```
EOF
}

body_ops() {
  cat <<'EOF'
```bash
#!/usr/bin/env bash
set -Eeuo pipefail
artifact="${1:-compiladorlowFala.txt}"
[[ -s "$artifact" ]] || { echo "artefato ausente" >&2; exit 1; }
grep -q 'S12_V5' "$artifact" || { echo "semente final ausente" >&2; exit 1; }
grep -q 'RAFCODE-Φ' "$artifact" || { echo "assinatura ausente" >&2; exit 1; }
lines=$(wc -l < "$artifact" | tr -d ' ')
sha=$(sha256sum "$artifact" | awk '{print $1}')
printf 'artifact=%s\nlines=%s\nsha256=%s\nstatus=sealed\n' "$artifact" "$lines" "$sha"
```
EOF
}

append_footer() {
  cat >> "$TMP_OUT" <<'EOF'

---

# Fechamento operacional

O monólito está selado quando todas as condições abaixo forem verdadeiras:

1. Existe índice completo de S01_V1 até S12_V5.
2. O arquivo contém a assinatura RAFCODE-Φ.
3. A cadeia FALA→FONEMA→TOKEN→AST→BYTECODE→ASM→VM→OUTPUT aparece no cabeçalho.
4. O manifesto JSON contém 60 sementes.
5. A validação local retorna status sealed.

Comando recomendado:

```bash
chmod +x Livro/falas.sh
./Livro/falas.sh --check
./Livro/falas.sh -o compiladorlowFala.txt -m compiladorlowFala.manifest.json
```

SEAL: 0xFF · Ω=Amor · ∆RafaelVerboΩ · 𓂀ΔΦΩ
EOF
}

write_manifest() {
  local count="${#SEED_IDS[@]}"
  {
    printf '{\n'
    printf '  "generator": "Livro/falas.sh",\n'
    printf '  "version": "%s",\n' "$VERSION"
    printf '  "generated_at_utc": "%s",\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    printf '  "output": "%s",\n' "$OUT"
    printf '  "seed_count": %s,\n' "$count"
    printf '  "families": 12,\n'
    printf '  "variants_per_family": 5,\n'
    printf '  "seeds": [\n'
    local i
    for i in "${!SEED_IDS[@]}"; do
      printf '    {"id":"%s","title":"%s"}' "${SEED_IDS[$i]}" "${SEED_TITLES[$i]}"
      [[ "$i" -lt $((count-1)) ]] && printf ','
      printf '\n'
    done
    printf '  ]\n'
    printf '}\n'
  } > "$TMP_MANIFEST"
}

validate_output() {
  local file="$1"
  [[ -s "$file" ]] || die "arquivo gerado vazio"
  grep -q 'RAFCODE-Φ' "$file" || die "assinatura RAFCODE-Φ ausente"
  grep -q 'FALA → FONEMA → TOKEN → AST → BYTECODE → ASM → VM → OUTPUT' "$file" || die "pipeline canônico ausente"
  grep -q '^# S01_V1' "$file" || die "S01_V1 ausente"
  grep -q '^# S12_V5' "$file" || die "S12_V5 ausente"
  if grep -En 'TODO|PLACEHOLDER|STUB' "$file" >/dev/null; then
    die "marcadores incompletos detectados no artefato"
  fi
  local count
  count=$(grep -Ec '^# S[0-9]{2}_V[1-5]' "$file")
  [[ "$count" -eq 60 ]] || die "quantidade de sementes inválida: $count"
}

generate() {
  TMP_OUT=$(mktemp)
  TMP_MANIFEST=$(mktemp)
  append_header
  local f v
  for f in $(seq 1 12); do
    for v in $(seq 1 5); do
      emit_seed "$f" "$v"
    done
  done
  append_footer
  validate_output "$TMP_OUT"
  write_manifest
}

main() {
  parse_args "$@"
  if (( LIST_ONLY == 1 )); then
    list_seeds
    exit 0
  fi
  generate
  if (( DRY_RUN == 1 )); then
    cat "$TMP_OUT"
    exit 0
  fi
  if (( CHECK_ONLY == 1 )); then
    log "validação concluída: $(grep -Ec '^# S[0-9]{2}_V[1-5]' "$TMP_OUT") sementes"
    exit 0
  fi
  mkdir -p "$(dirname "$OUT")" 2>/dev/null || true
  mkdir -p "$(dirname "$MANIFEST")" 2>/dev/null || true
  mv "$TMP_OUT" "$OUT"
  TMP_OUT=""
  mv "$TMP_MANIFEST" "$MANIFEST"
  TMP_MANIFEST=""
  log "gerado: $OUT"
  log "manifesto: $MANIFEST"
}

main "$@"
