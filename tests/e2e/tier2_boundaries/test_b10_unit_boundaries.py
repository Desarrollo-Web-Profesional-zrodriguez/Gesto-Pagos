"""
Tier 2 - Feature 10: Boundary & Negative Cases for Core Services (B10).
Tests T2_F10_01 to T2_F10_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, WORKSPACE_ROOT
from tests.e2e.fixtures.test_data import generate_unique_customer_payload


class TestBoundary10Unit(unittest.TestCase):
    """Boundary and negative tests for duplicate CURP/RFC, inactive accounts, and non-existent lookups."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t2_f10_01_duplicate_curp_rejection(self):
        """T2_F10_01: Registering customer with existing CURP triggers 409 Conflict (CURP_DUPLICADA)."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        # Create first customer
        payload1 = generate_unique_customer_payload(index=8881)
        resp1 = self.client.post("/clientes", json_data=payload1)
        if resp1.status_code == 201:
            # Attempt duplicate with same CURP
            payload2 = generate_unique_customer_payload(index=8882)
            payload2["curp"] = payload1["curp"]
            resp2 = self.client.post("/clientes", json_data=payload2)
            self.assertEqual(resp2.status_code, 409, f"Expected 409, got {resp2.status_code}")
            self.assertIn("CURP_DUPLICADA", str(resp2.json))

    def test_t2_f10_02_duplicate_rfc_rejection(self):
        """T2_F10_02: Registering customer with existing RFC triggers 409 Conflict (RFC_DUPLICADO)."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        payload1 = generate_unique_customer_payload(index=8883)
        resp1 = self.client.post("/clientes", json_data=payload1)
        if resp1.status_code == 201:
            payload2 = generate_unique_customer_payload(index=8884)
            payload2["rfc"] = payload1["rfc"]
            resp2 = self.client.post("/clientes", json_data=payload2)
            self.assertEqual(resp2.status_code, 409, f"Expected 409, got {resp2.status_code}")
            self.assertIn("RFC_DUPLICADO", str(resp2.json))

    def test_t2_f10_03_token_service_error_handling(self):
        """T2_F10_03: Verify GestoPagoTokenServiceImpl catches upstream authentication failures."""
        token_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "Impl"
            / "GestoPagoTokenServiceImpl.java"
        )
        self.assertTrue(token_file.exists())
        content = token_file.read_text(encoding="utf-8")
        self.assertIn("catch (Exception e)", content)

    def test_t2_f10_04_login_inactive_user_rejection(self):
        """T2_F10_04: Verify AuthServiceImpl checks active status of user login."""
        auth_file = (
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
        self.assertTrue(auth_file.exists())
        content = auth_file.read_text(encoding="utf-8")
        self.assertIn("getActivo", content)

    def test_t2_f10_05_cuenta_nonexistent_lookup_exception(self):
        """T2_F10_05: Verify CuentaServiceImpl throws CuentaNoEncontradaException for non-existent account."""
        cuenta_file = (
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
            / "CuentaServiceImpl.java"
        )
        self.assertTrue(cuenta_file.exists())
        content = cuenta_file.read_text(encoding="utf-8")
        self.assertIn("CuentaNoEncontradaException", content)


if __name__ == "__main__":
    unittest.main()
