import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';

class LoginResponseDto {
  const LoginResponseDto({
    required this.tokenSesion,
    required this.activa,
    required this.tipoToken,
    this.fechaInicio,
    this.fechaExpiracion,
    this.minutosInactividadMaximos = 5,
    this.idUsuario,
    this.idCliente,
    required this.username,
    required this.nombreCliente,
    this.mensaje,
  });

  final String tokenSesion;
  final bool activa;
  final String tipoToken;
  final String? fechaInicio;
  final String? fechaExpiracion;
  final int minutosInactividadMaximos;
  final int? idUsuario;
  final int? idCliente;
  final String username;
  final String nombreCliente;
  final String? mensaje;

  factory LoginResponseDto.fromJson(Map<String, dynamic> json) =>
      LoginResponseDto(
        tokenSesion: json['tokenSesion']?.toString() ?? '',
        activa: json['activa'] == true,
        tipoToken: json['tipoToken']?.toString() ?? 'Bearer',
        fechaInicio: json['fechaInicio']?.toString(),
        fechaExpiracion: json['fechaExpiracion']?.toString(),
        minutosInactividadMaximos:
            (json['minutosInactividadMaximos'] as num?)?.toInt() ?? 5,
        idUsuario: (json['idUsuario'] as num?)?.toInt(),
        idCliente: (json['idCliente'] as num?)?.toInt(),
        username: json['username']?.toString() ?? '',
        nombreCliente: json['nombreCliente']?.toString() ?? '',
        mensaje: json['mensaje']?.toString(),
      );
}

class AuthRepository {
  AuthRepository(this._api);

  final ApiClient _api;

  Future<LoginResponseDto> login({
    required String username,
    String? password,
    String? biometricoFacialEmbedding,
  }) async {
    final body = <String, dynamic>{
      'username': username.trim().toLowerCase(),
      if (password != null && password.isNotEmpty) 'password': password,
      if (biometricoFacialEmbedding != null &&
          biometricoFacialEmbedding.trim().isNotEmpty)
        'biometricoFacialEmbedding': biometricoFacialEmbedding.trim(),
    };
    final res = await _api.post('/auth/login', body: body);
    return LoginResponseDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<bool> validarSesion(String token) async {
    final res = await _api.get(
      '/auth/validar-sesion',
      headers: {'Authorization': 'Bearer $token'},
    );
    if (res.data is Map<String, dynamic>) {
      return (res.data as Map<String, dynamic>)['valida'] == true;
    }
    return true;
  }

  Future<void> logout(String token) async {
    await _api.post(
      '/auth/logout',
      headers: {'Authorization': 'Bearer $token'},
    );
  }
}

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(ref.watch(apiClientProvider)),
);
