import unittest
from scripts.validate_matrix_compose_contract import validate

class MatrixComposeContractTests(unittest.TestCase):
    def test_contract(self):
        self.assertEqual(validate(), [])

if __name__ == "__main__":
    unittest.main()
