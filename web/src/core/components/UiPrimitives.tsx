import React, { useEffect, useRef, useState } from 'react';
import {
  AlertCircle,
  AlertTriangle,
  Check,
  CheckCircle2,
  Clock,
  Copy,
  Inbox,
  MinusCircle,
  Receipt,
  Smartphone,
  X,
  XCircle,
} from 'lucide-react';
import { ApiException } from '../network/apiError';
import { useToast } from './ToastContext';

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
  const isValidationStatus = isApi && (error.statusCode === 400 || error.statusCode === 422);

  return (
    <div
      role="alert"
      className={`rounded-2xl border border-rose-200 bg-rose-50/90 p-4 text-sm text-rose-950 shadow-sm animate-fade-in ${className}`}
    >
      <div className="flex items-start gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-rose-600">
          <AlertCircle className="h-4 w-4" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wide text-rose-900">
              {isValidationStatus
                ? 'Revisa la información ingresada'
                : 'No se pudo completar la operación'}
            </span>
            {isApi && error.codigoGestoPago && (
              <span className="rounded-md bg-rose-100 px-2 py-0.5 font-mono text-[11px] font-semibold text-rose-800">
                Ref. proveedor {error.codigoGestoPago}
              </span>
            )}
          </div>
          <p className="mt-1 whitespace-pre-wrap text-xs leading-relaxed text-rose-800">
            {message}
          </p>
          {detalles.length > 0 && (
            <ul className="mt-2 list-disc space-y-0.5 pl-4 text-xs text-rose-700">
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
            className="rounded-lg p-1 text-rose-500 transition hover:bg-rose-100 hover:text-rose-800"
            aria-label="Cerrar alerta"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
};

export const ValidationSummary: React.FC<{
  message?: string | null;
  errorCount?: number;
  onDismiss?: () => void;
  className?: string;
}> = ({ message, errorCount = 1, onDismiss, className = '' }) => {
  if (!message) return null;

  return (
    <div
      role="alert"
      className={`rounded-2xl border border-amber-200/95 bg-amber-50/90 px-4 py-3 text-xs text-amber-950 shadow-sm animate-fade-in ${className}`}
    >
      <div className="flex items-start gap-2.5">
        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
        <div className="min-w-0 flex-1">
          <p className="font-bold text-amber-950">
            {errorCount > 1
              ? `Por favor revisa ${errorCount} campos marcados`
              : 'Verifica el siguiente dato para continuar'}
          </p>
          <p className="mt-0.5 leading-relaxed text-amber-900">{message}</p>
        </div>
        {onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            aria-label="Ocultar aviso"
            className="rounded-lg p-1 text-amber-600 transition hover:bg-amber-100 hover:text-amber-900"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  );
};

export const FieldError: React.FC<{ message?: string | null }> = ({ message }) => {
  if (!message) return null;
  return (
    <p className="mt-1.5 flex items-center gap-1.5 text-xs font-medium text-rose-600 animate-fade-in">
      <AlertCircle className="h-3.5 w-3.5 shrink-0" />
      <span>{message}</span>
    </p>
  );
};

export const OtpCodeInput: React.FC<{
  value: string;
  onChange: (next: string) => void;
  hasError?: boolean;
  disabled?: boolean;
  autoFocus?: boolean;
}> = ({ value, onChange, hasError = false, disabled = false, autoFocus = false }) => {
  const refs = useRef<Array<HTMLInputElement | null>>([]);
  const digits = Array.from({ length: 6 }, (_, i) => value[i] ?? '');

  useEffect(() => {
    if (autoFocus) {
      window.requestAnimationFrame(() => refs.current[0]?.focus());
    }
  }, [autoFocus]);

  const setDigitAt = (index: number, rawChar: string) => {
    const clean = rawChar.replace(/\D/g, '');
    const nextDigits = [...digits];
    nextDigits[index] = clean.slice(-1);
    onChange(nextDigits.join(''));
    if (clean && index < 5) {
      refs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    onChange(pasted);
    const focusIdx = Math.min(5, pasted.length);
    refs.current[focusIdx]?.focus();
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      if (digits[index]) {
        const nextDigits = [...digits];
        nextDigits[index] = '';
        onChange(nextDigits.join(''));
      } else if (index > 0) {
        const nextDigits = [...digits];
        nextDigits[index - 1] = '';
        onChange(nextDigits.join(''));
        refs.current[index - 1]?.focus();
      }
      e.preventDefault();
    } else if (e.key === 'ArrowLeft' && index > 0) {
      e.preventDefault();
      refs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < 5) {
      e.preventDefault();
      refs.current[index + 1]?.focus();
    }
  };

  return (
    <div className="grid grid-cols-6 gap-2 sm:gap-2.5" role="group" aria-label="Código de 6 dígitos">
      {digits.map((digit, idx) => (
        <input
          key={idx}
          ref={(el) => {
            refs.current[idx] = el;
          }}
          type="text"
          inputMode="numeric"
          autoComplete={idx === 0 ? 'one-time-code' : 'off'}
          disabled={disabled}
          maxLength={1}
          aria-label={`Dígito ${idx + 1} de 6`}
          value={digit}
          onPaste={handlePaste}
          onKeyDown={(e) => handleKeyDown(idx, e)}
          onChange={(e) => setDigitAt(idx, e.target.value)}
          className={`h-12 w-full rounded-xl border bg-white text-center font-mono text-xl font-extrabold text-gp-primary shadow-sm transition duration-150 focus:outline-none focus:ring-4 sm:h-14 sm:text-2xl ${
            hasError
              ? 'border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-rose-500/20'
              : digit
              ? 'border-gp-secondary/70 bg-sky-50/30 focus:border-gp-secondary focus:ring-gp-secondary/20'
              : 'border-gp-border focus:border-gp-secondary focus:ring-gp-secondary/20'
          }`}
        />
      ))}
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

  useEffect(() => {
    setFailed(false);
  }, [urlLogo]);

  const dims =
    size === 'sm'
      ? 'h-9 w-9 rounded-xl'
      : size === 'lg'
      ? 'h-14 w-14 rounded-2xl'
      : 'h-11 w-11 rounded-xl';

  const iconDims = size === 'sm' ? 'h-4 w-4' : size === 'lg' ? 'h-6 w-6' : 'h-5 w-5';
  const cleanUrl = urlLogo?.trim();

  if (cleanUrl && !failed) {
    return (
      <div
        className={`${dims} flex shrink-0 items-center justify-center overflow-hidden border border-gp-border bg-white p-1.5 shadow-sm`}
      >
        <img
          src={cleanUrl}
          alt={alt}
          loading="lazy"
          className="h-full w-full object-contain"
          onError={() => setFailed(true)}
        />
      </div>
    );
  }

  return (
    <div
      className={`${dims} flex shrink-0 items-center justify-center border ${
        tipoFront === 1
          ? 'border-sky-200/80 bg-sky-50 text-sky-700'
          : 'border-slate-200 bg-slate-100 text-gp-primary'
      }`}
    >
      {tipoFront === 1 ? <Smartphone className={iconDims} /> : <Receipt className={iconDims} />}
    </div>
  );
};

const STATUS_LABELS: Record<string, string> = {
  EXITOSA: 'Exitosa',
  ACTIVA: 'Activa',
  VALIDA: 'Válida',
  PENDIENTE_CONCILIACION: 'En conciliación',
  PROCESANDO: 'Procesando',
  PENDIENTE: 'Pendiente',
  REVISION_MANUAL: 'Revisión manual',
  INACTIVO: 'Inactivo',
  INACTIVA: 'Inactiva',
  RECHAZADA: 'Rechazada',
  FALLIDA: 'Fallida',
};

export const StatusBadge: React.FC<{ estatus?: string | null; inverse?: boolean }> = ({
  estatus,
  inverse = false,
}) => {
  const norm = (estatus ?? 'DESCONOCIDO').trim().toUpperCase();
  const label = STATUS_LABELS[norm] ?? norm.replace(/_/g, ' ');

  if (norm === 'EXITOSA' || norm === 'ACTIVA' || norm === 'VALIDA') {
    return (
      <span
        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
          inverse
            ? 'bg-emerald-500/20 text-emerald-200 ring-1 ring-inset ring-emerald-400/35'
            : 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20'
        }`}
      >
        <CheckCircle2 className="h-3.5 w-3.5 shrink-0" />
        {label}
      </span>
    );
  }

  if (norm === 'PENDIENTE_CONCILIACION' || norm === 'PROCESANDO' || norm === 'PENDIENTE') {
    return (
      <span
        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
          inverse
            ? 'bg-amber-500/20 text-amber-200 ring-1 ring-inset ring-amber-400/35'
            : 'bg-amber-50 text-amber-800 ring-1 ring-inset ring-amber-600/20'
        }`}
      >
        <Clock className="h-3.5 w-3.5 shrink-0" />
        {label}
      </span>
    );
  }

  if (norm === 'REVISION_MANUAL') {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full bg-violet-50 px-2.5 py-0.5 text-xs font-semibold text-violet-700 ring-1 ring-inset ring-violet-600/20">
        <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
        {label}
      </span>
    );
  }

  if (norm === 'INACTIVO' || norm === 'INACTIVA') {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600 ring-1 ring-inset ring-slate-500/20">
        <MinusCircle className="h-3.5 w-3.5 shrink-0" />
        {label}
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700 ring-1 ring-inset ring-rose-600/20">
      <XCircle className="h-3.5 w-3.5 shrink-0" />
      {label}
    </span>
  );
};

export const CopyChip: React.FC<{
  value?: string | null;
  label?: string;
  toastTitle?: string;
  className?: string;
}> = ({ value, label, toastTitle = 'Copiado al portapapeles', className = '' }) => {
  const { notify } = useToast();
  const [copied, setCopied] = useState(false);

  if (!value) return <span className="text-gp-muted">-</span>;

  const handleCopy = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      notify({ title: toastTitle, description: value, variant: 'success' });
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      // Ignore clipboard permission errors
    }
  };

  return (
    <button
      type="button"
      onClick={handleCopy}
      title={`Copiar ${value}`}
      className={`inline-flex items-center gap-1.5 rounded-lg border border-slate-200/80 bg-slate-50 px-2 py-0.5 font-mono text-xs font-semibold text-gp-primary transition hover:border-sky-300 hover:bg-sky-50 hover:text-gp-secondary ${className}`}
    >
      <span>{label ?? value}</span>
      {copied ? (
        <Check className="h-3 w-3 shrink-0 text-emerald-600" />
      ) : (
        <Copy className="h-3 w-3 shrink-0 text-slate-400" />
      )}
    </button>
  );
};

export const EmptyState: React.FC<{
  icon?: React.ComponentType<{ className?: string }>;
  title: string;
  description?: string;
  action?: React.ReactNode;
}> = ({ icon: Icon = Inbox, title, description, action }) => (
  <div className="card-surface flex flex-col items-center justify-center px-6 py-14 text-center">
    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
      <Icon className="h-6 w-6" />
    </div>
    <h3 className="mt-3.5 text-sm font-bold text-gp-primary">{title}</h3>
    {description && <p className="mt-1 max-w-sm text-xs text-gp-muted">{description}</p>}
    {action && <div className="mt-4">{action}</div>}
  </div>
);

export const SkeletonBlock: React.FC<{ className?: string }> = ({ className = 'h-24' }) => (
  <div className={`animate-pulse rounded-2xl bg-slate-200/70 ${className}`} />
);
