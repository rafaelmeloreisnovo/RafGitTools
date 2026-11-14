"""Hosted inventory compatibility auditor, not a freestanding runtime or writer.

Normalizes two observed Drive JSONL field layouts without importing private
records into a repository. Scope is ONE batch of node/edge observations; it
cannot prove historical uniqueness, a provider lock, or byte equivalence.
No third-party packages. Fail closed on malformed or ambiguous aliases.
"""


class InventoryContractError(ValueError):
    pass


def _str(record, field):
    value = record.get(field)
    if not isinstance(value, str) or not value or len(value) > 1024:
        raise InventoryContractError("invalid required field: " + field)
    return value


def _alias(record, old, new):
    if (old in record) == (new in record):
        raise InventoryContractError("missing/ambiguous alias: " + old + "/" + new)
    return record[old] if old in record else record[new]


def normalize_node(record):
    if not isinstance(record, dict):
        raise InventoryContractError("node must be object")
    source = _str(record, "provider"), _str(record, "account")
    key = source + (_str(record, "id"),)
    parents = _alias(record, "parent_ids", "parents")
    if not isinstance(parents, list) or not parents:
        raise InventoryContractError("missing node parents")
    if any(not isinstance(parent, str) or not parent or len(parent) > 1024
           for parent in parents) or len(set(parents)) != len(parents):
        raise InventoryContractError("invalid/duplicate node parents")
    return key, frozenset(source + (parent, key[-1], "parent_of")
                          for parent in parents)


def normalize_edge(record):
    if not isinstance(record, dict):
        raise InventoryContractError("edge must be object")
    source = _str(record, "provider"), _str(record, "account")
    old_layout = all(k in record for k in ("relation", "from", "to")) and not any(k in record for k in ("type", "parent", "child"))
    new_layout = all(k in record for k in ("type", "parent", "child")) and not any(k in record for k in ("relation", "from", "to"))
    if not (old_layout or new_layout):
        raise InventoryContractError("mixed/incomplete edge layout")
    relation = _alias(record, "relation", "type")
    parent = _alias(record, "from", "parent")
    child = _alias(record, "to", "child")
    if relation != "parent_of":
        raise InventoryContractError("unsupported relation type")
    if not isinstance(parent, str) or not parent or len(parent) > 1024:
        raise InventoryContractError("invalid edge parent")
    if not isinstance(child, str) or not child or len(child) > 1024:
        raise InventoryContractError("invalid edge child")
    return source + (parent, child, relation)


def audit_batch(nodes, edges):
    """Validate ONE scoped batch. Counts are observations, NOT global unique IDs."""
    if not isinstance(nodes, list) or not isinstance(edges, list) or not nodes or not edges:
        raise InventoryContractError("nonempty lists of nodes and edges required")
    keys, expected, actual = set(), set(), set()
    for raw in nodes:
        key, parent_edges = normalize_node(raw)
        if key in keys:
            raise InventoryContractError("duplicate node identity within batch")
        keys.add(key)
        expected.update(parent_edges)
    for raw in edges:
        edge = normalize_edge(raw)
        if edge in actual:
            raise InventoryContractError("duplicate edge within batch")
        actual.add(edge)
    if actual != expected:
        raise InventoryContractError("edge/parent mismatch or missing relation")
    return frozenset(keys), frozenset(actual)


def audit_pair(nodes_a, edges_a, nodes_b, edges_b):
    """Compare ONLY two chosen batches after successful independent audits."""
    a_nodes, a_edges = audit_batch(nodes_a, edges_a)
    b_nodes, b_edges = audit_batch(nodes_b, edges_b)
    return {
        "a_observed_nodes": len(nodes_a), "a_observed_edges": len(edges_a),
        "b_observed_nodes": len(nodes_b), "b_observed_edges": len(edges_b),
        "overlap_nodes": len(a_nodes & b_nodes),
        "overlap_edges": len(a_edges & b_edges),
        "distinct_nodes_in_pair": len(a_nodes | b_nodes),
        "distinct_edges_in_pair": len(a_edges | b_edges),
        "historical_global_uniqueness": "TOKEN_VAZIO",
        "provider_single_writer": "TOKEN_VAZIO",
        "source_byte_sha256": "NOT_TESTED",
        "claim_allowed": False,
    }
