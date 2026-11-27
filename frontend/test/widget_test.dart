import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/config/app_config.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/session/session_storage.dart';
import 'package:gestopago_app/main.dart';

class _TestHttpAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    Object? payload;
    int status = 200;

    if (options.path == '/auth/login') {
      final raw = options.data is Map<String, dynamic>
          ? options.data as Map<String, dynamic>
          : jsonDecode(options.data.toString()) as Map<String, dynamic>;
      if (raw['username'] == 'admin@gestopagos.mx') {
        status = 401;
        payload = {
          'codigo': 401,
          'codigoCatalogo': 'AUTENTICACION_2FA_FALLIDA',
          'mensaje': 'Credenciales inválidas',
        };
      } else {
        payload = {
          'tokenSesion': 'test-token-123',
          'activa': true,
          'tipoToken': 'Bearer',
          'minutosInactividadMaximos': 5,
          'idUsuario': 1,
          'idCliente': 10,
          'username': 'ana@correo.com',
          'nombreCliente': 'Ana Lopez',
        };
      }
    } else if (options.path == '/auth/backoffice/login') {
      payload = {
        'requiere2fa': true,
        'challengeId': 'ch-001',
        'emailEnmascarado': 'ad***@gestopagos.mx',
        'expiraEnSegundos': 300,
        'mensaje': 'Código enviado a ad***@gestopagos.mx',
      };
    } else if (options.path == '/auth/backoffice/verificar-2fa') {
      payload = {
        'autenticado': true,
        'tokenBackoffice': 'bo-tok-001',
        'email': 'admin@gestopagos.mx',
        'metodoUsado': 'EMAIL_OTP',
      };
    } else if (options.path == '/clientes/consultar') {
      payload = {
        'nombre': 'Ana',
        'apellidoPaterno': 'Lopez',
        'apellidoMaterno': 'Perez',
        'nombreCompleto': 'Ana Lopez Perez',
        'fechaNacimiento': '1995-04-12',
        'edad': 31,
        'curp': 'LOPA950412MDFRRL09',
        'rfc': 'LOPA950412AB1',
        'sexo': 'FEMENINO',
        'idNacionalidad': 1,
        'nacionalidad': 'Mexicana',
        'estadoCivil': 'Soltero',
        'correoElectronico': 'ana@correo.com',
        'telefonoMovil': '5511223344',
        'ocupacion': 'Ingeniera',
        'empresa': 'Tech MX',
        'ingresoMensual': 30000.0,
        'activo': true,
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
              'saldoDisponible': 2500.0,
              'saldoContable': 2500.0,
            },
          },
        ],
      };
    } else if (options.path == '/cuentas/4555000011112222/saldo') {
      payload = {
        'numeroCuenta': '4555000011112222',
        'saldoDisponible': 2500.0,
        'saldoContable': 2500.0,
      };
    } else if (options.path == '/pagos/historial') {
      payload = <dynamic>[];
    } else if (options.path == '/catalogos/nacionalidades') {
      payload = [
        {
          'idNacionalidad': 1,
          'claveIso': 'MEX',
          'pais': 'México',
          'gentilicio': 'Mexicana',
        },
      ];
    } else if (options.path == '/clientes/buscar') {
      payload = <dynamic>[];
    } else if (options.path == '/cuentas/activas') {
      payload = <dynamic>[];
    } else {
      payload = <String, dynamic>{};
    }

    return ResponseBody.fromString(
      jsonEncode(payload),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Dio _buildMockDio() {
  final dio = Dio(BaseOptions(baseUrl: AppEnvironment.productionBaseUrl));
  dio.httpClientAdapter = _TestHttpAdapter();
  return dio;
}

void main() {
  test('URL base predeterminada apunta a produccion sin servidor local', () {
    expect(
      AppEnvironment.defaultBaseUrl,
      'https://gesto-pagos-api.onrender.com',
    );
  });

  testWidgets(
      'LoginScreen oculta configuracion de servidor y backoffice directo, y navega a Onboarding',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          dioProvider.overrideWithValue(_buildMockDio()),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
        ],
        child: const GestoPagosApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Iniciar Sesión'), findsOneWidget);
    expect(find.byIcon(Icons.dns_outlined), findsNothing);
    expect(find.text('Abrir Portal BackOffice / Admin'), findsNothing);

    final onboardingBtn = find.text('Crear nueva cuenta');
    await tester.ensureVisible(onboardingBtn);
    await tester.tap(onboardingBtn);
    await tester.pumpAndSettle();
    expect(find.text('Abrir Cuenta Digital'), findsOneWidget);
  });

  testWidgets('Inicia sesion en Portal Cliente y muestra saldo y temporizador', (
    WidgetTester tester,
  ) async {
    final container = ProviderContainer(
      overrides: [
        dioProvider.overrideWithValue(_buildMockDio()),
        tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
      ],
    );
    addTearDown(container.dispose);

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
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Contraseña'),
      'Password#1',
    );
    await tester.tap(find.text('Iniciar sesión'));
    await tester.pumpAndSettle();

    expect(find.textContaining('05:00'), findsOneWidget);
    expect(find.text('\$2,500.00 MXN'), findsAtLeastNWidgets(1));
    expect(find.text('Saldo contable total'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Actividad Reciente'), 200);
    expect(find.text('Actividad Reciente'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.logout));
    await tester.pumpAndSettle();
    expect(find.text('Iniciar Sesión'), findsOneWidget);
  });

  testWidgets('Login administrativo abre 2FA con 6 casillas OTP y accede a Consola Operativa',
      (WidgetTester tester) async {
    final container = ProviderContainer(
      overrides: [
        dioProvider.overrideWithValue(_buildMockDio()),
        tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
      ],
    );
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const GestoPagosApp(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextFormField, 'Correo electrónico'),
      'admin@gestopagos.mx',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Contraseña'),
      'AdminPass#1',
    );
    await tester.tap(find.text('Iniciar sesión'));
    await tester.pumpAndSettle();

    expect(find.text('Verificación en Dos Pasos'), findsOneWidget);
    // Ingresar 6 dígitos en la primera casilla (soporta pegado/entrada completa)
    await tester.enterText(find.byKey(const ValueKey('otp_box_0')), '123456');
    await tester.pumpAndSettle();

    await tester.tap(find.text('Verificar e ingresar'));
    await tester.pumpAndSettle();

    expect(find.text('Consola Operativa'), findsOneWidget);
    expect(find.byIcon(Icons.dns_outlined), findsNothing);

    await tester.tap(find.text('Salir'));
    await tester.pumpAndSettle();
    expect(find.text('Iniciar Sesión'), findsOneWidget);
  });
}
