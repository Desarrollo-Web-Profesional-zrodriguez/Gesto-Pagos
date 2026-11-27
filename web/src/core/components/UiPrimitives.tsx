import React, { useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  Receipt,
  Server,
  Smartphone,
  X,
  XCircle,
} from 'lucide-react';
import { BACKEND_PRESETS, saveBaseUrl } from '../config/appConfig';
import { apiClient } from '../network/apiClient';
import { ApiException } from '../network/apiError';

export const ErrorBanner: React.FC<{
  error: unknown;
  onDismiss?: () => void;
  className?: string;
}> = ({ error, onDismiss, className = '' }) => {
  if (!error) return null;

  const isApi = error instanceof ApiException;
  const message = isApi
    ? error.message
    : error instanceof Error
    ? error.message
    : String(error);

  const detalles = isApi ? Object.entries(error.detallesValidacion) : [];
  const badgeParts: string[] = [];
  if (isApi && error.codigoCatalogo) badgeParts.push(error.codigoCatalogo);
  if (isApi && error.codigoGestoPago) badgeParts.push(`GestoPago ${error.codigoGestoPago}`);

  return (
    <div
      role="alert"
      className={`rounded-xl border border-red-200 bg-red-50 p-3.5 text-sm text-red-900 ${className}`}
    >
      <div className="flex items-start gap-2.5">
        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold">Error en la operación</span>
            {badgeParts.length > 0 && (
              <span className="rounded-md bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
                {badgeParts.join(' · ')}
              </span>
            )}
          </div>
          <p className="mt-1 whitespace-pre-wrap text-red-800">{message}</p>
          {detalles.length > 0 && (
            <ul className="mt-2 list-disc space-y-0.5 pl-4 text-xs text-red-700">
              {detalles.map(([campo, desc]) => (
                <li key={campo}>
                  <span className="font-semibold">{campo}:</span> {desc}
                </li>
              ))}
            </ul>
          )}
        </div>
        {onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            className="rounded-lg p-1 text-red-500 transition hover:bg-red-100 hover:text-red-800"
            aria-label="Cerrar error"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
};

export const ServiceLogo: React.FC<{
  urlLogo?: string | null;
  alt: string;
  tipoFront?: number;
  size?: 'sm' | 'md' | 'lg';
}> = ({ urlLogo, alt, tipoFront = 2, size = 'md' }) => {
  const [failed, setFailed] = useState(false);

  const dims =
    size === 'sm'
      ? 'h-9 w-9 rounded-lg'
      : size === 'lg'
      ? 'h-14 w-14 rounded-2xl'
      : 'h-11 w-11 rounded-xl';

  const iconDims = size === 'sm' ? 'h-4 w-4' : size === 'lg' ? 'h-6 w-6' : 'h-5 w-5';
  const cleanUrl = urlLogo?.trim();

  if (cleanUrl && !failed) {
    return (
      <div
        className={`${dims} flex shrink-0 items-center justify-center overflow-hidden border border-gp-border bg-white p-1`}
      >
        <img
          src={cleanUrl}
          alt={alt}
          className="h-full w-full object-contain"
          onError={() => setFailed(true)}
        />
      </div>
    );
  }

  return (
    <div
      className={`${dims} flex shrink-0 items-center justify-center border border-gp-border ${
        tipoFront === 1 ? 'bg-sky-50 text-sky-700' : 'bg-slate-100 text-gp-primary'
      }`}
    >
      {tipoFront === 1 ? <Smartphone className={iconDims} /> : <Receipt className={iconDims} />}
    </div>
  );
};

export const StatusBadge: React.FC<{ estatus?: string | null }> = ({ estatus }) => {
  const norm = (estatus ?? 'DESCONOCIDO').trim().toUpperCase();

  if (norm === 'EXITOSA' || norm === 'ACTIVA' || norm === 'VALIDA') {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 ring-1 ring-inset ring-emerald-600/20">
        <CheckCircle2 className="h-3.5 w-3.5" />
        {norm}
      </span>
    );
  }

  if (norm === 'PENDIENTE_CONCILIACION' || norm === 'PROCESANDO' || norm === 'PENDIENTE') {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-800 ring-1 ring-inset ring-amber-600/20">
        <Clock className="h-3.5 w-3.5" />
        {norm}
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-red-700 ring-1 ring-inset ring-red-600/20">
      <XCircle className="h-3.5 w-3.5" />
      {norm}
    </span>
  );
};

export const ServerConfigModal: React.FC<{
  open: boolean;
  onClose: () => void;
  onUpdated?: (newUrl: string) => void;
}> = ({ open, onClose, onUpdated }) => {
  const [url, setUrl] = useState(apiClient.getBaseUrl());
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = url.trim();
    if (!cleaned.startsWith('http://') && !cleaned.startsWith('https://')) {
      setError('La URL debe comenzar con http:// o https://');
      return;
    }
    const saved = saveBaseUrl(cleaned);
    apiClient.setBaseUrl(saved);
    onUpdated?.(saved);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-2xl border border-gp-border bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
              <Server className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-gp-primary">Configurar Backend API</h3>
              <p className="text-xs text-gp-muted">Servidor Spring Boot compartido con Flutter</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSave} className="mt-5 space-y-4">
          <div>
            <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
              Entornos predefinidos
            </label>
            <div className="space-y-2">
              {BACKEND_PRESETS.map((preset) => (
                <button
                  key={preset.url}
                  type="button"
                  onClick={() => {
                    setUrl(preset.url);
                    setError(null);
                  }}
                  className={`flex w-full items-center justify-between rounded-xl border px-3 py-2 text-left text-xs transition ${
                    url === preset.url
                      ? 'border-gp-secondary bg-sky-50 font-semibold text-gp-secondary'
                      : 'border-gp-border text-gp-primary hover:bg-slate-50'
                  }`}
                >
                  <span>{preset.label}</span>
                  <span className="font-mono text-[11px]">{preset.url}</span>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
              URL personalizada
            </label>
            <input
              type="url"
              value={url}
              onChange={(e) => {
                setUrl(e.target.value);
                setError(null);
              }}
              placeholder="http://localhost:8080"
              className="input-field font-mono text-xs"
            />
            {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancelar
            </button>
            <button type="submit" className="btn-primary">
              Guardar conexión
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
