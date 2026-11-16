import React, { useState } from 'react';
import {
  Check,
  Copy,
  CreditCard,
  Eye,
  EyeOff,
  History,
  RefreshCw,
  ShieldCheck,
  Store,
  Wallet,
} from 'lucide-react';
import { ErrorBanner, StatusBadge } from '../../../core/components/UiPrimitives';
import { useSession } from '../../../core/session/SessionContext';
import { formatFecha, formatMxn, maskCuenta } from '../../../core/utils/formatters';

interface CuentaTabProps {
  onGoCatalog: () => void;
  onGoHistory: () => void;
}

export const CuentaTab: React.FC<CuentaTabProps> = ({ onGoCatalog, onGoHistory }) => {
  const { cliente, cuentaActiva, saldoActual, refreshPerfilYSaldo, seleccionarCuenta } =
    useSession();

  const [revealNumber, setRevealNumber] = useState(false);
  const [copied, setCopied] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const cuentas = cliente?.cuentas ?? [];
  const disponible = saldoActual?.saldoDisponible ?? cuentaActiva?.saldo?.saldoDisponible ?? 0;
  const contable = saldoActual?.saldoContable ?? cuentaActiva?.saldo?.saldoContable ?? 0;
  const retenido = Math.max(0, contable - disponible);

  const handleRefresh = async () => {
    setRefreshing(true);
    setError(null);
    try {
      await refreshPerfilYSaldo();
    } catch (err) {
      setError(err);
    } finally {
      setRefreshing(false);
    }
  };

  const handleCopyCuenta = async () => {
    if (!cuentaActiva?.numeroCuenta || !navigator.clipboard) return;
    await navigator.clipboard.writeText(cuentaActiva.numeroCuenta);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gp-primary">Mi Cuenta Digital</h2>
          <p className="text-sm text-gp-muted">
            Saldos sincronizados en tiempo real con la base de datos bancaria
          </p>
        </div>
        <button
          type="button"
          onClick={handleRefresh}
          disabled={refreshing}
          className="btn-secondary"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          Actualizar saldo
        </button>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      {cuentas.length > 1 && (
        <div className="card-surface p-4">
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
            Seleccionar cuenta activa
          </label>
          <div className="flex flex-wrap gap-2">
            {cuentas.map((c) => {
              const selected = c.numeroCuenta === cuentaActiva?.numeroCuenta;
              return (
                <button
                  key={c.numeroCuenta}
                  type="button"
                  onClick={() => seleccionarCuenta(c)}
                  className={`rounded-xl border px-3.5 py-2 text-xs font-semibold transition ${
                    selected
                      ? 'border-gp-secondary bg-sky-50 text-gp-secondary'
                      : 'border-gp-border bg-white text-gp-primary hover:bg-slate-50'
                  }`}
                >
                  {maskCuenta(c.numeroCuenta)} · {c.tipoCuenta || 'DÉBITO'}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Main Digital Card + Balances */}
      <div className="grid gap-6 lg:grid-cols-12">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-sky-900 p-6 text-white shadow-xl lg:col-span-7">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-sky-300">
                GestoPagos Cuenta {cuentaActiva?.tipoCuenta || 'DIGITAL'}
              </p>
              <p className="mt-1 text-lg font-bold">
                {cliente?.nombreCompleto || cuentaActiva?.nombreTitular || 'Titular'}
              </p>
            </div>
            <StatusBadge estatus={cuentaActiva?.estatus || 'ACTIVA'} />
          </div>

          <div className="mt-8">
            <p className="text-xs text-slate-300">Saldo disponible para pagos</p>
            <p className="mt-1 text-3xl font-extrabold tracking-tight sm:text-4xl">
              {formatMxn(disponible)}
            </p>
          </div>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-white/15 pt-4">
            <div>
              <p className="text-[11px] uppercase tracking-wider text-slate-300">
                Número de cuenta (16 dígitos)
              </p>
              <p className="mt-0.5 font-mono text-base font-semibold tracking-wider">
                {revealNumber
                  ? cuentaActiva?.numeroCuenta || 'No disponible'
                  : maskCuenta(cuentaActiva?.numeroCuenta)}
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setRevealNumber((prev) => !prev)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-white/20 bg-white/10 px-3 py-1.5 text-xs font-medium text-white hover:bg-white/20"
              >
                {revealNumber ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                {revealNumber ? 'Ocultar' : 'Mostrar'}
              </button>
              <button
                type="button"
                onClick={handleCopyCuenta}
                className="inline-flex items-center gap-1.5 rounded-xl border border-white/20 bg-white/10 px-3 py-1.5 text-xs font-medium text-white hover:bg-white/20"
              >
                {copied ? <Check className="h-3.5 w-3.5 text-emerald-300" /> : <Copy className="h-3.5 w-3.5" />}
                {copied ? 'Copiado' : 'Copiar'}
              </button>
            </div>
          </div>
        </div>

        {/* Balance Breakdown & Quick Actions */}
        <div className="flex flex-col justify-between gap-4 lg:col-span-5">
          <div className="card-surface space-y-4 p-5">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                <Wallet className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-gp-primary">Desglose contable</h3>
                <p className="text-xs text-gp-muted">
                  Actualizado:{' '}
                  {formatFecha(
                    saldoActual?.fechaUltimaActualizacion ??
                      cuentaActiva?.saldo?.fechaUltimaActualizacion
                  )}
                </p>
              </div>
            </div>

            <div className="space-y-2.5 text-sm">
              <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3.5 py-2.5">
                <span className="text-gp-muted">Saldo disponible</span>
                <span className="font-bold text-emerald-700">{formatMxn(disponible)}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3.5 py-2.5">
                <span className="text-gp-muted">Saldo contable total</span>
                <span className="font-bold text-gp-primary">{formatMxn(contable)}</span>
              </div>
              {retenido > 0.001 && (
                <div className="flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-2.5">
                  <span className="text-amber-900">Retenido en conciliación</span>
                  <span className="font-bold text-amber-800">{formatMxn(retenido)}</span>
                </div>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={onGoCatalog}
              className="btn-primary flex-col items-start gap-1.5 rounded-2xl p-4 text-left"
            >
              <Store className="h-5 w-5 text-sky-300" />
              <span className="text-sm font-bold">Pagar Servicios</span>
              <span className="text-[11px] font-normal text-slate-300">
                Catálogo real GestoPago
              </span>
            </button>

            <button
              type="button"
              onClick={onGoHistory}
              className="btn-secondary flex-col items-start gap-1.5 rounded-2xl p-4 text-left"
            >
              <History className="h-5 w-5 text-gp-secondary" />
              <span className="text-sm font-bold">Historial</span>
              <span className="text-[11px] font-normal text-gp-muted">
                Folios y comprobantes
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Security note */}
      <div className="card-surface flex flex-wrap items-center justify-between gap-4 p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-semibold text-gp-primary">
              Operación protegida en 2 fases + Idempotencia
            </p>
            <p className="text-xs text-gp-muted">
              Toda referencia se valida antes de confirmar y ningún cargo se marca exitoso sin
              respuesta positiva del proveedor.
            </p>
          </div>
        </div>
        <div className="inline-flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-medium text-gp-muted">
          <CreditCard className="h-3.5 w-3.5" />
          Web &amp; Mobile Sync
        </div>
      </div>
    </div>
  );
};
