"""
Tier 3 - Pairwise Combination: Authentication & Session with Payment Processing.
Tests T3_PAIR_01 and T3_PAIR_08.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestCombination01AuthPayment(unittest.TestCase):
    """Pairwise combination tests linking F01 (Auth) and F04 (Payment)."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t3_pair_01_auth_session_token_payment(self):
        """T3_PAIR_01: User logs in, receives session token, and uses that token as 2FA to confirm payment."""
        # 1. Login
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200)
        token = login_resp.json.get("tokenSesion")
        self.assertTrue(token)

        # 2. Payment confirmation using session token as 2FA
        idemp_key = f"IDEMP-PAIR-01-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": token,
        }
        pay_resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])
        if pay_resp.status_code == 200:
            self.assertEqual(pay_resp.json.get("codigo"), "01")

    def test_t3_pair_08_expired_session_payment_rejection(self):
        """T3_PAIR_08: Payment confirmation attempt using an expired or invalid session token as 2FA returns 401."""
        expired_token = "ffffffff-ffff-ffff-ffff-ffffffffffff"
        idemp_key = f"IDEMP-PAIR-08-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": expired_token,
        }
        pay_resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(pay_resp.status_code, 401)
        self.assertIn("AUTENTICACION_2FA_FALLIDA", str(pay_resp.json))


if __name__ == "__main__":
    unittest.main()
