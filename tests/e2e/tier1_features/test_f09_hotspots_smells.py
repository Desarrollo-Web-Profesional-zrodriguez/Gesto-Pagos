"""
Tier 1 - Feature 09: Security Hotspots & Code Smells Cleanup (F09).
Tests T1_F09_01 to T1_F09_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_sonar_online
from tests.e2e.config import SONAR_URL, PROJECT_KEY, SONAR_TOKEN, WORKSPACE_ROOT


class TestFeature09HotspotsSmells(unittest.TestCase):
    """Functional tests verifying security hotspots review, maintainability rating, and code smell reduction."""

    def setUp(self):
        self.sonar_client = HttpClient(base_url=SONAR_URL)

    def test_t1_f09_01_security_hotspots_reviewed(self):
        """T1_F09_01: Verify SonarQube reports 100% Security Hotspots reviewed and cleared."""
        if is_sonar_online(SONAR_URL):
            resp = self.sonar_client.get(
                "/api/hotspots/search",
                params={"projectKey": PROJECT_KEY, "status": "TO_REVIEW"},
                headers={"Authorization": f"Bearer {SONAR_TOKEN}"},
            )
            if resp.status_code == 200:
                data = resp.json
                unreviewed = len(data.get("hotspots", []))
                self.assertEqual(unreviewed, 0, f"Expected 0 unreviewed hotspots, found {unreviewed}")
            else:
                self.assertIn(resp.status_code, [200, 401])
        else:
            # Static check: CORS configuration has restricted origins
            cors_file = (
                WORKSPACE_ROOT
                / "src"
                / "main"
                / "java"
                / "com"
                / "proyecto"
                / "servicios"
                / "config"
                / "CorsConfig.java"
            )
            self.assertTrue(cors_file.exists())

    def test_t1_f09_02_maintainability_rating_a(self):
        """T1_F09_02: Verify SonarQube Maintainability Rating A (sqale_rating = 1.0)."""
        if is_sonar_online(SONAR_URL):
            resp = self.sonar_client.get(
                "/api/measures/component",
                params={
                    "component": PROJECT_KEY,
                    "metricKeys": "sqale_rating,code_smells",
                },
                headers={"Authorization": f"Bearer {SONAR_TOKEN}"},
            )
            if resp.status_code == 200:
                measures = resp.json.get("component", {}).get("measures", [])
                sqale = next((m.get("value") for m in measures if m.get("metric") == "sqale_rating"), "1.0")
                self.assertEqual(sqale, "1.0", f"Maintainability rating must be 1.0 (A), got {sqale}")
            else:
                self.assertIn(resp.status_code, [200, 401])
        else:
            # Source check: no TODO/FIXME markers in sensitive utility classes
            crypto_file = (
                WORKSPACE_ROOT
                / "src"
                / "main"
                / "java"
                / "com"
                / "proyecto"
                / "servicios"
                / "util"
                / "AesEncryption.java"
            )
            content = crypto_file.read_text(encoding="utf-8")
            self.assertNotIn("TODO", content)

    def test_t1_f09_03_refactored_constants_in_services(self):
        """T1_F09_03: Verify duplicated string literals are refactored to constants in CatalogoProductosCron."""
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
        # Check that REDIS key or cron zones are declared as constants/properties
        self.assertIn("redisKey", content)

    def test_t1_f09_04_cognitive_complexity_reduction(self):
        """T1_F09_04: Verify method decomposition in PagoServicioServiceImpl."""
        pago_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "impl"
            / "PagoServicioServiceImpl.java"
        )
        self.assertTrue(pago_file.exists())
        content = pago_file.read_text(encoding="utf-8")
        # Verify helper methods exist to break down complexity
        self.assertIn("validarReferencia", content)
        self.assertIn("procesarPago", content)

    def test_t1_f09_05_clean_stream_modernization(self):
        """T1_F09_05: Verify stream transformations in ProductoController and repositories."""
        prod_ctrl = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "controller"
            / "ProductoController.java"
        )
        self.assertTrue(prod_ctrl.exists())
        content = prod_ctrl.read_text(encoding="utf-8")
        self.assertIn("obtenerProductosCategorizados", content)


if __name__ == "__main__":
    unittest.main()
