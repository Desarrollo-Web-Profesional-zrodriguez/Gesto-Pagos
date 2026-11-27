import React, { useEffect, useId } from 'react';
import { X } from 'lucide-react';

interface ModalShellProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: React.ReactNode;
  icon?: React.ReactNode;
  maxWidthClass?: string;
  preventClose?: boolean;
  subheader?: React.ReactNode;
  footer?: React.ReactNode;
  children: React.ReactNode;
}

export const ModalShell: React.FC<ModalShellProps> = ({
  open,
  onClose,
  title,
  subtitle,
  icon,
  maxWidthClass = 'max-w-2xl',
  preventClose = false,
  subheader,
  footer,
  children,
}) => {
  const titleId = useId();

  useEffect(() => {
    if (!open) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !preventClose) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = prevOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open, onClose, preventClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-3 backdrop-blur-sm animate-fade-in sm:p-5"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !preventClose) {
          onClose();
        }
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={`flex max-h-[92vh] w-full ${maxWidthClass} flex-col overflow-hidden rounded-3xl border border-gp-border bg-white shadow-2xl animate-modal-in`}
      >
        {/* Sticky Header */}
        <div className="flex items-center justify-between gap-3 border-b border-gp-border bg-white px-5 py-4 sm:px-6">
          <div className="flex min-w-0 items-center gap-3">
            {icon && <div className="shrink-0">{icon}</div>}
            <div className="min-w-0">
              <h3 id={titleId} className="truncate text-base font-bold text-gp-primary sm:text-lg">
                {title}
              </h3>
              {subtitle && <div className="mt-0.5 text-xs text-gp-muted">{subtitle}</div>}
            </div>
          </div>
          <button
            type="button"
            disabled={preventClose}
            onClick={onClose}
            aria-label="Cerrar ventana"
            className="rounded-xl p-2 text-gp-muted transition hover:bg-slate-100 hover:text-gp-primary disabled:opacity-40"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {subheader}

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-5 py-5 sm:px-6">{children}</div>

        {footer && (
          <div className="border-t border-gp-border bg-slate-50/90 px-5 py-3.5 sm:px-6">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
};
