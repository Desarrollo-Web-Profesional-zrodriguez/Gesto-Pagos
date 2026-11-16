"""
Tier 3 - Pairwise Combination: Catalog & Reference Validation with Payment Processing.
Tests T3_PAIR_03, T3_PAIR_04, and T3_PAIR_05.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestCombination02CatalogValidation(unittest.TestCase):
    """Pairwise combination tests linking F02 (Catalog), F03 (Reference Validation), and F04 (Payment)."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t3_pair_03_catalog_discovery_and_reference_validation(self):
        """T3_PAIR_03: Dynamically browse catalog, extract service/product, and validate reference."""
        # 1. Browse catalog
        cat_resp = self.client.get("/productos")
        self.assertEqual(cat_resp.status_code, 200)
        categorias = cat_resp.json.get("categorias", {})
        cat2_items = categorias.get("2", []) or categorias.get(2, [])
        self.assertTrue(len(cat2_items) > 0, "Catalog must contain services in category 2")

        item = cat2_items[0]
        id_servicio = item.get("idServicio")
        id_producto = item.get("idProducto")
        self.assertIsNotNone(id_servicio)
        self.assertIsNotNone(id_producto)

        # 2. Validate reference using discovered product
        val_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": id_servicio,
            "idProducto": id_producto,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        val_resp = self.client.post("/pagos/validar-referencia", json_data=val_payload)
        self.assertEqual(val_resp.status_code, 200)
        self.assertTrue(val_resp.json.get("valida"))

    def test_t3_pair_04_validated_reference_to_payment_execution(self):
        """T3_PAIR_04: Pre-validate utility reference, then execute payment with exact parameters."""
        # 1. Pre-validate reference
        val_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        val_resp = self.client.post("/pagos/validar-referencia", json_data=val_payload)
        self.assertEqual(val_resp.status_code, 200)
        monto_validado = float(val_resp.json.get("montoValidado", 150.00))

        # 2. Confirm payment
        idemp_key = f"IDEMP-PAIR-04-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": monto_validado,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])

    def test_t3_pair_05_invalid_reference_blocks_payment(self):
        """T3_PAIR_05: Direct payment attempt with reference that fails validation (000000) is rejected."""
        idemp_key = f"IDEMP-PAIR-05-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "000000",  # all zeros invalid
            "2fa": DEFAULT_PASSWORD,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(pay_resp.status_code, 400)
        self.assertIn("REFERENCIA_INVALIDA", str(pay_resp.json))


if __name__ == "__main__":
    unittest.main()
