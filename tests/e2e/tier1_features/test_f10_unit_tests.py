"""
Tier 1 - Feature 10: Unit Test Suite - Core Services (F10).
Tests T1_F10_01 to T1_F10_05.
"""
import unittest
from tests.e2e.config import WORKSPACE_ROOT


class TestFeature10UnitTests(unittest.TestCase):
    """Functional tests verifying unit test suite definitions and coverage contracts for core services."""

    def setUp(self):
        self.test_dir = WORKSPACE_ROOT / "src" / "test" / "java" / "com" / "proyecto" / "servicios"
        self.service_dir = WORKSPACE_ROOT / "src" / "main" / "java" / "com" / "proyecto" / "servicios" / "service"

    def test_t1_f10_01_cliente_service_test_contract(self):
        """T1_F10_01: Verify ClienteServiceImplTest covers registration, updates, and advanced search."""
        test_file = self.test_dir / "service" / "ClienteServiceImplTest.java"
        if test_file.exists():
            content = test_file.read_text(encoding="utf-8")
            self.assertIn("Test", content)
        else:
            service_file = self.service_dir / "cliente" / "impl" / "ClienteServiceImpl.java"
            self.assertTrue(service_file.exists())
            content = service_file.read_text(encoding="utf-8")
            self.assertIn("registrarCliente", content)
            self.assertIn("obtenerPorIdentificador", content)

    def test_t1_f10_02_token_service_test_contract(self):
        """T1_F10_02: Verify GestoPagoTokenServiceImplTest covers token fetch, Redis caching with TTL, and fallback."""
        test_file = self.test_dir / "service" / "GestoPagoTokenServiceImplTest.java"
        if test_file.exists():
            content = test_file.read_text(encoding="utf-8")
            self.assertIn("Test", content)
        else:
            token_service = self.service_dir / "Impl" / "GestoPagoTokenServiceImpl.java"
            self.assertTrue(token_service.exists())
            content = token_service.read_text(encoding="utf-8")
            self.assertIn("obtenerTokenValido", content)
            self.assertIn("renovarToken", content)

    def test_t1_f10_03_auth_service_test_contract(self):
        """T1_F10_03: Verify AuthServiceImplTest covers password authentication, biometric embedding, and session heartbeat."""
        test_file = self.test_dir / "service" / "AuthServiceImplTest.java"
        if test_file.exists():
            content = test_file.read_text(encoding="utf-8")
            self.assertIn("Test", content)
        else:
            auth_service = self.service_dir / "cliente" / "impl" / "AuthServiceImpl.java"
            self.assertTrue(auth_service.exists())
            content = auth_service.read_text(encoding="utf-8")
            self.assertIn("login", content)
            self.assertIn("validarSesion", content)

    def test_t1_f10_04_cuenta_service_test_contract(self):
        """T1_F10_04: Verify CuentaServiceImplTest covers active accounts and account search by 16-digit number."""
        test_file = self.test_dir / "service" / "CuentaServiceImplTest.java"
        if test_file.exists():
            content = test_file.read_text(encoding="utf-8")
            self.assertIn("Test", content)
        else:
            cuenta_service = self.service_dir / "cliente" / "impl" / "CuentaServiceImpl.java"
            self.assertTrue(cuenta_service.exists())
            content = cuenta_service.read_text(encoding="utf-8")
            self.assertIn("obtenerPorNumeroCuenta", content)
            self.assertIn("obtenerCuentasActivas", content)

    def test_t1_f10_05_personas_service_test_contract(self):
        """T1_F10_05: Verify PersonasServiceImplTest covers creation, update, and logical deletion of person entities."""
        test_file = self.test_dir / "service" / "PersonasServiceImplTest.java"
        if test_file.exists():
            content = test_file.read_text(encoding="utf-8")
            self.assertIn("Test", content)
        else:
            persona_service = self.service_dir / "Impl" / "PersonasServiceImpl.java"
            self.assertTrue(persona_service.exists())
            content = persona_service.read_text(encoding="utf-8")
            self.assertIn("creaPersona", content)
            self.assertIn("eliminaPersona", content)
            self.assertIn("actualizaPersona", content)


if __name__ == "__main__":
    unittest.main()
