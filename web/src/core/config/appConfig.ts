const STORAGE_KEY = 'gestopago_api_base_url';

export interface BackendPreset {
  label: string;
  url: string;
}

export const BACKEND_PRESETS: BackendPreset[] = [
  { label: 'Localhost Spring Boot (localhost:8080)', url: 'http://localhost:8080' },
  { label: 'Red local / 127.0.0.1:8080', url: 'http://127.0.0.1:8080' }
];

export function normalizeBaseUrl(raw: string): string {
  const trimmed = raw.trim();
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed;
}

export function getStoredBaseUrl(): string {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (typeof window !== 'undefined') {
    const saved = window.localStorage.getItem(STORAGE_KEY);
    if (saved && saved.trim().length > 0) {
      return normalizeBaseUrl(saved);
    }
  }
  if (typeof envUrl === 'string' && envUrl.trim().length > 0) {
    return normalizeBaseUrl(envUrl);
  }
  return 'http://localhost:8080';
}

export function saveBaseUrl(url: string): string {
  const normalized = normalizeBaseUrl(url);
  if (normalized && typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, normalized);
  }
  return normalized;
}
