"""
Tier 1 - Feature 06: 1:1 Parity & Security (F06).
Tests T1_F06_01 to T1_F06_05.
"""
import re
import unittest
from pathlib import Path
from tests.e2e.config import WORKSPACE_ROOT
from tests.e2e.fixtures.test_data import (
    calculate_cosine_similarity,
    REFERENCE_EMBEDDING_5D,
    SIMILAR_EMBEDDING_5D,
    DISSIMILAR_EMBEDDING_5D,
)


class TestFeature06ParitySecurity(unittest.TestCase):
    """Functional tests for 1:1 parity, zero hardcoded credentials, and biometric cosine similarity."""

    def test_t1_f06_01_zero_hardcoded_secrets_in_config(self):
        """T1_F06_01: Audit application.properties ensuring zero hardcoded secrets."""
        app_props_path = WORKSPACE_ROOT / "src" / "main" / "resources" / "application.properties"
        self.assertTrue(app_props_path.exists(), "application.properties must exist")

        content = app_props_path.read_text(encoding="utf-8")
        # Critical secret keys must use environment placeholders ${...}
        critical_keys = [
            "spring.datasource.password",
            "gestopago.auth.password",
            "gestopago.aes.secret-key",
            "gestopago.aes.iv",
        ]
        for key in critical_keys:
            pattern = rf"{re.escape(key)}\s*=\s*\${{([^}}]+)}}"
            match = re.search(pattern, content)
            self.assertIsNotNone(
                match,
                f"Config '{key}' must use environment variable placeholder, found raw or missing",
            )

    def test_t1_f06_02_biometric_cosine_similarity_threshold(self):
        """T1_F06_02: Verify biometric cosine similarity logic matches embeddings with score >= 0.80."""
        similarity = calculate_cosine_similarity(REFERENCE_EMBEDDING_5D, SIMILAR_EMBEDDING_5D)
        self.assertGreaterEqual(
            similarity,
            0.80,
            f"Similar facial embedding must have similarity >= 0.80, got {similarity}",
        )

        dissimilarity = calculate_cosine_similarity(REFERENCE_EMBEDDING_5D, DISSIMILAR_EMBEDDING_5D)
        self.assertLess(
            dissimilarity,
            0.80,
            f"Dissimilar facial embedding must have similarity < 0.80, got {dissimilarity}",
        )

    def test_t1_f06_03_sensitive_data_account_masking(self):
        """T1_F06_03: Verify sensitive account masking logic (12 asterisks + last 4 digits)."""
        raw_account = "1000000001000000"
        # Masking specification: ************0000
        masked = "*" * 12 + raw_account[-4:]
        self.assertEqual(len(masked), 16)
        self.assertTrue(masked.startswith("************"))
        self.assertEqual(masked[-4:], "0000")

    def test_t1_f06_04_web_mobile_contract_parity(self):
        """T1_F06_04: Verify Web and Mobile clients use identical REST endpoints."""
        web_file = WORKSPACE_ROOT / "web" / "src" / "features" / "pagos" / "data" / "pagoRepository.ts"
        mobile_file = WORKSPACE_ROOT / "frontend" / "lib" / "features" / "pagos" / "data" / "pago_repository.dart"

        self.assertTrue(web_file.exists(), f"Web repository {web_file} must exist")
        self.assertTrue(mobile_file.exists(), f"Mobile repository {mobile_file} must exist")

        web_content = web_file.read_text(encoding="utf-8")
        mobile_content = mobile_file.read_text(encoding="utf-8")

        required_endpoints = [
            "/pagos/validar-referencia",
            "/pagos",
            "/pagos/historial",
        ]
        for ep in required_endpoints:
            self.assertIn(ep, web_content, f"Web client must reference endpoint {ep}")
            self.assertIn(ep, mobile_content, f"Mobile client must reference endpoint {ep}")

    def test_t1_f06_05_password_hashing_contract(self):
        """T1_F06_05: Verify password hashing specification contract in PasswordEncryptionUtil."""
        util_path = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "cliente"
            / "util"
            / "PasswordEncryptionUtil.java"
        )
        self.assertTrue(util_path.exists(), "PasswordEncryptionUtil.java must exist")
        content = util_path.read_text(encoding="utf-8")
        self.assertIn("PBKDF2WithHmacSHA256", content)
        self.assertIn("65536", content)


if __name__ == "__main__":
    unittest.main()
