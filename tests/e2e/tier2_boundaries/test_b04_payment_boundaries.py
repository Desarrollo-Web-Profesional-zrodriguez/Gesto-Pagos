"""
Tier 2 - Feature 04: Boundary & Negative Cases for Payment Processing (B04).
Tests T2_F04_01 to T2_F04_05.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestBoundary04Payment(unittest.TestCase):
    """Boundary and negative tests for payment execution, idempotency, and 2FA."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t2_f04_01_insufficient_funds(self):
        """T2_F04_01: Payment attempt where amount exceeds available balance returns 422 (SALDO_INSUFICIENTE)."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 999999.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        idemp_key = f"IDEMP-ERR-FUNDS-{int(time.time() * 1000)}"
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(resp.status_code, 422, f"Expected 422, got {resp.status_code}")
        self.assertIn("SALDO_INSUFICIENTE", str(resp.json))

    def test_t2_f04_02_invalid_2fa(self):
        """T2_F04_02: Payment attempt with invalid 2FA code returns 401 (AUTENTICACION_2FA_FALLIDA)."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": "Wrong2FACode!999",
        }
        idemp_key = f"IDEMP-ERR-2FA-{int(time.time() * 1000)}"
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")
        self.assertIn("AUTENTICACION_2FA_FALLIDA", str(resp.json))

    def test_t2_f04_03_invalid_idempotency_key_format(self):
        """T2_F04_03: Payment with invalid Idempotency-Key format (< 8 chars) returns 400 Bad Request."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": "short"})
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f04_04_duplicate_idempotency_submission(self):
        """T2_F04_04: Duplicate payment submission with identical Idempotency-Key returns 409 Conflict."""
        idemp_key = f"IDEMP-DUP-TEST-{int(time.time() * 1000)}"
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        # First submission
        resp1 = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        if resp1.status_code in (200, 202):
            # Immediate second submission with identical key
            resp2 = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
            self.assertEqual(resp2.status_code, 409, f"Expected 409, got {resp2.status_code}")
            self.assertIn("SOLICITUD_DUPLICADA", str(resp2.json))

    def test_t2_f04_05_negative_or_zero_amount(self):
        """T2_F04_05: Payment with zero or negative amount returns 400 or 422 Bad Request."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 0.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        idemp_key = f"IDEMP-ZERO-{int(time.time() * 1000)}"
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(resp.status_code, [400, 422], f"Expected 400 or 422, got {resp.status_code}")


if __name__ == "__main__":
    unittest.main()
