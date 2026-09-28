package com.rafgittools.setupwizard

object SetupWizardCatalog {
    val steps: List<WizardStep> = listOf(
        WizardStep(
            id = "WELCOME",
            title = "Bem-vindo ao RafGitTools",
            summary = "Antes de ativar qualquer capacidade, você verá o que ela faz, quais dados toca, quais riscos existem e como voltar atrás.",
            enables = listOf(
                "Configuração guiada e revisável.",
                "Escolhas explícitas: concordar, discordar ou decidir depois.",
                "Nenhuma ação privilegiada é escondida no fluxo."
            ),
            risks = listOf("Nenhum recurso externo é ativado nesta etapa."),
            dataTouched = listOf("Somente o estado local do próprio wizard."),
            rollback = "Pode sair do wizard sem ativar capacidades.",
            zeroTrustRule = "Nada é confiado ou ativado por padrão.",
            risk = WizardRisk.LOW
        ),
        WizardStep(
            id = "PRIVACY",
            title = "Privacidade e governança de dados",
            summary = "O aplicativo separa configuração, execução, evidência e alegação. Segredos não entram em receipts, documentação ou mapa federado.",
            enables = listOf(
                "Armazenamento privado do app para preferências do wizard.",
                "Minimização de dados por padrão.",
                "Revisão posterior de decisões."
            ),
            risks = listOf(
                "Ativar integrações externas pode expor metadados ao provedor correspondente.",
                "A política do provedor externo continua aplicável fora do app."
            ),
            dataTouched = listOf("Decisão do usuário e identificador da etapa; nunca o valor de tokens, PATs ou senhas."),
            rollback = "Decisões locais podem ser revertidas pelo próprio wizard; efeitos externos exigem rollback específico e comprovado.",
            zeroTrustRule = "Segredo presente não significa permissão comprovada.",
            risk = WizardRisk.MEDIUM
        ),
        WizardStep(
            id = "IDENTITY_AND_ACCESS",
            title = "Identidade, autenticação e acesso",
            summary = "Cada credencial é tratada como uma capacidade separada. O app não troca automaticamente uma credencial por outra.",
            enables = listOf(
                "Vincular credenciais a funções específicas.",
                "Exigir escopo mínimo e alvo explícito.",
                "Bloquear fallback silencioso entre credenciais."
            ),
            risks = listOf(
                "Escopo excessivo aumenta impacto potencial de uma credencial comprometida.",
                "Permissões desconhecidas permanecem TOKEN_VAZIO até readback."
            ),
            dataTouched = listOf("Metadados de capacidade e estado; valores secretos permanecem fora do ledger."),
            rollback = "Remover/desativar uma vinculação local; revogação da credencial é feita no provedor quando aplicável.",
            zeroTrustRule = "CAPABILITY != PERMISSION != EXECUTION.",
            risk = WizardRisk.HIGH
        ),
        WizardStep(
            id = "EXTERNAL_MODULES",
            title = "Módulos e serviços externos",
            summary = "Cada módulo externo informa origem, finalidade, dados usados, permissões, dependências e evidência antes de ser habilitado.",
            enables = listOf(
                "Conectar módulos aprovados individualmente.",
                "Manter módulos desligados até decisão explícita.",
                "Auditar origem e versão."
            ),
            risks = listOf(
                "Módulos externos podem ter disponibilidade, políticas e superfícies de ataque próprias.",
                "Atualizações podem mudar comportamento e permissões."
            ),
            dataTouched = listOf("Somente dados declarados na ficha do módulo; ausência de declaração bloqueia a ativação."),
            rollback = "Desabilitar o módulo e restaurar a configuração anterior quando o módulo declarar rollback suportado.",
            zeroTrustRule = "Origem conhecida não equivale a comportamento confiável; verificar versão, escopo e readback.",
            risk = WizardRisk.HIGH
        ),
        WizardStep(
            id = "READ_WRITE_BOUNDARY",
            title = "Leitura, escrita e ações de alto impacto",
            summary = "Leituras e inspeções são separadas de mutações. Escritas exigem confirmação explícita, alvo exato, evidência e caminho de rollback quando possível.",
            enables = listOf(
                "Operações somente leitura por padrão.",
                "Confirmação separada para mudanças remotas.",
                "Bloqueio de force-push, deleção ou alteração de proteção sem política específica."
            ),
            risks = listOf(
                "Escritas remotas podem alterar histórico, permissões, branches ou artefatos.",
                "Nem toda operação externa é reversível."
            ),
            dataTouched = listOf("Alvo da operação, tipo de mudança, estado anterior quando disponível e receipt."),
            rollback = "Rollback só é oferecido quando há estado anterior verificável e operação inversa segura.",
            zeroTrustRule = "Nenhuma mutação nasce autorizada apenas por estar disponível na interface.",
            risk = WizardRisk.HIGH
        ),
        WizardStep(
            id = "AUDIT_AND_CUSTODY",
            title = "Rastreabilidade, auditoria e cadeia de custódia",
            summary = "Cada decisão do wizard gera um evento local encadeado. A execução real continua exigindo sua própria evidência.",
            enables = listOf(
                "Histórico de concordâncias, divergências e adiamentos.",
                "Encadeamento por hash para detectar alteração do ledger local.",
                "Separação entre intenção e execução."
            ),
            risks = listOf("O ledger local prova o registro local da decisão, não prova execução no provedor."),
            dataTouched = listOf("Etapa, decisão, tipo de evento, horário, hash anterior e hash atual."),
            rollback = "Correções são novos eventos; histórico anterior não é reescrito.",
            zeroTrustRule = "INTENT != EXECUTION != EVIDENCE != CLAIM.",
            risk = WizardRisk.MEDIUM
        ),
        WizardStep(
            id = "AUTOMATION",
            title = "Automação com limites claros",
            summary = "Automação só entra depois de uma operação estar padronizada, delimitada e testada. Tarefas privilegiadas continuam com gates humanos quando definidos.",
            enables = listOf(
                "Automatizar rotinas repetíveis e bem definidas.",
                "Registrar origem, alvo e resultado.",
                "Parar automaticamente quando pré-condições faltarem."
            ),
            risks = listOf(
                "Automação amplia velocidade e também pode ampliar erros.",
                "Mudanças de contexto podem invalidar uma regra antiga."
            ),
            dataTouched = listOf("Metadados necessários para a tarefa e receipts; minimização por padrão."),
            rollback = "Desativar a automação e executar rollback específico da operação quando existir.",
            zeroTrustRule = "Standardize before automate; validate before scale.",
            risk = WizardRisk.HIGH
        ),
        WizardStep(
            id = "REVIEW_AND_ROLLBACK",
            title = "Revisão e rollback",
            summary = "Você pode voltar ao wizard a qualquer momento para revisar decisões. O app diferencia reverter uma preferência local de reverter uma alteração externa.",
            enables = listOf(
                "Revisar cada escolha.",
                "Reverter preferências locais.",
                "Ver quais efeitos externos exigem procedimento separado."
            ),
            risks = listOf("Rollback externo pode ser impossível ou parcial; o wizard deve dizer isso antes da execução."),
            dataTouched = listOf("Somente histórico de decisões e referências de receipts."),
            rollback = "Preferências locais são reversíveis; operações externas seguem o contrato de rollback da capacidade.",
            zeroTrustRule = "Rollback não é presumido: precisa ser declarado e verificável.",
            risk = WizardRisk.MEDIUM
        ),
        WizardStep(
            id = "FINAL_REVIEW",
            title = "Resumo antes de concluir",
            summary = "Nada é ativado só porque você chegou ao fim. Revise concordâncias, divergências e itens adiados antes de concluir.",
            enables = listOf(
                "Visão final das decisões.",
                "Conclusão sem promover automaticamente capacidades privilegiadas.",
                "Retorno posterior ao wizard."
            ),
            risks = listOf("Capacidades ainda não validadas continuam bloqueadas mesmo se marcadas como desejadas."),
            dataTouched = listOf("Estado final do wizard e receipts locais."),
            rollback = "Reabrir o wizard e registrar novas decisões; alterações externas permanecem sujeitas ao próprio rollback.",
            zeroTrustRule = "Concluir configuração != provar permissão != executar operação.",
            risk = WizardRisk.LOW
        )
    )
}
