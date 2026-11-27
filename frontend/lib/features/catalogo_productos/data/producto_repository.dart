import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';

class ProductItemDto {
  const ProductItemDto({
    required this.idProducto,
    required this.idServicio,
    required this.servicio,
    required this.producto,
    this.precio,
    this.idCatTipoServicio,
    required this.tipoFront,
    this.hasDigitoVerificador,
    this.showAyuda,
    this.tipoReferencia,
    this.legend,
    this.urlLogo,
    this.urlReferenciaAyuda,
  });

  final int idProducto;
  final int idServicio;
  final String servicio;
  final String producto;
  final double? precio;
  final int? idCatTipoServicio;
  final int tipoFront;
  final bool? hasDigitoVerificador;
  final bool? showAyuda;
  final String? tipoReferencia;
  final String? legend;
  final String? urlLogo;
  final String? urlReferenciaAyuda;

  bool get esTipoFront1 => tipoFront == 1;

  bool get esPinOEntretenimiento =>
      idCatTipoServicio == 10 || idCatTipoServicio == 11;

  bool get esRecargaTelefonica => esTipoFront1 && !esPinOEntretenimiento;

  bool get esPagoServicio => tipoFront == 2;

  bool get esPrecioFijo =>
      (tipoFront != 2) && precio != null && precio! > 0;

  String get etiquetaFlujo {
    if (tipoFront == 1) {
      return esPinOEntretenimiento
          ? 'PIN / Entretenimiento'
          : 'Recarga Telefónica';
    }
    return 'Pago de Servicio';
  }

  factory ProductItemDto.fromJson(Map<String, dynamic> json) => ProductItemDto(
        idProducto: (json['idProducto'] as num).toInt(),
        idServicio: (json['idServicio'] as num).toInt(),
        servicio: json['servicio']?.toString() ?? '',
        producto: json['producto']?.toString() ?? '',
        precio: (json['precio'] as num?)?.toDouble(),
        idCatTipoServicio: (json['idCatTipoServicio'] as num?)?.toInt(),
        tipoFront: (json['tipoFront'] as num?)?.toInt() ?? 0,
        hasDigitoVerificador: json['hasDigitoVerificador'] as bool?,
        showAyuda: json['showAyuda'] as bool?,
        tipoReferencia: json['tipoReferencia']?.toString(),
        legend: json['legend']?.toString(),
        urlLogo: json['urlLogo']?.toString(),
        urlReferenciaAyuda: json['urlReferenciaAyuda']?.toString(),
      );
}

class ProductCategorizedResponseDto {
  const ProductCategorizedResponseDto({
    required this.status,
    required this.message,
    required this.origen,
    required this.totalProductos,
    required this.categorias,
  });

  final int status;
  final String message;
  final String origen;
  final int totalProductos;
  final Map<int, List<ProductItemDto>> categorias;

  factory ProductCategorizedResponseDto.fromJson(Map<String, dynamic> json) {
    final rawCategorias =
        json['categorias'] as Map<String, dynamic>? ?? const {};
    final parsed = <int, List<ProductItemDto>>{};

    for (final entry in rawCategorias.entries) {
      final key = int.tryParse(entry.key) ?? 0;
      final list = (entry.value as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(ProductItemDto.fromJson)
          .toList();
      parsed[key] = list;
    }

    final sortedKeys = parsed.keys.toList()..sort();
    final ordered = <int, List<ProductItemDto>>{
      for (final k in sortedKeys) k: parsed[k]!,
    };

    return ProductCategorizedResponseDto(
      status: (json['status'] as num?)?.toInt() ?? 200,
      message: json['message']?.toString() ?? '',
      origen: json['origen']?.toString() ?? '',
      totalProductos: (json['totalProductos'] as num?)?.toInt() ?? 0,
      categorias: ordered,
    );
  }
}

class ProductoRepository {
  ProductoRepository(this._api);

  final ApiClient _api;

  Future<ProductCategorizedResponseDto> obtenerProductos() async {
    final res = await _api.get('/productos');
    return ProductCategorizedResponseDto.fromJson(
      res.data as Map<String, dynamic>,
    );
  }
}

final productoRepositoryProvider = Provider<ProductoRepository>(
  (ref) => ProductoRepository(ref.watch(apiClientProvider)),
);
