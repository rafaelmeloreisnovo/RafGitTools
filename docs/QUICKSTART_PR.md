# 🚀 Quick Start: Pull Requests / Início Rápido: Pull Requests

**For contributors** | **Para contribuidores**

---

## English Version

### Create a Pull Request in 7 Steps

```bash
# 1. Fork the repository on GitHub (use the Fork button)

# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/RafGitTools.git
cd RafGitTools

# 3. Create a branch
git checkout -b feature/my-feature

# 4. Make changes and commit
git add .
git commit -m "feat: add my feature"

# 5. Push to GitHub
git push origin feature/my-feature

# 6. Go to GitHub and click "Compare & pull request"

# 7. Fill in the PR template and submit
```

### PR Title Format

**Must follow this format:**
```
type: description

Examples:
✅ feat: add user authentication
✅ fix: resolve crash on startup
✅ docs: update README
❌ Added some changes (WRONG - will fail validation)
```

**Valid types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation
- `style:` - Code style
- `refactor:` - Refactoring
- `test:` - Tests
- `chore:` - Maintenance

### What Happens After You Submit?

Automatically:
1. ✅ **Build** - Your code is built
2. ✅ **Test** - Unit tests run
3. ✅ **Lint** - Code style checked
4. ✅ **Security** - Vulnerabilities scanned
5. ✅ **Labels** - Added automatically
6. ✅ **Comment** - Results posted

**Wait 5-10 minutes for all checks to complete.**

### If Checks Fail

Run these locally to fix issues:
```bash
# Build
./gradlew assembleDevDebug

# Test
./gradlew testDevDebugUnitTest

# Lint
./gradlew lintDevDebug
```

### Getting Help

- 📖 [Full PR Guide](PR_GUIDE.md)
- 🤝 [Contributing Guide](../CONTRIBUTING.md)
- 💬 Open an issue with `help wanted` label

---

## Versão em Português

### Criar Pull Request em 7 Passos

```bash
# 1. Faça Fork do repositório no GitHub (use o botão Fork)

# 2. Clone seu fork
git clone https://github.com/SEU_USUARIO/RafGitTools.git
cd RafGitTools

# 3. Crie um branch
git checkout -b feature/minha-funcionalidade

# 4. Faça mudanças e commit
git add .
git commit -m "feat: adiciona minha funcionalidade"

# 5. Envie para o GitHub
git push origin feature/minha-funcionalidade

# 6. Vá ao GitHub e clique em "Compare & pull request"

# 7. Preencha o template do PR e submeta
```

### Formato do Título do PR

**Deve seguir este formato:**
```
tipo: descrição

Exemplos:
✅ feat: adiciona autenticação de usuário
✅ fix: resolve crash ao iniciar
✅ docs: atualiza README
❌ Adicionei algumas mudanças (ERRADO - falhará na validação)
```

**Tipos válidos:**
- `feat:` - Nova funcionalidade
- `fix:` - Correção de bug
- `docs:` - Documentação
- `style:` - Estilo de código
- `refactor:` - Refatoração
- `test:` - Testes
- `chore:` - Manutenção

### O Que Acontece Depois de Submeter?

Automaticamente:
1. ✅ **Build** - Seu código é compilado
2. ✅ **Teste** - Testes unitários executam
3. ✅ **Lint** - Estilo de código verificado
4. ✅ **Segurança** - Vulnerabilidades escaneadas
5. ✅ **Labels** - Adicionadas automaticamente
6. ✅ **Comentário** - Resultados postados

**Aguarde 5-10 minutos para todas as verificações completarem.**

### Se as Verificações Falharem

Execute estes comandos localmente para corrigir problemas:
```bash
# Build
./gradlew assembleDevDebug

# Teste
./gradlew testDevDebugUnitTest

# Lint
./gradlew lintDevDebug
```

### Obtendo Ajuda

- 📖 [Guia Completo de PR](PR_GUIDE.md)
- 🤝 [Guia de Contribuição](../CONTRIBUTING.md)
- 💬 Abra uma issue com label `help wanted`

---

## 📋 PR Checklist

Before submitting / Antes de submeter:

- [ ] Title follows format: `type: description`
- [ ] Branch is up to date with main
- [ ] Tests pass locally
- [ ] Code follows style guidelines
- [ ] PR description is complete

---

## 🎯 Quick Commands

```bash
# Update your branch from main
git fetch upstream
git rebase upstream/main

# Run all checks locally
./gradlew clean build test lint

# View commit history
git log --oneline -10

# Amend last commit
git commit --amend

# Force push (after rebase)
git push -f origin your-branch
```

---

## 🔗 Important Links

- **Repository**: https://github.com/rafaelmeloreisnovo/RafGitTools
- **Issues**: https://github.com/rafaelmeloreisnovo/RafGitTools/issues
- **Actions**: https://github.com/rafaelmeloreisnovo/RafGitTools/actions
- **Workflows**: [.github/workflows/README.md](../.github/workflows/README.md)

---

**Happy Contributing! 🚀 | Boa Contribuição! 🚀**
