"""Synthetic tests: no private metadata or provider API access."""
import unittest
from inventory_jsonl_compat_v1 import InventoryContractError, audit_batch, audit_pair


def old_rows(identifier="a", parent="p"):
    return ([{"schema": "v2", "batch": "test", "provider": "gdrive",
              "account": "test", "id": identifier, "parent_ids": [parent]}],
            [{"schema": "v2", "batch": "test", "provider": "gdrive",
              "account": "test", "relation": "parent_of", "from": parent,
              "to": identifier, "source_id": identifier}])


def new_rows(identifier="b", parent="p"):
    return ([{"provider": "gdrive", "account": "test", "id": identifier,
              "parents": [parent]}],
            [{"provider": "gdrive", "account": "test", "type": "parent_of",
              "parent": parent, "child": identifier, "source_page": 41}])


class TestCompatibility(unittest.TestCase):
    def test_old_schema(self):
        self.assertEqual(len(audit_batch(*old_rows())[0]), 1)

    def test_new_schema(self):
        self.assertEqual(len(audit_batch(*new_rows())[1]), 1)

    def test_pair_distinct(self):
        self.assertEqual(audit_pair(*old_rows(), *new_rows())["distinct_nodes_in_pair"], 2)

    def test_pair_overlap(self):
        self.assertEqual(audit_pair(*old_rows(), *new_rows("a"))["overlap_nodes"], 1)

    def test_no_global_claim(self):
        self.assertFalse(audit_pair(*old_rows(), *new_rows())["claim_allowed"])

    def test_missing_parents(self):
        nodes, edges = new_rows()
        del nodes[0]["parents"]
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_ambiguous_parent_alias(self):
        nodes, edges = new_rows()
        nodes[0]["parent_ids"] = ["p"]
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_ambiguous_edge_alias(self):
        nodes, edges = old_rows()
        edges[0]["parent"] = "p"
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_mixed_edge_layout(self):
        nodes, edges = old_rows()
        edges[0]["parent"] = edges[0].pop("from")
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_wrong_parent(self):
        nodes, edges = old_rows()
        edges[0]["from"] = "other"
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_duplicate_identity(self):
        nodes, edges = new_rows()
        nodes.append(dict(nodes[0]))
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_duplicate_edge(self):
        nodes, edges = new_rows()
        edges.append(dict(edges[0]))
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_empty_data(self):
        with self.assertRaises(InventoryContractError): audit_batch([], [])

    def test_unsupported_relation(self):
        nodes, edges = new_rows()
        edges[0]["type"] = "cites"
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)

    def test_cross_account_same_id(self):
        a_nodes, a_edges = old_rows()
        b_nodes, b_edges = new_rows("a")
        b_nodes[0]["account"] = "other"
        b_edges[0]["account"] = "other"
        self.assertEqual(audit_pair(a_nodes, a_edges, b_nodes, b_edges)["overlap_nodes"], 0)

    def test_cross_provider_same_id(self):
        a_nodes, a_edges = old_rows()
        b_nodes, b_edges = new_rows("a")
        b_nodes[0]["provider"] = "different"
        b_edges[0]["provider"] = "different"
        self.assertEqual(audit_pair(a_nodes, a_edges, b_nodes, b_edges)["overlap_nodes"], 0)

    def test_malformed_id(self):
        nodes, edges = old_rows()
        nodes[0]["id"] = None
        with self.assertRaises(InventoryContractError): audit_batch(nodes, edges)


if __name__ == "__main__":
    unittest.main()
