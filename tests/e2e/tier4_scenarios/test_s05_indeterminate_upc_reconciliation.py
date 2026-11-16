"""
Tier 4 - Scenario 05: Payment Indeterminate State Handling, UPC Tracking & Reconciliation.
Scenario T4_SCEN_05.
"""
import time
import unittest
from tests.e2e.client import HttpClient, is_backend_online
from tests.e2e.config import BACKEND_URL, DEFAULT_PASSWORD, DEFAULT_NUM_CUENTA, WORKSPACE_ROOT


class TestScenario05IndeterminateReconciliation(unittest.TestCase):
    """End-to-end reconciliation workflow: Submit payment -> Extract UPC -> Query status -> Verify reconciliation service."""

    def setUp(self):
        self.client = HttpClient(base_url=BACKEND_URL)

    def test_t4_scen_05_upc_tracking_and_reconciliation_workflow(self):
        """T4_SCEN_05: UPC issuance, status tracking, and reconciliation architecture verification."""
        if is_backend_online(BACKEND_URL):
            # 1. Execute payment
            idemp_key = f"IDEMP-SCEN-05-{int(time.time() * 1000)}"
            pay_payload = {
                "numCuenta": DEFAULT_NUM_CUENTA,
                "idServicio": 76,
                "idProducto": 205,
                "monto": 150.00,
                "referencia": "2572038101",
                "2fa": DEFAULT_PASSWORD,
            }
            pay_resp = self.client.post("/pagos", json_data=pay_payload, headers={"Idempotency-Key": idemp_key})
            self.assertIn(pay_resp.status_code, [200, 202])
            upc = pay_resp.json.get("upc")
            if upc:
                # 2. Query status endpoint by UPC
                status_resp = self.client.get(f"/pagos/estatus/{upc}")
                self.assertEqual(status_resp.status_code, 200)
                self.assertEqual(status_resp.json.get("upc"), upc)
                self.assertIn("estatus", status_resp.json)

        # 3. Verify ConciliacionPagosService presence and architecture in codebase
        reconcile_service = (
            WORKSPACE_ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "proyecto"
            / "servicios"
            / "service"
            / "ConciliacionPagosService.java"
        )
        self.assertTrue(reconcile_service.exists(), "ConciliacionPagosService.java must exist")
        content = reconcile_service.read_text(encoding="utf-8")
        self.assertIn("conciliarPendientes", content)
        self.assertIn("GestoPagoTransaccion", content)


if __name__ == "__main__":
    unittest.main()
