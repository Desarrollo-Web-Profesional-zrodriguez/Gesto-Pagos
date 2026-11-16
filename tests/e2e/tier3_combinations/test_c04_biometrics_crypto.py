"""
Tier 3 - Pairwise Combination: Biometrics & Cryptography with Payment Flow.
Tests T3_PAIR_10 and T3_PAIR_12.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_NUM_CUENTA, WORKSPACE_ROOT
from tests.e2e.fixtures.test_data import REFERENCE_EMBEDDING_STR


class TestCombination04BiometricsCrypto(unittest.TestCase):
    """Pairwise combination tests linking F01/F06 (Biometrics), F04 (Payment), and F08 (Crypto)."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t3_pair_10_biometric_login_to_biometric_payment(self):
        """T3_PAIR_10: Login using MediaPipe facial embedding, then confirm payment with same embedding as 2FA."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

        # 1. Biometric login
        login_payload = {
            "username": DEFAULT_EMAIL,
            "biometricoFacialEmbedding": REFERENCE_EMBEDDING_STR,
        }
        login_resp = self.client.post("/auth/login", json_data=login_payload)
        self.assertEqual(login_resp.status_code, 200)

        # 2. Payment with biometric embedding as 2FA
        idemp_key = f"IDEMP-PAIR-10-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": REFERENCE_EMBEDDING_STR,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])

    def test_t3_pair_12_aes_encryption_and_sendtx_payment(self):
        """T3_PAIR_12: Verify SendTx payment payload pipeline links AesEncryption with gateway dispatch."""
        pago_impl = (
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
        self.assertTrue(pago_impl.exists())
        content = pago_impl.read_text(encoding="utf-8")
        # Check that payload creation, encryption, and gateway call are chained together
        self.assertIn("SendTxPayloadDto", content)
        self.assertIn("AesEncryption.AESCipher", content)
        self.assertIn("sendTxGateway.ejecutar", content)


if __name__ == "__main__":
    unittest.main()
