import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';

class ClienteRepository {
  ClienteRepository(this._api);

  final ApiClient _api;

  Future<ClienteDto> registrarCliente(ClienteRegistroRequestDto request) async {
    final res = await _api.post(
      '/clientes',
      body: request.toStrictJsonMap(),
    );
    return ClienteDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<List<ClienteDto>> buscarClientes([
    ClienteBusquedaAvanzadaDto? filtro,
  ]) async {
    final res = await _api.post(
      '/clientes/buscar',
      body: filtro?.toJson() ?? <String, dynamic>{},
    );
    final list = res.data as List<dynamic>? ?? const [];
    return list
        .whereType<Map<String, dynamic>>()
        .map(ClienteDto.fromJson)
        .toList();
  }

  Future<ClienteDto> consultarPorIdentificador(String identificador) async {
    final res = await _api.post(
      '/clientes/consultar',
      body: {'identificador': identificador.trim()},
    );
    return ClienteDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ClienteDto> actualizarCompleto(
    ClienteRegistroRequestDto request,
  ) async {
    final res = await _api.put(
      '/clientes',
      body: request.toStrictJsonMap(),
    );
    return ClienteDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ClienteDto> actualizarParcial(ClientePatchRequestDto request) async {
    final res = await _api.patch(
      '/clientes',
      body: request.toStrictJsonMap(),
    );
    return ClienteDto.fromJson(res.data as Map<String, dynamic>);
  }
}

final clienteRepositoryProvider = Provider<ClienteRepository>(
  (ref) => ClienteRepository(ref.watch(apiClientProvider)),
);
