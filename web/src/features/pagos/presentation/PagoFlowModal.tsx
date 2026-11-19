import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Clock,
  Info,
  Loader2,
  Mail,
  RefreshCw,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-react';
import { ErrorBanner, ServiceLogo, StatusBadge } from '../../../core/components/UiPrimitives';
import { ApiException } from '../../../core/network/apiError';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import { useSession } from '../../../core/session/SessionContext';
import { formatFecha, formatMxn } from '../../../core/utils/formatters';
import { BackendValidators } from '../../../core/validation/backendValidators';
import {
  esMontoFijo,
  esPinEntretenimiento,
  etiquetaFlujo,
  ProductItemDto,
} from '../../catalogo_productos/data/productoRepository';
import {
  generarIdempotencyKey,
  PagoEstatusResponseDto,
  pagoRepository,
  PagoServicioResponseDto,
  ValidarReferenciaResponseDto,
} from '../data/pagoRepository';

interface PagoFlowModalProps {
  open: boolean;
  producto: ProductItemDto | null;
  onClose: () => void;
  onPaymentCompleted?: () => void;
}

type FlowPhase =
  | { kind: 'captura' }
  | { kind: 'confirmacion'; validacion: ValidarReferenciaResponseDto }
  | { kind: 'exitoso'; comprobante: PagoServicioResponseDto }
  | { kind: 'pendiente'; upc?: string; mensaje: string }
  | { kind: 'rechazado'; error: unknown };

export const PagoFlowModal: React.FC<PagoFlowModalProps> = ({
  open,
  producto,
  onClose,
  onPaymentCompleted,
}) => {
  const { sesion, cliente, cuentaActiva, saldoActual, refreshPerfilYSaldo } = useSession();

  const [referencia, setReferencia] = useState('');
  const [telefono, setTelefono] = useState('');
  const [montoStr, setMontoStr] = useState('');
  const [correoComprobante, setCorreoComprobante] = useState('');

  const [phase, setPhase] = useState<FlowPhase>({ kind: 'captura' });
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<unknown>(null);
  const [validating, setValidating] = useState(false);

  // Step 2 confirmation state
  const [confirmed, setConfirmed] = useState(false);
  const [twoFaMode, setTwoFaMode] = useState<'token' | 'password' | 'biometric'>('token');
  const [twoFaSecret, setTwoFaSecret] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [submittingPayment, setSubmittingPayment] = useState(false);

  // Step 3 receipt / status state
  const [liveStatus, setLiveStatus] = useState<PagoEstatusResponseDto | null>(null);
  const [checkingLive, setCheckingLive] = useState(false);
  const [emailDest, setEmailDest] = useState('');
  const [sendingEmail, setSendingEmail] = useState(false);
  const [emailFeedback, setEmailFeedback] = useState<string | null>(null);

  useEffect(() => {
    if (open && producto) {
      setReferencia('');
      setTelefono('');
      const initialMonto =
        esMontoFijo(producto) && (producto.precio ?? 0) > 0
          ? StrictDecimal.fromNumber(producto.precio ?? 0).toStrictLiteral()
          : '';
      setMontoStr(initialMonto);
      const userEmail = cliente?.correoElectronico ?? sesion?.username ?? '';
      setCorreoComprobante(userEmail);
      setEmailDest(userEmail);
      setPhase({ kind: 'captura' });
      setFieldError(null);
      setApiError(null);
      setConfirmed(false);
      setTwoFaMode('token');
      setTwoFaSecret('');
      setIdempotencyKey(generarIdempotencyKey());
      setLiveStatus(null);
      setEmailFeedback(null);
    }
  }, [open, producto, cliente?.correoElectronico, sesion?.username]);

  if (!open || !producto) return null;

  const isTipoRecarga = producto.tipoFront === 1;
  const isFijo = esMontoFijo(producto);
  const isPin = esPinEntretenimiento(producto);
  const disponible = saldoActual?.saldoDisponible ?? cuentaActiva?.saldo?.saldoDisponible ?? 0;

  const handleValidateReference = async (e: React.FormEvent) => {
    e.preventDefault();
    setFieldError(null);
    setApiError(null);

    if (!cuentaActiva?.numeroCuenta) {
      setFieldError('No hay una cuenta activa seleccionada para realizar el pago.');
      return;
    }

    if (isTipoRecarga) {
      const telErr = BackendValidators.telefono(telefono, true);
      if (telErr) {
        setFieldError(telErr);
        return;
      }
    } else {
      const refErr = BackendValidators.referenciaPago(referencia, true);
      if (refErr) {
        setFieldError(refErr);
        return;
      }
    }

    if (!isFijo) {
      const montoErr = BackendValidators.montoDecimal(montoStr, 'Monto', 1.0, 10000.0);
      if (montoErr) {
        setFieldError(montoErr);
        return;
      }
    }

    const parsedMonto = StrictDecimal.tryParse(montoStr);
    if (!parsedMonto) {
      setFieldError('Ingresa un monto válido con hasta 2 decimales.');
      return;
    }

    setValidating(true);
    try {
      const res = await pagoRepository.validarReferencia({
        numCuenta: cuentaActiva.numeroCuenta,
        idServicio: producto.idServicio,
        idProducto: producto.idProducto,
        referencia: isTipoRecarga ? undefined : referencia.trim(),
        telefono: isTipoRecarga ? telefono.trim() : undefined,
        monto: parsedMonto,
      });

      if (!res.valida) {
        setFieldError(res.mensaje || 'La referencia ingresada no pasó la validación.');
        return;
      }

      setConfirmed(false);
      setIdempotencyKey(generarIdempotencyKey());
      setPhase({ kind: 'confirmacion', validacion: res });
    } catch (err) {
      setApiError(err);
    } finally {
      setValidating(false);
    }
  };

  const handleConfirmPayment = async (validacion: ValidarReferenciaResponseDto) => {
    setFieldError(null);
    setApiError(null);

    if (!cuentaActiva?.numeroCuenta) {
      setFieldError('Cuenta activa no disponible.');
      return;
    }
    if (!confirmed) {
      setFieldError('Debes marcar la confirmación explícita antes de procesar el pago.');
      return;
    }
    if (validacion.saldoSuficiente === false) {
      setFieldError('Saldo disponible insuficiente para cubrir esta operación.');
      return;
    }
    if (correoComprobante.trim()) {
      const mailErr = BackendValidators.email(correoComprobante, false);
      if (mailErr) {
        setFieldError(mailErr);
        return;
      }
    }

    const twoFactor =
      twoFaMode === 'token' ? sesion?.tokenSesion ?? '' : twoFaSecret.trim();
    if (!twoFactor) {
      setFieldError('Proporciona el factor de autenticación 2FA requerido.');
      return;
    }

    const montoToSend =
      StrictDecimal.tryParse(montoStr) ??
      StrictDecimal.fromNumber(validacion.montoValidado ?? producto.precio ?? 0);

    setSubmittingPayment(true);
    try {
      const outcome = await pagoRepository.realizarPago({
        numCuenta: cuentaActiva.numeroCuenta,
        idServicio: producto.idServicio,
        idProducto: producto.idProducto,
        monto: montoToSend,
        telefono: isTipoRecarga ? validacion.telefonoNormalizado ?? telefono : undefined,
        referencia: isTipoRecarga ? undefined : validacion.referenciaNormalizada ?? referencia,
        twoFactor,
        correoElectronico: correoComprobante.trim() || undefined,
        idempotencyKey,
      });

      await refreshPerfilYSaldo();
      onPaymentCompleted?.();

      if (outcome.kind === 'exitoso') {
        setPhase({ kind: 'exitoso', comprobante: outcome.comprobante });
      } else {
        setPhase({
          kind: 'pendiente',
          upc: outcome.upc,
          mensaje: outcome.mensaje,
        });
      }
    } catch (err) {
      try {
        await refreshPerfilYSaldo();
      } catch {
        // Ignore refresh error on rejected payment
      }
      setPhase({ kind: 'rechazado', error: err });
    } finally {
      setSubmittingPayment(false);
    }
  };

  const handleConsultarEstatusVivo = async (upc: string) => {
    setCheckingLive(true);
    setApiError(null);
    try {
      const est = await pagoRepository.consultarEstatusPorUpc(upc);
      setLiveStatus(est);
      await refreshPerfilYSaldo();
      onPaymentCompleted?.();
    } catch (err) {
      setApiError(err);
    } finally {
      setCheckingLive(false);
    }
  };

  const handleReenviarComprobante = async (comp: PagoServicioResponseDto) => {
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
        numAutorizacion: comp.numAutorizacion,
        idTx: comp.idTx,
        upc: comp.upc,
        numCuenta: comp.numCuenta ?? cuentaActiva?.numeroCuenta,
        idServicio: comp.idServicio ?? producto.idServicio,
        servicio: comp.servicio ?? producto.servicio,
        idProducto: comp.idProducto ?? producto.idProducto,
        producto: comp.producto ?? producto.producto,
        telefono: comp.telefono,
        referencia: comp.referencia,
        montoPagado:
          comp.montoPagado !== undefined ? StrictDecimal.fromNumber(comp.montoPagado) : undefined,
        comision:
          comp.comision !== undefined ? StrictDecimal.fromNumber(comp.comision) : undefined,
        legend: comp.legend,
        fechaProveedor: comp.fechaProveedor,
        horaLocal: comp.horaLocal,
        logoUrl: comp.urlLogo ?? producto.urlLogo ?? undefined,
        tipoFront: producto.tipoFront,
      });
      setEmailFeedback(res.mensaje || (res.enviado ? 'Comprobante enviado.' : 'No enviado.'));
    } catch (err) {
      setEmailFeedback(err instanceof Error ? err.message : 'Error al reenviar correo');
    } finally {
      setSendingEmail(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
      <div className="flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-gp-border bg-white shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gp-border px-6 py-4">
          <div className="flex items-center gap-3">
            <ServiceLogo
              urlLogo={producto.urlLogo}
              alt={producto.servicio}
              tipoFront={producto.tipoFront}
            />
            <div>
              <h3 className="text-base font-bold text-gp-primary">{producto.producto}</h3>
              <p className="text-xs text-gp-muted">
                {producto.servicio} · {etiquetaFlujo(producto)}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1.5 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-5">
          {/* Progress indicator during input/confirm */}
          {(phase.kind === 'captura' || phase.kind === 'confirmacion') && (
            <div className="mb-5 grid grid-cols-2 gap-2">
              <div
                className={`rounded-xl border px-3 py-2 text-xs font-semibold ${
                  phase.kind === 'captura'
                    ? 'border-gp-secondary bg-sky-50 text-gp-secondary'
                    : 'border-emerald-200 bg-emerald-50 text-emerald-700'
                }`}
              >
                1. Captura y Validación de Referencia
              </div>
              <div
                className={`rounded-xl border px-3 py-2 text-xs font-semibold ${
                  phase.kind === 'confirmacion'
                    ? 'border-gp-secondary bg-sky-50 text-gp-secondary'
                    : 'border-gp-border bg-slate-50 text-gp-muted'
                }`}
              >
                2. Resumen y Confirmación Explícita
              </div>
            </div>
          )}

          <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} className="mb-4" />

          {/* PHASE 1: CAPTURE & VALIDATE REFERENCE */}
          {phase.kind === 'captura' && (
            <form onSubmit={handleValidateReference} className="space-y-4">
              <div className="rounded-2xl border border-gp-border bg-slate-50 p-4 text-xs text-slate-700">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span>
                    Saldo disponible: <strong className="text-emerald-700">{formatMxn(disponible)}</strong>
                  </span>
                  <span className="font-mono text-gp-muted">
                    Servicio #{producto.idServicio} · Prod #{producto.idProducto}
                  </span>
                </div>
                {producto.legend && (
                  <p className="mt-2 flex items-start gap-1.5 text-slate-600">
                    <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gp-secondary" />
                    <span>{producto.legend}</span>
                  </p>
                )}
              </div>

              {isTipoRecarga ? (
                <div>
                  <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                    Número telefónico (10 dígitos) *
                  </label>
                  <input
                    className="input-field font-mono"
                    maxLength={10}
                    value={telefono}
                    onChange={(e) => {
                      setTelefono(e.target.value.replace(/\D/g, ''));
                      setFieldError(null);
                    }}
                    placeholder="5512345678"
                  />
                  {isPin && (
                    <p className="mt-1 text-xs text-gp-muted">
                      Para PIN / Entretenimiento el backend enviará automáticamente el prefijo `#`
                      requerido por GestoPago.
                    </p>
                  )}
                </div>
              ) : (
                <div>
                  <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                    Referencia / Número de recibo (alfanumérica) *
                  </label>
                  <input
                    className="input-field font-mono"
                    maxLength={100}
                    value={referencia}
                    onChange={(e) => {
                      setReferencia(e.target.value);
                      setFieldError(null);
                    }}
                    placeholder="Ej. 25720381"
                  />
                </div>
              )}

              <div>
                <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                  Importe a pagar MXN *
                </label>
                <input
                  className="input-field font-mono"
                  value={montoStr}
                  disabled={isFijo}
                  onChange={(e) => {
                    setMontoStr(e.target.value);
                    setFieldError(null);
                  }}
                  placeholder="100.00"
                />
                {!isFijo && (producto.precio ?? 0) > 0 && producto.tipoFront === 2 && (
                  <p className="mt-1 text-xs text-gp-muted">
                    Comisión estimada de catálogo para este servicio: {formatMxn(producto.precio)}
                  </p>
                )}
              </div>

              {producto.urlReferenciaAyuda && (
                <div className="rounded-xl border border-gp-border p-3 text-xs">
                  <p className="mb-2 font-semibold text-gp-muted">Ayuda visual para ubicar tu referencia:</p>
                  <img
                    src={producto.urlReferenciaAyuda}
                    alt="Ayuda de referencia"
                    className="max-h-40 rounded-lg object-contain"
                  />
                </div>
              )}

              {fieldError && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-xs font-medium text-red-800">
                  {fieldError}
                </div>
              )}

              <div className="flex justify-end gap-2 border-t border-gp-border pt-4">
                <button type="button" onClick={onClose} className="btn-secondary">
                  Cancelar
                </button>
                <button type="submit" disabled={validating} className="btn-primary">
                  {validating ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Validando con GestoPago...
                    </>
                  ) : (
                    <>
                      Validar referencia y continuar
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>
              </div>
            </form>
          )}

          {/* PHASE 2: VALIDATED SUMMARY & EXPLICIT CONFIRMATION */}
          {phase.kind === 'confirmacion' && (
            <div className="space-y-4">
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                <div className="flex items-center gap-2 text-sm font-bold text-emerald-800">
                  <CheckCircle2 className="h-4 w-4" />
                  Referencia validada correctamente
                </div>
                <p className="mt-1 text-xs text-emerald-700">{phase.validacion.mensaje}</p>
              </div>

              <div className="rounded-2xl border border-gp-border bg-slate-50 p-4 text-sm">
                <h4 className="mb-3 text-xs font-bold uppercase tracking-wider text-gp-muted">
                  Resumen previo de la operación
                </h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Servicio / Producto</span>
                    <span className="font-semibold text-gp-primary">
                      {phase.validacion.servicio} · {phase.validacion.producto}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gp-muted">
                      {isTipoRecarga ? 'Teléfono validado' : 'Referencia validada'}
                    </span>
                    <span className="font-mono font-bold text-gp-primary">
                      {isTipoRecarga
                        ? phase.validacion.telefonoNormalizado
                        : phase.validacion.referenciaNormalizada}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Cuenta origen</span>
                    <span className="font-mono text-gp-primary">
                      {phase.validacion.numCuentaEnmascarada}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Monto validado</span>
                    <span className="font-bold text-gp-primary">
                      {formatMxn(phase.validacion.montoValidado)}
                    </span>
                  </div>
                  {(phase.validacion.comisionEstimada ?? 0) > 0 && (
                    <div className="flex justify-between">
                      <span className="text-gp-muted">Comisión catálogo estimada</span>
                      <span className="font-medium text-gp-primary">
                        {formatMxn(phase.validacion.comisionEstimada)}
                      </span>
                    </div>
                  )}
                  <div className="flex justify-between border-t border-gp-border pt-2">
                    <span className="font-semibold text-gp-primary">Saldo disponible actual</span>
                    <span
                      className={`font-bold ${
                        phase.validacion.saldoSuficiente === false
                          ? 'text-red-600'
                          : 'text-emerald-700'
                      }`}
                    >
                      {formatMxn(phase.validacion.saldoDisponibleActual)}
                    </span>
                  </div>
                </div>
              </div>

              {phase.validacion.saldoSuficiente === false && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs font-medium text-red-800">
                  Saldo insuficiente para completar el cargo. Regresa o ajusta el importe.
                </div>
              )}

              <div>
                <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                  Correo para envío automático del comprobante (opcional)
                </label>
                <input
                  type="email"
                  className="input-field"
                  value={correoComprobante}
                  onChange={(e) => setCorreoComprobante(e.target.value)}
                  placeholder="cliente@dominio.com"
                />
              </div>

              {/* 2FA factor */}
              <div className="rounded-2xl border border-gp-border p-4">
                <div className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-wide text-gp-muted">
                  <ShieldCheck className="h-4 w-4 text-gp-secondary" />
                  Factor de autorización 2FA *
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'token' as const, label: 'Sesión activa' },
                    { id: 'password' as const, label: 'Contraseña' },
                    { id: 'biometric' as const, label: 'Biometría' },
                  ].map((opt) => (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => {
                        setTwoFaMode(opt.id);
                        setTwoFaSecret('');
                      }}
                      className={`rounded-xl border px-2.5 py-2 text-xs font-semibold transition ${
                        twoFaMode === opt.id
                          ? 'border-gp-secondary bg-sky-50 text-gp-secondary'
                          : 'border-gp-border bg-white text-gp-muted hover:text-gp-primary'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>

                {twoFaMode === 'token' ? (
                  <p className="mt-2 text-xs text-gp-muted">
                    Se usará tu token de sesión vigente como segundo factor de autorización.
                  </p>
                ) : (
                  <input
                    type={twoFaMode === 'password' ? 'password' : 'text'}
                    className="input-field mt-3 text-xs"
                    value={twoFaSecret}
                    onChange={(e) => setTwoFaSecret(e.target.value)}
                    placeholder={
                      twoFaMode === 'password'
                        ? 'Ingresa tu contraseña actual'
                        : '[0.12, -0.45, 0.89, -0.05, 0.67]'
                    }
                  />
                )}
              </div>

              {/* Explicit Confirmation */}
              <label className="flex cursor-pointer items-start gap-2.5 rounded-xl border border-gp-border bg-slate-50 p-3.5 text-xs text-gp-primary">
                <input
                  type="checkbox"
                  checked={confirmed}
                  onChange={(e) => {
                    setConfirmed(e.target.checked);
                    setFieldError(null);
                  }}
                  className="mt-0.5 h-4 w-4 rounded border-slate-300 text-gp-secondary focus:ring-gp-secondary"
                />
                <span>
                  Confirmo que verifiqué el proveedor, referencia e importe de esta operación y
                  autorizo realizar el cargo contra mi cuenta.
                </span>
              </label>

              {fieldError && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-xs font-medium text-red-800">
                  {fieldError}
                </div>
              )}

              <div className="flex items-center justify-between border-t border-gp-border pt-4">
                <button
                  type="button"
                  disabled={submittingPayment}
                  onClick={() => setPhase({ kind: 'captura' })}
                  className="btn-secondary"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Editar datos
                </button>
                <button
                  type="button"
                  disabled={
                    submittingPayment || !confirmed || phase.validacion.saldoSuficiente === false
                  }
                  onClick={() => handleConfirmPayment(phase.validacion)}
                  className="btn-primary bg-emerald-600 hover:bg-emerald-700"
                >
                  {submittingPayment ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Procesando pago con GestoPago...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="h-4 w-4" />
                      Confirmar y pagar {formatMxn(phase.validacion.montoValidado)}
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

          {/* PHASE 3A: SUCCESSFUL OUTCOME */}
          {phase.kind === 'exitoso' && (
            <div className="space-y-5">
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-center">
                <CheckCircle2 className="mx-auto h-10 w-10 text-emerald-600" />
                <h4 className="mt-2 text-lg font-bold text-emerald-900">Pago Aplicado con Éxito</h4>
                <p className="mt-1 text-xs text-emerald-800">
                  {phase.comprobante.mensaje || 'Confirmado por el proveedor GestoPago.'}
                </p>
              </div>

              <div className="rounded-2xl border border-gp-border bg-slate-50 p-4 text-sm">
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Folio / UPC</span>
                    <span className="font-mono font-bold text-gp-primary">
                      {phase.comprobante.upc || 'N/A'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Autorización</span>
                    <span className="font-mono font-bold text-gp-secondary">
                      {phase.comprobante.numAutorizacion || 'N/A'}
                    </span>
                  </div>
                  {phase.comprobante.idTx && (
                    <div className="flex justify-between">
                      <span className="text-gp-muted">ID Tx GestoPago</span>
                      <span className="font-mono text-gp-primary">{phase.comprobante.idTx}</span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Servicio</span>
                    <span className="font-semibold text-gp-primary">
                      {phase.comprobante.servicio} · {phase.comprobante.producto}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Referencia / Teléfono</span>
                    <span className="font-mono text-gp-primary">
                      {phase.comprobante.referencia || phase.comprobante.telefono || '-'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gp-muted">Importe pagado</span>
                    <span className="font-bold text-gp-primary">
                      {formatMxn(phase.comprobante.montoPagado)}
                    </span>
                  </div>
                  {(phase.comprobante.comision ?? 0) > 0 && (
                    <div className="flex justify-between">
                      <span className="text-gp-muted">Comisión</span>
                      <span className="text-gp-primary">
                        {formatMxn(phase.comprobante.comision)}
                      </span>
                    </div>
                  )}
                  {phase.comprobante.pin && (
                    <div className="flex justify-between rounded-lg bg-amber-50 px-2.5 py-1.5">
                      <span className="font-semibold text-amber-900">PIN Digital</span>
                      <span className="font-mono font-bold text-amber-900">
                        {phase.comprobante.pin}
                      </span>
                    </div>
                  )}
                  {phase.comprobante.legend && (
                    <p className="rounded-lg bg-white p-2.5 text-xs text-slate-600">
                      {phase.comprobante.legend}
                    </p>
                  )}
                  <div className="flex justify-between border-t border-gp-border pt-2">
                    <span className="text-gp-muted">Saldo disponible actual</span>
                    <span className="font-bold text-emerald-700">
                      {formatMxn(phase.comprobante.saldoDisponibleActual)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Receipt Email Resend */}
              <div className="rounded-2xl border border-gp-border p-4">
                <p className="mb-2 text-xs font-bold uppercase tracking-wide text-gp-muted">
                  Enviar / reenviar comprobante por correo
                </p>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <input
                    type="email"
                    className="input-field flex-1"
                    value={emailDest}
                    onChange={(e) => setEmailDest(e.target.value)}
                    placeholder="destinatario@dominio.com"
                  />
                  <button
                    type="button"
                    disabled={sendingEmail}
                    onClick={() => handleReenviarComprobante(phase.comprobante)}
                    className="btn-secondary shrink-0"
                  >
                    {sendingEmail ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Mail className="h-4 w-4" />
                    )}
                    Enviar correo
                  </button>
                </div>
                {emailFeedback && <p className="mt-2 text-xs text-gp-secondary">{emailFeedback}</p>}
              </div>

              <div className="flex justify-end">
                <button type="button" onClick={onClose} className="btn-primary">
                  Cerrar
                </button>
              </div>
            </div>
          )}

          {/* PHASE 3B: PENDING RECONCILIATION */}
          {phase.kind === 'pendiente' && (
            <div className="space-y-4">
              <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-center">
                <Clock className="mx-auto h-10 w-10 text-amber-600" />
                <h4 className="mt-2 text-lg font-bold text-amber-900">
                  Operación Pendiente de Confirmación
                </h4>
                <p className="mt-1 text-xs text-amber-800">{phase.mensaje}</p>
              </div>

              <div className="rounded-2xl border border-gp-border bg-slate-50 p-4 text-sm">
                <div className="flex items-center justify-between py-1">
                  <span className="text-gp-muted">Estado actual</span>
                  <StatusBadge estatus={liveStatus?.estatus ?? 'PENDIENTE_CONCILIACION'} />
                </div>
                {phase.upc && (
                  <div className="flex items-center justify-between py-1">
                    <span className="text-gp-muted">UPC de seguimiento</span>
                    <span className="font-mono font-bold text-gp-primary">{phase.upc}</span>
                  </div>
                )}
                {liveStatus && (
                  <div className="mt-3 space-y-1 border-t border-gp-border pt-3 text-xs">
                    <p className="font-semibold text-gp-primary">{liveStatus.mensaje}</p>
                    {liveStatus.numAutorizacion && (
                      <p>Autorización: {liveStatus.numAutorizacion}</p>
                    )}
                    <p className="text-gp-muted">
                      Actualizado: {formatFecha(liveStatus.fechaActualizacion)}
                    </p>
                  </div>
                )}
              </div>

              <div className="flex flex-wrap justify-between gap-2 pt-2">
                {phase.upc && (
                  <button
                    type="button"
                    disabled={checkingLive}
                    onClick={() => handleConsultarEstatusVivo(phase.upc!)}
                    className="btn-secondary"
                  >
                    <RefreshCw className={`h-4 w-4 ${checkingLive ? 'animate-spin' : ''}`} />
                    Consultar estatus en vivo
                  </button>
                )}
                <button type="button" onClick={onClose} className="btn-primary">
                  Entendido
                </button>
              </div>
            </div>
          )}

          {/* PHASE 3C: REJECTED OUTCOME */}
          {phase.kind === 'rechazado' && (
            <div className="space-y-4">
              <div className="rounded-2xl border border-red-200 bg-red-50 p-5 text-center">
                <XCircle className="mx-auto h-10 w-10 text-red-600" />
                <h4 className="mt-2 text-lg font-bold text-red-900">Operación Rechazada</h4>
                <p className="mt-1 text-xs text-red-800">
                  {phase.error instanceof ApiException || phase.error instanceof Error
                    ? phase.error.message
                    : 'No fue posible procesar el pago.'}
                </p>
              </div>

              <div className="flex items-start gap-2.5 rounded-xl border border-slate-200 bg-slate-50 p-3.5 text-xs text-slate-700">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                <span>
                  El proveedor rechazó o invalidó el pago y la retención en cuenta fue liberada (no
                  se aplicó ningún cargo exitoso).
                </span>
              </div>

              <div className="flex justify-between gap-2">
                <button
                  type="button"
                  onClick={() => setPhase({ kind: 'captura' })}
                  className="btn-secondary"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Corregir datos
                </button>
                <button type="button" onClick={onClose} className="btn-primary">
                  Cerrar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
