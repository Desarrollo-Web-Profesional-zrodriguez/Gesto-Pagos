export interface ApiExceptionParams {
  mensaje: string;
  statusCode?: number;
  codigo?: number;
  codigoGestoPago?: string;
  codigoCatalogo?: string;
  origen?: string;
  detallesValidacion?: Record<string, string>;
  upcPendiente?: string;
}

export class ApiException extends Error {
  readonly statusCode?: number;
  readonly codigo?: number;
  readonly codigoGestoPago?: string;
  readonly codigoCatalogo?: string;
  readonly origen?: string;
  readonly detallesValidacion: Record<string, string>;
  readonly upcPendiente?: string;

  constructor(params: ApiExceptionParams) {
    super(params.mensaje);
    this.name = 'ApiException';
    this.statusCode = params.statusCode;
    this.codigo = params.codigo;
    this.codigoGestoPago = params.codigoGestoPago;
    this.codigoCatalogo = params.codigoCatalogo;
    this.origen = params.origen;
    this.detallesValidacion = params.detallesValidacion ?? {};
    this.upcPendiente = params.upcPendiente;
  }

  get esPendienteConciliacion(): boolean {
    return (
      this.statusCode === 202 ||
      this.codigo === 504 ||
      this.codigoCatalogo === 'PAGO_PENDIENTE_CONCILIACION'
    );
  }

  get esSesionExpirada(): boolean {
    return (
      this.codigo === 402 ||
      this.codigoCatalogo === 'SESION_EXPIRADA' ||
      (this.statusCode === 401 && this.message.toLowerCase().includes('expirado'))
    );
  }

  static extraerUpc(mensaje: string): string | undefined {
    const match = /\(UPC\)\s*:\s*([A-Za-z0-9]{10})\b|UPC[:\s]+([A-Za-z0-9]{10})\b/.exec(
      mensaje
    );
    return match ? match[1] || match[2] : undefined;
  }

  static fromResponse(statusCode: number | undefined, data: unknown): ApiException {
    if (data && typeof data === 'object' && !Array.isArray(data)) {
      const record = data as Record<string, unknown>;
      const mensaje = String(record.mensaje ?? record.message ?? 'Error de servidor');
      const detalles: Record<string, string> = {};
      if (record.detallesValidacion && typeof record.detallesValidacion === 'object') {
        for (const [k, v] of Object.entries(record.detallesValidacion as Record<string, unknown>)) {
          detalles[k] = String(v);
        }
      }
      const rawCodigo = record.codigo;
      const codigo =
        typeof rawCodigo === 'number'
          ? rawCodigo
          : rawCodigo !== undefined && rawCodigo !== null
          ? Number.parseInt(String(rawCodigo), 10) || undefined
          : undefined;

      return new ApiException({
        statusCode,
        codigo,
        codigoGestoPago: record.codigoGestoPago ? String(record.codigoGestoPago) : undefined,
        codigoCatalogo: record.codigoCatalogo ? String(record.codigoCatalogo) : undefined,
        origen: record.origen ? String(record.origen) : undefined,
        mensaje,
        detallesValidacion: detalles,
        upcPendiente: ApiException.extraerUpc(mensaje),
      });
    }

    if (typeof data === 'string' && data.trim().length > 0) {
      return new ApiException({ statusCode, mensaje: data.trim() });
    }

    return new ApiException({
      statusCode,
      mensaje: fallbackByStatus(statusCode),
    });
  }
}

function fallbackByStatus(statusCode?: number): string {
  switch (statusCode) {
    case 400:
      return 'Datos inválidos en la petición.';
    case 401:
      return 'Autenticación fallida o sesión inválida.';
    case 403:
      return 'Acceso denegado o usuario inactivo.';
    case 404:
      return 'Recurso no encontrado.';
    case 409:
      return 'Conflicto o solicitud duplicada.';
    case 422:
      return 'Operación no procesable (saldo insuficiente o monto inválido).';
    case 502:
      return 'Falla de comunicación con el proveedor GestoPago.';
    case 503:
      return 'Catálogo o servicio temporalmente no disponible.';
    default:
      return 'Ocurrió un error al procesar la solicitud.';
  }
}
