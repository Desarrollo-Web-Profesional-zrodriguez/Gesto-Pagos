import { getStoredBaseUrl, normalizeBaseUrl } from '../config/appConfig';
import { ApiException } from './apiError';
import { encodeStrictJson } from './strictDecimal';

export interface ApiResponse<T> {
  status: number;
  data: T;
}

export interface RequestOptions {
  headers?: Record<string, string>;
  timeoutMs?: number;
}

class HttpClient {
  private baseUrl: string = getStoredBaseUrl();
  private tokenProvider: () => string | null = () => null;
  private onUnauthorizedExpired?: () => void;
  private onUserNetworkActivity?: () => void;

  configure(hooks: {
    baseUrl?: string;
    tokenProvider?: () => string | null;
    onUnauthorizedExpired?: () => void;
    onUserNetworkActivity?: () => void;
  }): void {
    if (hooks.baseUrl) {
      this.baseUrl = normalizeBaseUrl(hooks.baseUrl);
    }
    if (hooks.tokenProvider) {
      this.tokenProvider = hooks.tokenProvider;
    }
    if (hooks.onUnauthorizedExpired) {
      this.onUnauthorizedExpired = hooks.onUnauthorizedExpired;
    }
    if (hooks.onUserNetworkActivity) {
      this.onUserNetworkActivity = hooks.onUserNetworkActivity;
    }
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }

  setBaseUrl(url: string): void {
    const normalized = normalizeBaseUrl(url);
    if (normalized) {
      this.baseUrl = normalized;
    }
  }

  async get<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>('GET', path, undefined, options);
  }

  async post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>('POST', path, body, options);
  }

  async put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>('PUT', path, body, options);
  }

  async patch<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>('PATCH', path, body, options);
  }

  async delete<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>('DELETE', path, undefined, options);
  }

  private async request<T>(
    method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    path: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    const headers: Record<string, string> = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    };
    const token = this.tokenProvider();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    if (options?.headers) {
      Object.assign(headers, options.headers);
    }

    const controller = new AbortController();
    const timeoutMs = options?.timeoutMs ?? 25000;
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);

    const encodedBody =
      body === undefined || body === null
        ? undefined
        : typeof body === 'string'
        ? body
        : encodeStrictJson(body);

    try {
      const response = await fetch(`${this.baseUrl}${path}`, {
        method,
        headers,
        body: encodedBody,
        signal: controller.signal,
      });

      const contentType = response.headers.get('content-type') ?? '';
      let parsed: unknown = null;
      const text = await response.text();
      if (text.trim().length > 0) {
        if (contentType.includes('application/json') || text.trim().startsWith('{') || text.trim().startsWith('[')) {
          try {
            parsed = JSON.parse(text);
          } catch {
            parsed = text;
          }
        } else {
          parsed = text;
        }
      }

      if (response.status >= 200 && response.status < 300) {
        this.onUserNetworkActivity?.();
        return {
          status: response.status,
          data: parsed as T,
        };
      }

      const apiError = ApiException.fromResponse(response.status, parsed);
      if (apiError.esSesionExpirada) {
        this.onUnauthorizedExpired?.();
      }
      throw apiError;
    } catch (err) {
      if (err instanceof ApiException) {
        throw err;
      }
      if (err instanceof DOMException && err.name === 'AbortError') {
        throw new ApiException({
          mensaje:
            'El servidor tardó demasiado en responder. Si realizaste un pago, verifica tu historial antes de reintentar.',
        });
      }
      throw new ApiException({
        mensaje:
          'No fue posible conectar con el servicio en este momento. Verifica tu conexión e intenta nuevamente.',
      });
    } finally {
      window.clearTimeout(timer);
    }
  }
}

export const apiClient = new HttpClient();
