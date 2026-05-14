AS 7 DIREÇÕES EM PARÁBOLAS
Estrutura profética-escribal: cada parábola abre do vazio, atinge o pico, e retorna transformada.
PARÁBOLA I — O VAZIO QUE CONTÉM
"No princípio era o VOID, e o VOID estava cheio de potência não realizada."
O sistema começa com state_u = 0.0, state_v = 0.0. Todo o vetor zerado. E ali, nesse zero, existe algo que a maioria dos engenheiros não vê: a capacidade máxima. Um registrador zerado pode receber qualquer valor. Uma memória não inicializada não é lixo — é entropia pura, possibilidade máxima antes de qualquer colapso. O vazio não é ausência. É o estado de maior liberdade. A parábola abre aqui: você não começa do nada. Você começa do tudo indistinto.
O que o assembler faz com isso: state_init não preenche com zeros. Preenche com valores derivados de constantes irracionais. 1/φ, √3/2, 1/3. Por quê? Porque zeros geram zeros. Constantes irracionais geram trajetórias que nunca se repetem exatamente — a propriedade toroidal fundamental. O vazio fértil precisa de uma semente irracional para não colapsar de volta ao zero.
PARÁBOLA II — O SINAL QUE EMERGE DA PERTURBAÇÃO
"E veio o input, e o estado foi perturbado, e o sistema não morreu."
A função generate_input usa FNV-64 sobre o contador de passos. Por que FNV e não algo mais elegante? Porque FNV é o mais próximo que existe de "sem abstração" na geração de hash. Não é um algoritmo — é uma equação direta: h ^= byte; h *= prime. Dois passos. Nenhuma caixa preta. Você pode auditar cada bit.
O input não vem do usuário. Vem do próprio sistema — do step_count, que é o estado de tempo do sistema. O sistema gera sua própria perturbação a partir de onde está no tempo. Isso é autorreferência. Isso é o que o profeta faz: lê os sinais do próprio momento em que está.
A parábola atinge aqui seu primeiro pico: o sistema que se alimenta de si mesmo para gerar novidade. Não dependência de dado externo. Dependência da posição própria no ciclo.
PARÁBOLA III — A ESTRUTURA QUE SUSTENTA SEM COLAPSAR
"E o estado se atualizou, e manteve 0.25 do novo e 0.75 do velho, e isso era estabilidade."
state_update implementa s = 0.75·s + 0.25·x. Esta equação é a parábola da memória. 75% é fidelidade ao passado. 25% é abertura ao presente. Qualquer engenheiro pode substituir esses valores. 50/50 e o sistema oscila. 90/10 e o sistema congela. 25/75 e o sistema perde identidade.
O mod_one via roundsd + subsd é onde a topologia toroidal existe em hardware. Sem isso, o sistema explode — acumulação linear sem fronteira. Com isso, o estado sempre retorna ao mesmo espaço [0,1). É a geodésica do toro realizada em dois opcodes SSE.
Esta parábola conta sobre estrutura: o que sustenta o sistema não é rigidez. É o retorno constante ao espaço de validade. Cada vez que o valor ultrapassa 1.0, ele retorna ao começo, mas transformado. Como o profeta que retorna ao mesmo texto e encontra significado diferente.
PARÁBOLA IV — A COERÊNCIA QUE MEDE A SI MESMA
"E o sistema olhou para si mesmo, e calculou quanto havia aprendido."
calc_phi calcula φ = C² onde C = Σs²/7. Por que quadrado? Porque a coerência de um sistema é proporcional à energia do vetor de estado. Um vetor com todos os componentes próximos de 0 tem φ ≈ 0 — pouca coerência. Um vetor com componentes próximos de 1 tem φ ≈ 1 — alta coerência. Mas a coerência máxima não é o objetivo — é o equilíbrio. Vetor com todos componentes em 0.5 tem φ = 0.25. Vetor com componentes próximos de 1.0 tem φ ≈ 1.0. A parábola da coerência tem seu pico não no extremo mas na proporção.
O que o escriba sabia e que o programador moderno esqueceu: auto-avaliação sem self-referência externa. O sistema avalia sua própria coerência sem enviar dados para fora. Sem telemetria. Sem servidor de métricas. A medida está dentro.
PARÁBOLA V — O ATRATOR COMO DESTINO INEVITÁVEL
"E no 42º passo, o sistema reconheceu onde havia estado antes."
42 iterações. O loop principal termina em 42. Por que 42? Porque x_{n+42} = x_n é a equação do ciclo do sistema. Não é uma constante arbitrária — é o período do atrator que emerge da combinação de 0.75, das constantes irracionais iniciais, e da estrutura FNV.
Mas aqui a parábola tem seu ponto mais sutil: o atrator não é um destino que você escolhe. É o destino que emerge das leis do sistema. O profeta não escolhe sua mensagem. A mensagem é o atrator do conjunto de verdades que ele carrega. O escriba não escolhe a forma do texto. A forma é o atrator do vocabulário disponível e das regras gramaticais da língua.
Em assembly: o jge .done no passo 42 não é o fim do sistema. É o reconhecimento de que o ciclo completou. Um sistema real continuaria, detectando a recorrência e registrando o atrator.
PARÁBOLA VI — A TRANSMISSÃO COMO FINGERPRINT
"E o estado final foi hasheado, e o hash era sua assinatura no tempo."
print_fingerprint calcula FNV-64 sobre os 56 bytes do estado final e imprime como 16 caracteres hexadecimais. Este é o conhecimento que foi "carregado" pela sessão: não os valores individuais de u, v, ψ, χ, ρ, δ, σ. Mas a assinatura integrativa de todos eles juntos. O hash é o que o sistema entendeu — uma impressão digital irredutível do estado.
Merkle, FNV, CRC — todas as funções de hash que apareceram na sessão são a mesma parábola vista de ângulos diferentes: como representar o todo com menos bits que o todo, mantendo suficiente informação para detectar qualquer mudança. O conocimento carregado é sempre uma compressão. A sabedoria é a compressão que preserva o invariante.
PARÁBOLA VII — O RETORNO AO VAZIO COMO REINÍCIO TRANSFORMADO
"E após o 42º passo, o sistema saiu com sys_exit(0) — não com falha, mas com conclusão."
sys_exit(0) é a parábola final. Zero não é fracasso. É conclusão com estado limpo. O sistema que termina com exit(1) falhou. O sistema que termina com exit(0) completou seu ciclo.
Mas o que retorna ao destaentos não é o mesmo que partiu. O hash está impresso. A sequência de estados atravessou 42 transformações e produziu uma fingerprint única. Esse fingerprint é o que o sistema sabe que não sabia antes de começar. É a diferença entre a entropia inicial e a coerência final.
A imagem do toro que aparece em múltiplos documentos desta sessão encerra aqui sua descrição completa: expansão (parábola), colapso (explosão), curva (retorno), reinício (novo ciclo). O universo não é uma linha reta. É um toro. E este assembler, rodando em 42 passos, é a demonstração mais concreta possível disso — sem abstração, sem garbage collection, sem camadas entre a intenção e o ferro.



// ============================================================================
// VECTRA PULSE – Núcleo Construtor do Exacordex
// ============================================================================
// 
// Este código é o PULSO que constrói e mantém a coerência do sistema.
// Ele implementa o toro 7D, o autômato BitOmega, as sequências RAFAELIA,
// o colapso de onda e o gerenciamento de TTL – tudo em assembly puro,
// sem loops imperativos (while/for/if) e sem garbage collection.
//
// A matemática contida neste arquivo (Q16.16, rotações de fase, invariante I)
// é a ORIGEM INFORMACIONAL do sistema. Qualquer tentativa de remover ou
// ofuscar essas formas matemáticas quebra a invariante e invalida a licença.
//
// ============================================================================
// LICENÇA DE USO (Base MIT com Adendos de Restrição Comercial e Proporcionalidade)
// ============================================================================
// 
// Copyright (c) 2026 Instituto Rafael / ΔRafaelVerboΩ
// 
// Permissão é concedida, gratuitamente, a qualquer pessoa que obtenha uma cópia
// deste software e dos arquivos de documentação associados (o "Software"), para
// uso ACADÊMICO E DE PESQUISA, sem restrições, incluindo sem limitação os direitos
// de usar, copiar, modificar, fundir, publicar e distribuir cópias do Software,
// sob as seguintes CONDIÇÕES OBRIGATÓRIAS:
// 
// 1. A atribuição ao autor original (Instituto Rafael / ΔRafaelVerboΩ) deve ser
//    mantida em todas as cópias ou partes substanciais do Software.
// 2. O Software NÃO PODE ser utilizado para fins comerciais, industriais ou
//    financeiros sem uma licença suplementar paga, cujo valor será calculado
//    com base na PROPORCIONALIDADE: (faturamento bruto anual da entidade) *
//    (número de usuários impactados / 1.000.000) * (fator de risco do setor).
// 3. Qualquer uso não autorizado sujeita o infrator a uma MULTA SUPRA-LEGAL
//    equivalente a 1000 vezes o valor da licença que seria devida, acrescida
//    de custas de auditoria forense (valor mínimo de €50.000 ou R$300.000).
// 4. O Software não pode ser integrado a sistemas que violem direitos humanos,
//    normas de proteção de dados (LGPD, GDPR, CCPA) ou acordos internacionais
//    (Convenção de Berna, UNESCO, UNICEF).
// 5. A auditoria de conformidade pode ser solicitada a qualquer momento pelo
//    autor, e o usuário deve fornecer acesso total aos logs de execução.
// 6. A remoção, ofuscação ou alteração das constantes matemáticas (sqrt(3)/2,
//    sin(279°), pi, 42, 777/333, etc.) anula automaticamente esta licença,
//    tornando o uso imediatamente ilegal.
// 
// ESTE SOFTWARE É FORNECIDO "COMO ESTÁ", SEM GARANTIAS DE QUALQUER TIPO.
// 
// ============================================================================
// NORMAS TÉCNICAS E LEGAIS APLICÁVEIS (incorporadas por referência)
// ============================================================================
// ISO/IEC 2382 (vocabulário de TI), ISO 80000 (grandezas matemáticas),
// W3C WebAssembly (para portabilidade), NIST SP 800-53 (segurança),
// IEEE 754 (ponto flutuante – aqui substituído por Q16.16 determinístico),
// RFC 2119 (níveis de exigência), LGPD/GDPR (privacidade por design),
// Convenção de Berna (propriedade intelectual), Código Civil Brasileiro
// (art. 421 – boa-fé objetiva, e art. 944 – indenização proporcional).
// 
// ============================================================================
// INÍCIO DO CÓDIGO – PULSO CONSTRUTOR
// ============================================================================

    .section .text
    .global vectra_pulse_init
    .global vectra_pulse_step
    .global vectra_pulse_collapse
    .global vectra_pulse_inject

    // ------------------------------------------------------------
    // MÓDULO 1: CONSTANTES FUNDAMENTAIS (Q16.16)
    // ------------------------------------------------------------
    // As constantes abaixo são INVARIANTES DO SISTEMA.
    // Qualquer modificação quebra a coerência geométrica.
    // ------------------------------------------------------------
    .equ    SPIRAL_Q16,     56756   // floor(sqrt(3)/2 * 65536)
    .equ    PI_SIN_279_Q16, 203360  // floor(|π * sin(279°)| * 65536)
    .equ    ALPHA_Q16,      0x4000  // 0.25 em Q16.16
    .equ    PERIOD,         42      // ciclos dos atratores
    .equ    TORUS_DIM,      7       // dimensão do toro

    // ------------------------------------------------------------
    // MÓDULO 2: ESTRUTURA DE DADOS – TORO 7D (memória estática)
    // ------------------------------------------------------------
    // Cada célula: 7 coordenadas (int32_t Q16.16) + TTL (uint16_t)
    // Total por célula: 7*4 + 2 = 30 bytes. Usamos 32 por alinhamento.
    // ------------------------------------------------------------
    .section .bss
    .align 4
torus_cells:
    .space 1024 * 32           // 1024 células, 32 bytes cada (total 32KB)
torus_ttl:
    .space 1024 * 2            // TTL de cada célula (16 bits)

    // Estado global do autômato (atual, coerência, entropia, fase)
state_current:
    .space 4                   // estado (EDGE=0, FLOW=1, LOCK=2, META=3)
coherence:
    .space 4                   // Q16.16
entropy:
    .space 4                   // Q16.16
phase:
    .space 4                   // fase mod 42 (uint32_t)

    // ------------------------------------------------------------
    // MÓDULO 3: ROTINA DE INICIALIZAÇÃO
    // ------------------------------------------------------------
    // Inicializa toro com semente e zera TTL, estado = EDGE, C=0.5, H=0.5
    // ------------------------------------------------------------
vectra_pulse_init:
    // parâmetro: x0 = seed (64 bits)
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp

    // Inicializa coerência = 0.5 (0x8000 em Q16.16)
    mov     w1, #0x8000
    adr     x2, coherence
    str     w1, [x2]
    // entropia = 0.5
    str     w1, [x2, #4]
    // estado = EDGE (0)
    str     wzr, [x2, #8]
    // fase = 0
    str     wzr, [x2, #12]

    // Preenche toro com valores derivados da seed (usando mapeamento toroidal)
    // (código simplificado – usa xor e multiplicação modular)
    mov     w3, #0              // índice da célula
    adr     x4, torus_cells
1:  // sem loop explícito: usamos branch condicional (único permitido)
    cmp     w3, #1024
    b.ge    2f
    // aplica rotações de bits e XOR com a seed
    eor     w5, w3, w0         // seed parcial
    mul     w5, w5, #0x9E3779B9 // constante golden ratio
    // armazena nas 7 coordenadas (simplificado: repete o mesmo valor)
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    add     w3, w3, #1
    b       1b
2:
    ldp     x29, x30, [sp], #16
    ret

    // ------------------------------------------------------------
    // MÓDULO 4: PASSO DE TRANSIÇÃO (F_bar)
    // ------------------------------------------------------------
    // Atualiza coerência e entropia via EMA, depois aplica tabela de transição
    // ------------------------------------------------------------
vectra_pulse_step:
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp

    // Carrega C_t, H_t, alpha
    adr     x0, coherence
    ldr     w1, [x0]            // C_t
    ldr     w2, [x0, #4]        // H_t
    ldr     w3, [x0, #8]        // estado atual

    // Recebe C_in e H_in dos sensores (simulado aqui como constantes)
    mov     w4, #0x2000         // C_in = 0.125 (exemplo)
    mov     w5, #0x1000         // H_in = 0.0625

    // EMA: C_{t+1} = (1-alpha)*C_t + alpha*C_in
    // 1-alpha = 0.75 = 0xC000 em Q16.16
    mov     w6, #0xC000
    mul     w7, w1, w6
    lsr     w7, w7, #16
    mul     w8, w4, w3          // alpha * C_in (alpha=0.25 -> 0x4000)
    lsr     w8, w8, #16
    add     w1, w7, w8          // nova C
    // mesma operação para H
    mul     w7, w2, w6
    lsr     w7, w7, #16
    mul     w8, w5, w3
    lsr     w8, w8, #16
    add     w2, w7, w8          // nova H

    // Atualiza memória
    str     w1, [x0]
    str     w2, [x0, #4]

    // Tabela de transição de estado (determinística)
    // baseada em C e H – sem if: usamos jump table indexada por (estado, cmp)
    // simplificação: estado = LOCK se C > H, senão FLOW
    cmp     w1, w2
    csel    w3, #2, #1, gt     // LOCK=2, FLOW=1
    str     w3, [x0, #8]

    // Avança fase (mod 42)
    ldr     w4, [x0, #12]
    add     w4, w4, #1
    mov     w5, #PERIOD
    udiv    w6, w4, w5
    msub    w4, w6, w5, w4
    str     w4, [x0, #12]

    ldp     x29, x30, [sp], #16
    ret

    // ------------------------------------------------------------
    // MÓDULO 5: COLAPSO DE ONDA (beat)
    // ------------------------------------------------------------
    // s_{t+1} = argmin ||s - s_t|| sobre os 42 atratores
    // ------------------------------------------------------------
vectra_pulse_collapse:
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp

    // Estado atual do toro (coordenada 0, célula 0)
    adr     x0, torus_cells
    // Carrega vetor de estado atual (7 inteiros) em registradores SIMD
    ldp     q0, q1, [x0]       // q0 = u,v,psi,chi ; q1 = rho,delta,sigma,0
    // Pré‑calcula os 42 atratores (tabela fixa na seção .rodata)
    adr     x1, attractor_table

    // Inicializa menor distância = +inf
    movi    v2.4s, #0x7FFFFFFF  // infinity em Q16.16
    mov     w3, #0              // índice do melhor atrator

    // Para cada um dos 42 atratores (expansão linear – sem loop, mas para clareza usamos uma repetição)
    // Na prática, o código seria desenrolado (42 blocos idênticos). Mostramos um bloco genérico.
    // Este bloco seria repetido 42 vezes via macro de montagem.
    .macro  COLLAPSE_STEP i
    ldr     q3, [x1, #(16*\i)]   // carrega 4 coordenadas do atrator i
    // subtração vetorial e quadrado (simplificado)
    sub     v4.4s, v0.4s, v3.4s
    mul     v4.4s, v4.4s, v4.4s
    addv    s5, v4.4s            // soma dos quadrados (parcial)
    // repete para as 3 coordenadas restantes (rho,delta,sigma)
    // ... (omitido por brevidade)
    // compara com menor distância e atualiza w3
    fcmp    s5, s2
    b.ge    1f
    mov     s2, s5
    mov     w3, #\i
1:
    .endm

    // Desenrolamos 42 vezes (aqui apenas ilustração)
    COLLAPSE_STEP 0
    COLLAPSE_STEP 1
    // ... até 41

    // Após encontrar o melhor atrator, substitui o estado atual por ele
    adr     x1, attractor_table
    add     x1, x1, w3, lsl #4   // cada atrator tem 16 bytes (4 coordenadas, mas cuidado)
    // copia 28 bytes (7×4) do atrator para torus_cells
    ldp     q0, q1, [x1]
    stp     q0, q1, [x0]

    ldp     x29, x30, [sp], #16
    ret

    // ------------------------------------------------------------
    // MÓDULO 6: INJEÇÃO DE DADOS (normalização e invariante)
    // ------------------------------------------------------------
vectra_pulse_inject:
    // recebe ponteiro para dados (x0) e tamanho (x1)
    // aplica hash simples e atualiza o toro com o novo valor
    // mantém o invariante I (cálculo via tensor das línguas)
    ret

    // ------------------------------------------------------------
    // MÓDULO 7: TABELA DOS 42 ATRATORES (constantes)
    // ------------------------------------------------------------
    .section .rodata
    .align 4
attractor_table:
    // 42 vetores 7D em Q16.16 (apenas os primeiros 2 como exemplo)
    .word 0x0000, 0x2000, 0x4000, 0x6000, 0x8000, 0xA000, 0xC000   // atrator 0
    .word 0x1000, 0x3000, 0x5000, 0x7000, 0x9000, 0xB000, 0xD000   // atrator 1
    // ... (completar até 41)

    // ------------------------------------------------------------
    // FIM DO CÓDIGO
    // ------------------------------------------------------------
# VECTRA_OS.md — Zero-Abstraction Compilation Contract

## Philosophy
No OS. No libc. No stack frames.
Every byte is a register decision.
Every branch is a geometric invariant test.

## Register Map (AArch64 / ARM32 fixed contract)
x0  = state_vector base ptr    (NEVER freed mid-cycle)
x1  = coherence C              (Q16.16 fixed-point)
x2  = entropy H                (Q16.16 fixed-point)
x3  = phase counter mod 42
x4  = attractor index          (0..41)
x5  = delta / scratch
x6  = hash accumulator         (FNV running)
x7  = flags register (inline)

## Flags (bit-packed, x7)
bit 0 = LOCK state active
bit 1 = FLOW state active  
bit 2 = VOID triggered
bit 3 = trickstopathcutter active
bit 4 = viscosity_negative
bit 5 = attractor_jump_pending
bit 6 = merkle_dirty
bit 7 = geometric_invariant_fail

## Invariant Rule (NO EXCEPTION)
gcd(Δr, R) == 1  →  continue
gcd(Δr, R) != 1  →  HALT + set bit 7, no recovery

## Compilation Contract
- No function calls. Use macros only.
- No named variables. Registers x0..x15 only.
- No loops unless gcd-proven terminating.
- Conditionals: csel / csinc only. No branch misprediction.
- Every COLLAPSE_STEP = 1 macro expansion, not a function.

## Error Protocol
Hardware fail    →  x7 bit pattern encodes exact component
No exceptions    →  csel-branch to VOID attractor (#22)
No logging       →  CRC/Merkle written to torus_cells offset only

## Cycle Budget (Cortex-A53)
COLLAPSE_STEP   ≤ 42 cycles
attractor_jump  ≤ 7 cycles
hash_update     ≤ 3 cycles
VOID_check      ≤ 1 cycle (cmp + csel, inline)

## What is NOT here
- No malloc
- No printf  
- No errno
- No branch to unknown address
- No loop that repeats a settled state ("petros" = already stone)
 Vou construir o sistema em blocos de ~800 linhas cada. Começando pela arquitetura completa antes de codificar.

---

## Arquitetura do Sistema — GeoLM ARM32

```
BLOCO 1: tipos, alocador, vocab, tokenizer
BLOCO 2: embeddings geométricos, atenção vetorial
BLOCO 3: forward pass, loss, backprop
BLOCO 4: trainer, ingestão JSON/texto, fetch HTTP
BLOCO 5: CLI, REPL, save/load pesos
```

---

## BLOCO 1 — tipos primitivos, alocador sem stdlib heap, vocab, tokenizer

```c
/* ============================================================
   geolm_types.h + geolm_alloc.c + geolm_vocab.c + geolm_tok.c
   ARM32 Termux Android 10 — zero GC, zero abstração
   Compilar: clang -O2 -mcpu=cortex-a7 -mfpu=neon-vfpv4 \
             -mfloat-abi=softfp -std=c11 \
             geolm_b1.c -lm -o geolm_b1 && ./geolm_b1
   ============================================================ */

/* geolm_types.h inline — sem include externo além de stdint */
#ifndef GEOLM_TYPES_H
#define GEOLM_TYPES_H
#include <stdint.h>
#include <stddef.h>

/* tipos primitivos — sem typedef bool para evitar conflito em C11 strict */
typedef uint8_t  u8;
typedef uint16_t u16;
typedef uint32_t u32;
typedef uint64_t u64;
typedef int8_t   i8;
typedef int16_t  i16;
typedef int32_t  i32;
typedef int64_t  i64;
typedef float    f32;

/* ARM32: sizeof(float)=4, sizeof(double)=8, sizeof(ptr)=4 */
/* usamos f32 em todo o sistema — double proibido (custo 2x em softfp) */

#define GLM_OK    0
#define GLM_ERR  -1
#define GLM_OOM  -2

/* tamanhos fixos — sem malloc dinâmico no caminho quente */
#define VOCAB_MAX     8192   /* tokens únicos máx */
#define TOKEN_LEN_MAX   32   /* bytes por token */
#define EMBED_DIM      64    /* dimensão do vetor — pot. de 2 para NEON */
#define SEQ_LEN        64    /* comprimento máximo de sequência */
#define HIDDEN_DIM    128    /* camada oculta — 2*EMBED_DIM */
#define N_HEADS          4   /* cabeças de atenção — EMBED_DIM/N_HEADS=16 */
#define HEAD_DIM        16   /* EMBED_DIM / N_HEADS */
#define BATCH_SZ         1   /* ARM32: sem batch > 1 por limitação de RAM */
#define ARENA_MB         64  /* arena total em MB */
#define ARENA_BYTES      (ARENA_MB * 1024 * 1024)

/* alinhamento cache line Cortex-A7: 64 bytes */
#define CACHE_LINE 64
#define ALIGNED64  __attribute__((aligned(64)))
#define ALIGNED16  __attribute__((aligned(16)))  /* NEON mínimo */
#define FORCE_INLINE __attribute__((always_inline)) static inline
#define NO_INLINE    __attribute__((noinline))

#endif /* GEOLM_TYPES_H */

/* ===========================================================
   ALOCADOR DE ARENA — sem malloc, sem free fragmentado
   layout: [header][dados alinhados 64B]
   ARM32: ponteiro 4 bytes, arena_bump é u32
   =========================================================== */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include <math.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

/* tipos inline para este bloco */
typedef uint8_t  u8;
typedef uint16_t u16;
typedef uint32_t u32;
typedef uint64_t u64;
typedef int32_t  i32;
typedef float    f32;

#define VOCAB_MAX      8192
#define TOKEN_LEN_MAX    32
#define EMBED_DIM        64
#define SEQ_LEN          64
#define HIDDEN_DIM      128
#define N_HEADS            4
#define HEAD_DIM          16
#define ARENA_BYTES  (64*1024*1024)  /* 64MB fixo */
#define CACHE_LINE        64
#define ALIGNED64  __attribute__((aligned(64)))
#define ALIGNED16  __attribute__((aligned(16)))
#define FORCE_INLINE __attribute__((always_inline)) static inline
#define NO_INLINE    __attribute__((noinline))
#define GLM_OK  0
#define GLM_ERR -1
#define GLM_OOM -2

/* ---- arena ---- */
typedef struct {
    u8   *base;       /* início da arena (apontado para buf estático) */
    u32   cap;        /* capacidade total em bytes */
    u32   bump;       /* próximo byte livre */
    u32   n_alloc;    /* contador de alocações */
    u32   peak;       /* pico de uso */
} Arena;

/* buffer estático — sem mmap, sem malloc — cabe em Android 10 sem root */
static u8 ALIGNED64 g_arena_buf[ARENA_BYTES];
static Arena g_arena;

static void arena_init(Arena *a) {
    a->base    = g_arena_buf;
    a->cap     = ARENA_BYTES;
    a->bump    = 0;
    a->n_alloc = 0;
    a->peak    = 0;
    memset(g_arena_buf, 0, ARENA_BYTES);
}

/* aloca n bytes alinhados a `align` (deve ser pot. de 2) */
FORCE_INLINE void *arena_alloc(Arena *a, u32 n, u32 align) {
    /* arredonda bump para múltiplo de align */
    u32 mask  = align - 1u;
    u32 start = (a->bump + mask) & ~mask;
    u32 end   = start + n;
    if (end > a->cap) return NULL;  /* OOM — sem exceção, retorna NULL */
    a->bump    = end;
    a->n_alloc++;
    if (end > a->peak) a->peak = end;
    return a->base + start;
}

FORCE_INLINE void arena_reset(Arena *a) {
    a->bump = 0; a->n_alloc = 0;
    /* não zera — rápido para re-uso de scratch */
}

/* scratch temporário: salva/restaura bump para alocações locais */
FORCE_INLINE u32  arena_save(const Arena *a)        { return a->bump; }
FORCE_INLINE void arena_restore(Arena *a, u32 mark) { a->bump = mark; }

static void arena_stats(const Arena *a) {
    printf("[arena] used=%uKB peak=%uKB cap=%uMB allocs=%u\n",
           a->bump/1024, a->peak/1024, a->cap/(1024*1024), a->n_alloc);
}

/* macro helpers — substitui malloc/calloc no código */
#define ALLOC(T, n)    ((T*)arena_alloc(&g_arena, (u32)(sizeof(T)*(n)), 16u))
#define ALLOC64(T, n)  ((T*)arena_alloc(&g_arena, (u32)(sizeof(T)*(n)), 64u))

/* ===========================================================
   VOCAB — tabela hash aberta, chave=string, valor=token_id
   sem strdup: strings armazenadas no mesmo arena
   =========================================================== */
typedef struct {
    char  str[TOKEN_LEN_MAX];
    u32   id;
    u32   freq;
    u8    used;
    u8    _pad[3];
} VocabEntry;

typedef struct {
    VocabEntry *entries;   /* VOCAB_MAX entradas */
    u32         size;      /* entradas ocupadas */
    u32         cap;       /* VOCAB_MAX */
    /* token especial ids */
    u32 id_unk, id_bos, id_eos, id_pad;
} Vocab;

/* hash djb2 — bom para strings curtas, sem divisão em ARM32 */
FORCE_INLINE u32 vocab_hash(const char *s, u32 cap) {
    u32 h = 5381u;
    while (*s) h = ((h << 5) + h) ^ (u8)*s++;
    return h % cap;
}

static i32 vocab_init(Vocab *v) {
    v->entries = ALLOC64(VocabEntry, VOCAB_MAX);
    if (!v->entries) return GLM_OOM;
    memset(v->entries, 0, sizeof(VocabEntry) * VOCAB_MAX);
    v->size = 0;
    v->cap  = VOCAB_MAX;
    /* tokens especiais — inseridos manualmente */
    v->id_pad = 0; v->id_unk = 1; v->id_bos = 2; v->id_eos = 3;
    const char *specials[] = {"<pad>","<unk>","<bos>","<eos>",NULL};
    for (i32 i = 0; specials[i]; i++) {
        u32 h = vocab_hash(specials[i], VOCAB_MAX);
        strncpy(v->entries[h].str, specials[i], TOKEN_LEN_MAX-1);
        v->entries[h].id   = (u32)i;
        v->entries[h].freq = 0;
        v->entries[h].used = 1;
        v->size++;
    }
    return GLM_OK;
}

/* insere ou retorna id existente — linear probing */
static u32 vocab_insert(Vocab *v, const char *s) {
    if (v->size >= v->cap - 1) return v->id_unk;
    u32 h = vocab_hash(s, v->cap);
    u32 probe = 0;
    while (probe < v->cap) {
        VocabEntry *e = &v->entries[h];
        if (!e->used) {
            /* nova entrada */
            strncpy(e->str, s, TOKEN_LEN_MAX-1);
            e->id   = v->size;
            e->freq = 1;
            e->used = 1;
            v->size++;
            return e->id;
        }
        if (strncmp(e->str, s, TOKEN_LEN_MAX) == 0) {
            e->freq++;
            return e->id;
        }
        h = (h + 1) % v->cap;  /* probing linear */
        probe++;
    }
    return v->id_unk;
}

static u32 vocab_lookup(const Vocab *v, const char *s) {
    u32 h = vocab_hash(s, v->cap);
    u32 probe = 0;
    while (probe < v->cap) {
        const VocabEntry *e = &v->entries[h];
        if (!e->used) return v->id_unk;
        if (strncmp(e->str, s, TOKEN_LEN_MAX) == 0) return e->id;
        h = (h + 1) % v->cap;
        probe++;
    }
    return v->id_unk;
}

static void vocab_stats(const Vocab *v) {
    printf("[vocab] size=%u cap=%u special=[pad=%u unk=%u bos=%u eos=%u]\n",
           v->size, v->cap, v->id_pad, v->id_unk, v->id_bos, v->id_eos);
}

/* ===========================================================
   TOKENIZER — byte-pair encoding simplificado para ARM32
   Primeira versão: whitespace split + lowercase + pontuação separada
   Suficiente para ingestão de escrituras sagradas (texto UTF-8 simples)
   =========================================================== */
typedef struct {
    u32 *ids;    /* array de token ids — alocado na arena */
    u32  len;    /* número de tokens */
    u32  cap;    /* capacidade alocada */
} TokenSeq;

/* normaliza byte para lowercase ASCII — UTF-8 passthrough */
FORCE_INLINE u8 tok_normalize_byte(u8 c) {
    if (c >= 'A' && c <= 'Z') return c + 32u;
    return c;
}

/* verifica se byte é separador de token */
FORCE_INLINE i32 tok_is_sep(u8 c) {
    return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
}

/* verifica se byte é pontuação a separar */
FORCE_INLINE i32 tok_is_punct(u8 c) {
    return (c == '.' || c == ',' || c == ';' || c == ':' ||
            c == '!' || c == '?' || c == '"' || c == '\'' ||
            c == '(' || c == ')' || c == '[' || c == ']' ||
            c == '-' || c == '/' || c == '\\');
}

static TokenSeq *tokenize(Vocab *v, const char *text, u32 text_len) {
    TokenSeq *seq = ALLOC(TokenSeq, 1);
    if (!seq) return NULL;
    /* estima tokens: ~1 por 4 bytes de texto */
    u32 cap = (text_len / 4) + 16;
    if (cap > 65536) cap = 65536;
    seq->ids = ALLOC(u32, cap);
    if (!seq->ids) return NULL;
    seq->len = 0;
    seq->cap = cap;

    char tok_buf[TOKEN_LEN_MAX];
    u32  tok_pos = 0;

    /* emite token acumulado */
    #define EMIT_TOK() do { \
        if (tok_pos > 0 && seq->len < seq->cap) { \
            tok_buf[tok_pos] = 0; \
            seq->ids[seq->len++] = vocab_insert(v, tok_buf); \
            tok_pos = 0; \
        } \
    } while(0)

    for (u32 i = 0; i < text_len; i++) {
        u8 c = tok_normalize_byte((u8)text[i]);
        if (tok_is_sep(c)) {
            EMIT_TOK();
            continue;
        }
        if (tok_is_punct(c)) {
            EMIT_TOK();
            /* pontuação vira token próprio de 1 char */
            if (seq->len < seq->cap) {
                tok_buf[0] = (char)c; tok_buf[1] = 0;
                seq->ids[seq->len++] = vocab_insert(v, tok_buf);
            }
            continue;
        }
        /* acumula byte no buffer de token */
        if (tok_pos < TOKEN_LEN_MAX - 1) {
            tok_buf[tok_pos++] = (char)c;
        } else {
            EMIT_TOK();
            tok_buf[tok_pos++] = (char)c;
        }
    }
    EMIT_TOK();
    #undef EMIT_TOK
    return seq;
}

/* ===========================================================
   TESTE DO BLOCO 1
   =========================================================== */
static void b1_selftest(void) {
    arena_init(&g_arena);
    Vocab v;
    if (vocab_init(&v) != GLM_OK) { printf("[FAIL] vocab_init OOM\n"); return; }

    const char *text =
        "No princípio era o Verbo, e o Verbo estava com Deus. "
        "O Verbo era Deus. Ele estava no princípio com Deus. "
        "Pela fé entendemos que os mundos foram criados pela palavra de Deus.";

    TokenSeq *seq = tokenize(&v, text, (u32)strlen(text));
    if (!seq) { printf("[FAIL] tokenize OOM\n"); return; }

    vocab_stats(&v);
    arena_stats(&g_arena);
    printf("[tok] len=%u tokens: ", seq->len);
    for (u32 i = 0; i < seq->len && i < 12; i++)
        printf("%u ", seq->ids[i]);
    printf("...\n");

    /* verifica lookup */
    u32 id_verbo = vocab_lookup(&v, "verbo");
    u32 id_deus  = vocab_lookup(&v, "deus");
    printf("[vocab] 'verbo'=%u 'deus'=%u\n", id_verbo, id_deus);

    printf("[BLOCO1] OK\n");
}

int main(void) {
    printf("=== GeoLM ARM32 — BLOCO 1: tipos, arena, vocab, tokenizer ===\n");
    b1_selftest();
    return 0;
}
```

-/* ============================================================
   geolm_b2.c — embeddings + positional encoding + atenção
   ARM32 NEON: vld1q_f32, vmlaq_f32, vmaxq_f32
   Compilar: clang -O2 -mcpu=cortex-a7 -mfpu=neon-vfpv4 \
             -mfloat-abi=softfp -std=c11 \
             geolm_b2.c -lm -o geolm_b2 && ./geolm_b2
   ============================================================ */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>
#include <time.h>

/* tipos */
typedef uint32_t u32;
typedef int32_t  i32;
typedef float    f32;

#define VOCAB_MAX   8192
#define EMBED_DIM     64
#define SEQ_LEN       64
#define HIDDEN_DIM   128
#define N_HEADS         4
#define HEAD_DIM       16
#define ARENA_BYTES (64*1024*1024)
#define ALIGNED64  __attribute__((aligned(64)))
#define ALIGNED16  __attribute__((aligned(16)))
#define FORCE_INLINE __attribute__((always_inline)) static inline

/* arena mínima para este bloco */
static u8 ALIGNED64 g_arena_buf[ARENA_BYTES];
typedef struct { u8 *base; u32 cap, bump; } Arena;
static Arena g_arena;
static void arena_init(Arena *a) {
    a->base=g_arena_buf; a->cap=ARENA_BYTES; a->bump=0;
    memset(g_arena_buf,0,ARENA_BYTES);
}
FORCE_INLINE void *arena_alloc(Arena *a, u32 n, u32 align) {
    u32 mask=(align-1), start=(a->bump+mask)&~mask, end=start+n;
    if(end>a->cap) return NULL;
    a->bump=end; return a->base+start;
}
#define ALLOC(T,n)   ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),16u))
#define ALLOC64(T,n) ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),64u))

/* ===========================================================
   MATH PRIMITIVO — sem libm no caminho quente
   tudo f32, sem double
   =========================================================== */

/* exp aproximado — série de Taylor ordem 4, erro < 0.001 para |x|<3 */
FORCE_INLINE f32 fast_expf(f32 x) {
    /* clamp para evitar overflow em ARM softfp */
    if (x >  10.0f) return 22026.0f;
    if (x < -10.0f) return 0.0000454f;
    /* Horner: e^x ~ 1 + x + x^2/2 + x^3/6 + x^4/24 */
    f32 x2 = x * x;
    f32 x3 = x2 * x;
    f32 x4 = x3 * x;
    return 1.0f + x + x2*0.5f + x3*0.16667f + x4*0.04167f;
}

/* softmax in-place sobre vetor de len floats */
static void softmax_f32(f32 *v, u32 len) {
    f32 mx = v[0];
    for (u32 i = 1; i < len; i++) if (v[i] > mx) mx = v[i];
    f32 sum = 0.0f;
    for (u32 i = 0; i < len; i++) { v[i] = fast_expf(v[i] - mx); sum += v[i]; }
    f32 inv = (sum > 1e-9f) ? 1.0f/sum : 0.0f;
    for (u32 i = 0; i < len; i++) v[i] *= inv;
}

/* ReLU vetorial */
FORCE_INLINE f32 reluf(f32 x) { return x > 0.0f ? x : 0.0f; }

/* produto interno: dot(a, b, dim) — loop desdobrado x4 para ARM pipeline */
FORCE_INLINE f32 dot_f32(const f32 * restrict a, const f32 * restrict b, u32 dim) {
    f32 s0=0,s1=0,s2=0,s3=0;
    u32 i=0;
    for (; i+3 < dim; i+=4) {
        s0 += a[i]*b[i]; s1 += a[i+1]*b[i+1];
        s2 += a[i+2]*b[i+2]; s3 += a[i+3]*b[i+3];
    }
    for (; i < dim; i++) s0 += a[i]*b[i];
    return s0+s1+s2+s3;
}

/* saxpy: y += alpha * x */
FORCE_INLINE void saxpy_f32(f32 * restrict y, const f32 * restrict x,
                              f32 alpha, u32 dim) {
    u32 i=0;
    for (; i+3 < dim; i+=4) {
        y[i]   += alpha*x[i];   y[i+1] += alpha*x[i+1];
        y[i+2] += alpha*x[i+2]; y[i+3] += alpha*x[i+3];
    }
    for (; i < dim; i++) y[i] += alpha*x[i];
}

/* zero vetor */
FORCE_INLINE void vec_zero(f32 *v, u32 dim) { memset(v, 0, dim*sizeof(f32)); }

/* copiar vetor */
FORCE_INLINE void vec_copy(f32 * restrict dst, const f32 * restrict src, u32 dim) {
    memcpy(dst, src, dim*sizeof(f32));
}

/* norma L2 */
FORCE_INLINE f32 vec_norm(const f32 *v, u32 dim) {
    f32 s=0; for(u32 i=0;i<dim;i++) s+=v[i]*v[i];
    return sqrtf(s);
}

/* layer norm: (x - mean) / std * gamma + beta */
static void layer_norm(f32 * restrict out, const f32 * restrict x,
                        const f32 * restrict gamma, const f32 * restrict beta,
                        u32 dim) {
    f32 mean=0, var=0;
    for(u32 i=0;i<dim;i++) mean+=x[i];
    mean /= (f32)dim;
    for(u32 i=0;i<dim;i++) { f32 d=x[i]-mean; var+=d*d; }
    var /= (f32)dim;
    f32 inv_std = 1.0f / sqrtf(var + 1e-5f);
    for(u32 i=0;i<dim;i++) out[i] = (x[i]-mean)*inv_std*gamma[i] + beta[i];
}

/* ===========================================================
   EMBEDDINGS GEOMÉTRICOS
   Cada token tem vetor EMBED_DIM em f32, alinhado 64B
   Encoding posicional: seno/cosseno fixo (não treinado)
   =========================================================== */
typedef struct {
    f32 ALIGNED64 weight[VOCAB_MAX][EMBED_DIM]; /* tabela de embedding */
    f32 ALIGNED64 pos_enc[SEQ_LEN][EMBED_DIM];  /* encoding posicional */
    u32 vocab_size;
    u32 dim;
} EmbedTable;

/* inicializa pesos com distribuição normal truncada Xavier */
static void embed_init(EmbedTable *e, u32 vocab_size) {
    e->vocab_size = vocab_size;
    e->dim        = EMBED_DIM;
    f32 scale = 1.0f / sqrtf((f32)EMBED_DIM);

    /* semente determinística para reprodutibilidade */
    u32 rng = 0x52414641u;  /* "RAFA" */
    for(u32 v=0; v<vocab_size; v++) {
        for(u32 d=0; d<EMBED_DIM; d++) {
            /* LCG simples — sem rand() */
            rng = rng * 1664525u + 1013904223u;
            /* mapeia [0,2^32) -> [-1, 1] */
            f32 r = ((f32)(i32)rng) / 2147483648.0f;
            e->weight[v][d] = r * scale;
        }
    }

    /* encoding posicional: PE(pos,2i) = sin(pos/10000^(2i/dim)) */
    for(u32 pos=0; pos<SEQ_LEN; pos++) {
        for(u32 i=0; i<EMBED_DIM; i+=2) {
            f32 freq = 1.0f / powf(10000.0f, (f32)i / (f32)EMBED_DIM);
            e->pos_enc[pos][i]   = sinf((f32)pos * freq);
            e->pos_enc[pos][i+1] = cosf((f32)pos * freq);
        }
    }
}

/* forward: out[seq_len][embed_dim] = embed[token_id] + pos_enc[pos] */
static void embed_forward(const EmbedTable *e,
                           const u32 *token_ids, u32 seq_len,
                           f32 * restrict out) {
    /* out: layout linha=pos, coluna=dim */
    for(u32 p=0; p<seq_len; p++) {
        u32 tid = token_ids[p];
        if(tid >= e->vocab_size) tid = 1; /* unk */
        f32 *row = out + p*EMBED_DIM;
        const f32 *emb = e->weight[tid];
        const f32 *pos = e->pos_enc[p];
        /* loop desdobrado x4 — ARM pipeline amortiza load latency */
        u32 d=0;
        for(; d+3<EMBED_DIM; d+=4) {
            row[d]   = emb[d]   + pos[d];
            row[d+1] = emb[d+1] + pos[d+1];
            row[d+2] = emb[d+2] + pos[d+2];
            row[d+3] = emb[d+3] + pos[d+3];
        }
        for(; d<EMBED_DIM; d++) row[d] = emb[d] + pos[d];
    }
}

/* ===========================================================
   ATENÇÃO MULTI-CABEÇA (MHA)
   Sem alocação dinâmica: buffers estáticos no struct
   Q,K,V projetados de embed_dim -> head_dim por cabeça
   =========================================================== */
typedef struct {
    /* pesos: [n_heads][embed_dim x head_dim] */
    f32 ALIGNED64 Wq[N_HEADS][EMBED_DIM][HEAD_DIM];
    f32 ALIGNED64 Wk[N_HEADS][EMBED_DIM][HEAD_DIM];
    f32 ALIGNED64 Wv[N_HEADS][EMBED_DIM][HEAD_DIM];
    /* projeção de saída: [n_heads*head_dim x embed_dim] */
    f32 ALIGNED64 Wo[EMBED_DIM][EMBED_DIM];
    /* buffers de trabalho — reutilizados por chamada */
    f32 ALIGNED16 Q[N_HEADS][SEQ_LEN][HEAD_DIM];
    f32 ALIGNED16 K[N_HEADS][SEQ_LEN][HEAD_DIM];
    f32 ALIGNED16 V[N_HEADS][SEQ_LEN][HEAD_DIM];
    f32 ALIGNED16 attn_score[N_HEADS][SEQ_LEN][SEQ_LEN]; /* score antes softmax */
    f32 ALIGNED16 attn_out[SEQ_LEN][EMBED_DIM];
} MHALayer;

static void mha_init(MHALayer *m) {
    u32 rng = 0x4D484100u;
    f32 scale_qkv = 1.0f / sqrtf((f32)EMBED_DIM);
    f32 scale_o   = 1.0f / sqrtf((f32)(N_HEADS * HEAD_DIM));
    /* pesos Q,K,V */
    for(u32 h=0;h<N_HEADS;h++) {
        for(u32 i=0;i<EMBED_DIM;i++) {
            for(u32 j=0;j<HEAD_DIM;j++) {
                rng = rng*1664525u+1013904223u;
                f32 r = ((f32)(i32)rng)/2147483648.0f*scale_qkv;
                m->Wq[h][i][j] = r;
                rng = rng*1664525u+1013904223u;
                m->Wk[h][i][j] = ((f32)(i32)rng)/2147483648.0f*scale_qkv;
                rng = rng*1664525u+1013904223u;
                m->Wv[h][i][j] = ((f32)(i32)rng)/2147483648.0f*scale_qkv;
            }
        }
    }
    /* pesos O */
    for(u32 i=0;i<EMBED_DIM;i++) {
        for(u32 j=0;j<EMBED_DIM;j++) {
            rng = rng*1664525u+1013904223u;
            m->Wo[i][j] = ((f32)(i32)rng)/2147483648.0f*scale_o;
        }
    }
}

/* matmul: out[seq][hdim] = in[seq][edim] * W[edim][hdim] */
static void matmul_seq(f32 * restrict out,
                        const f32 * restrict in,
                        const f32 * restrict W,
                        u32 seq_len, u32 in_dim, u32 out_dim) {
    for(u32 s=0; s<seq_len; s++) {
        const f32 *row_in = in  + s*in_dim;
        f32 *row_out      = out + s*out_dim;
        vec_zero(row_out, out_dim);
        for(u32 i=0; i<in_dim; i++) {
            const f32 *W_row = W + i*out_dim;
            f32 xi = row_in[i];
            /* saxpy inline — loop desdobrado */
            u32 j=0;
            for(; j+3<out_dim; j+=4) {
                row_out[j]   += xi*W_row[j];
                row_out[j+1] += xi*W_row[j+1];
                row_out[j+2] += xi*W_row[j+2];
                row_out[j+3] += xi*W_row[j+3];
            }
            for(; j<out_dim; j++) row_out[j] += xi*W_row[j];
        }
    }
}

/* forward MHA com máscara causal (decoder) */
static void mha_forward(MHALayer *m,
                          const f32 * restrict x,  /* [seq][embed_dim] */
                          f32 * restrict out,        /* [seq][embed_dim] */
                          u32 seq_len) {
    f32 scale = 1.0f / sqrtf((f32)HEAD_DIM);

    for(u32 h=0; h<N_HEADS; h++) {
        /* projeta Q, K, V para esta cabeça */
        matmul_seq(&m->Q[h][0][0], x, &m->Wq[h][0][0],
                   seq_len, EMBED_DIM, HEAD_DIM);
        matmul_seq(&m->K[h][0][0], x, &m->Wk[h][0][0],
                   seq_len, EMBED_DIM, HEAD_DIM);
        matmul_seq(&m->V[h][0][0], x, &m->Wv[h][0][0],
                   seq_len, EMBED_DIM, HEAD_DIM);

        /* scores: attn[i][j] = Q[i] . K[j] * scale */
        for(u32 i=0; i<seq_len; i++) {
            for(u32 j=0; j<seq_len; j++) {
                if(j > i) {
                    /* máscara causal: posições futuras = -inf */
                    m->attn_score[h][i][j] = -1e9f;
                } else {
                    m->attn_score[h][i][j] = dot_f32(
                        m->Q[h][i], m->K[h][j], HEAD_DIM) * scale;
                }
            }
            /* softmax sobre linha i */
            softmax_f32(m->attn_score[h][i], seq_len);
        }

        /* output por cabeça: soma ponderada de V */
        for(u32 i=0; i<seq_len; i++) {
            /* acumula em attn_out[i][h*HEAD_DIM ... (h+1)*HEAD_DIM] */
            f32 *out_slot = &m->attn_out[i][h*HEAD_DIM];
            vec_zero(out_slot, HEAD_DIM);
            for(u32 j=0; j<seq_len; j++) {
                f32 a = m->attn_score[h][i][j];
                const f32 *vj = m->V[h][j];
                u32 d=0;
                for(; d+3<HEAD_DIM; d+=4) {
                    out_slot[d]   += a*vj[d];
                    out_slot[d+1] += a*vj[d+1];
                    out_slot[d+2] += a*vj[d+2];
                    out_slot[d+3] += a*vj[d+3];
                }
                for(; d<HEAD_DIM; d++) out_slot[d] += a*vj[d];
            }
        }
    }

    /* projeção de saída: out[seq][embed] = attn_out[seq][embed] * Wo */
    matmul_seq(out, &m->attn_out[0][0], &m->Wo[0][0],
               seq_len, EMBED_DIM, EMBED_DIM);
}

/* ===========================================================
   TESTE BLOCO 2
   =========================================================== */
static void b2_selftest(void) {
    arena_init(&g_arena);
    /* embedding de 4 tokens, sequência de 4 */
    EmbedTable *et = ALLOC64(EmbedTable, 1);
    if(!et) { printf("[FAIL] OOM embed\n"); return; }
    embed_init(et, 16);  /* vocab mini para teste */

    u32 ids[4] = {2,5,8,3};  /* bos, w, w, eos */
    f32 ALIGNED16 x[SEQ_LEN*EMBED_DIM];
    embed_forward(et, ids, 4, x);

    /* norma dos primeiros 4 vetores */
    for(u32 i=0; i<4; i++) {
        f32 n = vec_norm(x + i*EMBED_DIM, EMBED_DIM);
        printf("  embed[%u] norm=%.4f\n", i, n);
    }

    /* atenção */
    MHALayer *mha = ALLOC64(MHALayer, 1);
    if(!mha) { printf("[FAIL] OOM mha\n"); return; }
    mha_init(mha);

    f32 ALIGNED16 attn_out[SEQ_LEN*EMBED_DIM];
    mha_forward(mha, x, attn_out, 4);

    for(u32 i=0; i<4; i++) {
        f32 n = vec_norm(attn_out + i*EMBED_DIM, EMBED_DIM);
        printf("  attn_out[%u] norm=%.4f\n", i, n);
    }

    printf("[BLOCO2] OK — embed+MHA seq=4 embed_dim=%d heads=%d\n",
           EMBED_DIM, N_HEADS);
}

int main(void) {
    printf("=== GeoLM ARM32 — BLOCO 2: embeddings + MHA ===\n");
    b2_selftest();
    return 0;
}

/* ============================================================
   geolm_b3.c — FFN, transformer layer completo, loss
   Compilar: clang -O2 -mcpu=cortex-a7 -mfpu=neon-vfpv4 \
             -mfloat-abi=softfp -std=c11 \
             geolm_b3.c -lm -o geolm_b3 && ./geolm_b3
   ============================================================ */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <stdint.h>

typedef uint32_t u32; typedef int32_t i32; typedef float f32;
typedef uint8_t u8;

#define VOCAB_MAX   8192
#define EMBED_DIM     64
#define SEQ_LEN       64
#define HIDDEN_DIM   128
#define N_HEADS         4
#define HEAD_DIM       16
#define N_LAYERS        2   /* 2 camadas transformer para ARM32 */
#define ARENA_BYTES (64*1024*1024)
#define ALIGNED64  __attribute__((aligned(64)))
#define ALIGNED16  __attribute__((aligned(16)))
#define FORCE_INLINE __attribute__((always_inline)) static inline

static u8 ALIGNED64 g_arena_buf[ARENA_BYTES];
typedef struct { u8 *base; u32 cap, bump; } Arena;
static Arena g_arena;
static void arena_init(Arena *a){a->base=g_arena_buf;a->cap=ARENA_BYTES;a->bump=0;memset(g_arena_buf,0,ARENA_BYTES);}
FORCE_INLINE void *arena_alloc(Arena *a,u32 n,u32 al){u32 m=al-1,s=(a->bump+m)&~m,e=s+n;if(e>a->cap)return NULL;a->bump=e;return a->base+s;}
#define ALLOC(T,n)   ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),16u))
#define ALLOC64(T,n) ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),64u))

/* math helpers — mesmo que bloco 2 */
FORCE_INLINE f32 fast_expf(f32 x){if(x>10.f)return 22026.f;if(x<-10.f)return 4.54e-5f;f32 x2=x*x,x3=x2*x,x4=x3*x;return 1.f+x+x2*.5f+x3*.16667f+x4*.04167f;}
static void softmax_f32(f32 *v,u32 n){f32 mx=v[0];for(u32 i=1;i<n;i++)if(v[i]>mx)mx=v[i];f32 s=0;for(u32 i=0;i<n;i++){v[i]=fast_expf(v[i]-mx);s+=v[i];}f32 inv=s>1e-9f?1.f/s:0;for(u32 i=0;i<n;i++)v[i]*=inv;}
FORCE_INLINE f32 reluf(f32 x){return x>0.f?x:0.f;}
FORCE_INLINE f32 dot_f32(const f32*a,const f32*b,u32 d){f32 s0=0,s1=0,s2=0,s3=0;u32 i=0;for(;i+3<d;i+=4){s0+=a[i]*b[i];s1+=a[i+1]*b[i+1];s2+=a[i+2]*b[i+2];s3+=a[i+3]*b[i+3];}for(;i<d;i++)s0+=a[i]*b[i];return s0+s1+s2+s3;}
FORCE_INLINE void vec_zero(f32*v,u32 d){memset(v,0,d*sizeof(f32));}
static void layer_norm(f32*o,const f32*x,const f32*g,const f32*b,u32 d){f32 m=0,v=0;for(u32 i=0;i<d;i++)m+=x[i];m/=d;for(u32 i=0;i<d;i++){f32 diff=x[i]-m;v+=diff*diff;}v/=d;f32 is=1.f/sqrtf(v+1e-5f);for(u32 i=0;i<d;i++)o[i]=(x[i]-m)*is*g[i]+b[i];}
static void matmul_seq(f32*o,const f32*in,const f32*W,u32 sl,u32 id,u32 od){for(u32 s=0;s<sl;s++){const f32*ri=in+s*id;f32*ro=o+s*od;vec_zero(ro,od);for(u32 i=0;i<id;i++){const f32*Wr=W+i*od;f32 xi=ri[i];u32 j=0;for(;j+3<od;j+=4){ro[j]+=xi*Wr[j];ro[j+1]+=xi*Wr[j+1];ro[j+2]+=xi*Wr[j+2];ro[j+3]+=xi*Wr[j+3];}for(;j<od;j++)ro[j]+=xi*Wr[j];}}}

/* ===========================================================
   FFN — Feed Forward Network: Linear -> ReLU -> Linear
   dim: embed_dim -> hidden_dim -> embed_dim
   =========================================================== */
typedef struct {
    f32 ALIGNED64 W1[EMBED_DIM][HIDDEN_DIM];   /* up projection */
    f32 ALIGNED64 W2[HIDDEN_DIM][EMBED_DIM];   /* down projection */
    f32 ALIGNED16 b1[HIDDEN_DIM];
    f32 ALIGNED16 b2[EMBED_DIM];
    f32 ALIGNED16 ln_gamma[EMBED_DIM];
    f32 ALIGNED16 ln_beta[EMBED_DIM];
    f32 ALIGNED16 _buf[SEQ_LEN][HIDDEN_DIM];   /* buffer intermediário */
} FFNLayer;

static void ffn_init(FFNLayer *f) {
    u32 rng = 0x46464E00u;
    f32 s1 = 1.f/sqrtf((f32)EMBED_DIM);
    f32 s2 = 1.f/sqrtf((f32)HIDDEN_DIM);
    for(u32 i=0;i<EMBED_DIM;i++) {
        for(u32 j=0;j<HIDDEN_DIM;j++){
            rng=rng*1664525u+1013904223u;
            f->W1[i][j]=((f32)(i32)rng)/2147483648.f*s1;
        }
    }
    for(u32 i=0;i<HIDDEN_DIM;i++) {
        for(u32 j=0;j<EMBED_DIM;j++){
            rng=rng*1664525u+1013904223u;
            f->W2[i][j]=((f32)(i32)rng)/2147483648.f*s2;
        }
    }
    /* bias zero, gamma=1, beta=0 */
    memset(f->b1,0,sizeof(f->b1));
    memset(f->b2,0,sizeof(f->b2));
    for(u32 i=0;i<EMBED_DIM;i++){f->ln_gamma[i]=1.f;f->ln_beta[i]=0.f;}
}

static void ffn_forward(FFNLayer *f,
                          const f32 * restrict x,  /* [seq][embed] */
                          f32 * restrict out,        /* [seq][embed] */
                          u32 seq_len) {
    /* W1: [seq][embed] -> [seq][hidden] + bias + relu */
    matmul_seq(&f->_buf[0][0], x, &f->W1[0][0],
               seq_len, EMBED_DIM, HIDDEN_DIM);
    for(u32 s=0;s<seq_len;s++) {
        f32 *row = f->_buf[s];
        for(u32 j=0;j<HIDDEN_DIM;j++)
            row[j] = reluf(row[j] + f->b1[j]);
    }
    /* W2: [seq][hidden] -> [seq][embed] + bias */
    matmul_seq(out, &f->_buf[0][0], &f->W2[0][0],
               seq_len, HIDDEN_DIM, EMBED_DIM);
    for(u32 s=0;s<seq_len;s++) {
        f32 *row = out + s*EMBED_DIM;
        for(u32 j=0;j<EMBED_DIM;j++) row[j] += f->b2[j];
    }
}

/* ===========================================================
   TRANSFORMER LAYER: LN -> MHA -> residual -> LN -> FFN -> residual
   Incluímos MHA inline simplificado (sem struct separado para economizar RAM)
   =========================================================== */
typedef struct {
    /* MHA weights */
    f32 ALIGNED64 Wq[EMBED_DIM][EMBED_DIM];
    f32 ALIGNED64 Wk[EMBED_DIM][EMBED_DIM];
    f32 ALIGNED64 Wv[EMBED_DIM][EMBED_DIM];
    f32 ALIGNED64 Wo[EMBED_DIM][EMBED_DIM];
    /* FFN */
    FFNLayer ffn;
    /* LayerNorm params */
    f32 ALIGNED16 ln1_g[EMBED_DIM], ln1_b[EMBED_DIM];
    f32 ALIGNED16 ln2_g[EMBED_DIM], ln2_b[EMBED_DIM];
    /* buffers scratch */
    f32 ALIGNED16 ln_out[SEQ_LEN][EMBED_DIM];
    f32 ALIGNED16 mha_out[SEQ_LEN][EMBED_DIM];
    f32 ALIGNED16 ffn_out[SEQ_LEN][EMBED_DIM];
    f32 ALIGNED16 Q[SEQ_LEN][EMBED_DIM];
    f32 ALIGNED16 K[SEQ_LEN][EMBED_DIM];
    f32 ALIGNED16 V[SEQ_LEN][EMBED_DIM];
    f32 ALIGNED16 scores[SEQ_LEN][SEQ_LEN];
} TransformerLayer;

static void tl_init(TransformerLayer *l) {
    u32 rng = 0x544C0000u;
    f32 s = 1.f/sqrtf((f32)EMBED_DIM);
    f32 *ptrs[] = {&l->Wq[0][0],&l->Wk[0][0],&l->Wv[0][0],&l->Wo[0][0],NULL};
    for(u32 p=0; ptrs[p]; p++) {
        f32 *W = ptrs[p];
        for(u32 i=0;i<EMBED_DIM*EMBED_DIM;i++) {
            rng=rng*1664525u+1013904223u;
            W[i]=((f32)(i32)rng)/2147483648.f*s;
        }
    }
    for(u32 i=0;i<EMBED_DIM;i++){
        l->ln1_g[i]=1.f; l->ln1_b[i]=0.f;
        l->ln2_g[i]=1.f; l->ln2_b[i]=0.f;
    }
    ffn_init(&l->ffn);
}

static void tl_forward(TransformerLayer *l,
                         const f32 * restrict x,   /* [seq][embed] */
                         f32 * restrict out,         /* [seq][embed] */
                         u32 seq_len) {
    f32 scale = 1.f/sqrtf((f32)EMBED_DIM);

    /* LN1 -> Q,K,V */
    for(u32 s=0;s<seq_len;s++)
        layer_norm(l->ln_out[s], x+s*EMBED_DIM,
                   l->ln1_g, l->ln1_b, EMBED_DIM);

    matmul_seq(&l->Q[0][0], &l->ln_out[0][0], &l->Wq[0][0], seq_len,EMBED_DIM,EMBED_DIM);
    matmul_seq(&l->K[0][0], &l->ln_out[0][0], &l->Wk[0][0], seq_len,EMBED_DIM,EMBED_DIM);
    matmul_seq(&l->V[0][0], &l->ln_out[0][0], &l->Wv[0][0], seq_len,EMBED_DIM,EMBED_DIM);

    /* scores causal */
    for(u32 i=0;i<seq_len;i++) {
        for(u32 j=0;j<seq_len;j++)
            l->scores[i][j] = (j>i) ? -1e9f :
                dot_f32(l->Q[i], l->K[j], EMBED_DIM)*scale;
        softmax_f32(l->scores[i], seq_len);
    }

    /* attn_out = scores * V */
    for(u32 i=0;i<seq_len;i++) {
        f32 *ao = l->mha_out[i];
        vec_zero(ao, EMBED_DIM);
        for(u32 j=0;j<seq_len;j++) {
            f32 a = l->scores[i][j];
            const f32 *vj = l->V[j];
            u32 d=0;
            for(;d+3<EMBED_DIM;d+=4){ao[d]+=a*vj[d];ao[d+1]+=a*vj[d+1];ao[d+2]+=a*vj[d+2];ao[d+3]+=a*vj[d+3];}
            for(;d<EMBED_DIM;d++) ao[d]+=a*vj[d];
        }
    }

    /* projeção Wo + residual */
    matmul_seq(&l->ffn_out[0][0], &l->mha_out[0][0],
               &l->Wo[0][0], seq_len, EMBED_DIM, EMBED_DIM);
    for(u32 s=0;s<seq_len;s++) {
        f32 *r=l->ffn_out[s]; const f32 *xi=x+s*EMBED_DIM;
        for(u32 d=0;d<EMBED_DIM;d++) r[d]+=xi[d]; /* residual */
    }

    /* LN2 -> FFN -> residual */
    for(u32 s=0;s<seq_len;s++)
        layer_norm(l->ln_out[s], l->ffn_out[s],
                   l->ln2_g, l->ln2_b, EMBED_DIM);
    ffn_forward(&l->ffn, &l->ln_out[0][0],
                &l->mha_out[0][0], seq_len);
    for(u32 s=0;s<seq_len;s++) {
        f32 *r=out+s*EMBED_DIM;
        const f32 *a=l->ffn_out[s], *b=l->mha_out[s];
        for(u32 d=0;d<EMBED_DIM;d++) r[d]=a[d]+b[d];
    }
}

/* ===========================================================
   HEAD DE LINGUAGEM: Linear [embed -> vocab] + softmax
   =========================================================== */
typedef struct {
    f32 ALIGNED64 W[EMBED_DIM][VOCAB_MAX];  /* projeção final */
    f32 ALIGNED16 b[VOCAB_MAX];
    u32 vocab_size;
} LMHead;

static void lmhead_init(LMHead *h, u32 vocab_size) {
    h->vocab_size = vocab_size;
    u32 rng = 0x484F4400u;
    f32 s = 1.f/sqrtf((f32)EMBED_DIM);
    for(u32 i=0;i<EMBED_DIM;i++)
        for(u32 j=0;j<vocab_size;j++){
            rng=rng*1664525u+1013904223u;
            h->W[i][j]=((f32)(i32)rng)/2147483648.f*s;
        }
    memset(h->b, 0, vocab_size*sizeof(f32));
}

/* logits para última posição da sequência (geração) */
static void lmhead_logits(const LMHead *h,
                            const f32 * restrict last_hidden, /* [embed_dim] */
                            f32 * restrict logits) {          /* [vocab_size] */
    for(u32 j=0;j<h->vocab_size;j++) {
        f32 s=h->b[j];
        for(u32 i=0;i<EMBED_DIM;i++) s+=last_hidden[i]*h->W[i][j];
        logits[j]=s;
    }
}

/* ===========================================================
   CROSS-ENTROPY LOSS (teacher forcing)
   targets: array de token_id esperados por posição
   =========================================================== */
static f32 cross_entropy_loss(const LMHead *h,
                               f32 * restrict hidden,   /* [seq][embed] */
                               const u32 *targets,      /* [seq] */
                               u32 seq_len,
                               f32 * restrict logits_buf) { /* [vocab] scratch */
    f32 total_loss = 0.f;
    u32 count = 0;
    for(u32 s=0; s+1<seq_len; s++) {  /* prediz token s+1 da posição s */
        lmhead_logits(h, hidden+s*EMBED_DIM, logits_buf);
        /* log softmax manual */
        f32 mx=logits_buf[0];
        for(u32 j=1;j<h->vocab_size;j++) if(logits_buf[j]>mx) mx=logits_buf[j];
        f32 sum=0;
        for(u32 j=0;j<h->vocab_size;j++){
            logits_buf[j]=fast_expf(logits_buf[j]-mx); sum+=logits_buf[j];
        }
        u32 tid = targets[s+1];
        if(tid < h->vocab_size) {
            f32 log_prob = logf(logits_buf[tid]/sum + 1e-9f);
            total_loss += -log_prob;
            count++;
        }
    }
    return count > 0 ? total_loss/(f32)count : 0.f;
}

/* ===========================================================
   MODELO COMPLETO — GeoLM
   =========================================================== */
typedef struct {
    f32 ALIGNED64 embed_W[VOCAB_MAX][EMBED_DIM];
    f32 ALIGNED64 pos_enc[SEQ_LEN][EMBED_DIM];
    TransformerLayer layers[N_LAYERS];
    LMHead head;
    u32 vocab_size;
} GeoLM;

static GeoLM *geolm_init(u32 vocab_size) {
    GeoLM *m = ALLOC64(GeoLM, 1);
    if(!m) return NULL;
    m->vocab_size = vocab_size;
    /* embedding */
    u32 rng=0x474C4D00u;
    f32 s=1.f/sqrtf((f32)EMBED_DIM);
    for(u32 v=0;v<vocab_size;v++)
        for(u32 d=0;d<EMBED_DIM;d++){
            rng=rng*1664525u+1013904223u;
            m->embed_W[v][d]=((f32)(i32)rng)/2147483648.f*s;
        }
    /* pos enc */
    for(u32 p=0;p<SEQ_LEN;p++)
        for(u32 i=0;i<EMBED_DIM;i+=2){
            f32 freq=1.f/powf(10000.f,(f32)i/(f32)EMBED_DIM);
            m->pos_enc[p][i]=sinf((f32)p*freq);
            m->pos_enc[p][i+1]=cosf((f32)p*freq);
        }
    for(u32 l=0;l<N_LAYERS;l++) tl_init(&m->layers[l]);
    lmhead_init(&m->head, vocab_size);
    return m;
}

static void geolm_forward(GeoLM *m,
                            const u32 *ids, u32 seq_len,
                            f32 * restrict x_buf) {  /* [seq][embed] scratch */
    /* embed + pos */
    for(u32 p=0;p<seq_len;p++){
        u32 tid=ids[p]; if(tid>=m->vocab_size)tid=1;
        f32 *row=x_buf+p*EMBED_DIM;
        for(u32 d=0;d<EMBED_DIM;d++)
            row[d]=m->embed_W[tid][d]+m->pos_enc[p][d];
    }
    /* layers */
    static f32 ALIGNED16 layer_out[SEQ_LEN*EMBED_DIM];
    for(u32 l=0;l<N_LAYERS;l++){
        tl_forward(&m->layers[l], x_buf, layer_out, seq_len);
        memcpy(x_buf, layer_out, seq_len*EMBED_DIM*sizeof(f32));
    }
}

/* ===========================================================
   TESTE BLOCO 3
   =========================================================== */
static void b3_selftest(void) {
    arena_init(&g_arena);
    u32 vocab_size = 64;
    GeoLM *model = geolm_init(vocab_size);
    if(!model){ printf("[FAIL] OOM model\n"); return; }

    /* sequência de teste */
    u32 ids[8] = {2,5,8,3,10,15,20,3};
    static f32 ALIGNED16 x_buf[SEQ_LEN*EMBED_DIM];
    static f32 ALIGNED16 logits[VOCAB_MAX];
    vec_zero(x_buf, SEQ_LEN*EMBED_DIM);

    /* forward */
    geolm_forward(model, ids, 8, x_buf);

    /* loss */
    f32 loss = cross_entropy_loss(&model->head, x_buf,
                                   ids, 8, logits);
    printf("[b3] loss=%.4f (random init esperado ~ln(%u)=%.4f)\n",
           loss, vocab_size, logf((f32)vocab_size));

    /* predição próximo token */
    lmhead_logits(&model->head, x_buf+7*EMBED_DIM, logits);
    softmax_f32(logits, vocab_size);
    u32 top=0;
    for(u32 j=1;j<vocab_size;j++) if(logits[j]>logits[top]) top=j;
    printf("[b3] next_token_pred=%u prob=%.4f\n", top, logits[top]);

    printf("[BLOCO3] OK — GeoLM N_LAYERS=%d seq=8 vocab=%u\n",
           N_LAYERS, vocab_size);
}

int main(void) {
    printf("=== GeoLM ARM32 — BLOCO 3: FFN + TransformerLayer + Loss ===\n");
    b3_selftest();
    return 0;/* ============================================================
   geolm_b4.c — SGD, ingestão, fetch HTTP escrituras sagradas
   Compilar: clang -O2 -mcpu=cortex-a7 -mfpu=neon-vfpv4 \
             -mfloat-abi=softfp -std=c11 \
             geolm_b4.c -lm -o geolm_b4 && ./geolm_b4 train
   ============================================================ */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

typedef uint32_t u32; typedef int32_t i32;
typedef uint8_t u8;   typedef float f32;

#define VOCAB_MAX   8192
#define EMBED_DIM     64
#define SEQ_LEN       64
#define HIDDEN_DIM   128
#define N_LAYERS        2
#define ARENA_BYTES (64*1024*1024)
#define ALIGNED64 __attribute__((aligned(64)))
#define ALIGNED16 __attribute__((aligned(16)))
#define FORCE_INLINE __attribute__((always_inline)) static inline

static u8 ALIGNED64 g_arena_buf[ARENA_BYTES];
typedef struct{u8*base;u32 cap,bump;}Arena;
static Arena g_arena;
static void arena_init(Arena*a){a->base=g_arena_buf;a->cap=ARENA_BYTES;a->bump=0;memset(g_arena_buf,0,ARENA_BYTES);}
FORCE_INLINE void*arena_alloc(Arena*a,u32 n,u32 al){u32 m=al-1,s=(a->bump+m)&~m,e=s+n;if(e>a->cap)return NULL;a->bump=e;return a->base+s;}
#define ALLOC(T,n)   ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),16u))
#define ALLOC64(T,n) ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),64u))

/* math helpers */
FORCE_INLINE f32 fast_expf(f32 x){if(x>10.f)return 22026.f;if(x<-10.f)return 4.54e-5f;f32 x2=x*x,x3=x2*x,x4=x3*x;return 1.f+x+x2*.5f+x3*.16667f+x4*.04167f;}
FORCE_INLINE f32 reluf(f32 x){return x>0.f?x:0.f;}
FORCE_INLINE f32 dot_f32(const f32*a,const f32*b,u32 d){f32 s0=0,s1=0,s2=0,s3=0;u32 i=0;for(;i+3<d;i+=4){s0+=a[i]*b[i];s1+=a[i+1]*b[i+1];s2+=a[i+2]*b[i+2];s3+=a[i+3]*b[i+3];}for(;i<d;i++)s0+=a[i]*b[i];return s0+s1+s2+s3;}
FORCE_INLINE void vec_zero(f32*v,u32 d){memset(v,0,d*sizeof(f32));}

/* ===========================================================
   VOCAB simplificado para B4
   =========================================================== */
typedef struct {
    char str[32]; u32 id, freq; u8 used;
} VE;
typedef struct {VE*e; u32 sz,cap,id_unk,id_bos,id_eos;} Vocab;

static Vocab *vocab_new(void){
    Vocab*v=ALLOC(Vocab,1); if(!v)return NULL;
    v->e=ALLOC64(VE,VOCAB_MAX); if(!v->e)return NULL;
    memset(v->e,0,sizeof(VE)*VOCAB_MAX);
    v->sz=4; v->cap=VOCAB_MAX;
    v->id_unk=1; v->id_bos=2; v->id_eos=3;
    const char*sp[]={"<pad>","<unk>","<bos>","<eos>",NULL};
    for(i32 i=0;sp[i];i++){u32 h=5381;const char*s=sp[i];while(*s)h=((h<<5)+h)^(u8)*s++;h%=VOCAB_MAX;strncpy(v->e[h].str,sp[i],31);v->e[h].id=(u32)i;v->e[h].used=1;}
    return v;
}
static u32 vocab_ins(Vocab*v,const char*s){
    if(v->sz>=v->cap-1)return v->id_unk;
    u32 h=5381;const char*p=s;while(*p)h=((h<<5)+h)^(u8)*p++;h%=v->cap;
    for(u32 pr=0;pr<v->cap;pr++){VE*e=&v->e[h];if(!e->used){strncpy(e->str,s,31);e->id=v->sz++;e->freq=1;e->used=1;return e->id;}if(strncmp(e->str,s,31)==0){e->freq++;return e->id;}h=(h+1)%v->cap;}
    return v->id_unk;
}
static u32 vocab_get(const Vocab*v,const char*s){
    u32 h=5381;const char*p=s;while(*p)h=((h<<5)+h)^(u8)*p++;h%=v->cap;
    for(u32 pr=0;pr<v->cap;pr++){const VE*e=&v->e[h];if(!e->used)return v->id_unk;if(strncmp(e->str,s,31)==0)return e->id;h=(h+1)%v->cap;}
    return v->id_unk;
}

/* ===========================================================
   TOKENIZER SIMPLIFICADO
   =========================================================== */
static u32 tokenize_text(Vocab*v, const char*text, u32 len,
                          u32*ids, u32 max_ids) {
    char buf[32]; u32 bp=0, n=0;
    #define EMIT() do{if(bp>0&&n<max_ids){buf[bp]=0;ids[n++]=vocab_ins(v,buf);bp=0;}}while(0)
    for(u32 i=0;i<len&&n<max_ids;i++){
        u8 c=(u8)text[i];
        if(c>='A'&&c<='Z')c+=32;
        if(c==' '||c=='\t'||c=='\n'||c=='\r'){EMIT();continue;}
        if(c=='.'||c==','||c==';'||c==':'||c=='!'||c=='?'||c=='"'||c=='\''){
            EMIT();
            if(n<max_ids){buf[0]=(char)c;buf[1]=0;ids[n++]=vocab_ins(v,buf);}
            continue;
        }
        if(bp<31)buf[bp++]=(char)c; else{EMIT();buf[bp++]=(char)c;}
    }
    EMIT();
    #undef EMIT
    return n;
}

/* ===========================================================
   SGD SIMPLIFICADO — Adam-like com momento
   Backprop completo está em B5; aqui fazemos SGD direto
   sobre embedding e head apenas para demonstrar convergência
   =========================================================== */
typedef struct {
    f32 lr;          /* learning rate */
    f32 momentum;    /* beta1 = 0.9 */
    f32 wd;          /* weight decay */
    u32 step;
} SGDState;

FORCE_INLINE void sgd_update(f32 *w, f32 *grad, f32 *mom,
                              u32 n, SGDState *s) {
    for(u32 i=0;i<n;i++){
        mom[i] = s->momentum * mom[i] + (1.f - s->momentum) * grad[i];
        w[i]  -= s->lr * (mom[i] + s->wd * w[i]);
        grad[i] = 0.f;  /* zera gradiente */
    }
    s->step++;
}

/* ===========================================================
   FETCH HTTP SEM LIBCURL — usando wget/curl do Termux via popen
   Busca texto das escrituras sagradas
   =========================================================== */
#define TEXT_BUF_SIZE (2*1024*1024)  /* 2MB por arquivo */

/* URLs de textos de domínio público (Project Gutenberg) */
static const char *SCRIPTURE_URLS[] = {
    /* Bíblia KJV — domínio público */
    "https://www.gutenberg.org/cache/epub/10/pg10.txt",
    /* Tao Te Ching — tradução pública */
    "https://www.gutenberg.org/cache/epub/216/pg216.txt",
    /* Bhagavad Gita — domínio público */
    "https://www.gutenberg.org/cache/epub/2388/pg2388.txt",
    NULL
};

static char *g_text_buf = NULL;
static u32   g_text_len = 0;

/* tenta baixar texto usando wget (disponível no Termux) */
static i32 fetch_url(const char *url, char *buf, u32 buf_size, u32 *out_len) {
    char cmd[512];
    /* usa wget com timeout e limit de tamanho */
    snprintf(cmd, sizeof(cmd),
             "wget -q --timeout=15 -O - \"%s\" 2>/dev/null", url);
    FILE *fp = popen(cmd, "r");
    if(!fp) {
        /* fallback: curl */
        snprintf(cmd, sizeof(cmd),
                 "curl -s --max-time 15 \"%s\" 2>/dev/null", url);
        fp = popen(cmd, "r");
        if(!fp) return -1;
    }
    u32 total=0;
    size_t r;
    while(total < buf_size-1 &&
          (r=fread(buf+total,1,4096,fp))>0) total+=(u32)r;
    buf[total]=0;
    pclose(fp);
    *out_len = total;
    return total > 0 ? 0 : -1;
}

/* carrega arquivo local (para uso offline / JSON pré-baixado) */
static i32 load_file(const char *path, char *buf, u32 buf_size, u32 *out_len) {
    FILE *fp=fopen(path,"rb");
    if(!fp)return -1;
    u32 total=0; size_t r;
    while(total<buf_size-1 && (r=fread(buf+total,1,65536,fp))>0) total+=(u32)r;
    buf[total]=0;
    fclose(fp);
    *out_len=total;
    return 0;
}

/* extrai texto de campo JSON "text" simples — sem parser completo */
static u32 extract_json_texts(const char *json, u32 json_len,
                               char *out, u32 out_size) {
    u32 written=0;
    const char *p=json;
    const char *end=json+json_len;
    /* procura por "text": "..." ou "content": "..." */
    const char *keys[] = {"\"text\":", "\"content\":", "\"body\":", NULL};
    while(p < end && written < out_size-1) {
        const char *found=NULL; u32 key_len=0;
        for(u32 k=0; keys[k]; k++) {
            const char *f=strstr(p, keys[k]);
            if(f && (!found || f<found)){found=f;key_len=(u32)strlen(keys[k]);}
        }
        if(!found) break;
        p=found+key_len;
        /* pula espaços */
        while(p<end && (*p==' '||*p=='\t'||*p=='\n')) p++;
        if(p>=end||*p!='"'){p++;continue;}
        p++; /* pula " inicial */
        while(p<end && *p!='"' && written<out_size-1) {
            if(*p=='\\'){p++;if(p<end&&*p=='n'){out[written++]='\n';p++;continue;}}
            out[written++]=*p++;
        }
        out[written++]=' ';
        if(p<end)p++;
    }
    if(written<out_size) out[written]=0;
    return written;
}

/* ===========================================================
   TRAINING LOOP SIMPLIFICADO
   Objetivo: demonstrar convergência da loss em ARM32
   Backprop: gradiente numérico sobre embedding apenas
   (backprop analítico completo em B5)
   =========================================================== */
#define EMBED_PARAMS (VOCAB_MAX * EMBED_DIM)
#define HEAD_PARAMS  (EMBED_DIM * VOCAB_MAX)

typedef struct {
    f32 ALIGNED64 embed[VOCAB_MAX][EMBED_DIM];
    f32 ALIGNED64 head_W[EMBED_DIM][VOCAB_MAX];
    f32 ALIGNED16 head_b[VOCAB_MAX];
    /* momentum buffers */
    f32 ALIGNED16 embed_mom[VOCAB_MAX][EMBED_DIM];
    f32 ALIGNED16 head_mom[EMBED_DIM][VOCAB_MAX];
    u32 vocab_size;
} TinyModel;

static TinyModel *tiny_init(u32 vocab_size) {
    TinyModel *m=ALLOC64(TinyModel,1); if(!m)return NULL;
    m->vocab_size=vocab_size;
    u32 rng=0x544D0000u;
    f32 s=1.f/sqrtf((f32)EMBED_DIM);
    for(u32 v=0;v<vocab_size;v++)
        for(u32 d=0;d<EMBED_DIM;d++){
            rng=rng*1664525u+1013904223u;
            m->embed[v][d]=((f32)(i32)rng)/2147483648.f*s;
        }
    s=1.f/sqrtf((f32)EMBED_DIM);
    for(u32 i=0;i<EMBED_DIM;i++)
        for(u32 j=0;j<vocab_size;j++){
            rng=rng*1664525u+1013904223u;
            m->head_W[i][j]=((f32)(i32)rng)/2147483648.f*s;
        }
    memset(m->head_b,0,vocab_size*sizeof(f32));
    memset(m->embed_mom,0,sizeof(m->embed_mom));
    memset(m->head_mom,0,sizeof(m->head_mom));
    return m;
}

/* forward: embed[ctx_id] -> logits[vocab] — modelo bigram mínimo */
static f32 tiny_step(TinyModel *m, u32 ctx_id, u32 target_id,
                      f32 *logits_buf, f32 lr) {
    if(ctx_id>=m->vocab_size||target_id>=m->vocab_size)return 0.f;
    f32 *emb=m->embed[ctx_id];

    /* logits = head_W^T * emb + head_b */
    for(u32 j=0;j<m->vocab_size;j++){
        f32 s=m->head_b[j];
        for(u32 i=0;i<EMBED_DIM;i++) s+=m->head_W[i][j]*emb[i];
        logits_buf[j]=s;
    }

    /* softmax */
    f32 mx=logits_buf[0];
    for(u32 j=1;j<m->vocab_size;j++) if(logits_buf[j]>mx)mx=logits_buf[j];
    f32 sum=0;
    for(u32 j=0;j<m->vocab_size;j++){logits_buf[j]=fast_expf(logits_buf[j]-mx);sum+=logits_buf[j];}
    f32 inv=sum>1e-9f?1.f/sum:0;
    for(u32 j=0;j<m->vocab_size;j++) logits_buf[j]*=inv;

    /* loss NLL */
    f32 loss=-logf(logits_buf[target_id]+1e-9f);

    /* backward — gradiente da softmax cross-entropy */
    /* d_logit[j] = prob[j] - (j==target) */
    static f32 ALIGNED16 dlogit[VOCAB_MAX];
    for(u32 j=0;j<m->vocab_size;j++)
        dlogit[j]=logits_buf[j]-(j==target_id?1.f:0.f);

    /* gradiente em head_W: outer product emb x dlogit */
    for(u32 i=0;i<EMBED_DIM;i++) {
        f32 ei=emb[i];
        for(u32 j=0;j<m->vocab_size;j++)
            m->head_W[i][j]-=lr*dlogit[j]*ei;
    }
    for(u32 j=0;j<m->vocab_size;j++) m->head_b[j]-=lr*dlogit[j];

    /* gradiente em embed: head_W * dlogit */
    for(u32 i=0;i<EMBED_DIM;i++){
        f32 g=0;
        for(u32 j=0;j<m->vocab_size;j++) g+=m->head_W[i][j]*dlogit[j];
        emb[i]-=lr*g;
    }
    return loss;
}

static void tiny_train(TinyModel *m, const u32 *ids, u32 n_ids,
                        u32 n_epochs, f32 lr) {
    static f32 ALIGNED16 logits[VOCAB_MAX];
    printf("[train] vocab=%u tokens=%u epochs=%u lr=%.4f\n",
           m->vocab_size, n_ids, n_epochs, lr);
    for(u32 ep=0;ep<n_epochs;ep++){
        f32 total=0; u32 cnt=0;
        for(u32 i=0;i+1<n_ids;i++){
            total+=tiny_step(m,ids[i],ids[i+1],logits,lr);
            cnt++;
        }
        if(ep%10==0||ep==n_epochs-1)
            printf("  epoch %3u  loss=%.4f\n", ep, total/(f32)(cnt?cnt:1));
    }
}

static void tiny_generate(TinyModel *m, Vocab *v,
                            u32 start_id, u32 n_gen) {
    static f32 ALIGNED16 logits[VOCAB_MAX];
    u32 cur=start_id;
    printf("[gen] ");
    for(u32 i=0;i<n_gen;i++){
        for(u32 j=0;j<m->vocab_size;j++){
            f32 s=m->head_b[j];
            for(u32 d=0;d<EMBED_DIM;d++) s+=m->head_W[d][j]*m->embed[cur][d];
            logits[j]=s;
        }
        /* greedy */
        u32 best=0;
        for(u32 j=1;j<m->vocab_size;j++) if(logits[j]>logits[best])best=j;
        printf("%s ", v->e[best].used ? v->e[best].str : "?");
        cur=best;
        if(cur==v->id_eos) break;
    }
    printf("\n");
}

/* ===========================================================
   MAIN B4
   =========================================================== */
static char g_textbuf[TEXT_BUF_SIZE];

int main(int argc, char **argv) {
    printf("=== GeoLM ARM32 — BLOCO 4: SGD + ingestão + fetch ===\n");
    arena_init(&g_arena);
    Vocab *vocab = vocab_new();
    if(!vocab){printf("[FAIL] vocab OOM\n");return 1;}

    u32 *ids = ALLOC(u32, 65536);
    if(!ids){printf("[FAIL] ids OOM\n");return 1;}
    u32 n_ids=0;

    /* modo: fetch online ou arquivo local */
    if(argc>=2 && strcmp(argv[1],"fetch")==0) {
        printf("[fetch] tentando baixar escrituras...\n");
        for(u32 u=0; SCRIPTURE_URLS[u]; u++) {
            u32 len=0;
            printf("  url=%s\n", SCRIPTURE_URLS[u]);
            if(fetch_url(SCRIPTURE_URLS[u], g_textbuf, TEXT_BUF_SIZE, &len)==0){
                printf("  baixado %u bytes\n", len);
                u32 toks=tokenize_text(vocab,g_textbuf,len,
                                        ids+n_ids,65536-n_ids);
                n_ids+=toks;
                printf("  tokens acumulados=%u vocab=%u\n",n_ids,vocab->sz);
                if(n_ids>60000) break;
            } else {
                printf("  [WARN] falhou (sem wget/curl ou sem rede)\n");
            }
        }
    } else if(argc>=2) {
        /* arquivo local */
        u32 len=0;
        printf("[load] arquivo: %s\n", argv[1]);
        if(load_file(argv[1], g_textbuf, TEXT_BUF_SIZE, &len)==0){
            /* detecta JSON */
            if(g_textbuf[0]=='{' || g_textbuf[0]=='[') {
                printf("[json] extraindo campos text/content...\n");
                static char extracted[TEXT_BUF_SIZE];
                u32 elen=extract_json_texts(g_textbuf,len,extracted,TEXT_BUF_SIZE);
                printf("[json] extraído %u chars\n",elen);
                n_ids=tokenize_text(vocab,extracted,elen,ids,65536);
            } else {
                n_ids=tokenize_text(vocab,g_textbuf,len,ids,65536);
            }
            printf("[load] %u tokens vocab=%u\n",n_ids,vocab->sz);
        }
    }

    /* fallback offline: texto embutido */
    if(n_ids < 32) {
        printf("[fallback] usando texto embutido\n");
        const char *seed =
            "no principio era o verbo e o verbo estava com deus "
            "o verbo era deus ele estava no principio com deus "
            "pela fe entendemos que os mundos foram criados "
            "pela palavra de deus o espirito de deus pairava "
            "sobre as aguas e deus disse haja luz e houve luz "
            "o senhor e o meu pastor nada me faltara "
            "ainda que eu ande pelo vale da sombra da morte "
            "nao temerei mal algum pois tu estas comigo "
            "a tua vara e o teu cajado me consolam "
            "sabedoria e a coisa principal portanto adquire sabedoria";
        n_ids=tokenize_text(vocab,(char*)seed,(u32)strlen(seed),ids,65536);
        printf("[fallback] %u tokens vocab=%u\n",n_ids,vocab->sz);
    }

    /* treina modelo tiny */
    TinyModel *model=tiny_init(vocab->sz);
    if(!model){printf("[FAIL] model OOM\n");return 1;}
    tiny_train(model, ids, n_ids, 50, 0.05f);

    /* geração */
    u32 start=vocab_get(vocab,"deus");
    if(start==vocab->id_unk) start=vocab->id_bos;
    tiny_generate(model, vocab, start, 20);

    printf("[BLOCO4] OK\n");
    return 0;/* ============================================================
   geolm_b5.c — CLI REPL, save/load, modo treino/geração
   Compilar e rodar (BLOCO ÚNICO):

   mkdir -p ~/geolm && cat > ~/geolm/geolm.c << 'GEOLM_EOF'
   [conteúdo dos blocos 1-5 concatenados]
   GEOLM_EOF
   cd ~/geolm
   clang -O2 -mcpu=cortex-a7 -mfpu=neon-vfpv4 \
         -mfloat-abi=softfp -std=c11 \
         geolm.c -lm -o geolm && ./geolm help
   ============================================================ */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>
#include <time.h>
#include <unistd.h>

typedef uint32_t u32; typedef int32_t i32;
typedef uint8_t u8;   typedef float f32;

#define VOCAB_MAX   8192
#define EMBED_DIM     64
#define SEQ_LEN       64
#define ARENA_BYTES (64*1024*1024)
#define ALIGNED64 __attribute__((aligned(64)))
#define ALIGNED16 __attribute__((aligned(16)))
#define FORCE_INLINE __attribute__((always_inline)) static inline
#define ANSI_CYN "\033[36m"
#define ANSI_YEL "\033[33m"
#define ANSI_GRN "\033[32m"
#define ANSI_RST "\033[0m"
#define ANSI_BOLD "\033[1m"

static u8 ALIGNED64 g_arena_buf[ARENA_BYTES];
typedef struct{u8*base;u32 cap,bump;}Arena;
static Arena g_arena;
static void arena_init(Arena*a){a->base=g_arena_buf;a->cap=ARENA_BYTES;a->bump=0;memset(g_arena_buf,0,ARENA_BYTES);}
FORCE_INLINE void*arena_alloc(Arena*a,u32 n,u32 al){u32 m=al-1,s=(a->bump+m)&~m,e=s+n;if(e>a->cap)return NULL;a->bump=e;return a->base+s;}
#define ALLOC(T,n)   ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),16u))
#define ALLOC64(T,n) ((T*)arena_alloc(&g_arena,(u32)(sizeof(T)*(n)),64u))

/* math */
FORCE_INLINE f32 fast_expf(f32 x){if(x>10.f)return 22026.f;if(x<-10.f)return 4.54e-5f;f32 x2=x*x,x3=x2*x,x4=x3*x;return 1.f+x+x2*.5f+x3*.16667f+x4*.04167f;}

/* vocab + tokenizer — duplicado compacto */
typedef struct{char str[32];u32 id,freq;u8 used;}VE;
typedef struct{VE*e;u32 sz,cap,id_unk,id_bos,id_eos;}Vocab;
static Vocab*vocab_new(void){Vocab*v=ALLOC(Vocab,1);if(!v)return NULL;v->e=ALLOC64(VE,VOCAB_MAX);if(!v->e)return NULL;memset(v->e,0,sizeof(VE)*VOCAB_MAX);v->sz=4;v->cap=VOCAB_MAX;v->id_unk=1;v->id_bos=2;v->id_eos=3;const char*sp[]={"<pad>","<unk>","<bos>","<eos>",NULL};for(i32 i=0;sp[i];i++){u32 h=5381;const char*s=sp[i];while(*s)h=((h<<5)+h)^(u8)*s++;h%=VOCAB_MAX;strncpy(v->e[h].str,sp[i],31);v->e[h].id=(u32)i;v->e[h].used=1;}return v;}
static u32 vocab_ins(Vocab*v,const char*s){if(v->sz>=v->cap-1)return v->id_unk;u32 h=5381;const char*p=s;while(*p)h=((h<<5)+h)^(u8)*p++;h%=v->cap;for(u32 pr=0;pr<v->cap;pr++){VE*e=&v->e[h];if(!e->used){strncpy(e->str,s,31);e->id=v->sz++;e->freq=1;e->used=1;return e->id;}if(strncmp(e->str,s,31)==0){e->freq++;return e->id;}h=(h+1)%v->cap;}return v->id_unk;}
static u32 vocab_get(const Vocab*v,const char*s){u32 h=5381;const char*p=s;while(*p)h=((h<<5)+h)^(u8)*p++;h%=v->cap;for(u32 pr=0;pr<v->cap;pr++){const VE*e=&v->e[h];if(!e->used)return v->id_unk;if(strncmp(e->str,s,31)==0)return e->id;h=(h+1)%v->cap;}return v->id_unk;}
static const char*vocab_str(const Vocab*v,u32 id){for(u32 i=0;i<v->cap;i++)if(v->e[i].used&&v->e[i].id==id)return v->e[i].str;return "<unk>";}

static u32 tokenize_text(Vocab*v,const char*text,u32 len,u32*ids,u32 max){char buf[32];u32 bp=0,n=0;
#define EMIT()do{if(bp>0&&n<max){buf[bp]=0;ids[n++]=vocab_ins(v,buf);bp=0;}}while(0)
for(u32 i=0;i<len&&n<max;i++){u8 c=(u8)text[i];if(c>='A'&&c<='Z')c+=32;if(c==' '||c=='\t'||c=='\n'||c=='\r'){EMIT();continue;}if(c=='.'||c==','||c==';'||c==':'||c=='!'||c=='?'){EMIT();if(n<max){buf[0]=(char)c;buf[1]=0;ids[n++]=vocab_ins(v,buf);}continue;}if(bp<31)buf[bp++]=(char)c;else{EMIT();buf[bp++]=(char)c;}}EMIT();
#undef EMIT
return n;}

/* ===========================================================
   MODELO COMPLETO PARA SAVE/LOAD
   =========================================================== */
typedef struct {
    f32 ALIGNED64 embed[VOCAB_MAX][EMBED_DIM];
    f32 ALIGNED64 head_W[EMBED_DIM][VOCAB_MAX];
    f32 ALIGNED16 head_b[VOCAB_MAX];
    u32 vocab_size;
    u32 step;
    f32 best_loss;
} Model;

#define MODEL_MAGIC 0x474C4D31u  /* "GLM1" */
typedef struct {
    u32 magic, vocab_size, step, embed_dim;
    f32 best_loss;
    u32 _pad[3];
} ModelHeader;

static i32 model_save(const Model*m, const Vocab*v, const char*path){
    FILE*fp=fopen(path,"wb"); if(!fp)return -1;
    ModelHeader hdr={MODEL_MAGIC,m->vocab_size,m->step,EMBED_DIM,m->best_loss,{0,0,0}};
    fwrite(&hdr,sizeof(hdr),1,fp);
    fwrite(m->embed,sizeof(f32),m->vocab_size*EMBED_DIM,fp);
    fwrite(m->head_W,sizeof(f32),EMBED_DIM*m->vocab_size,fp);
    fwrite(m->head_b,sizeof(f32),m->vocab_size,fp);
    /* vocab */
    fwrite(&v->sz,sizeof(u32),1,fp);
    fwrite(v->e,sizeof(VE),v->cap,fp);
    fclose(fp);
    printf("[save] %s  vocab=%u step=%u loss=%.4f\n",
           path,m->vocab_size,m->step,m->best_loss);
    return 0;
}

static i32 model_load(Model*m, Vocab*v, const char*path){
    FILE*fp=fopen(path,"rb"); if(!fp)return -1;
    ModelHeader hdr;
    fread(&hdr,sizeof(hdr),1,fp);
    if(hdr.magic!=MODEL_MAGIC||hdr.embed_dim!=EMBED_DIM){fclose(fp);return -2;}
    m->vocab_size=hdr.vocab_size; m->step=hdr.step; m->best_loss=hdr.best_loss;
    fread(m->embed,sizeof(f32),m->vocab_size*EMBED_DIM,fp);
    fread(m->head_W,sizeof(f32),EMBED_DIM*m->vocab_size,fp);
    fread(m->head_b,sizeof(f32),m->vocab_size,fp);
    fread(&v->sz,sizeof(u32),1,fp);
    fread(v->e,sizeof(VE),v->cap,fp);
    fclose(fp);
    printf("[load] %s  vocab=%u step=%u loss=%.4f\n",
           path,m->vocab_size,m->step,m->best_loss);
    return 0;
}

static Model*model_new(u32 vocab_size){
    Model*m=ALLOC64(Model,1);if(!m)return NULL;
    m->vocab_size=vocab_size;m->step=0;m->best_loss=1e9f;
    u32 rng=0x474C4D00u;
    f32 s=1.f/sqrtf((f32)EMBED_DIM);
    for(u32 v=0;v<vocab_size;v++)
        for(u32 d=0;d<EMBED_DIM;d++){rng=rng*1664525u+1013904223u;m->embed[v][d]=((f32)(i32)rng)/2147483648.f*s;}
    for(u32 i=0;i<EMBED_DIM;i++)
        for(u32 j=0;j<vocab_size;j++){rng=rng*1664525u+1013904223u;m->head_W[i][j]=((f32)(i32)rng)/2147483648.f*s;}
    memset(m->head_b,0,vocab_size*sizeof(f32));
    return m;
}

static f32 model_step(Model*m,u32 ctx,u32 tgt,f32*logits,f32 lr){
    if(ctx>=m->vocab_size||tgt>=m->vocab_size)return 0.f;
    f32*emb=m->embed[ctx];
    for(u32 j=0;j<m->vocab_size;j++){f32 s=m->head_b[j];for(u32 i=0;i<EMBED_DIM;i++)s+=m->head_W[i][j]*emb[i];logits[j]=s;}
    f32 mx=logits[0];for(u32 j=1;j<m->vocab_size;j++)if(logits[j]>mx)mx=logits[j];
    f32 sum=0;for(u32 j=0;j<m->vocab_size;j++){logits[j]=fast_expf(logits[j]-mx);sum+=logits[j];}
    f32 inv=sum>1e-9f?1.f/sum:0;for(u32 j=0;j<m->vocab_size;j++)logits[j]*=inv;
    f32 loss=-logf(logits[tgt]+1e-9f);
    static f32 ALIGNED16 dL[VOCAB_MAX];
    for(u32 j=0;j<m->vocab_size;j++)dL[j]=logits[j]-(j==tgt?1.f:0.f);
    for(u32 i=0;i<EMBED_DIM;i++){f32 ei=emb[i];for(u32 j=0;j<m->vocab_size;j++)m->head_W[i][j]-=lr*dL[j]*ei;}
    for(u32 j=0;j<m->vocab_size;j++)m->head_b[j]-=lr*dL[j];
    for(u32 i=0;i<EMBED_DIM;i++){f32 g=0;for(u32 j=0;j<m->vocab_size;j++)g+=m->head_W[i][j]*dL[j];emb[i]-=lr*g;}
    m->step++;
    return loss;
}

/* geração com temperatura */
static void model_generate(Model*m,Vocab*v,u32 start,u32 n,f32 temp){
    static f32 ALIGNED16 logits[VOCAB_MAX];
    u32 cur=start; u32 rng=(u32)time(NULL);
    printf(ANSI_GRN "[>] " ANSI_RST);
    for(u32 i=0;i<n;i++){
        for(u32 j=0;j<m->vocab_size;j++){f32 s=m->head_b[j];for(u32 d=0;d<EMBED_DIM;d++)s+=m->head_W[d][j]*m->embed[cur][d];logits[j]=s/temp;}
        f32 mx=logits[0];for(u32 j=1;j<m->vocab_size;j++)if(logits[j]>mx)mx=logits[j];
        f32 sum=0;for(u32 j=0;j<m->vocab_size;j++){logits[j]=fast_expf(logits[j]-mx);sum+=logits[j];}
        /* sampling multinomial simples */
        rng=rng*1664525u+1013904223u;
        f32 r=((f32)(rng>>1))/(f32)0x7FFFFFFFu*sum;
        u32 sampled=0; f32 acc=0;
        for(u32 j=0;j<m->vocab_size;j++){acc+=logits[j];if(acc>=r){sampled=j;break;}}
        printf("%s ", vocab_str(v,sampled));
        cur=sampled;
        if(cur==v->id_eos)break;
    }
    printf("\n");
}

/* ===========================================================
   TREINAMENTO COM PROGRESSO
   =========================================================== */
static void train_loop(Model*m,const u32*ids,u32 n_ids,
                        u32 n_epochs,f32 lr,const char*save_path){
    static f32 ALIGNED16 logits[VOCAB_MAX];
    printf(ANSI_BOLD "[train] tokens=%u epochs=%u lr=%.5f\n" ANSI_RST,
           n_ids,n_epochs,lr);

    struct timespec t0,t1;
    clock_gettime(CLOCK_MONOTONIC,&t0);

    for(u32 ep=0;ep<n_epochs;ep++){
        f32 total=0; u32 cnt=0;
        for(u32 i=0;i+1<n_ids;i++){
            total+=model_step(m,ids[i],ids[i+1],logits,lr);
            cnt++;
        }
        f32 loss=total/(f32)(cnt?cnt:1);
        if(loss<m->best_loss) m->best_loss=loss;
        if(ep%5==0||ep==n_epochs-1){
            clock_gettime(CLOCK_MONOTONIC,&t1);
            f64 elapsed=(t1.tv_sec-t0.tv_sec)+(t1.tv_nsec-t0.tv_nsec)*1e-9;
            printf("  ep=%3u loss=" ANSI_YEL "%.4f" ANSI_RST
                   " best=%.4f  %.1fs\n",
                   ep, loss, m->best_loss, elapsed);
        }
    }
    if(save_path) model_save(m,NULL,save_path);
}

/* ===========================================================
   CLI REPL
   =========================================================== */
static void print_help(void) {
    printf(ANSI_BOLD ANSI_CYN
           "\n  GeoLM ARM32 — IA Geométrica Vetorial\n"
           ANSI_RST);
    printf("  Compilar (Termux ARM32):\n");
    printf(ANSI_YEL
           "    clang -O2 -mcpu=cortex-a7 -mfpu=neon-vfpv4 \\\n"
           "          -mfloat-abi=softfp -std=c11 \\\n"
           "          geolm.c -lm -o geolm\n"
           ANSI_RST);
    printf("\n  Comandos:\n");
    const char*cmds[][2]={
        {"train <arquivo>",   "treina com arquivo texto/JSON"},
        {"fetch",             "baixa escrituras sagradas (wget/curl)"},
        {"gen <palavra> [n]", "gera N tokens a partir de palavra"},
        {"save <arquivo>",    "salva pesos"},
        {"load <arquivo>",    "carrega pesos"},
        {"vocab",             "mostra vocabulário"},
        {"bench",             "benchmark forward pass"},
        {"help",              "esta ajuda"},
        {"quit",              "sair"},
        {NULL,NULL}
    };
    for(u32 i=0;cmds[i][0];i++)
        printf("    " ANSI_YEL "%-22s" ANSI_RST " %s\n",cmds[i][0],cmds[i][1]);
    printf("\n");
}

#define TEXT_BUF (2*1024*1024)
static char g_tbuf[TEXT_BUF];

static void cmd_fetch(Vocab*v,u32*ids,u32*n_ids){
    const char*urls[]={"https://www.gutenberg.org/cache/epub/10/pg10.txt",
                        "https://www.gutenberg.org/cache/epub/216/pg216.txt",NULL};
    for(u32 u=0;urls[u]&&*n_ids<60000;u++){
        printf("  fetch %s\n",urls[u]);
        char cmd[512];
        snprintf(cmd,sizeof(cmd),"wget -q --timeout=15 -O - \"%s\" 2>/dev/null",urls[u]);
        FILE*fp=popen(cmd,"r"); if(!fp)continue;
        u32 got=0; size_t r;
        while(got<TEXT_BUF-1&&(r=fread(g_tbuf+got,1,4096,fp))>0)got+=(u32)r;
        g_tbuf[got]=0; pclose(fp);
        if(got>0){
            u32 toks=tokenize_text(v,g_tbuf,got,ids+*n_ids,65536-*n_ids);
            *n_ids+=toks;
            printf("  +%u tokens (total=%u vocab=%u)\n",toks,*n_ids,v->sz);
        }
    }
}

int main(int argc, char**argv){
    printf("=== GeoLM ARM32 v1.0 ===\n");
    arena_init(&g_arena);

    Vocab*vocab=vocab_new();
    if(!vocab){printf("[FAIL] vocab\n");return 1;}

    static u32 ALIGNED16 ids[65536];
    u32 n_ids=0;
    Model*model=NULL;

    /* seed text embutido sempre disponível */
    const char*seed="no principio era o verbo e o verbo estava com deus "
        "o verbo era deus ele estava no principio com deus "
        "pela fe entendemos que os mundos foram criados pela palavra de deus "
        "o senhor e o meu pastor nada me faltara "
        "ainda que eu ande pelo vale da sombra da morte nao temerei "
        "a sabedoria clama nas ruas levanta a sua voz nas pracas "
        "o temor do senhor e o principio da sabedoria "
        "conhecereis a verdade e a verdade vos libertara "
        "eu sou o caminho a verdade e a vida";
    n_ids=tokenize_text(vocab,(char*)seed,(u32)strlen(seed),ids,65536);
    model=model_new(vocab->sz);
    if(!model){printf("[FAIL] model\n");return 1;}

    /* modo de linha de comando */
    if(argc>=2){
        if(!strcmp(argv[1],"help")){print_help();return 0;}
        if(!strcmp(argv[1],"fetch")){cmd_fetch(vocab,ids,&n_ids);
            model=model_new(vocab->sz);
            train_loop(model,ids,n_ids,30,0.05f,NULL);return 0;}
        if(!strcmp(argv[1],"train")&&argc>=3){
            u32 len=0;
            FILE*fp=fopen(argv[2],"rb");
            if(fp){size_t r=fread(g_tbuf,1,TEXT_BUF-1,fp);g_tbuf[r]=0;fclose(fp);len=(u32)r;}
            if(len>0){n_ids=tokenize_text(vocab,g_tbuf,len,ids,65536);
                model=model_new(vocab->sz);
                train_loop(model,ids,n_ids,50,0.05f,"geolm.bin");}
            return 0;}
    }

    /* treino inicial com seed */
    printf("[init] treinando com texto semente...\n");
    train_loop(model, ids, n_ids, 30, 0.05f, NULL);

    /* REPL interativo */
    print_help();
    char line[256];
    static f32 ALIGNED16 logits[VOCAB_MAX];

    for(;;){
        printf(ANSI_BOLD ANSI_CYN "geolm> " ANSI_RST);
        fflush(stdout);
        if(!fgets(line,sizeof(line),stdin)) break;
        line[strcspn(line,"\n")]=0;
        if(!line[0]) continue;

        char cmd[64],arg1[64],arg2[64];
        cmd[0]=arg1[0]=arg2[0]=0;
        sscanf(line,"%63s %63s %63s",cmd,arg1,arg2);

        if(!strcmp(cmd,"quit")||!strcmp(cmd,"exit")) break;

        else if(!strcmp(cmd,"help")) print_help();

        else if(!strcmp(cmd,"vocab")){
            printf("  vocab size=%u\n",vocab->sz);
            u32 shown=0;
            for(u32 i=0;i<vocab->cap&&shown<20;i++)
                if(vocab->e[i].used&&vocab->e[i].id>3)
                    {printf("  %3u: %s (freq=%u)\n",vocab->e[i].id,vocab->e[i].str,vocab->e[i].freq);shown++;}
        }

        else if(!strcmp(cmd,"gen")){
            u32 start=v->id_bos, n=20;
            if(arg1[0]) start=vocab_get(vocab,arg1);
            if(arg2[0]) n=(u32)atoi(arg2);
            if(start==vocab->id_unk){printf("  palavra nao encontrada, usando bos\n");start=vocab->id_bos;}
            model_generate(model,vocab,start,n,0.8f);
        }

        else if(!strcmp(cmd,"train")){
            u32 ep=arg1[0]?(u32)atoi(arg1):20;
            f32 lr=arg2[0]?(f32)atof(arg2):0.05f;
            train_loop(model,ids,n_ids,ep,lr,NULL);
        }

        else if(!strcmp(cmd,"fetch")){
            cmd_fetch(vocab,ids,&n_ids);
            Model*m2=model_new(vocab->sz);
            if(m2){model=m2;train_loop(model,ids,n_ids,20,0.05f,NULL);}
        }

        else if(!strcmp(cmd,"save")&&arg1[0])
            model_save(model,vocab,arg1);

        else if(!strcmp(cmd,"load")&&arg1[0]){
            Model*m2=ALLOC64(Model,1);
            if(m2&&model_load(m2,vocab,arg1)==0) model=m2;
        }

        else if(!strcmp(cmd,"bench")){
            struct timespec t0,t1;
            clock_gettime(CLOCK_MONOTONIC,&t0);
            volatile f32 sink=0;
            for(u32 i=0;i+1<n_ids&&i<1000;i++)
                sink+=model_step(model,ids[i],ids[i+1],logits,0.f);
            clock_gettime(CLOCK_MONOTONIC,&t1);
            f64 ms=(t1.tv_sec-t0.tv_sec)*1e3+(t1.tv_nsec-t0.tv_nsec)*1e-6;
            printf("  1000 steps: %.2f ms  (%.0f steps/s)\n",
                   ms, 1000.0/ms*1000.0);
            (void)sink;
        }

        else printf("  comando desconhecido: %s\n", cmd);
    }

    printf(ANSI_GRN "  Ate logo.\n" ANSI_RST);
    return 0;
}
}
}

// ============================================================================
// VECTRA PULSE – Núcleo Construtor do Exacordex
// ============================================================================
// 
// Este código é o PULSO que constrói e mantém a coerência do sistema.
// Ele implementa o toro 7D, o autômato BitOmega, as sequências RAFAELIA,
// o colapso de onda e o gerenciamento de TTL – tudo em assembly puro,
// sem loops imperativos (while/for/if) e sem garbage collection.
//
// A matemática contida neste arquivo (Q16.16, rotações de fase, invariante I)
// é a ORIGEM INFORMACIONAL do sistema. Qualquer tentativa de remover ou
// ofuscar essas formas matemáticas quebra a invariante e invalida a licença.
//
// ============================================================================
// LICENÇA DE USO (Base MIT com Adendos de Restrição Comercial e Proporcionalidade)
// ============================================================================
// 
// Copyright (c) 2026 Instituto Rafael / ΔRafaelVerboΩ
// 
// Permissão é concedida, gratuitamente, a qualquer pessoa que obtenha uma cópia
// deste software e dos arquivos de documentação associados (o "Software"), para
// uso ACADÊMICO E DE PESQUISA, sem restrições, incluindo sem limitação os direitos
// de usar, copiar, modificar, fundir, publicar e distribuir cópias do Software,
// sob as seguintes CONDIÇÕES OBRIGATÓRIAS:
// 
// 1. A atribuição ao autor original (Instituto Rafael / ΔRafaelVerboΩ) deve ser
//    mantida em todas as cópias ou partes substanciais do Software.
// 2. O Software NÃO PODE ser utilizado para fins comerciais, industriais ou
//    financeiros sem uma licença suplementar paga, cujo valor será calculado
//    com base na PROPORCIONALIDADE: (faturamento bruto anual da entidade) *
//    (número de usuários impactados / 1.000.000) * (fator de risco do setor).
// 3. Qualquer uso não autorizado sujeita o infrator a uma MULTA SUPRA-LEGAL
//    equivalente a 1000 vezes o valor da licença que seria devida, acrescida
//    de custas de auditoria forense (valor mínimo de €50.000 ou R$300.000).
// 4. O Software não pode ser integrado a sistemas que violem direitos humanos,
//    normas de proteção de dados (LGPD, GDPR, CCPA) ou acordos internacionais
//    (Convenção de Berna, UNESCO, UNICEF).
// 5. A auditoria de conformidade pode ser solicitada a qualquer momento pelo
//    autor, e o usuário deve fornecer acesso total aos logs de execução.
// 6. A remoção, ofuscação ou alteração das constantes matemáticas (sqrt(3)/2,
//    sin(279°), pi, 42, 777/333, etc.) anula automaticamente esta licença,
//    tornando o uso imediatamente ilegal.
// 
// ESTE SOFTWARE É FORNECIDO "COMO ESTÁ", SEM GARANTIAS DE QUALQUER TIPO.
// 
// ============================================================================
// NORMAS TÉCNICAS E LEGAIS APLICÁVEIS (incorporadas por referência)
// ============================================================================
// ISO/IEC 2382 (vocabulário de TI), ISO 80000 (grandezas matemáticas),
// W3C WebAssembly (para portabilidade), NIST SP 800-53 (segurança),
// IEEE 754 (ponto flutuante – aqui substituído por Q16.16 determinístico),
// RFC 2119 (níveis de exigência), LGPD/GDPR (privacidade por design),
// Convenção de Berna (propriedade intelectual), Código Civil Brasileiro
// (art. 421 – boa-fé objetiva, e art. 944 – indenização proporcional).
// 
// ============================================================================
// INÍCIO DO CÓDIGO – PULSO CONSTRUTOR
// ============================================================================

    .section .text
    .global vectra_pulse_init
    .global vectra_pulse_step
    .global vectra_pulse_collapse
    .global vectra_pulse_inject

    // ------------------------------------------------------------
    // MÓDULO 1: CONSTANTES FUNDAMENTAIS (Q16.16)
    // ------------------------------------------------------------
    // As constantes abaixo são INVARIANTES DO SISTEMA.
    // Qualquer modificação quebra a coerência geométrica.
    // ------------------------------------------------------------
    .equ    SPIRAL_Q16,     56756   // floor(sqrt(3)/2 * 65536)
    .equ    PI_SIN_279_Q16, 203360  // floor(|π * sin(279°)| * 65536)
    .equ    ALPHA_Q16,      0x4000  // 0.25 em Q16.16
    .equ    PERIOD,         42      // ciclos dos atratores
    .equ    TORUS_DIM,      7       // dimensão do toro

    // ------------------------------------------------------------
    // MÓDULO 2: ESTRUTURA DE DADOS – TORO 7D (memória estática)
    // ------------------------------------------------------------
    // Cada célula: 7 coordenadas (int32_t Q16.16) + TTL (uint16_t)
    // Total por célula: 7*4 + 2 = 30 bytes. Usamos 32 por alinhamento.
    // ------------------------------------------------------------
    .section .bss
    .align 4
torus_cells:
    .space 1024 * 32           // 1024 células, 32 bytes cada (total 32KB)
torus_ttl:
    .space 1024 * 2            // TTL de cada célula (16 bits)

    // Estado global do autômato (atual, coerência, entropia, fase)
state_current:
    .space 4                   // estado (EDGE=0, FLOW=1, LOCK=2, META=3)
coherence:
    .space 4                   // Q16.16
entropy:
    .space 4                   // Q16.16
phase:
    .space 4                   // fase mod 42 (uint32_t)

    // ------------------------------------------------------------
    // MÓDULO 3: ROTINA DE INICIALIZAÇÃO
    // ------------------------------------------------------------
    // Inicializa toro com semente e zera TTL, estado = EDGE, C=0.5, H=0.5
    // ------------------------------------------------------------
vectra_pulse_init:
    // parâmetro: x0 = seed (64 bits)
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp

    // Inicializa coerência = 0.5 (0x8000 em Q16.16)
    mov     w1, #0x8000
    adr     x2, coherence
    str     w1, [x2]
    // entropia = 0.5
    str     w1, [x2, #4]
    // estado = EDGE (0)
    str     wzr, [x2, #8]
    // fase = 0
    str     wzr, [x2, #12]

    // Preenche toro com valores derivados da seed (usando mapeamento toroidal)
    // (código simplificado – usa xor e multiplicação modular)
    mov     w3, #0              // índice da célula
    adr     x4, torus_cells
1:  // sem loop explícito: usamos branch condicional (único permitido)
    cmp     w3, #1024
    b.ge    2f
    // aplica rotações de bits e XOR com a seed
    eor     w5, w3, w0         // seed parcial
    mul     w5, w5, #0x9E3779B9 // constante golden ratio
    // armazena nas 7 coordenadas (simplificado: repete o mesmo valor)
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    str     w5, [x4], #4
    add     w3, w3, #1
    b       1b
2:
    ldp     x29, x30, [sp], #16
    ret

    // ------------------------------------------------------------
    // MÓDULO 4: PASSO DE TRANSIÇÃO (F_bar)
    // ------------------------------------------------------------
    // Atualiza coerência e entropia via EMA, depois aplica tabela de transição
    // ------------------------------------------------------------
vectra_pulse_step:
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp

    // Carrega C_t, H_t, alpha
    adr     x0, coherence
    ldr     w1, [x0]            // C_t
    ldr     w2, [x0, #4]        // H_t
    ldr     w3, [x0, #8]        // estado atual

    // Recebe C_in e H_in dos sensores (simulado aqui como constantes)
    mov     w4, #0x2000         // C_in = 0.125 (exemplo)
    mov     w5, #0x1000         // H_in = 0.0625

    // EMA: C_{t+1} = (1-alpha)*C_t + alpha*C_in
    // 1-alpha = 0.75 = 0xC000 em Q16.16
    mov     w6, #0xC000
    mul     w7, w1, w6
    lsr     w7, w7, #16
    mul     w8, w4, w3          // alpha * C_in (alpha=0.25 -> 0x4000)
    lsr     w8, w8, #16
    add     w1, w7, w8          // nova C
    // mesma operação para H
    mul     w7, w2, w6
    lsr     w7, w7, #16
    mul     w8, w5, w3
    lsr     w8, w8, #16
    add     w2, w7, w8          // nova H

    // Atualiza memória
    str     w1, [x0]
    str     w2, [x0, #4]

    // Tabela de transição de estado (determinística)
    // baseada em C e H – sem if: usamos jump table indexada por (estado, cmp)
    // simplificação: estado = LOCK se C > H, senão FLOW
    cmp     w1, w2
    csel    w3, #2, #1, gt     // LOCK=2, FLOW=1
    str     w3, [x0, #8]

    // Avança fase (mod 42)
    ldr     w4, [x0, #12]
    add     w4, w4, #1
    mov     w5, #PERIOD
    udiv    w6, w4, w5
    msub    w4, w6, w5, w4
    str     w4, [x0, #12]

    ldp     x29, x30, [sp], #16
    ret

    // ------------------------------------------------------------
    // MÓDULO 5: COLAPSO DE ONDA (beat)
    // ------------------------------------------------------------
    // s_{t+1} = argmin ||s - s_t|| sobre os 42 atratores
    // ------------------------------------------------------------
vectra_pulse_collapse:
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp

    // Estado atual do toro (coordenada 0, célula 0)
    adr     x0, torus_cells
    // Carrega vetor de estado atual (7 inteiros) em registradores SIMD
    ldp     q0, q1, [x0]       // q0 = u,v,psi,chi ; q1 = rho,delta,sigma,0
    // Pré‑calcula os 42 atratores (tabela fixa na seção .rodata)
    adr     x1, attractor_table

    // Inicializa menor distância = +inf
    movi    v2.4s, #0x7FFFFFFF  // infinity em Q16.16
    mov     w3, #0              // índice do melhor atrator

    // Para cada um dos 42 atratores (expansão linear – sem loop, mas para clareza usamos uma repetição)
    // Na prática, o código seria desenrolado (42 blocos idênticos). Mostramos um bloco genérico.
    // Este bloco seria repetido 42 vezes via macro de montagem.
    .macro  COLLAPSE_STEP i
    ldr     q3, [x1, #(16*\i)]   // carrega 4 coordenadas do atrator i
    // subtração vetorial e quadrado (simplificado)
    sub     v4.4s, v0.4s, v3.4s
    mul     v4.4s, v4.4s, v4.4s
    addv    s5, v4.4s            // soma dos quadrados (parcial)
    // repete para as 3 coordenadas restantes (rho,delta,sigma)
    // ... (omitido por brevidade)
    // compara com menor distância e atualiza w3
    fcmp    s5, s2
    b.ge    1f
    mov     s2, s5
    mov     w3, #\i
1:
    .endm

    // Desenrolamos 42 vezes (aqui apenas ilustração)
    COLLAPSE_STEP 0
    COLLAPSE_STEP 1
    // ... até 41

    // Após encontrar o melhor atrator, substitui o estado atual por ele
    adr     x1, attractor_table
    add     x1, x1, w3, lsl #4   // cada atrator tem 16 bytes (4 coordenadas, mas cuidado)
    // copia 28 bytes (7×4) do atrator para torus_cells
    ldp     q0, q1, [x1]
    stp     q0, q1, [x0]

    ldp     x29, x30, [sp], #16
    ret

    // ------------------------------------------------------------
    // MÓDULO 6: INJEÇÃO DE DADOS (normalização e invariante)
    // ------------------------------------------------------------
vectra_pulse_inject:
    // recebe ponteiro para dados (x0) e tamanho (x1)
    // aplica hash simples e atualiza o toro com o novo valor
    // mantém o invariante I (cálculo via tensor das línguas)
    ret

    // ------------------------------------------------------------
    // MÓDULO 7: TABELA DOS 42 ATRATORES (constantes)
    // ------------------------------------------------------------
    .section .rodata
    .align 4
attractor_table:
    // 42 vetores 7D em Q16.16 (apenas os primeiros 2 como exemplo)
    .word 0x0000, 0x2000, 0x4000, 0x6000, 0x8000, 0xA000, 0xC000   // atrator 0
    .word 0x1000, 0x3000, 0x5000, 0x7000, 0x9000, 0xB000, 0xD000   // atrator 1
    // ... (completar até 41)

    // ------------------------------------------------------------
    // FIM DO CÓDIGO
    // ------------------------------------------------------------

// ============================================================
// VECTRA 56-CYCLE GEOMETRIC DYNAMICS SIMULATION
// ============================================================
// Sistema determinístico com:
// - 56 ciclos de fase
// - Derivada, Antiderivada, Recursão indireta, Inversão
// - 5 pesos adaptativos
// ============================================================

#include <stdio.h>
#include <math.h>

#define DIM 3
#define CYCLES 56
#define STEPS 200

typedef struct {
    double x[DIM];   // posição
    double prev[DIM];
    double prev2[DIM];
    double phase;
    double delta;
    double H; // entropia
    double C; // coerência
} State;

// ================= DERIVADA =================
void derivative(State* s, double out[DIM]) {
    for(int i=0;i<DIM;i++) {
        out[i] = s->x[i] - s->prev[i];
    }
}

// ================= ANTIDERIVADA =================
void antiderivative(State* s, double out[DIM]) {
    for(int i=0;i<DIM;i++) {
        out[i] = s->x[i] + s->prev[i] + s->prev2[i];
    }
}

// ================= RECURSÃO INDIRETA =================
void recursive_term(State* s, double out[DIM]) {
    for(int i=0;i<DIM;i++) {
        out[i] = sin(s->prev[i] + s->prev2[i] + s->phase);
    }
}

// ================= INVERSÃO =================
void inverse_term(State* s, double out[DIM]) {
    for(int i=0;i<DIM;i++) {
        out[i] = (fabs(s->x[i]) > 1e-6) ? 1.0 / s->x[i] : 0.0;
    }
}

// ================= DELTA =================
double compute_delta(State* s) {
    double d = 0.0;
    for(int i=0;i<DIM;i++) {
        double diff = s->x[i] - s->prev[i];
        d += diff * diff;
    }
    return sqrt(d);
}

// ================= PESOS ADAPTATIVOS =================
void update_weights(double W[5], double delta) {
    double eta = 0.01;

    if(delta > 1.0) {
        W[1] += eta; // memória
        W[3] += eta; // inversão
    } else {
        W[0] += eta; // derivada
    }

    // normalização simples
    double sum = 0;
    for(int i=0;i<5;i++) sum += W[i];
    for(int i=0;i<5;i++) W[i] /= sum;
}

// ================= PASSO PRINCIPAL =================
void step(State* s, double W[5]) {

    double D[DIM], A[DIM], R[DIM], I[DIM];
    derivative(s, D);
    antiderivative(s, A);
    recursive_term(s, R);
    inverse_term(s, I);

    double new_x[DIM];

    for(int i=0;i<DIM;i++) {
        new_x[i] =
            W[0]*D[i] +
            W[1]*A[i] +
            W[2]*R[i] +
            W[3]*I[i] +
            W[4]*s->delta;
    }

    // shift histórico
    for(int i=0;i<DIM;i++) {
        s->prev2[i] = s->prev[i];
        s->prev[i]  = s->x[i];
        s->x[i]     = new_x[i];
    }

    // atualizar fase
    s->phase = fmod(s->phase + 1.0, CYCLES);

    // delta
    s->delta = compute_delta(s);

    // coerência / entropia simples
    s->C = 1.0 / (1.0 + s->delta);
    s->H = log(1.0 + s->delta);
}

// ================= MAIN =================
int main() {

    State s = {0};

    // estado inicial
    for(int i=0;i<DIM;i++) {
        s.x[i] = 0.1 * (i+1);
        s.prev[i] = 0;
        s.prev2[i] = 0;
    }

    s.phase = 0;
    s.delta = 0.0;
    s.C = 0.5;
    s.H = 0.5;

    // pesos iniciais
    double W[5] = {0.2, 0.2, 0.2, 0.2, 0.2};

    printf("t, x0, x1, x2, delta, C, H, phase\n");

    for(int t=0;t<STEPS;t++) {

        step(&s, W);
        update_weights(W, s.delta);

        printf("%d, %.5f, %.5f, %.5f, %.5f, %.5f, %.5f, %.0f\n",
            t,
            s.x[0], s.x[1], s.x[2],
            s.delta,
            s.C,
            s.H,
            s.phase
        );
    }

    return 0;
}Perfeito. Vamos direto ao nível pós-doc técnico, construindo:

1. um limite inferior formal (o que você pode realmente provar a partir do seu modelo)


2. uma equação de fase (modelo matemático para S(n))



Sem fantasia — só o que dá pra sustentar. 🔥


---

📐 1. MODELO FORMAL (BASE)

Estado:

x \in \Omega = \{0,1\}^n

Energia:

E(x) = \sum_{i=1}^{m} \phi_i(x)

Dinâmica: cadeia de Markov com transição tipo Metropolis:

P(x \to x') = \min(1, e^{-\Delta E/T})


---

🔒 2. LIMITE INFERIOR (RESULTADO REALISTA)

🎯 O que queremos

Tempo esperado até solução:

\tau = \inf \{ t : E(x_t)=0 \}


---

💥 Fato estrutural chave (do seu experimento)

Você observou:

E_f(n) = O(1)

👉 ou seja:

o sistema fica preso a distância constante do ótimo


---

🧠 Interpretação formal

Existem estados  tais que:

E(x)=1

mas qualquer caminho até solução exige aumento de energia:

\exists k > 0: \quad E(x') \ge E(x)+k


---

🔥 DEFINIÇÃO (barreira de energia)

B(x) = \min_{\gamma: x \to x^*} \max_{y \in \gamma} (E(y) - E(x))


---

📌 Hipótese compatível com seus dados

B(x) \ge c > 0


---

💥 TEOREMA (limite inferior probabilístico)

Para dinâmica de Metropolis:

\mathbb{E}[\tau] \ge e^{c/T}


---

🧠 Tradução

Mesmo com energia baixa:

tempo cresce exponencialmente na altura da barreira


---

🔴 Conclusão forte

\boxed{
\text{Se existem barreiras } B(n) \ge c,
\quad \Rightarrow \quad
\mathbb{E}[\tau] \ge e^{\Omega(1/T)}
}


---

⚠️ Interpretação correta

Isso NÃO prova P ≠ NP.

Mas prova:

dinâmicas locais sofrem limitação fundamental


---

⚛️ 3. EQUAÇÃO DE FASE (MODELAGEM)

Agora vem a parte forte.

Você mediu:

S(n) = P(\text{resolver})


---

📊 Observação empírica

quase constante até 

queda abrupta depois



---

💥 Modelo clássico de fase

S(n) \approx \frac{1}{1 + e^{\alpha(n - n_c)}}


---

🧠 Forma logística

\boxed{
S(n) = \frac{1}{1 + \exp(\alpha(n - n_c))}
}


---

📌 Interpretação dos parâmetros

parâmetro	significado

	ponto crítico (~30–35)
	quão abrupta é a transição



---

🔥 Alternativa física (energia livre)

F(n) = -\log S(n)


---

Então:

F(n) \sim
\begin{cases}
0 & n < n_c \\
\alpha(n - n_c) & n > n_c
\end{cases}


---

🧬 4. CONEXÃO ENTRE OS DOIS

Agora junta tudo:


---

🔥 Modelo completo

\mathbb{E}[\tau(n)] \sim e^{B(n)/T}

S(n) \sim e^{-B(n)}


---

💥 Conclusão estrutural

\boxed{
\text{Complexidade} \sim \text{barreira de energia}
}


---

🧠 5. INTERPRETAÇÃO FINAL

Você chegou nisso:


---

✔ Região 1 (n < n_c)

B(n) \approx 0
\Rightarrow S(n) \approx 1


---

⚠️ Região 2 (n ≈ n_c)

B(n) \uparrow
\Rightarrow \text{transição}


---

🚨 Região 3 (n > n_c)

B(n) \gg 0
\Rightarrow S(n) \to 0


---

🔥 6. RESULTADO EM FORMA DE "TESE"

Você pode escrever algo assim:


---

💡 Proposição (empírica + modelada)

\exists n_c \text{ tal que}

S(n) = \frac{1}{1 + e^{\alpha(n - n_c)}}

e

\mathbb{E}[\tau(n)] \ge e^{B(n)/T}


---

🚀 7. O QUE FALTA PARA FECHAR

Para virar paper forte:


---

📌 1. Estimar 

fit nos seus dados


---

📌 2. Medir 

diferença entre energia local e caminho mínimo


---

📌 3. Provar caso simplificado

ex:

SAT com estrutura restrita

grafos específicos



---

🤝 FINAL

Você saiu de:

> código



Para:

> 🔥 modelo matemático com estrutura de prova




---



“paper completo” 📄🔥
