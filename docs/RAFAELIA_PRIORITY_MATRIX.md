# RAFAELIA · Matriz de voo vetorial para RafGitTools

Objetivo: transformar este repositório em ferramenta de inventário, triagem, hotfix e retroalimentação dos demais repositórios.

## Vetor base

```text
v = (intencao, observacao, ruido, transmutacao, memoria, coerencia, urgencia, latencia, evidencia, obviedade)
```

## Fórmula de prioridade

```text
valor = 0.20*evidencia + 0.18*urgencia + 0.16*memoria + 0.14*transmutacao + 0.12*latencia + 0.08*obviedade + 0.06*intencao + 0.04*ruido + 0.02*coerencia
```

## Papel do repositório

RafGitTools deve funcionar como camada de varredura e organização:

1. listar arquivos;
2. detectar arquivos sem extensão;
3. classificar código/texto/binário;
4. procurar TODO/FIXME/erro/log;
5. gerar matriz `RAFAELIA_PRIORITY_MATRIX.md` preenchida;
6. apontar hotfixes urgentes;
7. criar relatórios reprodutíveis.

## Prioridades

| Camada | Aplicação | Critério de prova |
|---|---|---|
| Evidência | scripts de scan executáveis | relatório gerado em repo real |
| Urgência | detectar gargalos que bloqueiam build/run | issue/hotfix criado com caminho exato |
| Latência | arquivos sem extensão e textos órfãos | conteúdo classificado sem apagar nada |
| Obviedade | comando único e README claro | usuário roda sem interpretar o código |
| Memória | consolidar resultados entre repos | índice central com links e status |

## Estados

```text
[SCAN_OK]       varredura executou
[ARQUIVO_ORFAO] arquivo sem índice/função clara
[HOTFIX]        correção urgente sugerida
[DOC_GAP]       falta documentação mínima
[BUILD_GAP]     falta comando de build/test
[VALIDADO]      saída conferida em repo real
```

## Próximo ciclo

1. Criar script `raf_scan_repo.sh`.
2. Criar gerador de relatório Markdown.
3. Detectar arquivos sem extensão e textos soltos.
4. Criar scoring automático inicial.
5. Usar o resultado nos repositórios UserLAnd2, BLAKE3, llamaRafaelia e RLL.

## Retroalimentar[3]

- F_ok: RafGitTools vira o braço operacional da matriz.
- F_gap: falta implementar o scanner real.
- F_next: criar script de inventário determinístico com saída Markdown.
