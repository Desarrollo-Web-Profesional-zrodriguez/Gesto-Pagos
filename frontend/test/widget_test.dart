import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
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
    if (options.path == '/auth/login') {
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
        'sexo': 'Femenino',
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
    } else if (options.path == '/catalogos/nacionalidades') {
      payload = [
        {'idNacionalidad': 1, 'pais': 'Mexico', 'gentilicio': 'Mexicana'},
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
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Dio _buildMockDio() {
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
  dio.httpClientAdapter = _TestHttpAdapter();
  return dio;
}

void main() {
  testWidgets('Renderiza LoginScreen, navega a Onboarding y BackOffice', (
    WidgetTester tester,
  ) async {
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

    expect(find.text('Bienvenido de nuevo'), findsOneWidget);
    expect(find.text('Iniciar sesión'), findsOneWidget);

    final onboardingBtn = find.text('Registrar nuevo cliente (Onboarding)');
    await tester.ensureVisible(onboardingBtn);
    await tester.tap(onboardingBtn);
    await tester.pumpAndSettle();
    expect(find.text('Registro de Cliente (Onboarding)'), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();

    final backofficeBtn = find.text('Abrir Portal BackOffice / Admin');
    await tester.ensureVisible(backofficeBtn);
    await tester.tap(backofficeBtn);
    await tester.pumpAndSettle();
    expect(find.text('BackOffice / Operador'), findsOneWidget);
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
    expect(find.text('\$2,500.00 MXN'), findsOneWidget);
    expect(find.text('Saldo contable: \$2,500.00 MXN'), findsOneWidget);

    // Cerrar sesion antes de terminar para cancelar el Timer.periodic limpiamente.
    await tester.tap(find.byIcon(Icons.logout));
    await tester.pumpAndSettle();
    expect(find.text('Bienvenido de nuevo'), findsOneWidget);
  });
}
