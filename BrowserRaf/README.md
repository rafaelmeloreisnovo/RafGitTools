# BrowserRaf

Diretório para **extrair e testar** o `Browser.sh` de origem.

## Arquivos
- `Browser.sh`: arquivo bruto baixado de `termux-app-rafacodephi/master/Browser.sh`.
- `extract_browser.sh`: extrai o script interno executável para um arquivo `.sh` limpo.
- `Browser_extracted.sh`: resultado da extração (gerado localmente e versionado para uso direto).

## Como extrair
```bash
./BrowserRaf/extract_browser.sh
```

## Como testar
```bash
bash BrowserRaf/Browser_extracted.sh
```
