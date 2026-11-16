"""
Tier 1 - Feature 11: Unit Test Suite - Config & Deserializers (F11).
Tests T1_F11_01 to T1_F11_05.
"""
import unittest
from tests.e2e.config import WORKSPACE_ROOT


class TestFeature11ConfigTests(unittest.TestCase):
    """Functional tests verifying configuration classes, deserializer rules, and OpenAPI/Feign setups."""

    def setUp(self):
        self.config_dir = WORKSPACE_ROOT / "src" / "main" / "java" / "com" / "proyecto" / "servicios" / "config"

    def test_t1_f11_01_flexible_date_deserializer_iso_format(self):
        """T1_F11_01: Verify FlexibleLocalDateDeserializer enforces ISO format yyyy-MM-dd."""
        deser_file = self.config_dir / "FlexibleLocalDateDeserializer.java"
        self.assertTrue(deser_file.exists(), "FlexibleLocalDateDeserializer.java must exist")
        content = deser_file.read_text(encoding="utf-8")
        self.assertIn("yyyy-MM-dd", content)
        self.assertIn("AAAA-MM-DD", content)

    def test_t1_f11_02_flexible_date_deserializer_rejects_slashes(self):
        """T1_F11_02: Verify FlexibleLocalDateDeserializer rejects slashes and invalid dates."""
        deser_file = self.config_dir / "FlexibleLocalDateDeserializer.java"
        self.assertTrue(deser_file.exists())
        content = deser_file.read_text(encoding="utf-8")
        self.assertIn("IllegalArgumentException", content)
        self.assertIn("formato de fecha invalido", content)

    def test_t1_f11_03_strict_two_decimal_deserializer_rules(self):
        """T1_F11_03: Verify StrictTwoDecimalNumberDeserializer enforces exactly two decimals and rejects quoted strings."""
        deser_file = self.config_dir / "StrictTwoDecimalNumberDeserializer.java"
        self.assertTrue(deser_file.exists(), "StrictTwoDecimalNumberDeserializer.java must exist")
        content = deser_file.read_text(encoding="utf-8")
        self.assertIn("JsonToken.VALUE_STRING", content)
        self.assertIn("no debe enviarse entre comillas", content)
        self.assertIn("d{2}", content)

    def test_t1_f11_04_redis_and_flyway_config_contracts(self):
        """T1_F11_04: Verify RedisConfig and FlywayConfig presence and bean setup in config package."""
        redis_file = self.config_dir / "RedisConfig.java"
        self.assertTrue(redis_file.exists(), "RedisConfig.java must exist")
        redis_content = redis_file.read_text(encoding="utf-8")
        self.assertIn("redisTemplate", redis_content)

        flyway_file = self.config_dir / "FlywayConfig.java"
        self.assertTrue(flyway_file.exists(), "FlywayConfig.java must exist")

    def test_t1_f11_05_openapi_and_feign_config_contracts(self):
        """T1_F11_05: Verify OpenApi and GestoPagoFeignConfig configurations."""
        openapi_file = self.config_dir / "OpenApi.java"
        self.assertTrue(openapi_file.exists(), "OpenApi.java must exist")
        openapi_content = openapi_file.read_text(encoding="utf-8")
        self.assertIn("OpenAPI", openapi_content)

        feign_file = self.config_dir / "GestoPagoFeignConfig.java"
        self.assertTrue(feign_file.exists(), "GestoPagoFeignConfig.java must exist")


if __name__ == "__main__":
    unittest.main()
