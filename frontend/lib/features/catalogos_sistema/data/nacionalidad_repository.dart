import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';

class NacionalidadDto {
  const NacionalidadDto({
    required this.idNacionalidad,
    required this.claveIso,
    required this.pais,
    required this.gentilicio,
  });

  final int idNacionalidad;
  final String claveIso;
  final String pais;
  final String gentilicio;

  factory NacionalidadDto.fromJson(Map<String, dynamic> json) => NacionalidadDto(
        idNacionalidad: (json['idNacionalidad'] as num).toInt(),
        claveIso: json['claveIso']?.toString() ?? '',
        pais: json['pais']?.toString() ?? '',
        gentilicio: json['gentilicio']?.toString() ?? '',
      );
}

class NacionalidadRepository {
  NacionalidadRepository(this._api);

  final ApiClient _api;

  Future<List<NacionalidadDto>> listarActivas() async {
    final res = await _api.get('/catalogos/nacionalidades');
    final list = res.data as List<dynamic>? ?? const [];
    return list
        .whereType<Map<String, dynamic>>()
        .map(NacionalidadDto.fromJson)
        .toList();
  }

  Future<NacionalidadDto> obtenerPorId(int id) async {
    final res = await _api.get('/catalogos/nacionalidades/$id');
    return NacionalidadDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<NacionalidadDto> buscarPorCriterio(String criterio) async {
    final res = await _api.post(
      '/catalogos/nacionalidades/buscar',
      body: {'criterio': criterio.trim()},
    );
    return NacionalidadDto.fromJson(res.data as Map<String, dynamic>);
  }
}

final nacionalidadRepositoryProvider = Provider<NacionalidadRepository>(
  (ref) => NacionalidadRepository(ref.watch(apiClientProvider)),
);

final nacionalidadesCatalogProvider = FutureProvider<List<NacionalidadDto>>(
  (ref) => ref.watch(nacionalidadRepositoryProvider).listarActivas(),
);
