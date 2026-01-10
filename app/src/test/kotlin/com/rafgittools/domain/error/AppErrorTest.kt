package com.rafgittools.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for AppError domain error types
 */
class AppErrorTest {
    
    @Test
    fun `NetworkError NoConnection has correct message`() {
        val error = AppError.NetworkError.NoConnection()
        assertEquals("No internet connection", error.message)
    }
    
    @Test
    fun `NetworkError Timeout has correct message`() {
        val error = AppError.NetworkError.Timeout()
        assertEquals("Request timed out", error.message)
    }
    
    @Test
    fun `NetworkError ServerError has code and message`() {
        val error = AppError.NetworkError.ServerError(500, "Internal Server Error")
        assertEquals(500, error.code)
        assertEquals("Internal Server Error", error.message)
    }
    
    @Test
    fun `AuthError Unauthorized has correct message`() {
        val error = AppError.AuthError.Unauthorized()
        assertEquals("Invalid credentials", error.message)
    }
    
    @Test
    fun `AuthError TokenExpired has correct message`() {
        val error = AppError.AuthError.TokenExpired()
        assertEquals("Authentication token has expired", error.message)
    }
    
    @Test
    fun `GitError RepositoryNotFound has correct message`() {
        val error = AppError.GitError.RepositoryNotFound()
        assertEquals("Repository not found", error.message)
    }
    
    @Test
    fun `GitError BranchNotFound includes branch name`() {
        val error = AppError.GitError.BranchNotFound("feature/test")
        assertEquals("Branch not found: feature/test", error.message)
        assertEquals("feature/test", error.branch)
    }
    
    @Test
    fun `GitError MergeConflict lists conflicts`() {
        val conflicts = listOf("file1.txt", "file2.txt")
        val error = AppError.GitError.MergeConflict(conflicts)
        assertEquals("Merge conflict in: file1.txt, file2.txt", error.message)
        assertEquals(conflicts, error.conflicts)
    }
    
    @Test
    fun `StorageError FileNotFound includes path`() {
        val error = AppError.StorageError.FileNotFound("/path/to/file.txt")
        assertEquals("File not found: /path/to/file.txt", error.message)
        assertEquals("/path/to/file.txt", error.path)
    }
    
    @Test
    fun `ValidationError EmptyField includes field name`() {
        val error = AppError.ValidationError.EmptyField("username")
        assertEquals("username cannot be empty", error.message)
        assertEquals("username", error.fieldName)
    }
    
    @Test
    fun `ValidationError InvalidFormat includes reason`() {
        val error = AppError.ValidationError.InvalidFormat("email", "must contain @")
        assertEquals("Invalid email: must contain @", error.message)
    }
    
    @Test
    fun `toAppError converts UnknownHostException to NoConnection`() {
        val exception = UnknownHostException("host not found")
        val appError = exception.toAppError()
        assertTrue(appError is AppError.NetworkError.NoConnection)
    }
    
    @Test
    fun `toAppError converts SocketTimeoutException to Timeout`() {
        val exception = SocketTimeoutException("timed out")
        val appError = exception.toAppError()
        assertTrue(appError is AppError.NetworkError.Timeout)
    }
    
    @Test
    fun `toAppError converts FileNotFoundException to FileNotFound`() {
        val exception = FileNotFoundException("/missing/file.txt")
        val appError = exception.toAppError()
        assertTrue(appError is AppError.StorageError.FileNotFound)
    }
    
    @Test
    fun `toAppError converts SecurityException to PermissionDenied`() {
        val exception = SecurityException("access denied")
        val appError = exception.toAppError()
        assertTrue(appError is AppError.StorageError.PermissionDenied)
    }
    
    @Test
    fun `toAppError returns AppError unchanged`() {
        val originalError = AppError.GitError.RepositoryNotFound()
        val result = originalError.toAppError()
        assertEquals(originalError, result)
    }
    
    @Test
    fun `toAppError converts unknown exception to GeneralError Unknown`() {
        val exception = RuntimeException("unexpected error")
        val appError = exception.toAppError()
        assertTrue(appError is AppError.GeneralError.Unknown)
        assertEquals("unexpected error", appError.message)
    }
}
