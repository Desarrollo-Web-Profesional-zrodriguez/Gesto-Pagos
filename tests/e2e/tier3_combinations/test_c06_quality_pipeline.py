"""
Tier 3 - Pairwise Combination: Quality Pipeline, Cron Resilience & SonarQube Integration.
Tests T3_PAIR_11 and T3_PAIR_13.
"""
import unittest
from tests.e2e.config import WORKSPACE_ROOT


class TestCombination06QualityPipeline(unittest.TestCase):
    """Pairwise combination tests linking F07/F02 (Cron & DB Fallback) and F10/F12/F13 (Tests & Gate)."""

    def test_t3_pair_11_cron_proxy_and_catalog_sync_fallback(self):
        """T3_PAIR_11: Verify CatalogoProductosCron fallback links Redis error catch to DB persistence."""
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

    def test_t3_pair_13_unit_tests_coverage_and_sonarqube_gate(self):
        """T3_PAIR_13: Verify build.gradle binds unit test execution, JaCoCo XML generation, and SonarQube analysis."""
        build_gradle = WORKSPACE_ROOT / "build.gradle"
        self.assertTrue(build_gradle.exists())
        content = build_gradle.read_text(encoding="utf-8")
        self.assertIn("useJUnitPlatform()", content)
        self.assertIn("jacocoTestReport", content)
        self.assertIn("org.sonarqube", content)
        self.assertIn("sonar.coverage.exclusions", content)


if __name__ == "__main__":
    unittest.main()
