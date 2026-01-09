# RafGitTools - Unified Git/GitHub Android Client

## Project Vision

RafGitTools aims to create a comprehensive, high-performance Android application that combines the best features from multiple popular Git and GitHub clients while respecting their open-source licenses. The goal is to provide a unified, intuitive, and powerful mobile Git experience.

## Source Projects & Inspiration

### 1. FastHub (GitHub Client)
- **Repository**: https://github.com/k0shk0sh/FastHub
- **License**: GPL-3.0
- **Key Features**:
  - Full GitHub API integration
  - Repository browsing
  - Issue and PR management
  - Code review capabilities
  - Notifications

### 2. FastHub-RE (FastHub Revival)
- **Repository**: https://github.com/LightDestory/FastHub-RE
- **License**: GPL-3.0
- **Key Features**:
  - Modernized FastHub fork
  - Updated dependencies
  - Bug fixes and improvements

### 3. MGit (Local Git Client)
- **Repository**: https://github.com/maks/MGit
- **License**: GPL-3.0
- **Key Features**:
  - Local Git repository management
  - Clone, commit, push, pull operations
  - SSH key management
  - Offline capabilities

### 4. PuppyGit (Local Git Client)
- **Repository**: https://github.com/catpuppyapp/PuppyGit
- **License**: Apache-2.0
- **Key Features**:
  - Modern UI/UX
  - Advanced Git operations
  - File diff viewer
  - Branch management

### 5. Termux
- **Repository**: https://github.com/termux/termux-app
- **License**: GPL-3.0
- **Key Features**:
  - Terminal emulation
  - Command-line Git access
  - Package management
  - Scripting capabilities

### 6. GitHub Mobile (Official App)
- **Repository**: Closed-source
- **Key Features** (for reference only):
  - Native GitHub experience
  - Fast and responsive
  - Modern Material Design
  - Rich notifications

## Core Features

### Git Operations (96+ Advanced Features)
- **Clone Operations**: Full clone, shallow clone, single branch, with submodules, with LFS
- **Commit Management**: Standard commits, amend, GPG signing, templates, interactive staging
- **Branch Operations**: Create, delete, rename, track, compare, checkout
- **Push/Pull**: Force push with lease, pull with rebase, tag operations, multi-remote sync
- **Merge Operations**: Multiple strategies (recursive, ours, theirs), fast-forward, no-fast-forward, squash
- **Rebase**: Interactive rebase, rebase --onto, autosquash, continue/skip/abort
- **Stash Management**: With message, untracked files, apply/pop, drop/clear, stash branch
- **Cherry-pick**: Single and range cherry-pick operations with continue/abort
- **Tag Management**: Annotated, lightweight, signed (GPG), push/pull, deletion
- **Submodule Support**: Add, update, init, sync, foreach commands, nested submodules
- **Git LFS**: Install, track patterns, fetch/pull, prune operations
- **Worktrees**: Add, list, remove, prune worktrees
- **Advanced Operations**: Reflog, bisect, blame annotations, clean, reset (soft/mixed/hard), revert, remote management, patches, archives, bundles, sparse checkout, partial clone

### GitHub Integration (169+ Comprehensive Features)
- **Repository Management**: Browse, search, create, settings, delete, transfer, archive, templates, topics, visibility, collaborators, webhooks, secrets, environments
- **Issue Management**: Create, edit, comment, reactions, labels, milestones, assignments, templates, forms, pinning, locking, transfer, linking, search, filters, sorting
- **Pull Requests**: Complete workflow including creation, editing, review, multiple merge strategies, draft mode, auto-merge, templates, checks status, required reviews, review requests, suggestions, file changes, commits, conversation, reactions, labels, milestones, assignments, linked issues, conflict detection
- **Code Review**: Inline comments, review suggestions, approval, changes requested, multi-line comments, suggested changes, batch comments, review threads
- **Notifications**: Push notifications, in-app notifications, filters, grouping, threads, muting, custom rules, email sync, scheduling
- **GitHub Actions**: Workflow viewing, runs, logs, re-run, cancellation, triggers, editing, Action marketplace
- **Releases**: Creation, editing, asset upload, release notes, drafts, pre-releases, badges, auto-generation
- **Wikis**: Browse, edit, create, search, history tracking
- **Gists**: Create, edit, comment, star, fork, secret gists, multiple files, revisions
- **Organizations**: Profile management, members, teams, settings, repositories, projects, events
- **Projects**: Boards, views, items, automation, fields, insights
- **Discussions**: Categories, creation, commenting, reactions, polls, answers
- **Sponsors**: Tiers, goals, dashboard integration
- **User Profiles**: Repositories, followers, activity, stars, gists, organizations
- **Search**: Repository, code, commit, user search with advanced filters
- **Security**: Security advisories, Dependabot alerts, code scanning, secret scanning
- **Packages**: Container registry, package management

### User Experience (119+ UI/UX Features)
- Material Design 3 (Material You)
- Dark/Light/Auto theme
- Intuitive gesture navigation
- Fast and responsive UI
- Offline-first architecture
- Syntax highlighting
- File diff viewer
- Search functionality

### Advanced Features
- SSH key management
- GPG signature support
- Custom Git server support (GitLab, Gitea, etc.)
- Terminal emulation for advanced operations
- Markdown editor and preview
- Code snippet sharing
- Repository statistics and insights

## Technical Architecture

### Technology Stack
- **Language**: Kotlin (primary) with minimal Java interop
- **UI Framework**: Jetpack Compose (modern declarative UI)
- **Architecture**: Clean Architecture + MVVM
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Database**: Room (for local storage)
- **Git Library**: JGit (pure Java Git implementation)
- **Async**: Kotlin Coroutines + Flow

### Modular Design
```
app/
├── core/
│   ├── common/         # Shared utilities
│   ├── data/           # Data layer
│   ├── domain/         # Business logic
│   └── ui/             # UI components
├── feature/
│   ├── auth/           # Authentication
│   ├── repository/     # Repository management
│   ├── commit/         # Commit operations
│   ├── branch/         # Branch management
│   ├── github/         # GitHub specific features
│   ├── diff/           # File diff viewer
│   └── terminal/       # Terminal emulation
└── app/                # Application module
```

### Key Design Principles
1. **Separation of Concerns**: Clear boundaries between layers
2. **Single Responsibility**: Each module has one purpose
3. **Dependency Inversion**: Depend on abstractions
4. **Testability**: Unit and integration tests for all features
5. **Performance**: Optimized for mobile constraints
6. **Offline-First**: Work without internet when possible

## License Compliance

This project respects all source licenses:

- **GPL-3.0 Components**: FastHub, MGit, Termux features
  - Any derivative work must be GPL-3.0 compatible
  - Source code must be made available
  
- **Apache-2.0 Components**: PuppyGit features
  - Compatible with GPL-3.0
  - Must preserve copyright notices

**Project License**: GPL-3.0 (to maintain compatibility with all sources)

## Development Roadmap

### Phase 1: Foundation (Weeks 1-4)
- [ ] Project setup and architecture
- [ ] Core Git operations (clone, commit, push, pull)
- [ ] Basic repository browsing
- [ ] Authentication system

### Phase 2: GitHub Integration (Weeks 5-8)
- [ ] GitHub API client
- [ ] Issue and PR management
- [ ] Code review features
- [ ] Notifications

### Phase 3: Advanced Features (Weeks 9-12)
- [ ] Terminal emulation
- [ ] Advanced Git operations
- [ ] SSH/GPG key management
- [ ] Custom Git server support

### Phase 4: Polish & Release (Weeks 13-16)
- [ ] UI/UX refinement
- [ ] Performance optimization
- [ ] Comprehensive testing
- [ ] Documentation
- [ ] Beta release

## Performance Goals

- App startup: < 2 seconds
- Repository list load: < 1 second
- Commit operation: < 500ms
- UI response time: < 100ms (60 FPS)
- Memory usage: < 100MB typical
- Battery efficient background operations

## Accessibility

- Screen reader support
- High contrast themes
- Scalable text
- Keyboard navigation
- Color blind friendly palettes

## Security

- Secure credential storage (Android Keystore)
- HTTPS enforcement
- SSH key encryption
- GPG signature verification
- No logging of sensitive data
- Regular security audits

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Community

- Issues: GitHub Issues
- Discussions: GitHub Discussions
- Documentation: Wiki

## Acknowledgments

This project is inspired by and builds upon the work of many open-source projects. We are grateful to the maintainers and contributors of FastHub, MGit, PuppyGit, Termux, and the broader Git community.
