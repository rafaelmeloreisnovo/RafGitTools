package com.rafgittools.workspace

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ManifoldRouteBindingTest {
    @Test
    fun binding_exposes_canonical_v1_route_annotations() {
        val binding = ManifoldRouteBinding(
            triggerCode = 4,
            routeCode = 4,
            routeTagHex = "A1B2C3D4",
            authorityRef = "Mapa:data/manifold/routes_omega_v1.jsonl"
        )

        assertThat(binding.routeId).isEqualTo("R0004")
        assertThat(binding.toAnnotations()["manifold_route_tag"]).isEqualTo("a1b2c3d4")
        assertThat(binding.toAnnotations()["manifold_route_state"]).isEqualTo("ROUTE_RESOLVED")
    }

    @Test
    fun binding_rejects_v1_mapping_drift() {
        assertThrows(IllegalArgumentException::class.java) {
            ManifoldRouteBinding(
                triggerCode = 4,
                routeCode = 5,
                routeTagHex = "12345678",
                authorityRef = "Mapa:data/manifold/routes_omega_v1.jsonl"
            )
        }
    }

    @Test
    fun context_bundle_preserves_route_binding_without_promoting_claim() {
        val broker = ContextBroker()
        val resource = ResourceRef(
            provider = "LOCAL_GIT",
            repositoryOrCorpus = "/repo",
            refOrGeneration = "main",
            pathOrLocator = "src/a.kt",
            objectId = "blob-1",
            visibility = ResourceVisibility.LOCAL_ONLY
        )
        broker.addText(resource, "bounded")

        val bundle = broker.buildBundle(
            bundleId = "b-route",
            objective = "Inspect code runtime route",
            createdAt = "2026-09-23T05:20:00Z",
            routeBinding = ManifoldRouteBinding(
                triggerCode = 4,
                routeCode = 4,
                routeTagHex = "deadbeef",
                authorityRef = "Mapa:data/manifold/routes_omega_v1.jsonl"
            )
        )

        assertThat(bundle.annotations["manifold_route_id"]).isEqualTo("R0004")
        assertThat(bundle.annotations["manifold_route_resolver"]).isEqualTo("rafcode_route_v1")
        assertThat(bundle.annotations["claim_allowed"]).isEqualTo(false)
    }

    @Test
    fun user_annotations_cannot_spoof_reserved_route_fields() {
        val broker = ContextBroker()
        val binding = ManifoldRouteBinding(
            triggerCode = 7,
            routeCode = 7,
            routeTagHex = "01020304",
            authorityRef = "Mapa:data/manifold/routes_omega_v1.jsonl"
        )

        assertThrows(IllegalArgumentException::class.java) {
            broker.buildBundle(
                bundleId = "b",
                objective = "ATLAS route",
                createdAt = "2026-09-23T05:20:00Z",
                annotations = mapOf(
                    "claim_allowed" to false,
                    "manifold_route_id" to "R9999"
                ),
                routeBinding = binding
            )
        }
    }
}
