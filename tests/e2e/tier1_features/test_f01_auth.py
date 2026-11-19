"""
Tier 1 - Feature 01: Shared Authentication & Session Heartbeat (F01).
Tests T1_F01_01 to T1_F01_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_PASSWORD
from tests.e2e.fixtures.test_data import REFERENCE_EMBEDDING_STR


class TestFeature01Auth(unittest.TestCase):
    """Functional tests for shared authentication, biometrics, and session heartbeat."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t1_f01_01_email_password_login(self):
        """T1_F01_01: Authenticate existing customer via email and password credentials."""
        payload = {
            "username": DEFAULT_EMAIL,
            "password": DEFAULT_PASSWORD,
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("activa"), "Session should be active")
        self.assertTrue(data.get("tokenSesion"), "Token should be present")
        self.assertEqual(data.get("minutosInactividadMaximos"), 5)
        self.assertEqual(data.get("username"), DEFAULT_EMAIL)

    def test_t1_f01_02_biometric_login(self):
        """T1_F01_02: Authenticate customer via MediaPipe facial embedding biometric vector."""
        payload = {
            "username": DEFAULT_EMAIL,
            "biometricoFacialEmbedding": REFERENCE_EMBEDDING_STR,
        }
        resp = self.client.post("/auth/login", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("activa"), "Session should be active")
        self.assertTrue(data.get("tokenSesion"), "Token should be present")
        self.assertIn("MediaPipe", data.get("mensaje", ""), "Message should reference MediaPipe biometrics")

    def test_t1_f01_03_validate_session_header(self):
        """T1_F01_03: Validate active session and reset 5-minute inactivity heartbeat via Bearer header."""
        # 1. Login to get fresh token
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200)
        token = login_resp.json.get("tokenSesion")

        # 2. Validate session via header
        resp = self.client.get(
            "/auth/validar-sesion",
            headers={"Authorization": f"Bearer {token}"},
        )
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("valida"))
        self.assertIn("Inactividad reseteada a 5 minutos", data.get("mensaje", ""))

    def test_t1_f01_04_validate_session_query_param(self):
        """T1_F01_04: Validate active session and reset inactivity heartbeat via query parameter fallback."""
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200)
        token = login_resp.json.get("tokenSesion")

        resp = self.client.get("/auth/validar-sesion", params={"token": token})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        self.assertTrue(resp.json.get("valida"))

    def test_t1_f01_05_explicit_logout(self):
        """T1_F01_05: Explicit logout terminating active session and setting status to inactive."""
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200)
        token = login_resp.json.get("tokenSesion")

        resp = self.client.post(
            "/auth/logout",
            headers={"Authorization": f"Bearer {token}"},
        )
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        self.assertIn("exitosamente", resp.json.get("mensaje", "").lower())


if __name__ == "__main__":
    unittest.main()
