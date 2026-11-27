import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:local_auth/local_auth.dart';

class FingerprintAuthResult {
  const FingerprintAuthResult({required this.autenticado, this.mensaje});

  final bool autenticado;
  final String? mensaje;
}

class FingerprintAuthService {
  FingerprintAuthService({LocalAuthentication? localAuth})
      : _localAuth = localAuth ?? LocalAuthentication();

  final LocalAuthentication _localAuth;

  /// La huella digital solo está disponible en dispositivos móviles nativos (Android / iOS).
  Future<bool> estaDisponibleEnTelefono() async {
    if (kIsWeb) return false;
    try {
      final supported = await _localAuth.isDeviceSupported();
      final canCheck = await _localAuth.canCheckBiometrics;
      if (!supported || !canCheck) return false;
      final disponibles = await _localAuth.getAvailableBiometrics();
      return disponibles.isNotEmpty;
    } on PlatformException {
      return false;
    } catch (_) {
      return false;
    }
  }

  Future<FingerprintAuthResult> autenticarConHuella({
    required String motivo,
  }) async {
    if (kIsWeb) {
      return const FingerprintAuthResult(
        autenticado: false,
        mensaje: 'La verificación por huella digital solo funciona en el teléfono.',
      );
    }

    try {
      final disponible = await estaDisponibleEnTelefono();
      if (!disponible) {
        return const FingerprintAuthResult(
          autenticado: false,
          mensaje:
              'Este teléfono no tiene huella o biometría configurada. Usa el código enviado al correo.',
        );
      }

      final ok = await _localAuth.authenticate(
        localizedReason: motivo,
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
          useErrorDialogs: true,
        ),
      );

      if (!ok) {
        return const FingerprintAuthResult(
          autenticado: false,
          mensaje: 'Autenticación biométrica cancelada o no reconocida.',
        );
      }

      return const FingerprintAuthResult(autenticado: true);
    } on PlatformException catch (e) {
      return FingerprintAuthResult(
        autenticado: false,
        mensaje: e.message ?? 'No se pudo validar la huella digital en el dispositivo.',
      );
    } catch (_) {
      return const FingerprintAuthResult(
        autenticado: false,
        mensaje: 'No se pudo validar la huella digital en el dispositivo.',
      );
    }
  }
}

final fingerprintAuthServiceProvider = Provider<FingerprintAuthService>(
  (ref) => FingerprintAuthService(),
);
