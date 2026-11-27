import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/biometrics/face_scanner_sheet.dart';

/// Proveedor de captura y verificación de reconocimiento facial mediante cámara frontal.
abstract class FaceEmbeddingProvider {
  bool get isCameraSupported;

  String get statusMessage;

  Future<String?> captureFacialEmbedding(
    BuildContext context, {
    String title = 'Reconocimiento Facial',
    String subtitle = 'Centra tu rostro frente a la cámara con buena iluminación',
  });
}

class MobileCameraFaceEmbeddingProvider implements FaceEmbeddingProvider {
  const MobileCameraFaceEmbeddingProvider();

  @override
  bool get isCameraSupported => !kIsWeb;

  @override
  String get statusMessage =>
      'Verificación de identidad con cámara frontal activa.';

  @override
  Future<String?> captureFacialEmbedding(
    BuildContext context, {
    String title = 'Reconocimiento Facial',
    String subtitle = 'Centra tu rostro frente a la cámara con buena iluminación',
  }) {
    return showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => FaceScannerSheet(
        title: title,
        subtitle: subtitle,
      ),
    );
  }
}

final faceEmbeddingProvider = Provider<FaceEmbeddingProvider>(
  (ref) => const MobileCameraFaceEmbeddingProvider(),
);
