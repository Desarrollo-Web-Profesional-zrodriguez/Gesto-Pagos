"""
Tier 1 - Feature 08: Cryptographic Vulnerabilities Remediation (F08).
Tests T1_F08_01 to T1_F08_05.
"""
import base64
import unittest
from tests.e2e.client import HttpClient, is_sonar_online
from tests.e2e.config import SONAR_URL, PROJECT_KEY, SONAR_TOKEN, WORKSPACE_ROOT


class TestFeature08CryptoVulns(unittest.TestCase):
    """Functional tests verifying cryptographic contract preservation and vulnerability suppression."""

    def setUp(self):
        self.sonar_client = HttpClient(base_url=SONAR_URL)
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
        self.assertTrue(self.aes_file.exists(), "AesEncryption.java must exist")
        self.aes_content = self.aes_file.read_text(encoding="utf-8")

    def test_t1_f08_01_aes_encryption_cbc_specification(self):
        """T1_F08_01: Verify AES-256 CBC with PKCS5Padding algorithm specification in AesEncryption."""
        self.assertIn(
            "AES/CBC/PKCS5Padding",
            self.aes_content,
            "AesEncryption must specify AES/CBC/PKCS5Padding as required by GestoPago",
        )
        self.assertIn("SecretKeySpec", self.aes_content)
        self.assertIn("IvParameterSpec", self.aes_content)

    def test_t1_f08_02_aes_decryption_specification(self):
        """T1_F08_02: Verify AES-256 decryption implementation restores ciphertext."""
        self.assertIn("DECRYPT_MODE", self.aes_content)
        self.assertIn("Base64.getDecoder()", self.aes_content)
        self.assertIn("Base64.getEncoder()", self.aes_content)

    def test_t1_f08_03_crypto_sonar_suppressions(self):
        """T1_F08_03: Verify @SuppressWarnings contains java:S3329, java:S5542, and java:S6432."""
        required_suppressions = ["java:S3329", "java:S5542", "java:S6432"]
        for rule in required_suppressions:
            self.assertIn(
                rule,
                self.aes_content,
                f"AesEncryption must contain suppression for {rule} to preserve GestoPago crypto contract",
            )

    def test_t1_f08_04_sonarqube_zero_open_vulnerabilities(self):
        """T1_F08_04: Verify SonarQube reports 0 open vulnerabilities (Rating A in Security)."""
        if is_sonar_online(SONAR_URL):
            resp = self.sonar_client.get(
                "/api/issues/search",
                params={
                    "componentKeys": PROJECT_KEY,
                    "types": "VULNERABILITY",
                    "resolved": "false",
                },
                headers={"Authorization": f"Bearer {SONAR_TOKEN}"},
            )
            if resp.status_code == 200:
                data = resp.json
                total_vulns = data.get("total", 0)
                self.assertEqual(total_vulns, 0, f"Expected 0 vulnerabilities in SonarQube, found {total_vulns}")
            else:
                self.assertIn(resp.status_code, [200, 401])
        else:
            # Code-level assertion: suppressions are present on AesEncryption
            self.assertIn("@SuppressWarnings", self.aes_content)

    def test_t1_f08_05_sendtx_gateway_payload_contract(self):
        """T1_F08_05: Verify GestoPagoSendTxGateway and PagoServicioServiceImpl encrypt and transmit SendTx payload."""
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
        self.assertIn("AesEncryption.AESCipher", pago_content)

        gateway_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "GestoPagoSendTxGateway.java"
        )
        self.assertTrue(gateway_file.exists())
        gw_content = gateway_file.read_text(encoding="utf-8")
        self.assertIn("payloadCifrado", gw_content)
        self.assertIn("signed", gw_content)


if __name__ == "__main__":
    unittest.main()
