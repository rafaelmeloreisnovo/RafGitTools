# GitHub Actions Workflows

This directory contains comprehensive GitHub Actions workflows for the RafGitTools project. These workflows automate various aspects of the development lifecycle including building, testing, security scanning, releasing, and more.

## 📋 Overview of Workflows

### Core CI/CD Workflows

#### 1. **CI** (`ci.yml`)
**Trigger**: Push/PR to main/develop branches, manual dispatch

**Purpose**: Main continuous integration pipeline

**Jobs**:
- **Build**: Builds all variants (devDebug, devRelease, productionDebug, productionRelease)
- **Test**: Runs unit tests and generates reports
- **Lint**: Performs code linting checks
- **Code Quality**: Runs code quality checks

**Artifacts**: APKs, test results, lint reports, code quality reports

---

#### 2. **PR Validation** (`pr-validation.yml`)
**Trigger**: Pull request events (opened, synchronized, reopened)

**Purpose**: Validates pull requests before merge

**Jobs**:
- **Validate**: Builds, tests, and lints PR changes
- **Label**: Automatically labels PRs based on changed files

**Features**:
- Checks PR title format (Conventional Commits)
- Posts results as PR comment
- Analyzes APK size

---

#### 3. **Release** (`release.yml`)
**Trigger**: Git tags (v*.*.*), manual dispatch

**Purpose**: Automates release creation and distribution

**Jobs**:
- **Build Release**: Builds production release APK
- **Publish to Store**: Prepares app bundle for Play Store (manual)

**Features**:
- Automatic version extraction
- Changelog generation
- GitHub release creation
- APK/AAB artifact upload

---

### Security & Quality Workflows

#### 4. **Security Scan** (`security.yml`)
**Trigger**: Push/PR to main/develop, daily schedule (2 AM UTC), manual

**Purpose**: Comprehensive security scanning

**Jobs**:
- **CodeQL Analysis**: Static code analysis for security vulnerabilities
- **Dependency Scan**: Checks dependencies for known vulnerabilities using Trivy
- **Secret Scan**: Scans for exposed secrets using TruffleHog
- **License Check**: Verifies license compliance

---

#### 5. **Code Coverage** (`coverage.yml`)
**Trigger**: Push/PR to main/develop, manual

**Purpose**: Generates and tracks code coverage

**Jobs**:
- **Coverage**: Runs tests with coverage and generates reports

**Features**:
- JaCoCo integration (if configured)
- Coverage report artifacts
- PR comments with coverage summary

---

#### 6. **Performance Metrics** (`performance.yml`)
**Trigger**: Push/PR to main, manual

**Purpose**: Tracks performance metrics

**Jobs**:
- **APK Size Analysis**: Monitors APK size and warns if too large
- **Build Time Analysis**: Measures and reports build times
- **Method Count Analysis**: Tracks method count (DEX limit)

**Features**:
- APK size threshold checks (50MB)
- Build time benchmarking
- PR comments with metrics

---

### Automation & Maintenance Workflows

#### 7. **Documentation** (`docs.yml`)
**Trigger**: Push/PR affecting docs, manual

**Purpose**: Validates and generates documentation

**Jobs**:
- **Validate**: Checks markdown syntax and links
- **Generate Docs**: Creates API documentation (Dokka)
- **Check README**: Verifies README completeness

**Features**:
- Markdown linting
- Link checking
- Spell checking
- API doc generation

---

#### 8. **Nightly Build** (`nightly.yml`)
**Trigger**: Daily schedule (3 AM UTC), manual

**Purpose**: Comprehensive nightly build and test

**Jobs**:
- **Nightly Build**: Builds all variants, runs tests, performs linting
- **Statistics**: Generates repository statistics

**Features**:
- Builds all variants
- Creates issue on failure
- Generates nightly version
- Repository statistics

---

#### 9. **Stale Issues and PRs** (`stale.yml`)
**Trigger**: Daily schedule (1 AM UTC), manual

**Purpose**: Manages stale issues and PRs

**Configuration**:
- Issues: Marked stale after 60 days, closed after 7 more days
- PRs: Marked stale after 30 days, closed after 14 more days
- Exempt labels: `keep-open`, `pinned`, `security`, `critical`

---

## 🔧 Configuration Files

### Dependabot (`dependabot.yml`)
Automated dependency updates for:
- Gradle dependencies (grouped by category)
- GitHub Actions

**Schedule**: Weekly on Mondays at 9 AM UTC

---

### Auto-labeling (`labeler.yml`)
Automatic PR labeling based on changed files:
- `documentation`: Markdown and docs files
- `code`: Kotlin/Java source files
- `ui`: UI and theme files
- `tests`: Test files
- `build`: Build configuration
- `ci/cd`: Workflow files
- And more...

---

### Documentation Tools
- `markdown-link-check.json`: Link checking configuration
- `spellcheck.yml`: Spell checking configuration

---

## 📝 Templates

### Issue Templates
Located in `.github/ISSUE_TEMPLATE/`:
1. **Bug Report** (`bug_report.md`): For reporting bugs
2. **Feature Request** (`feature_request.md`): For suggesting features
3. **Documentation** (`documentation.md`): For documentation issues

### Pull Request Template
`PULL_REQUEST_TEMPLATE.md`: Comprehensive PR template with checklists for:
- Description and related issues
- Type of change
- Testing requirements
- Code quality checklist
- Documentation updates
- Security and performance considerations

---

## 🚀 Usage Guide

### Running Workflows Manually

All workflows support manual triggering via `workflow_dispatch`. To run manually:

1. Go to the **Actions** tab in GitHub
2. Select the workflow you want to run
3. Click **Run workflow**
4. Select branch and provide inputs (if required)
5. Click **Run workflow**

### Workflow Triggers Summary

| Workflow | Push | PR | Schedule | Manual |
|----------|------|----|----|--------|
| CI | ✅ | ✅ | ❌ | ✅ |
| PR Validation | ❌ | ✅ | ❌ | ❌ |
| Security | ✅ | ✅ | ✅ Daily 2 AM | ✅ |
| Release | ❌ | ❌ | ❌ | ✅ |
| Coverage | ✅ | ✅ | ❌ | ✅ |
| Performance | ✅ | ✅ | ❌ | ✅ |
| Documentation | ✅ | ✅ | ❌ | ✅ |
| Nightly | ❌ | ❌ | ✅ Daily 3 AM | ✅ |
| Stale | ❌ | ❌ | ✅ Daily 1 AM | ✅ |

---

## 🎯 Best Practices

### For Contributors

1. **PR Title Format**: Use Conventional Commits format:
   - `feat:` for new features
   - `fix:` for bug fixes
   - `docs:` for documentation
   - `refactor:` for refactoring
   - `test:` for tests
   - `chore:` for maintenance

2. **Wait for Checks**: All PR validation checks must pass before merge

3. **Review Artifacts**: Check uploaded artifacts for issues

4. **Security Alerts**: Address security findings promptly

### For Maintainers

1. **Monitor Nightly Builds**: Review nightly build failures

2. **Review Dependabot PRs**: Keep dependencies up to date

3. **Manage Stale Items**: Review stale issues/PRs before auto-close

4. **Release Process**:
   - Tag commits with semantic version (e.g., `v1.0.0`)
   - Release workflow triggers automatically
   - Review and edit generated release notes

---

## 📊 Artifacts & Reports

### Build Artifacts
- APKs for all build variants
- AAB files for Play Store

### Test Reports
- Unit test results (XML)
- Test coverage reports (HTML)
- Lint reports (HTML)

### Security Reports
- CodeQL SARIF files
- Trivy vulnerability reports
- License compliance reports

### Performance Reports
- APK size analysis
- Build time metrics
- Method count analysis

### Documentation
- API documentation (Dokka)
- Repository statistics

**Retention**: Most artifacts are kept for 30 days, nightly builds for 7 days

---

## 🔒 Security Considerations

### Secrets Management
Required secrets (configure in repository settings):
- `GITHUB_TOKEN`: Automatically provided by GitHub
- `PLAY_STORE_SERVICE_ACCOUNT_JSON`: For Play Store publishing (optional)

### Permissions
Workflows use minimal required permissions:
- Read: For checking out code
- Write: For creating releases, commenting on PRs
- Security-events: For uploading security scan results

---

## 🛠️ Customization

### Modifying Workflows

1. **Edit workflow files** in `.github/workflows/`
2. **Test changes** on a feature branch
3. **Monitor workflow runs** in the Actions tab
4. **Review logs** for any issues

### Adding New Workflows

1. Create new `.yml` file in `.github/workflows/`
2. Define workflow triggers and jobs
3. Test thoroughly
4. Document in this README

---

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow Syntax](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/build/building-cmdline)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

## 🤝 Contributing

Improvements to workflows are welcome! Please:
1. Test changes thoroughly
2. Update this documentation
3. Follow existing patterns
4. Consider security implications

---

## 📧 Support

For workflow issues:
1. Check workflow logs in the Actions tab
2. Review this documentation
3. Open an issue with the `ci/cd` label
4. Contact maintainers

---

**Last Updated**: January 2026

**Maintained by**: RafGitTools Team
