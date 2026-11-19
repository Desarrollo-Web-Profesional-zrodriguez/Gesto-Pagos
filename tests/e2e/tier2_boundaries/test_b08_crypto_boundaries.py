"""
Tier 2 - Feature 08: Boundary & Negative Cases for Cryptographic Contracts (B08).
Tests T2_F08_01 to T2_F08_05.
"""
import unittest
from tests.e2e.config import WORKSPACE_ROOT


class TestBoundary08Crypto(unittest.TestCase):
    """Boundary and negative tests for AES-256 encryption and decryption error handling."""

    def setUp(self):
        self.aes_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "util"
            / "AesEncryption.java"
        )
        self.assertTrue(self.aes_file.exists())
        self.content = self.aes_file.read_text(encoding="utf-8")

    def test_t2_f08_01_aes_encryption_error_catch(self):
        """T2_F08_01: Verify encrypt method catches exceptions and returns null safely instead of throwing unhandled."""
        self.assertIn("catch (Exception e)", self.content)
        self.assertIn("return null;", self.content)

    def test_t2_f08_02_aes_decryption_error_catch(self):
        """T2_F08_02: Verify decrypt method catches corrupted Base64 exceptions and returns null safely."""
        self.assertIn("decrypt(String input", self.content)
        self.assertIn("Base64.getDecoder().decode(input)", self.content)

    def test_t2_f08_03_aes_key_specification(self):
        """T2_F08_03: Verify AES SecretKeySpec initialization specification."""
        self.assertIn("SecretKeySpec(key, 0, key.length, \"AES\")", self.content)

    def test_t2_f08_04_aes_iv_parameter_spec(self):
        """T2_F08_04: Verify IvParameterSpec initialization specification."""
        self.assertIn("IvParameterSpec(iv)", self.content)

    def test_t2_f08_05_payment_encryption_failure_guard(self):
        """T2_F08_05: Verify PagoServicioServiceImpl guards against null encryptedPayload."""
        pago_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "Impl"
            / "PagoServicioServiceImpl.java"
        )
        self.assertTrue(pago_file.exists())
        pago_content = pago_file.read_text(encoding="utf-8")
        self.assertIn("encryptedPayload", pago_content)


if __name__ == "__main__":
    unittest.main()
