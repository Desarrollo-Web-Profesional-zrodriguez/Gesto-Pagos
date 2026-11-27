"""
Tier 1 - Feature 05: Transaction History & Multi-Account Parity (F05).
Tests T1_F05_01 to T1_F05_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_NUM_CUENTA


class TestFeature05History(unittest.TestCase):
    """Functional tests for user transaction history, email receipts, and multi-account queries."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t1_f05_01_account_scoped_transaction_history(self):
        """T1_F05_01: Retrieve transaction history scoped strictly to the requesting 16-digit account."""
        resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": DEFAULT_NUM_CUENTA})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertIsInstance(data, list, "History should return a list")
        for tx in data:
            self.assertEqual(
                str(tx.get("numeroCuenta")),
                DEFAULT_NUM_CUENTA,
                "All history items must belong to the queried account",
            )

    def test_t1_f05_02_resend_email_receipt(self):
        """T1_F05_02: Re-send digital transaction receipt to customer email via Brevo client."""
        payload = {
            "destinatarioEmail": "cliente.test@example.com",
            "destinatarioNombre": "Juan Perez",
            "numAutorizacion": "100074211",
            "servicio": "CFE",
            "montoPagado": 150.00,
            "referencia": "2572038101",
        }
        resp = self.client.post("/pagos/enviar-comprobante", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertIn("enviado", data, "Response should indicate receipt delivery status")

    def test_t1_f05_03_list_active_accounts(self):
        """T1_F05_03: List all active bank accounts for multi-account selector."""
        resp = self.client.get("/cuentas/activas")
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertIsInstance(data, list)
        self.assertTrue(len(data) > 0, "Active accounts list should not be empty")
        for acct in data:
            self.assertEqual(acct.get("estatus"), "ACTIVA")
            self.assertEqual(len(str(acct.get("numeroCuenta"))), 16)

    def test_t1_f05_04_query_account_by_number(self):
        """T1_F05_04: Query individual account details by 16-digit account number."""
        resp = self.client.get(f"/cuentas/{DEFAULT_NUM_CUENTA}")
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(str(data.get("numeroCuenta")), DEFAULT_NUM_CUENTA)
        self.assertEqual(data.get("estatus"), "ACTIVA")
        self.assertTrue(data.get("nombreTitular"))

    def test_t1_f05_05_query_account_balance(self):
        """T1_F05_05: Query real-time available and ledger balance for an account."""
        resp = self.client.get(f"/cuentas/{DEFAULT_NUM_CUENTA}/saldo")
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(str(data.get("numeroCuenta")), DEFAULT_NUM_CUENTA)
        self.assertTrue("saldoDisponible" in data)
        self.assertTrue("saldoContable" in data)
        self.assertTrue(float(data.get("saldoDisponible")) >= 0)


if __name__ == "__main__":
    unittest.main()
