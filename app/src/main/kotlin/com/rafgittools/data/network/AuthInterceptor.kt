package com.rafgittools.data.network

// FIX D2: This class is DEAD CODE — the DI graph uses data.auth.AuthInterceptor (injected via Hilt).
// This class uses CredentialManager directly and is never instantiated.
// Kept for reference only. Safe to delete in cleanup pass.
// 
// If you need a fallback interceptor, use data.auth.AuthInterceptor which:
//   - Is @Singleton @Inject annotated (Hilt)
//   - Uses AuthTokenCache (in-memory, thread-safe)
//   - Adds correct GitHub API headers (Accept, X-GitHub-Api-Version)
@Deprecated("Dead code — Hilt provides data.auth.AuthInterceptor. Delete this file.")
class AuthInterceptor
