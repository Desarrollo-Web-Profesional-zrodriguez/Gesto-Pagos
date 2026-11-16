import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Configuración de entornos y URLs base del backend Spring Boot.
class AppEnvironment {
  AppEnvironment._();

  static const String envBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: '',
  );

  static String get defaultBaseUrl {
    if (envBaseUrl.trim().isNotEmpty) {
      return _normalizeBaseUrl(envBaseUrl);
    }
    if (!kIsWeb && Platform.isAndroid) {
      // Dirección estándar del host desde el emulador Android.
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static List<BackendPreset> get presets => [
        const BackendPreset(
          label: 'Emulador Android (10.0.2.2:8080)',
          url: 'http://10.0.2.2:8080',
        ),
        const BackendPreset(
          label: 'Localhost / Simulador iOS (localhost:8080)',
          url: 'http://localhost:8080',
        ),
      ];

  static String normalize(String raw) => _normalizeBaseUrl(raw);

  static String _normalizeBaseUrl(String raw) {
    final trimmed = raw.trim();
    if (trimmed.endsWith('/')) {
      return trimmed.substring(0, trimmed.length - 1);
    }
    return trimmed;
  }
}

class BackendPreset {
  const BackendPreset({required this.label, required this.url});

  final String label;
  final String url;
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
