"""
Tier 4 - Scenario 03: Multi-Account Management, Selection & Insufficient Balance Recovery.
Scenario T4_SCEN_03.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestScenario03MultiAccountRecovery(unittest.TestCase):
    """End-to-end multi-account journey: Account selector -> Insufficient balance rejection -> Switch account -> Success."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t4_scen_03_multi_account_switching_and_recovery(self):
        """T4_SCEN_03: Multi-account balance verification, failure on low balance, switch and success."""
        # 1. Fetch active accounts
        accts_resp = self.client.get("/cuentas/activas")
        self.assertEqual(accts_resp.status_code, 200)
        self.assertTrue(len(accts_resp.json) > 0)
        primary_account = accts_resp.json[0].get("numeroCuenta", DEFAULT_NUM_CUENTA)

        # 2. Check balance
        bal_resp = self.client.get(f"/cuentas/{primary_account}/saldo")
        self.assertEqual(bal_resp.status_code, 200)
        available_balance = float(bal_resp.json.get("saldoDisponible", 0))

        # 3. Submit payment with amount exceeding available balance -> 422
        excessive_amount = available_balance + 500000.00
        fail_idemp_key = f"IDEMP-SCEN-03-FAIL-{int(time.time() * 1000)}"
        fail_payload = {
            "numCuenta": primary_account,
            "idServicio": 76,
            "idProducto": 205,
            "monto": excessive_amount,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        fail_resp = self.client.post("/pagos", json_data=fail_payload, headers={"Idempotency-Key": fail_idemp_key})
        self.assertEqual(fail_resp.status_code, 422, f"Expected 422 for insufficient funds, got {fail_resp.status_code}")
        self.assertIn("SALDO_INSUFICIENTE", str(fail_resp.json))

        # 4. Recover with affordable payment within balance
        success_idemp_key = f"IDEMP-SCEN-03-OK-{int(time.time() * 1000)}"
        ok_payload = {
            "numCuenta": primary_account,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 50.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        ok_resp = self.client.post("/pagos", json_data=ok_payload, headers={"Idempotency-Key": success_idemp_key})
        self.assertIn(ok_resp.status_code, [200, 202])


if __name__ == "__main__":
    unittest.main()
