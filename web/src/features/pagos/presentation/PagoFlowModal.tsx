import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  Clock,
  Eye,
  EyeOff,
  Info,
  Loader2,
  Mail,
  Printer,
  RefreshCw,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { ModalShell } from '../../../core/components/ModalShell';
import { useToast } from '../../../core/components/ToastContext';
import {
  CopyChip,
  ErrorBanner,
  FieldError,
  ServiceLogo,
  StatusBadge,
  ValidationSummary,
} from '../../../core/components/UiPrimitives';
import { ApiException } from '../../../core/network/apiError';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import { useSession } from '../../../core/session/SessionContext';
import { formatFecha, formatMxn, maskCuenta } from '../../../core/utils/formatters';
import {
  BackendValidators,
  collectValidationErrors,
  FieldErrors,
} from '../../../core/validation/backendValidators';
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

const QUICK_AMOUNTS = ['50.00', '100.00', '200.00', '500.00'];

export const PagoFlowModal: React.FC<PagoFlowModalProps> = ({
  open,
  producto,
  onClose,
  onPaymentCompleted,
}) => {
  const { sesion, cliente, cuentaActiva, saldoActual, refreshPerfilYSaldo } = useSession();
  const { notify } = useToast();

  const [referencia, setReferencia] = useState('');
  const [telefono, setTelefono] = useState('');
  const [montoStr, setMontoStr] = useState('');
  const [correoComprobante, setCorreoComprobante] = useState('');

  const [phase, setPhase] = useState<FlowPhase>({ kind: 'captura' });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [summaryMsg, setSummaryMsg] = useState<string | null>(null);
  const [summaryCount, setSummaryCount] = useState<number>(0);
  const [apiError, setApiError] = useState<unknown>(null);
  const [validating, setValidating] = useState(false);

  // Step 2 confirmation state
  const [confirmed, setConfirmed] = useState(false);
  const [twoFaMode, setTwoFaMode] = useState<'token' | 'password'>('token');
  const [twoFaSecret, setTwoFaSecret] = useState('');
  const [showTwoFaPassword, setShowTwoFaPassword] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [submittingPayment, setSubmittingPayment] = useState(false);

  // Step 3 receipt / status state
  const [liveStatus, setLiveStatus] = useState<PagoEstatusResponseDto | null>(null);
  const [checkingLive, setCheckingLive] = useState(false);
  const [emailDest, setEmailDest] = useState('');
  const [emailDestError, setEmailDestError] = useState<string | null>(null);
  const [sendingEmail, setSendingEmail] = useState(false);
  const [emailSentMsg, setEmailSentMsg] = useState<string | null>(null);

  const clearField = (key: string) => {
    if (errors[key]) {
      setErrors((prev) => ({ ...prev, [key]: undefined }));
    }
    if (summaryMsg) {
      setSummaryMsg(null);
    }
  };

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
      setEmailDestError(null);
      setEmailSentMsg(null);
      setPhase({ kind: 'captura' });
      setErrors({});
      setSummaryMsg(null);
      setSummaryCount(0);
      setApiError(null);
      setConfirmed(false);
      setTwoFaMode('token');
      setTwoFaSecret('');
      setShowTwoFaPassword(false);
      setIdempotencyKey(generarIdempotencyKey());
      setLiveStatus(null);
    }
  }, [open, producto, cliente?.correoElectronico, sesion?.username]);

  if (!producto) return null;

  const isTipoRecarga = producto.tipoFront === 1;
  const isFijo = esMontoFijo(producto);
  const isPin = esPinEntretenimiento(producto);
  const disponible = saldoActual?.saldoDisponible ?? cuentaActiva?.saldo?.saldoDisponible ?? 0;

  const currentStepIndex =
    phase.kind === 'captura' ? 0 : phase.kind === 'confirmacion' ? 1 : 2;

  const handleValidateReference = async (e: React.FormEvent) => {
    e.preventDefault();
    setApiError(null);

    if (!cuentaActiva?.numeroCuenta) {
      setSummaryMsg('Selecciona una cuenta activa para realizar el pago.');
      setSummaryCount(1);
      return;
    }

    const resCheck = collectValidationErrors({
      identificador: isTipoRecarga
        ? BackendValidators.telefono(telefono, true)
        : BackendValidators.referenciaPago(referencia, true),
      monto: isFijo
        ? null
        : BackendValidators.montoDecimal(montoStr, 'El importe a pagar', 1.0, 10000.0),
    });

    if (!resCheck.valid) {
      setErrors(resCheck.errors);
      setSummaryMsg(resCheck.firstError);
      setSummaryCount(resCheck.errorCount);
      return;
    }

    const parsedMonto = StrictDecimal.tryParse(montoStr);
    if (!parsedMonto) {
      setErrors({ monto: 'Ingresa un importe válido con máximo 2 decimales.' });
      setSummaryMsg('Ingresa un importe válido con máximo 2 decimales.');
      setSummaryCount(1);
      return;
    }

    setErrors({});
    setSummaryMsg(null);
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
        const msg = res.mensaje || 'La referencia ingresada no es válida para este servicio.';
        setErrors({ identificador: msg });
        setSummaryMsg(msg);
        setSummaryCount(1);
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
    setApiError(null);

    if (!cuentaActiva?.numeroCuenta) {
      setSummaryMsg('Cuenta activa no disponible.');
      setSummaryCount(1);
      return;
    }
    if (validacion.saldoSuficiente === false) {
      setSummaryMsg('Tu saldo disponible es insuficiente para cubrir esta operación.');
      setSummaryCount(1);
      return;
    }

    const twoFactor = twoFaMode === 'token' ? sesion?.tokenSesion ?? '' : twoFaSecret.trim();

    const confirmCheck = collectValidationErrors({
      correoComprobante: correoComprobante.trim()
        ? BackendValidators.email(correoComprobante, false)
        : null,
      twoFaSecret:
        twoFaMode === 'password' && !twoFactor
          ? 'Ingresa tu contraseña de cuenta para autorizar el pago.'
          : null,
      confirmed: !confirmed
        ? 'Marca la casilla de confirmación explícita antes de autorizar el cargo.'
        : null,
    });

    if (!confirmCheck.valid) {
      setErrors(confirmCheck.errors);
      setSummaryMsg(confirmCheck.firstError);
      setSummaryCount(confirmCheck.errorCount);
      return;
    }

    setErrors({});
    setSummaryMsg(null);

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
        notify({
          title: 'Pago aplicado con éxito',
          description: `${producto.producto} · ${formatMxn(outcome.comprobante.montoPagado)}`,
          variant: 'success',
        });
        setPhase({ kind: 'exitoso', comprobante: outcome.comprobante });
      } else {
        notify({
          title: 'Pago en verificación',
          description: outcome.upc ? `Folio UPC: ${outcome.upc}` : outcome.mensaje,
          variant: 'warning',
        });
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
      notify({
        title: 'Estatus actualizado',
        description: `UPC ${upc}: ${est.estatus}`,
        variant: 'info',
      });
    } catch (err) {
      setApiError(err);
    } finally {
      setCheckingLive(false);
    }
  };

  const handleReenviarComprobante = async (
    e: React.FormEvent,
    comp: PagoServicioResponseDto
  ) => {
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
        comision: comp.comision !== undefined ? StrictDecimal.fromNumber(comp.comision) : undefined,
        pin: comp.pin,
        legend: comp.legend,
        fechaProveedor: comp.fechaProveedor,
        horaLocal: comp.horaLocal,
        logoUrl: comp.urlLogo ?? producto.urlLogo ?? undefined,
        tipoFront: producto.tipoFront,
      });
      if (res.enviado !== false) {
        const msg = res.mensaje || 'Comprobante enviado correctamente.';
        setEmailSentMsg(msg);
        notify({ title: 'Comprobante enviado', description: emailDest.trim(), variant: 'success' });
      } else {
        setEmailDestError(res.mensaje || 'No fue posible enviar el comprobante.');
      }
    } catch (err) {
      setEmailDestError(err instanceof Error ? err.message : 'Error al reenviar correo.');
    } finally {
      setSendingEmail(false);
    }
  };

  return (
    <ModalShell
      open={open}
      onClose={onClose}
      preventClose={validating || submittingPayment}
      maxWidthClass="max-w-2xl"
      title={producto.producto}
      subtitle={`${producto.servicio} · ${etiquetaFlujo(producto)}`}
      icon={
        <ServiceLogo
          urlLogo={producto.urlLogo}
          alt={producto.servicio}
          tipoFront={producto.tipoFront}
        />
      }
      subheader={
        <div className="border-b border-slate-100 bg-slate-50/80 px-5 py-3 sm:px-6">
          <div className="grid grid-cols-3 gap-2">
            {['1. Validación', '2. Autorización', '3. Comprobante'].map((label, idx) => {
              const done = idx < currentStepIndex;
              const active = idx === currentStepIndex;
              return (
                <div
                  key={label}
                  className={`flex items-center gap-1.5 rounded-xl border px-2.5 py-1.5 text-xs font-semibold ${
                    active
                      ? 'border-gp-secondary bg-white text-gp-secondary shadow-sm'
                      : done
                      ? 'border-emerald-200 bg-emerald-50/80 text-emerald-700'
                      : 'border-slate-200/70 bg-white/60 text-slate-400'
                  }`}
                >
                  {done && <Check className="h-3.5 w-3.5 shrink-0 text-emerald-600" />}
                  <span className="truncate">{label}</span>
                </div>
              );
            })}
          </div>
        </div>
      }
    >
      <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} className="mb-4" />

      {/* PHASE 1: CAPTURE & VALIDATE REFERENCE */}
      {phase.kind === 'captura' && (
        <form noValidate onSubmit={handleValidateReference} className="space-y-4">
          <ValidationSummary
            message={summaryMsg}
            errorCount={summaryCount}
            onDismiss={() => setSummaryMsg(null)}
          />

          <div className="rounded-2xl border border-gp-border bg-slate-50/90 p-4 text-xs text-slate-700">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span>
                Saldo disponible:{' '}
                <strong className="font-mono text-emerald-700">{formatMxn(disponible)}</strong>
              </span>
              <span className="font-mono text-gp-muted">
                Cuenta cargo: {maskCuenta(cuentaActiva?.numeroCuenta)}
              </span>
            </div>
            {producto.legend && (
              <p className="mt-2.5 flex items-start gap-2 border-t border-slate-200/70 pt-2.5 text-slate-600">
                <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gp-secondary" />
                <span>{producto.legend}</span>
              </p>
            )}
          </div>

          {isTipoRecarga ? (
            <div>
              <label className="input-label">
                Número telefónico celular (10 dígitos) <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field font-mono text-base tracking-wider ${
                  errors.identificador ? 'input-field-error' : ''
                }`}
                inputMode="numeric"
                autoFocus
                maxLength={10}
                value={telefono}
                onChange={(e) => {
                  setTelefono(e.target.value.replace(/\D/g, ''));
                  clearField('identificador');
                }}
                placeholder="5512345678"
              />
              <FieldError message={errors.identificador} />
              {isPin && (
                <p className="mt-1.5 text-xs text-gp-muted">
                  Ingresa el número celular de 10 dígitos al que se asociará tu código digital.
                </p>
              )}
            </div>
          ) : (
            <div>
              <label className="input-label">
                Referencia o línea de captura (alfanumérica){' '}
                <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field font-mono text-base ${
                  errors.identificador ? 'input-field-error' : ''
                }`}
                autoFocus
                maxLength={100}
                value={referencia}
                onChange={(e) => {
                  setReferencia(e.target.value);
                  clearField('identificador');
                }}
                placeholder="Ej. 25720381"
              />
              <FieldError message={errors.identificador} />
            </div>
          )}

          <div>
            <label className="input-label">
              Importe a pagar <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm font-bold text-gp-muted">
                $
              </span>
              <input
                type="text"
                inputMode="decimal"
                className={`input-field pl-8 pr-14 font-mono text-base font-bold ${
                  errors.monto ? 'input-field-error' : ''
                }`}
                value={montoStr}
                disabled={isFijo}
                onChange={(e) => {
                  setMontoStr(e.target.value);
                  clearField('monto');
                }}
                placeholder="100.00"
              />
              <span className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-gp-muted">
                MXN
              </span>
            </div>
            <FieldError message={errors.monto} />

            {!isFijo && (
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <span className="mr-1 text-[11px] font-medium text-gp-muted">Importes rápidos:</span>
                {QUICK_AMOUNTS.map((amt) => (
                  <button
                    key={amt}
                    type="button"
                    onClick={() => {
                      setMontoStr(amt);
                      clearField('monto');
                    }}
                    className={`rounded-lg border px-2.5 py-1 font-mono text-xs font-semibold transition ${
                      montoStr === amt
                        ? 'border-gp-secondary bg-sky-50 text-gp-secondary'
                        : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50'
                    }`}
                  >
                    ${amt}
                  </button>
                ))}
              </div>
            )}

            {!isFijo && (producto.precio ?? 0) > 0 && producto.tipoFront === 2 && (
              <p className="mt-1.5 text-xs text-gp-muted">
                Comisión estimada para este servicio: {formatMxn(producto.precio)}
              </p>
            )}
          </div>

          {producto.urlReferenciaAyuda && (
            <div className="rounded-2xl border border-gp-border bg-slate-50/50 p-3.5 text-xs">
              <p className="mb-2 font-semibold text-gp-muted">
                Guía visual para localizar tu referencia en el recibo:
              </p>
              <img
                src={producto.urlReferenciaAyuda}
                alt="Ayuda visual de referencia"
                className="max-h-40 rounded-xl object-contain"
              />
            </div>
          )}

          <div className="flex justify-end gap-2.5 border-t border-gp-border pt-4">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancelar
            </button>
            <button type="submit" disabled={validating} className="btn-primary">
              {validating ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Validando referencia...
                </>
              ) : (
                <>
                  Validar y continuar
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
          <ValidationSummary
            message={summaryMsg}
            errorCount={summaryCount}
            onDismiss={() => setSummaryMsg(null)}
          />

          <div className="flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50/90 p-4">
            <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
            <div>
              <p className="text-sm font-bold text-emerald-900">Referencia verificada</p>
              <p className="mt-0.5 text-xs text-emerald-800">{phase.validacion.mensaje}</p>
            </div>
          </div>

          <div className="rounded-2xl border border-gp-border bg-slate-50/80 p-4 text-sm">
            <h4 className="mb-3 text-xs font-bold uppercase tracking-wider text-gp-muted">
              Resumen previo al cargo
            </h4>
            <div className="space-y-2 text-xs sm:text-sm">
              <div className="flex justify-between gap-3">
                <span className="text-gp-muted">Servicio / Producto</span>
                <span className="text-right font-semibold text-gp-primary">
                  {phase.validacion.servicio} · {phase.validacion.producto}
                </span>
              </div>
              <div className="flex justify-between gap-3">
                <span className="text-gp-muted">
                  {isTipoRecarga ? 'Teléfono validado' : 'Referencia validada'}
                </span>
                <span className="font-mono font-bold text-gp-primary">
                  {isTipoRecarga
                    ? phase.validacion.telefonoNormalizado
                    : phase.validacion.referenciaNormalizada}
                </span>
              </div>
              <div className="flex justify-between gap-3">
                <span className="text-gp-muted">Cuenta de cargo</span>
                <span className="font-mono text-gp-primary">
                  {phase.validacion.numCuentaEnmascarada}
                </span>
              </div>
              <div className="flex justify-between gap-3">
                <span className="text-gp-muted">Importe del servicio</span>
                <span className="font-mono font-bold text-gp-primary tabular-nums">
                  {formatMxn(phase.validacion.montoValidado)}
                </span>
              </div>
              {(phase.validacion.comisionEstimada ?? 0) > 0 && (
                <div className="flex justify-between gap-3">
                  <span className="text-gp-muted">Comisión estimada</span>
                  <span className="font-mono font-medium text-gp-primary tabular-nums">
                    {formatMxn(phase.validacion.comisionEstimada)}
                  </span>
                </div>
              )}
              <div className="flex justify-between gap-3 border-t border-slate-200/80 pt-2.5">
                <span className="font-semibold text-gp-primary">Saldo disponible actual</span>
                <span
                  className={`font-mono font-bold tabular-nums ${
                    phase.validacion.saldoSuficiente === false
                      ? 'text-rose-600'
                      : 'text-emerald-700'
                  }`}
                >
                  {formatMxn(phase.validacion.saldoDisponibleActual)}
                </span>
              </div>
            </div>
          </div>

          {phase.validacion.saldoSuficiente === false && (
            <div className="rounded-2xl border border-rose-200 bg-rose-50 p-3.5 text-xs font-semibold text-rose-800">
              Tu saldo disponible no es suficiente para cubrir esta operación.
            </div>
          )}

          <div>
            <label className="input-label">
              Correo para recibir comprobante digital{' '}
              <span className="font-normal text-slate-400">(opcional)</span>
            </label>
            <input
              type="email"
              className={`input-field ${errors.correoComprobante ? 'input-field-error' : ''}`}
              value={correoComprobante}
              onChange={(e) => {
                setCorreoComprobante(e.target.value);
                clearField('correoComprobante');
              }}
              placeholder="cliente@dominio.com"
            />
            <FieldError message={errors.correoComprobante} />
          </div>

          {/* 2FA Factor */}
          <div className="rounded-2xl border border-gp-border p-4">
            <div className="mb-2.5 flex items-center gap-2 text-xs font-bold uppercase tracking-wide text-gp-muted">
              <ShieldCheck className="h-4 w-4 text-gp-secondary" />
              Confirmación de seguridad (2FA)
            </div>
            <div className="grid grid-cols-2 gap-2">
              {[
                { id: 'token' as const, label: 'Sesión activa' },
                { id: 'password' as const, label: 'Contraseña de cuenta' },
              ].map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => {
                    setTwoFaMode(opt.id);
                    setTwoFaSecret('');
                    clearField('twoFaSecret');
                  }}
                  className={`rounded-xl border px-3 py-2 text-xs font-semibold transition ${
                    twoFaMode === opt.id
                      ? 'border-gp-secondary bg-sky-50 text-gp-secondary ring-2 ring-gp-secondary/15'
                      : 'border-gp-border bg-white text-gp-muted hover:text-gp-primary'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>

            {twoFaMode === 'token' ? (
              <p className="mt-2.5 text-xs text-gp-muted">
                Se confirmará la operación mediante tu sesión segura actual.
              </p>
            ) : (
              <div className="mt-3">
                <div className="relative">
                  <input
                    type={showTwoFaPassword ? 'text' : 'password'}
                    className={`input-field pr-10 text-xs ${
                      errors.twoFaSecret ? 'input-field-error' : ''
                    }`}
                    value={twoFaSecret}
                    onChange={(e) => {
                      setTwoFaSecret(e.target.value);
                      clearField('twoFaSecret');
                    }}
                    placeholder="Ingresa tu contraseña de cuenta"
                  />
                  <button
                    type="button"
                    onClick={() => setShowTwoFaPassword((prev) => !prev)}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100"
                    aria-label="Mostrar u ocultar contraseña"
                  >
                    {showTwoFaPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
                <FieldError message={errors.twoFaSecret} />
              </div>
            )}
          </div>

          {/* Explicit Confirmation Checkbox */}
          <div>
            <label
              className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-3.5 text-xs transition ${
                errors.confirmed
                  ? 'border-rose-400 bg-rose-50/30 text-gp-primary'
                  : confirmed
                  ? 'border-emerald-300 bg-emerald-50/50 text-gp-primary'
                  : 'border-gp-border bg-slate-50 text-gp-primary hover:bg-slate-100/70'
              }`}
            >
              <input
                type="checkbox"
                checked={confirmed}
                onChange={(e) => {
                  setConfirmed(e.target.checked);
                  clearField('confirmed');
                }}
                className="mt-0.5 h-4 w-4 rounded border-slate-300 text-gp-secondary focus:ring-gp-secondary"
              />
              <span className="leading-relaxed">
                Confirmo que verifiqué el proveedor, referencia e importe de esta operación y
                autorizo el cargo definitivo a mi cuenta.
              </span>
            </label>
            <FieldError message={errors.confirmed} />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-2.5 border-t border-gp-border pt-4">
            <button
              type="button"
              disabled={submittingPayment}
              onClick={() => {
                setErrors({});
                setSummaryMsg(null);
                setPhase({ kind: 'captura' });
              }}
              className="btn-secondary"
            >
              <ArrowLeft className="h-4 w-4" />
              Modificar datos
            </button>
            <button
              type="button"
              disabled={submittingPayment || phase.validacion.saldoSuficiente === false}
              onClick={() => void handleConfirmPayment(phase.validacion)}
              className="btn-primary bg-emerald-600 hover:bg-emerald-700"
            >
              {submittingPayment ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Procesando pago...
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

      {/* PHASE 3A: SUCCESSFUL RECEIPT */}
      {phase.kind === 'exitoso' && (
        <div className="space-y-5">
          <div className="overflow-hidden rounded-3xl border border-emerald-200 bg-gradient-to-b from-emerald-50/90 via-white to-slate-50/80 shadow-sm">
            <div className="border-b border-dashed border-emerald-200 px-6 py-5 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                <CheckCircle2 className="h-7 w-7" />
              </div>
              <h4 className="mt-2.5 text-lg font-extrabold text-emerald-950">
                ¡Pago Aplicado Exitosamente!
              </h4>
              <p className="mt-0.5 text-xs text-emerald-800">
                {phase.comprobante.mensaje || 'Operación autorizada exitosamente.'}
              </p>
              <p className="mt-3 font-display text-3xl font-extrabold tracking-tight text-gp-primary tabular-nums">
                {formatMxn(phase.comprobante.montoPagado)}
              </p>
            </div>

            <div className="space-y-2.5 px-6 py-5 text-xs sm:text-sm">
              <div className="flex items-center justify-between gap-2">
                <span className="text-gp-muted">Folio UPC</span>
                <CopyChip value={phase.comprobante.upc} toastTitle="UPC copiado" />
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-gp-muted">Autorización</span>
                <CopyChip
                  value={phase.comprobante.numAutorizacion}
                  toastTitle="Número de autorización copiado"
                />
              </div>
              {phase.comprobante.idTx && (
                <div className="flex items-center justify-between gap-2">
                  <span className="text-gp-muted">ID de operación</span>
                  <span className="font-mono text-gp-primary">{phase.comprobante.idTx}</span>
                </div>
              )}
              <div className="flex items-center justify-between gap-2">
                <span className="text-gp-muted">Servicio</span>
                <span className="text-right font-semibold text-gp-primary">
                  {phase.comprobante.servicio} · {phase.comprobante.producto}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-gp-muted">Referencia / Teléfono</span>
                <span className="font-mono font-semibold text-gp-primary">
                  {phase.comprobante.referencia || phase.comprobante.telefono || '-'}
                </span>
              </div>
              {(phase.comprobante.comision ?? 0) > 0 && (
                <div className="flex items-center justify-between gap-2">
                  <span className="text-gp-muted">Comisión</span>
                  <span className="font-mono text-gp-primary tabular-nums">
                    {formatMxn(phase.comprobante.comision)}
                  </span>
                </div>
              )}
              {phase.comprobante.pin && (
                <div className="flex items-center justify-between gap-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2">
                  <span className="font-bold text-amber-900">PIN Digital</span>
                  <CopyChip value={phase.comprobante.pin} toastTitle="PIN digital copiado" />
                </div>
              )}
              {phase.comprobante.legend && (
                <p className="rounded-xl border border-slate-200/80 bg-white p-3 text-xs leading-relaxed text-slate-600">
                  {phase.comprobante.legend}
                </p>
              )}
              <div className="flex items-center justify-between gap-2 border-t border-slate-200/80 pt-2.5">
                <span className="font-semibold text-gp-muted">Nuevo saldo disponible</span>
                <span className="font-mono font-bold text-emerald-700 tabular-nums">
                  {formatMxn(phase.comprobante.saldoDisponibleActual)}
                </span>
              </div>
            </div>
          </div>

          {/* Email Receipt Resend */}
          <form
            noValidate
            onSubmit={(e) => void handleReenviarComprobante(e, phase.comprobante)}
            className="rounded-2xl border border-gp-border bg-slate-50/50 p-4"
          >
            <label htmlFor="receipt-email-input" className="input-label">
              Enviar o reenviar comprobante por correo
            </label>
            <div className="flex flex-col gap-2 sm:flex-row">
              <input
                id="receipt-email-input"
                type="email"
                className={`input-field flex-1 ${emailDestError ? 'input-field-error' : ''}`}
                value={emailDest}
                onChange={(e) => {
                  setEmailDest(e.target.value);
                  if (emailDestError) setEmailDestError(null);
                  if (emailSentMsg) setEmailSentMsg(null);
                }}
                placeholder="destinatario@dominio.com"
              />
              <button type="submit" disabled={sendingEmail} className="btn-secondary shrink-0">
                {sendingEmail ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Mail className="h-4 w-4 text-gp-secondary" />
                )}
                Enviar correo
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

          <div className="flex flex-wrap items-center justify-between gap-2">
            <button
              type="button"
              onClick={() => window.print()}
              className="btn-secondary text-xs"
            >
              <Printer className="h-4 w-4" />
              Imprimir comprobante
            </button>
            <button type="button" onClick={onClose} className="btn-primary">
              Finalizar
            </button>
          </div>
        </div>
      )}

      {/* PHASE 3B: PENDING RECONCILIATION */}
      {phase.kind === 'pendiente' && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-center">
            <Clock className="mx-auto h-10 w-10 text-amber-600" />
            <h4 className="mt-2 text-lg font-bold text-amber-950">
              Operación en Verificación Automática
            </h4>
            <p className="mt-1 text-xs leading-relaxed text-amber-800">{phase.mensaje}</p>
          </div>

          <div className="rounded-2xl border border-gp-border bg-slate-50 p-4 text-sm">
            <div className="flex items-center justify-between py-1">
              <span className="text-xs text-gp-muted">Estado actual</span>
              <StatusBadge estatus={liveStatus?.estatus ?? 'PENDIENTE_CONCILIACION'} />
            </div>
            {phase.upc && (
              <div className="mt-2 flex items-center justify-between border-t border-slate-200/70 pt-2">
                <span className="text-xs text-gp-muted">Código UPC de seguimiento</span>
                <CopyChip value={phase.upc} toastTitle="UPC de seguimiento copiado" />
              </div>
            )}
            {liveStatus && (
              <div className="mt-3 space-y-1 border-t border-gp-border pt-3 text-xs">
                <p className="font-semibold text-gp-primary">{liveStatus.mensaje}</p>
                {liveStatus.numAutorizacion && (
                  <p className="font-mono">Autorización: {liveStatus.numAutorizacion}</p>
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
                onClick={() => void handleConsultarEstatusVivo(phase.upc!)}
                className="btn-secondary"
              >
                <RefreshCw className={`h-4 w-4 ${checkingLive ? 'animate-spin' : ''}`} />
                Consultar estatus en vivo
              </button>
            )}
            <button type="button" onClick={onClose} className="btn-primary ml-auto">
              Entendido
            </button>
          </div>
        </div>
      )}

      {/* PHASE 3C: REJECTED OUTCOME */}
      {phase.kind === 'rechazado' && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-center">
            <XCircle className="mx-auto h-10 w-10 text-rose-600" />
            <h4 className="mt-2 text-lg font-bold text-rose-950">Operación Rechazada</h4>
            <p className="mt-1 text-xs leading-relaxed text-rose-800">
              {phase.error instanceof ApiException || phase.error instanceof Error
                ? phase.error.message
                : 'No fue posible procesar el pago.'}
            </p>
          </div>

          <div className="flex items-start gap-2.5 rounded-2xl border border-slate-200 bg-slate-50 p-3.5 text-xs text-slate-700">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
            <span>
              La retención preventiva en tu cuenta fue liberada inmediatamente; no se aplicó ningún
              cargo a tu saldo disponible.
            </span>
          </div>

          <div className="flex justify-between gap-2 pt-1">
            <button
              type="button"
              onClick={() => setPhase({ kind: 'captura' })}
              className="btn-secondary"
            >
              <ArrowLeft className="h-4 w-4" />
              Revisar datos e intentar de nuevo
            </button>
            <button type="button" onClick={onClose} className="btn-primary">
              Cerrar
            </button>
          </div>
        </div>
      )}
    </ModalShell>
  );
};
