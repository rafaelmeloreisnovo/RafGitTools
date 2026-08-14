# 🔄 Pull Request Guide / Guia de Pull Request

[English](#english) | [Português](#português)

---

## English

### What is a Pull Request?

A Pull Request (PR) is a way to propose changes to the codebase. It allows other developers to review your code before it's merged into the main branch.

### How Pull Requests Work in RafGitTools

This repository has comprehensive GitHub Actions workflows that automatically:
- ✅ Build your code on all variants
- ✅ Run unit tests
- ✅ Check code style (lint)
- ✅ Scan for security vulnerabilities
- ✅ Validate PR title format
- ✅ Add labels automatically
- ✅ Comment results on your PR

### Creating Your First Pull Request

#### Step 1: Fork the Repository
1. Go to https://github.com/rafaelmeloreisnovo/RafGitTools
2. Click the **Fork** button in the top right
3. This creates your own copy of the repository

#### Step 2: Clone Your Fork
```bash
git clone https://github.com/YOUR_USERNAME/RafGitTools.git
cd RafGitTools
```

#### Step 3: Create a Branch
```bash
# Create a new branch for your changes
git checkout -b feature/my-awesome-feature

# Or for a bug fix
git checkout -b fix/bug-description
```

#### Step 4: Make Your Changes
1. Edit files in your favorite editor
2. Follow the coding standards in [CONTRIBUTING.md](../CONTRIBUTING.md)
3. Test your changes locally

#### Step 5: Commit Your Changes
```bash
# Stage your changes
git add .

# Commit with a conventional commit message
git commit -m "feat: add awesome new feature"
```

**Commit Message Format:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes
- `refactor:` - Code refactoring
- `test:` - Test additions
- `chore:` - Maintenance tasks

#### Step 6: Push to GitHub
```bash
git push origin feature/my-awesome-feature
```

#### Step 7: Create Pull Request
1. Go to your fork on GitHub
2. Click **Compare & pull request** button
3. Fill in the PR template:
   - Clear description of changes
   - Link related issues
   - Check appropriate change types
   - Complete the checklist
4. Click **Create pull request**

### Understanding PR Workflows

When you create a PR, these workflows run automatically:

#### 1. PR Validation (`pr-validation.yml`)
- ✅ Validates PR title follows conventional commits
- ✅ Builds devDebug variant
- ✅ Runs unit tests
- ✅ Runs lint checks
- ✅ Adds appropriate labels
- ✅ Posts comment with results

#### 2. CI Workflow (`ci.yml`)
- ✅ Builds all 4 variants
- ✅ Runs comprehensive tests
- ✅ Performs lint checks
- ✅ Runs code quality checks
- ✅ Uploads build artifacts

#### 3. Security Scan (`security.yml`)
- ✅ CodeQL analysis
- ✅ Dependency vulnerability scan
- ✅ Secret scanning
- ✅ License metadata/policy check

#### 4. Code Coverage (`coverage.yml`)
- ✅ Measures test coverage
- ✅ Generates coverage report
- ✅ Comments coverage on PR

#### 5. Performance Check (`performance.yml`)
- ✅ Analyzes APK size
- ✅ Measures build time
- ✅ Checks method count

### PR Checklist

Before submitting your PR, ensure:

- [ ] Code follows project style guidelines
- [ ] All tests pass locally
- [ ] New tests added for new features
- [ ] Documentation updated if needed
- [ ] Commit messages follow conventional format
- [ ] PR title follows format: `type: description`
- [ ] PR description is complete
- [ ] No security vulnerabilities introduced
- [ ] Branch is up to date with main

### Common PR Issues and Solutions

#### Issue: PR Title Validation Fails
**Solution:** Use conventional commit format
```
✅ Good: feat: add user authentication
❌ Bad: added some stuff
```

#### Issue: Build Fails
**Solution:** Run locally first
```bash
./gradlew assembleDevDebug
./gradlew testDevDebugUnitTest
./gradlew lintDevDebug
```

#### Issue: Merge Conflicts
**Solution:** Update your branch
```bash
git fetch upstream
git rebase upstream/main
# Resolve conflicts
git push -f origin your-branch
```

### Getting Help

If you need help with PRs:
1. Check [CONTRIBUTING.md](../CONTRIBUTING.md)
2. Review [.github/workflows/README.md](../.github/workflows/README.md)
3. Open an issue with label `help wanted`
4. Ask in GitHub Discussions

---

## Português

### O que é um Pull Request?

Um Pull Request (PR) é uma forma de propor mudanças no código. Permite que outros desenvolvedores revisem seu código antes de ser mesclado no branch principal.

### Como os Pull Requests Funcionam no RafGitTools

Este repositório possui workflows abrangentes do GitHub Actions que automaticamente:
- ✅ Compilam seu código em todas as variantes
- ✅ Executam testes unitários
- ✅ Verificam o estilo do código (lint)
- ✅ Escaneiam vulnerabilidades de segurança
- ✅ Validam o formato do título do PR
- ✅ Adicionam labels automaticamente
- ✅ Comentam os resultados no seu PR

### Criando Seu Primeiro Pull Request

#### Passo 1: Faça um Fork do Repositório
1. Acesse https://github.com/rafaelmeloreisnovo/RafGitTools
2. Clique no botão **Fork** no canto superior direito
3. Isso cria sua própria cópia do repositório

#### Passo 2: Clone Seu Fork
```bash
git clone https://github.com/SEU_USUARIO/RafGitTools.git
cd RafGitTools
```

#### Passo 3: Crie um Branch
```bash
# Crie um novo branch para suas mudanças
git checkout -b feature/minha-funcionalidade

# Ou para correção de bug
git checkout -b fix/descricao-do-bug
```

#### Passo 4: Faça Suas Mudanças
1. Edite os arquivos no seu editor favorito
2. Siga os padrões de código em [CONTRIBUTING.md](../CONTRIBUTING.md)
3. Teste suas mudanças localmente

#### Passo 5: Faça Commit das Suas Mudanças
```bash
# Adicione suas mudanças
git add .

# Faça commit com mensagem no formato conventional
git commit -m "feat: adiciona nova funcionalidade incrível"
```

**Formato da Mensagem de Commit:**
- `feat:` - Nova funcionalidade
- `fix:` - Correção de bug
- `docs:` - Mudanças na documentação
- `style:` - Mudanças de estilo de código
- `refactor:` - Refatoração de código
- `test:` - Adição de testes
- `chore:` - Tarefas de manutenção

#### Passo 6: Envie para o GitHub
```bash
git push origin feature/minha-funcionalidade
```

#### Passo 7: Crie o Pull Request
1. Vá para seu fork no GitHub
2. Clique no botão **Compare & pull request**
3. Preencha o template do PR:
   - Descrição clara das mudanças
   - Vincule issues relacionadas
   - Marque os tipos de mudança apropriados
   - Complete o checklist
4. Clique em **Create pull request**

### Entendendo os Workflows do PR

Quando você cria um PR, estes workflows executam automaticamente:

#### 1. Validação do PR (`pr-validation.yml`)
- ✅ Valida que o título do PR segue conventional commits
- ✅ Compila a variante devDebug
- ✅ Executa testes unitários
- ✅ Executa verificações de lint
- ✅ Adiciona labels apropriadas
- ✅ Posta comentário com resultados

#### 2. Workflow de CI (`ci.yml`)
- ✅ Compila todas as 4 variantes
- ✅ Executa testes abrangentes
- ✅ Realiza verificações de lint
- ✅ Executa verificações de qualidade de código
- ✅ Faz upload dos artefatos de build

#### 3. Escaneamento de Segurança (`security.yml`)
- ✅ Análise CodeQL
- ✅ Escaneamento de vulnerabilidades em dependências
- ✅ Escaneamento de secrets
- ✅ Verificação de conformidade de licenças

#### 4. Cobertura de Código (`coverage.yml`)
- ✅ Mede a cobertura de testes
- ✅ Gera relatório de cobertura
- ✅ Comenta cobertura no PR

#### 5. Verificação de Performance (`performance.yml`)
- ✅ Analisa tamanho do APK
- ✅ Mede tempo de build
- ✅ Verifica contagem de métodos

### Checklist do PR

Antes de submeter seu PR, garanta que:

- [ ] Código segue as diretrizes de estilo do projeto
- [ ] Todos os testes passam localmente
- [ ] Novos testes adicionados para novas funcionalidades
- [ ] Documentação atualizada se necessário
- [ ] Mensagens de commit seguem formato convencional
- [ ] Título do PR segue formato: `tipo: descrição`
- [ ] Descrição do PR está completa
- [ ] Nenhuma vulnerabilidade de segurança introduzida
- [ ] Branch está atualizado com main

### Problemas Comuns em PRs e Soluções

#### Problema: Validação do Título do PR Falha
**Solução:** Use formato de conventional commit
```
✅ Bom: feat: adiciona autenticação de usuário
❌ Ruim: adicionei umas coisas
```

#### Problema: Build Falha
**Solução:** Execute localmente primeiro
```bash
./gradlew assembleDevDebug
./gradlew testDevDebugUnitTest
./gradlew lintDevDebug
```

#### Problema: Conflitos de Merge
**Solução:** Atualize seu branch
```bash
git fetch upstream
git rebase upstream/main
# Resolva os conflitos
git push -f origin seu-branch
```

### Obtendo Ajuda

Se você precisar de ajuda com PRs:
1. Verifique [CONTRIBUTING.md](../CONTRIBUTING.md)
2. Revise [.github/workflows/README.md](../.github/workflows/README.md)
3. Abra uma issue com a label `help wanted`
4. Pergunte no GitHub Discussions

---

## 📊 Workflow Status

Check the status of all workflows:
- **Actions Tab**: https://github.com/rafaelmeloreisnovo/RafGitTools/actions
- **Security Tab**: https://github.com/rafaelmeloreisnovo/RafGitTools/security

## 🎯 Best Practices

1. **Keep PRs Small**: Easier to review and merge
2. **One Feature Per PR**: Don't mix multiple features
3. **Write Tests**: Always include tests for new features
4. **Update Documentation**: Keep docs in sync with code
5. **Respond to Reviews**: Address feedback promptly
6. **Be Patient**: Reviews take time

## 🏆 PR Labels

PRs are automatically labeled based on files changed:
- `documentation` - Changes to docs
- `code` - Changes to source code
- `ui` - Changes to UI/theme
- `tests` - Changes to tests
- `build` - Changes to build config
- `ci/cd` - Changes to workflows
- `dependencies` - Dependency updates

---

**Happy Contributing! / Boa Contribuição!** 🚀
