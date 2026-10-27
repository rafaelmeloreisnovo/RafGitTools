# RafYMLC — compilador YAML restrito para C (2026-10-25)

**Fonte:** [rafymlc.py](rafymlc.py). O compilador é Python hospedado (stdlib); gera fontes C e validação no alvo, declaradas como freestanding, mas a compilação final não foi executada nesta etapa.

**Entrada:** subconjunto controlado de YAML, com limites de profundidade, tamanho e nós, não um parser universal. **Saídas:** arquivos .generated.h e .generated.c, e receipt JSON de geração com claim_allowed=false.

**Gates seguintes:** testar rejeição de YAML desconhecido, casos limites e escaping; construir os arquivos C sem libc no HEAD exato, conferir importações e link ABI, reproduzir digests. Geração não implica EXECUTION_PASS.

**Governança:** conservar fonte, licenças, revisões e rollback. Estado documental HOLD.
