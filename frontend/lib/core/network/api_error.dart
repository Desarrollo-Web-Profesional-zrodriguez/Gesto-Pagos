import 'package:dio/dio.dart';

/// Representa un error o respuesta estructurada devuelta por el servicio.
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
      var mensaje =
          (data['mensaje'] ?? data['message'] ?? 'No se pudo completar la operación')
              .toString();
      final rawDetalles = data['detallesValidacion'];
      final Map<String, String> detalles = {};
      if (rawDetalles is Map) {
        for (final entry in rawDetalles.entries) {
          detalles[entry.key.toString()] = entry.value.toString();
        }
      }
      if (detalles.isNotEmpty &&
          mensaje.startsWith('Datos de entrada no validos en la peticion:')) {
        mensaje = 'Por favor revisa la información ingresada en los campos marcados.';
      }
      mensaje = _commercializeMessage(mensaje);
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
      return ApiException(
        statusCode: statusCode,
        mensaje: _commercializeMessage(data.trim()),
      );
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
              'Tiempo de espera agotado al conectar con el servicio. Verifica tu conexión a internet.',
        );
      case DioExceptionType.receiveTimeout:
        return const ApiException(
          mensaje:
              'El servicio tardó demasiado en responder. Si realizaste un pago, verifica tu historial antes de reintentar.',
        );
      case DioExceptionType.connectionError:
        return const ApiException(
          mensaje:
              'No fue posible establecer conexión en este momento. Verifica tu acceso a internet e intenta nuevamente.',
        );
      default:
        return const ApiException(
          mensaje: 'Error de conexión inesperado. Intenta nuevamente.',
        );
    }
  }

  static String? extraerUpc(String mensaje) {
    final match = RegExp(
      r'\(UPC\)\s*:\s*([A-Za-z0-9]{10})\b|UPC[:\s]+([A-Za-z0-9]{10})\b',
    ).firstMatch(mensaje);
    return match?.group(1) ?? match?.group(2);
  }

  static String _commercializeMessage(String raw) {
    final lower = raw.toLowerCase();
    if (lower.contains('no tiene registrado perfil biometrico facial')) {
      return 'Tu cuenta aún no tiene reconocimiento facial activo. Inicia sesión con tu contraseña y actívalo desde Mi Perfil.';
    }
    if (lower.contains('biometrico facial es distinto')) {
      return 'No pudimos confirmar tu identidad facial. Asegúrate de contar con buena iluminación y mirar de frente.';
    }
    if (lower.contains('proporcionar contrasena o')) {
      return 'Ingresa tu contraseña o verifica tu identidad facial para continuar.';
    }
    if (lower.contains('validacion 2fa fallida')) {
      return 'No se pudo autorizar la operación. Verifica tu identidad o contraseña e intenta nuevamente.';
    }
    return raw;
  }

  static String _fallbackByStatus(int? statusCode) {
    switch (statusCode) {
      case 400:
        return 'Revisa los datos ingresados.';
      case 401:
        return 'Credenciales inválidas o sesión expirada.';
      case 403:
        return 'Acceso restringido o cuenta inactiva.';
      case 404:
        return 'No se encontró el registro solicitado.';
      case 409:
        return 'Ya existe una operación o registro equivalente.';
      case 422:
        return 'Operación no procesable (saldo insuficiente o monto inválido).';
      case 502:
      case 503:
        return 'Servicio temporalmente no disponible. Intenta más tarde.';
      default:
        return 'Ocurrió un inconveniente al procesar tu solicitud.';
    }
  }

  @override
  String toString() => mensaje;
}
