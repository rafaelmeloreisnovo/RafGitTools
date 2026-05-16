# BrowserRaf

- `Browser.sh`: arquivo bruto extraído do repositório `termux-app-rafacodephi` (`master/Browser.sh`).
- `Browser_hotfix.sh`: versão corrigida para execução direta (remove envelope `cat > /tmp/browser.txt << 'OUTER_SCRIPT'`, marcador `OUTER_SCRIPT` e lixo final).

## Teste rápido
```bash
bash BrowserRaf/Browser_hotfix.sh
```

Saída esperada: geração dos arquivos em `~/.rafaelia/BROWSER` e etapa de compilação (pode falhar em toolchain local, com log em `~/.rafaelia/BROWSER/build.log`).
