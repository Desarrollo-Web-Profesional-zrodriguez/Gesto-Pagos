import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/config/app_config.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';

/// Cliente HTTP centralizado con soporte estricto de serialización para montos
/// (`StrictDecimal`), timeouts diferenciados (75s para `POST /pagos`), y
/// manejo uniforme de `202 Accepted` y `GenericResponse`.
class ApiClient {
  ApiClient({
    required this._dio,
    this._tokenProvider,
    this._onUnauthorizedExpired,
    this._onUserNetworkActivity,
  });

  final Dio _dio;
  final String? Function()? _tokenProvider;
  final void Function()? _onUnauthorizedExpired;
  final void Function()? _onUserNetworkActivity;

  Dio get rawDio => _dio;

  Future<Response<dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
    Map<String, String>? headers,
    Duration? receiveTimeout,
  }) =>
      _execute(
        () => _dio.get<dynamic>(
          path,
          queryParameters: queryParameters,
          options: _options(headers: headers, receiveTimeout: receiveTimeout),
        ),
      );

  Future<Response<dynamic>> post(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    Map<String, String>? headers,
    Duration? receiveTimeout,
  }) =>
      _execute(
        () => _dio.post<dynamic>(
          path,
          data: _encodeBody(body),
          queryParameters: queryParameters,
          options: _options(headers: headers, receiveTimeout: receiveTimeout),
        ),
      );

  Future<Response<dynamic>> put(
    String path, {
    Object? body,
    Map<String, String>? headers,
  }) =>
      _execute(
        () => _dio.put<dynamic>(
          path,
          data: _encodeBody(body),
          options: _options(headers: headers),
        ),
      );

  Future<Response<dynamic>> patch(
    String path, {
    Object? body,
    Map<String, String>? headers,
  }) =>
      _execute(
        () => _dio.patch<dynamic>(
          path,
          data: _encodeBody(body),
          options: _options(headers: headers),
        ),
      );

  Object? _encodeBody(Object? body) {
    if (body == null) return null;
    if (body is String) return body;
    return encodeStrictJson(body);
  }

  Options _options({
    Map<String, String>? headers,
    Duration? receiveTimeout,
  }) {
    final mergedHeaders = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    };
    final token = _tokenProvider?.call();
    if (token != null && token.isNotEmpty) {
      mergedHeaders['Authorization'] = 'Bearer $token';
    }
    if (headers != null) {
      mergedHeaders.addAll(headers);
    }
    return Options(
      headers: mergedHeaders,
      receiveTimeout: receiveTimeout,
      // Manejamos 202 y errores explícitamente en _execute.
      validateStatus: (status) => status != null && status >= 200 && status < 300,
    );
  }

  Future<Response<dynamic>> _execute(
    Future<Response<dynamic>> Function() call,
  ) async {
    try {
      final response = await call();
      _onUserNetworkActivity?.call();
      return response;
    } on DioException catch (e) {
      final apiError = ApiException.fromDio(e);
      if (apiError.esSesionExpirada) {
        _onUnauthorizedExpired?.call();
      }
      throw apiError;
    }
  }
}

/// Callback mutable para registrar token sin acoplamientos circulares en Riverpod.
class SessionHooks {
  String? Function()? readToken;
  void Function()? onExpired;
  void Function()? onActivity;
}

final sessionHooksProvider = Provider<SessionHooks>((ref) => SessionHooks());

final dioProvider = Provider<Dio>((ref) {
  final baseUrl = ref.watch(apiBaseUrlProvider);
  return Dio(
    BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 25),
      sendTimeout: const Duration(seconds: 20),
    ),
  );
});

final apiClientProvider = Provider<ApiClient>((ref) {
  final dio = ref.watch(dioProvider);
  final hooks = ref.watch(sessionHooksProvider);
  return ApiClient(
    dio: dio,
    tokenProvider: () => hooks.readToken?.call(),
    onUnauthorizedExpired: () => hooks.onExpired?.call(),
    onUserNetworkActivity: () => hooks.onActivity?.call(),
  );
});
