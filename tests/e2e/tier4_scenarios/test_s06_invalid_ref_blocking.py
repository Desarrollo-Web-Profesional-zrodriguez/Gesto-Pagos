"""
Tier 4 - Scenario 06: Invalid Reference Progression Blocking & Error Recovery.
Scenario T4_SCEN_06.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestScenario06InvalidRefBlocking(unittest.TestCase):
    """End-to-end negative progression journey: Invalid reference blocked -> Bypass blocked -> Validated -> Success."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t4_scen_06_reference_blocking_and_recovery_flow(self):
        """T4_SCEN_06: Invalid reference strictly blocks payment and permits seamless error recovery."""
        # 1. User submits invalid short reference "12" (< 4 chars)
        bad_val_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "12",
            "monto": 150.00,
        }
        bad_val_resp = self.client.post("/pagos/validar-referencia", json_data=bad_val_payload)
        self.assertEqual(bad_val_resp.status_code, 400)
        self.assertIn("REFERENCIA_INVALIDA", str(bad_val_resp.json))

        # 2. Malicious user tries direct payment with invalid reference -> Backend rejects
        bad_idemp_key = f"IDEMP-SCEN-06-BAD-{int(time.time() * 1000)}"
        bad_pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "12",
            "2fa": DEFAULT_PASSWORD,
        }
        bad_pay_resp = self.client.post("/pagos", json_data=bad_pay_payload, headers={"Idempotency-Key": bad_idemp_key})
        self.assertEqual(bad_pay_resp.status_code, 400)

        # 3. User corrects reference input to valid "2572038101" -> 200 OK
        good_val_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        good_val_resp = self.client.post("/pagos/validar-referencia", json_data=good_val_payload)
        self.assertEqual(good_val_resp.status_code, 200)
        self.assertTrue(good_val_resp.json.get("valida"))

        # 4. Confirmation succeeds
        good_idemp_key = f"IDEMP-SCEN-06-OK-{int(time.time() * 1000)}"
        good_pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        good_pay_resp = self.client.post("/pagos", json_data=good_pay_payload, headers={"Idempotency-Key": good_idemp_key})
        self.assertIn(good_pay_resp.status_code, [200, 202])


if __name__ == "__main__":
    unittest.main()
