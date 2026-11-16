import 'package:dio/dio.dart';

/// Representa un error o respuesta estructurada (`GenericResponse`) devuelta por el backend.
class ApiException implements Exception {
  const ApiException({
    required this.mensaje,
    this.statusCode,
    this.codigo,
    this.codigoGestoPago,
    this.codigoCatalogo,
    this.origen,
    this.detallesValidacion = const {},
    this.upcPendiente,
  });

  final String mensaje;
  final int? statusCode;
  final int? codigo;
  final String? codigoGestoPago;
  final String? codigoCatalogo;
  final String? origen;
  final Map<String, String> detallesValidacion;
  final String? upcPendiente;

  bool get esPendienteConciliacion =>
      statusCode == 202 ||
      codigo == 504 ||
      codigoCatalogo == 'PAGO_PENDIENTE_CONCILIACION';

  bool get esSesionExpirada =>
      codigo == 402 ||
      codigoCatalogo == 'SESION_EXPIRADA' ||
      (statusCode == 401 &&
          mensaje.toLowerCase().contains('expirado'));

  factory ApiException.fromResponse(int? statusCode, Object? data) {
    if (data is Map<String, dynamic>) {
      final mensaje = (data['mensaje'] ?? data['message'] ?? 'Error de servidor')
          .toString();
      final rawDetalles = data['detallesValidacion'];
      final Map<String, String> detalles = {};
      if (rawDetalles is Map) {
        for (final entry in rawDetalles.entries) {
          detalles[entry.key.toString()] = entry.value.toString();
        }
      }
      final codigoCatalogo = data['codigoCatalogo']?.toString();
      final codigo = data['codigo'] is int
          ? data['codigo'] as int
          : int.tryParse(data['codigo']?.toString() ?? '');

      return ApiException(
        statusCode: statusCode,
        codigo: codigo,
        codigoGestoPago: data['codigoGestoPago']?.toString(),
        codigoCatalogo: codigoCatalogo,
        origen: data['origen']?.toString(),
        mensaje: mensaje,
        detallesValidacion: detalles,
        upcPendiente: extraerUpc(mensaje),
      );
    }

    if (data is String && data.trim().isNotEmpty) {
      return ApiException(statusCode: statusCode, mensaje: data.trim());
    }

    return ApiException(
      statusCode: statusCode,
      mensaje: _fallbackByStatus(statusCode),
    );
  }

  factory ApiException.fromDio(DioException e) {
    if (e.response != null) {
      return ApiException.fromResponse(
        e.response?.statusCode,
        e.response?.data,
      );
    }
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
        return const ApiException(
          mensaje:
              'Tiempo de espera agotado al conectar con el servidor. Verifica tu conexión o URL base.',
        );
      case DioExceptionType.receiveTimeout:
        return const ApiException(
          mensaje:
              'El servidor tardó demasiado en responder. Si realizaste un pago, verifica el historial antes de reintentar.',
        );
      case DioExceptionType.connectionError:
        return const ApiException(
          mensaje:
              'No fue posible conectar con el servidor backend. Verifica que esté encendido y accesible.',
        );
      default:
        return ApiException(
          mensaje: e.message ?? 'Error de red inesperado al comunicar con la API.',
        );
    }
  }

  static String? extraerUpc(String mensaje) {
    final match = RegExp(
      r'\(UPC\)\s*:\s*([A-Za-z0-9]{10})\b|UPC[:\s]+([A-Za-z0-9]{10})\b',
    ).firstMatch(mensaje);
    return match?.group(1) ?? match?.group(2);
  }

  static String _fallbackByStatus(int? statusCode) {
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

  @override
  String toString() => mensaje;
}
