"""
Tier 1 - Feature 04: Payment Processing & State Differentiation (F04).
Tests T1_F04_01 to T1_F04_05.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA
from tests.e2e.fixtures.test_data import REFERENCE_EMBEDDING_STR


class TestFeature04Payment(unittest.TestCase):
    """Functional tests for payment execution, idempotency, 2FA validation, and UPC status query."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def _get_active_session_token(self) -> str:
        resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(resp.status_code, 200, f"Login failed: {resp.text}")
        return resp.json.get("tokenSesion")

    def test_t1_f04_01_utility_payment_session_token_2fa(self):
        """T1_F04_01: Execute successful utility payment with active session token as 2FA."""
        token = self._get_active_session_token()
        idemp_key = f"IDEMP-TX-UTIL-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": token,
        }
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("codigo"), "01")
        self.assertEqual(data.get("mensaje"), "Pago Exitoso")
        self.assertTrue(data.get("upc"), "UPC should be issued")
        self.assertTrue(data.get("numAutorizacion"), "Authorization should be issued")

    def test_t1_f04_02_airtime_payment_password_2fa(self):
        """T1_F04_02: Execute successful airtime recharge with user password as 2FA."""
        idemp_key = f"IDEMP-TX-AIR-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 1,
            "idProducto": 10,
            "monto": 100.00,
            "telefono": "5512345678",
            "2fa": DEFAULT_PASSWORD,
        }
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("codigo"), "01")
        self.assertTrue(data.get("numAutorizacion"))

    def test_t1_f04_03_entertainment_pin_biometrics_2fa(self):
        """T1_F04_03: Execute entertainment pin purchase with MediaPipe facial embedding as 2FA."""
        idemp_key = f"IDEMP-TX-PIN-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 50,
            "idProducto": 120,
            "monto": 250.00,
            "2fa": REFERENCE_EMBEDDING_STR,
        }
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("codigo"), "01")
        self.assertTrue(data.get("pin") or data.get("numAutorizacion"), "Should return PIN or auth number")

    def test_t1_f04_04_payment_with_email_receipt(self):
        """T1_F04_04: Execute payment with explicit receipt recipient email."""
        idemp_key = f"IDEMP-TX-MAIL-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
            "correoElectronico": "destinatario@example.com",
        }
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("codigo"), "01")
        self.assertIn("emailEnviado", data)

    def test_t1_f04_05_query_payment_status_by_upc(self):
        """T1_F04_05: Query status of previously executed payment by 10-char UPC."""
        # 1. Execute payment to get real UPC
        idemp_key = f"IDEMP-TX-UPC-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        pay_resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(pay_resp.status_code, 200)
        upc = pay_resp.json.get("upc")
        self.assertTrue(upc, "Payment must return UPC")

        # 2. Query status
        resp = self.client.get(f"/pagos/estatus/{upc}")
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("upc"), upc)
        self.assertEqual(data.get("estatus"), "EXITOSA")
        self.assertTrue(data.get("definitivo"))


if __name__ == "__main__":
    unittest.main()
