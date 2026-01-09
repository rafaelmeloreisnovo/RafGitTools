# Contributing to RafGitTools

First off, thank you for considering contributing to RafGitTools! 🎉

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please be respectful and constructive in all interactions.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the behavior
- **Expected behavior**
- **Actual behavior**
- **Screenshots** (if applicable)
- **Device/Android version** information
- **App version** you're using

### Suggesting Features

Feature suggestions are welcome! Please provide:

- **Clear description** of the feature
- **Use case** - why is this feature needed?
- **Proposed implementation** (if you have ideas)
- **Alternative solutions** you've considered

### Pull Requests

1. **Fork the repo** and create your branch from `main`
2. **Follow the coding style** used in the project
3. **Add tests** if applicable
4. **Update documentation** as needed
5. **Ensure tests pass**
6. **Create a pull request**

## Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or newer
- Android SDK 24+
- Git

### Setting Up Development Environment

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/RafGitTools.git
cd RafGitTools

# Add upstream remote
git remote add upstream https://github.com/rafaelmeloreisnovo/RafGitTools.git

# Create a feature branch
git checkout -b feature/your-feature-name
```

### Building and Running

1. Open project in Android Studio
2. Sync Gradle files
3. Select `devDebug` build variant
4. Run on emulator or device

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# All tests
./gradlew test connectedAndroidTest
```

## Coding Standards

### Kotlin Style Guide

We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use descriptive variable names
- Avoid unnecessary comments (code should be self-documenting)

### Architecture Patterns

- Follow Clean Architecture principles
- Use MVVM for presentation layer
- Keep business logic in Use Cases
- Use Repository pattern for data access

### Code Examples

```kotlin
// Good: Clear, descriptive function name
fun cloneRepository(url: String, path: String): Result<Repository> {
    // Implementation
}

// Good: Proper error handling
suspend fun fetchRepositories(): Result<List<Repository>> = 
    withContext(Dispatchers.IO) {
        try {
            val repos = api.getRepositories()
            Result.Success(repos)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError(e.message))
        }
    }

// Good: Use sealed classes for state
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: Any) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
feat: add SSH key management
fix: resolve crash on repository clone
docs: update architecture documentation
style: format code according to style guide
refactor: simplify authentication flow
test: add unit tests for git operations
chore: update dependencies
```

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation
- `refactor/description` - Code refactoring
- `test/description` - Test additions/changes

## Testing Guidelines

### Unit Tests

- Test business logic in Use Cases
- Test state management in ViewModels
- Use MockK for mocking
- Aim for >80% code coverage

```kotlin
@Test
fun `when clone repository succeeds, state is updated`() = runTest {
    // Given
    val repo = TestData.repository
    coEvery { cloneUseCase(any(), any()) } returns flowOf(Result.Success(repo))
    
    // When
    viewModel.cloneRepository("url", "path")
    
    // Then
    assertEquals(Success(repo), viewModel.uiState.value)
}
```

### UI Tests

- Use Compose Testing for UI tests
- Test user interactions
- Test navigation flows

```kotlin
@Test
fun clickCloneButton_opensCloneScreen() {
    composeTestRule.setContent {
        RepositoryListScreen()
    }
    
    composeTestRule.onNodeWithText("Clone").performClick()
    composeTestRule.onNodeWithText("Clone Repository").assertIsDisplayed()
}
```

## Documentation

- Update README.md if adding major features
- Add KDoc comments for public APIs
- Update relevant documentation in `docs/`
- Include examples where helpful

```kotlin
/**
 * Clones a Git repository from the specified URL.
 *
 * @param url The Git repository URL (HTTP/HTTPS/SSH)
 * @param path Local path where the repository should be cloned
 * @return Result containing the cloned Repository or an error
 *
 * @throws GitException if the repository cannot be cloned
 * @throws NetworkException if there's a network error
 *
 * Example:
 * ```
 * val result = cloneRepository(
 *     url = "https://github.com/user/repo.git",
 *     path = "/storage/repos/repo"
 * )
 * ```
 */
suspend fun cloneRepository(url: String, path: String): Result<Repository>
```

## License

By contributing to RafGitTools, you agree that your contributions will be licensed under the GPL-3.0 License.

All contributions must:
- Be compatible with GPL-3.0
- Include proper attribution
- Not include proprietary code

## Questions?

Feel free to:
- Open an issue for questions
- Start a discussion on GitHub Discussions
- Reach out to maintainers

## Recognition

Contributors will be:
- Listed in the project's contributors page
- Mentioned in release notes (for significant contributions)
- Given credit in documentation

Thank you for contributing to RafGitTools! 🚀
