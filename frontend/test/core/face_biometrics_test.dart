import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_extractor.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_provider.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/session/session_storage.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/presentation/perfil_tab.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_screen.dart';
import 'package:gestopago_app/main.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

class _FakeFaceEmbeddingProvider implements FaceEmbeddingProvider {
  _FakeFaceEmbeddingProvider(this.resultToReturn);

  final String? resultToReturn;
  int captureCalls = 0;

  @override
  bool get isCameraSupported => true;

  @override
  String get statusMessage => 'Cámara simulada lista';

  @override
  Future<String?> captureFacialEmbedding(
    BuildContext context, {
    String title = 'Reconocimiento Facial',
    String subtitle = '',
  }) async {
    captureCalls++;
    return resultToReturn;
  }
}

class _MockAdapter implements HttpClientAdapter {
  _MockAdapter(this.handler);

  final Future<ResponseBody> Function(RequestOptions options) handler;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) =>
      handler(options);

  @override
  void close({bool force = false}) {}
}

ResponseBody _json(int status, Object? body) => ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );

Face _buildSyntheticFace({
  double scale = 1.0,
  double offsetX = 0.0,
  double offsetY = 0.0,
  double rollDeg = 0.0,
  double? yawDeg = 2.0,
  double? leftEyeOpen = 0.92,
  double? rightEyeOpen = 0.94,
  double noseFactor = 0.66,
  double mouthWidthFactor = 0.96,
  double chinFactor = 1.48,
  double headWidthFactor = 2.55,
}) {
  final rad = rollDeg * math.pi / 180.0;
  final cosA = math.cos(rad);
  final sinA = math.sin(rad);

  const baseDist = 100.0;
  final d = baseDist * scale;
  final cx = 250.0 + offsetX;
  final cy = 300.0 + offsetY;

  math.Point<int> pt(double ux, double vy) {
    final rx = (ux * cosA - vy * sinA) * d + cx;
    final ry = (ux * sinA + vy * cosA) * d + cy;
    return math.Point<int>(rx.round(), ry.round());
  }

  final landmarks = <FaceLandmarkType, FaceLandmark?>{
    FaceLandmarkType.leftEye: FaceLandmark(
      type: FaceLandmarkType.leftEye,
      position: pt(-0.5, 0.0),
    ),
    FaceLandmarkType.rightEye: FaceLandmark(
      type: FaceLandmarkType.rightEye,
      position: pt(0.5, 0.0),
    ),
    FaceLandmarkType.noseBase: FaceLandmark(
      type: FaceLandmarkType.noseBase,
      position: pt(0.0, noseFactor),
    ),
    FaceLandmarkType.leftMouth: FaceLandmark(
      type: FaceLandmarkType.leftMouth,
      position: pt(-mouthWidthFactor / 2, chinFactor - 0.20),
    ),
    FaceLandmarkType.rightMouth: FaceLandmark(
      type: FaceLandmarkType.rightMouth,
      position: pt(mouthWidthFactor / 2, chinFactor - 0.20),
    ),
    FaceLandmarkType.bottomMouth: FaceLandmark(
      type: FaceLandmarkType.bottomMouth,
      position: pt(0.0, chinFactor),
    ),
    FaceLandmarkType.leftCheek: FaceLandmark(
      type: FaceLandmarkType.leftCheek,
      position: pt(-0.68, noseFactor * 0.90),
    ),
    FaceLandmarkType.rightCheek: FaceLandmark(
      type: FaceLandmarkType.rightCheek,
      position: pt(0.68, noseFactor * 0.90),
    ),
    FaceLandmarkType.leftEar: FaceLandmark(
      type: FaceLandmarkType.leftEar,
      position: pt(-headWidthFactor / 2, 0.15),
    ),
    FaceLandmarkType.rightEar: FaceLandmark(
      type: FaceLandmarkType.rightEar,
      position: pt(headWidthFactor / 2, 0.15),
    ),
  };

  final boxW = headWidthFactor * 1.08 * d;
  final boxH = (chinFactor + 1.55) * d;

  return Face(
    boundingBox: Rect.fromCenter(
      center: Offset(cx, cy + 0.35 * d),
      width: boxW,
      height: boxH,
    ),
    landmarks: landmarks,
    contours: const <FaceContourType, FaceContour?>{},
    headEulerAngleX: 1.5,
    headEulerAngleY: yawDeg,
    headEulerAngleZ: rollDeg,
    leftEyeOpenProbability: leftEyeOpen,
    rightEyeOpenProbability: rightEyeOpen,
  );
}

void main() {
  group('FaceEmbeddingExtractor & Flujos de Reconocimiento Facial', () {
    test(
        'extrae vector invariante a escala, desplazamiento e inclinacion con longitud < 240 chars',
        () {
      final baseRes = FaceEmbeddingExtractor.extractFromFaces([
        _buildSyntheticFace(),
      ]);
      expect(baseRes.isValid, isTrue);
      expect(baseRes.serialized!.length, lessThan(240));

      final zoomedRotated = FaceEmbeddingExtractor.extractFromFaces([
        _buildSyntheticFace(
          scale: 1.45,
          offsetX: 60.0,
          offsetY: -40.0,
          rollDeg: 10.0,
        ),
      ]);
      expect(zoomedRotated.isValid, isTrue);

      final simSame = FaceEmbeddingExtractor.cosineSimilarity(
        baseRes.serialized!,
        zoomedRotated.serialized!,
      );
      expect(simSame, greaterThan(0.95));

      // Proporciones faciales distintas quedan por debajo del umbral 0.80 del backend
      final otherPerson = FaceEmbeddingExtractor.extractFromFaces([
        _buildSyntheticFace(
          noseFactor: 0.44,
          mouthWidthFactor: 0.66,
          chinFactor: 1.02,
          headWidthFactor: 1.80,
        ),
      ]);
      expect(otherPerson.isValid, isTrue);
      final simDifferent = FaceEmbeddingExtractor.cosineSimilarity(
        baseRes.serialized!,
        otherPerson.serialized!,
      );
      expect(simDifferent, lessThan(0.75));
    });

    test('valida rostro ausente, multiples rostros, giro excesivo y ojos cerrados',
        () {
      expect(
        FaceEmbeddingExtractor.extractFromFaces(const []).isValid,
        isFalse,
      );
      expect(
        FaceEmbeddingExtractor.extractFromFaces([
          _buildSyntheticFace(),
          _buildSyntheticFace(offsetX: 200),
        ]).isValid,
        isFalse,
      );
      expect(
        FaceEmbeddingExtractor.extractFromFaces([
          _buildSyntheticFace(yawDeg: 25.0),
        ]).isValid,
        isFalse,
      );
      expect(
        FaceEmbeddingExtractor.extractFromFaces([
          _buildSyntheticFace(leftEyeOpen: 0.10),
        ]).isValid,
        isFalse,
      );
    });

    testWidgets(
        'PerfilTab permite activar y desactivar reconocimiento facial confirmando contrasena',
        (WidgetTester tester) async {
      Map<String, dynamic>? lastBioPayload;
      final fakeFace =
          _FakeFaceEmbeddingProvider('[0.512,-0.411,0.754,0.019]');

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _MockAdapter((options) async {
        if (options.path == '/auth/login') {
          return _json(200, {
            'tokenSesion': 'tok-perfil-1',
            'activa': true,
            'tipoToken': 'Bearer',
            'minutosInactividadMaximos': 5,
            'username': 'ana@correo.com',
            'nombreCliente': 'Ana Lopez',
            'biometriaFacialActiva': false,
          });
        } else if (options.path == '/auth/biometria-facial') {
          lastBioPayload = options.data is String
              ? jsonDecode(options.data as String) as Map<String, dynamic>
              : options.data as Map<String, dynamic>;
          final isDelete = lastBioPayload?['eliminar'] == true;
          return _json(200, {
            'biometriaFacialActiva': !isDelete,
            'mensaje': isDelete
                ? 'Reconocimiento facial desactivado en tu cuenta.'
                : 'Reconocimiento facial activado correctamente en tu cuenta.',
          });
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          faceEmbeddingProvider.overrideWithValue(fakeFace),
        ],
      );
      addTearDown(container.dispose);

      final loginFut = container
          .read(sessionControllerProvider.notifier)
          .loginWithPassword(
            username: 'ana@correo.com',
            password: 'Password#1',
          );
      await tester.pump(const Duration(milliseconds: 50));
      await loginFut;

      container.read(sessionControllerProvider.notifier).actualizarClienteLocal(
            const ClienteDto(
              nombre: 'Ana',
              apellidoPaterno: 'Lopez',
              apellidoMaterno: 'Perez',
              nombreCompleto: 'Ana Lopez Perez',
              fechaNacimiento: '1995-01-01',
              curp: 'LOPA950101MDFRRL01',
              rfc: 'LOPA950101AB1',
              sexo: 'FEMENINO',
              estadoCivil: 'Soltero',
              correoElectronico: 'ana@correo.com',
              telefonoMovil: '5511223344',
              ocupacion: 'Ingeniera',
              empresa: 'Tech MX',
              ingresoMensual: 25000.0,
              activo: true,
              biometriaFacialActiva: false,
            ),
          );

      tester.view.physicalSize = const Size(800, 1400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const MaterialApp(
            home: Scaffold(body: PerfilTab()),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Reconocimiento Facial'), findsOneWidget);
      expect(find.text('Sin activar'), findsOneWidget);

      await tester.tap(find.text('Activar reconocimiento facial'));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.widgetWithText(TextFormField, 'Contraseña actual'),
        'Password#1',
      );
      await tester.tap(find.text('Continuar'));
      await tester.pumpAndSettle();

      expect(fakeFace.captureCalls, 1);
      expect(lastBioPayload?['passwordActual'], 'Password#1');
      expect(
        lastBioPayload?['biometricoFacialEmbedding'],
        '[0.512,-0.411,0.754,0.019]',
      );
      expect(find.text('Activo'), findsOneWidget);
      expect(find.text('Actualizar rostro'), findsOneWidget);

      // Desactivar reconocimiento facial
      await tester.tap(find.text('Desactivar'));
      await tester.pumpAndSettle();
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Contraseña actual'),
        'Password#1',
      );
      await tester.tap(find.text('Continuar'));
      await tester.pumpAndSettle();

      expect(lastBioPayload?['eliminar'], isTrue);
      expect(find.text('Sin activar'), findsOneWidget);

      final logoutFut =
          container.read(sessionControllerProvider.notifier).logout();
      await tester.pump(const Duration(milliseconds: 50));
      await logoutFut;
    });

    testWidgets(
        'LoginScreen permite iniciar sesion con reconocimiento facial',
        (WidgetTester tester) async {
      String? loginEmbeddingSent;
      final fakeFace =
          _FakeFaceEmbeddingProvider('[0.621,0.334,-0.709,0.012]');

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _MockAdapter((options) async {
        if (options.path == '/auth/login') {
          final body = options.data is String
              ? jsonDecode(options.data as String) as Map<String, dynamic>
              : options.data as Map<String, dynamic>;
          loginEmbeddingSent =
              body['biometricoFacialEmbedding']?.toString();
          return _json(200, {
            'tokenSesion': 'tok-face-login',
            'activa': true,
            'tipoToken': 'Bearer',
            'minutosInactividadMaximos': 5,
            'username': 'ana@correo.com',
            'nombreCliente': 'Ana Lopez',
            'biometriaFacialActiva': true,
          });
        } else if (options.path == '/clientes/consultar') {
          return _json(200, {
            'nombre': 'Ana',
            'apellidoPaterno': 'Lopez',
            'apellidoMaterno': 'Perez',
            'nombreCompleto': 'Ana Lopez Perez',
            'fechaNacimiento': '1995-01-01',
            'curp': 'LOPA950101MDFRRL01',
            'rfc': 'LOPA950101AB1',
            'sexo': 'FEMENINO',
            'estadoCivil': 'Soltero',
            'correoElectronico': 'ana@correo.com',
            'telefonoMovil': '5511223344',
            'ocupacion': 'Ingeniera',
            'empresa': 'Tech MX',
            'ingresoMensual': 25000.0,
            'activo': true,
            'biometriaFacialActiva': true,
            'domicilios': const [],
            'cuentas': [
              {
                'idCuenta': 1,
                'nombreTitular': 'Ana Lopez Perez',
                'numeroCuenta': '4555000011112222',
                'tipoCuenta': 'DEBITO',
                'estatus': 'ACTIVA',
                'saldo': {
                  'numeroCuenta': '4555000011112222',
                  'saldoDisponible': 3000.0,
                  'saldoContable': 3000.0,
                },
              },
            ],
          });
        } else if (options.path == '/pagos/historial') {
          return _json(200, const <dynamic>[]);
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          faceEmbeddingProvider.overrideWithValue(fakeFace),
        ],
      );
      addTearDown(container.dispose);

      tester.view.physicalSize = const Size(800, 1400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const GestoPagosApp(),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.widgetWithText(TextFormField, 'Correo electrónico'),
        'ana@correo.com',
      );
      await tester.tap(find.text('Iniciar sesión con reconocimiento facial'));
      await tester.pumpAndSettle();

      expect(loginEmbeddingSent, '[0.621,0.334,-0.709,0.012]');
      expect(find.text('\$3,000.00 MXN'), findsAtLeastNWidgets(1));

      final logoutFut =
          container.read(sessionControllerProvider.notifier).logout();
      await tester.pump(const Duration(milliseconds: 50));
      await logoutFut;
    });

    testWidgets(
        'PagoScreen muestra opcion Rostro y autoriza 2FA con reconocimiento facial',
        (WidgetTester tester) async {
      String? pago2faSent;
      final fakeFace =
          _FakeFaceEmbeddingProvider('[0.621,0.334,-0.709,0.012]');

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _MockAdapter((options) async {
        if (options.path == '/auth/login') {
          return _json(200, {
            'tokenSesion': 'tok-face-pay',
            'activa': true,
            'tipoToken': 'Bearer',
            'minutosInactividadMaximos': 5,
            'username': 'ana@correo.com',
            'nombreCliente': 'Ana Lopez',
            'biometriaFacialActiva': true,
          });
        } else if (options.path == '/pagos/validar-referencia') {
          return _json(200, {
            'valida': true,
            'mensaje': 'Referencia verificada',
            'servicio': 'CFE',
            'producto': 'CFE Basico',
            'referenciaNormalizada': '9988776655',
            'numCuentaEnmascarada': '**** **** **** 2222',
            'montoValidado': 100.0,
            'saldoDisponibleActual': 3000.0,
            'saldoSuficiente': true,
          });
        } else if (options.path == '/pagos') {
          final body = options.data is String
              ? jsonDecode(options.data as String) as Map<String, dynamic>
              : options.data as Map<String, dynamic>;
          pago2faSent = body['2fa']?.toString();
          return _json(200, {
            'codigo': '01',
            'mensaje': 'Pago aplicado',
            'numAutorizacion': 'AUT555777',
            'upc': 'UPC9988776',
            'numCuenta': '4555000011112222',
            'idServicio': 76,
            'servicio': 'CFE',
            'idProducto': 205,
            'producto': 'CFE Basico',
            'referencia': '9988776655',
            'montoPagado': 100.0,
            'saldoDisponibleActual': 2900.0,
          });
        } else if (options.path == '/cuentas/4555000011112222/saldo') {
          return _json(200, {
            'numeroCuenta': '4555000011112222',
            'saldoDisponible': 2900.0,
            'saldoContable': 2900.0,
          });
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          faceEmbeddingProvider.overrideWithValue(fakeFace),
        ],
      );
      addTearDown(container.dispose);

      final loginFut = container
          .read(sessionControllerProvider.notifier)
          .loginWithPassword(
            username: 'ana@correo.com',
            password: 'Password#1',
          );
      await tester.pump(const Duration(milliseconds: 50));
      await loginFut;

      container.read(sessionControllerProvider.notifier).actualizarClienteLocal(
            const ClienteDto(
              nombre: 'Ana',
              apellidoPaterno: 'Lopez',
              apellidoMaterno: 'Perez',
              nombreCompleto: 'Ana Lopez Perez',
              fechaNacimiento: '1995-01-01',
              curp: 'LOPA950101MDFRRL01',
              rfc: 'LOPA950101AB1',
              sexo: 'FEMENINO',
              estadoCivil: 'Soltero',
              correoElectronico: 'ana@correo.com',
              telefonoMovil: '5511223344',
              ocupacion: 'Ingeniera',
              empresa: 'Tech MX',
              ingresoMensual: 25000.0,
              activo: true,
              biometriaFacialActiva: true,
              cuentas: [
                CuentaDto(
                  idCuenta: 1,
                  nombreTitular: 'Ana Lopez Perez',
                  numeroCuenta: '4555000011112222',
                  tipoCuenta: 'DEBITO',
                  estatus: 'ACTIVA',
                  saldo: SaldoDto(
                    numeroCuenta: '4555000011112222',
                    saldoDisponible: 3000.0,
                    saldoContable: 3000.0,
                  ),
                ),
              ],
            ),
          );

      tester.view.physicalSize = const Size(800, 1400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const MaterialApp(
            home: PagoScreen(
              producto: ProductItemDto(
                idProducto: 205,
                idServicio: 76,
                servicio: 'CFE',
                producto: 'CFE Basico',
                tipoFront: 2,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.widgetWithText(TextFormField, 'Referencia o línea de captura *'),
        '9988776655',
      );
      await tester.tap(find.text('\$100.00'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Validar y continuar'));
      await tester.pumpAndSettle();

      // Seleccionar segmento 'Rostro'
      expect(find.text('Rostro'), findsOneWidget);
      await tester.tap(find.text('Rostro'));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byType(CheckboxListTile));
      await tester.tap(find.byType(CheckboxListTile));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.text('Confirmar y pagar'));
      await tester.tap(find.text('Confirmar y pagar'));
      await tester.pumpAndSettle();

      expect(pago2faSent, '[0.621,0.334,-0.709,0.012]');
      expect(find.text('¡Pago Aplicado Exitosamente!'), findsOneWidget);

      final logoutFut =
          container.read(sessionControllerProvider.notifier).logout();
      await tester.pump(const Duration(milliseconds: 50));
      await logoutFut;
    });
  });
}
