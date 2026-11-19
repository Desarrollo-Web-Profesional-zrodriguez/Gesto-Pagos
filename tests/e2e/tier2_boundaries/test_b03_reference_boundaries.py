"""
Tier 2 - Feature 03: Boundary & Negative Cases for Reference Validation (B03).
Tests T2_F03_01 to T2_F03_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_NUM_CUENTA


class TestBoundary03Reference(unittest.TestCase):
    """Boundary and negative tests for reference and phone validation."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t2_f03_01_reference_illegal_special_characters(self):
        """T2_F03_01: Reference string containing illegal characters returns 400 Bad Request."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "REF#123!",
            "monto": 150.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f03_02_reference_all_zeros_rejection(self):
        """T2_F03_02: Reference composed solely of zeros returns 400 (GESTOPAGO_55_REFERENCIA_INVALIDA)."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "000000",
            "monto": 150.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")
        self.assertIn("REFERENCIA_INVALIDA", str(resp.json))

    def test_t2_f03_03_phone_invalid_length_recharge(self):
        """T2_F03_03: Telephone number for airtime recharge with invalid length (9 digits) returns 400."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 1,
            "idProducto": 10,
            "telefono": "551234567",  # 9 digits
            "monto": 100.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f03_04_nonexistent_service_or_product(self):
        """T2_F03_04: Reference validation on non-existent idServicio or idProducto returns 404 Not Found."""
        payload = {
            "numCuenta": DEFAULT_NUM_CUENTA,
            "idServicio": 99999,
            "idProducto": 88888,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 404, f"Expected 404, got {resp.status_code}")

    def test_t2_f03_05_malformed_account_number(self):
        """T2_F03_05: Reference validation with malformed account number (< 16 digits) returns 400."""
        payload = {
            "numCuenta": "12345678",  # not 16 digits
            "idServicio": 76,
            "idProducto": 205,
            "referencia": "2572038101",
            "monto": 150.00,
        }
        resp = self.client.post("/pagos/validar-referencia", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")


if __name__ == "__main__":
    unittest.main()
