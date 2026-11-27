"""
Tier 2 - Feature 13: Boundary & Pipeline Error Cases (B13).
Tests T2_F13_01 to T2_F13_05.
"""
import unittest
from tests.e2e.client import HttpClient, is_sonar_online
from tests.e2e.config import SONAR_URL, WORKSPACE_ROOT


class TestBoundary13E2E(unittest.TestCase):
    """Boundary and negative tests for build tool failure propagation and SonarQube unreachable handling."""

    def setUp(self):
        self.sonar_client = HttpClient(base_url=SONAR_URL)

    def test_t2_f13_01_gradle_build_file_exists(self):
        """T2_F13_01: Verify build.gradle defines standard test task."""
        build_gradle = WORKSPACE_ROOT / "build.gradle"
        self.assertTrue(build_gradle.exists())
        content = build_gradle.read_text(encoding="utf-8")
        self.assertIn("useJUnitPlatform()", content)

    def test_t2_f13_02_sonar_unreachable_error_resilience(self):
        """T2_F13_02: Verify client handles unreachable SonarQube URL safely without raising unhandled exception."""
        unreachable_client = HttpClient(base_url="http://localhost:59999")
        resp = unreachable_client.get("/api/system/status")
        self.assertEqual(resp.status_code, 0)
        self.assertTrue(resp.error_message)

    def test_t2_f13_03_quality_gate_metric_coverage_definition(self):
        """T2_F13_03: Verify build.gradle defines JaCoCo coverage targets."""
        build_gradle = WORKSPACE_ROOT / "build.gradle"
        content = build_gradle.read_text(encoding="utf-8")
        self.assertIn("jacocoTestReport", content)

    def test_t2_f13_04_sonar_multicriteria_suppression_rules(self):
        """T2_F13_04: Verify SonarQube multicriteria rules in build.gradle cover e1 to e5."""
        build_gradle = WORKSPACE_ROOT / "build.gradle"
        content = build_gradle.read_text(encoding="utf-8")
        for e in ["e1", "e2", "e3", "e4", "e5"]:
            self.assertIn(f"sonar.issue.ignore.multicriteria.{e}.ruleKey", content)

    def test_t2_f13_05_documentation_structure_validation(self):
        """T2_F13_05: Verify technical documentation deliverables in workspace."""
        doc_file = WORKSPACE_ROOT / "DOCUMENTO_TECNICO.md"
        readme_file = WORKSPACE_ROOT / "README.md"
        self.assertTrue(doc_file.exists() or readme_file.exists())


if __name__ == "__main__":
    unittest.main()
