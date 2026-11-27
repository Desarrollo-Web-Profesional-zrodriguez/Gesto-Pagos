import { apiClient } from '../../../core/network/apiClient';
import { ApiException } from '../../../core/network/apiError';
import { StrictDecimal } from '../../../core/network/strictDecimal';

export interface ValidarReferenciaInput {
  numCuenta: string;
  idServicio: number;
  idProducto: number;
  referencia?: string;
  telefono?: string;
  monto?: StrictDecimal;
}

export interface ValidarReferenciaResponseDto {
  valida: boolean;
  mensaje: string;
  idServicio: number;
  servicio: string;
  idProducto: number;
  producto: string;
  tipoFront?: number;
  idCatTipoServicio?: number;
  tipoReferencia?: string;
  hasDigitoVerificador?: boolean;
  legend?: string;
  urlLogo?: string;
  urlReferenciaAyuda?: string;
  referenciaNormalizada?: string;
  telefonoNormalizado?: string;
  precioCatalogo?: number;
  comisionEstimada?: number;
  montoValidado?: number;
  totalEstimado?: number;
  numCuentaEnmascarada?: string;
  saldoDisponibleActual?: number;
  saldoSuficiente?: boolean;
  fechaVerificacion?: string;
}

export interface RealizarPagoInput {
  numCuenta: string;
  idServicio: number;
  idProducto: number;
  monto: StrictDecimal;
  telefono?: string;
  referencia?: string;
  twoFactor: string;
  correoElectronico?: string;
  idempotencyKey?: string;
}

export interface PagoServicioResponseDto {
  codigo?: string;
  mensaje?: string;
  detalleProveedor?: string;
  idTx?: string;
  numAutorizacion?: string;
  upc?: string;
  numCuenta?: string;
  idServicio?: number;
  servicio?: string;
  idProducto?: number;
  producto?: string;
  telefono?: string;
  referencia?: string;
  montoPagado?: number;
  comision?: number;
  saldoAnterior?: number;
  saldoDisponibleActual?: number;
  saldoContableActual?: number;
  pin?: string;
  legend?: string;
  horaLocal?: string;
  fechaProveedor?: string;
  urlLogo?: string;
  emailEnviado?: boolean;
  emailMessageId?: string;
}

export type PagoOutcome =
  | { kind: 'exitoso'; comprobante: PagoServicioResponseDto }
  | { kind: 'pendiente'; upc?: string; mensaje: string };

export interface PagoEstatusResponseDto {
  upc: string;
  estatus: string;
  definitivo: boolean;
  mensaje?: string;
  monto?: number;
  numAutorizacion?: string;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface TransaccionHistorialDto {
  idTransaccionLocal?: number;
  upc?: string;
  numeroCuenta?: string;
  idServicio?: number;
  idProducto?: number;
  telefono?: string;
  referencia?: string;
  monto?: number;
  horaLocal?: string;
  unidad?: string;
  codigoRespuesta?: string;
  mensajeRespuesta?: string;
  idTxGestoPago?: string;
  numAutorizacion?: string;
  comision?: number;
  legend?: string;
  estatus?: string;
  intentosConciliacion?: number;
  fechaCreacion?: string;
  fechaActualizacion?: string;
  servicio?: string;
  producto?: string;
  urlLogo?: string;
}

export interface ComprobantePagoEmailInput {
  destinatarioEmail: string;
  destinatarioNombre?: string;
  numAutorizacion?: string;
  idTx?: string;
  upc?: string;
  numCuenta?: string;
  idServicio?: number;
  servicio?: string;
  idProducto?: number;
  producto?: string;
  telefono?: string;
  referencia?: string;
  montoPagado?: StrictDecimal;
  comision?: StrictDecimal;
  pin?: string;
  legend?: string;
  fechaProveedor?: string;
  horaLocal?: string;
  logoUrl?: string;
  tipoFront?: number;
}

export interface ComprobanteEmailResultDto {
  enviado: boolean;
  messageId?: string;
  mensaje?: string;
  errorDetalle?: string;
}

export function generarIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const rand = Math.random().toString(36).slice(2, 10);
  return `gp-web-${Date.now()}-${rand}`;
}

export const pagoRepository = {
  async validarReferencia(input: ValidarReferenciaInput): Promise<ValidarReferenciaResponseDto> {
    const payload: Record<string, unknown> = {
      numCuenta: input.numCuenta.trim(),
      idServicio: input.idServicio,
      idProducto: input.idProducto,
      referencia: input.referencia?.trim() || undefined,
      telefono: input.telefono?.trim() || undefined,
      monto: input.monto,
    };
    const res = await apiClient.post<ValidarReferenciaResponseDto>(
      '/pagos/validar-referencia',
      payload
    );
    return res.data;
  },

  async realizarPago(input: RealizarPagoInput): Promise<PagoOutcome> {
    const key = input.idempotencyKey?.trim() || generarIdempotencyKey();
    const payload: Record<string, unknown> = {
      numCuenta: input.numCuenta.trim(),
      idServicio: input.idServicio,
      idProducto: input.idProducto,
      monto: input.monto,
      telefono: input.telefono?.trim() || undefined,
      referencia: input.referencia?.trim() || undefined,
      '2fa': input.twoFactor.trim(),
      correoElectronico: input.correoElectronico?.trim() || undefined,
    };

    try {
      const res = await apiClient.post<Record<string, unknown>>('/pagos', payload, {
        headers: { 'Idempotency-Key': key },
        timeoutMs: 75000,
      });

      if (
        res.status === 202 ||
        res.data?.codigo === 504 ||
        res.data?.codigoCatalogo === 'PAGO_PENDIENTE_CONCILIACION'
      ) {
        const ex = ApiException.fromResponse(res.status, res.data);
        return {
          kind: 'pendiente',
          upc: ex.upcPendiente,
          mensaje: ex.message,
        };
      }

      return {
        kind: 'exitoso',
        comprobante: res.data as unknown as PagoServicioResponseDto,
      };
    } catch (err) {
      if (err instanceof ApiException && err.esPendienteConciliacion) {
        return {
          kind: 'pendiente',
          upc: err.upcPendiente,
          mensaje: err.message,
        };
      }
      throw err;
    }
  },

  async consultarEstatusPorUpc(upc: string): Promise<PagoEstatusResponseDto> {
    const res = await apiClient.get<PagoEstatusResponseDto>(
      `/pagos/estatus/${encodeURIComponent(upc.trim())}`
    );
    return res.data;
  },

  async obtenerHistorial(numeroCuenta: string): Promise<TransaccionHistorialDto[]> {
    const res = await apiClient.post<TransaccionHistorialDto[]>('/pagos/historial', {
      numeroCuenta: numeroCuenta.trim(),
    });
    return Array.isArray(res.data) ? res.data : [];
  },

  async enviarComprobanteEmail(
    dto: ComprobantePagoEmailInput
  ): Promise<ComprobanteEmailResultDto> {
    const res = await apiClient.post<ComprobanteEmailResultDto>('/pagos/enviar-comprobante', {
      ...dto,
      destinatarioEmail: dto.destinatarioEmail.trim(),
    });
    return res.data;
  },
};
