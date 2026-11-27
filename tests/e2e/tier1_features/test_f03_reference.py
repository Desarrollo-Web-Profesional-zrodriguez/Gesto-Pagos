"""
Tier 1 - Feature 03: Reference Validation Enforcement (F03).
Tests T1_F03_01 to T1_F03_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_NUM_CUENTA


class TestFeature03Reference(unittest.TestCase):
    """Functional tests for pre-payment reference and phone validation rules."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t1_f03_01_utility_reference_validation(self):
        """T1_F03_01: Validate valid utility service reference (tipoFront=2, CFE) with sufficient balance."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("valida"), "Reference should be validated as true")
        self.assertIn("saldoSuficiente", data)
        self.assertEqual(float(data.get("montoValidado", 0)), 150.00)
        self.assertEqual(data.get("telefonoNormalizado"), "1111111111")

    def test_t1_f03_02_airtime_phone_validation(self):
        """T1_F03_02: Validate mobile telephone recharge (tipoFront=1, Telcel) with 10-digit number."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 1,
            "idProducto": 10,
            "telefono": "5512345678",
            "monto": 100.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("valida"))
        self.assertEqual(data.get("telefonoNormalizado"), "5512345678")
        self.assertEqual(data.get("referenciaNormalizada"), "5512345678")

    def test_t1_f03_03_fixed_price_product_amount_inheritance(self):
        """T1_F03_03: Validate fixed-price product without explicit amount in payload; inherits catalog price."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 1,
            "idProducto": 10,
            "telefono": "5512345678",
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("valida"))
        self.assertTrue(float(data.get("montoValidado", 0)) > 0, "Amount should be inherited from catalog")

    def test_t1_f03_04_gift_card_entertainment_default_phone(self):
        """T1_F03_04: Validate entertainment gift card service with default phone #1111111111."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 50,
            "idProducto": 120,
            "monto": 250.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("valida"))
        self.assertEqual(data.get("telefonoNormalizado"), "#1111111111")

    def test_t1_f03_05_aid_receipt_image_url(self):
        """T1_F03_05: Validate reference returning aid image URL (urlReferenciaAyuda) when available."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "2572038101",
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertTrue(data.get("valida"))
        self.assertIn("urlReferenciaAyuda", data, "Should return urlReferenciaAyuda field")


if __name__ == "__main__":
    unittest.main()
