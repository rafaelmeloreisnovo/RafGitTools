#!/usr/bin/env python3
"""Validate RAFAELIA epistemic vocabulary and emit bounded standards projections.

The projections are deterministic structural mappings. They do not assert formal
conformance, signed provenance, scientific validity, remote execution or claim
promotion.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
import uuid
from pathlib import Path
from typing import Any

REQUIRED_STANDARDS = {
    "w3c_prov",
    "openlineage",
    "slsa_provenance",
    "spdx",
    "nist_ai_rmf",
}
REQUIRED_AXES = {
    "source_status",
    "epistemic_status",
    "operational_status",
    "claim_gate",
}
REQUIRED_TERMS = {
    "SESSION",
    "CLAIM",
    "SOURCE",
    "IMPLEMENTATION",
    "TEST",
    "EVIDENCE",
    "DECISION",
    "ROLLBACK",
    "TOKEN_VAZIO",
    "CONTRADICTION",
    "FALSIFIER",
    "GAP",
    "AUTHORITY",
    "ARTIFACT",
    "RUNTIME_CONTEXT",
}
MAPPING_STATUSES = {
    "STRUCTURAL_MAPPING",
    "STRUCTURAL_MAPPING_ONLY",
    "CONCEPTUAL_CROSSWALK",
}
BOUNDARY_FALSE = {
    "structural_mapping_is_conformance",
    "hash_is_authenticated_provenance",
    "schema_validity_is_scientific_validity",
    "documentation_is_runtime_evidence",
    "token_vazio_is_numeric",
    "automatic_cross_repository_write",
    "automatic_claim_promotion",
}


def load_json(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path}: root must be an object")
    return data


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def sha256_json(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def validate(profile: dict[str, Any], index: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    if profile.get("schema_version") != "1.0.0":
        errors.append("profile schema_version must be 1.0.0")
    if profile.get("profile") != "RAFAELIA-EPISTEMIC-INTEROP-1":
        errors.append("profile identifier mismatch")
    if profile.get("claim_allowed") is not False:
        errors.append("profile claim_allowed must remain false")
    if profile.get("authority_mode") != "federated-local-authority":
        errors.append("authority_mode mismatch")
    if profile.get("source_index") != "configs/workflow-master-index.json":
        errors.append("source_index must reference the canonical workflow master index")

    boundary = profile.get("boundary")
    if not isinstance(boundary, dict):
        errors.append("boundary must be an object")
    else:
        if set(boundary) != BOUNDARY_FALSE:
            errors.append("boundary fields mismatch")
        for field in BOUNDARY_FALSE:
            if boundary.get(field) is not False:
                errors.append(f"boundary.{field} must remain false")

    axes = profile.get("state_axes")
    if not isinstance(axes, list) or set(axes) != REQUIRED_AXES or len(axes) != 4:
        errors.append("state_axes must contain exactly the four independent axes")

    standards = profile.get("standards")
    if not isinstance(standards, dict) or set(standards) != REQUIRED_STANDARDS:
        errors.append("standards set mismatch")
    else:
        for name, spec in standards.items():
            if not isinstance(spec, dict):
                errors.append(f"standards.{name} must be an object")
                continue
            if spec.get("status") not in MAPPING_STATUSES:
                errors.append(f"standards.{name}.status invalid")
            if spec.get("conformance") != "TOKEN_VAZIO":
                errors.append(f"standards.{name}.conformance must remain TOKEN_VAZIO")
            for field in ("target", "purpose"):
                if not isinstance(spec.get(field), str) or not spec[field].strip():
                    errors.append(f"standards.{name}.{field} required")
        slsa = standards.get("slsa_provenance", {})
        if isinstance(slsa, dict) and slsa.get("attestation") != "TOKEN_VAZIO":
            errors.append("SLSA attestation must remain TOKEN_VAZIO")

    vocabulary = profile.get("vocabulary")
    if not isinstance(vocabulary, list):
        errors.append("vocabulary must be a list")
    else:
        seen: set[str] = set()
        for pos, entry in enumerate(vocabulary):
            label = f"vocabulary[{pos}]"
            if not isinstance(entry, dict):
                errors.append(f"{label} must be an object")
                continue
            term = entry.get("term")
            if not isinstance(term, str) or not term:
                errors.append(f"{label}.term required")
                continue
            if term in seen:
                errors.append(f"duplicate vocabulary term {term}")
            seen.add(term)
            if entry.get("kind") not in {"ENTITY", "ACTIVITY", "AGENT", "EPISTEMIC_STATE"}:
                errors.append(f"{term}: invalid kind")
            if not isinstance(entry.get("semantics"), str) or len(entry["semantics"].strip()) < 20:
                errors.append(f"{term}: semantics too short")
            mappings = entry.get("mappings")
            if not isinstance(mappings, dict) or set(mappings) != REQUIRED_STANDARDS:
                errors.append(f"{term}: mappings must cover every standard")
            elif any(not isinstance(v, str) or not v.strip() for v in mappings.values()):
                errors.append(f"{term}: mapping values must be non-empty strings")
        missing = REQUIRED_TERMS - seen
        if missing:
            errors.append(f"vocabulary missing {sorted(missing)}")

    relations = profile.get("relation_semantics")
    if not isinstance(relations, list) or not relations:
        errors.append("relation_semantics must be a non-empty list")
    else:
        relation_ids: set[str] = set()
        for pos, relation in enumerate(relations):
            if not isinstance(relation, dict):
                errors.append(f"relation_semantics[{pos}] must be an object")
                continue
            relation_id = relation.get("relation")
            if not isinstance(relation_id, str) or not relation_id:
                errors.append(f"relation_semantics[{pos}].relation required")
                continue
            if relation_id in relation_ids:
                errors.append(f"duplicate relation semantic {relation_id}")
            relation_ids.add(relation_id)
            for field in ("rule", "w3c_prov"):
                if not isinstance(relation.get(field), str) or not relation[field].strip():
                    errors.append(f"{relation_id}.{field} required")

    rules = profile.get("promotion_rules")
    if not isinstance(rules, list) or not rules:
        errors.append("promotion_rules must be a non-empty list")
    else:
        rule_ids: set[str] = set()
        for pos, rule in enumerate(rules):
            if not isinstance(rule, dict):
                errors.append(f"promotion_rules[{pos}] must be an object")
                continue
            rule_id = rule.get("id")
            if not isinstance(rule_id, str) or not rule_id:
                errors.append(f"promotion_rules[{pos}].id required")
                continue
            if rule_id in rule_ids:
                errors.append(f"duplicate promotion rule {rule_id}")
            rule_ids.add(rule_id)
            if rule.get("effect") not in {"BLOCK", "PRESERVE", "BLOCK_CONFORMANCE_CLAIM"}:
                errors.append(f"{rule_id}: invalid effect")
            if not isinstance(rule.get("condition"), str) or len(rule["condition"].strip()) < 20:
                errors.append(f"{rule_id}: condition too short")

    if index.get("claim_allowed") is not False:
        errors.append("master index claim_allowed must remain false")
    nodes = index.get("nodes")
    relations_index = index.get("relations")
    if not isinstance(nodes, list) or not nodes:
        errors.append("master index nodes must be a non-empty list")
        nodes = []
    if not isinstance(relations_index, list):
        errors.append("master index relations must be a list")
        relations_index = []

    node_ids: set[str] = set()
    for pos, node in enumerate(nodes):
        if not isinstance(node, dict):
            errors.append(f"master index node[{pos}] must be an object")
            continue
        node_id = node.get("id")
        if not isinstance(node_id, str) or not node_id:
            errors.append(f"master index node[{pos}].id required")
            continue
        if node_id in node_ids:
            errors.append(f"duplicate master index node {node_id}")
        node_ids.add(node_id)
        states = node.get("states")
        if not isinstance(states, dict) or set(states) != REQUIRED_AXES:
            errors.append(f"master index node {node_id}: state axes mismatch")

    for pos, relation in enumerate(relations_index):
        if not isinstance(relation, dict):
            errors.append(f"master index relation[{pos}] must be an object")
            continue
        if relation.get("producer") not in node_ids or relation.get("consumer") not in node_ids:
            errors.append(f"master index relation[{pos}] references unknown node")

    return errors


def node_map(index: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        node["id"]: node
        for node in index.get("nodes", [])
        if isinstance(node, dict) and isinstance(node.get("id"), str)
    }


def build_prov_projection(profile: dict[str, Any], index: dict[str, Any]) -> dict[str, Any]:
    nodes = node_map(index)
    entities: dict[str, Any] = {}
    activities: dict[str, Any] = {}
    agents = {
        "agent:rafgittools": {
            "prov:type": "prov:SoftwareAgent",
            "rafaelia:authorityMode": profile["authority_mode"],
            "rafaelia:claimAllowed": False,
        }
    }
    used: dict[str, Any] = {}
    derived: dict[str, Any] = {}
    associated: dict[str, Any] = {}
    attributed: dict[str, Any] = {}

    for node_id, node in sorted(nodes.items()):
        entity_id = f"repo:{node_id}"
        entities[entity_id] = {
            "prov:type": "rafaelia:RepositoryAuthority",
            "rafaelia:repository": node.get("repository"),
            "rafaelia:layer": node.get("layer"),
            "rafaelia:canonicalSource": node.get("canonical_source"),
            "rafaelia:sourceStatus": node.get("states", {}).get("source_status"),
            "rafaelia:epistemicStatus": node.get("states", {}).get("epistemic_status"),
            "rafaelia:operationalStatus": node.get("states", {}).get("operational_status"),
            "rafaelia:claimGate": node.get("states", {}).get("claim_gate"),
        }
        attributed[f"attribution:{node_id}"] = {
            "prov:entity": entity_id,
            "prov:agent": "agent:rafgittools",
        }

    for relation in sorted(index.get("relations", []), key=lambda item: item.get("id", "")):
        relation_id = relation["id"]
        activity_id = f"relation:{relation_id}"
        producer = f"repo:{relation['producer']}"
        consumer = f"repo:{relation['consumer']}"
        activities[activity_id] = {
            "prov:type": "rafaelia:FederatedContractActivity",
            "rafaelia:schemaVersion": relation.get("schema_version"),
            "rafaelia:compatibility": relation.get("compatibility"),
            "rafaelia:failureBehavior": relation.get("failure_behavior"),
            "rafaelia:evidenceRequired": relation.get("evidence_required"),
            "rafaelia:projectionStatus": "STRUCTURAL_MAPPING",
        }
        used[f"used:{relation_id}"] = {
            "prov:activity": activity_id,
            "prov:entity": producer,
        }
        derived[f"derivation:{relation_id}"] = {
            "prov:generatedEntity": consumer,
            "prov:usedEntity": producer,
            "prov:activity": activity_id,
        }
        associated[f"association:{relation_id}"] = {
            "prov:activity": activity_id,
            "prov:agent": "agent:rafgittools",
        }

    return {
        "prefix": {
            "prov": "http://www.w3.org/ns/prov#",
            "rafaelia": "urn:rafaelia:epistemic:",
        },
        "entity": entities,
        "activity": activities,
        "agent": agents,
        "used": used,
        "wasDerivedFrom": derived,
        "wasAssociatedWith": associated,
        "wasAttributedTo": attributed,
        "rafaelia:boundary": {
            "projectionStatus": "STRUCTURAL_MAPPING",
            "formalConformance": "TOKEN_VAZIO",
            "claimAllowed": False,
        },
    }


def build_openlineage_projection(profile: dict[str, Any], index: dict[str, Any]) -> dict[str, Any]:
    digest = sha256_json({"profile": profile, "index": index})
    run_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"urn:rafaelia:interop:{digest}"))
    nodes = node_map(index)
    producers = sorted({rel["producer"] for rel in index.get("relations", [])})
    consumers = sorted({rel["consumer"] for rel in index.get("relations", [])})

    def dataset(node_id: str) -> dict[str, Any]:
        node = nodes[node_id]
        return {
            "namespace": "github",
            "name": node["repository"],
            "facets": {
                "rafaelia_state": {
                    "_producer": "urn:rafaelia:rafgittools",
                    "_schemaURL": "urn:rafaelia:schema:openlineage-facet:state:1",
                    "sourceStatus": node["states"]["source_status"],
                    "epistemicStatus": node["states"]["epistemic_status"],
                    "operationalStatus": node["states"]["operational_status"],
                    "claimGate": node["states"]["claim_gate"],
                }
            },
        }

    return {
        "eventType": "OTHER",
        "eventTime": profile["observed_at"],
        "run": {
            "runId": run_id,
            "facets": {
                "rafaelia_boundary": {
                    "_producer": "urn:rafaelia:rafgittools",
                    "_schemaURL": "urn:rafaelia:schema:openlineage-facet:boundary:1",
                    "structuralMappingOnly": True,
                    "formalConformance": "TOKEN_VAZIO",
                    "claimAllowed": False,
                }
            },
        },
        "job": {
            "namespace": "rafaelia",
            "name": "federation.epistemic-provenance-projection",
            "facets": {},
        },
        "inputs": [dataset(node_id) for node_id in producers],
        "outputs": [dataset(node_id) for node_id in consumers],
        "producer": "urn:rafaelia:rafgittools",
        "schemaURL": "urn:rafaelia:schema:openlineage-run-event-projection:1",
    }


def build_slsa_projection(profile: dict[str, Any], index: dict[str, Any]) -> dict[str, Any]:
    index_digest = sha256_json(index)
    dependencies = [
        {
            "uri": f"https://github.com/{node['repository']}",
            "digest": {"sha256": "TOKEN_VAZIO"},
            "annotations": {
                "sourceStatus": node["states"]["source_status"],
                "canonicalSource": node["canonical_source"],
            },
        }
        for node in sorted(index.get("nodes", []), key=lambda item: item.get("id", ""))
    ]
    invocation_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"urn:rafaelia:slsa:{index_digest}"))
    return {
        "_type": "https://in-toto.io/Statement/v1",
        "subject": [
            {
                "name": "configs/workflow-master-index.json",
                "digest": {"sha256": index_digest},
            }
        ],
        "predicateType": "https://slsa.dev/provenance/v1",
        "predicate": {
            "buildDefinition": {
                "buildType": "urn:rafaelia:epistemic-projection:v1",
                "externalParameters": {
                    "profile": profile["profile"],
                    "claimAllowed": False,
                },
                "internalParameters": {
                    "projectionStatus": "STRUCTURAL_MAPPING_ONLY",
                    "attestationStatus": "TOKEN_VAZIO",
                },
                "resolvedDependencies": dependencies,
            },
            "runDetails": {
                "builder": {"id": "urn:rafaelia:rafgittools"},
                "metadata": {
                    "invocationId": invocation_id,
                    "startedOn": profile["observed_at"],
                    "finishedOn": profile["observed_at"],
                },
            },
        },
        "rafaeliaBoundary": {
            "signed": False,
            "verifiedBuilder": "TOKEN_VAZIO",
            "formalSlsaConformance": "TOKEN_VAZIO",
            "claimAllowed": False,
        },
    }


def spdx_id(node_id: str) -> str:
    safe = "".join(ch if ch.isalnum() or ch in ".-" else "-" for ch in node_id)
    return f"SPDXRef-Repository-{safe}"


def build_spdx_projection(profile: dict[str, Any], index: dict[str, Any]) -> dict[str, Any]:
    digest = sha256_json(index)
    packages = []
    for node in sorted(index.get("nodes", []), key=lambda item: item.get("id", "")):
        packages.append(
            {
                "SPDXID": spdx_id(node["id"]),
                "name": node["repository"],
                "downloadLocation": f"https://github.com/{node['repository']}",
                "filesAnalyzed": False,
                "licenseConcluded": "NOASSERTION",
                "licenseDeclared": "NOASSERTION",
                "copyrightText": "NOASSERTION",
                "annotations": [
                    {
                        "annotationType": "OTHER",
                        "annotator": "Tool: RafGitTools-epistemic-interop-v1",
                        "annotationDate": profile["observed_at"],
                        "comment": json.dumps(
                            {
                                "source_status": node["states"]["source_status"],
                                "epistemic_status": node["states"]["epistemic_status"],
                                "operational_status": node["states"]["operational_status"],
                                "claim_gate": node["states"]["claim_gate"],
                            },
                            sort_keys=True,
                        ),
                    }
                ],
            }
        )

    relationships = []
    for node in index.get("nodes", []):
        for dependency in node.get("dependencies", []):
            relationships.append(
                {
                    "spdxElementId": spdx_id(node["id"]),
                    "relationshipType": "DEPENDS_ON",
                    "relatedSpdxElement": spdx_id(dependency),
                }
            )
    for relation in index.get("relations", []):
        relationships.append(
            {
                "spdxElementId": spdx_id(relation["consumer"]),
                "relationshipType": "DEPENDS_ON",
                "relatedSpdxElement": spdx_id(relation["producer"]),
                "comment": f"federated contract {relation['id']} ({relation['schema_version']})",
            }
        )

    return {
        "spdxVersion": "SPDX-2.3",
        "dataLicense": "CC0-1.0",
        "SPDXID": "SPDXRef-DOCUMENT",
        "name": "RAFAELIA epistemic federation structural inventory",
        "documentNamespace": f"urn:rafaelia:spdx:epistemic:{digest}",
        "creationInfo": {
            "created": profile["observed_at"],
            "creators": ["Tool: RafGitTools-epistemic-interop-v1"],
        },
        "documentDescribes": [package["SPDXID"] for package in packages],
        "packages": packages,
        "relationships": relationships,
        "annotations": [
            {
                "annotationType": "OTHER",
                "annotator": "Tool: RafGitTools-epistemic-interop-v1",
                "annotationDate": profile["observed_at"],
                "comment": "Structural inventory only; SPDX conformance and legal conclusions remain TOKEN_VAZIO.",
            }
        ],
    }


def build_nist_crosswalk(profile: dict[str, Any]) -> dict[str, Any]:
    terms = {entry["term"]: entry for entry in profile["vocabulary"]}
    functions = {name: [] for name in ("GOVERN", "MAP", "MEASURE", "MANAGE")}
    for term, entry in sorted(terms.items()):
        mapping = entry["mappings"]["nist_ai_rmf"]
        for function in functions:
            if function in mapping.split("|"):
                functions[function].append(term)
    return {
        "profile": profile["profile"],
        "status": "CONCEPTUAL_CROSSWALK",
        "conformance": "TOKEN_VAZIO",
        "functions": functions,
        "boundary": "This crosswalk does not assert NIST certification, compliance or risk acceptance.",
    }


def build_outputs(profile: dict[str, Any], index: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        "epistemic-provenance.prov.json": build_prov_projection(profile, index),
        "epistemic-lineage.openlineage.json": build_openlineage_projection(profile, index),
        "epistemic-provenance.slsa.json": build_slsa_projection(profile, index),
        "epistemic-inventory.spdx.json": build_spdx_projection(profile, index),
        "epistemic-risk-crosswalk.nist-ai-rmf.json": build_nist_crosswalk(profile),
    }


def build_report(
    profile: dict[str, Any],
    index: dict[str, Any],
    errors: list[str],
    outputs: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    return {
        "schema": "rafaelia.epistemic-provenance-interop.report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "profile_digest": sha256_json(profile),
        "master_index_digest": sha256_json(index),
        "vocabulary_terms": len(profile.get("vocabulary", [])),
        "repository_nodes": len(index.get("nodes", [])),
        "federated_relations": len(index.get("relations", [])),
        "projection_digests": {
            name: sha256_json(value) for name, value in sorted(outputs.items())
        }
        if not errors
        else {},
        "formal_conformance": {
            standard: "TOKEN_VAZIO" for standard in sorted(REQUIRED_STANDARDS)
        },
        "validation_errors": errors,
        "boundary": (
            "PASS proves deterministic structural mapping and internal invariants only; "
            "it does not prove signed provenance, standard certification, remote runtime, "
            "scientific truth or permission to promote claims."
        ),
    }


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--index", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    try:
        profile = load_json(args.profile)
        index = load_json(args.index)
    except Exception as exc:
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 2

    errors = validate(profile, index)
    outputs = build_outputs(profile, index) if not errors else {}
    report = build_report(profile, index, errors, outputs)

    if args.output_dir and not errors:
        for name, value in outputs.items():
            write_json(args.output_dir / name, value)
    if args.report:
        write_json(args.report, report)
    print(json.dumps(report, indent=2, ensure_ascii=False, sort_keys=True))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
