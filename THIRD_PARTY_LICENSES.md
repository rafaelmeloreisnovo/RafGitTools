# Third Party Licenses

This document contains the licenses and copyright notices of all third-party open source projects that are integrated or referenced in RafGitTools.

## Table of Contents
- [FastHub](#fasthub)
- [FastHub-RE](#fasthub-re)
- [MGit](#mgit)
- [PuppyGit](#puppygit)
- [Termux App](#termux-app)
- [Termux Packages](#termux-packages)

---

## FastHub

**Repository:** https://github.com/k0shk0sh/FastHub  
**Description:** FastHub - The ultimate GitHub client for Android  
**License:** GNU General Public License v3.0 (GPL-3.0)  
**Copyright:** Copyright (c) k0shk0sh

### License Terms

FastHub is licensed under the GNU General Public License version 3.0. This is a copyleft license that requires anyone who distributes the code or a derivative work to make the source available under the same terms.

**Key Requirements:**
- Source code must be made available when distributing the software
- Modifications must be released under the same license
- Changes made to the code must be documented
- If patented algorithms are used in the code, you grant a license to use those patents

For the full license text, see: https://github.com/k0shk0sh/FastHub/blob/master/LICENSE

---

## FastHub-RE

**Repository:** https://github.com/LightDestory/FastHub-RE  
**Description:** Revival Attempt for FastHub - A GitHub client for Android  
**License:** GNU General Public License v3.0 (GPL-3.0-only)  
**Copyright:** Copyright (c) LightDestory and original FastHub contributors

### License Terms

FastHub-RE is a fork and revival of the original FastHub project, maintaining the same GPL-3.0 license. As a derivative work of FastHub, it must comply with the GPL-3.0 terms.

**Key Requirements:**
- Source code must be made available when distributing the software
- Modifications must be released under the same license
- Must maintain attribution to original authors
- Changes made to the code must be documented

For the full license text, see: https://github.com/LightDestory/FastHub-RE/blob/master/LICENSE

---

## MGit

**Repository:** https://github.com/maks/MGit  
**Description:** A Git client for Android  
**License:** GNU General Public License v3.0 (GPL-3.0)  
**Copyright:** Copyright (c) Maksim Lin and contributors

### License Terms

MGit is an open source Git client for Android that allows management of local Git repositories on Android devices.

**Key Requirements:**
- Source code must be made available when distributing the software
- Modifications must be released under the GPL-3.0 license
- Changes made to the code must be documented
- Must include copyright and license notices

**Minimum Android Version:** Android 5.0 (Lollipop)

For the full license text, see: https://github.com/maks/MGit/blob/master/LICENSE

---

## PuppyGit

**Repository:** https://github.com/catpuppyapp/PuppyGit  
**Description:** An Android Git Client  
**License:** GNU General Public License v3.0 (GPL-3.0)  
**Copyright:** Copyright (c) Bandeapart1964/catpuppyapp

### License Terms

PuppyGit is a free, open-source, and ad-free Git client for Android featuring a full Git workflow.

**Key Requirements:**
- Source code must be made available when distributing the software
- Modifications must be released under the GPL-3.0 license
- Changes and modifications must be documented
- Must maintain proper attribution

For the full license text, see: https://github.com/catpuppyapp/PuppyGit/blob/main/LICENSE

---

## Termux App

**Repository:** https://github.com/termux/termux-app  
**Description:** Termux - Android terminal emulator and Linux environment  
**License:** GNU General Public License v3.0 (GPL-3.0) with exceptions  
**Copyright:** Copyright (c) Termux contributors

### License Terms

The main Termux application is licensed under GPL-3.0, with specific exceptions for terminal emulator components.

**Main App:** GPL-3.0 only

**Terminal Emulator Libraries (terminal-view and terminal-emulator):**  
Licensed under the Apache License 2.0  
- Forked from Android-Terminal-Emulator
- More permissive than GPL-3.0
- Allows use in proprietary software with proper attribution

**Key Requirements for GPL-3.0 Components:**
- Source code must be made available when distributing
- Modifications must be released under GPL-3.0
- Changes must be documented

**Key Requirements for Apache-2.0 Components:**
- Must include copyright notice and license
- Must state significant changes made
- Must include NOTICE file if one exists

For the full license details, see: https://github.com/termux/termux-app/blob/master/LICENSE.md

---

## Termux Packages

**Repository:** https://github.com/termux/termux-packages  
**Description:** A package build system for Termux  
**License:** Various open source licenses (per package)  
**Copyright:** Copyright (c) Termux contributors and respective package authors

### License Terms

The Termux packages repository contains build scripts and package definitions for hundreds of packages, each with its own license.

**Build Scripts:** Apache-2.0 / GPL-3.0 compatible licenses

**Individual Packages:** Each package specifies its license in the `build.sh` file using the `TERMUX_PKG_LICENSE` variable. Common licenses include:
- MIT License
- GNU General Public License v2.0 and v3.0
- Apache License 2.0
- BSD Licenses (2-clause and 3-clause)
- Custom licenses (as specified by upstream projects)

**Key Requirements:**
- Each package includes its license file
- License information is documented in build.sh files
- When redistributing, include all relevant license files
- Comply with each package's specific license terms

**License Files Location:**
- Generic licenses: https://github.com/termux/termux-packages/tree/master/packages/termux-licenses/LICENSES
- Package-specific licenses: Included in each package's distribution

For repository license, see: https://github.com/termux/termux-packages/blob/master/LICENSE.md  
For package-specific licenses, check: Each package's `build.sh` file

---

## GPL-3.0 Summary

Since most projects in this collection use GPL-3.0, here's a summary of the key obligations:

### Permissions
✅ Commercial use  
✅ Modification  
✅ Distribution  
✅ Patent use  
✅ Private use  

### Conditions
⚠️ **License and copyright notice** - Must include the license and copyright notice with the code  
⚠️ **State changes** - Must document changes made to the code  
⚠️ **Disclose source** - Source code must be made available when distributing  
⚠️ **Same license** - Modifications must be released under the same license when distributing  

### Limitations
❌ Liability - No liability for damages  
❌ Warranty - No warranty provided  

---

## Compliance Guidelines

To comply with all these licenses when creating a combined APK:

1. **Include All License Files**
   - Bundle this THIRD_PARTY_LICENSES.md file
   - Include individual LICENSE files from each project
   - Maintain all copyright notices

2. **Provide Source Code**
   - Make source code available for all GPL-3.0 components
   - Document all modifications made
   - Use a public repository (e.g., GitHub)

3. **Attribution**
   - Credit all original authors
   - Link to original repositories
   - Acknowledge all contributors

4. **License Compatibility**
   - All projects use GPL-3.0 (highly compatible)
   - Apache-2.0 components are GPL-3.0 compatible
   - Combined work must be distributed under GPL-3.0

5. **Documentation**
   - Document which components are included
   - Explain modifications made
   - Provide build instructions

---

## Additional Resources

- **GPL-3.0 Full Text:** https://www.gnu.org/licenses/gpl-3.0.html
- **Apache-2.0 Full Text:** https://www.apache.org/licenses/LICENSE-2.0
- **MIT License Full Text:** https://opensource.org/licenses/MIT
- **Choose a License Guide:** https://choosealicense.com/
- **GNU License Compatibility:** https://www.gnu.org/licenses/license-compatibility.html

---

*Last Updated: 2026-01-09*
