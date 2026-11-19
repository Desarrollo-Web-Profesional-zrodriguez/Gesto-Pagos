"""
Resilient HTTP Client for Gesto Pago E2E Tests.
Built exclusively with Python 3.12 standard library (urllib.request, http.client).
Zero external dependencies.
"""
import json
import socket
import time
import urllib.request
import urllib.error
import urllib.parse
from http.client import HTTPResponse
from typing import Any, Dict, Optional, Union

from tests.e2e.config import BACKEND_URL, SONAR_URL, CONNECT_TIMEOUT, READ_TIMEOUT

_CACHE_TTL = 3.0
_backend_cache: Dict[str, Any] = {"ts": 0.0, "status": False}
_sonar_cache: Dict[str, Any] = {"ts": 0.0, "status": False}


def is_backend_online(url: str = BACKEND_URL, timeout: float = 0.5) -> bool:
    """Checks if the backend is listening and responding to HTTP requests with caching."""
    now = time.time()
    if now - _backend_cache["ts"] < _CACHE_TTL:
        return _backend_cache["status"]

    parsed = urllib.parse.urlparse(url)
    host = parsed.hostname or "localhost"
    port = parsed.port or (443 if parsed.scheme == "https" else 80)

    # Fast socket pre-check
    try:
        with socket.create_connection((host, port), timeout=0.15):
            pass
    except (socket.timeout, ConnectionRefusedError, OSError):
        _backend_cache["ts"] = now
        _backend_cache["status"] = False
        return False

    try:
        req = urllib.request.Request(
            f"{url.rstrip('/')}/productos",
            headers={"Accept": "application/json"},
            method="GET"
        )
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            status = resp.status in (200, 401, 403, 404)
            _backend_cache["ts"] = now
            _backend_cache["status"] = status
            return status
    except urllib.error.HTTPError:
        _backend_cache["ts"] = now
        _backend_cache["status"] = True
        return True
    except Exception:
        _backend_cache["ts"] = now
        _backend_cache["status"] = False
        return False


def is_sonar_online(url: str = SONAR_URL, timeout: float = 0.5) -> bool:
    """Checks if SonarQube is listening on the configured URL with caching."""
    now = time.time()
    if now - _sonar_cache["ts"] < _CACHE_TTL:
        return _sonar_cache["status"]

    parsed = urllib.parse.urlparse(url)
    host = parsed.hostname or "localhost"
    port = parsed.port or (443 if parsed.scheme == "https" else 80)

    # Fast socket pre-check
    try:
        with socket.create_connection((host, port), timeout=0.15):
            pass
    except (socket.timeout, ConnectionRefusedError, OSError):
        _sonar_cache["ts"] = now
        _sonar_cache["status"] = False
        return False

    try:
        req = urllib.request.Request(
            f"{url.rstrip('/')}/api/system/status",
            headers={"Accept": "application/json"},
            method="GET"
        )
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            status = resp.status in (200, 401)
            _sonar_cache["ts"] = now
            _sonar_cache["status"] = status
            return status
    except urllib.error.HTTPError:
        _sonar_cache["ts"] = now
        _sonar_cache["status"] = True
        return True
    except Exception:
        _sonar_cache["ts"] = now
        _sonar_cache["status"] = False
        return False



class ApiResponse:
    """Encapsulates HTTP response data for assertions."""

    def __init__(
        self,
        status_code: int,
        headers: Dict[str, str],
        raw_bytes: bytes,
        url: str,
        error_message: Optional[str] = None,
    ):
        self.status_code = status_code
        self.headers = headers
        self.raw_bytes = raw_bytes
        self.url = url
        self.error_message = error_message
        self._parsed_json: Optional[Any] = None
        self._text: Optional[str] = None

    @property
    def text(self) -> str:
        """Decodes raw_bytes to UTF-8 text."""
        if self._text is None:
            try:
                self._text = self.raw_bytes.decode("utf-8")
            except UnicodeDecodeError:
                self._text = self.raw_bytes.decode("latin-1", errors="replace")
        return self._text

    @property
    def json(self) -> Any:
        """Parses response body as JSON. Returns parsed dict/list or empty dict if invalid."""
        if self._parsed_json is None:
            if not self.raw_bytes:
                return {}
            try:
                self._parsed_json = json.loads(self.text)
            except (json.JSONDecodeError, ValueError):
                self._parsed_json = {}
        return self._parsed_json

    @property
    def body(self) -> Any:
        """Alias for json or text depending on content type."""
        parsed = self.json
        if parsed != {}:
            return parsed
        return self.text

    @property
    def is_success(self) -> bool:
        """Returns True if status is 2xx."""
        return 200 <= self.status_code < 300

    def __repr__(self) -> str:
        return f"<ApiResponse [{self.status_code}] len={len(self.raw_bytes)}>"


class HttpClient:
    """Resilient HTTP client supporting GET, POST, PUT, PATCH, DELETE with JSON."""

    def __init__(
        self,
        base_url: str = BACKEND_URL,
        timeout: float = READ_TIMEOUT,
        default_headers: Optional[Dict[str, str]] = None,
    ):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.default_headers = {
            "Accept": "application/json",
            "User-Agent": "GestoPago-E2E-Harness/1.0",
        }
        if default_headers:
            self.default_headers.update(default_headers)
        self.auth_token: Optional[str] = None

    def set_auth_token(self, token: Optional[str]) -> None:
        """Sets the Bearer token for subsequent requests."""
        self.auth_token = token

    def request(
        self,
        method: str,
        path: str,
        params: Optional[Dict[str, Any]] = None,
        json_data: Optional[Any] = None,
        data: Optional[Union[bytes, str]] = None,
        headers: Optional[Dict[str, str]] = None,
        timeout: Optional[float] = None,
    ) -> ApiResponse:
        """Sends an HTTP request and returns an ApiResponse."""
        url = path if path.startswith("http://") or path.startswith("https://") else f"{self.base_url}/{path.lstrip('/')}"

        if params:
            query_string = urllib.parse.urlencode(params)
            url = f"{url}?{query_string}" if "?" not in url else f"{url}&{query_string}"

        req_headers = dict(self.default_headers)
        if self.auth_token:
            req_headers["Authorization"] = f"Bearer {self.auth_token}"
        if headers:
            req_headers.update(headers)

        body_bytes: Optional[bytes] = None
        if json_data is not None:
            body_bytes = json.dumps(json_data).encode("utf-8")
            req_headers["Content-Type"] = "application/json"
        elif data is not None:
            if isinstance(data, str):
                body_bytes = data.encode("utf-8")
            else:
                body_bytes = data

        req = urllib.request.Request(
            url=url,
            data=body_bytes,
            headers=req_headers,
            method=method.upper(),
        )

        effective_timeout = timeout if timeout is not None else self.timeout

        try:
            with urllib.request.urlopen(req, timeout=effective_timeout) as resp:
                resp_headers = {k.lower(): v for k, v in resp.headers.items()}
                raw_bytes = resp.read()
                return ApiResponse(
                    status_code=resp.status,
                    headers=resp_headers,
                    raw_bytes=raw_bytes,
                    url=url,
                )
        except urllib.error.HTTPError as http_err:
            resp_headers = {k.lower(): v for k, v in http_err.headers.items()} if http_err.headers else {}
            raw_bytes = http_err.read() if hasattr(http_err, "read") else b""
            return ApiResponse(
                status_code=http_err.code,
                headers=resp_headers,
                raw_bytes=raw_bytes,
                url=url,
                error_message=str(http_err),
            )
        except urllib.error.URLError as url_err:
            return ApiResponse(
                status_code=0,
                headers={},
                raw_bytes=b"",
                url=url,
                error_message=f"Connection failure to {url}: {url_err.reason}",
            )
        except Exception as ex:
            return ApiResponse(
                status_code=0,
                headers={},
                raw_bytes=b"",
                url=url,
                error_message=f"Unexpected error: {type(ex).__name__}: {ex}",
            )

    def get(self, path: str, **kwargs) -> ApiResponse:
        return self.request("GET", path, **kwargs)

    def post(self, path: str, **kwargs) -> ApiResponse:
        return self.request("POST", path, **kwargs)

    def put(self, path: str, **kwargs) -> ApiResponse:
        return self.request("PUT", path, **kwargs)

    def patch(self, path: str, **kwargs) -> ApiResponse:
        return self.request("PATCH", path, **kwargs)

    def delete(self, path: str, **kwargs) -> ApiResponse:
        return self.request("DELETE", path, **kwargs)
