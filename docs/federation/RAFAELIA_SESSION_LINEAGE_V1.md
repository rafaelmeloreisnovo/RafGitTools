# RAFAELIA Session Lineage v1

Status: `DRIVE_FIRST / POINTER_ONLY / NON_DESTRUCTIVE / CLAIM_ALLOWED=false`.

## Correção central

A ponte histórica `Termux ARM64 ↔ Drive/rclone ↔ Debian ARM64` não está ativa atualmente. Hoje o Drive é usado para custódia periódica. A leitura histórica permanece como evidência, mas foi marcada como `SUPERSEDED` quando tratada como estado atual.

## Cadeia histórica de captura

```text
PROMPT_LOG
→ RESPONSE_LOG
→ POINT_STATE
→ ZIP_SNAPSHOT
→ SUCCESSOR_LINK
```

Dois ZIPs com os mesmos bytes não são automaticamente o mesmo objeto histórico. Contexto, horário, predecessores e estado dos pontos também integram a identidade do snapshot.

## Heurísticas

1. Leitura direta: correção explícita do autor prevalece sobre inferência anterior.
2. Derivada temporal: registrar o delta entre estados adjacentes.
3. Reversa causal: voltar do resultado ao prompt, log e backup.
4. Ambiente/ABI: separar ARM64/ARM32, Bionic/glibc e dados/binários.
5. Linhagem de snapshot: ligar prompt, resposta, pontos, ZIP e sucessor.
6. Autoridade: escrever apenas no repositório responsável.
7. Contrafactual: overlays devem ser removíveis sem alterar fontes.
8. Ausência tipada: `TOKEN_VAZIO` não é PASS, zero ou não examinado.
9. Deduplicação contextual: bytes iguais não apagam momentos distintos.
10. Fronteira de claim: histórico não prova runtime atual.

## Repositórios necessários

```text
RafGitTools #265 → control plane e heurísticas
Mapa #25        → navegação pointer-only
papers #9       → síntese metodológica e falsificadores
```

Não foram escolhidos Termux, ChipQuantum ou RLL nesta passagem porque a sessão não introduz novo runtime Android, kernel matemático ou resultado cosmológico.

## Drive

```text
folder      1PYiDEe1L-1_NoctAiIeKa7tvp8CObyHm
document    1089HfqukEGOIoeCGMeDSAmVeCklt8bKIsLz-DPuHp0A
spreadsheet 1Giq2Iux_xeRA3jYGxBzwFFry51xlocEWYnT1jA7uHrY
```

A planilha nativa ficou parcialmente preenchida; um XLSX completo foi produzido localmente. Não se afirma que todo ZIP foi inspecionado ou ligado ao prompt exato.

## Fronteiras

```text
bridge_active_now=false
drive_periodic_custody_now=true
historical_prompt_response_zip_capture=VERIFIED_BY_AUTHOR
every_zip_fully_inspected=TOKEN_VAZIO
every_snapshot_linked_to_exact_prompt=TOKEN_VAZIO
private_payload_copied=false
automatic_merge=false
claim_allowed=false
```
