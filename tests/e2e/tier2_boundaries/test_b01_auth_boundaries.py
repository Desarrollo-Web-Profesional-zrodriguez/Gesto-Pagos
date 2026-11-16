"""
Tier 2 - Feature 01: Boundary & Negative Cases for Shared Authentication (B01).
Tests T2_F01_01 to T2_F01_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_PASSWORD


class TestBoundary01Auth(unittest.TestCase):
    """Boundary and negative tests for authentication and session heartbeat."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t2_f01_01_login_incorrect_password(self):
        """T2_F01_01: Login attempt with incorrect password for existing user returns 401."""
        payload = {
            "username": DEFAULT_EMAIL,
            "password": "WrongPassword999!",
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")
        data = resp.json
        self.assertIn("AUTENTICACION_2FA_FALLIDA", str(data))

    def test_t2_f01_02_login_nonexistent_email(self):
        """T2_F01_02: Login attempt with non-existent email username returns 401."""
        payload = {
            "username": "noexiste_987654@gestopago.test",
            "password": DEFAULT_PASSWORD,
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")

    def test_t2_f01_03_login_malformed_email(self):
        """T2_F01_03: Login attempt with malformed email syntax returns 400 Bad Request."""
        payload = {
            "username": "usuario_sin_arroba",
            "password": DEFAULT_PASSWORD,
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f01_04_session_heartbeat_expired_token(self):
        """T2_F01_04: Session heartbeat validation on expired or non-existent token returns 401."""
        fake_expired_token = "00000000-0000-0000-0000-000000000000"
        resp = self.client.get(
            "/auth/validar-sesion",
            headers={"Authorization": f"Bearer {fake_expired_token}"},
        )
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")

    def test_t2_f01_05_session_heartbeat_missing_token(self):
        """T2_F01_05: Session heartbeat invocation without Authorization header and without query param returns 401."""
        resp = self.client.get("/auth/validar-sesion")
        self.assertEqual(resp.status_code, 401, f"Expected 401, got {resp.status_code}")


if __name__ == "__main__":
    unittest.main()
