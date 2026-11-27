"""
Tier 2 - Feature 02: Boundary & Corner Cases for Dynamic Catalog & Categories (B02).
Tests T2_F02_01 to T2_F02_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL


class TestBoundary02Catalog(unittest.TestCase):
    """Boundary and negative tests for catalog and nationality endpoints."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend service offline at {BACKEND_URL}")

    def test_t2_f02_01_nationality_invalid_non_positive_id(self):
        """T2_F02_01: Nationality lookup by invalid non-positive ID 0 returns 400 Bad Request."""
        resp = self.client.get("/catalogos/nacionalidades/0")
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f02_02_nationality_nonexistent_id(self):
        """T2_F02_02: Nationality lookup for non-existent ID 99999999 returns 404 Not Found."""
        resp = self.client.get("/catalogos/nacionalidades/99999999")
        self.assertEqual(resp.status_code, 404, f"Expected 404, got {resp.status_code}")

    def test_t2_f02_03_nationality_search_blank_criterion(self):
        """T2_F02_03: Nationality search request with blank criterion string returns 400."""
        resp = self.client.post("/catalogos/nacionalidades/buscar", json_data={"criterio": "   "})
        self.assertEqual(resp.status_code, 400, f"Expected 400, got {resp.status_code}")

    def test_t2_f02_04_nationality_search_unknown_code(self):
        """T2_F02_04: Nationality search request for non-existent country code returns 404."""
        resp = self.client.post("/catalogos/nacionalidades/buscar", json_data={"criterio": "UNKNOWN_ISO_99"})
        self.assertEqual(resp.status_code, 404, f"Expected 404, got {resp.status_code}")

    def test_t2_f02_05_catalog_fallback_origin_resilience(self):
        """T2_F02_05: Catalog retrieval accepts fallback data origins (REDIS, POSTGRESQL, or GESTOPAGO)."""
        resp = self.client.get("/productos")
        self.assertEqual(resp.status_code, 200)
        data = resp.json
        self.assertIn(data.get("origen"), ["REDIS", "POSTGRESQL", "GESTOPAGO"])


if __name__ == "__main__":
    unittest.main()
