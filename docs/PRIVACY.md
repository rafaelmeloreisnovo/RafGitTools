# Privacy Policy and Data Protection

## Overview

RafGitTools is committed to protecting user privacy and ensuring transparent data handling practices. This document outlines our privacy practices, data control mechanisms, and compliance with international privacy standards.

## Data Collection and Usage

### What Data We Collect

#### 1. User-Provided Data
- **Git Credentials**: Username, email, SSH keys, access tokens (stored encrypted)
- **Repository Data**: Local repository clones, commit history, branches
- **Account Information**: GitHub/GitLab authentication tokens (OAuth)
- **Settings**: User preferences, theme choices, language settings

#### 2. Automatically Collected Data
- **Usage Statistics**: Feature usage, error reports (opt-in only)
- **Performance Data**: App performance metrics (opt-in only)
- **Device Information**: Android version, device model (for compatibility)

### What Data We Do NOT Collect
- Personal identification beyond what's necessary for Git operations
- Browsing history outside the app
- Location data
- Contacts or other personal information
- Repository contents are stored locally only

## User Data Control

### Data Access Rights
Users have complete control over their data:

1. **Right to Access**: View all stored data through Settings → Privacy → My Data
2. **Right to Export**: Export all user data in JSON/ZIP format
3. **Right to Delete**: Permanently delete all user data
4. **Right to Modify**: Update or correct any stored information

### Data Control Features

#### Data Export
```
Settings → Privacy → Export My Data
```
Exports all user data including:
- Credentials (encrypted)
- Repository metadata
- Settings and preferences
- Usage history (if enabled)

#### Data Deletion
```
Settings → Privacy → Delete My Data
```
Options:
- Delete credentials only
- Delete all app data
- Delete specific repositories
- Complete account removal

#### Privacy Settings
```
Settings → Privacy
```
Controls:
- Analytics opt-in/opt-out
- Crash reporting opt-in/opt-out
- Usage statistics opt-in/opt-out
- Data retention period

## Data Storage and Security

### Local Storage
- All repository data stored locally on device
- Database encrypted using Android Encrypted Shared Preferences
- Credentials stored in Android Keystore System
- Automatic data cleanup on app uninstall

### Encryption
- **At Rest**: AES-256 encryption for sensitive data
- **In Transit**: TLS 1.3 for all network communications
- **Credentials**: Hardware-backed Android Keystore when available
- **Backups**: Encrypted backups with user-controlled keys

### Data Retention
- **Active Users**: Data retained while app is in use
- **Inactive Users**: Cached data cleared after 90 days of inactivity (configurable)
- **Deleted Data**: Permanently removed within 30 days (except legal requirements)
- **Backups**: User-controlled, not automatic

## Privacy by Design

### Principles
1. **Data Minimization**: Collect only necessary data
2. **Purpose Limitation**: Use data only for intended purposes
3. **Transparency**: Clear communication about data practices
4. **User Control**: Users have full control over their data
5. **Security**: Strong security measures to protect data
6. **Accountability**: Regular privacy audits and reviews

### Implementation
- **Offline-First**: App works primarily offline
- **Local Processing**: Data processed locally when possible
- **No Third-Party Tracking**: No advertising or tracking SDKs
- **Open Source**: Code available for audit
- **Privacy-Preserving Analytics**: Anonymous, aggregated only

## Compliance

### GDPR (General Data Protection Regulation)
- **Legal Basis**: User consent and legitimate interest
- **Data Subject Rights**: All GDPR rights implemented
- **Data Protection Officer**: Contact privacy@rafgittools.dev
- **Data Processing Agreements**: Available for enterprise users

### CCPA (California Consumer Privacy Act)
- **Do Not Sell**: We do not sell user data
- **Disclosure**: Annual privacy report available
- **Rights**: All CCPA rights implemented

### ISO/IEC 27001 (Information Security)
- Information Security Management System (ISMS)
- Risk assessment and treatment
- Security controls implementation
- Regular security audits

### ISO/IEC 27701 (Privacy Information Management)
- Privacy Information Management System (PIMS)
- Privacy by design and by default
- Data protection impact assessments
- Privacy compliance monitoring

## Third-Party Services

### GitHub/GitLab APIs
- **Purpose**: Repository synchronization
- **Data Shared**: Authentication tokens only
- **Privacy Policy**: [GitHub Privacy](https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement)
- **Data Processing Agreement**: Available

### JGit Library
- **Purpose**: Local Git operations
- **Data Shared**: None (local processing only)
- **License**: Eclipse Distribution License

### No Analytics by Default
- All analytics features are opt-in only
- Anonymous, aggregated data only
- User can opt-out at any time
- No personal identification in analytics

## Data Breach Response

### Notification
- Users notified within 72 hours of discovery
- Detailed information about the breach
- Remediation steps provided
- Regulatory authorities notified as required

### Mitigation
- Immediate security measures activated
- Affected systems isolated
- Security patches deployed
- Independent security audit conducted

## Children's Privacy

RafGitTools is not intended for children under 13. We do not knowingly collect personal information from children. If you believe a child has provided data, contact us immediately at privacy@rafgittools.dev.

## International Data Transfers

- Data stored locally on user's device
- No international data transfers by default
- Cloud sync (optional) uses user-chosen provider
- Appropriate safeguards for any transfers

## Privacy Settings Reference

### Available Controls

#### Analytics
```kotlin
// Analytics opt-in/opt-out
Settings → Privacy → Analytics
- Enable/Disable usage analytics
- Enable/Disable crash reporting
- View collected data
```

#### Data Management
```kotlin
// Data control options
Settings → Privacy → My Data
- View stored data
- Export data (JSON/ZIP)
- Delete specific data
- Clear cache
```

#### Security
```kotlin
// Security settings
Settings → Privacy → Security
- Biometric authentication
- Auto-lock timeout
- Secure screenshot prevention
- Clipboard security
```

## Updates to Privacy Policy

- Users notified of material changes
- Continued use implies acceptance
- Previous versions archived and accessible
- Last updated: [Current Date]

## Contact Information

**Privacy Concerns**: privacy@rafgittools.dev  
**Security Issues**: security@rafgittools.dev  
**General Support**: support@rafgittools.dev  

**Data Protection Officer**  
RafGitTools Privacy Team  
[Address if applicable]

## Your Rights

You have the right to:
1. Access your personal data
2. Rectify inaccurate data
3. Erase your data ("right to be forgotten")
4. Restrict processing
5. Data portability
6. Object to processing
7. Withdraw consent
8. Lodge a complaint with supervisory authority

To exercise these rights, contact privacy@rafgittools.dev.

## Privacy Audit Trail

All privacy-related actions are logged (with user consent):
- Data access requests
- Data exports
- Data deletions
- Privacy setting changes
- Security events

Audit logs available through Settings → Privacy → Activity Log.

---

**Last Updated**: January 2026  
**Version**: 1.0  
**Effective Date**: [Release Date]
