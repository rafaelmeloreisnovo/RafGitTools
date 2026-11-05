package com.rafgittools.diagnostics

import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * A user-triggered, PRIVATE-BY-DEFAULT observation of the running Android UI.
 *
 * The receipt deliberately contains no login, token, device ID, local path,
 * repository names or private source identifiers. SHA-256 protects its exact
 * payload bytes, NOT its authenticity. This is SELF_REPORT, NOT attestation.
 *
 * Contract: rafaelia.public-proof-envelope/v1 (conhecimentos).
 */
object DeviceProofV1 {
    private const val ENVELOPE_SCHEMA = "rafaelia.public-proof-envelope/v1"
    private const val PAYLOAD_SCHEMA = "rafaelia.rafgittools.device-evidence/v1"

    fun create(
        appVersion: String,
        androidSdk: Int,
        credentialPresent: Boolean,
        authFlag: Boolean,
        profileVisible: Boolean,
        repositoryCount: Int,
        repositoryListingComplete: Boolean,
        liveProbe: String
    ): String {
        require(liveProbe in setOf("PASS_SCOPED", "FAILED", "NOT_RUN"))
        require(androidSdk in 16..99)
        require(repositoryCount in 0..100000)
        require(appVersion.matches(Regex("[a-zA-Z0-9_.+-]{1,64}")))
        require(authFlag || (!profileVisible && repositoryCount == 0 && !repositoryListingComplete))

        val timestampUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        val payload = JSONObject().apply {
            put("schema", PAYLOAD_SCHEMA)
            put("event_id", UUID.randomUUID().toString())
            put("captured_at_utc", timestampUtc)
            put("app_version", appVersion)
            put("android_sdk", androidSdk)
            put("credential_present", credentialPresent)
            put("auth_flag", authFlag)
            put("profile_visible", profileVisible)
            put("repository_count", repositoryCount)
            put("repository_listing_complete", repositoryListingComplete)
            put("live_probe", liveProbe)
            put("capture_scope", "ANDROID_APP_SELF_REPORT")
            put("device_attestation", "TOKEN_VAZIO")
            put("source_commit", "TOKEN_VAZIO")
            put("claim_allowed", false)
        }.toString()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }

        return JSONObject().apply {
            put("schema", ENVELOPE_SCHEMA)
            put("payload_json", payload)
            put("payload_sha256", digest)
        }.toString(2)
    }
}
