import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';

class SolicitarOtpOnboardingResponseDto {
  const SolicitarOtpOnboardingResponseDto({
    required this.enviado,
    required this.emailEnmascarado,
    required this.expiraEnSegundos,
    required this.mensaje,
  });

  final bool enviado;
  final String emailEnmascarado;
  final int expiraEnSegundos;
  final String mensaje;

  factory SolicitarOtpOnboardingResponseDto.fromJson(
    Map<String, dynamic> json,
  ) =>
      SolicitarOtpOnboardingResponseDto(
        enviado: json['enviado'] == true,
        emailEnmascarado: json['emailEnmascarado']?.toString() ?? '',
        expiraEnSegundos:
            (json['expiraEnSegundos'] as num?)?.toInt() ?? 300,
        mensaje: json['mensaje']?.toString() ?? '',
      );
}

class ClienteRepository {
  ClienteRepository(this._api);

  final ApiClient _api;

  Future<SolicitarOtpOnboardingResponseDto> solicitarOtpOnboarding({
    required String correoElectronico,
    String? nombre,
    String? curp,
    String? rfc,
  }) async {
    final res = await _api.post(
      '/auth/onboarding/enviar-otp',
      body: {
        'correoElectronico': correoElectronico.trim().toLowerCase(),
        if (nombre != null && nombre.trim().isNotEmpty) 'nombre': nombre.trim(),
        if (curp != null && curp.trim().isNotEmpty)
          'curp': curp.trim().toUpperCase(),
        if (rfc != null && rfc.trim().isNotEmpty)
          'rfc': rfc.trim().toUpperCase(),
      },
    );
    return SolicitarOtpOnboardingResponseDto.fromJson(
      res.data as Map<String, dynamic>,
    );
  }

  Future<ClienteDto> registrarConOtp({
    required String codigoOtp,
    required ClienteRegistroRequestDto registro,
  }) async {
    final res = await _api.post(
      '/auth/onboarding/registrar-verificado',
      body: {
        'codigoOtp': codigoOtp.replaceAll(RegExp(r'\D'), ''),
        'registro': registro.toStrictJsonMap(),
      },
    );
    return ClienteDto.fromJson(res.data as Map<String, dynamic>);
  }

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
