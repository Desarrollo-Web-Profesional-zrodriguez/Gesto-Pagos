import React, { createContext, useCallback, useContext, useState } from 'react';
import { AlertCircle, CheckCircle2, Info, X, AlertTriangle } from 'lucide-react';

export type ToastVariant = 'success' | 'info' | 'warning' | 'error';

export interface ToastItem {
  id: string;
  title: string;
  description?: string;
  variant: ToastVariant;
}

interface ToastContextValue {
  notify: (params: { title: string; description?: string; variant?: ToastVariant }) => void;
}

const ToastContext = createContext<ToastContextValue>({
  notify: () => {},
});

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const notify = useCallback(
    ({
      title,
      description,
      variant = 'info',
    }: {
      title: string;
      description?: string;
      variant?: ToastVariant;
    }) => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      setToasts((prev) => [...prev.slice(-3), { id, title, description, variant }]);
      window.setTimeout(() => dismiss(id), 4200);
    },
    [dismiss]
  );

  return (
    <ToastContext.Provider value={{ notify }}>
      {children}
      <div
        aria-live="polite"
        aria-atomic="false"
        className="pointer-events-none fixed bottom-4 right-4 z-[70] flex w-full max-w-sm flex-col gap-2.5 px-4 sm:px-0"
      >
        {toasts.map((t) => {
          const styles: Record<
            ToastVariant,
            { ring: string; iconBg: string; Icon: React.ComponentType<{ className?: string }> }
          > = {
            success: {
              ring: 'border-emerald-200/90 bg-white',
              iconBg: 'bg-emerald-50 text-emerald-600',
              Icon: CheckCircle2,
            },
            info: {
              ring: 'border-sky-200/90 bg-white',
              iconBg: 'bg-sky-50 text-sky-600',
              Icon: Info,
            },
            warning: {
              ring: 'border-amber-200/90 bg-white',
              iconBg: 'bg-amber-50 text-amber-600',
              Icon: AlertTriangle,
            },
            error: {
              ring: 'border-rose-200/90 bg-white',
              iconBg: 'bg-rose-50 text-rose-600',
              Icon: AlertCircle,
            },
          };
          const { ring, iconBg, Icon } = styles[t.variant];

          return (
            <div
              key={t.id}
              role="status"
              className={`pointer-events-auto flex items-start gap-3 rounded-2xl border p-3.5 shadow-elevated animate-toast-in ${ring}`}
            >
              <div className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-xl ${iconBg}`}>
                <Icon className="h-4 w-4" />
              </div>
              <div className="min-w-0 flex-1 pt-0.5">
                <p className="text-xs font-bold text-gp-primary">{t.title}</p>
                {t.description && (
                  <p className="mt-0.5 text-xs leading-relaxed text-gp-muted">{t.description}</p>
                )}
              </div>
              <button
                type="button"
                onClick={() => dismiss(t.id)}
                aria-label="Cerrar notificación"
                className="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-gp-primary"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
};

export function useToast(): ToastContextValue {
  return useContext(ToastContext);
}
