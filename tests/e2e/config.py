"""
Configuration module for Gesto Pago E2E Test Suite.
Provides endpoints, timeouts, credentials, and project paths.
"""
import os
from pathlib import Path

# Workspace Paths
TESTS_E2E_DIR = Path(__file__).resolve().parent
WORKSPACE_ROOT = TESTS_E2E_DIR.parent.parent

# Backend Service Configuration
BACKEND_HOST = os.environ.get("BACKEND_HOST", "localhost")
BACKEND_PORT = int(os.environ.get("BACKEND_PORT", "8085"))
BACKEND_URL = os.environ.get("BACKEND_URL", f"http://{BACKEND_HOST}:{BACKEND_PORT}")

# SonarQube Configuration
SONAR_HOST = os.environ.get("SONAR_HOST", "localhost")
SONAR_PORT = int(os.environ.get("SONAR_PORT", "9000"))
SONAR_URL = os.environ.get("SONAR_URL", f"http://{SONAR_HOST}:{SONAR_PORT}")
SONAR_TOKEN = os.environ.get("SONAR_TOKEN", "sqa_c3b3d1cf0b37d7fd0e130243692a35a204d004b0")
PROJECT_KEY = os.environ.get("PROJECT_KEY", "com.proyecto.servicio:prueba")

# HTTP Client Timeouts (seconds)
CONNECT_TIMEOUT = float(os.environ.get("CONNECT_TIMEOUT", "10.0"))
READ_TIMEOUT = float(os.environ.get("READ_TIMEOUT", "30.0"))

# Gradle Wrapper Path
GRADLEW_BAT = WORKSPACE_ROOT / "gradlew.bat"
GRADLEW_SH = WORKSPACE_ROOT / "gradlew"
JACOCO_REPORT_XML = WORKSPACE_ROOT / "build" / "reports" / "jacoco" / "test" / "jacocoTestReport.xml"

# Test Defaults
DEFAULT_NUM_CUENTA = "1000000001000000"
DEFAULT_PASSWORD = "PasswordSeguro123*"
DEFAULT_EMAIL = "juan.perez@example.com"
