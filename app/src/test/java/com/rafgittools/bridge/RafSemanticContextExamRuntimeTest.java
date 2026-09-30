package com.rafgittools.bridge;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.junit.Test;

public final class RafSemanticContextExamRuntimeTest {
    @Test
    public void safe_manifest_executes_and_blocks_expected_operations() throws Exception {
        RafSemanticContextExamRuntime.Result result =
                RafSemanticContextExamRuntime.evaluate(validManifest().toString());

        assertThat(result.state).isEqualTo(RafSemanticContextExamRuntime.PASS_STATE);
        assertThat(result.executableOperations).isEqualTo(1);
        assertThat(result.blockedOperations).isEqualTo(1);
        assertThat(result.claimAllowed).isFalse();
        assertThat(result.executionState).isEqualTo("TESTED_NOT_PHYSICALLY_PROVEN");
        RafSemanticContextExamRuntime.requireReadOnlySafe(result);
    }

    @Test
    public void dimension_mismatch_cannot_be_declared_executable() {
        JsonObject manifest = validManifest();
        manifest.getAsJsonArray("operations")
                .get(1).getAsJsonObject()
                .addProperty("expected_state", "EXECUTABLE");

        RafSemanticContextExamRuntime.ExamException error = assertThrows(
                RafSemanticContextExamRuntime.ExamException.class,
                () -> RafSemanticContextExamRuntime.evaluate(manifest.toString())
        );
        assertThat(error).hasMessageThat().contains("expected_state=EXECUTABLE");
    }

    @Test
    public void transform_requires_non_vazio_invariant() {
        JsonObject manifest = validManifest();
        manifest.getAsJsonArray("transforms")
                .get(0).getAsJsonObject()
                .addProperty("invariant", "TOKEN_VAZIO");

        RafSemanticContextExamRuntime.ExamException error = assertThrows(
                RafSemanticContextExamRuntime.ExamException.class,
                () -> RafSemanticContextExamRuntime.evaluate(manifest.toString())
        );
        assertThat(error).hasMessageThat().contains("invariant is TOKEN_VAZIO");
    }

    @Test
    public void read_only_route_refuses_even_a_fully_proven_claim() throws Exception {
        JsonObject manifest = validManifest();
        JsonObject physicalEvidence = new JsonObject();
        physicalEvidence.addProperty("id", "E_PHYSICAL");
        physicalEvidence.addProperty("source_ref", "SRC");
        physicalEvidence.addProperty("locator", "physical receipt");
        manifest.getAsJsonArray("evidence").add(physicalEvidence);

        JsonObject execution = manifest.getAsJsonObject("execution_state");
        execution.addProperty("physically_proven", true);
        execution.getAsJsonArray("physical_evidence_refs").add("E_PHYSICAL");

        JsonObject claim = manifest.getAsJsonObject("claim_gate");
        claim.addProperty("test_scope_matches_claim", true);
        claim.addProperty("physical_evidence", "E_PHYSICAL");
        claim.addProperty("claim_authority", "PHYSICAL_AUTHORITY");
        manifest.addProperty("claim_allowed", true);

        RafSemanticContextExamRuntime.Result result =
                RafSemanticContextExamRuntime.evaluate(manifest.toString());
        assertThat(result.claimAllowed).isTrue();

        RafSemanticContextExamRuntime.ExamException error = assertThrows(
                RafSemanticContextExamRuntime.ExamException.class,
                () -> RafSemanticContextExamRuntime.requireReadOnlySafe(result)
        );
        assertThat(error).hasMessageThat().contains("refuses claim_allowed=true");
    }

    @Test
    public void delivery_requires_next_executable_action() {
        JsonObject manifest = validManifest();
        manifest.getAsJsonObject("delivery").addProperty("next_executable_action", "");

        RafSemanticContextExamRuntime.ExamException error = assertThrows(
                RafSemanticContextExamRuntime.ExamException.class,
                () -> RafSemanticContextExamRuntime.evaluate(manifest.toString())
        );
        assertThat(error).hasMessageThat().contains("next_executable_action");
    }

    private static JsonObject validManifest() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", "rafaelia.semantic-context-exam.v1");
        root.addProperty("exam_id", "RUNTIME-EXAM-001");
        root.addProperty("observed_at", "2026-09-29T14:08:00-03:00");
        root.addProperty("declared_task", "Read selected context safely.");

        JsonArray sources = new JsonArray();
        JsonObject source = new JsonObject();
        source.addProperty("id", "SRC");
        source.addProperty("locator", "explicit-user-selected-context");
        source.addProperty("state", "SOURCE_OBSERVED");
        sources.add(source);
        root.add("sources", sources);

        JsonArray evidence = new JsonArray();
        evidence.add(evidence("E_SEM", "semantic declaration"));
        evidence.add(evidence("E_TEST", "runtime unit test"));
        root.add("evidence", evidence);

        JsonArray objects = new JsonArray();
        objects.add(object("P", "VALUE", "pressure", "Pa", "ENERGY_DENSITY"));
        objects.add(object("RHO", "VALUE", "energy_density", "J/m^3", "ENERGY_DENSITY"));
        objects.add(object("MASS", "VALUE", "mass_density", "kg/m^3", "MASS_DENSITY"));
        root.add("objects", objects);

        JsonArray transforms = new JsonArray();
        JsonObject transform = new JsonObject();
        transform.addProperty("id", "T_P");
        transform.addProperty("from_unit", "Pa");
        transform.addProperty("to_unit", "J/m^3");
        transform.addProperty("from_dimension", "ENERGY_DENSITY");
        transform.addProperty("to_dimension", "ENERGY_DENSITY");
        transform.addProperty("invariant", "1 Pa = 1 J/m^3");
        transform.addProperty("evidence_ref", "E_SEM");
        transforms.add(transform);
        root.add("transforms", transforms);

        JsonArray operations = new JsonArray();
        JsonObject executable = new JsonObject();
        executable.addProperty("id", "OP_EXEC");
        executable.addProperty("kind", "ADD");
        JsonArray executableBindings = new JsonArray();
        executableBindings.add(binding("P", "T_P"));
        executableBindings.add(binding("RHO", null));
        executable.add("input_bindings", executableBindings);
        executable.addProperty("expected_state", "EXECUTABLE");
        operations.add(executable);

        JsonObject blocked = new JsonObject();
        blocked.addProperty("id", "OP_BLOCK");
        blocked.addProperty("kind", "ADD");
        JsonArray blockedBindings = new JsonArray();
        blockedBindings.add(binding("MASS", null));
        blockedBindings.add(binding("RHO", null));
        blocked.add("input_bindings", blockedBindings);
        blocked.addProperty("expected_state", "BLOCKED");
        operations.add(blocked);
        root.add("operations", operations);

        JsonArray representations = new JsonArray();
        representations.add("ENERGY_DENSITY_CONVENTION");
        representations.add("MASS_DENSITY_CONVENTION");
        root.add("representation_options", representations);
        root.addProperty("physical_representation_selection", "TOKEN_VAZIO");

        JsonObject execution = new JsonObject();
        execution.addProperty("implemented", true);
        execution.addProperty("tested", true);
        execution.addProperty("physically_proven", false);
        JsonArray testEvidence = new JsonArray();
        testEvidence.add("E_TEST");
        execution.add("test_evidence_refs", testEvidence);
        execution.add("physical_evidence_refs", new JsonArray());
        root.add("execution_state", execution);

        JsonObject claim = new JsonObject();
        claim.addProperty("test_scope_matches_claim", false);
        claim.addProperty("physical_evidence", "TOKEN_VAZIO");
        claim.addProperty("claim_authority", "TOKEN_VAZIO");
        root.add("claim_gate", claim);

        JsonObject delivery = new JsonObject();
        delivery.addProperty("answer_state", "PARTIAL_SAFE");
        delivery.addProperty("what_was_done", "Context was examined.");
        JsonArray deliveryEvidence = new JsonArray();
        deliveryEvidence.add("E_SEM");
        deliveryEvidence.add("E_TEST");
        delivery.add("evidence_refs", deliveryEvidence);
        JsonArray limits = new JsonArray();
        limits.add("No physical claim.");
        delivery.add("limits", limits);
        JsonArray tokenVazio = new JsonArray();
        tokenVazio.add("claim_gate.physical_evidence");
        tokenVazio.add("claim_gate.claim_authority");
        delivery.add("token_vazio", tokenVazio);
        delivery.addProperty(
                "next_executable_action",
                "Use only explicitly selected ContextBroker sources in read-only mode."
        );
        root.add("delivery", delivery);
        root.addProperty("claim_allowed", false);
        return root;
    }

    private static JsonObject evidence(String id, String locator) {
        JsonObject value = new JsonObject();
        value.addProperty("id", id);
        value.addProperty("source_ref", "SRC");
        value.addProperty("locator", locator);
        return value;
    }

    private static JsonObject object(
            String id,
            String kind,
            String semanticType,
            String unit,
            String dimension
    ) {
        JsonObject value = new JsonObject();
        value.addProperty("id", id);
        value.addProperty("kind", kind);
        value.addProperty("semantic_type", semanticType);
        value.addProperty("unit", unit);
        value.addProperty("dimension", dimension);
        value.addProperty("source_ref", "SRC");
        value.addProperty("state", "DECLARED");
        return value;
    }

    private static JsonObject binding(String objectRef, String transformRef) {
        JsonObject value = new JsonObject();
        value.addProperty("object_ref", objectRef);
        if (transformRef == null) {
            value.add("transform_ref", JsonNull.INSTANCE);
        } else {
            value.addProperty("transform_ref", transformRef);
        }
        return value;
    }
}
