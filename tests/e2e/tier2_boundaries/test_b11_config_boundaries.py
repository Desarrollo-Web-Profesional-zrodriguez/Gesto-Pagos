"""
Tier 2 - Feature 11: Boundary & Negative Cases for Config & Deserializers (B11).
Tests T2_F11_01 to T2_F11_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, WORKSPACE_ROOT, DEFAULT_NUM_CUENTA
from tests.e2e.fixtures.test_data import generate_unique_customer_payload


class TestBoundary11Config(unittest.TestCase):
    """Boundary and negative tests for strict number and date deserializers."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t2_f11_01_strict_deserializer_rejects_integer_no_decimals(self):
        """T2_F11_01: StrictTwoDecimalNumberDeserializer rejects amount without decimals (e.g. 100) returning 400."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        # Raw JSON with integer 100
        raw_json = f'{{"numCuenta":"{DEFAULT_NUM_CUENTA}","idServicio":76,"idProducto":205,"referencia":"2572038101","monto":100}}'
        resp = self.client.post("/pagos/validar-referencia", data=raw_json, headers={"Content-Type": "application/json"})
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}: {resp.text}")
        self.assertIn("exactamente dos decimales", resp.text.lower())

    def test_t2_f11_02_strict_deserializer_rejects_three_decimals(self):
        """T2_F11_02: StrictTwoDecimalNumberDeserializer rejects amount with 3 decimals (e.g. 100.555) returning 400."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        raw_json = f'{{"numCuenta":"{DEFAULT_NUM_CUENTA}","idServicio":76,"idProducto":205,"referencia":"2572038101","monto":100.555}}'
        resp = self.client.post("/pagos/validar-referencia", data=raw_json, headers={"Content-Type": "application/json"})
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}: {resp.text}")

    def test_t2_f11_03_flexible_date_deserializer_rejects_slashes(self):
        """T2_F11_03: FlexibleLocalDateDeserializer rejects date strings with slashes (e.g. 20/05/1992) returning 400."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        payload = generate_unique_customer_payload(index=7771)
        payload["fechaNacimiento"] = "20/05/1992"
        resp = self.client.post("/clientes", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")
        self.assertIn("aaaa-mm-dd", resp.text.lower())

    def test_t2_f11_04_flexible_date_deserializer_empty_string_handling(self):
        """T2_F11_04: Verify FlexibleLocalDateDeserializer handles empty string safely."""
        deser_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "config"
            / "FlexibleLocalDateDeserializer.java"
        )
        self.assertTrue(deser_file.exists())
        content = deser_file.read_text(encoding="utf-8")
        self.assertIn("text.trim().isEmpty()", content)
        self.assertIn("return null;", content)

    def test_t2_f11_05_config_db_datasource_contract(self):
        """T2_F11_05: Verify ConfigDB defines DataSource configuration."""
        config_db = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "config"
            / "ConfigDB.java"
        )
        self.assertTrue(config_db.exists())
        content = config_db.read_text(encoding="utf-8")
        self.assertIn("datasource", content.lower())


if __name__ == "__main__":
    unittest.main()
