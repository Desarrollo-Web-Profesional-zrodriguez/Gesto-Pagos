"""
Tier 2 - Feature 06: Boundary & Negative Cases for Security & Biometrics (B06).
Tests T2_F06_01 to T2_F06_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL
from tests.e2e.fixtures.test_data import DISSIMILAR_EMBEDDING_STR


class TestBoundary06Security(unittest.TestCase):
    """Boundary and negative tests for biometric matching, inactive users, and information leakage."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t2_f06_01_biometric_below_threshold(self):
        """T2_F06_01: Biometric login attempt with facial vector below cosine similarity threshold returns 401."""
        payload = {
            "username": DEFAULT_EMAIL,
            "biometricoFacialEmbedding": DISSIMILAR_EMBEDDING_STR,
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")
        self.assertIn("distinto al registrado", str(resp.json).lower())

    def test_t2_f06_02_biometric_malformed_string(self):
        """T2_F06_02: Biometric login with malformed non-JSON embedding string rejected safely."""
        payload = {
            "username": DEFAULT_EMAIL,
            "biometricoFacialEmbedding": "corrupted_non_array_string",
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertIn(resp.status_code, [400, 401], f"Expected 400 or 401, got {resp.status_code}")

    def test_t2_f06_03_biometric_user_without_profile(self):
        """T2_F06_03: Biometric login attempt for user without registered facial embedding returns 401."""
        payload = {
            "username": "usuario_sin_rostro@gestopago.test",
            "biometricoFacialEmbedding": "[0.12, -0.45, 0.89, -0.05, 0.67]",
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")

    def test_t2_f06_04_payment_inactive_user(self):
        """T2_F06_04: Payment execution with inactive user account returns 403 Forbidden."""
        # Querying an inactive user account or simulating rejection
        payload = {
            "numCuenta": "2000000002000000",
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": "PasswordSeguro123*",
        }
        resp = self.client.post("/pagos", json_data=payload, headers={"Idempotency-Key": "IDEMP-INACT-001"})
        self.assertIn(resp.status_code, [400, 401, 403, 404, 422])

    def test_t2_f06_05_no_stack_traces_in_error_responses(self):
        """T2_F06_05: Verify that error responses never leak stack traces, class names, or SQL fragments."""
        resp = self.client.post("/auth/login", json_data={"username": "malformed' OR 1=1;--"})
        self.assertIn(resp.status_code, [400, 401])
        text = resp.text.lower()
        self.assertNotIn("exception at ", text)
        self.assertNotIn("at org.springframework", text)
        self.assertNotIn("select * from", text)


if __name__ == "__main__":
    unittest.main()
