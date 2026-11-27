import React, { useEffect, useState } from 'react';
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  Copy,
  CreditCard,
  Eye,
  EyeOff,
  History,
  Lock,
  RefreshCw,
  ShieldCheck,
  Store,
  Wallet,
  Wifi,
} from 'lucide-react';
import { useToast } from '../../../core/components/ToastContext';
import {
  ErrorBanner,
  ServiceLogo,
  SkeletonBlock,
  StatusBadge,
} from '../../../core/components/UiPrimitives';
import { useSession } from '../../../core/session/SessionContext';
import { formatFecha, formatMxn, maskCuenta } from '../../../core/utils/formatters';
import { pagoRepository, TransaccionHistorialDto } from '../../pagos/data/pagoRepository';

interface CuentaTabProps {
  refreshSignal?: number;
  onGoCatalog: () => void;
  onGoHistory: () => void;
}

function formatCuentaGrupos(numero?: string): string {
  if (!numero) return 'No disponible';
  return numero.replace(/(.{4})/g, '$1 ').trim();
}

export const CuentaTab: React.FC<CuentaTabProps> = ({
  refreshSignal = 0,
  onGoCatalog,
  onGoHistory,
}) => {
  const { cliente, cuentaActiva, saldoActual, refreshPerfilYSaldo, seleccionarCuenta } =
    useSession();
  const { notify } = useToast();

  const [revealNumber, setRevealNumber] = useState(false);
  const [hideBalance, setHideBalance] = useState(false);
  const [copied, setCopied] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<unknown>(null);

  // Recent transactions preview
  const [recentTx, setRecentTx] = useState<TransaccionHistorialDto[]>([]);
  const [loadingRecent, setLoadingRecent] = useState(false);

  const cuentas = cliente?.cuentas ?? [];
  const disponible = saldoActual?.saldoDisponible ?? cuentaActiva?.saldo?.saldoDisponible ?? 0;
  const contable = saldoActual?.saldoContable ?? cuentaActiva?.saldo?.saldoContable ?? 0;
  const retenido = Math.max(0, contable - disponible);
  const pctDisponible =
    contable > 0 ? Math.min(100, Math.round((disponible / contable) * 100)) : 100;

  useEffect(() => {
    const numCuenta = cuentaActiva?.numeroCuenta;
    if (!numCuenta) {
      setRecentTx([]);
      return;
    }
    let active = true;
    setLoadingRecent(true);
    pagoRepository
      .obtenerHistorial(numCuenta)
      .then((list) => {
        if (active) setRecentTx(list.slice(0, 4));
      })
      .catch(() => {
        if (active) setRecentTx([]);
      })
      .finally(() => {
        if (active) setLoadingRecent(false);
      });
    return () => {
      active = false;
    };
  }, [cuentaActiva?.numeroCuenta, refreshSignal]);

  const handleRefresh = async () => {
    setRefreshing(true);
    setError(null);
    try {
      await refreshPerfilYSaldo();
      if (cuentaActiva?.numeroCuenta) {
        const list = await pagoRepository.obtenerHistorial(cuentaActiva.numeroCuenta);
        setRecentTx(list.slice(0, 4));
      }
      notify({
        title: 'Saldos actualizados',
        description: 'Información sincronizada en tiempo real.',
        variant: 'success',
      });
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
    notify({
      title: 'Cuenta copiada',
      description: `Número ${maskCuenta(cuentaActiva.numeroCuenta)} copiado al portapapeles.`,
      variant: 'success',
    });
    window.setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-extrabold tracking-tight text-gp-primary">
            Resumen de Cuenta
          </h2>
          <p className="mt-0.5 text-sm text-gp-muted">
            Disponibilidad inmediata, desglose contable y actividad reciente
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setHideBalance((prev) => !prev)}
            className="btn-secondary"
            title={hideBalance ? 'Mostrar importes' : 'Ocultar importes'}
          >
            {hideBalance ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
            <span className="hidden sm:inline">
              {hideBalance ? 'Mostrar saldos' : 'Modo discreto'}
            </span>
          </button>
          <button
            type="button"
            onClick={handleRefresh}
            disabled={refreshing}
            className="btn-secondary"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      {/* Multi-Account Selector */}
      {cuentas.length > 1 && (
        <div className="card-surface p-4 sm:p-5">
          <p className="mb-3 text-xs font-bold uppercase tracking-wider text-gp-muted">
            Tus cuentas digitales ({cuentas.length})
          </p>
          <div className="grid gap-2.5 sm:grid-cols-2 lg:grid-cols-3">
            {cuentas.map((c) => {
              const selected = c.numeroCuenta === cuentaActiva?.numeroCuenta;
              return (
                <button
                  key={c.numeroCuenta}
                  type="button"
                  onClick={() => void seleccionarCuenta(c)}
                  className={`flex items-center justify-between gap-3 rounded-2xl border p-3.5 text-left transition ${
                    selected
                      ? 'border-gp-secondary bg-sky-50/80 ring-2 ring-gp-secondary/15'
                      : 'border-gp-border bg-white hover:border-slate-300 hover:bg-slate-50'
                  }`}
                >
                  <div>
                    <p className="font-mono text-xs font-bold text-gp-primary">
                      {maskCuenta(c.numeroCuenta)}
                    </p>
                    <p className="mt-0.5 text-[11px] text-gp-muted">
                      {c.tipoCuenta || 'DÉBITO'} ·{' '}
                      {hideBalance ? '••••' : formatMxn(c.saldo?.saldoDisponible ?? 0)}
                    </p>
                  </div>
                  <StatusBadge estatus={c.estatus || 'ACTIVA'} />
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Main Digital Card + Accounting Breakdown */}
      <div className="grid gap-6 lg:grid-cols-12">
        {/* Virtual Banking Card */}
        <div className="group relative flex flex-col justify-between overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-sky-950 p-6 text-white shadow-xl transition-all duration-300 hover:shadow-2xl hover:shadow-sky-950/20 sm:p-7 lg:col-span-7">
          <div className="pointer-events-none absolute inset-0 bg-grid-dark opacity-30" />
          <div className="pointer-events-none absolute -right-16 -top-16 h-60 w-60 rounded-full bg-sky-500/20 blur-3xl transition-transform duration-500 group-hover:scale-110" />
          <div className="pointer-events-none absolute -bottom-20 -left-16 h-60 w-60 rounded-full bg-emerald-500/15 blur-3xl transition-transform duration-500 group-hover:scale-110" />

          {/* Card Header */}
          <div className="relative flex items-start justify-between gap-4">
            <div className="flex items-center gap-3.5">
              {/* Realistic Metallic EMV Chip */}
              <div className="flex h-9 w-12 items-center justify-center rounded-lg border border-amber-200/45 bg-gradient-to-br from-amber-200/35 via-amber-100/20 to-amber-400/30 shadow-inner backdrop-blur-sm">
                <div className="grid h-5 w-7 grid-cols-2 gap-0.5 rounded border border-amber-200/50 p-0.5">
                  <span className="rounded-[1px] bg-amber-200/30" />
                  <span className="rounded-[1px] bg-amber-200/30" />
                </div>
              </div>
              <div>
                <p className="text-[11px] font-bold uppercase tracking-widest text-sky-300">
                  GestoPagos · {cuentaActiva?.tipoCuenta || 'DÉBITO DIGITAL'}
                </p>
                <p className="mt-0.5 font-display text-base font-bold text-white sm:text-lg">
                  {cliente?.nombreCompleto || cuentaActiva?.nombreTitular || 'Titular'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Wifi className="h-4 w-4 rotate-90 text-sky-300/75" />
              <StatusBadge estatus={cuentaActiva?.estatus || 'ACTIVA'} inverse />
            </div>
          </div>

          {/* Primary Balance */}
          <div className="relative my-8">
            <p className="text-xs font-medium text-slate-300/90">Saldo disponible para pagos</p>
            <div className="mt-1.5 flex flex-wrap items-baseline gap-3">
              <p className="font-display text-3xl font-extrabold tracking-tight tabular-nums sm:text-4xl">
                {hideBalance ? '••••••••' : formatMxn(disponible)}
              </p>
              {!hideBalance && retenido > 0.001 && (
                <span className="rounded-full border border-amber-400/30 bg-amber-400/15 px-2.5 py-0.5 text-[11px] font-semibold text-amber-200">
                  {formatMxn(retenido)} en conciliación
                </span>
              )}
            </div>
          </div>

          {/* Account Number & Copy/Reveal Controls */}
          <div className="relative flex flex-wrap items-end justify-between gap-4 border-t border-white/15 pt-4">
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-widest text-slate-400">
                Cuenta CLABE / Tarjeta Digital (16 dígitos)
              </p>
              <p className="mt-1 font-mono text-base font-semibold tracking-widest text-slate-100 sm:text-lg">
                {revealNumber
                  ? formatCuentaGrupos(cuentaActiva?.numeroCuenta)
                  : maskCuenta(cuentaActiva?.numeroCuenta)}
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setRevealNumber((prev) => !prev)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-white/15 bg-white/10 px-3 py-1.5 text-xs font-semibold text-white backdrop-blur-sm transition hover:bg-white/20"
              >
                {revealNumber ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                {revealNumber ? 'Ocultar' : 'Ver 16 dígitos'}
              </button>
              <button
                type="button"
                onClick={handleCopyCuenta}
                className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-semibold backdrop-blur-sm transition ${
                  copied
                    ? 'border-emerald-400/40 bg-emerald-500/20 text-emerald-200'
                    : 'border-white/15 bg-white/10 text-white hover:bg-white/20'
                }`}
              >
                {copied ? (
                  <Check className="h-3.5 w-3.5 text-emerald-300" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
                {copied ? 'Copiado' : 'Copiar'}
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Accounting Breakdown + Quick Shortcuts */}
        <div className="flex flex-col justify-between gap-4 lg:col-span-5">
          <div className="card-surface space-y-4 p-5">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-sky-50 text-gp-secondary">
                  <Wallet className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-gp-primary">Desglose contable</h3>
                  <p className="flex items-center gap-1.5 text-xs text-gp-muted">
                    <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500" />
                    <span>
                      {formatFecha(
                        saldoActual?.fechaUltimaActualizacion ??
                          cuentaActiva?.saldo?.fechaUltimaActualizacion
                      )}
                    </span>
                  </p>
                </div>
              </div>
              <span className="rounded-lg bg-emerald-50 px-2.5 py-1 font-mono text-[11px] font-bold text-emerald-700 tabular-nums">
                {pctDisponible}% libre
              </span>
            </div>

            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-sky-500 transition-all duration-500"
                style={{ width: `${pctDisponible}%` }}
              />
            </div>

            <div className="space-y-2 text-sm">
              <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3.5 py-2.5">
                <span className="text-xs font-medium text-gp-muted">Disponible inmediato</span>
                <span className="font-mono text-xs font-bold text-emerald-700 tabular-nums sm:text-sm">
                  {hideBalance ? '••••••' : formatMxn(disponible)}
                </span>
              </div>
              <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3.5 py-2.5">
                <span className="text-xs font-medium text-gp-muted">Saldo contable total</span>
                <span className="font-mono text-xs font-bold text-gp-primary tabular-nums sm:text-sm">
                  {hideBalance ? '••••••' : formatMxn(contable)}
                </span>
              </div>
              {retenido > 0.001 && (
                <div className="flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-2.5">
                  <span className="text-xs font-medium text-amber-900">
                    Retenido en conciliación
                  </span>
                  <span className="font-mono text-xs font-bold text-amber-800 tabular-nums sm:text-sm">
                    {hideBalance ? '••••••' : formatMxn(retenido)}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={onGoCatalog}
              className="group flex flex-col justify-between rounded-2xl bg-slate-950 p-4 text-left text-white shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-sky-700 hover:shadow-elevated"
            >
              <div className="flex w-full items-center justify-between">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/10 text-sky-300 group-hover:bg-white/20 group-hover:text-white">
                  <Store className="h-4 w-4" />
                </div>
                <ArrowUpRight className="h-4 w-4 text-slate-400 transition-transform duration-200 group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-white" />
              </div>
              <div className="mt-4">
                <p className="text-sm font-bold">Pagar Servicio</p>
                <p className="mt-0.5 text-[11px] text-slate-300 group-hover:text-sky-100">
                  Recargas, luz, agua y pines
                </p>
              </div>
            </button>

            <button
              type="button"
              onClick={onGoHistory}
              className="group card-interactive flex flex-col justify-between p-4 text-left"
            >
              <div className="flex w-full items-center justify-between">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                  <History className="h-4 w-4" />
                </div>
                <ArrowUpRight className="h-4 w-4 text-slate-400 transition-transform duration-200 group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-gp-secondary" />
              </div>
              <div className="mt-4">
                <p className="text-sm font-bold text-gp-primary">Ver Historial</p>
                <p className="mt-0.5 text-[11px] text-gp-muted">Comprobantes y estatus UPC</p>
              </div>
            </button>
          </div>
        </div>
      </div>

      {/* Recent Activity Section */}
      <section className="card-surface overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <div>
            <h3 className="text-base font-bold text-gp-primary">Actividad Reciente</h3>
            <p className="text-xs text-gp-muted">Últimas operaciones de pago en tu cuenta activa</p>
          </div>
          <button
            type="button"
            onClick={onGoHistory}
            className="inline-flex items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-semibold text-gp-secondary transition hover:bg-sky-50"
          >
            Ver todo
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="p-5">
          {loadingRecent ? (
            <div className="space-y-2.5">
              <SkeletonBlock className="h-16" />
              <SkeletonBlock className="h-16" />
            </div>
          ) : recentTx.length === 0 ? (
            <div className="flex flex-col items-center justify-between gap-3 rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 px-5 py-7 text-center sm:flex-row sm:text-left">
              <div>
                <p className="text-sm font-bold text-gp-primary">
                  Aún no tienes movimientos registrados
                </p>
                <p className="mt-0.5 text-xs text-gp-muted">
                  Explora el catálogo GestoPago para realizar tu primera recarga o pago de servicio.
                </p>
              </div>
              <button type="button" onClick={onGoCatalog} className="btn-primary shrink-0 text-xs">
                <Store className="h-4 w-4" />
                Ir al catálogo
              </button>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {recentTx.map((tx) => {
                const title = tx.producto || tx.servicio || `Servicio #${tx.idServicio}`;
                const refOrTel = tx.referencia || tx.telefono || 'Sin referencia';
                return (
                  <div
                    key={tx.upc || `${tx.idTransaccionLocal}-${tx.fechaCreacion}`}
                    className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <ServiceLogo
                        urlLogo={tx.urlLogo}
                        alt={tx.servicio || title}
                        tipoFront={tx.referencia ? 2 : 1}
                        size="sm"
                      />
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="truncate text-sm font-bold text-gp-primary">{title}</p>
                          <StatusBadge estatus={tx.estatus} />
                        </div>
                        <p className="mt-0.5 truncate text-xs text-gp-muted">
                          Ref: <span className="font-mono">{refOrTel}</span> ·{' '}
                          {formatFecha(tx.fechaCreacion)}
                        </p>
                      </div>
                    </div>
                    <p className="font-mono text-sm font-extrabold text-gp-primary tabular-nums">
                      {hideBalance ? '••••••' : `-${formatMxn(tx.monto)}`}
                    </p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </section>

      {/* Security Architecture Strip */}
      <div className="card-surface flex flex-wrap items-center justify-between gap-4 bg-gradient-to-r from-white to-slate-50/80 p-4 sm:px-5">
        <div className="flex items-center gap-3.5">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200/70">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-gp-primary">
              Protección Bancaria en Cada Operación
            </p>
            <p className="text-xs text-gp-muted">
              Toda referencia se valida antes del cargo y tu saldo disponible cuenta con respaldo
              automático ante tiempos de espera del proveedor.
            </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
            <Lock className="h-3.5 w-3.5" />
            Confirmación 2FA
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-semibold text-gp-muted">
            <CreditCard className="h-3.5 w-3.5" />
            Sincronización Móvil
          </span>
        </div>
      </div>
    </div>
  );
};
