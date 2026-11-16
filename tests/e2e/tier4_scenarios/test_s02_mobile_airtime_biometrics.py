"""
Tier 4 - Scenario 02: Mobile Client Airtime Recharge with Biometric Authentication.
Scenario T4_SCEN_02.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_EMAIL, DEFAULT_NUM_CUENTA
from tests.e2e.fixtures.test_data import REFERENCE_EMBEDDING_STR


class TestScenario02MobileAirtimeBiometrics(unittest.TestCase):
    """End-to-end mobile user journey: Biometric Login -> Accounts -> Catalog Cat 1 -> Phone Validation -> Recharge."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")

    def test_t4_scen_02_mobile_airtime_recharge_biometric_journey(self):
        """T4_SCEN_02: Mobile Flutter client airtime recharge using MediaPipe biometric authentication."""
        # 1. Biometric Login
        login_resp = self.client.post(
            "/auth/login",
            json_data={
                "username": DEFAULT_EMAIL,
                "biometricoFacialEmbedding": REFERENCE_EMBEDDING_STR,
            },
        )
        self.assertEqual(login_resp.status_code, 200)
        token = login_resp.json.get("tokenSesion")
        self.assertTrue(token)

        # 2. Fetch Active Accounts
        accts_resp = self.client.get("/cuentas/activas")
        self.assertEqual(accts_resp.status_code, 200)
        num_cuenta = accts_resp.json[0].get("numeroCuenta") if accts_resp.json else DEFAULT_NUM_CUENTA

        # 3. Browse Category 1 (Recharges) Catalog
        cat_resp = self.client.get("/productos")
        self.assertEqual(cat_resp.status_code, 200)
        cat1 = cat_resp.json.get("categorias", {}).get("1", []) or cat_resp.json.get("categorias", {}).get(1, [])
        self.assertTrue(len(cat1) > 0, "Category 1 products must be present")

        # 4. Validate 10-digit Phone
        phone = "5512345678"
        val_payload = {
            "numCuenta": num_cuenta,
            "idServicio": 1,
            "idProducto": 10,
            "telefono": phone,
            "monto": 100.00,
        }
        val_resp = self.client.post("/pagos/validar-referencia", json_data=val_payload)
        self.assertEqual(val_resp.status_code, 200)
        self.assertTrue(val_resp.json.get("valida"))
        self.assertEqual(val_resp.json.get("telefonoNormalizado"), phone)

        # 5. Execute Recharge with Biometric 2FA
        idemp_key = f"IDEMP-SCEN-02-{int(time.time() * 1000)}"
        pay_payload = {
            "numCuenta": num_cuenta,
            "idServicio": 1,
            "idProducto": 10,
            "telefono": phone,
            "monto": 100.00,
            "2fa": REFERENCE_EMBEDDING_STR,
        }
        pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
        self.assertIn(pay_resp.status_code, [200, 202])

        # 6. Verify Scoped History
        hist_resp = self.client.post("/pagos/historial", json_data={"numeroCuenta": num_cuenta})
        self.assertEqual(hist_resp.status_code, 200)


if __name__ == "__main__":
    unittest.main()
