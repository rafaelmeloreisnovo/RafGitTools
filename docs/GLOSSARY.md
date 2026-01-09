# Glossary - RafGitTools Technical Terminology

## Overview

This glossary provides definitions of technical terms used in RafGitTools documentation. Terms are defined in English (currently the only supported language).

**Note**: Previous versions of this document claimed multilingual support. That was aspirational and not reflective of current reality.

---

## Git & Version Control Terms

### Branch
A parallel version of a repository that diverges from the main working project.  
**Command**: `git branch`

### Clone
Creating a copy of a repository on your local machine.  
**Command**: `git clone <url>`

### Commit
A snapshot of changes to files in a repository with a descriptive message.  
**Command**: `git commit -m "message"`

### Diff
Display of differences between two versions of files.  
**Command**: `git diff`

### Fork
Creating a personal copy of someone else's repository.

### Merge
Combining changes from one branch into another.  
**Command**: `git merge <branch>`

### Pull
Fetching and merging changes from a remote repository.  
**Command**: `git pull`

### Pull Request (PR)
A request to merge changes from one branch into another, typically for code review.

### Push
Uploading local commits to a remote repository.  
**Command**: `git push`

### Rebase
Reapplying commits on top of another base commit.  
**Command**: `git rebase <branch>`

### Repository
A storage location for a software project containing all files and history.

### Stash
Temporarily storing uncommitted changes.  
**Command**: `git stash`

### Tag
A marker for a specific point in Git history, typically for releases.  
**Command**: `git tag <name>`

---

## Development Terms

### API (Application Programming Interface)
A set of protocols for building software applications and communication between components.

### CI/CD (Continuous Integration/Continuous Deployment)
Automated building, testing, and deployment of code changes.

### Commit Hash
A unique SHA identifier for each commit.

### HEAD
Pointer to the current branch/commit you're working on.

### Issue
A task, bug report, or feature request in a project tracker.

### Merge Conflict
When Git cannot automatically merge changes because the same lines were modified differently.

### Origin
The default name for a remote repository.

### Remote
A version of your repository hosted on a server.

### Working Directory
Your local copy of the repository files.

---

## Android Development Terms

### Android
Mobile operating system developed by Google.

### APK (Android Package Kit)
File format used for Android app installation.

### Gradle
Build automation tool used for Android projects.

### Jetpack Compose
Modern UI toolkit for Android.

### Kotlin
Programming language used for Android development.

### Material Design
Design language developed by Google.

---

## Security & Privacy Terms

### Authentication
Process of verifying user identity.

### Encryption
Converting data into a secure format.

### OAuth
Open standard for access delegation and authentication.

### SSH (Secure Shell)
Cryptographic network protocol for secure communication.

### Token
Authentication credential used for API access.

---

## Project-Specific Terms

### RafGitTools
This Android application for Git and GitHub operations.

### FastHub, MGit, PuppyGit, Termux
Open-source projects that inspire RafGitTools features.

---

## Usage Guidelines

### For Documentation Writers

- Use these standard terms consistently
- Define new terms when first introduced
- Link to this glossary for detailed definitions
- Avoid jargon without explanation

### For Developers

- Follow this terminology in code comments
- Use consistent naming in the codebase
- Update this glossary when adding new concepts

---

## Contributing

To suggest new terms:
1. Open a GitHub Issue
2. Provide: term, definition, context, related commands
3. Documentation team will review and add

---

**Document Owner**: Documentation Team  
**Version**: 2.0-honest  
**Last Updated**: January 2026  
**Status**: English only - reflects actual project state

*This glossary has been revised to remove false claims about multilingual support and focus on providing clear English definitions.*
