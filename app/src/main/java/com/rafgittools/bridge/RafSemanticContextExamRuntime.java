package com.rafgittools.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime mirror of the RAFAELIA Semantic Context Exam V1 for Android/JVM.
 *
 * This class validates the same operational boundary used by the canonical
 * Python examiner. It does not infer semantic types, units, dimensions,
 * transforms, evidence, or authority. Missing declarations fail closed.
 */
public final class RafSemanticContextExamRuntime {
    public static final String MANIFEST_SCHEMA = "rafaelia.semantic-context-exam.v1";
    public static final String PASS_STATE = "PASS_SAFE_CONTEXT_EXAM";
    public static final String TOKEN_VAZIO = "TOKEN_VAZIO";

    private RafSemanticContextExamRuntime() {
    }

    public static Result evaluate(String json) throws ExamException {
        final JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                throw new ExamException("semantic exam root must be an object");
            }
            root = parsed.getAsJsonObject();
        } catch (ExamException error) {
            throw error;
        } catch (Exception error) {
            throw new ExamException("semantic exam JSON is invalid");
        }

        requireEquals(string(root, "schema"), MANIFEST_SCHEMA, "schema");
        nonEmpty(string(root, "exam_id"), "exam_id");
        nonEmpty(string(root, "declared_task"), "declared_task");

        Map<String, JsonObject> sources = indexObjects(array(root, "sources"), "sources");
        Map<String, JsonObject> evidence = indexObjects(array(root, "evidence"), "evidence");
        Map<String, JsonObject> objects = indexObjects(array(root, "objects"), "objects");
        Map<String, JsonObject> transforms = indexObjects(array(root, "transforms"), "transforms");
        Map<String, JsonObject> operations = indexObjects(array(root, "operations"), "operations");

        for (Map.Entry<String, JsonObject> entry : sources.entrySet()) {
            nonEmpty(string(entry.getValue(), "locator"), "sources." + entry.getKey() + ".locator");
            nonEmpty(string(entry.getValue(), "state"), "sources." + entry.getKey() + ".state");
        }

        for (Map.Entry<String, JsonObject> entry : evidence.entrySet()) {
            String sourceRef = string(entry.getValue(), "source_ref");
            if (!sources.containsKey(sourceRef)) {
                throw new ExamException("evidence." + entry.getKey() + ".source_ref is unknown");
            }
            nonEmpty(string(entry.getValue(), "locator"), "evidence." + entry.getKey() + ".locator");
        }

        for (Map.Entry<String, JsonObject> entry : objects.entrySet()) {
            JsonObject value = entry.getValue();
            nonEmpty(string(value, "kind"), "objects." + entry.getKey() + ".kind");
            nonEmpty(string(value, "semantic_type"), "objects." + entry.getKey() + ".semantic_type");
            nonEmpty(string(value, "unit"), "objects." + entry.getKey() + ".unit");
            nonEmpty(string(value, "dimension"), "objects." + entry.getKey() + ".dimension");
            String sourceRef = string(value, "source_ref");
            if (!sources.containsKey(sourceRef)) {
                throw new ExamException("objects." + entry.getKey() + ".source_ref is unknown");
            }
            String state = string(value, "state");
            nonEmpty(state, "objects." + entry.getKey() + ".state");
            if ((TOKEN_VAZIO.equals(string(value, "unit"))
                    || TOKEN_VAZIO.equals(string(value, "dimension")))
                    && !TOKEN_VAZIO.equals(state)) {
                throw new ExamException(
                        "objects." + entry.getKey() + ": missing representation requires TOKEN_VAZIO state"
                );
            }
        }

        for (Map.Entry<String, JsonObject> entry : transforms.entrySet()) {
            JsonObject value = entry.getValue();
            for (String field : new String[]{
                    "from_unit", "to_unit", "from_dimension", "to_dimension", "invariant"
            }) {
                nonEmpty(string(value, field), "transforms." + entry.getKey() + "." + field);
            }
            if (TOKEN_VAZIO.equals(string(value, "invariant"))) {
                throw new ExamException("transforms." + entry.getKey() + ": invariant is TOKEN_VAZIO");
            }
            String evidenceRef = string(value, "evidence_ref");
            if (!evidence.containsKey(evidenceRef)) {
                throw new ExamException("transforms." + entry.getKey() + ".evidence_ref is unknown");
            }
        }

        int executable = 0;
        int blocked = 0;
        for (Map.Entry<String, JsonObject> entry : operations.entrySet()) {
            OperationResult result = evaluateOperation(
                    entry.getKey(), entry.getValue(), objects, transforms, evidence.keySet()
            );
            if ("EXECUTABLE".equals(result.state)) {
                executable++;
            } else {
                blocked++;
            }
        }

        JsonArray options = array(root, "representation_options");
        if (options.size() == 0) {
            throw new ExamException("representation_options must be non-empty");
        }
        String selection = string(root, "physical_representation_selection");
        if (options.size() > 1 && !TOKEN_VAZIO.equals(selection)) {
            boolean found = false;
            for (JsonElement option : options) {
                if (option.isJsonPrimitive() && selection.equals(option.getAsString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ExamException(
                        "physical_representation_selection must be an option or TOKEN_VAZIO"
                );
            }
        }

        JsonObject execution = object(root, "execution_state");
        boolean implemented = bool(execution, "implemented");
        boolean tested = bool(execution, "tested");
        boolean physicallyProven = bool(execution, "physically_proven");
        List<String> testRefs = strings(array(execution, "test_evidence_refs"));
        List<String> physicalRefs = strings(array(execution, "physical_evidence_refs"));
        validateRefs(testRefs, evidence.keySet(), "execution_state.test_evidence_refs");
        validateRefs(physicalRefs, evidence.keySet(), "execution_state.physical_evidence_refs");
        if (tested && !implemented) {
            throw new ExamException("tested=true requires implemented=true");
        }
        if (tested && testRefs.isEmpty()) {
            throw new ExamException("tested=true requires test evidence");
        }
        if (physicallyProven && (!tested || physicalRefs.isEmpty())) {
            throw new ExamException(
                    "physically_proven=true requires tested=true and physical evidence"
            );
        }

        JsonObject claim = object(root, "claim_gate");
        boolean scopeMatch = bool(claim, "test_scope_matches_claim");
        String physicalEvidence = string(claim, "physical_evidence");
        String authority = string(claim, "claim_authority");
        boolean evaluatedClaimAllowed = scopeMatch
                && evidence.containsKey(physicalEvidence)
                && !TOKEN_VAZIO.equals(physicalEvidence)
                && physicallyProven
                && authority != null
                && !authority.trim().isEmpty()
                && !TOKEN_VAZIO.equals(authority);

        boolean declaredClaimAllowed = bool(root, "claim_allowed");
        if (declaredClaimAllowed != evaluatedClaimAllowed) {
            throw new ExamException(
                    "claim_allowed does not match evaluated claim gate: " + evaluatedClaimAllowed
            );
        }

        JsonObject delivery = object(root, "delivery");
        for (String field : new String[]{
                "answer_state",
                "what_was_done",
                "evidence_refs",
                "limits",
                "token_vazio",
                "next_executable_action"
        }) {
            if (!delivery.has(field)) {
                throw new ExamException("delivery missing required field: " + field);
            }
        }
        nonEmpty(string(delivery, "answer_state"), "delivery.answer_state");
        nonEmpty(string(delivery, "what_was_done"), "delivery.what_was_done");
        nonEmpty(string(delivery, "next_executable_action"), "delivery.next_executable_action");

        List<String> deliveryEvidence = strings(array(delivery, "evidence_refs"));
        validateRefs(deliveryEvidence, evidence.keySet(), "delivery.evidence_refs");
        List<String> limits = strings(array(delivery, "limits"));
        List<String> tokenVazio = strings(array(delivery, "token_vazio"));

        String executionLabel = physicallyProven
                ? "PHYSICALLY_PROVEN"
                : tested
                ? "TESTED_NOT_PHYSICALLY_PROVEN"
                : implemented
                ? "IMPLEMENTED_UNTESTED"
                : "NOT_IMPLEMENTED";

        return new Result(
                string(root, "exam_id"),
                PASS_STATE,
                executable,
                blocked,
                declaredClaimAllowed,
                executionLabel,
                selection,
                deliveryEvidence,
                limits,
                tokenVazio,
                string(delivery, "next_executable_action")
        );
    }

    public static void requireReadOnlySafe(Result result) throws ExamException {
        if (result == null || !PASS_STATE.equals(result.state)) {
            throw new ExamException("semantic exam did not reach PASS_SAFE_CONTEXT_EXAM");
        }
        if (result.claimAllowed) {
            throw new ExamException("read-only local-model route refuses claim_allowed=true");
        }
    }

    private static OperationResult evaluateOperation(
            String id,
            JsonObject operation,
            Map<String, JsonObject> objects,
            Map<String, JsonObject> transforms,
            Set<String> evidenceIds
    ) throws ExamException {
        String kind = string(operation, "kind");
        nonEmpty(kind, "operations." + id + ".kind");

        JsonArray bindings = array(operation, "input_bindings");
        if (bindings.size() == 0) {
            throw new ExamException("operations." + id + ".input_bindings must be non-empty");
        }

        List<String> units = new ArrayList<>();
        List<String> dimensions = new ArrayList<>();
        boolean transformGate = true;
        boolean tokenVazio = false;

        for (int index = 0; index < bindings.size(); index++) {
            JsonElement element = bindings.get(index);
            if (!element.isJsonObject()) {
                throw new ExamException(
                        "operations." + id + ".input_bindings[" + index + "] must be object"
                );
            }
            JsonObject binding = element.getAsJsonObject();
            String objectRef = string(binding, "object_ref");
            JsonObject source = objects.get(objectRef);
            if (source == null) {
                throw new ExamException(
                        "operations." + id + ".input_bindings[" + index + "].object_ref is unknown"
                );
            }

            String unit = string(source, "unit");
            String dimension = string(source, "dimension");
            String transformRef = nullableString(binding, "transform_ref");

            if (TOKEN_VAZIO.equals(unit) || TOKEN_VAZIO.equals(dimension)) {
                tokenVazio = true;
            }

            if (transformRef != null) {
                if (TOKEN_VAZIO.equals(transformRef)) {
                    transformGate = false;
                    tokenVazio = true;
                } else {
                    JsonObject transform = transforms.get(transformRef);
                    if (transform == null) {
                        throw new ExamException(
                                "operations." + id + ".input_bindings[" + index
                                        + "].transform_ref is unknown"
                        );
                    }
                    if (!unit.equals(string(transform, "from_unit"))
                            || !dimension.equals(string(transform, "from_dimension"))) {
                        throw new ExamException(
                                "operations." + id + ": transform does not match source representation"
                        );
                    }
                    String invariant = string(transform, "invariant");
                    String evidenceRef = string(transform, "evidence_ref");
                    if (invariant == null || invariant.trim().isEmpty()
                            || TOKEN_VAZIO.equals(invariant)
                            || !evidenceIds.contains(evidenceRef)) {
                        transformGate = false;
                    }
                    unit = string(transform, "to_unit");
                    dimension = string(transform, "to_dimension");
                }
            }

            units.add(unit);
            dimensions.add(dimension);
        }

        String state = "EXECUTABLE";
        String reason = "TYPE_UNIT_TRANSFORM_INVARIANT_PASS";
        if (tokenVazio) {
            state = "BLOCKED";
            reason = "TYPE_OR_REPRESENTATION_TOKEN_VAZIO";
        } else if (!transformGate) {
            state = "BLOCKED";
            reason = "TRANSFORM_GATE_FAILED";
        } else if ("ADD".equals(kind)
                || "SUBTRACT".equals(kind)
                || "COMPARE".equals(kind)
                || "ASSERT_EQUIVALENCE".equals(kind)) {
            if (new HashSet<>(dimensions).size() != 1) {
                state = "BLOCKED";
                reason = "DIMENSION_MISMATCH";
            } else if (new HashSet<>(units).size() != 1) {
                state = "BLOCKED";
                reason = "UNIT_MISMATCH_NO_COMMON_REPRESENTATION";
            }
        }

        String expected = string(operation, "expected_state");
        if (!"EXECUTABLE".equals(expected) && !"BLOCKED".equals(expected)) {
            throw new ExamException(
                    "operations." + id + ".expected_state must be EXECUTABLE or BLOCKED"
            );
        }
        if (!expected.equals(state)) {
            throw new ExamException(
                    "operations." + id + ": expected_state=" + expected
                            + " but evaluated=" + state + " (" + reason + ")"
            );
        }
        return new OperationResult(state, reason);
    }

    private static Map<String, JsonObject> indexObjects(JsonArray values, String field)
            throws ExamException {
        Map<String, JsonObject> out = new HashMap<>();
        for (int index = 0; index < values.size(); index++) {
            JsonElement element = values.get(index);
            if (!element.isJsonObject()) {
                throw new ExamException(field + "[" + index + "] must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            String id = string(object, "id");
            nonEmpty(id, field + "[" + index + "].id");
            if (out.put(id, object) != null) {
                throw new ExamException("duplicate " + field + " id: " + id);
            }
        }
        return out;
    }

    private static void validateRefs(List<String> refs, Set<String> allowed, String field)
            throws ExamException {
        for (String ref : refs) {
            if (!allowed.contains(ref)) {
                throw new ExamException(field + " references unknown id: " + ref);
            }
        }
    }

    private static JsonArray array(JsonObject object, String field) throws ExamException {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonArray()) {
            throw new ExamException(field + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private static JsonObject object(JsonObject object, String field) throws ExamException {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonObject()) {
            throw new ExamException(field + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static String string(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String nullableString(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        return string(object, field);
    }

    private static boolean bool(JsonObject object, String field) throws ExamException {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isBoolean()) {
            throw new ExamException(field + " must be boolean");
        }
        return element.getAsBoolean();
    }

    private static List<String> strings(JsonArray array) throws ExamException {
        List<String> out = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new ExamException("array entries must be strings");
            }
            out.add(element.getAsString());
        }
        return out;
    }

    private static void nonEmpty(String value, String field) throws ExamException {
        if (value == null || value.trim().isEmpty()) {
            throw new ExamException(field + " must be a non-empty string");
        }
    }

    private static void requireEquals(String actual, String expected, String field)
            throws ExamException {
        if (!expected.equals(actual)) {
            throw new ExamException(field + " must be " + expected);
        }
    }

    private static final class OperationResult {
        final String state;
        final String reason;

        OperationResult(String state, String reason) {
            this.state = state;
            this.reason = reason;
        }
    }

    public static final class Result {
        public final String examId;
        public final String state;
        public final int executableOperations;
        public final int blockedOperations;
        public final boolean claimAllowed;
        public final String executionState;
        public final String physicalRepresentationSelection;
        public final List<String> evidenceRefs;
        public final List<String> limits;
        public final List<String> tokenVazio;
        public final String nextExecutableAction;

        Result(
                String examId,
                String state,
                int executableOperations,
                int blockedOperations,
                boolean claimAllowed,
                String executionState,
                String physicalRepresentationSelection,
                List<String> evidenceRefs,
                List<String> limits,
                List<String> tokenVazio,
                String nextExecutableAction
        ) {
            this.examId = examId;
            this.state = state;
            this.executableOperations = executableOperations;
            this.blockedOperations = blockedOperations;
            this.claimAllowed = claimAllowed;
            this.executionState = executionState;
            this.physicalRepresentationSelection = physicalRepresentationSelection;
            this.evidenceRefs = new ArrayList<>(evidenceRefs);
            this.limits = new ArrayList<>(limits);
            this.tokenVazio = new ArrayList<>(tokenVazio);
            this.nextExecutableAction = nextExecutableAction;
        }

        public String toJson() {
            return new Gson().toJson(this);
        }
    }

    public static final class ExamException extends Exception {
        public ExamException(String message) {
            super(message);
        }
    }
}
