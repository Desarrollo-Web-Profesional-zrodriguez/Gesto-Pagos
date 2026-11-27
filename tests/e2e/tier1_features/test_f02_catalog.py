"""
Tier 1 - Feature 02: Dynamic Catalog & Category Labels (F02).
Tests T1_F02_01 to T1_F02_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL


class TestFeature02Catalog(unittest.TestCase):
    """Functional tests for dynamic catalog, category labels, logos, and nationalities."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t1_f02_01_catalog_categories(self):
        """T1_F02_01: Retrieve full categorized product catalog with Redis or PostgreSQL origin."""
        resp = self.client.get("/productos")
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("status"), 200)
        self.assertIn("categorias", data)
        self.assertIn(data.get("origen"), ["REDIS", "POSTGRESQL", "GESTOPAGO"])
        categorias = data.get("categorias", {})
        self.assertTrue(len(categorias) >= 1, "Should contain categories")
        self.assertTrue(data.get("totalProductos", 0) > 0, "totalProductos should be > 0")

    def test_t1_f02_02_category1_airtime_metadata(self):
        """T1_F02_02: Verify category 1 items contain airtime/recarga metadata, logos, and phone flags."""
        resp = self.client.get("/productos")
        self.assertEqual(resp.status_code, 200)
        data = resp.json
        cat1_items = data.get("categorias", {}).get("1", [])
        if not cat1_items:
            cat1_items = data.get("categorias", {}).get(1, [])
        self.assertTrue(len(cat1_items) > 0, "Category 1 should have products")
        first_item = cat1_items[0]
        self.assertEqual(first_item.get("tipoFront"), 1)
        self.assertTrue(first_item.get("servicio"), "Servicio name should be present")
        self.assertIn("urlLogo", first_item, "urlLogo field should be present")

    def test_t1_f02_03_category2_services_metadata(self):
        """T1_F02_03: Verify category 2 items contain utility service metadata, reference rules, and legends."""
        resp = self.client.get("/productos")
        self.assertEqual(resp.status_code, 200)
        data = resp.json
        cat2_items = data.get("categorias", {}).get("2", [])
        if not cat2_items:
            cat2_items = data.get("categorias", {}).get(2, [])
        self.assertTrue(len(cat2_items) > 0, "Category 2 should have products")
        first_item = cat2_items[0]
        self.assertEqual(first_item.get("tipoFront"), 2)
        self.assertIn("hasDigitoVerificador", first_item)
        self.assertIn("legend", first_item)

    def test_t1_f02_04_nationalities_list(self):
        """T1_F02_04: List system nationalities catalog for onboarding dropdowns."""
        resp = self.client.get("/catalogos/nacionalidades")
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertIsInstance(data, list)
        self.assertTrue(len(data) > 0, "Nationalities catalog should not be empty")
        # Check for Mexico
        mexico = next((n for n in data if n.get("claveIso") == "MEX"), None)
        self.assertIsNotNone(mexico, "Mexico (MEX) nationality must be in catalog")
        self.assertEqual(mexico.get("pais"), "Mexico")

    def test_t1_f02_05_nationalities_search(self):
        """T1_F02_05: Search nationality catalog by ISO code criteria."""
        resp = self.client.post("/catalogos/nacionalidades/buscar", json_data={"criterio": "MEX"})
        self.assertEqual(resp.status_code, 200, f"Expected 200, got {resp.status_code}: {resp.text}")
        data = resp.json
        self.assertEqual(data.get("claveIso"), "MEX")
        self.assertEqual(data.get("pais"), "Mexico")


if __name__ == "__main__":
    unittest.main()
