package com.rafgittools.workspace

/**
 * Typed bridge from the freestanding rafcode_route_v1 receipt into ContextBundle V2.
 *
 * This class does not execute the native resolver. It only carries an already
 * resolved, evidence-bound route result across the Android/workbench boundary.
 */
data class ManifoldRouteBinding(
    val triggerCode: Int,
    val routeCode: Int,
    val routeTagHex: String,
    val authorityRef: String,
    val resolver: String = "rafcode_route_v1",
    val state: String = "ROUTE_RESOLVED"
) {
    init {
        require(triggerCode in 1..10) { "triggerCode must be in 1..10" }
        require(routeCode in 1..10) { "routeCode must be in 1..10" }
        require(routeCode == triggerCode) {
            "V1 routeCode must equal triggerCode; changed mapping requires a new contract version"
        }
        require(routeTagHex.length == 8 && routeTagHex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "routeTagHex must be exactly 8 hexadecimal characters"
        }
        require(authorityRef.isNotBlank()) { "authorityRef must not be blank" }
        require(resolver == "rafcode_route_v1") { "unsupported resolver" }
        require(state == "ROUTE_RESOLVED") { "unsupported route state" }
    }

    val routeId: String
        get() = "R" + routeCode.toString().padStart(4, '0')

    fun toAnnotations(): Map<String, Any> = mapOf(
        "manifold_route_id" to routeId,
        "manifold_trigger_code" to triggerCode,
        "manifold_route_code" to routeCode,
        "manifold_route_tag" to routeTagHex.lowercase(),
        "manifold_route_authority" to authorityRef,
        "manifold_route_resolver" to resolver,
        "manifold_route_state" to state
    )
}
