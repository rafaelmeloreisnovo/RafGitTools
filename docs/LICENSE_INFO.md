# License Information

## Project License

**RafGitTools** is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

This licensing choice ensures compatibility with all source projects and maintains the open-source nature of the combined work.

## Source Project Licenses

### 1. FastHub
- **Repository**: https://github.com/k0shk0sh/FastHub
- **License**: GPL-3.0
- **Copyright**: Copyright (c) Kosh
- **Usage**: Inspiration for GitHub integration features, UI patterns
- **Compliance**: As both projects are GPL-3.0, full compatibility is maintained

### 2. FastHub-RE (FastHub Revival Edition)
- **Repository**: https://github.com/LightDestory/FastHub-RE
- **License**: GPL-3.0
- **Copyright**: Copyright (c) LightDestory and original FastHub contributors
- **Usage**: Modern implementation patterns, updated API usage
- **Compliance**: GPL-3.0 compatible

### 3. MGit
- **Repository**: https://github.com/maks/MGit
- **License**: GPL-3.0
- **Copyright**: Copyright (c) Maks and contributors
- **Usage**: Local Git operations, repository management patterns
- **Compliance**: GPL-3.0 compatible

### 4. PuppyGit
- **Repository**: https://github.com/catpuppyapp/PuppyGit
- **License**: Apache License 2.0
- **Copyright**: Copyright (c) catpuppyapp
- **Usage**: UI/UX patterns, diff viewer implementation ideas
- **Compliance**: Apache 2.0 is GPL-3.0 compatible (one-way: Apache → GPL)

### 5. Termux
- **Repository**: https://github.com/termux/termux-app
- **License**: GPL-3.0
- **Copyright**: Copyright (c) Termux contributors
- **Usage**: Terminal emulation concepts, command-line integration
- **Compliance**: GPL-3.0 compatible

### 6. GitHub Mobile (Official)
- **Repository**: Closed-source (proprietary)
- **License**: Proprietary
- **Copyright**: Copyright (c) GitHub, Inc.
- **Usage**: Reference only for feature concepts and UX patterns
- **Compliance**: No code or proprietary assets are used; only general UI/UX concepts serve as inspiration

## Third-Party Libraries

### Core Dependencies

#### JGit (Java Git Implementation)
- **License**: Eclipse Distribution License v1.0 (BSD-3-Clause)
- **Copyright**: Eclipse Foundation
- **Usage**: Git operations implementation
- **Compliance**: BSD-3 is GPL-3.0 compatible

#### Kotlin Standard Library
- **License**: Apache License 2.0
- **Copyright**: JetBrains
- **Usage**: Primary programming language
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

#### Jetpack Compose
- **License**: Apache License 2.0
- **Copyright**: Google LLC
- **Usage**: UI framework
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

#### Retrofit
- **License**: Apache License 2.0
- **Copyright**: Square, Inc.
- **Usage**: HTTP client for API calls
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

#### OkHttp
- **License**: Apache License 2.0
- **Copyright**: Square, Inc.
- **Usage**: HTTP client foundation
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

#### Room
- **License**: Apache License 2.0
- **Copyright**: Google LLC
- **Usage**: Local database
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

#### Hilt
- **License**: Apache License 2.0
- **Copyright**: Google LLC
- **Usage**: Dependency injection
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

#### Coil
- **License**: Apache License 2.0
- **Copyright**: Coil Contributors
- **Usage**: Image loading
- **Compliance**: Apache 2.0 is GPL-3.0 compatible

## License Compatibility Matrix

```
┌─────────────────────┬──────────────┬────────────────────┐
│ Source              │ License      │ GPL-3.0 Compatible │
├─────────────────────┼──────────────┼────────────────────┤
│ FastHub             │ GPL-3.0      │ ✓ Yes              │
│ FastHub-RE          │ GPL-3.0      │ ✓ Yes              │
│ MGit                │ GPL-3.0      │ ✓ Yes              │
│ PuppyGit            │ Apache-2.0   │ ✓ Yes (one-way)    │
│ Termux              │ GPL-3.0      │ ✓ Yes              │
│ GitHub Mobile       │ Proprietary  │ N/A (ref only)     │
│ JGit                │ EDL-1.0/BSD  │ ✓ Yes              │
│ Android Libraries   │ Apache-2.0   │ ✓ Yes              │
└─────────────────────┴──────────────┴────────────────────┘
```

## Attribution Requirements

### GPL-3.0 Requirements
When using GPL-3.0 code, we must:
1. Include the full GPL-3.0 license text
2. Clearly state what has been changed
3. Provide access to source code
4. Use compatible license (GPL-3.0)
5. Include copyright notices

### Apache-2.0 Requirements
When using Apache-2.0 code, we must:
1. Include Apache license and copyright notices
2. State significant changes made
3. Include NOTICE file if present
4. Preserve patent grant

## Copyright Notices

```
Copyright (c) 2024-2026 RafGitTools Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.

---

This project incorporates concepts and patterns from several open-source
projects. See LICENSES/ directory for individual license texts and
attributions.
```

## Source Code Availability

As required by GPL-3.0:
- Full source code is available at: https://github.com/rafaelmeloreisnovo/RafGitTools
- Build instructions are provided in BUILD.md
- All dependencies and their versions are documented

## Patents

This project does not knowingly infringe on any patents. Users should be aware of potential patent claims in:
- Git protocol implementations
- Diff algorithms
- UI/UX patterns

## Warranty Disclaimer

```
THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
```

## Contributing

By contributing to this project, you agree that your contributions will be licensed under the GPL-3.0 license. See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

## Questions

For licensing questions, please open an issue on GitHub or contact the maintainers.

---

Last updated: January 2026
