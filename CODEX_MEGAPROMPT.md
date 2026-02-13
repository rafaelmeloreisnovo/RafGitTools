# 🤖 MEGA-PROMPT CODEX - RafGitTools Implementation Plan

**Target**: GitHub Codex / Claude Code / AI Development Assistant  
**Project**: RafGitTools Android App  
**Priority**: CRITICAL - Test Coverage + Missing Features  
**Timeline**: 12 semanas (3 meses)  
**Complexity**: HIGH

---

## 📋 CONTEXTO EXECUTIVO

Você está sendo contratado para implementar features críticas e testes no **RafGitTools**, um cliente Git/GitHub para Android que combina as melhores features de FastHub, MGit, PuppyGit e Termux.

### Situação Atual
- ✅ **Arquitetura sólida**: Clean Architecture + MVVM + Hilt
- ✅ **Core funcional**: 25+ operações Git, 50+ endpoints GitHub API
- ✅ **UI rica**: 18 telas Compose funcionais
- 🔴 **Test coverage CRÍTICO**: 5.8% (meta: 80%)
- 🔴 **Features faltantes**: Terminal, Multi-platform, GPG, LFS
- ⚠️ **Docs incompletas**: 10 guias técnicos faltando

### Seu Objetivo
Implementar testes, features críticas e documentação seguindo os padrões RAFAELIA de qualidade (Φ_ethica, coerência, precisão).

---

## 🎯 MISSÕES POR PRIORIDADE

### 🔴 MISSÃO 1: TESTES (SEMANAS 1-4)
**Objetivo**: Elevar coverage de 5.8% → 40% mínimo (80% ideal)  
**Criticidade**: MÁXIMA  
**Blockers**: Nenhum

#### 1.1 Testing Infrastructure

**Criar**: `app/src/test/kotlin/com/rafgittools/TestUtils.kt`
```kotlin
package com.rafgittools

import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Utilities para testes do RafGitTools
 * 
 * Padrões:
 * - MockK para mocking
 * - Coroutines Test para async
 * - JUnit 4 como framework base
 * 
 * Standards: IEEE 829, ISO 25010
 */
object TestFixtures {
    // Git fixtures
    fun createMockGitRepository() = GitRepository(
        id = "test-repo",
        name = "TestRepo",
        path = "/tmp/test",
        remoteUrl = "https://github.com/test/repo",
        currentBranch = "main",
        lastUpdated = System.currentTimeMillis()
    )
    
    fun createMockGitCommit(message: String = "Test commit") = GitCommit(
        hash = "abc123",
        shortHash = "abc123",
        author = "Test Author",
        email = "test@example.com",
        timestamp = System.currentTimeMillis(),
        message = message,
        parentHashes = emptyList()
    )
    
    // GitHub API fixtures
    fun createMockIssue(number: Int = 1) = GithubIssue(
        id = number.toLong(),
        number = number,
        title = "Test Issue $number",
        body = "Test body",
        state = "open",
        author = "testuser",
        labels = emptyList(),
        comments = 0,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )
    
    // Credentials fixtures
    fun createTokenCredentials() = Credentials.Token("test_token_123")
    fun createSshCredentials() = Credentials.SshKey(
        privateKeyPath = "/tmp/id_ed25519",
        passphrase = null
    )
}

class CoroutineTestRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

#### 1.2 Core Service Tests (PRIORITY 1)

**Criar**: `app/src/test/kotlin/com/rafgittools/data/git/JGitServiceTest.kt`
```kotlin
package com.rafgittools.data.git

import com.rafgittools.*
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/**
 * Testes para JGitService
 * 
 * Coverage target: 80%
 * Test cases: 25+ (uma para cada operação Git)
 * 
 * Standards: IEEE 829, Git Protocol v2
 */
class JGitServiceTest {
    
    @get:Rule
    val coroutineRule = CoroutineTestRule()
    
    private lateinit var jgitService: JGitService
    private lateinit var mockDiffAuditLogger: DiffAuditLogger
    
    @Before
    fun setup() {
        mockDiffAuditLogger = mockk(relaxed = true)
        jgitService = JGitService(mockDiffAuditLogger)
    }
    
    @Test
    fun `cloneRepository with token auth should succeed`() = runTest {
        // Given
        val url = "https://github.com/test/repo"
        val localPath = "/tmp/test-repo"
        val credentials = TestFixtures.createTokenCredentials()
        
        // When
        val result = jgitService.cloneRepository(url, localPath, credentials)
        
        // Then
        assertTrue(result.isSuccess)
        val repo = result.getOrNull()
        assertNotNull(repo)
        assertEquals("test-repo", repo?.name)
        assertEquals(url, repo?.remoteUrl)
    }
    
    @Test
    fun `cloneRepository with SSH auth should succeed`() = runTest {
        // Given
        val url = "git@github.com:test/repo.git"
        val localPath = "/tmp/test-repo-ssh"
        val credentials = TestFixtures.createSshCredentials()
        
        // When
        val result = jgitService.cloneRepository(url, localPath, credentials)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `cloneRepository with invalid URL should fail`() = runTest {
        // Given
        val url = "invalid-url"
        val localPath = "/tmp/test"
        
        // When
        val result = jgitService.cloneRepository(url, localPath, null)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
    
    @Test
    fun `commitChanges should create commit with message`() = runTest {
        // TODO: Implementar test de commit
        // Usar repositório temporário para teste
        fail("Not implemented")
    }
    
    @Test
    fun `pushChanges should push to remote`() = runTest {
        // TODO: Implementar test de push
        fail("Not implemented")
    }
    
    @Test
    fun `pullChanges should fetch and merge`() = runTest {
        // TODO: Implementar test de pull
        fail("Not implemented")
    }
    
    @Test
    fun `createBranch should create new branch`() = runTest {
        // TODO: Implementar test de branch
        fail("Not implemented")
    }
    
    @Test
    fun `deleteBranch should remove branch`() = runTest {
        // TODO: Implementar test de branch delete
        fail("Not implemented")
    }
    
    @Test
    fun `mergeBranch should merge successfully`() = runTest {
        // TODO: Implementar test de merge
        fail("Not implemented")
    }
    
    @Test
    fun `stashChanges should stash working directory`() = runTest {
        // TODO: Implementar test de stash
        fail("Not implemented")
    }
    
    @Test
    fun `getStatus should return correct status`() = runTest {
        // TODO: Implementar test de status
        fail("Not implemented")
    }
    
    // ... continuar com todos os 25+ testes
}
```

**Criar**: `app/src/test/kotlin/com/rafgittools/data/github/GithubApiServiceTest.kt`
```kotlin
package com.rafgittools.data.github

import com.rafgittools.*
import io.mockk.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.*
import org.junit.*

/**
 * Testes para GitHub API Service
 * 
 * Usa MockWebServer para simular API GitHub
 * Coverage target: 80%
 * Test cases: 50+ (um para cada endpoint)
 */
class GithubApiServiceTest {
    
    @get:Rule
    val coroutineRule = CoroutineTestRule()
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: GithubApiService
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        // Setup Retrofit com base URL do mockWebServer
        apiService = createMockApiService(mockWebServer.url("/"))
    }
    
    @After
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `getRepositories should return list of repos`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""[{"id":1,"name":"repo1"}]"""))
        
        // When
        val result = apiService.getRepositories()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("repo1", result[0].name)
    }
    
    @Test
    fun `createIssue should POST new issue`() = runTest {
        // TODO: Implementar test de criação de issue
        fail("Not implemented")
    }
    
    // ... continuar com todos os 50+ testes de API
}
```

#### 1.3 Use Case Tests (PRIORITY 2)

**Template para cada Use Case**:
```kotlin
package com.rafgittools.domain.usecase.git

import com.rafgittools.*
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.*

class [UseCaseName]Test {
    
    @get:Rule
    val coroutineRule = CoroutineTestRule()
    
    private lateinit var useCase: [UseCaseName]
    private lateinit var mockRepository: GitRepository
    
    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = [UseCaseName](mockRepository)
    }
    
    @Test
    fun `invoke should call repository method`() = runTest {
        // Given
        coEvery { mockRepository.someMethod() } returns Result.success(/* data */)
        
        // When
        val result = useCase.invoke(/* params */)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockRepository.someMethod() }
    }
    
    @Test
    fun `invoke with error should return failure`() = runTest {
        // Given
        coEvery { mockRepository.someMethod() } returns 
            Result.failure(Exception("Test error"))
        
        // When
        val result = useCase.invoke(/* params */)
        
        // Then
        assertTrue(result.isFailure)
    }
}
```

**Implementar testes para**:
- `PushChangesUseCaseTest` ✅
- `PullChangesUseCaseTest` ✅
- `CommitChangesUseCaseTest` ✅
- `GetBranchesUseCaseTest` ✅
- `IssueUseCasesTest` ✅
- `PullRequestUseCasesTest` ✅
- Etc. (20+ use cases)

#### 1.4 ViewModel Tests (PRIORITY 3)

**Template para ViewModels**:
```kotlin
package com.rafgittools.ui.screens.[screen]

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rafgittools.*
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.*

class [Screen]ViewModelTest {
    
    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()
    
    @get:Rule
    val coroutineRule = CoroutineTestRule()
    
    private lateinit var viewModel: [Screen]ViewModel
    private lateinit var mockUseCase: [UseCase]
    
    @Before
    fun setup() {
        mockUseCase = mockk()
        viewModel = [Screen]ViewModel(mockUseCase)
    }
    
    @Test
    fun `initial state should be correct`() {
        // Then
        assertEquals([ExpectedState], viewModel.state.value)
    }
    
    @Test
    fun `action should update state correctly`() = runTest {
        // Given
        coEvery { mockUseCase.invoke() } returns Result.success(/* data */)
        
        // When
        viewModel.handleAction([Action])
        
        // Then
        advanceUntilIdle()
        assertEquals([ExpectedState], viewModel.state.value)
    }
    
    @Test
    fun `error should update state with error`() = runTest {
        // Given
        coEvery { mockUseCase.invoke() } returns 
            Result.failure(Exception("Test error"))
        
        // When
        viewModel.handleAction([Action])
        
        // Then
        advanceUntilIdle()
        assertTrue(viewModel.state.value is [ErrorState])
    }
}
```

**Implementar ViewModels a testar** (18 telas):
- `AuthViewModelTest`
- `HomeViewModelTest`
- `RepositoryListViewModelTest`
- `RepositoryDetailViewModelTest`
- `CommitListViewModelTest`
- `BranchesViewModelTest`
- `IssueListViewModelTest`
- `IssueDetailViewModelTest`
- `PullRequestListViewModelTest`
- Etc.

#### 1.5 Integration Tests (PRIORITY 4)

**Criar**: `app/src/androidTest/kotlin/com/rafgittools/integration/GitIntegrationTest.kt`
```kotlin
package com.rafgittools.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rafgittools.data.git.JGitService
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

/**
 * Integration tests para operações Git completas
 * 
 * Usa repositório real temporário
 * Testa fluxos end-to-end
 */
@RunWith(AndroidJUnit4::class)
class GitIntegrationTest {
    
    private lateinit var jgitService: JGitService
    private lateinit var testDir: File
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "git-test-${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        jgitService = JGitService(/* dependencies */)
    }
    
    @After
    fun teardown() {
        testDir.deleteRecursively()
    }
    
    @Test
    fun `full Git workflow should work`() {
        // Test: clone → modify → commit → push
        // TODO: Implementar fluxo completo
    }
    
    @Test
    fun `branch workflow should work`() {
        // Test: create branch → checkout → commit → merge
        // TODO: Implementar
    }
}
```

#### 1.6 UI Tests (PRIORITY 5)

**Criar**: `app/src/androidTest/kotlin/com/rafgittools/ui/AuthScreenTest.kt`
```kotlin
package com.rafgittools.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rafgittools.ui.screens.auth.AuthScreen
import org.junit.*

class AuthScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `login button should be disabled with empty token`() {
        // Given
        composeTestRule.setContent {
            AuthScreen(/* ... */)
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Login")
            .assertIsNotEnabled()
    }
    
    @Test
    fun `entering token should enable login button`() {
        // Given
        composeTestRule.setContent {
            AuthScreen(/* ... */)
        }
        
        // When
        composeTestRule
            .onNodeWithTag("tokenField")
            .performTextInput("ghp_test_token_123")
        
        // Then
        composeTestRule
            .onNodeWithText("Login")
            .assertIsEnabled()
    }
}
```

**Implementar UI tests para**:
- Login flow
- Repository list
- Issue creation
- PR creation
- Navigation

---

### 🟡 MISSÃO 2: DOCUMENTAÇÃO (SEMANAS 2-3)
**Objetivo**: Criar guias técnicos faltantes  
**Criticidade**: ALTA  
**Blockers**: Nenhum

#### 2.1 Testing Guide

**Criar**: `docs/TESTING_GUIDE.md`
```markdown
# RafGitTools - Testing Guide

## 📋 Overview

Este guia cobre tudo sobre testes no RafGitTools.

## 🎯 Objetivos de Cobertura

- **Unit Tests**: 80% minimum
- **Integration Tests**: 60% minimum
- **UI Tests**: 40% minimum
- **E2E Tests**: 20% minimum

## 🏗️ Test Architecture

### Test Pyramid
```
     /\
    /UI\     ← 10% (UI Tests)
   /────\
  /Integ\    ← 20% (Integration)
 /──────\
/  Unit  \   ← 70% (Unit Tests)
──────────
```

## 🛠️ Setup

### Dependencies

```gradle
testImplementation "junit:junit:4.13.2"
testImplementation "io.mockk:mockk:1.14.7"
testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3"
testImplementation "androidx.arch.core:core-testing:2.2.0"

androidTestImplementation "androidx.test.ext:junit:1.1.5"
androidTestImplementation "androidx.compose.ui:ui-test-junit4"
```

### Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew connectedAndroidTest

# Specific test
./gradlew test --tests JGitServiceTest

# Coverage report
./gradlew testDebugUnitTestCoverage
```

## ✍️ Writing Tests

### Unit Test Template

[Incluir templates completos aqui...]

### Best Practices

1. **AAA Pattern**: Arrange, Act, Assert
2. **One assertion per test**: Foco em um comportamento
3. **Descriptive names**: Usar backticks para nomes descritivos
4. **MockK over Mockito**: Usar MockK para Kotlin
5. **Test fixtures**: Usar TestFixtures para dados mock

### Example: JGit Service Test

[Incluir exemplo completo aqui...]

### Example: ViewModel Test

[Incluir exemplo completo aqui...]

## 🔧 Troubleshooting

### Common Issues

**Problem**: Coroutines don't complete in tests  
**Solution**: Use `runTest` and `advanceUntilIdle()`

**Problem**: StateFlow doesn't update  
**Solution**: Use `InstantTaskExecutorRule` for LiveData/StateFlow

[Etc.]

## 📊 Coverage Reports

Reports são gerados em `app/build/reports/coverage/`

Para visualizar:
```bash
open app/build/reports/coverage/index.html
```

## 🎓 Resources

- [JUnit 4 Guide](...)
- [MockK Documentation](...)
- [Coroutines Testing](...)

---

**Standards**: IEEE 829, ISO 25010  
**Last Updated**: 2026-02-13
```

#### 2.2 API Integration Guide

**Criar**: `docs/API_INTEGRATION_GUIDE.md`
```markdown
# RafGitTools - API Integration Guide

## 📋 Overview

Como integrar e usar as APIs do RafGitTools.

## 🔌 GitHub API

### Setup

A API GitHub é implementada via Retrofit em `GithubApiService.kt`.

### Authentication

```kotlin
// Token authentication
val credentials = Credentials.Token("ghp_your_token_here")

// Use via repository
val repositories = githubRepository.getRepositories(credentials)
```

### Available Endpoints

#### Repositories

```kotlin
// List user repositories
suspend fun getRepositories(): List<GithubRepository>

// Get specific repository
suspend fun getRepository(owner: String, repo: String): GithubRepository

// Search repositories
suspend fun searchRepositories(query: String): List<GithubRepository>

// Fork repository
suspend fun forkRepository(owner: String, repo: String): GithubRepository

// Star/unstar repository
suspend fun starRepository(owner: String, repo: String)
suspend fun unstarRepository(owner: String, repo: String)
```

#### Issues

[Documentar todos os endpoints...]

#### Pull Requests

[Documentar todos os endpoints...]

### Rate Limiting

GitHub API tem rate limits:
- **Authenticated**: 5,000 requests/hour
- **Unauthenticated**: 60 requests/hour

Handling rate limits:
```kotlin
try {
    val result = githubApi.getRepositories()
} catch (e: HttpException) {
    if (e.code() == 403) {
        // Check X-RateLimit headers
        val resetTime = e.response()?.headers()?.get("X-RateLimit-Reset")
        // Wait until reset
    }
}
```

### Pagination

[Documentar como lidar com paginação...]

### Error Handling

[Documentar tratamento de erros...]

## 🔗 GitLab API (Coming Soon)

[Placeholder para futura implementação]

## 📊 Best Practices

1. Always use tokens, never passwords
2. Cache responses when possible
3. Handle rate limits gracefully
4. Use conditional requests (ETags)
5. Implement retry logic

---

**Standards**: OpenAPI 3.0, RFC 7231  
**Last Updated**: 2026-02-13
```

#### 2.3 JGit Operations Guide

**Criar**: `docs/JGIT_OPERATIONS_GUIDE.md`
```markdown
# RafGitTools - JGit Operations Guide

## 📋 Overview

Guia completo de operações Git usando JGit.

## 🔧 Basic Operations

### Clone Repository

```kotlin
// Token authentication
val result = jgitService.cloneRepository(
    url = "https://github.com/user/repo",
    localPath = "/path/to/local",
    credentials = Credentials.Token("ghp_token")
)

// SSH authentication
val result = jgitService.cloneRepository(
    url = "git@github.com:user/repo.git",
    localPath = "/path/to/local",
    credentials = Credentials.SshKey(
        privateKeyPath = "/path/to/id_ed25519",
        passphrase = "optional_passphrase"
    )
)

// Handle result
result.onSuccess { repo ->
    println("Cloned: ${repo.name}")
}.onFailure { error ->
    println("Error: ${error.message}")
}
```

### Commit Changes

[Documentar com exemplos...]

### Push/Pull

[Documentar com exemplos...]

### Branching

[Documentar com exemplos...]

### Stashing

[Documentar com exemplos...]

### Tagging

[Documentar com exemplos...]

## 🔐 SSH Authentication

### Generating Keys

```kotlin
val keyPair = sshKeyManager.generateKey(
    algorithm = SshKeyAlgorithm.ED25519,
    path = "/path/to/key",
    passphrase = "optional_passphrase"
)
```

### Using SSH Keys

[Documentar...]

## 🎯 Advanced Operations

### Rebase

[Documentar...]

### Cherry-pick

[Documentar...]

### Reflog

[Documentar...]

### Blame

[Documentar...]

## ⚠️ Common Issues

### Issue: Authentication failed
**Solution**: Verify credentials and permissions

### Issue: Merge conflicts
**Solution**: Use conflict resolution API

[Etc.]

---

**Standards**: Git Protocol v2, RFC 4253  
**Last Updated**: 2026-02-13
```

#### 2.4 Outros Docs Faltantes

**Criar também**:
- `docs/UI_SCREENS_MAP.md` - Mapa navegação + componentes
- `docs/DATABASE_SCHEMA.md` - Diagrama ER + migrations
- `docs/ERROR_CODES.md` - Catálogo erros
- `docs/TROUBLESHOOTING.md` - Problemas comuns
- `docs/PERFORMANCE_GUIDE.md` - Profiling + otimização
- `docs/CHANGELOG_TECHNICAL.md` - Changelog técnico
- `docs/MIGRATION_GUIDE.md` - Migrações de versão

---

### 🟠 MISSÃO 3: TERMINAL EMULATION MVP (SEMANAS 5-8)
**Objetivo**: Implementar terminal básico funcional  
**Criticidade**: ALTA  
**Blockers**: Nenhum  
**Features**: #145-162 (18 features, MVP = 4-6 features)

#### 3.1 Terminal Core

**Criar**: `app/src/main/kotlin/com/rafgittools/core/terminal/`

**Files necessários**:
1. `TerminalEmulator.kt` - Core emulator (VT100/ANSI)
2. `PtyProcess.kt` - PTY process handling
3. `AnsiParser.kt` - ANSI escape code parser
4. `TerminalBuffer.kt` - Screen buffer
5. `KeyHandler.kt` - Keyboard input
6. `TerminalColors.kt` - Color schemes

**Exemplo**: `TerminalEmulator.kt`
```kotlin
package com.rafgittools.core.terminal

import kotlinx.coroutines.flow.StateFlow

/**
 * Terminal Emulator Core
 * 
 * Implements VT100/VT220 terminal emulation
 * 
 * Standards:
 * - ECMA-48 (ANSI escape codes)
 * - ISO 6429
 * - VT100/VT220 specifications
 */
class TerminalEmulator(
    private val rows: Int = 24,
    private val cols: Int = 80
) {
    
    private val buffer = TerminalBuffer(rows, cols)
    private val ansiParser = AnsiParser()
    
    val screenState: StateFlow<TerminalScreen>
        get() = buffer.screenState
    
    /**
     * Process input data (from PTY)
     */
    fun processInput(data: ByteArray) {
        val text = String(data, Charsets.UTF_8)
        ansiParser.parse(text).forEach { command ->
            when (command) {
                is AnsiCommand.Print -> buffer.print(command.text)
                is AnsiCommand.MoveCursor -> buffer.moveCursor(command.row, command.col)
                is AnsiCommand.ClearScreen -> buffer.clearScreen()
                is AnsiCommand.SetColor -> buffer.setColor(command.fg, command.bg)
                // ... outros comandos
            }
        }
    }
    
    /**
     * Send keyboard input (to PTY)
     */
    fun sendKey(key: Key): ByteArray {
        return when (key) {
            is Key.Character -> key.char.toString().toByteArray()
            is Key.Enter -> "\r".toByteArray()
            is Key.Backspace -> byteArrayOf(0x7F)
            is Key.ArrowUp -> "\u001b[A".toByteArray()
            is Key.ArrowDown -> "\u001b[B".toByteArray()
            // ... outras teclas
        }
    }
    
    // ... mais métodos
}

data class TerminalScreen(
    val lines: List<TerminalLine>,
    val cursorRow: Int,
    val cursorCol: Int
)

data class TerminalLine(
    val text: String,
    val attributes: List<CellAttribute>
)

sealed class AnsiCommand {
    data class Print(val text: String) : AnsiCommand()
    data class MoveCursor(val row: Int, val col: Int) : AnsiCommand()
    object ClearScreen : AnsiCommand()
    data class SetColor(val fg: Int, val bg: Int) : AnsiCommand()
}

sealed class Key {
    data class Character(val char: Char) : Key()
    object Enter : Key()
    object Backspace : Key()
    object ArrowUp : Key()
    object ArrowDown : Key()
    // ... outras teclas
}
```

#### 3.2 PTY Integration

**Criar**: `app/src/main/kotlin/com/rafgittools/core/terminal/PtyProcess.kt`
```kotlin
package com.rafgittools.core.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*

/**
 * PTY (Pseudo-Terminal) Process Handler
 * 
 * Gerencia processo shell via PTY
 * 
 * Standards: POSIX PTY (IEEE 1003.1)
 */
class PtyProcess(
    private val command: String = "/system/bin/sh",
    private val environment: Map<String, String> = emptyMap()
) {
    
    private var process: Process? = null
    private var outputReader: BufferedReader? = null
    private var inputWriter: BufferedWriter? = null
    
    private val _output = MutableSharedFlow<ByteArray>()
    val output: SharedFlow<ByteArray> = _output.asSharedFlow()
    
    /**
     * Start PTY process
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            // Setup environment
            val envArray = environment.map { "${it.key}=${it.value}" }.toTypedArray()
            
            // Start process
            process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .apply { 
                    environment().putAll(this@PtyProcess.environment)
                }
                .start()
            
            // Setup I/O streams
            outputReader = BufferedReader(InputStreamReader(process!!.inputStream))
            inputWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            
            // Start reading output
            startOutputReader()
            
        } catch (e: Exception) {
            throw TerminalException("Failed to start PTY process", e)
        }
    }
    
    /**
     * Write input to process
     */
    suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            inputWriter?.write(String(data, Charsets.UTF_8))
            inputWriter?.flush()
        } catch (e: IOException) {
            throw TerminalException("Failed to write to PTY", e)
        }
    }
    
    /**
     * Stop process
     */
    fun stop() {
        process?.destroy()
        outputReader?.close()
        inputWriter?.close()
    }
    
    private fun startOutputReader() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputReader?.let { reader ->
                    val buffer = CharArray(4096)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count == -1) break
                        
                        val data = String(buffer, 0, count).toByteArray()
                        _output.emit(data)
                    }
                }
            } catch (e: IOException) {
                // Process ended
            }
        }
    }
}

class TerminalException(message: String, cause: Throwable? = null) 
    : Exception(message, cause)
```

#### 3.3 Terminal UI Screen

**Criar**: `app/src/main/kotlin/com/rafgittools/ui/screens/terminal/TerminalScreen.kt`
```kotlin
package com.rafgittools.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel
) {
    val state by viewModel.state.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        // Terminal output
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TerminalOutput(
                lines = state.lines,
                cursorRow = state.cursorRow,
                cursorCol = state.cursorCol
            )
        }
        
        // Input field
        TerminalInput(
            onInput = { char ->
                viewModel.handleInput(char)
            }
        )
    }
}

@Composable
private fun TerminalOutput(
    lines: List<TerminalLine>,
    cursorRow: Int,
    cursorCol: Int
) {
    Column {
        lines.forEachIndexed { index, line ->
            TerminalLineView(
                line = line,
                showCursor = index == cursorRow,
                cursorCol = cursorCol
            )
        }
    }
}

@Composable
private fun TerminalLineView(
    line: TerminalLine,
    showCursor: Boolean,
    cursorCol: Int
) {
    Text(
        text = line.text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color.White
        )
    )
}

@Composable
private fun TerminalInput(
    onInput: (Char) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    BasicTextField(
        value = text,
        onValueChange = { newText ->
            if (newText.length > text.length) {
                val newChar = newText.last()
                onInput(newChar)
            }
            text = ""
        },
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(8.dp)
    )
}
```

#### 3.4 Terminal ViewModel

**Criar**: `app/src/main/kotlin/com/rafgittools/ui/screens/terminal/TerminalViewModel.kt`
```kotlin
package com.rafgittools.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.core.terminal.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor() : ViewModel() {
    
    private val terminalEmulator = TerminalEmulator(rows = 24, cols = 80)
    private val ptyProcess = PtyProcess(command = "/system/bin/sh")
    
    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()
    
    init {
        startTerminal()
    }
    
    private fun startTerminal() {
        viewModelScope.launch {
            // Start PTY process
            ptyProcess.start()
            
            // Collect PTY output
            ptyProcess.output.collect { data ->
                terminalEmulator.processInput(data)
            }
            
            // Collect emulator state
            terminalEmulator.screenState.collect { screen ->
                _state.value = TerminalState(
                    lines = screen.lines,
                    cursorRow = screen.cursorRow,
                    cursorCol = screen.cursorCol
                )
            }
        }
    }
    
    fun handleInput(char: Char) {
        viewModelScope.launch {
            val key = Key.Character(char)
            val data = terminalEmulator.sendKey(key)
            ptyProcess.write(data)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ptyProcess.stop()
    }
}

data class TerminalState(
    val lines: List<TerminalLine> = emptyList(),
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val isRunning: Boolean = true
)
```

#### 3.5 Terminal Tests

**Criar testes para**:
- `TerminalEmulatorTest.kt`
- `PtyProcessTest.kt`
- `AnsiParserTest.kt`
- `TerminalViewModelTest.kt`

---

### 🟡 MISSÃO 4: MULTI-PLATFORM (SEMANAS 9-12)
**Objetivo**: Suporte básico para GitLab  
**Criticidade**: MÉDIA  
**Blockers**: Nenhum  
**Features**: #199-216 (MVP = GitLab API basic)

#### 4.1 GitLab API Service

**Criar**: `app/src/main/kotlin/com/rafgittools/data/gitlab/GitLabApiService.kt`

```kotlin
package com.rafgittools.data.gitlab

import retrofit2.http.*

/**
 * GitLab API Service
 * 
 * Implements GitLab REST API v4
 * 
 * Standards: 
 * - GitLab API v4
 * - OpenAPI 3.0
 * - OAuth 2.0
 * 
 * Docs: https://docs.gitlab.com/ee/api/
 */
interface GitLabApiService {
    
    // Projects (equivalent to GitHub Repositories)
    @GET("projects")
    suspend fun getProjects(
        @Header("PRIVATE-TOKEN") token: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): List<GitLabProject>
    
    @GET("projects/{id}")
    suspend fun getProject(
        @Path("id") projectId: String,
        @Header("PRIVATE-TOKEN") token: String
    ): GitLabProject
    
    // Issues
    @GET("projects/{id}/issues")
    suspend fun getIssues(
        @Path("id") projectId: String,
        @Header("PRIVATE-TOKEN") token: String,
        @Query("state") state: String = "opened"
    ): List<GitLabIssue>
    
    @POST("projects/{id}/issues")
    suspend fun createIssue(
        @Path("id") projectId: String,
        @Header("PRIVATE-TOKEN") token: String,
        @Body request: CreateIssueRequest
    ): GitLabIssue
    
    // Merge Requests (equivalent to GitHub Pull Requests)
    @GET("projects/{id}/merge_requests")
    suspend fun getMergeRequests(
        @Path("id") projectId: String,
        @Header("PRIVATE-TOKEN") token: String,
        @Query("state") state: String = "opened"
    ): List<GitLabMergeRequest>
    
    @POST("projects/{id}/merge_requests")
    suspend fun createMergeRequest(
        @Path("id") projectId: String,
        @Header("PRIVATE-TOKEN") token: String,
        @Body request: CreateMergeRequestRequest
    ): GitLabMergeRequest
    
    // User
    @GET("user")
    suspend fun getCurrentUser(
        @Header("PRIVATE-TOKEN") token: String
    ): GitLabUser
    
    // ... mais endpoints
}

// Data models
data class GitLabProject(
    val id: Int,
    val name: String,
    val description: String?,
    val web_url: String,
    val default_branch: String,
    val visibility: String,
    val created_at: String,
    val last_activity_at: String
)

data class GitLabIssue(
    val id: Int,
    val iid: Int,
    val project_id: Int,
    val title: String,
    val description: String?,
    val state: String,
    val created_at: String,
    val updated_at: String,
    val author: GitLabUser
)

data class GitLabMergeRequest(
    val id: Int,
    val iid: Int,
    val project_id: Int,
    val title: String,
    val description: String?,
    val state: String,
    val source_branch: String,
    val target_branch: String,
    val created_at: String,
    val updated_at: String,
    val author: GitLabUser
)

data class GitLabUser(
    val id: Int,
    val username: String,
    val name: String,
    val avatar_url: String?
)

// Request models
data class CreateIssueRequest(
    val title: String,
    val description: String?
)

data class CreateMergeRequestRequest(
    val source_branch: String,
    val target_branch: String,
    val title: String,
    val description: String?
)
```

#### 4.2 Platform Abstraction

**Criar**: `app/src/main/kotlin/com/rafgittools/domain/platform/`

```kotlin
// Platform.kt
sealed class Platform {
    object GitHub : Platform()
    object GitLab : Platform()
    object Bitbucket : Platform()
    object Custom : Platform()
}

// PlatformRepository.kt
interface PlatformRepository {
    suspend fun getRepositories(): Result<List<Repository>>
    suspend fun getIssues(repoId: String): Result<List<Issue>>
    suspend fun getPullRequests(repoId: String): Result<List<PullRequest>>
    // ... unified interface
}

// GitHubPlatformRepository.kt
class GitHubPlatformRepository(
    private val githubApi: GithubApiService
) : PlatformRepository {
    override suspend fun getRepositories(): Result<List<Repository>> {
        // Map GitHub models to unified Repository model
    }
    // ...
}

// GitLabPlatformRepository.kt
class GitLabPlatformRepository(
    private val gitlabApi: GitLabApiService
) : PlatformRepository {
    override suspend fun getRepositories(): Result<List<Repository>> {
        // Map GitLab models to unified Repository model
    }
    // ...
}
```

#### 4.3 Platform Selector UI

**Atualizar**: Auth screen para permitir seleção de platform

---

## 📊 DELIVERABLES & ACCEPTANCE CRITERIA

### Milestone 1: Testes (Semana 4)
- [ ] 100+ unit tests implementados
- [ ] Coverage mínimo 40% (ideal 80%)
- [ ] Todos os services core testados
- [ ] CI/CD rodando testes automaticamente
- [ ] `TESTING_GUIDE.md` completo

### Milestone 2: Docs (Semana 3)
- [ ] 10 guias técnicos criados
- [ ] Exemplos práticos em cada doc
- [ ] Links entre docs funcionando
- [ ] Screenshots/diagramas inclusos
- [ ] Docs validados por 2+ devs

### Milestone 3: Terminal MVP (Semana 8)
- [ ] Terminal básico funcional
- [ ] Input/output working
- [ ] ANSI colors suportado
- [ ] Git commands executam no terminal
- [ ] 10+ tests para terminal
- [ ] Documentado em guia específico

### Milestone 4: GitLab Basic (Semana 12)
- [ ] GitLab API service implementado
- [ ] 5+ endpoints essenciais
- [ ] Platform abstraction layer
- [ ] UI permite escolher platform
- [ ] Tests para GitLab API
- [ ] Documentado

---

## 🎓 TECHNICAL STANDARDS

### Must Follow
- **IEEE 829** - Test documentation
- **ISO 25010** - Software quality model
- **Git Protocol v2** - Git operations
- **ECMA-48** - ANSI terminal
- **OpenAPI 3.0** - API design
- **Material Design 3** - UI/UX

### Code Style
- **Kotlin official conventions**
- **4 spaces indentation**
- **120 chars line length**
- **KDoc for public APIs**
- **Meaningful variable names**

### Git Commit Messages
```
feat: add terminal emulator core
test: add JGitService unit tests
docs: create testing guide
fix: resolve SSH auth issue
refactor: improve ViewModel architecture
```

---

## 🔧 TOOLING

### Required Tools
- Android Studio Hedgehog+
- JDK 17+
- Git 2.40+
- Gradle 8.2+

### Recommended Plugins
- Kotlin
- JUnit
- MockK
- Detekt (code analysis)
- SonarLint (quality)

---

## 📈 SUCCESS METRICS

### Quantitative
- Test coverage: **5.8% → 80%** (1280% improvement)
- Tests count: **7 → 100+** (1329% improvement)
- Docs count: **31 → 41** (32% improvement)
- Features complete: **115 → 150** (30% improvement)

### Qualitative
- All critical services have 80%+ coverage
- Every public API is documented
- Terminal emulator works end-to-end
- GitLab integration functional
- New contributors can onboard in <1 day

---

## 🚨 CRITICAL SUCCESS FACTORS

1. **Test First**: Escrever testes ANTES de features novas
2. **Documentation Parallel**: Doc junto com código
3. **Incremental**: Commits pequenos, reviews rápidos
4. **Standards Compliance**: Sempre seguir padrões
5. **Quality Gates**: CI/CD bloqueia se coverage cair

---

## 💬 COMMUNICATION

### Daily Updates
Commit messages descritivos com:
- Qual milestone está sendo trabalhado
- Progresso atual (X/Y tests, etc)
- Blockers se houver

### Weekly Summary
Todo domingo:
- Resumo do que foi feito
- Coverage atual
- Próximos passos
- Issues encontrados

### Questions
Para dúvidas, criar issue com label `question` incluindo:
- Contexto claro
- O que já tentou
- Comportamento esperado vs atual

---

## 🎯 FINAL GOAL

**Transform RafGitTools from "partially tested prototype" to "production-ready application" with:**
- ✅ 80%+ test coverage
- ✅ Comprehensive documentation
- ✅ Terminal emulation working
- ✅ Multi-platform support (GitHub + GitLab)
- ✅ Release-ready quality

**Timeline**: 12 weeks  
**Budget**: 300-400 hours  
**Team**: 1-2 developers  

---

**Good luck and happy coding! 🚀**

*Remember: Quality over speed. Tests and docs are not optional.*

---

**Standards Applied**:
- IEEE 829 (Testing)
- ISO 25010 (Quality)
- NIST SP 800-53 (Security)
- Git Protocol v2 (Git ops)
- ECMA-48 (Terminal)
- Material Design 3 (UI)

**Last Updated**: 2026-02-13  
**Version**: 1.0  
**Author**: RAFAELIA System
