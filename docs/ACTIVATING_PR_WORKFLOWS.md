# 🔧 Activating Pull Request Workflows / Ativando Workflows de Pull Request

[English](#english) | [Português](#português)

---

## English

### Overview

This guide explains how to activate and configure the Pull Request workflows for the RafGitTools repository. All the workflow files are already in place, but some GitHub repository settings need to be configured for full functionality.

### Prerequisites

You need **Admin** or **Maintainer** access to the repository to configure these settings.

### Step 1: Enable GitHub Actions

1. Go to your repository on GitHub
2. Click **Settings** tab
3. Click **Actions** → **General** in the left sidebar
4. Under **Actions permissions**, select:
   - ✅ **Allow all actions and reusable workflows**
5. Under **Workflow permissions**, select:
   - ✅ **Read and write permissions**
   - ✅ Check **Allow GitHub Actions to create and approve pull requests**
6. Click **Save**

### Step 2: Configure Branch Protection Rules

Protect your main branch to ensure all PRs are properly validated:

1. Go to **Settings** → **Branches**
2. Click **Add branch protection rule**
3. Configure as follows:

**Branch name pattern**: `main`

**Protect matching branches**:
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: `1` (or more)
  - ✅ Dismiss stale pull request approvals when new commits are pushed
  - ✅ Require review from Code Owners (optional)
- ✅ **Require status checks to pass before merging**
  - ✅ Require branches to be up to date before merging
  - Add these required status checks:
    - `Build and Test (devDebug)`
    - `Build and Test (productionRelease)`
    - `Unit Tests`
    - `Lint Check`
    - `Validate Pull Request`
- ✅ **Require conversation resolution before merging**
- ✅ **Require signed commits** (optional, recommended for security)
- ✅ **Require linear history** (optional, recommended)
- ✅ **Include administrators** (optional, enforces rules on admins too)

4. Click **Create** or **Save changes**

### Step 3: Configure Required Secrets (Optional)

For full functionality, configure these secrets in **Settings** → **Secrets and variables** → **Actions**:

#### Optional Secrets:
- `PLAY_STORE_SERVICE_ACCOUNT_JSON` - For automated Play Store deployment
  - Only needed if you want to publish to Google Play automatically

**Note**: `GITHUB_TOKEN` is automatically provided by GitHub Actions.

### Step 4: Configure Dependabot

Dependabot is already configured, but verify it's enabled:

1. Go to **Settings** → **Code security and analysis**
2. Enable:
   - ✅ **Dependency graph** (should be enabled by default)
   - ✅ **Dependabot alerts**
   - ✅ **Dependabot security updates**

### Step 5: Verify Workflow Files

All workflow files are in `.github/workflows/`. Verify they're present:

```bash
.github/workflows/
├── ci.yml                 # Main CI pipeline
├── coverage.yml           # Code coverage
├── docs.yml              # Documentation validation
├── nightly.yml           # Nightly builds
├── performance.yml       # Performance metrics
├── pr-validation.yml     # PR validation
├── release.yml           # Release automation
├── security.yml          # Security scanning
└── stale.yml            # Stale issue/PR management
```

### Step 6: Test PR Workflows

Create a test PR to verify everything works:

1. Create a new branch:
   ```bash
   git checkout -b test/workflow-validation
   ```

2. Make a small change (e.g., update README)

3. Commit and push:
   ```bash
   git commit -m "docs: test PR workflows"
   git push origin test/workflow-validation
   ```

4. Create a PR on GitHub

5. Verify that workflows run:
   - Check the **Checks** tab in the PR
   - All workflows should appear and run
   - PR should receive automated comments
   - Labels should be added automatically

### Step 7: Monitor Workflow Runs

Monitor all workflow runs:

1. Go to **Actions** tab in your repository
2. You'll see:
   - All workflow runs
   - Success/failure status
   - Execution time
   - Artifacts generated

### Troubleshooting

#### Workflows Don't Run

**Possible causes:**
1. GitHub Actions not enabled → Check Step 1
2. Workflow files have syntax errors → Check YAML syntax
3. Repository is a fork → Workflows may need manual approval

**Solution:**
```bash
# Validate YAML syntax locally
yamllint .github/workflows/*.yml
```

#### Status Checks Not Appearing in PR

**Possible causes:**
1. Workflows haven't run yet → Wait a few minutes
2. Workflow names don't match protection rules → Update branch protection
3. Workflows failed to start → Check Actions tab for errors

**Solution:** Go to **Settings** → **Branches** and verify required status check names match job names in workflow files.

#### Permission Errors

**Possible causes:**
1. Insufficient workflow permissions → Check Step 1
2. GITHUB_TOKEN lacks permissions → Review workflow permissions

**Solution:** Ensure workflows have `write` permissions where needed.

### Additional Configuration (Optional)

#### Enable Auto-merge

Allow PRs to be auto-merged when all checks pass:

1. Go to **Settings** → **General**
2. Scroll to **Pull Requests**
3. Enable:
   - ✅ **Allow auto-merge**

#### Configure Notifications

Set up notifications for workflow failures:

1. Go to your GitHub profile **Settings**
2. Click **Notifications**
3. Under **Actions**, configure how you want to be notified

#### Add Status Badges to README

Add workflow status badges to your README.md:

```markdown
[![CI](https://github.com/rafaelmeloreisnovo/RafGitTools/workflows/CI/badge.svg)](https://github.com/rafaelmeloreisnovo/RafGitTools/actions?query=workflow%3ACI)
[![Security](https://github.com/rafaelmeloreisnovo/RafGitTools/workflows/Security%20Scan/badge.svg)](https://github.com/rafaelmeloreisnovo/RafGitTools/actions?query=workflow%3A%22Security+Scan%22)
```

See [WORKFLOW_BADGES.md](../.github/WORKFLOW_BADGES.md) for all available badges.

---

## Português

### Visão Geral

Este guia explica como ativar e configurar os workflows de Pull Request para o repositório RafGitTools. Todos os arquivos de workflow já estão em vigor, mas algumas configurações do repositório no GitHub precisam ser ajustadas para funcionalidade completa.

### Pré-requisitos

Você precisa de acesso de **Admin** ou **Maintainer** ao repositório para configurar essas definições.

### Passo 1: Habilitar GitHub Actions

1. Acesse seu repositório no GitHub
2. Clique na aba **Settings**
3. Clique em **Actions** → **General** na barra lateral esquerda
4. Em **Actions permissions**, selecione:
   - ✅ **Allow all actions and reusable workflows**
5. Em **Workflow permissions**, selecione:
   - ✅ **Read and write permissions**
   - ✅ Marque **Allow GitHub Actions to create and approve pull requests**
6. Clique em **Save**

### Passo 2: Configurar Regras de Proteção de Branch

Proteja seu branch principal para garantir que todos os PRs sejam validados adequadamente:

1. Vá para **Settings** → **Branches**
2. Clique em **Add branch protection rule**
3. Configure da seguinte forma:

**Branch name pattern**: `main`

**Protect matching branches**:
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: `1` (ou mais)
  - ✅ Dismiss stale pull request approvals when new commits are pushed
  - ✅ Require review from Code Owners (opcional)
- ✅ **Require status checks to pass before merging**
  - ✅ Require branches to be up to date before merging
  - Adicione estas verificações de status obrigatórias:
    - `Build and Test (devDebug)`
    - `Build and Test (productionRelease)`
    - `Unit Tests`
    - `Lint Check`
    - `Validate Pull Request`
- ✅ **Require conversation resolution before merging**
- ✅ **Require signed commits** (opcional, recomendado para segurança)
- ✅ **Require linear history** (opcional, recomendado)
- ✅ **Include administrators** (opcional, aplica regras aos admins também)

4. Clique em **Create** ou **Save changes**

### Passo 3: Configurar Secrets Necessários (Opcional)

Para funcionalidade completa, configure estes secrets em **Settings** → **Secrets and variables** → **Actions**:

#### Secrets Opcionais:
- `PLAY_STORE_SERVICE_ACCOUNT_JSON` - Para deployment automatizado na Play Store
  - Necessário apenas se você deseja publicar no Google Play automaticamente

**Nota**: `GITHUB_TOKEN` é fornecido automaticamente pelo GitHub Actions.

### Passo 4: Configurar Dependabot

O Dependabot já está configurado, mas verifique se está habilitado:

1. Vá para **Settings** → **Code security and analysis**
2. Habilite:
   - ✅ **Dependency graph** (deve estar habilitado por padrão)
   - ✅ **Dependabot alerts**
   - ✅ **Dependabot security updates**

### Passo 5: Verificar Arquivos de Workflow

Todos os arquivos de workflow estão em `.github/workflows/`. Verifique se estão presentes:

```bash
.github/workflows/
├── ci.yml                 # Pipeline CI principal
├── coverage.yml           # Cobertura de código
├── docs.yml              # Validação de documentação
├── nightly.yml           # Builds noturnos
├── performance.yml       # Métricas de performance
├── pr-validation.yml     # Validação de PR
├── release.yml           # Automação de release
├── security.yml          # Escaneamento de segurança
└── stale.yml            # Gerenciamento de issues/PRs obsoletos
```

### Passo 6: Testar Workflows de PR

Crie um PR de teste para verificar se tudo funciona:

1. Crie um novo branch:
   ```bash
   git checkout -b test/validacao-workflow
   ```

2. Faça uma pequena mudança (ex: atualizar README)

3. Faça commit e push:
   ```bash
   git commit -m "docs: testar workflows de PR"
   git push origin test/validacao-workflow
   ```

4. Crie um PR no GitHub

5. Verifique que os workflows executam:
   - Verifique a aba **Checks** no PR
   - Todos os workflows devem aparecer e executar
   - PR deve receber comentários automatizados
   - Labels devem ser adicionadas automaticamente

### Passo 7: Monitorar Execuções de Workflow

Monitore todas as execuções de workflow:

1. Vá para a aba **Actions** no seu repositório
2. Você verá:
   - Todas as execuções de workflow
   - Status de sucesso/falha
   - Tempo de execução
   - Artefatos gerados

### Solução de Problemas

#### Workflows Não Executam

**Possíveis causas:**
1. GitHub Actions não habilitado → Verifique Passo 1
2. Arquivos de workflow têm erros de sintaxe → Verifique sintaxe YAML
3. Repositório é um fork → Workflows podem precisar aprovação manual

**Solução:**
```bash
# Validar sintaxe YAML localmente
yamllint .github/workflows/*.yml
```

#### Status Checks Não Aparecem no PR

**Possíveis causas:**
1. Workflows ainda não executaram → Aguarde alguns minutos
2. Nomes dos workflows não correspondem às regras de proteção → Atualize proteção de branch
3. Workflows falharam ao iniciar → Verifique aba Actions para erros

**Solução:** Vá para **Settings** → **Branches** e verifique se os nomes dos status checks obrigatórios correspondem aos nomes dos jobs nos arquivos de workflow.

#### Erros de Permissão

**Possíveis causas:**
1. Permissões de workflow insuficientes → Verifique Passo 1
2. GITHUB_TOKEN não tem permissões → Revise permissões do workflow

**Solução:** Garanta que workflows tenham permissões de `write` onde necessário.

### Configuração Adicional (Opcional)

#### Habilitar Auto-merge

Permitir que PRs sejam mesclados automaticamente quando todas as verificações passarem:

1. Vá para **Settings** → **General**
2. Role até **Pull Requests**
3. Habilite:
   - ✅ **Allow auto-merge**

#### Configurar Notificações

Configure notificações para falhas de workflow:

1. Vá para **Settings** do seu perfil GitHub
2. Clique em **Notifications**
3. Em **Actions**, configure como deseja ser notificado

#### Adicionar Status Badges ao README

Adicione badges de status de workflow ao seu README.md:

```markdown
[![CI](https://github.com/rafaelmeloreisnovo/RafGitTools/workflows/CI/badge.svg)](https://github.com/rafaelmeloreisnovo/RafGitTools/actions?query=workflow%3ACI)
[![Security](https://github.com/rafaelmeloreisnovo/RafGitTools/workflows/Security%20Scan/badge.svg)](https://github.com/rafaelmeloreisnovo/RafGitTools/actions?query=workflow%3A%22Security+Scan%22)
```

Veja [WORKFLOW_BADGES.md](../.github/WORKFLOW_BADGES.md) para todos os badges disponíveis.

---

## 📊 Verification Checklist / Checklist de Verificação

After completing all steps, verify:
Após completar todos os passos, verifique:

- [ ] GitHub Actions enabled / GitHub Actions habilitado
- [ ] Workflow permissions set to read/write / Permissões de workflow definidas para leitura/escrita
- [ ] Branch protection rules configured / Regras de proteção de branch configuradas
- [ ] Required status checks added / Verificações de status obrigatórias adicionadas
- [ ] Dependabot enabled / Dependabot habilitado
- [ ] Test PR created and workflows ran successfully / PR de teste criado e workflows executaram com sucesso
- [ ] Automated PR comments working / Comentários automatizados de PR funcionando
- [ ] Labels automatically added / Labels adicionadas automaticamente
- [ ] Artifacts uploaded / Artefatos enviados

## 🎯 Success Indicators / Indicadores de Sucesso

Your PR workflows are fully activated when:
Seus workflows de PR estão totalmente ativados quando:

✅ PRs automatically trigger workflows
✅ Status checks appear in PRs
✅ Automated comments posted
✅ Labels added automatically
✅ Artifacts uploaded to Actions tab
✅ Security scans run and report
✅ Coverage reports generated

---

## 📚 Additional Resources / Recursos Adicionais

- [PR Guide](PR_GUIDE.md) - How to create and work with PRs
- [Workflow Documentation](../.github/workflows/README.md) - Detailed workflow info
- [Contributing Guide](../CONTRIBUTING.md) - Contribution guidelines
- [Workflow Badges](../.github/WORKFLOW_BADGES.md) - Status badges

---

**Repository Administrator Guide / Guia do Administrador do Repositório** 🔧
