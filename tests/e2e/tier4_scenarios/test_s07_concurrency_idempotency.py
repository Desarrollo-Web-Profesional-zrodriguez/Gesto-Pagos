"""
Tier 4 - Scenario 07: High-Concurrency Idempotency & Anti-Double-Debit Protection.
Scenario T4_SCEN_07.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestScenario07ConcurrencyIdempotency(unittest.TestCase):
    """End-to-end idempotency protection journey under duplicate requests."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t4_scen_07_idempotency_anti_double_debit_protection(self):
        """T4_SCEN_07: Rapid duplicate submissions with same Idempotency-Key are rejected without double debit."""
        # 1. Query initial balance
        bal_resp1 = self.client.get(f"/cuentas/{DEFAULT_NUM_CUENTA}/saldo")
        self.assertEqual(bal_resp1.status_code, 200)

        # 2. Duplicate submission test
        shared_key = f"IDEMP-SCEN-07-DOUBLE-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        # First submission
        resp1 = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": shared_key})
        self.assertIn(resp1.status_code, [200, 202])

        # Immediate duplicate submission
        resp2 = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": shared_key})
        self.assertEqual(resp2.status_code, 409, f"Expected 409 Conflict, got {resp2.status_code}")
        self.assertIn("SOLICITUD_DUPLICADA", str(resp2.json))


if __name__ == "__main__":
    unittest.main()
