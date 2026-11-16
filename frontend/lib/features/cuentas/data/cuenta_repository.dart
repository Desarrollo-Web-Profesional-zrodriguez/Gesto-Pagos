import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';

class CuentaRepository {
  CuentaRepository(this._api);

  final ApiClient _api;

  Future<CuentaDto> obtenerPorNumeroCuenta(String numeroCuenta) async {
    final res = await _api.get('/cuentas/${numeroCuenta.trim()}');
    return CuentaDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<CuentaDto> consultarCuentaPost(String numeroCuenta) async {
    final res = await _api.post(
      '/cuentas/consultar',
      body: {'numeroCuenta': numeroCuenta.trim()},
    );
    return CuentaDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<List<CuentaDto>> listarCuentasActivas() async {
    final res = await _api.get('/cuentas/activas');
    final list = res.data as List<dynamic>? ?? const [];
    return list
        .whereType<Map<String, dynamic>>()
        .map(CuentaDto.fromJson)
        .toList();
  }

  Future<SaldoDto> obtenerSaldoGet(String numeroCuenta) async {
    final res = await _api.get('/cuentas/${numeroCuenta.trim()}/saldo');
    return SaldoDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<SaldoDto> consultarSaldoPost(String numeroCuenta) async {
    final res = await _api.post(
      '/cuentas/consultar-saldo',
      body: {'numeroCuenta': numeroCuenta.trim()},
    );
    return SaldoDto.fromJson(res.data as Map<String, dynamic>);
  }
}

final cuentaRepositoryProvider = Provider<CuentaRepository>(
  (ref) => CuentaRepository(ref.watch(apiClientProvider)),
);
