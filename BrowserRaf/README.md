# BrowserRaf

Diretório para extrair, testar e versionar **os dados internos** gerados pelo `Browser.sh`.

## Arquivos principais
- `Browser.sh`: arquivo bruto baixado de `termux-app-rafacodephi/master/Browser.sh`.
- `extract_browser.sh`: extrai o script interno executável para `Browser_extracted.sh`.
- `Browser_extracted.sh`: script executável extraído.

## Dados internos extraídos (commitados)
Os arquivos que o script cria ficam versionados em `BrowserRaf/internal/`:
- `br_types.h`
- `br_sys.h`
- `br_tls.h`
- `br_http.h`
- `br_html.h`
- `br_dns.h`
- `br_main.c`
- `br_start.S`
- `Makefile`
- `build.log` (log da compilação da extração executada)

## Fluxo
```bash
./BrowserRaf/extract_browser.sh
bash BrowserRaf/Browser_extracted.sh
cp ~/.rafaelia/BROWSER/* BrowserRaf/internal/
```
