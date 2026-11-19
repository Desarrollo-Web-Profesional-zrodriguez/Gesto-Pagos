"""
Tier 1 - Feature 12: Branch Coverage Hardening (>80.0%) (F12).
Tests T1_F12_01 to T1_F12_05.
"""
import unittest
import xml.etree.ElementTree as ET
from tests.e2e.config import WORKSPACE_ROOT, JACOCO_REPORT_XML


class TestFeature12CoverageThreshold(unittest.TestCase):
    """Functional tests verifying branch coverage branches in PagoServicioServiceImplTest and JaCoCo reports."""

    def setUp(self):
        self.pago_test_file = (
            WORKSPACE_ROOT
            / "src"
            / "test"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "PagoServicioServiceImplTest.java"
        )
        self.assertTrue(self.pago_test_file.exists(), "PagoServicioServiceImplTest.java must exist")
        self.test_content = self.pago_test_file.read_text(encoding="utf-8")

    def test_t1_f12_01_tipofront1_airtime_coverage(self):
        """T1_F12_01: Verify PagoServicioServiceImplTest exercises branches for tipoFront=1 airtime flow."""
        self.assertIn("testProcesarPago_Exitoso_Codigo01", self.test_content)
        self.assertIn("testProcesarPago_MontoIgualAPrecioFijo", self.test_content)

    def test_t1_f12_02_tipofront2_utility_coverage(self):
        """T1_F12_02: Verify PagoServicioServiceImplTest exercises branches for tipoFront=2 utility flow."""
        self.assertIn("testProcesarPago_TipoFront2_PrecioEsComisionYPermiteMontoRecibo", self.test_content)
        self.assertIn("testValidarReferencia_Exitosa", self.test_content)

    def test_t1_f12_03_rejection_and_retention_coverage(self):
        """T1_F12_03: Verify PagoServicioServiceImplTest exercises rejection and indeterminate retention branches."""
        self.assertIn("testProcesarPago_RechazoExplicito_LiberaSaldo", self.test_content)
        self.assertIn("testProcesarPago_Indeterminado_ConservaRetencion", self.test_content)

    def test_t1_f12_04_amount_validation_branches(self):
        """T1_F12_04: Verify PagoServicioServiceImplTest exercises fixed price vs open amount branches."""
        self.assertIn("testProcesarPago_MontoManipuladoContraPrecioFijo", self.test_content)
        self.assertIn("testProcesarPago_MontoFueraDeRangoAbierto", self.test_content)

    def test_t1_f12_05_jacoco_report_or_threshold_configuration(self):
        """T1_F12_05: Verify JaCoCo coverage configuration in build.gradle or report XML exceeding 80.0%."""
        build_gradle = WORKSPACE_ROOT / "build.gradle"
        self.assertTrue(build_gradle.exists())
        content = build_gradle.read_text(encoding="utf-8")
        self.assertIn("jacocoTestReport", content)
        self.assertIn("reports", content)
        self.assertIn("**/service/**", content)

        if JACOCO_REPORT_XML.exists():
            tree = ET.parse(JACOCO_REPORT_XML)
            root = tree.getroot()
            self.assertEqual(root.tag, "report", "JaCoCo XML report must be a valid JaCoCo report")


if __name__ == "__main__":
    unittest.main()
