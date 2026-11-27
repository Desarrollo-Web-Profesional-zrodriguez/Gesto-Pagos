import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_extractor.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

/// Hoja modal interactiva que abre la cámara frontal, guía al usuario en un óvalo
/// biométrico y extrae una firma facial verificada en dos muestras consecutivas.
class FaceScannerSheet extends StatefulWidget {
  const FaceScannerSheet({
    super.key,
    required this.title,
    required this.subtitle,
  });

  final String title;
  final String subtitle;

  @override
  State<FaceScannerSheet> createState() => _FaceScannerSheetState();
}

class _FaceScannerSheetState extends State<FaceScannerSheet> {
  CameraController? _cameraController;
  FaceDetector? _faceDetector;

  bool _initializing = true;
  bool _capturing = false;
  bool _verified = false;
  String? _initError;
  String? _hintError;
  String _statusLabel = 'Ubica tu rostro dentro del óvalo y mira de frente';

  @override
  void initState() {
    super.initState();
    _initializeScanner();
  }

  Future<void> _initializeScanner() async {
    if (kIsWeb) {
      setState(() {
        _initializing = false;
        _initError =
            'El escáner facial móvil requiere ejecutarse en un dispositivo Android o iOS.';
      });
      return;
    }

    setState(() {
      _initializing = true;
      _initError = null;
      _hintError = null;
    });

    try {
      final cameras = await availableCameras();
      if (cameras.isEmpty) {
        if (!mounted) return;
        setState(() {
          _initializing = false;
          _initError =
              'No se encontró una cámara disponible en este dispositivo.';
        });
        return;
      }

      final selectedCamera = cameras.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.front,
        orElse: () => cameras.first,
      );

      final controller = CameraController(
        selectedCamera,
        ResolutionPreset.medium,
        enableAudio: false,
        imageFormatGroup: Platform.isAndroid
            ? ImageFormatGroup.jpeg
            : ImageFormatGroup.bgra8888,
      );

      await controller.initialize();

      _faceDetector = FaceDetector(
        options: FaceDetectorOptions(
          enableLandmarks: true,
          enableContours: true,
          enableClassification: true,
          minFaceSize: 0.22,
          performanceMode: FaceDetectorMode.accurate,
        ),
      );

      if (!mounted) {
        await controller.dispose();
        return;
      }

      setState(() {
        _cameraController = controller;
        _initializing = false;
      });
    } on CameraException catch (e) {
      if (!mounted) return;
      final denied = e.code.toLowerCase().contains('access') ||
          e.code.toLowerCase().contains('permission');
      setState(() {
        _initializing = false;
        _initError = denied
            ? 'Permiso de cámara denegado. Habilita el acceso a la cámara en los ajustes de tu teléfono.'
            : 'No fue posible iniciar la cámara frontal (${e.description ?? e.code}).';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _initializing = false;
        _initError =
            'No fue posible activar la cámara frontal en este dispositivo.';
      });
    }
  }

  @override
  void dispose() {
    _cameraController?.dispose();
    _faceDetector?.close();
    super.dispose();
  }

  Future<FaceExtractionResult> _captureSingleSample(
    CameraController controller,
    FaceDetector detector,
  ) async {
    final shot = await controller.takePicture();
    final file = File(shot.path);
    try {
      final inputImage = InputImage.fromFilePath(shot.path);
      final faces = await detector.processImage(inputImage);
      return FaceEmbeddingExtractor.extractFromFaces(faces);
    } finally {
      try {
        if (await file.exists()) {
          await file.delete();
        }
      } catch (_) {}
    }
  }

  Future<void> _scanFace() async {
    final controller = _cameraController;
    final detector = _faceDetector;
    if (controller == null ||
        !controller.value.isInitialized ||
        detector == null ||
        _capturing) {
      return;
    }

    setState(() {
      _capturing = true;
      _hintError = null;
      _statusLabel = 'Analizando rasgos faciales (1/2)...';
    });

    try {
      final first = await _captureSingleSample(controller, detector);
      if (!first.isValid) {
        if (!mounted) return;
        setState(() {
          _capturing = false;
          _hintError = first.errorMessage;
          _statusLabel = 'Ajusta tu posición e inténtalo nuevamente';
        });
        return;
      }

      if (!mounted) return;
      setState(() {
        _statusLabel = 'Confirmando estabilidad facial (2/2)...';
      });
      await Future<void>.delayed(const Duration(milliseconds: 220));

      final second = await _captureSingleSample(controller, detector);
      if (!second.isValid) {
        if (!mounted) return;
        setState(() {
          _capturing = false;
          _hintError = second.errorMessage;
          _statusLabel = 'Ajusta tu posición e inténtalo nuevamente';
        });
        return;
      }

      final stability = FaceEmbeddingExtractor.cosineSimilarity(
        first.serialized!,
        second.serialized!,
      );
      if (stability < 0.82) {
        if (!mounted) return;
        setState(() {
          _capturing = false;
          _hintError =
              'Detectamos movimiento durante la captura. Mantén tu rostro firme un instante.';
          _statusLabel = 'Intenta escanear de nuevo sin moverte';
        });
        return;
      }

      final combined = FaceEmbeddingExtractor.combineVectors([
        first.vector!,
        second.vector!,
      ]);

      if (!mounted) return;
      setState(() {
        _verified = true;
        _capturing = false;
        _statusLabel = '¡Rostro verificado correctamente!';
      });

      await Future<void>.delayed(const Duration(milliseconds: 320));
      if (mounted) {
        Navigator.of(context).pop(combined);
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _capturing = false;
        _hintError =
            'Ocurrió un inconveniente al procesar la captura. Intenta de nuevo.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).padding.bottom;
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.slate950,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      padding: EdgeInsets.fromLTRB(20, 14, 20, 20 + bottomInset),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.24),
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: AppColors.secondary.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(
                    Icons.face_retouching_natural,
                    color: Color(0xFF7DD3FC),
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.title,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 17,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        widget.subtitle,
                        style: const TextStyle(
                          color: Color(0xFF94A3B8),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  onPressed: _capturing
                      ? null
                      : () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close, color: Color(0xFFCBD5E1)),
                ),
              ],
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 330,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(24),
                child: _buildCameraViewport(),
              ),
            ),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: _hintError != null
                    ? AppColors.danger.withValues(alpha: 0.16)
                    : _verified
                        ? AppColors.accent.withValues(alpha: 0.18)
                        : Colors.white.withValues(alpha: 0.07),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: _hintError != null
                      ? AppColors.danger.withValues(alpha: 0.45)
                      : _verified
                          ? AppColors.accent.withValues(alpha: 0.5)
                          : Colors.white.withValues(alpha: 0.12),
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    _hintError != null
                        ? Icons.warning_amber_rounded
                        : _verified
                            ? Icons.check_circle
                            : Icons.center_focus_strong_outlined,
                    size: 19,
                    color: _hintError != null
                        ? const Color(0xFFFCA5A5)
                        : _verified
                            ? const Color(0xFF6EE7B7)
                            : const Color(0xFF7DD3FC),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _hintError ?? _statusLabel,
                      style: TextStyle(
                        color: _hintError != null
                            ? const Color(0xFFFECACA)
                            : Colors.white,
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            if (_initError != null)
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.white,
                  side: BorderSide(color: Colors.white.withValues(alpha: 0.3)),
                ),
                onPressed: _initializeScanner,
                icon: const Icon(Icons.refresh),
                label: const Text('Reintentar cámara'),
              )
            else
              FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor:
                      _verified ? AppColors.accent : AppColors.secondary,
                  minimumSize: const Size.fromHeight(50),
                ),
                onPressed:
                    (_initializing || _capturing || _verified) ? null : _scanFace,
                icon: _capturing
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.2,
                          color: Colors.white,
                        ),
                      )
                    : Icon(
                        _verified
                            ? Icons.check_circle_outline
                            : Icons.camera_front_outlined,
                      ),
                label: Text(
                  _capturing
                      ? 'Verificando rostro...'
                      : _verified
                          ? 'Verificación completada'
                          : 'Escanear mi rostro',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildCameraViewport() {
    if (_initializing) {
      return Container(
        color: AppColors.primary,
        alignment: Alignment.center,
        child: const Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(color: Color(0xFF7DD3FC)),
            SizedBox(height: 12),
            Text(
              'Activando cámara frontal...',
              style: TextStyle(color: Color(0xFFCBD5E1), fontSize: 13),
            ),
          ],
        ),
      );
    }

    if (_initError != null || _cameraController == null) {
      return Container(
        color: AppColors.primary,
        padding: const EdgeInsets.all(24),
        alignment: Alignment.center,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.videocam_off_outlined,
              color: Color(0xFFFCA5A5),
              size: 42,
            ),
            const SizedBox(height: 12),
            Text(
              _initError ?? 'Cámara no disponible.',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white, fontSize: 13),
            ),
          ],
        ),
      );
    }

    return Stack(
      fit: StackFit.expand,
      children: [
        FittedBox(
          fit: BoxFit.cover,
          child: SizedBox(
            width: _cameraController!.value.previewSize?.height ?? 480,
            height: _cameraController!.value.previewSize?.width ?? 640,
            child: CameraPreview(_cameraController!),
          ),
        ),
        CustomPaint(
          painter: _FaceOvalGuidePainter(
            borderColor: _verified
                ? AppColors.accent
                : _hintError != null
                    ? AppColors.warning
                    : const Color(0xFF38BDF8),
          ),
        ),
      ],
    );
  }
}

class _FaceOvalGuidePainter extends CustomPainter {
  const _FaceOvalGuidePainter({required this.borderColor});

  final Color borderColor;

  @override
  void paint(Canvas canvas, Size size) {
    final overlayPaint = Paint()
      ..color = AppColors.slate950.withValues(alpha: 0.52);

    final ovalRect = Rect.fromCenter(
      center: Offset(size.width / 2, size.height * 0.48),
      width: size.width * 0.62,
      height: size.height * 0.74,
    );

    final fullPath = Path()..addRect(Offset.zero & size);
    final holePath = Path()..addOval(ovalRect);
    final maskPath = Path.combine(PathOperation.difference, fullPath, holePath);
    canvas.drawPath(maskPath, overlayPaint);

    final borderPaint = Paint()
      ..color = borderColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.0;
    canvas.drawOval(ovalRect, borderPaint);
  }

  @override
  bool shouldRepaint(covariant _FaceOvalGuidePainter oldDelegate) =>
      oldDelegate.borderColor != borderColor;
}
