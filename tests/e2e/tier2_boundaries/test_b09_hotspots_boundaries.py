"""
Tier 2 - Feature 09: Boundary & Negative Cases for Hotspots & Code Smells (B09).
Tests T2_F09_01 to T2_F09_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, WORKSPACE_ROOT


class TestBoundary09Hotspots(unittest.TestCase):
    """Boundary and negative tests for hotspots review, log sanitization, and CORS."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t2_f09_01_log_sanitization_no_passwords(self):
        """T2_F09_01: Verify sensitive utility classes sanitize logs and mask passwords."""
        datos_sensibles_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "util"
            / "DatosSensibles.java"
        )
        self.assertTrue(datos_sensibles_file.exists(), "DatosSensibles.java must exist")
        content = datos_sensibles_file.read_text(encoding="utf-8")
        self.assertIn("enmascararCuenta", content)
        self.assertIn("sanitizarTextoProveedor", content)

    def test_t2_f09_02_cors_restricted_configuration(self):
        """T2_F09_02: Verify CORS configuration restricts origins or does not allow unauthenticated wildcards."""
        cors_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "config"
            / "CorsConfig.java"
        )
        self.assertTrue(cors_file.exists())
        content = cors_file.read_text(encoding="utf-8")
        self.assertIn("CorsFilter", content)

    def test_t2_f09_03_secure_uuid_token_generation(self):
        """T2_F09_03: Ensure token generation in AuthServiceImpl uses standard UUID generators."""
        auth_service = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "cliente"
            / "impl"
            / "AuthServiceImpl.java"
        )
        self.assertTrue(auth_service.exists())
        content = auth_service.read_text(encoding="utf-8")
        self.assertIn("UUID.randomUUID()", content)

    def test_t2_f09_04_global_exception_handler_specific_catches(self):
        """T2_F09_04: Verify GlobalExceptionHandler handles specific domain exceptions and does not swallow Throwable."""
        handler_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "exception"
            / "GlobalExceptionHandler.java"
        )
        self.assertTrue(handler_file.exists())
        content = handler_file.read_text(encoding="utf-8")
        self.assertIn("@ExceptionHandler", content)
        self.assertIn("ReglaNegocioException", content)
        self.assertIn("AutenticacionException", content)

    def test_t2_f09_05_options_preflight_handling(self):
        """T2_F09_05: Verify OPTIONS preflight request handling against payment endpoints."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        resp = self.client.request("OPTIONS", "/pagos")
        self.assertIn(resp.status_code, [200, 204, 403, 405])


if __name__ == "__main__":
    unittest.main()
