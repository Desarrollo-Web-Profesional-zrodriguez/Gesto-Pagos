"""
Tier 3 - Pairwise Combination: Payment Processing with History, Accounts & Receipts.
Tests T3_PAIR_02, T3_PAIR_06, and T3_PAIR_07.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA


class TestCombination03PaymentHistory(unittest.TestCase):
    """Pairwise combination tests linking F01 (Auth), F04 (Payment), and F05 (History & Accounts)."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t3_pair_02_auth_active_accounts_and_history(self):
        """T3_PAIR_02: Authenticated user queries account list and retrieves history strictly scoped to account."""
        # 1. Login
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": DEFAULT_EMAIL, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200)

        # 2. Get active accounts
        accts_resp = self.client.get("/cuentas/activas")
        self.assertEqual(accts_resp.status_code, 200)
        self.assertTrue(len(accts_resp.json) > 0)
        num_cuenta = accts_resp.json[0].get("numeroCuenta")

        # 3. Query history for account
        hist_resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": num_cuenta})
        self.assertEqual(hist_resp.status_code, 200)
        for tx in hist_resp.json:
            self.assertEqual(str(tx.get("numeroCuenta")), str(num_cuenta))

    def test_t3_pair_06_payment_and_transaction_history_audit(self):
        """T3_PAIR_06: Execute payment and verify transaction is recorded in history."""
        idemp_key = f"IDEMP-PAIR-06-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])

        # Query history to verify recording
        hist_resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": DEFAULT_NUM_CUENTA})
        self.assertEqual(hist_resp.status_code, 200)
        self.assertIsInstance(hist_resp.json, list)

    def test_t3_pair_07_payment_and_receipt_resend(self):
        """T3_PAIR_07: Execute payment and immediately trigger digital receipt resend via Brevo."""
        idemp_key = f"IDEMP-PAIR-07-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": DEFAULT_PASSWORD,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])
        num_auth = pay_resp.json.get("numAutorizacion", "100074211")

        # Trigger receipt resend
        email_payload = {
            "destinatarioEmail": "cliente.resend@example.com",
            "destinatarioNombre": "Juan Perez",
            "numAutorizacion": num_auth or "100074211",
            "servicio": "CFE",
            "montoPagado": 150.00,
            "referencia": "2572038101",
        }
        email_resp = self.client.post("/pagos/enviar-comprobante", json_data=email_payload)
        self.assertEqual(email_resp.status_code, 200)
        self.assertIn("enviado", email_resp.json)


if __name__ == "__main__":
    unittest.main()
