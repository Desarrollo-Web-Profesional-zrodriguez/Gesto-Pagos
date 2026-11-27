import 'dart:math' as math;

import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

class FaceExtractionResult {
  const FaceExtractionResult._({
    this.vector,
    this.serialized,
    this.errorMessage,
  });

  factory FaceExtractionResult.success(List<double> vector) {
    final normalized = FaceEmbeddingExtractor.l2Normalize(vector);
    return FaceExtractionResult._(
      vector: normalized,
      serialized: FaceEmbeddingExtractor.serializeVector(normalized),
    );
  }

  factory FaceExtractionResult.failure(String message) =>
      FaceExtractionResult._(errorMessage: message);

  final List<double>? vector;
  final String? serialized;
  final String? errorMessage;

  bool get isValid => vector != null && serialized != null;
}

/// Extractor determinista de geometría antropométrica facial invariante a escala,
/// traslación, inclinación (roll) y espejo horizontal de cámara frontal.
class FaceEmbeddingExtractor {
  FaceEmbeddingExtractor._();

  static const double minInterOcularPixels = 28.0;
  static const double maxHeadYawDeg = 16.0;
  static const double maxHeadRollDeg = 15.0;
  static const double maxHeadPitchDeg = 16.0;
  static const double minEyeOpenProb = 0.40;

  static FaceExtractionResult extractFromFaces(List<Face> faces) {
    if (faces.isEmpty) {
      return FaceExtractionResult.failure(
        'No se detectó tu rostro. Ubícate de frente dentro del guía ovalado.',
      );
    }
    if (faces.length > 1) {
      return FaceExtractionResult.failure(
        'Se detectó más de una persona. Asegúrate de aparecer únicamente tú.',
      );
    }

    final face = faces.first;

    final yaw = face.headEulerAngleY;
    if (yaw != null && yaw.abs() > maxHeadYawDeg) {
      return FaceExtractionResult.failure(
        'Mira de frente hacia la cámara sin girar la cabeza.',
      );
    }

    final roll = face.headEulerAngleZ;
    if (roll != null && roll.abs() > maxHeadRollDeg) {
      return FaceExtractionResult.failure(
        'Mantén tu cabeza recta sin inclinarla hacia los lados.',
      );
    }

    final pitch = face.headEulerAngleX;
    if (pitch != null && pitch.abs() > maxHeadPitchDeg) {
      return FaceExtractionResult.failure(
        'Nivela el teléfono a la altura de tu rostro.',
      );
    }

    final leftOpen = face.leftEyeOpenProbability;
    final rightOpen = face.rightEyeOpenProbability;
    if ((leftOpen != null && leftOpen < minEyeOpenProb) ||
        (rightOpen != null && rightOpen < minEyeOpenProb)) {
      return FaceExtractionResult.failure(
        'Mantén ambos ojos bien abiertos mirando hacia la cámara.',
      );
    }

    final leftEye = face.landmarks[FaceLandmarkType.leftEye]?.position;
    final rightEye = face.landmarks[FaceLandmarkType.rightEye]?.position;
    final noseBase = face.landmarks[FaceLandmarkType.noseBase]?.position;
    final bottomMouth = face.landmarks[FaceLandmarkType.bottomMouth]?.position;
    final leftMouth = face.landmarks[FaceLandmarkType.leftMouth]?.position;
    final rightMouth = face.landmarks[FaceLandmarkType.rightMouth]?.position;

    if (leftEye == null ||
        rightEye == null ||
        noseBase == null ||
        bottomMouth == null ||
        leftMouth == null ||
        rightMouth == null) {
      return FaceExtractionResult.failure(
        'Ilumina mejor tu rostro para detectar todos los rasgos con claridad.',
      );
    }

    final dx = (rightEye.x - leftEye.x).toDouble();
    final dy = (rightEye.y - leftEye.y).toDouble();
    final eyeDist = math.sqrt(dx * dx + dy * dy);
    if (eyeDist < minInterOcularPixels) {
      return FaceExtractionResult.failure(
        'Acércate un poco más a la cámara para capturar tu rostro con nitidez.',
      );
    }

    // Origen canónico en punto medio inter-ocular
    final mx = 0.5 * (leftEye.x + rightEye.x);
    final my = 0.5 * (leftEye.y + rightEye.y);

    final ux = dx / eyeDist;
    final uy = dy / eyeDist;

    // Vector perpendicular orientado siempre hacia la nariz/mentón (invariante a espejo)
    var vx = -uy;
    var vy = ux;
    final noseDot = (noseBase.x - mx) * vx + (noseBase.y - my) * vy;
    if (noseDot < 0) {
      vx = -vx;
      vy = -vy;
    }

    double projVNum(num px, num py) =>
        ((px - mx) * vx + (py - my) * vy) / eyeDist;

    double distNorm(math.Point<int> a, math.Point<int> b) {
      final adx = (a.x - b.x).toDouble();
      final ady = (a.y - b.y).toDouble();
      return math.sqrt(adx * adx + ady * ady) / eyeDist;
    }

    final noseV = projVNum(noseBase.x, noseBase.y);
    final mouthMidX = 0.5 * (leftMouth.x + rightMouth.x);
    final mouthMidY = 0.5 * (leftMouth.y + rightMouth.y);
    final mouthMidV = projVNum(mouthMidX, mouthMidY);
    final chinLipV = projVNum(bottomMouth.x, bottomMouth.y);
    final mouthWidth = distNorm(leftMouth, rightMouth);

    final lipThicknessV = math.max(0.02, chinLipV - mouthMidV);
    final philtrumV = math.max(0.10, mouthMidV - noseV);
    final midToLowerRatio = noseV / math.max(chinLipV - noseV, 0.15);
    final mouthToNoseRatio = mouthWidth / math.max(noseV, 0.20);
    final meanEyeNose =
        0.5 * (distNorm(leftEye, noseBase) + distNorm(rightEye, noseBase));
    final meanEyeMouth =
        0.5 * (distNorm(leftEye, leftMouth) + distNorm(rightEye, rightMouth));

    final box = face.boundingBox;
    final boxW = box.width / eyeDist;
    final boxH = box.height / eyeDist;
    final boxAspect = box.width / math.max(box.height, 1.0);

    final leftCheek = face.landmarks[FaceLandmarkType.leftCheek]?.position;
    final rightCheek = face.landmarks[FaceLandmarkType.rightCheek]?.position;
    final cheekSpan = (leftCheek != null && rightCheek != null)
        ? distNorm(leftCheek, rightCheek)
        : boxW * 0.50;
    final cheekV = (leftCheek != null && rightCheek != null)
        ? 0.5 *
            (projVNum(leftCheek.x, leftCheek.y) +
                projVNum(rightCheek.x, rightCheek.y))
        : noseV * 0.92;
    final cheekToMouthRatio = cheekSpan / math.max(mouthWidth, 0.25);

    final leftEar = face.landmarks[FaceLandmarkType.leftEar]?.position;
    final rightEar = face.landmarks[FaceLandmarkType.rightEar]?.position;
    final faceOval = face.contours[FaceContourType.face]?.points;

    double headSpan;
    if (leftEar != null && rightEar != null) {
      headSpan = distNorm(leftEar, rightEar);
    } else if (faceOval != null && faceOval.length >= 28) {
      headSpan = distNorm(faceOval[9], faceOval[27]);
    } else {
      headSpan = boxW * 0.90;
    }
    final jawTaper = mouthWidth / math.max(headSpan, 0.50);

    double chinDepthV;
    double foreheadHeightV;
    double lowerJawWidth;
    if (faceOval != null && faceOval.length >= 36) {
      chinDepthV = projVNum(faceOval[18].x, faceOval[18].y);
      foreheadHeightV = -projVNum(faceOval[0].x, faceOval[0].y);
      lowerJawWidth = distNorm(faceOval[13], faceOval[23]);
    } else {
      // Aproximación geométrica consistente respecto al cuadro delimitador rotado
      chinDepthV = chinLipV + 0.46;
      foreheadHeightV = math.max(0.55, boxH - chinDepthV);
      lowerJawWidth = headSpan * 0.74;
    }

    final noseBottomPts = face.contours[FaceContourType.noseBottom]?.points;
    final noseAlarWidth = (noseBottomPts != null && noseBottomPts.length >= 3)
        ? distNorm(noseBottomPts.first, noseBottomPts.last)
        : mouthWidth * 0.52;

    final leftBrow = face.contours[FaceContourType.leftEyebrowTop]?.points;
    final rightBrow = face.contours[FaceContourType.rightEyebrowTop]?.points;
    double browHeightV;
    if (leftBrow != null &&
        leftBrow.isNotEmpty &&
        rightBrow != null &&
        rightBrow.isNotEmpty) {
      final lbMid = leftBrow[leftBrow.length ~/ 2];
      final rbMid = rightBrow[rightBrow.length ~/ 2];
      browHeightV =
          -0.5 * (projVNum(lbMid.x, lbMid.y) + projVNum(rbMid.x, rbMid.y));
    } else {
      browHeightV = foreheadHeightV * 0.29;
    }

    final faceVerticalBalance = foreheadHeightV / math.max(chinDepthV, 0.40);

    // 24 rasgos estandarizados alrededor de medias antropométricas canónicas
    final rawFeatures = <double>[
      (noseV - 0.58) / 0.09,
      (mouthMidV - 1.12) / 0.12,
      (chinLipV - 1.28) / 0.14,
      (mouthWidth - 0.84) / 0.11,
      (lipThicknessV - 0.16) / 0.05,
      (philtrumV - 0.54) / 0.09,
      (midToLowerRatio - 0.83) / 0.15,
      (mouthToNoseRatio - 1.45) / 0.24,
      (meanEyeNose - 0.77) / 0.08,
      (meanEyeMouth - 1.14) / 0.12,
      (boxW - 2.35) / 0.26,
      (boxH - 2.95) / 0.32,
      (boxAspect - 0.80) / 0.09,
      (cheekSpan - 1.18) / 0.14,
      (cheekV - 0.54) / 0.09,
      (cheekToMouthRatio - 1.40) / 0.20,
      (headSpan - 2.12) / 0.22,
      (jawTaper - 0.40) / 0.06,
      (chinDepthV - 1.78) / 0.18,
      (foreheadHeightV - 1.05) / 0.16,
      (noseAlarWidth - 0.44) / 0.08,
      (browHeightV - 0.30) / 0.06,
      (lowerJawWidth - 1.56) / 0.18,
      (faceVerticalBalance - 0.59) / 0.10,
    ];

    return FaceExtractionResult.success(rawFeatures);
  }

  /// Combina múltiples muestras consecutivas para cancelar ruido de captura.
  static String combineVectors(List<List<double>> vectors) {
    if (vectors.isEmpty) return '';
    final dim = vectors.first.length;
    final avg = List<double>.filled(dim, 0.0);
    for (final v in vectors) {
      for (var i = 0; i < dim; i++) {
        avg[i] += v[i];
      }
    }
    for (var i = 0; i < dim; i++) {
      avg[i] /= vectors.length;
    }
    return serializeVector(l2Normalize(avg));
  }

  static List<double> l2Normalize(List<double> input) {
    var normSq = 0.0;
    for (final val in input) {
      normSq += val * val;
    }
    final norm = math.sqrt(normSq);
    if (norm < 1e-8) {
      // Evita vector degenerado si coincidiera exactamente con la media poblacional
      final fallback = List<double>.from(input);
      if (fallback.isNotEmpty) fallback[0] = 1.0;
      return fallback;
    }
    return input.map((v) => v / norm).toList(growable: false);
  }

  /// Formato JSON compacto (< 200 caracteres) compatible con PostgreSQL y BiometriaFacialUtil.
  static String serializeVector(List<double> unitVector) {
    final parts = unitVector.map((v) {
      final rounded = double.parse(v.toStringAsFixed(3));
      return rounded == 0 ? '0.0' : rounded.toStringAsFixed(3);
    }).join(',');
    return '[$parts]';
  }

  static List<double> parseVector(String raw) {
    final cleaned = raw.replaceAll(RegExp(r'[\[\]\s]'), '');
    if (cleaned.isEmpty) return const [];
    return cleaned.split(',').map(double.parse).toList(growable: false);
  }

  static double cosineSimilarity(String embeddingA, String embeddingB) {
    try {
      final a = parseVector(embeddingA);
      final b = parseVector(embeddingB);
      if (a.isEmpty || a.length != b.length) return 0.0;
      var dot = 0.0;
      var na = 0.0;
      var nb = 0.0;
      for (var i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        na += a[i] * a[i];
        nb += b[i] * b[i];
      }
      if (na == 0.0 || nb == 0.0) return 0.0;
      return dot / (math.sqrt(na) * math.sqrt(nb));
    } catch (_) {
      return 0.0;
    }
  }
}
