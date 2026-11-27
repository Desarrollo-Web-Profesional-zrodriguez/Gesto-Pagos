"""
Tier 2 - Feature 05: Boundary & Negative Cases for History & Accounts (B05).
Tests T2_F05_01 to T2_F05_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL


class TestBoundary05History(unittest.TestCase):
    """Boundary and negative tests for history, receipts, and accounts."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t2_f05_01_history_nonexistent_account(self):
        """T2_F05_01: Transaction history lookup for non-existent 16-digit account returns 404 or empty list."""
        resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": "9999999999999999"})
        self.assertIn(resp.status_code, [200, 404], f"Expected 200 or 404, got {resp.status_code}")
        if resp.status_code == 200:
            self.assertEqual(len(resp.json), 0, "Nonexistent account should have 0 transactions")

    def test_t2_f05_02_history_malformed_account(self):
        """T2_F05_02: Transaction history request with invalid account format (< 16 digits) returns 400."""
        resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": "12345"})
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f05_03_resend_receipt_malformed_email(self):
        """T2_F05_03: Re-send receipt request with invalid destination email format returns 400."""
        payload = {
            "destinatarioEmail": "bad-email-address",
            "numAutorizacion": "100074211",
        }
        resp = self.client.post("/pagos/enviar-comprobante", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f05_04_query_upc_invalid_syntax(self):
        """T2_F05_04: Status query by UPC with invalid syntax (not 10 alphanumeric chars) returns 400."""
        resp = self.client.get("/pagos/estatus/UPC#123")
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f05_05_balance_malformed_account_path(self):
        """T2_F05_05: Balance query for malformed account path variable returns 400 Bad Request."""
        resp = self.client.get("/cuentas/INVALID_ACCT/saldo")
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")


if __name__ == "__main__":
    unittest.main()
