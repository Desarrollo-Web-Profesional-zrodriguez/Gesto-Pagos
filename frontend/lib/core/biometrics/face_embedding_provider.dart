import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Contrato abstracto para la futura integración de biometría facial MediaPipe
/// con cámara nativa en Android / iOS.
///
/// El backend espera un string con un vector JSON de flotantes
/// (por ejemplo: `"[0.12, -0.45, 0.89, -0.05, 0.67]"`), comparado por similitud
/// coseno >= 0.80 en `BiometriaFacialUtil`.
abstract class FaceEmbeddingProvider {
  bool get isCameraSupported;

  String get statusMessage;

  Future<String?> captureFacialEmbedding();
}

/// Implementación inicial mientras el flujo opera con contraseña/token.
/// Lista para reemplazarse por MediaPipe Face Landmarker + TFLite sin cambiar
/// repositorios ni pantallas.
class PlaceholderFaceEmbeddingProvider implements FaceEmbeddingProvider {
  const PlaceholderFaceEmbeddingProvider();

  @override
  bool get isCameraSupported => false;

  @override
  String get statusMessage =>
      'Reconocimiento facial MediaPipe programado para la siguiente fase. Usa contraseña o sesión activa.';

  @override
  Future<String?> captureFacialEmbedding() async => null;
}

final faceEmbeddingProvider = Provider<FaceEmbeddingProvider>(
  (ref) => const PlaceholderFaceEmbeddingProvider(),
);
