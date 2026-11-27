import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';

class ValidarReferenciaRequestDto {
  const ValidarReferenciaRequestDto({
    required this.numCuenta,
    required this.idServicio,
    required this.idProducto,
    this.referencia,
    this.telefono,
    this.monto,
  });

  final String numCuenta;
  final int idServicio;
  final int idProducto;
  final String? referencia;
  final String? telefono;
  final StrictDecimal? monto;

  Map<String, dynamic> toStrictJsonMap() => {
        'numCuenta': numCuenta.trim(),
        'idServicio': idServicio,
        'idProducto': idProducto,
        if (referencia != null && referencia!.trim().isNotEmpty)
          'referencia': referencia!.trim(),
        if (telefono != null && telefono!.trim().isNotEmpty)
          'telefono': telefono!.trim(),
        if (monto != null) 'monto': monto,
      };
}

class ValidarReferenciaResponseDto {
  const ValidarReferenciaResponseDto({
    required this.valida,
    required this.mensaje,
    required this.idServicio,
    required this.servicio,
    required this.idProducto,
    required this.producto,
    this.tipoFront,
    this.idCatTipoServicio,
    this.tipoReferencia,
    this.hasDigitoVerificador,
    this.legend,
    this.urlLogo,
    this.urlReferenciaAyuda,
    this.referenciaNormalizada,
    this.telefonoNormalizado,
    this.precioCatalogo,
    this.comisionEstimada,
    this.montoValidado,
    this.totalEstimado,
    this.numCuentaEnmascarada,
    this.saldoDisponibleActual,
    this.saldoSuficiente,
    this.fechaVerificacion,
  });

  final bool valida;
  final String mensaje;
  final int idServicio;
  final String servicio;
  final int idProducto;
  final String producto;
  final int? tipoFront;
  final int? idCatTipoServicio;
  final String? tipoReferencia;
  final bool? hasDigitoVerificador;
  final String? legend;
  final String? urlLogo;
  final String? urlReferenciaAyuda;
  final String? referenciaNormalizada;
  final String? telefonoNormalizado;
  final double? precioCatalogo;
  final double? comisionEstimada;
  final double? montoValidado;
  final double? totalEstimado;
  final String? numCuentaEnmascarada;
  final double? saldoDisponibleActual;
  final bool? saldoSuficiente;
  final String? fechaVerificacion;

  factory ValidarReferenciaResponseDto.fromJson(Map<String, dynamic> json) =>
      ValidarReferenciaResponseDto(
        valida: json['valida'] == true,
        mensaje: json['mensaje']?.toString() ?? '',
        idServicio: (json['idServicio'] as num?)?.toInt() ?? 0,
        servicio: json['servicio']?.toString() ?? '',
        idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
        producto: json['producto']?.toString() ?? '',
        tipoFront: (json['tipoFront'] as num?)?.toInt(),
        idCatTipoServicio: (json['idCatTipoServicio'] as num?)?.toInt(),
        tipoReferencia: json['tipoReferencia']?.toString(),
        hasDigitoVerificador: json['hasDigitoVerificador'] as bool?,
        legend: json['legend']?.toString(),
        urlLogo: json['urlLogo']?.toString(),
        urlReferenciaAyuda: json['urlReferenciaAyuda']?.toString(),
        referenciaNormalizada: json['referenciaNormalizada']?.toString(),
        telefonoNormalizado: json['telefonoNormalizado']?.toString(),
        precioCatalogo: (json['precioCatalogo'] as num?)?.toDouble(),
        comisionEstimada: (json['comisionEstimada'] as num?)?.toDouble(),
        montoValidado: (json['montoValidado'] as num?)?.toDouble(),
        totalEstimado: (json['totalEstimado'] as num?)?.toDouble(),
        numCuentaEnmascarada: json['numCuentaEnmascarada']?.toString(),
        saldoDisponibleActual:
            (json['saldoDisponibleActual'] as num?)?.toDouble(),
        saldoSuficiente: json['saldoSuficiente'] as bool?,
        fechaVerificacion: json['fechaVerificacion']?.toString(),
      );
}

class PagoServicioRequestDto {
  const PagoServicioRequestDto({
    required this.numCuenta,
    required this.idServicio,
    required this.idProducto,
    required this.monto,
    this.telefono,
    this.referencia,
    required this.codigo2fa,
    this.correoElectronico,
  });

  final String numCuenta;
  final int idServicio;
  final int idProducto;
  final StrictDecimal monto;
  final String? telefono;
  final String? referencia;
  final String codigo2fa;
  final String? correoElectronico;

  Map<String, dynamic> toStrictJsonMap() => {
        'numCuenta': numCuenta.trim(),
        'idServicio': idServicio,
        'idProducto': idProducto,
        'monto': monto,
        if (telefono != null && telefono!.trim().isNotEmpty)
          'telefono': telefono!.trim(),
        if (referencia != null && referencia!.trim().isNotEmpty)
          'referencia': referencia!.trim(),
        '2fa': codigo2fa.trim(),
        if (correoElectronico != null && correoElectronico!.trim().isNotEmpty)
          'correoElectronico': correoElectronico!.trim(),
      };
}

class PagoServicioResponseDto {
  const PagoServicioResponseDto({
    required this.codigo,
    required this.mensaje,
    this.detalleProveedor,
    this.idTx,
    this.numAutorizacion,
    required this.upc,
    required this.numCuenta,
    required this.idServicio,
    required this.servicio,
    required this.idProducto,
    required this.producto,
    this.telefono,
    this.referencia,
    required this.montoPagado,
    this.comision,
    this.saldoAnterior,
    this.saldoDisponibleActual,
    this.saldoContableActual,
    this.pin,
    this.legend,
    this.horaLocal,
    this.fechaProveedor,
    this.urlLogo,
    this.emailEnviado,
    this.emailMessageId,
  });

  final String codigo;
  final String mensaje;
  final String? detalleProveedor;
  final String? idTx;
  final String? numAutorizacion;
  final String upc;
  final String numCuenta;
  final int idServicio;
  final String servicio;
  final int idProducto;
  final String producto;
  final String? telefono;
  final String? referencia;
  final double montoPagado;
  final double? comision;
  final double? saldoAnterior;
  final double? saldoDisponibleActual;
  final double? saldoContableActual;
  final String? pin;
  final String? legend;
  final String? horaLocal;
  final String? fechaProveedor;
  final String? urlLogo;
  final bool? emailEnviado;
  final String? emailMessageId;

  factory PagoServicioResponseDto.fromJson(Map<String, dynamic> json) =>
      PagoServicioResponseDto(
        codigo: json['codigo']?.toString() ?? '',
        mensaje: json['mensaje']?.toString() ?? '',
        detalleProveedor: json['detalleProveedor']?.toString(),
        idTx: json['idTx']?.toString(),
        numAutorizacion: json['numAutorizacion']?.toString(),
        upc: json['upc']?.toString() ?? '',
        numCuenta: json['numCuenta']?.toString() ?? '',
        idServicio: (json['idServicio'] as num?)?.toInt() ?? 0,
        servicio: json['servicio']?.toString() ?? '',
        idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
        producto: json['producto']?.toString() ?? '',
        telefono: json['telefono']?.toString(),
        referencia: json['referencia']?.toString(),
        montoPagado: (json['montoPagado'] as num?)?.toDouble() ?? 0.0,
        comision: (json['comision'] as num?)?.toDouble(),
        saldoAnterior: (json['saldoAnterior'] as num?)?.toDouble(),
        saldoDisponibleActual:
            (json['saldoDisponibleActual'] as num?)?.toDouble(),
        saldoContableActual:
            (json['saldoContableActual'] as num?)?.toDouble(),
        pin: json['pin']?.toString(),
        legend: json['legend']?.toString(),
        horaLocal: json['horaLocal']?.toString(),
        fechaProveedor: json['fechaProveedor']?.toString(),
        urlLogo: json['urlLogo']?.toString(),
        emailEnviado: json['emailEnviado'] as bool?,
        emailMessageId: json['emailMessageId']?.toString(),
      );
}

sealed class PagoExecutionResult {
  const PagoExecutionResult();
}

class PagoExitosoResult extends PagoExecutionResult {
  const PagoExitosoResult(this.comprobante);
  final PagoServicioResponseDto comprobante;
}

class PagoPendienteConciliacionResult extends PagoExecutionResult {
  const PagoPendienteConciliacionResult({
    required this.upc,
    required this.mensaje,
  });

  final String? upc;
  final String mensaje;
}

class PagoEstatusDto {
  const PagoEstatusDto({
    required this.upc,
    required this.estatus,
    required this.definitivo,
    required this.mensaje,
    this.monto,
    this.numAutorizacion,
    this.fechaCreacion,
    this.fechaActualizacion,
  });

  final String upc;
  final String estatus;
  final bool definitivo;
  final String mensaje;
  final double? monto;
  final String? numAutorizacion;
  final String? fechaCreacion;
  final String? fechaActualizacion;

  factory PagoEstatusDto.fromJson(Map<String, dynamic> json) => PagoEstatusDto(
        upc: json['upc']?.toString() ?? '',
        estatus: json['estatus']?.toString() ?? '',
        definitivo: json['definitivo'] == true,
        mensaje: json['mensaje']?.toString() ?? '',
        monto: (json['monto'] as num?)?.toDouble(),
        numAutorizacion: json['numAutorizacion']?.toString(),
        fechaCreacion: json['fechaCreacion']?.toString(),
        fechaActualizacion: json['fechaActualizacion']?.toString(),
      );
}

class TransaccionHistorialDto {
  const TransaccionHistorialDto({
    this.idTransaccionLocal,
    required this.upc,
    required this.numeroCuenta,
    required this.idServicio,
    required this.idProducto,
    required this.telefono,
    required this.referencia,
    required this.monto,
    required this.estatus,
    this.codigoRespuesta,
    this.mensajeRespuesta,
    this.idTxGestoPago,
    this.numAutorizacion,
    this.comision,
    this.legend,
    this.horaLocal,
    this.intentosConciliacion = 0,
    this.fechaCreacion,
    this.fechaActualizacion,
    this.servicio,
    this.producto,
    this.urlLogo,
  });

  final int? idTransaccionLocal;
  final String upc;
  final String numeroCuenta;
  final int idServicio;
  final int idProducto;
  final String telefono;
  final String referencia;
  final double monto;
  final String estatus;
  final String? codigoRespuesta;
  final String? mensajeRespuesta;
  final String? idTxGestoPago;
  final String? numAutorizacion;
  final double? comision;
  final String? legend;
  final String? horaLocal;
  final int intentosConciliacion;
  final String? fechaCreacion;
  final String? fechaActualizacion;
  final String? servicio;
  final String? producto;
  final String? urlLogo;

  bool get estaSinResolver =>
      estatus == 'PROCESANDO' || estatus == 'PENDIENTE_CONCILIACION';

  factory TransaccionHistorialDto.fromJson(Map<String, dynamic> json) =>
      TransaccionHistorialDto(
        idTransaccionLocal: (json['idTransaccionLocal'] as num?)?.toInt(),
        upc: json['upc']?.toString() ?? '',
        numeroCuenta: json['numeroCuenta']?.toString() ?? '',
        idServicio: (json['idServicio'] as num?)?.toInt() ?? 0,
        idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
        telefono: json['telefono']?.toString() ?? '',
        referencia: json['referencia']?.toString() ?? '',
        monto: (json['monto'] as num?)?.toDouble() ?? 0.0,
        estatus: json['estatus']?.toString() ?? '',
        codigoRespuesta: json['codigoRespuesta']?.toString(),
        mensajeRespuesta: json['mensajeRespuesta']?.toString(),
        idTxGestoPago: json['idTxGestoPago']?.toString(),
        numAutorizacion: json['numAutorizacion']?.toString(),
        comision: (json['comision'] as num?)?.toDouble(),
        legend: json['legend']?.toString(),
        horaLocal: json['horaLocal']?.toString(),
        intentosConciliacion:
            (json['intentosConciliacion'] as num?)?.toInt() ?? 0,
        fechaCreacion: json['fechaCreacion']?.toString(),
        fechaActualizacion: json['fechaActualizacion']?.toString(),
        servicio: json['servicio']?.toString(),
        producto: json['producto']?.toString(),
        urlLogo: json['urlLogo']?.toString(),
      );
}

class ComprobanteEmailDto {
  const ComprobanteEmailDto({
    required this.destinatarioEmail,
    this.destinatarioNombre,
    this.numAutorizacion,
    this.idTx,
    this.upc,
    this.numCuenta,
    this.idServicio,
    this.servicio,
    this.idProducto,
    this.producto,
    this.telefono,
    this.referencia,
    this.montoPagado,
    this.comision,
    this.legend,
    this.fechaProveedor,
    this.horaLocal,
    this.logoUrl,
    this.tipoFront,
  });

  final String destinatarioEmail;
  final String? destinatarioNombre;
  final String? numAutorizacion;
  final String? idTx;
  final String? upc;
  final String? numCuenta;
  final int? idServicio;
  final String? servicio;
  final int? idProducto;
  final String? producto;
  final String? telefono;
  final String? referencia;
  final double? montoPagado;
  final double? comision;
  final String? legend;
  final String? fechaProveedor;
  final String? horaLocal;
  final String? logoUrl;
  final int? tipoFront;

  Map<String, dynamic> toJson() => {
        'destinatarioEmail': destinatarioEmail.trim(),
        if (destinatarioNombre != null)
          'destinatarioNombre': destinatarioNombre,
        if (numAutorizacion != null) 'numAutorizacion': numAutorizacion,
        if (idTx != null) 'idTx': idTx,
        if (upc != null) 'upc': upc,
        if (numCuenta != null) 'numCuenta': numCuenta,
        if (idServicio != null) 'idServicio': idServicio,
        if (servicio != null) 'servicio': servicio,
        if (idProducto != null) 'idProducto': idProducto,
        if (producto != null) 'producto': producto,
        if (telefono != null) 'telefono': telefono,
        if (referencia != null) 'referencia': referencia,
        if (montoPagado != null) 'montoPagado': montoPagado,
        if (comision != null) 'comision': comision,
        if (legend != null) 'legend': legend,
        if (fechaProveedor != null) 'fechaProveedor': fechaProveedor,
        if (horaLocal != null) 'horaLocal': horaLocal,
        if (logoUrl != null) 'logoUrl': logoUrl,
        if (tipoFront != null) 'tipoFront': tipoFront,
      };
}

class ComprobanteEmailResultDto {
  const ComprobanteEmailResultDto({
    required this.enviado,
    this.messageId,
    required this.mensaje,
    this.errorDetalle,
  });

  final bool enviado;
  final String? messageId;
  final String mensaje;
  final String? errorDetalle;

  factory ComprobanteEmailResultDto.fromJson(Map<String, dynamic> json) =>
      ComprobanteEmailResultDto(
        enviado: json['enviado'] == true,
        messageId: json['messageId']?.toString(),
        mensaje: json['mensaje']?.toString() ?? '',
        errorDetalle: json['errorDetalle']?.toString(),
      );
}

class PagoRepository {
  PagoRepository(this._api);

  final ApiClient _api;

  Future<ValidarReferenciaResponseDto> validarReferencia(
    ValidarReferenciaRequestDto request,
  ) async {
    final res = await _api.post(
      '/pagos/validar-referencia',
      body: request.toStrictJsonMap(),
    );
    return ValidarReferenciaResponseDto.fromJson(
      res.data as Map<String, dynamic>,
    );
  }

  Future<PagoExecutionResult> realizarPago(
    PagoServicioRequestDto request, {
    required String idempotencyKey,
  }) async {
    final res = await _api.post(
      '/pagos',
      body: request.toStrictJsonMap(),
      headers: {'Idempotency-Key': idempotencyKey},
      // El backend tiene read-timeout=65s hacia GestoPago.
      receiveTimeout: const Duration(seconds: 75),
    );

    if (res.statusCode == 202) {
      final apiInfo = ApiException.fromResponse(202, res.data);
      return PagoPendienteConciliacionResult(
        upc: apiInfo.upcPendiente,
        mensaje: apiInfo.mensaje,
      );
    }

    return PagoExitosoResult(
      PagoServicioResponseDto.fromJson(res.data as Map<String, dynamic>),
    );
  }

  Future<PagoEstatusDto> consultarEstatus(String upc) async {
    final res = await _api.get('/pagos/estatus/${upc.trim()}');
    return PagoEstatusDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<List<TransaccionHistorialDto>> consultarHistorial(
    String numeroCuenta,
  ) async {
    final res = await _api.post(
      '/pagos/historial',
      body: {'numeroCuenta': numeroCuenta.trim()},
    );
    final list = res.data as List<dynamic>? ?? const [];
    return list
        .whereType<Map<String, dynamic>>()
        .map(TransaccionHistorialDto.fromJson)
        .toList();
  }

  Future<ComprobanteEmailResultDto> enviarComprobante(
    ComprobanteEmailDto dto,
  ) async {
    final res = await _api.post(
      '/pagos/enviar-comprobante',
      body: dto.toJson(),
    );
    return ComprobanteEmailResultDto.fromJson(
      res.data as Map<String, dynamic>,
    );
  }
}

final pagoRepositoryProvider = Provider<PagoRepository>(
  (ref) => PagoRepository(ref.watch(apiClientProvider)),
);
