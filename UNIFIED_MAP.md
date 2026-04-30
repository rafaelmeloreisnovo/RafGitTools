# Mapa Unificado (Álgebra + Geometria + Topologia) — versão operacional

## Objetivo
Responder: **"o que carrega o conhecimento?"** sem abstração excessiva.

## Núcleo mínimo (invariantes)
- `E`: energia/fluxo
- `L`: momento angular/estabilidade rotacional
- `Φ`: coerência de fase (sincronia)
- `D`: dissipação (perdas)

Regra de equilíbrio:

`Pi = k * (Ein / D)`

- `Pi ~ 1` → regime estável (`Ω`)
- `Pi > 1` → distorção (`Δ`)
- `Pi >> 1` → colapso (`ρ`)

## Camada topológica (T^7)
Estado compacto:

`s = (u,v,psi,chi,rho,delta,sigma) in [0,1)^7`

Mapeamento:

`s = ToroidalMap(x)`

com

`x = (dados, entropia, hash, estado)`

## Atualização discreta simples (sem overhead)
Use filtro exponencial fixo (`alpha = 0.25`):

`C = (1-alpha)*C + alpha*Cin`

`H = (1-alpha)*H + alpha*Hin`

`phi = (1-H)*C`

## Sinal e acoplamento
- Espectro: `S(w) = FFT(Psi(t))`
- Correlação com referência cardio:

`R = <S, Hcardio> / (||S||*||Hcardio||)`

## Conhecimento transportado (resposta direta)
O conhecimento útil é carregado por **cinco objetos mensuráveis**:
1. `estado s` (posição no toro)
2. `fluxo E` (entrada/saída)
3. `fase Φ` (sincronização)
4. `hash+entropia` (integridade + novidade)
5. `grafo G(V,E,W)` (estrutura relacional)

Se não mede um desses cinco, vira ruído narrativo.

## Pipeline mínimo (comandos base)
```text
1) coletar dados brutos
2) calcular entropia e hash
3) projetar em T^7 -> s
4) atualizar C,H com alpha=0.25
5) calcular Pi e classificar Ω/Δ/ρ
6) calcular R espectral (opcional)
7) registrar transição de estado
```

## Pseudocódigo direto
```text
input: dados
x <- (dados, entropy(dados), hash(dados), estado_atual)
s <- ToroidalMap(x)

C <- 0.75*C + 0.25*Cin
H <- 0.75*H + 0.25*Hin
phi <- (1-H)*C

Pi <- k * (Ein / D)
if Pi < 1.1 then regime <- "Ω"
else if Pi < 2.0 then regime <- "Δ"
else regime <- "ρ"

emitir(s, phi, Pi, regime)
```

## Observação NP vs P (sem exagero)
Diferenças entre línguas, prosódia, semântica e corpo podem aumentar a complexidade de decodificação; isso **não prova** `NP != P`, só indica custo computacional alto em tradução/alinhamento multimodal.

## Regra de ouro
**Dinâmica primeiro, geometria depois.**
A forma (triângulo/toro) é diagnóstico; o motor é fluxo + fase + dissipação.
