# Architecture Documentation

## Overview

RafGitTools follows Clean Architecture principles combined with MVVM pattern, implemented in Kotlin with Jetpack Compose for the UI layer.

## Layer Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│  (Jetpack Compose UI + ViewModels)                  │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│                   Domain Layer                       │
│  (Use Cases + Domain Models + Repository Interfaces)│
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│                    Data Layer                        │
│  (Repository Implementations + Data Sources)         │
│  ├─ Remote (GitHub API, Git servers)                │
│  ├─ Local (Room Database, File System)              │
│  └─ Cache (In-memory)                                │
└─────────────────────────────────────────────────────┘
```

## Module Structure

### Core Modules

#### `core:common`
Shared utilities, extensions, and base classes.

```kotlin
core:common/
├── util/
│   ├── Result.kt           # Wrapper for success/error
│   ├── Extensions.kt       # Kotlin extensions
│   └── Constants.kt        # App constants
├── di/
│   └── CommonModule.kt     # Common DI setup
└── error/
    └── AppError.kt         # Error types
```

#### `core:data`
Data layer implementations.

```kotlin
core:data/
├── repository/
│   ├── GitRepositoryImpl.kt
│   └── GitHubRepositoryImpl.kt
├── source/
│   ├── local/
│   │   ├── database/       # Room database
│   │   └── preferences/    # DataStore
│   └── remote/
│       ├── api/            # Retrofit interfaces
│       └── git/            # JGit wrappers
└── model/
    └── dto/                # Data transfer objects
```

#### `core:domain`
Business logic and domain models.

```kotlin
core:domain/
├── model/
│   ├── Repository.kt
│   ├── Commit.kt
│   ├── Branch.kt
│   ├── Issue.kt
│   └── PullRequest.kt
├── repository/
│   ├── GitRepository.kt    # Interface
│   └── GitHubRepository.kt # Interface
└── usecase/
    ├── git/
    │   ├── CloneRepoUseCase.kt
    │   ├── CommitChangesUseCase.kt
    │   └── PushChangesUseCase.kt
    └── github/
        ├── GetIssuesUseCase.kt
        └── CreatePRUseCase.kt
```

#### `core:ui`
Shared UI components and theme.

```kotlin
core:ui/
├── theme/
│   ├── Theme.kt
│   ├── Color.kt
│   └── Typography.kt
├── component/
│   ├── Button.kt
│   ├── TextField.kt
│   └── LoadingIndicator.kt
└── navigation/
    └── Navigator.kt
```

### Feature Modules

#### `feature:repository`
Repository management screens.

```kotlin
feature:repository/
├── list/
│   ├── RepositoryListScreen.kt
│   └── RepositoryListViewModel.kt
├── detail/
│   ├── RepositoryDetailScreen.kt
│   └── RepositoryDetailViewModel.kt
└── clone/
    ├── CloneScreen.kt
    └── CloneViewModel.kt
```

#### `feature:commit`
Commit and staging operations.

```kotlin
feature:commit/
├── stage/
│   ├── StagingScreen.kt
│   └── StagingViewModel.kt
├── commit/
│   ├── CommitScreen.kt
│   └── CommitViewModel.kt
└── history/
    ├── CommitHistoryScreen.kt
    └── CommitHistoryViewModel.kt
```

#### `feature:github`
GitHub-specific features.

```kotlin
feature:github/
├── issues/
│   ├── IssueListScreen.kt
│   └── IssueDetailScreen.kt
├── pullrequest/
│   ├── PRListScreen.kt
│   └── PRDetailScreen.kt
└── actions/
    ├── ActionsScreen.kt
    └── WorkflowRunScreen.kt
```

## Data Flow

### Standard Flow (MVVM + Clean Architecture)

```
User Action
    ↓
UI (Compose)
    ↓
ViewModel
    ↓
Use Case
    ↓
Repository (Interface)
    ↓
Repository (Implementation)
    ↓
Data Source (Remote/Local)
    ↓
← Result Flow ←
    ↓
UI Update
```

### Example: Cloning a Repository

```kotlin
// 1. User clicks "Clone" button
CloneScreen() {
    viewModel.cloneRepository(url, path)
}

// 2. ViewModel calls use case
class CloneViewModel @Inject constructor(
    private val cloneRepoUseCase: CloneRepoUseCase
) : ViewModel() {
    
    fun cloneRepository(url: String, path: String) {
        viewModelScope.launch {
            cloneRepoUseCase(url, path)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> _state.value = Loading
                        is Result.Success -> _state.value = Success
                        is Result.Error -> _state.value = Error(result.error)
                    }
                }
        }
    }
}

// 3. Use case executes business logic
class CloneRepoUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    operator fun invoke(url: String, path: String): Flow<Result<Repository>> {
        return flow {
            emit(Result.Loading)
            val result = gitRepository.clone(url, path)
            emit(result)
        }
    }
}

// 4. Repository implementation uses data source
class GitRepositoryImpl @Inject constructor(
    private val gitDataSource: GitDataSource,
    private val localDatabase: AppDatabase
) : GitRepository {
    
    override suspend fun clone(url: String, path: String): Result<Repository> {
        return try {
            val repo = gitDataSource.clone(url, path)
            localDatabase.repositoryDao().insert(repo.toEntity())
            Result.Success(repo)
        } catch (e: Exception) {
            Result.Error(AppError.GitError(e.message))
        }
    }
}

// 5. Data source interacts with JGit
class GitDataSource @Inject constructor() {
    
    fun clone(url: String, path: String): Repository {
        val git = Git.cloneRepository()
            .setURI(url)
            .setDirectory(File(path))
            .call()
        
        return git.toRepository()
    }
}
```

## Dependency Injection

Using Hilt for dependency injection:

```kotlin
// Application module
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGitDataSource(): GitDataSource {
        return GitDataSource()
    }
    
    @Provides
    @Singleton
    fun provideGitRepository(
        gitDataSource: GitDataSource,
        database: AppDatabase
    ): GitRepository {
        return GitRepositoryImpl(gitDataSource, database)
    }
}

// ViewModel injection
@HiltViewModel
class CloneViewModel @Inject constructor(
    private val cloneRepoUseCase: CloneRepoUseCase
) : ViewModel()
```

## State Management

### UI State

```kotlin
data class CloneUiState(
    val isLoading: Boolean = false,
    val repository: Repository? = null,
    val error: String? = null,
    val progress: Int = 0
)

// In ViewModel
private val _uiState = MutableStateFlow(CloneUiState())
val uiState: StateFlow<CloneUiState> = _uiState.asStateFlow()
```

### Side Effects

```kotlin
sealed class CloneEffect {
    data class ShowToast(val message: String) : CloneEffect()
    data class NavigateToRepo(val repoId: String) : CloneEffect()
}

// In ViewModel
private val _effects = Channel<CloneEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()
```

## Navigation

Using Compose Navigation:

```kotlin
@Composable
fun RafGitToolsNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "repository_list"
    ) {
        composable("repository_list") {
            RepositoryListScreen(
                onRepositoryClick = { id ->
                    navController.navigate("repository_detail/$id")
                }
            )
        }
        
        composable("repository_detail/{id}") { backStackEntry ->
            RepositoryDetailScreen(
                repositoryId = backStackEntry.arguments?.getString("id")
            )
        }
    }
}
```

## Threading Model

- **Main Thread**: UI rendering and user interactions
- **IO Dispatcher**: Network requests, database operations, file I/O
- **Default Dispatcher**: CPU-intensive operations
- **Custom Git Dispatcher**: Git operations (to avoid blocking)

```kotlin
// In Repository
override suspend fun getCommits(): Result<List<Commit>> = withContext(Dispatchers.IO) {
    // Network or database operation
}

// In ViewModel
fun loadCommits() {
    viewModelScope.launch {
        // Automatically uses Dispatchers.Main.immediate
        val result = getCommitsUseCase()
        _uiState.value = result
    }
}
```

## Error Handling

Centralized error handling:

```kotlin
sealed class AppError {
    data class GitError(val message: String) : AppError()
    data class NetworkError(val code: Int) : AppError()
    data class AuthError(val message: String) : AppError()
    object UnknownError : AppError()
}

// In UI
when (val error = state.error) {
    is AppError.GitError -> ShowErrorDialog(error.message)
    is AppError.NetworkError -> ShowNetworkError()
    is AppError.AuthError -> NavigateToLogin()
    else -> ShowGenericError()
}
```

## Testing Strategy

### Unit Tests
- Use Cases: Test business logic
- ViewModels: Test state management
- Repositories: Test data operations (with mocks)

### Integration Tests
- Repository + Data Source: Test data flow
- Navigation: Test screen transitions

### UI Tests
- Compose Testing: Test user interactions
- Screenshot Tests: Visual regression

```kotlin
// Example ViewModel test
@Test
fun `when clone repository succeeds, state is updated correctly`() = runTest {
    // Given
    val repository = TestData.repository
    coEvery { cloneRepoUseCase(any(), any()) } returns flowOf(Result.Success(repository))
    
    // When
    viewModel.cloneRepository("url", "path")
    
    // Then
    assertEquals(CloneUiState(repository = repository), viewModel.uiState.value)
}
```

## Performance Considerations

### Optimization Techniques
1. **Lazy Loading**: Load data on demand
2. **Pagination**: Load large lists in chunks
3. **Caching**: Cache frequently accessed data
4. **Background Processing**: Move heavy operations off main thread
5. **Image Loading**: Use Coil for efficient image loading
6. **Database Indexing**: Index frequently queried columns

### Memory Management
- Use `collectAsStateWithLifecycle()` to avoid leaks
- Clear references in ViewModel.onCleared()
- Use `Flow` instead of `LiveData` for better performance
- Implement proper cancellation for coroutines

## Security and Privacy Architecture

### Core Security Components

#### SecurityManager
Centralized security operations and cryptographic functions.

```kotlin
core:security/
├── SecurityManager.kt          # Main security operations
├── CryptoUtils.kt              # Cryptographic utilities
├── InputValidator.kt           # Input validation and sanitization
└── KeystoreManager.kt          # Android Keystore operations
```

**Features:**
- AES-256-GCM encryption for sensitive data
- Android Keystore integration (hardware-backed when available)
- Input validation and sanitization
- Security threat detection (root, debugger)
- Secure random number generation

**Standards Compliance:**
- NIST SP 800-53 (Security Controls)
- OWASP MASVS Level 2
- FIPS 140-2 (Cryptographic algorithms)
- ISO/IEC 27001 (Information Security)

#### PrivacyManager
User privacy controls and data management.

```kotlin
core:privacy/
├── PrivacyManager.kt           # Privacy operations
├── DataController.kt           # User data control
├── ConsentManager.kt           # Consent management
└── AuditLogger.kt              # Privacy audit trail
```

**Features:**
- User data export (GDPR Article 20)
- User data deletion (GDPR Article 17)
- Privacy settings management
- Consent tracking and management
- Privacy audit logging
- Analytics data anonymization

**Regulatory Compliance:**
- GDPR (General Data Protection Regulation)
- CCPA (California Consumer Privacy Act)
- ISO/IEC 27701 (Privacy Information Management)
- LGPD (Brazilian Data Protection Law)

#### ComplianceManager
Standards and regulatory compliance tracking.

```kotlin
core:compliance/
├── ComplianceManager.kt        # Compliance tracking
├── SecurityControls.kt         # Security control implementation
├── PrivacyControls.kt          # Privacy control implementation
└── QualityMetrics.kt           # Quality management metrics
```

**Standards Tracked:**
- ISO/IEC 27001 (Information Security)
- ISO/IEC 27701 (Privacy Management)
- ISO 9001 (Quality Management)
- NIST Cybersecurity Framework
- OWASP Mobile Security
- IEEE Software Engineering Standards (730, 828, 829, 1012)

### Security Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│  (UI Components with security-aware implementations)    │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│                  Security Middleware                     │
│  ├─ Authentication & Authorization                      │
│  ├─ Input Validation & Sanitization                     │
│  ├─ Privacy Controls & Consent Management               │
│  └─ Audit Logging & Monitoring                          │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│                   Business Logic                         │
│  (Domain Layer with security checks)                    │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│                Data Security Layer                       │
│  ├─ Encryption at Rest (AES-256-GCM)                   │
│  ├─ Secure Storage (Android Keystore)                   │
│  ├─ Database Encryption (SQLCipher)                     │
│  └─ Secure Preferences (EncryptedSharedPreferences)     │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│            Network Security Layer                        │
│  ├─ TLS 1.3 Enforcement                                 │
│  ├─ Certificate Pinning                                 │
│  ├─ No Cleartext Traffic                                │
│  └─ Secure DNS (DNS-over-HTTPS)                         │
└─────────────────────────────────────────────────────────┘
```

### Data Protection

#### Encryption at Rest
```kotlin
// AES-256-GCM encryption using Android Keystore
val securityManager = SecurityManager(context)
val encrypted = securityManager.encryptData(sensitiveData, "credential_key")
```

**Implementation:**
- Algorithm: AES-256-GCM (Galois/Counter Mode)
- Key Storage: Android Keystore System
- Hardware Backing: Required when available (StrongBox)
- Key Size: 256 bits
- IV Generation: Secure random per encryption

#### Encryption in Transit
```xml
<!-- network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Certificate pinning for critical services -->
    <domain-config>
        <domain includeSubdomains="true">api.github.com</domain>
        <pin-set>
            <pin digest="SHA-256">primary_certificate_pin</pin>
            <pin digest="SHA-256">backup_certificate_pin</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

**Network Security:**
- Protocol: TLS 1.3 (minimum TLS 1.2)
- Certificate Validation: Strict
- Certificate Pinning: Enabled for GitHub, GitLab APIs
- Cleartext Traffic: Blocked
- Cipher Suites: AEAD ciphers only

#### Secure Storage
```kotlin
// Credential storage in Android Keystore
credentials → SecurityManager.encryptData() → Android Keystore → Encrypted SharedPreferences

// Database encryption
Database → SQLCipher (AES-256-CBC) → Encrypted File → Device Storage
```

### Privacy by Design

#### Data Minimization
- Collect only necessary data for functionality
- No advertising or tracking SDKs
- Analytics opt-in only (default: disabled)
- Anonymous aggregated data only

#### User Control
```kotlin
// Privacy settings
Settings → Privacy → {
    - View My Data (GDPR Article 15)
    - Export My Data (GDPR Article 20)
    - Delete My Data (GDPR Article 17)
    - Manage Consent
    - Privacy Audit Log
}
```

#### Privacy Audit Trail
```kotlin
// All privacy-related actions logged
PrivacyEvent(
    type = DATA_EXPORT_REQUESTED,
    timestamp = Date(),
    userId = hashedUserId,
    details = "User requested data export"
)
```

### Authentication and Authorization

#### OAuth 2.0 Implementation
```kotlin
// GitHub OAuth with PKCE
GitHubAuth.authorize()
    .withPKCE()
    .withScope("repo", "user")
    .execute()
    
// Token storage
accessToken → encrypt() → Android Keystore
refreshToken → encrypt() → Android Keystore
```

#### Biometric Authentication
```kotlin
// Biometric-protected operations
BiometricPrompt.authenticate(
    cryptoObject = CryptoObject(cipher),
    callback = { result ->
        if (result.authenticated) {
            performSensitiveOperation()
        }
    }
)
```

### Security Monitoring

#### Threat Detection
```kotlin
// Security checks
if (securityManager.isDeviceRooted()) {
    showSecurityWarning()
}

if (securityManager.isDebuggerAttached()) {
    logSecurityEvent(DEBUGGER_DETECTED)
}
```

#### Audit Logging
```kotlin
// Security events logged
SecurityEvent(
    type = FAILED_AUTHENTICATION,
    timestamp = Date(),
    severity = HIGH,
    details = "Failed login attempt",
    ipAddress = null // Privacy-preserving
)
```

### Input Validation and Sanitization

```kotlin
// Prevent injection attacks
val gitUrl = userInput
if (!securityManager.validateInput(gitUrl, InputValidationType.GIT_URL)) {
    throw SecurityException("Invalid Git URL")
}

val sanitized = securityManager.sanitizeInput(gitUrl, InputValidationType.GIT_URL)
```

**Protection Against:**
- Command injection
- Path traversal
- SQL injection
- XSS (in WebView components)
- SSRF (Server-Side Request Forgery)

### Secure Development Practices

#### Code Security
- Static analysis (Android Lint, SpotBugs, SonarQube)
- Dependency scanning (OWASP Dependency-Check)
- Security-focused code reviews
- ProGuard/R8 obfuscation (release builds)
- No hardcoded secrets or credentials

#### Testing
- Security unit tests
- Penetration testing (annual)
- Fuzzing (automated)
- SAST/DAST scanning
- Third-party security audits

### Compliance Documentation

For detailed compliance information, see:
- [PRIVACY.md](PRIVACY.md) - Privacy policy and data protection
- [SECURITY.md](SECURITY.md) - Security standards and practices
- [COMPLIANCE.md](COMPLIANCE.md) - Regulatory and standards compliance

## Monitoring and Logging

```kotlin
// Centralized logging
object Logger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        // Send to crash reporting service
    }
}
```

## Build Configuration

### Flavors
- `dev`: Development build with debug tools
- `staging`: Staging environment
- `production`: Production release

### Build Types
- `debug`: Debug symbols, logging enabled
- `release`: Optimized, obfuscated, signed

```gradle
productFlavors {
    dev {
        applicationIdSuffix = ".dev"
        versionNameSuffix = "-dev"
    }
    production {
        // Production configuration
    }
}
```
