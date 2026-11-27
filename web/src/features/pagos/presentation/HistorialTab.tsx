import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  Clock,
  Eye,
  History,
  Loader2,
  Mail,
  Receipt,
  RefreshCw,
  Search,
  X,
  XCircle,
} from 'lucide-react';
import { ModalShell } from '../../../core/components/ModalShell';
import { useToast } from '../../../core/components/ToastContext';
import {
  CopyChip,
  EmptyState,
  ErrorBanner,
  FieldError,
  ServiceLogo,
  SkeletonBlock,
  StatusBadge,
} from '../../../core/components/UiPrimitives';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import { useSession } from '../../../core/session/SessionContext';
import { formatFecha, formatMxn } from '../../../core/utils/formatters';
import { BackendValidators } from '../../../core/validation/backendValidators';
import {
  PagoEstatusResponseDto,
  pagoRepository,
  TransaccionHistorialDto,
} from '../data/pagoRepository';

export const HistorialTab: React.FC = () => {
  const { cliente, cuentaActiva, refreshPerfilYSaldo } = useSession();
  const { notify } = useToast();

  const [items, setItems] = useState<TransaccionHistorialDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');
  const [search, setSearch] = useState('');

  const [selectedTx, setSelectedTx] = useState<TransaccionHistorialDto | null>(null);
  const [liveStatus, setLiveStatus] = useState<PagoEstatusResponseDto | null>(null);
  const [checkingLive, setCheckingLive] = useState(false);
  const [emailDest, setEmailDest] = useState('');
  const [emailDestError, setEmailDestError] = useState<string | null>(null);
  const [sendingEmail, setSendingEmail] = useState(false);
  const [emailSentMsg, setEmailSentMsg] = useState<string | null>(null);

  const cargarHistorial = async () => {
    if (!cuentaActiva?.numeroCuenta) {
      setItems([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const list = await pagoRepository.obtenerHistorial(cuentaActiva.numeroCuenta);
      setItems(list);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void cargarHistorial();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cuentaActiva?.numeroCuenta]);

  const counts = useMemo(() => {
    let exitosas = 0;
    let pendientes = 0;
    let rechazadas = 0;
    let volumenExitoso = 0;
    for (const tx of items) {
      const est = (tx.estatus ?? '').toUpperCase();
      if (est === 'EXITOSA') {
        exitosas++;
        volumenExitoso += tx.monto ?? 0;
      } else if (est === 'PENDIENTE_CONCILIACION' || est === 'PROCESANDO') {
        pendientes++;
      } else {
        rechazadas++;
      }
    }
    return { total: items.length, exitosas, pendientes, rechazadas, volumenExitoso };
  }, [items]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return items.filter((tx) => {
      const est = (tx.estatus ?? '').toUpperCase();
      if (statusFilter === 'EXITOSA' && est !== 'EXITOSA') return false;
      if (
        statusFilter === 'PENDIENTE' &&
        est !== 'PENDIENTE_CONCILIACION' &&
        est !== 'PROCESANDO'
      ) {
        return false;
      }
      if (
        statusFilter === 'RECHAZADA' &&
        (est === 'EXITOSA' || est === 'PENDIENTE_CONCILIACION' || est === 'PROCESANDO')
      ) {
        return false;
      }
      if (!q) return true;
      return (
        (tx.servicio ?? '').toLowerCase().includes(q) ||
        (tx.producto ?? '').toLowerCase().includes(q) ||
        (tx.referencia ?? '').toLowerCase().includes(q) ||
        (tx.telefono ?? '').toLowerCase().includes(q) ||
        (tx.upc ?? '').toLowerCase().includes(q) ||
        (tx.numAutorizacion ?? '').toLowerCase().includes(q)
      );
    });
  }, [items, search, statusFilter]);

  const openDetalle = (tx: TransaccionHistorialDto) => {
    setSelectedTx(tx);
    setLiveStatus(null);
    setEmailDest(cliente?.correoElectronico ?? '');
    setEmailDestError(null);
    setEmailSentMsg(null);
  };

  const handleCheckLive = async (upc: string) => {
    setCheckingLive(true);
    try {
      const est = await pagoRepository.consultarEstatusPorUpc(upc);
      setLiveStatus(est);
      await cargarHistorial();
      await refreshPerfilYSaldo();
      notify({
        title: 'Estatus verificado en vivo',
        description: `UPC ${upc}: ${est.estatus}`,
        variant: 'info',
      });
    } catch (err) {
      setError(err);
    } finally {
      setCheckingLive(false);
    }
  };

  const handleResendEmail = async (e: React.FormEvent, tx: TransaccionHistorialDto) => {
    e.preventDefault();
    setEmailSentMsg(null);
    const mailErr = BackendValidators.email(emailDest, true);
    if (mailErr) {
      setEmailDestError(mailErr);
      return;
    }
    setEmailDestError(null);
    setSendingEmail(true);
    try {
      const res = await pagoRepository.enviarComprobanteEmail({
        destinatarioEmail: emailDest.trim(),
        destinatarioNombre: cliente?.nombreCompleto ?? cliente?.nombre,
        numAutorizacion: tx.numAutorizacion,
        idTx: tx.idTxGestoPago,
        upc: tx.upc,
        numCuenta: tx.numeroCuenta,
        idServicio: tx.idServicio,
        servicio: tx.servicio,
        idProducto: tx.idProducto,
        producto: tx.producto,
        telefono: tx.telefono,
        referencia: tx.referencia,
        montoPagado: tx.monto !== undefined ? StrictDecimal.fromNumber(tx.monto) : undefined,
        comision: tx.comision !== undefined ? StrictDecimal.fromNumber(tx.comision) : undefined,
        legend: tx.legend,
        fechaProveedor: tx.fechaCreacion,
        horaLocal: tx.horaLocal,
        logoUrl: tx.urlLogo,
        tipoFront: tx.referencia ? 2 : 1,
      });
      if (res.enviado !== false) {
        setEmailSentMsg(res.mensaje || 'Comprobante enviado correctamente.');
        notify({ title: 'Comprobante enviado', description: emailDest.trim(), variant: 'success' });
      } else {
        setEmailDestError(res.mensaje || 'No fue posible enviar el comprobante.');
      }
    } catch (err) {
      setEmailDestError(
        err instanceof Error ? err.message : 'No fue posible enviar el correo.'
      );
    } finally {
      setSendingEmail(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-extrabold tracking-tight text-gp-primary">
            Historial de Operaciones
          </h2>
          <p className="mt-0.5 text-sm text-gp-muted">
            Bitácora auditable de transacciones, comprobantes y seguimiento por UPC
          </p>
        </div>
        <button
          type="button"
          onClick={cargarHistorial}
          disabled={loading}
          className="btn-secondary"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Actualizar</span>
        </button>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      {/* KPI Cards */}
      <div className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
        <div className="card-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gp-muted">Total operaciones</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-100 text-gp-primary">
              <Receipt className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 font-display text-2xl font-extrabold text-gp-primary tabular-nums">
            {counts.total}
          </p>
          <p className="mt-0.5 text-[11px] text-gp-muted">
            Pagado: <span className="font-mono font-semibold">{formatMxn(counts.volumenExitoso)}</span>
          </p>
        </div>

        <div className="card-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gp-muted">Exitosas</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <CheckCircle2 className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 font-display text-2xl font-extrabold text-emerald-700 tabular-nums">
            {counts.exitosas}
          </p>
          <p className="mt-0.5 text-[11px] text-gp-muted">Cargo confirmado</p>
        </div>

        <div className="card-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gp-muted">En conciliación</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
              <Clock className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 font-display text-2xl font-extrabold text-amber-700 tabular-nums">
            {counts.pendientes}
          </p>
          <p className="mt-0.5 text-[11px] text-gp-muted">Verificación automática</p>
        </div>

        <div className="card-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gp-muted">No aplicadas</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-rose-50 text-rose-600">
              <XCircle className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 font-display text-2xl font-extrabold text-rose-700 tabular-nums">
            {counts.rechazadas}
          </p>
          <p className="mt-0.5 text-[11px] text-gp-muted">Retención liberada</p>
        </div>
      </div>

      {/* Search & Status Filter Bar */}
      <div className="card-surface space-y-3.5 p-4 sm:p-5">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
          <input
            type="text"
            className="input-field pl-10 pr-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Filtrar por servicio, referencia, teléfono, UPC o número de autorización..."
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch('')}
              aria-label="Limpiar búsqueda"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {[
            { id: 'TODOS', label: 'Todas', count: counts.total },
            { id: 'EXITOSA', label: 'Exitosas', count: counts.exitosas },
            { id: 'PENDIENTE', label: 'En conciliación', count: counts.pendientes },
            { id: 'RECHAZADA', label: 'Rechazadas / Revisión', count: counts.rechazadas },
          ].map((f) => {
            const active = statusFilter === f.id;
            return (
              <button
                key={f.id}
                type="button"
                onClick={() => setStatusFilter(f.id)}
                className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
                  active
                    ? 'bg-slate-950 text-white shadow-sm'
                    : 'bg-slate-100 text-gp-muted hover:bg-slate-200 hover:text-gp-primary'
                }`}
              >
                <span>{f.label}</span>
                <span
                  className={`rounded-full px-1.5 py-0.5 font-mono text-[10px] tabular-nums ${
                    active ? 'bg-white/20 text-white' : 'bg-white text-gp-muted'
                  }`}
                >
                  {f.count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Transactions List */}
      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, idx) => (
            <SkeletonBlock key={idx} className="h-24" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={History}
          title="No hay transacciones para mostrar"
          description={
            search || statusFilter !== 'TODOS'
              ? 'Ningún movimiento coincide con los filtros actuales.'
              : 'Cuando realices pagos de servicios o recargas aparecerán listados aquí.'
          }
          action={
            search || statusFilter !== 'TODOS' ? (
              <button
                type="button"
                onClick={() => {
                  setSearch('');
                  setStatusFilter('TODOS');
                }}
                className="btn-secondary text-xs"
              >
                Limpiar filtros
              </button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3">
          {filtered.map((tx) => {
            const refOrTel = tx.referencia || tx.telefono || 'Sin referencia';
            const labelServicio = tx.producto || tx.servicio || `Servicio #${tx.idServicio}`;
            return (
              <div
                key={tx.upc || `${tx.idTransaccionLocal}-${tx.fechaCreacion}`}
                onClick={() => openDetalle(tx)}
                className="card-interactive flex cursor-pointer flex-col justify-between gap-4 p-4 sm:flex-row sm:items-center sm:p-5"
              >
                <div className="flex min-w-0 items-start gap-3.5">
                  <ServiceLogo
                    urlLogo={tx.urlLogo}
                    alt={tx.servicio || labelServicio}
                    tipoFront={tx.referencia ? 2 : 1}
                  />
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="truncate text-sm font-bold text-gp-primary">
                        {labelServicio}
                      </h3>
                      <StatusBadge estatus={tx.estatus} />
                    </div>
                    {tx.servicio && tx.producto && (
                      <p className="text-xs font-medium text-gp-muted">{tx.servicio}</p>
                    )}
                    <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gp-muted">
                      <span>
                        Ref/Tel: <strong className="font-mono text-gp-primary">{refOrTel}</strong>
                      </span>
                      {tx.upc && (
                        <span className="inline-flex items-center gap-1">
                          UPC: <CopyChip value={tx.upc} toastTitle="UPC copiado" />
                        </span>
                      )}
                      {tx.numAutorizacion && (
                        <span>
                          Aut:{' '}
                          <strong className="font-mono text-gp-secondary">
                            {tx.numAutorizacion}
                          </strong>
                        </span>
                      )}
                      <span>{formatFecha(tx.fechaCreacion)}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between gap-4 border-t border-slate-100 pt-3 sm:justify-end sm:border-0 sm:pt-0">
                  <div className="text-left sm:text-right">
                    <p className="font-display text-base font-extrabold text-gp-primary tabular-nums">
                      {formatMxn(tx.monto)}
                    </p>
                    {(tx.comision ?? 0) > 0 && (
                      <p className="font-mono text-[11px] text-gp-muted tabular-nums">
                        +{formatMxn(tx.comision)} comisión
                      </p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      openDetalle(tx);
                    }}
                    className="btn-secondary px-3 py-2 text-xs"
                  >
                    <Eye className="h-3.5 w-3.5" />
                    Comprobante
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Transaction Detail Modal */}
      <ModalShell
        open={Boolean(selectedTx)}
        onClose={() => setSelectedTx(null)}
        maxWidthClass="max-w-xl"
        title={
          selectedTx
            ? selectedTx.producto || selectedTx.servicio || 'Detalle de Operación'
            : 'Detalle'
        }
        subtitle={selectedTx ? `Folio UPC: ${selectedTx.upc || 'N/A'}` : undefined}
        icon={
          selectedTx ? (
            <ServiceLogo
              urlLogo={selectedTx.urlLogo}
              alt={selectedTx.servicio || 'Servicio'}
              tipoFront={selectedTx.referencia ? 2 : 1}
            />
          ) : undefined
        }
      >
        {selectedTx && (
          <div className="space-y-4 text-sm">
            <div className="flex items-center justify-between rounded-2xl border border-slate-200/80 bg-slate-50 px-4 py-3">
              <span className="text-xs font-semibold text-gp-muted">Estado de la transacción</span>
              <StatusBadge estatus={liveStatus?.estatus ?? selectedTx.estatus} />
            </div>

            <div className="space-y-2 rounded-2xl border border-gp-border p-4 text-xs sm:text-sm">
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Servicio</span>
                <span className="text-right font-semibold text-gp-primary">
                  {selectedTx.servicio || `#${selectedTx.idServicio}`}
                </span>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Producto</span>
                <span className="text-right font-medium text-gp-primary">
                  {selectedTx.producto || `#${selectedTx.idProducto}`}
                </span>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Referencia / Teléfono</span>
                <CopyChip
                  value={selectedTx.referencia || selectedTx.telefono}
                  toastTitle="Referencia copiada"
                />
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Importe pagado</span>
                <span className="font-mono font-bold text-gp-primary tabular-nums">
                  {formatMxn(selectedTx.monto)}
                </span>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Comisión</span>
                <span className="font-mono text-gp-primary tabular-nums">
                  {formatMxn(selectedTx.comision ?? 0)}
                </span>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Folio UPC</span>
                <CopyChip value={selectedTx.upc} toastTitle="UPC copiado" />
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Número de autorización</span>
                {selectedTx.numAutorizacion ? (
                  <CopyChip
                    value={selectedTx.numAutorizacion}
                    toastTitle="Autorización copiada"
                  />
                ) : (
                  <span className="text-gp-muted">Pendiente / Sin folio</span>
                )}
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">ID de operación</span>
                <span className="font-mono text-gp-primary">{selectedTx.idTxGestoPago || '-'}</span>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Respuesta proveedor</span>
                <span className="text-right text-gp-primary">
                  {selectedTx.codigoRespuesta ? `[${selectedTx.codigoRespuesta}] ` : ''}
                  {selectedTx.mensajeRespuesta || '-'}
                </span>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <span className="text-gp-muted">Fecha operación</span>
                <span className="text-gp-primary">{formatFecha(selectedTx.fechaCreacion)}</span>
              </div>
              <div className="flex items-center justify-between gap-4 pt-1.5">
                <span className="text-gp-muted">Última actualización</span>
                <span className="text-gp-primary">{formatFecha(selectedTx.fechaActualizacion)}</span>
              </div>
            </div>

            {selectedTx.legend && (
              <div className="rounded-2xl border border-slate-200/80 bg-slate-50 p-3.5 text-xs leading-relaxed text-slate-600">
                {selectedTx.legend}
              </div>
            )}

            {liveStatus && (
              <div className="rounded-2xl border border-sky-200 bg-sky-50 p-3.5 text-xs text-sky-950">
                <p className="font-bold">Estatus en vivo: {liveStatus.estatus}</p>
                <p className="mt-0.5 text-sky-800">{liveStatus.mensaje}</p>
              </div>
            )}

            {selectedTx.upc && (
              <button
                type="button"
                disabled={checkingLive}
                onClick={() => handleCheckLive(selectedTx.upc!)}
                className="btn-secondary w-full"
              >
                <RefreshCw className={`h-4 w-4 ${checkingLive ? 'animate-spin' : ''}`} />
                Verificar estatus actual en vivo
              </button>
            )}

            {(selectedTx.estatus ?? '').toUpperCase() === 'EXITOSA' && (
              <form
                noValidate
                onSubmit={(e) => void handleResendEmail(e, selectedTx)}
                className="rounded-2xl border border-gp-border bg-slate-50/60 p-4"
              >
                <label htmlFor="tx-resend-email" className="input-label">
                  Reenviar comprobante digital por correo
                </label>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <input
                    id="tx-resend-email"
                    type="email"
                    className={`input-field flex-1 ${emailDestError ? 'input-field-error' : ''}`}
                    value={emailDest}
                    onChange={(e) => {
                      setEmailDest(e.target.value);
                      if (emailDestError) setEmailDestError(null);
                      if (emailSentMsg) setEmailSentMsg(null);
                    }}
                    placeholder="correo@ejemplo.com"
                  />
                  <button type="submit" disabled={sendingEmail} className="btn-primary shrink-0">
                    {sendingEmail ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Mail className="h-4 w-4" />
                    )}
                    Enviar
                  </button>
                </div>
                <FieldError message={emailDestError} />
                {emailSentMsg && (
                  <p className="mt-2 flex items-center gap-1.5 text-xs font-medium text-emerald-700">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    <span>{emailSentMsg}</span>
                  </p>
                )}
              </form>
            )}
          </div>
        )}
      </ModalShell>
    </div>
  );
};
