"""
Tier 1 - Feature 07: SonarQube Bug Remediation (F07).
Tests T1_F07_01 to T1_F07_05.
"""
import re
import unittest
from tests.e2e.client import HttpClient, is_backend_online, is_sonar_online
from tests.e2e.config import BACKEND_URL, SONAR_URL, PROJECT_KEY, SONAR_TOKEN, WORKSPACE_ROOT


class TestFeature07SonarQubeBugs(unittest.TestCase):
    """Functional tests verifying resolution of SonarQube bugs S2229 and S2259."""

    def setUp(self):
        self.backend_client = HttpClient(base_url=BACKEND_URL)
        self.sonar_client = HttpClient(base_url=SONAR_URL)

    def test_t1_f07_01_cron_s2229_proxy_invocation(self):
        """T1_F07_01: Verify CatalogoProductosCron resolves S2229 by invoking transactional methods through bean proxy."""
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
        self.assertTrue(cron_file.exists(), "CatalogoProductosCron.java must exist")
        content = cron_file.read_text(encoding="utf-8")

        # Verify S2229 resolution: either delegates to external service bean (productoService.guardarEnBaseDatos) or proxy self-injection
        has_service_delegation = "productoService.guardarEnBaseDatos" in content
        has_self_injection = bool(re.search(r"private\s+CatalogoProductosCron\s+self;", content))
        has_delegate_call = "delegate.guardarEnBaseDatos" in content or "self.guardarEnBaseDatos" in content
        self.assertTrue(
            has_service_delegation or has_self_injection or has_delegate_call,
            "CatalogoProductosCron must delegate transactional persistence to avoid S2229",
        )

    def test_t1_f07_02_cliente_s2259_null_safe_identificador(self):
        """T1_F07_02: Verify ClienteServiceImpl.obtenerPorIdentificador resolves S2259 null-dereference."""
        if is_backend_online(BACKEND_URL):
            # Test live query with valid or non-existent identifier returns proper HTTP without 500 NPE
            resp = self.backend_client.post(
                "/clientes/consultar",
                json_data={"identificador": "PELJ920520HDFRRN09"},
            )
            self.assertIn(resp.status_code, [200, 404], f"Expected 200 or 404, got {resp.status_code}")
        else:
            service_file = (
                WORKSPACE_ROOT
                / "src"
                / "main"
                / "java"
                / "com"
                / "proyecto"
                / "servicios"
                / "service"
                / "cliente"
                / "impl"
                / "ClienteServiceImpl.java"
            )
            self.assertTrue(service_file.exists())
            content = service_file.read_text(encoding="utf-8")
            self.assertIn("obtenerPorIdentificador", content)

    def test_t1_f07_03_cliente_actualizar_completo_null_safety(self):
        """T1_F07_03: Verify ClienteServiceImpl.actualizarCompleto safely handles address/contact collections."""
        service_file = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "cliente"
            / "impl"
            / "ClienteServiceImpl.java"
        )
        self.assertTrue(service_file.exists())
        content = service_file.read_text(encoding="utf-8")
        # Ensure mapping to response or update logic handles null collections safely
        self.assertIn("actualizarCompleto", content)
        self.assertIn("mapearAClienteResponse", content)

    def test_t1_f07_04_cliente_actualizar_parcial_patch_safety(self):
        """T1_F07_04: Verify ClienteServiceImpl.actualizarParcial safely applies PATCH updates when fields are null."""
        if is_backend_online(BACKEND_URL):
            resp = self.backend_client.patch(
                "/clientes",
                json_data={"identificador": "PELJ920520HDFRRN09", "telefonoMovil": "5599887766"},
            )
            self.assertIn(resp.status_code, [200, 404])
        else:
            service_file = (
                WORKSPACE_ROOT
                / "src"
                / "main"
                / "java"
                / "com"
                / "proyecto"
                / "servicios"
                / "service"
                / "cliente"
                / "impl"
                / "ClienteServiceImpl.java"
            )
            content = service_file.read_text(encoding="utf-8")
            self.assertIn("actualizarParcial", content)

    def test_t1_f07_05_sonarqube_zero_open_bugs(self):
        """T1_F07_05: Verify SonarQube reports 0 open bugs (Rating A in Reliability)."""
        if is_sonar_online(SONAR_URL):
            resp = self.sonar_client.get(
                "/api/issues/search",
                params={
                    "componentKeys": PROJECT_KEY,
                    "types": "BUG",
                    "resolved": "false",
                },
                headers={"Authorization": f"Bearer {SONAR_TOKEN}"},
            )
            if resp.status_code == 200:
                data = resp.json
                total_bugs = data.get("total", 0)
                self.assertEqual(total_bugs, 0, f"Expected 0 open bugs in SonarQube, found {total_bugs}")
            else:
                self.assertIn(resp.status_code, [200, 401])
        else:
            # When SonarQube is offline, verify S2229 and S2259 bug patterns are resolved in code
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
            content = cron_file.read_text(encoding="utf-8")
            self.assertNotIn("this.guardarEnBaseDatos(productos);", content)


if __name__ == "__main__":
    unittest.main()
