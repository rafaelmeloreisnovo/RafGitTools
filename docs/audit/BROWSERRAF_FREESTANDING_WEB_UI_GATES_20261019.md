# BrowserRaf — navegador freestanding para aplicações web, não API

## Intenção

Abrir `https://chatgpt.com` como **website**, por navegação normal com uma pessoa operando a interface. **Não** implementar um cliente OpenAI API, **não** usar token de API e **não** chamar endpoints internos não documentados do ChatGPT para imitar o navegador.

Código de execução alvo: C/Assembly freestanding, sem libc/WebView/Chromium/OpenSSL/libcurl ou motor externo embutido. Código de teste/compilador CI pode usar ferramentas hospedadas; isso NÃO é uma dependência do binário final. Linux/Android e servidor remoto são dependências operacionais inevitáveis para rede; "bare-metal estrito" é diferente de "freestanding em cima de syscalls".

## Fonte e estado

- Produtor do módulo ativo: `RafGitTools/BrowserRaf/internal/`. O `termux-app-rafacodephi/Browser.sh` é fonte histórica; materialização canônica por script versionado separado.
- Transporte TLS 1.3 **não implementado completamente** e `HTTPS_FAIL_CLOSED` é invariável. Um ClientHello serializado não prova handshake autenticado.
- Renderização existente: texto HTML e estilos ANSI limitados, **não** motor DOM/CSS/JavaScript; aceitar texto HTML não é executar uma SPA.
- A página ChatGPT requer JavaScript e cookies para os fluxos de autenticação, inclusive origens `chatgpt.com`, `openai.com` e `auth.openai.com`. Recursos podem exigir WSS. Fonte: https://help.openai.com/pt-br/articles/7426629-why-cant-i-log-in-to-chatgpt e https://help.openai.com/pt-br/articles/9247338-recomenda%C3%A7%C3%B5es-de-rede-para-erros-do-chatgpt-na-web-e-em-apps
- Nenhuma execução, autenticação, renderização funcional ou transmissão com ChatGPT demonstrada.

## Gates de navegador real (ordem de segurança)

1. **G0**: limites dos buffers, parser de URL/DNS e HTTP wire, interpretação de redirects sem downgrade e anti-injeção terminal/HTML. Testes nativos de host e ABI; HTML bruto não pode executar comandos de terminal. Reprovar em dados truncados.
2. **G1**: TLS 1.3 completo, autenticação PKIX, hostname, transcript, HKDF, key_share, Finished, AEAD, anti-replay, ALPN e interop negativa. Sem chave, sem HTTP sobre TLS não autenticado.
3. **G2**: HTTP moderno real: redirects, headers/corpos fragmentados, Content-Length/chunked corretos, compressão negociada, cookies com escopo Secure/HttpOnly/SameSite, cache e política de origem. Suporte HTTP/2 conforme negociação; protocolos não negociados devem falhar claramente.
4. **G3**: tokenizer HTML evoluir para árvore DOM, CSS/layout, eventos, formulários, navegação e fetch. Separar recursos ativos e passivos, proibir scripts de receber privilégios do kernel.
5. **G4**: interpretador JavaScript compatível com as funcionalidades demandadas pela aplicação real, APIs de browser necessárias e contexto de origem, isolamento de scripts e limite de recursos; não marcar `PASS` apenas por exibir HTML.
6. **G5**: login feito pela pessoa via site oficial, consentimentos/cookies válidos e armazenamento local protegido. Sem coletar credenciais em logs, sem contornar desafios antiabuso, 2FA ou políticas de segurança.
7. **G6**: teste em dispositivo real, com sessão autorizada, funcionamento de entrada, envio, saída e WebSocket quando exigido, incluindo reconexão e expiração de sessão. Nenhuma alegação de compatibilidade antes de prova reproduzível.

## Riscos recentes e prioridade

Este hotfix trata somente o renderizador texto atual: limites `cap==0`, saída sanitizada para controles C0/DEL provenientes da página, `<script>`/`<style>` como raw text não exibível e remoção de atributos do nome da tag. Manter versões e falsificadores.

Pontos ainda detectados em código: `URL_PARSE` trunca silenciosamente host/path e pode aceitar porta malformada; DNS hardcoded `8.8.8.8` e txid fixo; resposta HTTP não distingue header incompleto de corpo e faz leitura com buffer finito; ausência de motor JavaScript, DOM, CSS e WebSocket. Cada um requer testes e contrato de fronteira. **Não ativar HTTPS** para compensar.

## Claim gate

`SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM`.
`IMPLEMENTED_UNTESTED ≠ PASS`.
`HTTPS_READY=false`; `CHATGPT_WEB_READY=false`; `claim_allowed=false`.

PR Draft, NÃO PROMOVER. Revisão humana obrigatória, preservação de `main`, rollback por commit/revert, resultados por exact HEAD no CI.
