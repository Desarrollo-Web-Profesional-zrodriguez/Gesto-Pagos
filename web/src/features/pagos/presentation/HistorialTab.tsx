import React, { useEffect, useMemo, useState } from 'react';
import {
  Eye,
  Loader2,
  Mail,
  RefreshCw,
  Search,
  X,
} from 'lucide-react';
import { ErrorBanner, ServiceLogo, StatusBadge } from '../../../core/components/UiPrimitives';
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

  const [items, setItems] = useState<TransaccionHistorialDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');
  const [search, setSearch] = useState('');

  const [selectedTx, setSelectedTx] = useState<TransaccionHistorialDto | null>(null);
  const [liveStatus, setLiveStatus] = useState<PagoEstatusResponseDto | null>(null);
  const [checkingLive, setCheckingLive] = useState(false);
  const [emailDest, setEmailDest] = useState('');
  const [sendingEmail, setSendingEmail] = useState(false);
  const [emailFeedback, setEmailFeedback] = useState<string | null>(null);

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
    for (const tx of items) {
      const est = (tx.estatus ?? '').toUpperCase();
      if (est === 'EXITOSA') exitosas++;
      else if (est === 'PENDIENTE_CONCILIACION' || est === 'PROCESANDO') pendientes++;
      else rechazadas++;
    }
    return { total: items.length, exitosas, pendientes, rechazadas };
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
    setEmailFeedback(null);
  };

  const handleCheckLive = async (upc: string) => {
    setCheckingLive(true);
    try {
      const est = await pagoRepository.consultarEstatusPorUpc(upc);
      setLiveStatus(est);
      await cargarHistorial();
      await refreshPerfilYSaldo();
    } catch (err) {
      setError(err);
    } finally {
      setCheckingLive(false);
    }
  };

  const handleResendEmail = async (tx: TransaccionHistorialDto) => {
    const mailErr = BackendValidators.email(emailDest, true);
    if (mailErr) {
      setEmailFeedback(mailErr);
      return;
    }
    setSendingEmail(true);
    setEmailFeedback(null);
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
      setEmailFeedback(res.mensaje || (res.enviado ? 'Comprobante enviado.' : 'Error enviando.'));
    } catch (err) {
      setEmailFeedback(err instanceof Error ? err.message : 'No fue posible enviar el correo');
    } finally {
      setSendingEmail(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gp-primary">Historial de Pagos</h2>
          <p className="text-sm text-gp-muted">
            Operaciones exclusivas de tu cuenta activa sincronizadas entre Web y Flutter
          </p>
        </div>
        <button
          type="button"
          onClick={cargarHistorial}
          disabled={loading}
          className="btn-secondary"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Actualizar historial
        </button>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      {/* Summary Cards */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          { label: 'Total operaciones', value: counts.total, color: 'text-gp-primary' },
          { label: 'Exitosas', value: counts.exitosas, color: 'text-emerald-700' },
          { label: 'Pendientes', value: counts.pendientes, color: 'text-amber-700' },
          { label: 'Rechazadas', value: counts.rechazadas, color: 'text-red-700' },
        ].map((stat) => (
          <div key={stat.label} className="card-surface p-4">
            <p className="text-xs font-medium text-gp-muted">{stat.label}</p>
            <p className={`mt-1 text-2xl font-extrabold ${stat.color}`}>{stat.value}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="card-surface space-y-3 p-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
          <input
            type="search"
            className="input-field pl-10"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Filtrar por servicio, referencia, teléfono, UPC o autorización..."
          />
        </div>
        <div className="flex flex-wrap gap-2">
          {[
            { id: 'TODOS', label: 'Todos' },
            { id: 'EXITOSA', label: 'Exitosas' },
            { id: 'PENDIENTE', label: 'Pendientes' },
            { id: 'RECHAZADA', label: 'Rechazadas / Error' },
          ].map((f) => (
            <button
              key={f.id}
              type="button"
              onClick={() => setStatusFilter(f.id)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
                statusFilter === f.id
                  ? 'bg-gp-primary text-white'
                  : 'bg-slate-100 text-gp-muted hover:bg-slate-200 hover:text-gp-primary'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Transactions List */}
      {loading ? (
        <div className="card-surface flex flex-col items-center justify-center gap-3 py-16 text-gp-muted">
          <Loader2 className="h-8 w-8 animate-spin text-gp-secondary" />
          <p className="text-sm font-medium">Consultando operaciones de la cuenta...</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="card-surface py-16 text-center text-sm text-gp-muted">
          No hay transacciones registradas para el criterio seleccionado.
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((tx) => {
            const refOrTel = tx.referencia || tx.telefono || 'Sin referencia';
            const labelServicio = tx.producto || tx.servicio || `Servicio #${tx.idServicio}`;
            return (
              <div
                key={tx.upc || `${tx.idTransaccionLocal}-${tx.fechaCreacion}`}
                className="card-surface flex flex-col justify-between gap-4 p-4 transition hover:border-sky-300 sm:flex-row sm:items-center"
              >
                <div className="flex items-start gap-3.5">
                  <ServiceLogo
                    urlLogo={tx.urlLogo}
                    alt={tx.servicio || labelServicio}
                    tipoFront={tx.referencia ? 2 : 1}
                  />
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-sm font-bold text-gp-primary">{labelServicio}</h3>
                      <StatusBadge estatus={tx.estatus} />
                    </div>
                    {tx.servicio && tx.producto && (
                      <p className="text-xs font-medium text-gp-muted">{tx.servicio}</p>
                    )}
                    <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gp-muted">
                      <span>
                        Ref/Tel: <strong className="font-mono text-gp-primary">{refOrTel}</strong>
                      </span>
                      <span>
                        UPC: <strong className="font-mono text-gp-primary">{tx.upc || '-'}</strong>
                      </span>
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

                <div className="flex items-center justify-between gap-4 sm:justify-end">
                  <div className="text-left sm:text-right">
                    <p className="text-base font-extrabold text-gp-primary">
                      {formatMxn(tx.monto)}
                    </p>
                    {(tx.comision ?? 0) > 0 && (
                      <p className="text-[11px] text-gp-muted">
                        Comisión: {formatMxn(tx.comision)}
                      </p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => openDetalle(tx)}
                    className="btn-secondary px-3 py-2 text-xs"
                  >
                    <Eye className="h-3.5 w-3.5" />
                    Detalle
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Transaction Detail Modal */}
      {selectedTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
          <div className="flex max-h-[90vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl border border-gp-border bg-white shadow-2xl">
            <div className="flex items-center justify-between border-b border-gp-border px-6 py-4">
              <div className="flex items-center gap-3">
                <ServiceLogo
                  urlLogo={selectedTx.urlLogo}
                  alt={selectedTx.servicio || 'Servicio'}
                  tipoFront={selectedTx.referencia ? 2 : 1}
                />
                <div>
                  <h3 className="text-base font-bold text-gp-primary">
                    {selectedTx.producto || selectedTx.servicio || 'Detalle de Operación'}
                  </h3>
                  <p className="text-xs text-gp-muted">Folio UPC: {selectedTx.upc || 'N/A'}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedTx(null)}
                className="rounded-xl p-1.5 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex-1 space-y-4 overflow-y-auto px-6 py-5 text-sm">
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3">
                <span className="text-gp-muted">Estado registrado</span>
                <StatusBadge estatus={liveStatus?.estatus ?? selectedTx.estatus} />
              </div>

              <div className="space-y-2 rounded-2xl border border-gp-border p-4">
                {[
                  ['Servicio', selectedTx.servicio || `#${selectedTx.idServicio}`],
                  ['Producto', selectedTx.producto || `#${selectedTx.idProducto}`],
                  ['Referencia / Teléfono', selectedTx.referencia || selectedTx.telefono || '-'],
                  ['Importe pagado', formatMxn(selectedTx.monto)],
                  ['Comisión', formatMxn(selectedTx.comision ?? 0)],
                  ['UPC', selectedTx.upc || '-'],
                  ['Número de autorización', selectedTx.numAutorizacion || 'Pendiente / Sin folio'],
                  ['ID Tx GestoPago', selectedTx.idTxGestoPago || '-'],
                  ['Código respuesta', selectedTx.codigoRespuesta || '-'],
                  ['Mensaje proveedor', selectedTx.mensajeRespuesta || '-'],
                  ['Fecha operación', formatFecha(selectedTx.fechaCreacion)],
                  ['Última actualización', formatFecha(selectedTx.fechaActualizacion)],
                ].map(([label, value]) => (
                  <div key={label} className="flex justify-between gap-4 border-b border-slate-100 py-1.5 last:border-0">
                    <span className="text-gp-muted">{label}</span>
                    <span className="text-right font-medium text-gp-primary">{value}</span>
                  </div>
                ))}
              </div>

              {selectedTx.legend && (
                <div className="rounded-xl bg-slate-50 p-3 text-xs text-slate-600">
                  {selectedTx.legend}
                </div>
              )}

              {liveStatus && (
                <div className="rounded-xl border border-sky-200 bg-sky-50 p-3 text-xs text-sky-900">
                  <p className="font-semibold">Estatus actualizado en tiempo real: {liveStatus.estatus}</p>
                  <p className="mt-0.5">{liveStatus.mensaje}</p>
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
                  Consultar estatus actual (GET /pagos/estatus/{selectedTx.upc})
                </button>
              )}

              {(selectedTx.estatus ?? '').toUpperCase() === 'EXITOSA' && (
                <div className="rounded-2xl border border-gp-border p-4">
                  <p className="mb-2 text-xs font-bold uppercase tracking-wide text-gp-muted">
                    Reenviar comprobante digital por correo
                  </p>
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <input
                      type="email"
                      className="input-field flex-1"
                      value={emailDest}
                      onChange={(e) => setEmailDest(e.target.value)}
                      placeholder="correo@ejemplo.com"
                    />
                    <button
                      type="button"
                      disabled={sendingEmail}
                      onClick={() => handleResendEmail(selectedTx)}
                      className="btn-primary shrink-0"
                    >
                      {sendingEmail ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Mail className="h-4 w-4" />
                      )}
                      Enviar
                    </button>
                  </div>
                  {emailFeedback && (
                    <p className="mt-2 text-xs text-gp-secondary">{emailFeedback}</p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
