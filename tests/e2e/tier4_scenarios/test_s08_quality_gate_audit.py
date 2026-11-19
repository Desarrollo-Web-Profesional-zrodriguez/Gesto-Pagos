"""
Tier 4 - Scenario 08: Full Platform CI/CD Quality Gate & Regression Verification Journey.
Scenario T4_SCEN_08.
"""
import unittest
from tests.e2e.client import HttpClient, is_sonar_online
from tests.e2e.config import SONAR_URL, PROJECT_KEY, SONAR_TOKEN, WORKSPACE_ROOT


class TestScenario08QualityGateAudit(unittest.TestCase):
    """End-to-end quality gate pipeline audit: Compiler, JaCoCo, SonarQube Ratings & Miniregistro."""

    def setUp(self):
        self.sonar_client = HttpClient(base_url=SONAR_URL)

    def test_t4_scen_08_quality_gate_and_documentation_audit(self):
        """T4_SCEN_08: Audit compiler artifacts, JaCoCo coverage config, Sonar ratings, and documentation."""
        # 1. Gradle build wrapper verification
        gradlew = WORKSPACE_ROOT / "gradlew.bat"
        self.assertTrue(gradlew.exists(), "gradlew.bat must exist")

        build_gradle = WORKSPACE_ROOT / "build.gradle"
        self.assertTrue(build_gradle.exists(), "build.gradle must exist")
        gradle_content = build_gradle.read_text(encoding="utf-8")

        # 2. Coverage setup verification
        self.assertIn("jacocoTestReport", gradle_content)
        self.assertIn("reports", gradle_content)

        # 3. SonarQube Quality Gate audit if server is online
        if is_sonar_online(SONAR_URL):
            status_resp = self.sonar_client.get(
                "/api/qualitygates/project_status",
                params={"projectKey": PROJECT_KEY},
                headers={"Authorization": f"Bearer {SONAR_TOKEN}"},
            )
            if status_resp.status_code == 200:
                status = status_resp.json.get("projectStatus", {}).get("status")
                self.assertIn(status, ["OK", "WARN"])

            # Verify zero bugs
            bugs_resp = self.sonar_client.get(
                "/api/issues/search",
                params={"componentKeys": PROJECT_KEY, "types": "BUG", "resolved": "false"},
                headers={"Authorization": f"Bearer {SONAR_TOKEN}"},
            )
            if bugs_resp.status_code == 200:
                self.assertEqual(bugs_resp.json.get("total", 0), 0)

        # 4. Verify technical documentation in workspace
        doc_candidates = [
            WORKSPACE_ROOT / "MINIREGISTRO.md",
            WORKSPACE_ROOT / "miniregistro.md",
            WORKSPACE_ROOT / "DOCUMENTO_TECNICO.md",
            WORKSPACE_ROOT / "README.md",
        ]
        found = any(p.exists() for p in doc_candidates)
        self.assertTrue(found, "Documentation deliverable must exist in workspace")


if __name__ == "__main__":
    unittest.main()
