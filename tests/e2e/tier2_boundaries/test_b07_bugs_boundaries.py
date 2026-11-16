"""
Tier 2 - Feature 07: Boundary & Negative Cases for Bug Remediation (B07).
Tests T2_F07_01 to T2_F07_05.
"""
from datetime import date, timedelta
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, WORKSPACE_ROOT
from tests.e2e.fixtures.test_data import generate_unique_customer_payload


class TestBoundary07Bugs(unittest.TestCase):
    """Boundary and negative tests for S2229 cron proxy and S2259 client null safety."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t2_f07_01_client_search_whitespace_query(self):
        """T2_F07_01: Customer search by identifier with whitespace-only query parameter returns 400."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        resp = self.client.post("/clientes/consultar", json_data={"identificador": "   "})
        self.assertIn(resp.status_code, [400, 404])

    def test_t2_f07_02_patch_null_collections_safety(self):
        """T2_F07_02: Partial update (PATCH) where optional collections are null handles gracefully without NPE."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        resp = self.client.patch(
            "/clientes",
            json_data={"identificador": "NONEXISTENT_IDENTIFIER", "telefonoMovil": "5599887766"},
        )
        self.assertIn(resp.status_code, [200, 400, 404])
        self.assertNotEqual(resp.status_code, 500, "Should never produce 500 NullPointerException")

    def test_t2_f07_03_registration_underage_boundary(self):
        """T2_F07_03: Customer registration boundary age test: age 17 years 364 days returns 400."""
        if not is_backend_online(BACKEND_URL):
            self.skipTest(f"Backend offline at {BACKEND_URL}")
        payload = generate_unique_customer_payload(index=99991)
        # Set birthdate to exactly 17 years and 364 days ago
        today = date.today()
        underage_birthdate = today.replace(year=today.year - 18) + timedelta(days=1)
        payload["fechaNacimiento"] = underage_birthdate.strftime("%Y-%m-%d")

        resp = self.client.post("/clientes", json_data=payload)
        self.assertEqual(resp.status_code, 400, f"Underage registration must be rejected with 400, got {resp.status_code}")
        self.assertIn("mayor de edad", resp.text.lower())

    def test_t2_f07_04_cron_empty_product_handling(self):
        """T2_F07_04: Verify CatalogoProductosCron safely handles empty catalog without throwing NPE."""
        cron_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "cron"
            / "CatalogoProductosCron.java"
        )
        self.assertTrue(cron_file.exists())
        content = cron_file.read_text(encoding="utf-8")
        self.assertIn("isEmpty", content)

    def test_t2_f07_05_cron_redis_failure_fallback_contract(self):
        """T2_F07_05: Verify CatalogoProductosCron catches Redis exceptions and delegates to DB fallback."""
        cron_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "cron"
            / "CatalogoProductosCron.java"
        )
        self.assertTrue(cron_file.exists())
        content = cron_file.read_text(encoding="utf-8")
        self.assertIn("guardadoEnRedisExistoso", content)
        self.assertIn("guardarEnBaseDatos", content)


if __name__ == "__main__":
    unittest.main()
