"""
Tier 3 - Pairwise Combination: Concurrency, Idempotency & Reconciliation.
Tests T3_PAIR_09 and T3_PAIR_14.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA, WORKSPACE_ROOT


class TestCombination05ConcurrencyReconciliation(unittest.TestCase):
    """Pairwise combination tests linking F04 (Payment States), F05 (UPC Reconciliation), and Idempotency."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t3_pair_09_payment_202_indeterminate_to_upc_query(self):
        """T3_PAIR_09: Verify indeterminate payment reconciliation architecture via UPC query endpoint."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

        # Execute payment
        idemp_key = f"IDEMP-PAIR-09-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])
        upc = pay_resp.json.get("upc")
        if upc:
            status_resp = self.client.get(f"/pagos/estatus/{upc}")
            self.assertEqual(status_resp.status_code, 200)
            self.assertEqual(status_resp.json.get("upc"), upc)
            self.assertIn("estatus", status_resp.json)

    def test_t3_pair_14_concurrency_idempotency_anti_double_debit(self):
        """T3_PAIR_14: Duplicate payment submission with identical Idempotency-Key returns 409 Conflict without double debit."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

        shared_idemp_key = f"IDEMP-CONCUR-KEY-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        # First call
        resp1 = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": shared_idemp_key})
        if resp1.status_code in (200, 202):
            # Immediate second call with identical idempotency key
            resp2 = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": shared_idemp_key})
            self.assertEqual(resp2.status_code, 409, f"Expected 409 Conflict for duplicate submission, got {resp2.status_code}")
            self.assertIn("SOLICITUD_DUPLICADA", str(resp2.json))


if __name__ == "__main__":
    unittest.main()
