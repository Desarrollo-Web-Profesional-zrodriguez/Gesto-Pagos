"""
Tier 4 - Scenario 04: Session Lifecycle, Inactivity Timeout & Heartbeat Renewal.
Scenario T4_SCEN_04.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestScenario04SessionLifecycle(unittest.TestCase):
    """End-to-end session management: Login -> Heartbeat reset -> Expiration -> Re-login -> Payment."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t4_scen_04_session_heartbeat_and_reauthentication_journey(self):
        """T4_SCEN_04: 5-minute inactivity heartbeat, rejection on expired token, relogin and payment."""
        # 1. Login to obtain active session
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200)
        token = login_resp.json.get("tokenSesion")
        self.assertTrue(token)

        # 2. Heartbeat renewal resets 5-minute inactivity timer
        val_resp = self.client.get(
            "/auth/validar-sesion",
            headers={"Authorization": f"Bearer {token}"},
        )
        self.assertEqual(val_resp.status_code, 200)
        self.assertTrue(val_resp.json.get("valida"))
        self.assertIn("Inactividad reseteada a 5 minutos", val_resp.json.get("mensaje", ""))

        # 3. Attempt payment with invalid/expired session token -> 401
        invalid_token = "00000000-0000-0000-0000-000000000000"
        fail_idemp_key = f"IDEMP-SCEN-04-EXP-{int(time.time() * 1000)}"
        fail_pay = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": invalid_token,
        }
        fail_resp = self.client.post("/pagos", json_data=fail_pay, headers={"Idempotency-Key": fail_idemp_key})
        self.assertEqual(fail_resp.status_code, 401)

        # 4. Re-login to obtain fresh active session
        relogin_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(relogin_resp.status_code, 200)
        new_token = relogin_resp.json.get("tokenSesion")

        # 5. Complete payment using new session token
        success_idemp_key = f"IDEMP-SCEN-04-OK-{int(time.time() * 1000)}"
        ok_pay = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": new_token,
        }
        ok_resp = self.client.post("/pagos", json_data=ok_pay, headers={"Idempotency-Key": success_idemp_key})
        self.assertIn(ok_resp.status_code, [200, 202])


if __name__ == "__main__":
    unittest.main()
