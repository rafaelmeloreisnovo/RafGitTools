# RafGitTools Documentation Index

## 📚 Documentation Overview

> **Current scope:** **GitHub + JGit**. Items outside this scope are tracked as roadmap or stub/structure until functional implementation lands.

**Status**: RafGitTools is currently in active development with implementation maturity tracked module-by-module in `docs/STATUS_REPORT.md`.

| Category | Maturity |
|----------|----------|
| Architecture & Infrastructure | ✅ Functional in production |
| Git Operations | 🟡 Functional partial |
| GitHub API | 🟡 Functional partial |
| UI/UX | 🟡 Functional partial |
| Security & Privacy | 🟡 Functional partial |
| Testing | 🟡 Functional partial |
| Terminal Emulation | ⚪ Stub/Structure only |
| Multi-platform | ⚪ Stub/Structure only |
| RAFAELIA Scanner | 🟡 External deterministic tool |

---

## 📏 Unified Metrics Snapshot

**Source of truth**: [STATUS_REPORT.md](STATUS_REPORT.md)  
**Last updated**: 2026-02-24

| Metric | Value |
|---|---:|
| Total features | 288 |
| Completed | 115 |
| In progress | 26 |
| Pending | 147 |
| Kotlin files | 168 |
| Test files (.kt under `test`/`androidTest`) | 11 |
| Documentation files (`docs/**/*.md`) | 38+ |

---

## 🎯 Quick Start

- [README](../README.md) - Project overview and getting started
- [**Status Report**](STATUS_REPORT.md) - **Current implementation status and pending items**
- [Architecture](ARCHITECTURE.md) - System architecture and design
- [Contributing](../CONTRIBUTING.md) - How to contribute
- [RAFAELIA Scanner](RAFAELIA_SCANNER.md) - Deterministic repository scan, priority report, hotfix candidates and latent/orphan files

---

## 📊 Status & Progress

- [**Status Report**](STATUS_REPORT.md) - Detailed status of what's ready, in progress, and pending
- [Roadmap](ROADMAP.md) - **288 features** with standards alignment (ISO, NIST, IEEE, W3C)
- [RAFAELIA Priority Matrix](RAFAELIA_PRIORITY_MATRIX.md) - Vector/tensor criteria for urgency, evidence, latency and obvious-but-critical items

---

## 📋 Project Documentation

### Core Documentation
- [Project Overview](PROJECT_OVERVIEW.md) - Comprehensive project information
- [Architecture Guide](ARCHITECTURE.md) - Detailed architecture documentation
- [Feature Matrix](FEATURE_MATRIX.md) - Feature comparison with source projects
- [Build Instructions](BUILD.md) - How to build the project
- [Build/Release/CI Audit 2026-05-05](AUDIT_BUILD_RELEASE_2026-05-05.md) - Auditoria de Android, Gradle, CMake, NDK, JNI, SDK, JDK e GitHub Actions
- [Assembler/Orquestração HW-SW](ASSEMBLER_ORQUESTRACAO_HARDWARE_SOFTWARE.md) - Material técnico de baixo nível e validação por fórmulas/grafos em CI
- [Toro7D Knowledge Carrier](maths/TORO7D_KNOWLEDGE_CARRIER.md) - Ponte auditável entre a especificação Toro7D, linguagem, entropia, hash, i18n e validação compilável
- [RAFAELIA Scanner](RAFAELIA_SCANNER.md) - Scanner externo para inventário, ranking, hotfix candidates e arquivos latentes
- [RAFAELIA Priority Matrix](RAFAELIA_PRIORITY_MATRIX.md) - Critério vetorial para ordenar prova, urgência, memória, latência e obviedade

### Security & Compliance
- [Privacy Policy](PRIVACY.md) - Privacy practices and data protection (GDPR/CCPA compliant)
- [Security Policy](SECURITY.md) - Security standards and practices
- [Compliance Guide](COMPLIANCE.md) - Standards compliance (ISO 27001, NIST CSF)
- [License Information](LICENSE_INFO.md) - License compliance and attribution

### Development Resources
- [Contributing Guide](../CONTRIBUTING.md) - Contribution guidelines
- [Implementation Notes](IMPLEMENTATION_NOTES.md) - Technical implementation details
- [Mapa código → documentação](CODE_TO_DOC_MAP.md) - Relação entre módulos de código e documentos

---

## 🌍 Internationalization

RafGitTools supports multiple languages:

| Language | Status |
|----------|--------|
| English (en) | ✅ Complete |
| Portuguese (pt-BR) | ✅ Complete |
| Spanish (es) | ✅ Complete |

See [Translation Guide](i18n/TRANSLATION_GUIDE.md) for information about adding new languages.

---

## ✅ What's Implemented

### Git Operations (via JGit)
- Clone (full, shallow, single-branch, with submodules)
- Commit, Push, Pull, Fetch
- Branch management (create, delete, checkout, rename)
- Merge, Rebase, Cherry-pick
- Stash operations
- Tag management
- Diff, Blame, Reflog
- Reset, Revert, Clean

### GitHub API (via Retrofit)
- Repository management (list, search, fork, star)
- Issues (list, create, edit, comments, reactions)
- Pull Requests (list, create, merge, reviews)
- Commits, Branches, Releases
- Notifications
- User profiles

### Security & Privacy
- AES-256-GCM encryption (Android Keystore)
- GDPR compliance (Articles 15, 17, 20)
- CCPA compliance
- Audit logging
- Consent management

### RAFAELIA External Tooling
- `tools/raf_scan_repo.sh` deterministic repository scanner
- Markdown reports under `.rafaelia/reports/`
- Priority ranking based on evidence, urgency, memory, transformation, latency and obvious structural value
- Hotfix candidate detection for TODO/FIXME/error/crash/SDK/logcat/permission/overflow-style signals
- Orphan/latent file report for files without extension or unclear classification

---

## 🔴 What's Pending

See [Status Report](STATUS_REPORT.md) for complete list.

**High Priority:**
- Unit test coverage > 80%
- Terminal emulation
- SSH key authentication
- CI/CD pipeline completion
- Fill scanner reports in target repositories and convert the top findings into issues or small hotfix PRs

**Medium Priority:**
- Multi-platform support (GitLab, Bitbucket)
- GPG key management
- Git LFS support
- Optional Kotlin/Android UI integration for the scanner results

---

## 📞 Contact & Support

### Getting Help
- **Issues**: [Report Issues](https://github.com/rafaelmeloreisnovo/RafGitTools/issues)
- **Discussions**: [GitHub Discussions](https://github.com/rafaelmeloreisnovo/RafGitTools/discussions)

---

**Documentation Version**: 2.2  
**Last Updated**: 2026-05-24  
**Overall Progress**: tracked by implementation maturity, validated scope (GitHub + JGit), and RAFAELIA scanner reports