"""
Tier 4 - Scenario 01: Complete Customer Onboarding, Login & First Utility Bill Payment.
Scenario T4_SCEN_01.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD
from tests.e2e.fixtures.test_data import generate_unique_customer_payload


class TestScenario01OnboardingPayment(unittest.TestCase):
    """End-to-end user workflow: Onboarding -> Login -> Balance -> Catalog -> Pre-validation -> Payment -> Ledger -> Receipt -> Logout."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t4_scen_01_full_onboarding_and_utility_payment_journey(self):
        """T4_SCEN_01: Complete journey from customer registration to utility payment and receipt."""
        # 1. Customer Registration
        payload = generate_unique_customer_payload()
        reg_resp = self.client.post("/clientes", json_data=payload)
        self.assertIn(reg_resp.status_code, [200, 201], f"Registration failed: {reg_resp.text}")
        customer_data = reg_resp.json
        email = payload["correoElectronico"]
        cuentas = customer_data.get("cuentas", [])
        num_cuenta = cuentas[0].get("numeroCuenta") if cuentas else None

        # 2. Login
        login_resp = self.client.post(
            "/auth/login",
            json_data={"username": email, "password": DEFAULT_PASSWORD},
        )
        self.assertEqual(login_resp.status_code, 200, f"Login failed: {login_resp.text}")
        token = login_resp.json.get("tokenSesion")
        self.assertTrue(token)

        # 3. Account Balance Check
        if num_cuenta:
            bal_resp = self.client.get(f"/cuentas/{num_cuenta}/saldo")
            self.assertEqual(bal_resp.status_code, 200)
            self.assertGreaterEqual(float(bal_resp.json.get("saldoDisponible", 0)), 1000.0)

        # 4. Catalog Browsing
        cat_resp = self.client.get("/productos")
        self.assertEqual(cat_resp.status_code, 200)

        target_account = num_cuenta or "1000000001000000"

        # 5. Reference Pre-Validation
        val_payload = {
            "numCuenta": target_account,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        val_resp = self.client.post("/pagos/validar-referencia", json_data=val_payload)
        self.assertEqual(val_resp.status_code, 200)
        self.assertTrue(val_resp.json.get("valida"))

        # 6. Payment Execution
        idemp_key = f"IDEMP-SCEN-01-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": target_account,
            "idServicio": 76,
            "idProducto": 205,
            "monto": 150.00,
            "referencia": "2572038101",
            "2fa": token,
            "correoElectronico": email,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])

        # 7. Audit History
        hist_resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": target_account})
        self.assertEqual(hist_resp.status_code, 200)
        self.assertIsInstance(hist_resp.json, list)

        # 8. Receipt Dispatch
        receipt_payload = {
            "destinatarioEmail": email,
            "destinatarioNombre": payload["nombre"],
            "numAutorizacion": "100074211",
            "servicio": "CFE",
            "montoPagado": 150.00,
            "referencia": "2572038101",
        }
        rec_resp = self.client.post("/pagos/enviar-comprobante", json_data=receipt_payload)
        self.assertEqual(rec_resp.status_code, 200)

        # 9. Logout
        logout_resp = self.client.post("/auth/logout", headers={"Authorization": f"Bearer {token}"})
        self.assertEqual(logout_resp.status_code, 200)


if __name__ == "__main__":
    unittest.main()
