import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Configuración de entorno y URL base del servicio GestoPagos.
class AppEnvironment {
  AppEnvironment._();

  static const String productionBaseUrl = 'https://gesto-pagos-api.onrender.com';

  static const String envBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: '',
  );

  static String get defaultBaseUrl {
    if (envBaseUrl.trim().isNotEmpty) {
      return _normalizeBaseUrl(envBaseUrl);
    }
    return productionBaseUrl;
  }

  static String normalize(String raw) => _normalizeBaseUrl(raw);

  static String _normalizeBaseUrl(String raw) {
    final trimmed = raw.trim();
    if (trimmed.endsWith('/')) {
      return trimmed.substring(0, trimmed.length - 1);
    }
    return trimmed;
  }
}

class ApiBaseUrlNotifier extends Notifier<String> {
  @override
  String build() => AppEnvironment.defaultBaseUrl;

  void setBaseUrl(String url) {
    final normalized = AppEnvironment.normalize(url);
    if (normalized.isNotEmpty) {
      state = normalized;
    }
  }
}

final apiBaseUrlProvider = NotifierProvider<ApiBaseUrlNotifier, String>(
  ApiBaseUrlNotifier.new,
);
