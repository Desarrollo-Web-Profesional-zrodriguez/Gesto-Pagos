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
    this.biometriaFacialActiva = false,
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
  final bool biometriaFacialActiva;
  final String? mensaje;

  LoginResponseDto copyWith({bool? biometriaFacialActiva}) => LoginResponseDto(
        tokenSesion: tokenSesion,
        activa: activa,
        tipoToken: tipoToken,
        fechaInicio: fechaInicio,
        fechaExpiracion: fechaExpiracion,
        minutosInactividadMaximos: minutosInactividadMaximos,
        idUsuario: idUsuario,
        idCliente: idCliente,
        username: username,
        nombreCliente: nombreCliente,
        biometriaFacialActiva:
            biometriaFacialActiva ?? this.biometriaFacialActiva,
        mensaje: mensaje,
      );

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
        biometriaFacialActiva: json['biometriaFacialActiva'] == true,
        mensaje: json['mensaje']?.toString(),
      );
}

class BiometriaConfigResultDto {
  const BiometriaConfigResultDto({
    required this.biometriaFacialActiva,
    required this.mensaje,
  });

  final bool biometriaFacialActiva;
  final String mensaje;

  factory BiometriaConfigResultDto.fromJson(Map<String, dynamic> json) =>
      BiometriaConfigResultDto(
        biometriaFacialActiva: json['biometriaFacialActiva'] == true,
        mensaje: json['mensaje']?.toString() ??
            'Configuración de reconocimiento facial actualizada.',
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

  Future<BiometriaConfigResultDto> configurarBiometriaFacial({
    required String token,
    required String passwordActual,
    String? biometricoFacialEmbedding,
    bool eliminar = false,
  }) async {
    final res = await _api.post(
      '/auth/biometria-facial',
      headers: {'Authorization': 'Bearer $token'},
      body: {
        'passwordActual': passwordActual,
        if (biometricoFacialEmbedding != null &&
            biometricoFacialEmbedding.trim().isNotEmpty)
          'biometricoFacialEmbedding': biometricoFacialEmbedding.trim(),
        'eliminar': eliminar,
      },
    );
    return BiometriaConfigResultDto.fromJson(res.data as Map<String, dynamic>);
  }

  Future<bool> consultarEstadoBiometria(String token) async {
    final res = await _api.get(
      '/auth/biometria-facial/estado',
      headers: {'Authorization': 'Bearer $token'},
    );
    if (res.data is Map<String, dynamic>) {
      return (res.data as Map<String, dynamic>)['biometriaFacialActiva'] ==
          true;
    }
    return false;
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
