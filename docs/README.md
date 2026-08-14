# 📚 Documentation Index / Índice de Documentação

Welcome to the RafGitTools documentation! | Bem-vindo à documentação do RafGitTools!

---

> **Escopo atual / Current scope:** **GitHub + JGit**. Recursos fora desse escopo devem ser tratados como roadmap, estrutura ou stub até implementação funcional.

## 📊 Project Status / Status do Projeto

| Category | Maturity | Status |
|----------|----------|--------|
| Architecture | Funcional em produção | ✅ Stable baseline |
| Git Operations | Funcional parcial | 🟡 Expanding coverage |
| GitHub API | Funcional parcial | 🟡 Expanding endpoints |
| UI/UX | Funcional parcial | 🟡 Incremental delivery |
| Security | Funcional parcial | 🟡 Mixed: implemented + pending |
| Testing | Funcional parcial | 🔴 Coverage still limited |
| Terminal/Multi-platform | Somente estrutura/stub | ⚪ Planned/Stub |

👉 **[Full Status Report](STATUS_REPORT.md)** - Detailed implementation status

## 📏 Métricas Oficiais (fonte única)

**Fonte única de verdade**: [STATUS_REPORT.md](STATUS_REPORT.md)  
**Última atualização**: 2026-02-24

| Métrica | Valor |
|---|---:|
| Total de features | 288 |
| Concluídas | 115 |
| Em progresso | 26 |
| Pendentes | 147 |
| Arquivos Kotlin | 168 |
| Arquivos de teste (.kt em `test`/`androidTest`) | 11 |
| Arquivos de documentação (`docs/**/*.md`) | 36 |

### Como atualizar métricas

1. Atualize **primeiro** `docs/STATUS_REPORT.md` com os novos números e a data.
2. Replique exatamente os mesmos valores em `README.md`, `docs/README.md` e `docs/INDEX.md`.
3. Revalide contagens com comandos locais (`rg --files -g '*.kt'`, `rg --files -g '*.kt' | rg '/(test|androidTest)/'`, `rg --files docs -g '*.md'`).
4. Faça commit único com mensagem clara de atualização de métricas para evitar drift.

---

## 🚀 Getting Started / Começando

### For Contributors / Para Contribuidores

1. **[Quick Start: Pull Requests](QUICKSTART_PR.md)** ⚡
   - Create your first PR in 7 steps
   - Crie seu primeiro PR em 7 passos

2. **[Complete PR Guide](PR_GUIDE.md)** 📖
   - Comprehensive guide for contributors
   - Available in English and Portuguese

3. **[Contributing Guidelines](../CONTRIBUTING.md)** 🤝
   - Code standards and best practices

### For Repository Administrators / Para Administradores

- **[Activating PR Workflows](ACTIVATING_PR_WORKFLOWS.md)** 🔧

---

## 📖 Project Documentation / Documentação do Projeto

### Core Documentation / Documentação Principal

| Document | Description |
|----------|-------------|
| **[Status Report](STATUS_REPORT.md)** | What's ready, in progress, and pending |
| **[Roadmap](ROADMAP.md)** | 288 features with timeline |
| **[Project Overview](PROJECT_OVERVIEW.md)** | Complete project information |
| **[Architecture Guide](ARCHITECTURE.md)** | Technical architecture details |
| **[Build Instructions](BUILD.md)** | How to build the project |
| **[Feature Matrix](FEATURE_MATRIX.md)** | Feature comparison |

### Security, Privacy & Reference Alignment / Segurança, Privacidade e Referências

| Document | Description |
|----------|-------------|
| **[Privacy Design Notice](PRIVACY.md)** | Design boundary; legal/operational review remains required |
| **[Security Design References](SECURITY.md)** | Reference-based security review guidance |
| **[Normative Reference Guide](COMPLIANCE.md)** | Evidence boundary for ISO, NIST and IEEE references |
| **[Claim Language Policy](CLAIM_LANGUAGE_POLICY.md)** | Repository-wide wording and promotion gate |
| **[License Information](LICENSE_INFO.md)** | License metadata and attribution |

### Implementation Details / Detalhes de Implementação

| Document | Description |
|----------|-------------|
| **[Multilingual & Responsive](MULTILINGUAL_RESPONSIVE.md)** | i18n and responsive design |
| **[Implementation Notes](IMPLEMENTATION_NOTES.md)** | Technical implementation details |
| **[Data Flow Security](DATA_FLOW_SECURITY.md)** | Security data flow |

---

## 🎯 Quick Links / Links Rápidos

| I want to... | Quero... | Link |
|--------------|----------|------|
| Contribute code | Contribuir código | [Quick Start PR](QUICKSTART_PR.md) |
| Report a bug | Reportar bug | [Issues](https://github.com/rafaelmeloreisnovo/RafGitTools/issues) |
| Understand architecture | Entender arquitetura | [Architecture Guide](ARCHITECTURE.md) |
| Check what's implemented | Ver o que está pronto | [Status Report](STATUS_REPORT.md) |
| Build the project | Compilar o projeto | [Build Instructions](BUILD.md) |

---

## 🌐 Languages / Idiomas

| Language | Status |
|----------|--------|
| 🇺🇸 English | ✅ Complete |
| 🇧🇷 Português (Brasil) | ✅ Complete |
| 🇪🇸 Español | ✅ Complete |

---

**Last Updated**: 2026-02-24  
**Progress**: Qualitative maturity view; numeric totals remain in `STATUS_REPORT.md`.  
**Maintained by**: RafGitTools Team

**Happy Contributing! 🚀 | Boa Contribuição! 🚀**
