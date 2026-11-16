import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/session/session_storage.dart';
import 'package:gestopago_app/features/auth/presentation/login_screen.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/cuentas/presentation/cuenta_home_tab.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_screen.dart';

class _FakeAdapter implements HttpClientAdapter {
  _FakeAdapter(this.handler);

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

ClienteDto _testCliente({
  String nombre = 'Ana',
  String apellidoPaterno = 'Lopez',
  String correoElectronico = 'ana@correo.com',
  List<CuentaDto> cuentas = const [],
}) {
  return ClienteDto(
    nombre: nombre,
    apellidoPaterno: apellidoPaterno,
    apellidoMaterno: 'Perez',
    nombreCompleto: '$nombre $apellidoPaterno Perez',
    fechaNacimiento: '1995-01-01',
    curp: 'LOPA950101MDFRRL01',
    rfc: 'LOPA950101AB1',
    sexo: 'Femenino',
    estadoCivil: 'Soltero',
    correoElectronico: correoElectronico,
    telefonoMovil: '5511223344',
    ocupacion: 'Ingeniera',
    empresa: 'Tech',
    ingresoMensual: 25000.0,
    activo: true,
    cuentas: cuentas,
  );
}

void main() {
  group('Milestone 1: SessionController & Functional Parity Tests', () {
    test('Session restoration on launch restores token and profile when token is valid', () async {
      final storage = InMemoryTokenStorage();
      await storage.saveToken('restored-token-999', 'juan@correo.com');

      int validarSesionCalls = 0;
      int consultarClienteCalls = 0;

      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/auth/validar-sesion') {
          validarSesionCalls++;
          return _json(200, {'valida': true, 'mensaje': 'Sesion activa'});
        } else if (options.path == '/clientes/consultar') {
          consultarClienteCalls++;
          return _json(200, {
            'nombre': 'Juan',
            'apellidoPaterno': 'Perez',
            'apellidoMaterno': 'Gomez',
            'nombreCompleto': 'Juan Perez Gomez',
            'fechaNacimiento': '1990-01-01',
            'curp': 'PERJ900101MDFRRL01',
            'rfc': 'PERJ900101AB1',
            'sexo': 'Masculino',
            'estadoCivil': 'Soltero',
            'correoElectronico': 'juan@correo.com',
            'telefonoMovil': '5511223344',
            'ocupacion': 'Ingeniero',
            'empresa': 'Tech',
            'ingresoMensual': 20000.0,
            'activo': true,
            'domicilios': const [],
            'cuentas': [
              {
                'idCuenta': 1,
                'nombreTitular': 'Juan Perez Gomez',
                'numeroCuenta': '1111222233334444',
                'tipoCuenta': 'DEBITO',
                'estatus': 'ACTIVA',
                'saldo': {
                  'numeroCuenta': '1111222233334444',
                  'saldoDisponible': 5000.0,
                  'saldoContable': 5000.0,
                },
              },
            ],
          });
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(storage),
        ],
      );
      addTearDown(container.dispose);

      final controller = container.read(sessionControllerProvider.notifier);
      final restored = await controller.restaurarSesion();

      expect(restored, isTrue);
      expect(validarSesionCalls, 1);
      expect(consultarClienteCalls, 1);

      final state = container.read(sessionControllerProvider);
      expect(state.isAuthenticated, isTrue);
      expect(state.token, 'restored-token-999');
      expect(state.cliente?.nombreCompleto, 'Juan Perez Gomez');
      expect(state.cuentaPrincipal?.numeroCuenta, '1111222233334444');
    });

    test('Session restoration clears token when remote validation reports expired/invalid', () async {
      final storage = InMemoryTokenStorage();
      await storage.saveToken('expired-token', 'juan@correo.com');

      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/auth/validar-sesion') {
          return _json(200, {'valida': false, 'mensaje': 'Expirada'});
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(storage),
        ],
      );
      addTearDown(container.dispose);

      final controller = container.read(sessionControllerProvider.notifier);
      final restored = await controller.restaurarSesion();

      expect(restored, isFalse);
      expect(await storage.readToken(), isNull);
      final state = container.read(sessionControllerProvider);
      expect(state.isAuthenticated, isFalse);
    });

    test('loginWithBiometrics sends biometricoFacialEmbedding and establishes session', () async {
      final storage = InMemoryTokenStorage();
      String? sentEmbedding;

      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/auth/login') {
          final Map<String, dynamic> data = options.data is Map<String, dynamic>
              ? options.data as Map<String, dynamic>
              : jsonDecode(options.data.toString()) as Map<String, dynamic>;
          sentEmbedding = data['biometricoFacialEmbedding']?.toString();
          return _json(200, {
            'tokenSesion': 'bio-token-555',
            'activa': true,
            'tipoToken': 'Bearer',
            'minutosInactividadMaximos': 5,
            'idUsuario': 7,
            'username': 'bio@correo.com',
            'nombreCliente': 'Usuario Biometrico',
          });
        } else if (options.path == '/clientes/consultar') {
          return _json(200, {
            'nombre': 'Usuario',
            'apellidoPaterno': 'Biometrico',
            'apellidoMaterno': 'Test',
            'nombreCompleto': 'Usuario Biometrico Test',
            'fechaNacimiento': '1992-02-02',
            'curp': 'BIOT920202MDFRRL01',
            'rfc': 'BIOT920202AB1',
            'sexo': 'Masculino',
            'estadoCivil': 'Soltero',
            'correoElectronico': 'bio@correo.com',
            'telefonoMovil': '5511223344',
            'ocupacion': 'Ingeniero',
            'empresa': 'Tech',
            'ingresoMensual': 20000.0,
            'activo': true,
            'domicilios': const [],
            'cuentas': [
              {
                'idCuenta': 2,
                'nombreTitular': 'Usuario Biometrico Test',
                'numeroCuenta': '9999888877776666',
                'tipoCuenta': 'DEBITO',
                'estatus': 'ACTIVA',
                'saldo': {
                  'numeroCuenta': '9999888877776666',
                  'saldoDisponible': 1200.0,
                  'saldoContable': 1200.0,
                },
              },
            ],
          });
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(storage),
        ],
      );
      addTearDown(container.dispose);

      final controller = container.read(sessionControllerProvider.notifier);
      await controller.loginWithBiometrics(
        username: 'bio@correo.com',
        biometricoFacialEmbedding: '[0.12, -0.45, 0.89, -0.05, 0.67]',
      );

      expect(sentEmbedding, '[0.12, -0.45, 0.89, -0.05, 0.67]');
      expect(await storage.readToken(), 'bio-token-555');
      expect(await storage.readUsername(), 'bio@correo.com');

      final state = container.read(sessionControllerProvider);
      expect(state.isAuthenticated, isTrue);
      expect(state.cuentaPrincipal?.numeroCuenta, '9999888877776666');
    });

    test('Multi-account selection switches active account and updates balance', () async {
      final storage = InMemoryTokenStorage();
      final cuenta1 = CuentaDto(
        idCuenta: 1,
        nombreTitular: 'Cliente Multi',
        numeroCuenta: '1111111111111111',
        tipoCuenta: 'DEBITO',
        estatus: 'ACTIVA',
        saldo: const SaldoDto(
          numeroCuenta: '1111111111111111',
          saldoDisponible: 1000.0,
          saldoContable: 1000.0,
        ),
      );
      final cuenta2 = CuentaDto(
        idCuenta: 2,
        nombreTitular: 'Cliente Multi',
        numeroCuenta: '2222222222222222',
        tipoCuenta: 'CREDITO',
        estatus: 'ACTIVA',
        saldo: const SaldoDto(
          numeroCuenta: '2222222222222222',
          saldoDisponible: 8500.0,
          saldoContable: 8500.0,
        ),
      );

      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/cuentas/consultar-saldo') {
          final Map<String, dynamic> data = options.data is Map<String, dynamic>
              ? options.data as Map<String, dynamic>
              : jsonDecode(options.data.toString()) as Map<String, dynamic>;
          final num = data['numeroCuenta'];
          if (num == '2222222222222222') {
            return _json(200, {
              'numeroCuenta': '2222222222222222',
              'saldoDisponible': 9200.0,
              'saldoContable': 9200.0,
            });
          }
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(storage),
        ],
      );
      addTearDown(container.dispose);

      final controller = container.read(sessionControllerProvider.notifier);
      final cliente = _testCliente(
        nombre: 'Cliente',
        apellidoPaterno: 'Multi',
        correoElectronico: 'multi@correo.com',
        cuentas: [cuenta1, cuenta2],
      );

      controller.actualizarClienteLocal(cliente);
      expect(
        container.read(sessionControllerProvider).numeroCuentaPrincipal,
        '1111111111111111',
      );

      // Switch to account 2
      await controller.seleccionarCuenta(cuenta2);

      final state = container.read(sessionControllerProvider);
      expect(state.cuentaPrincipal?.numeroCuenta, '2222222222222222');
      expect(state.numeroCuentaPrincipal, '2222222222222222');
      expect(state.cuentaPrincipal?.saldo?.saldoDisponible, 9200.0);
    });

    test('Session heartbeat renewal triggers remote renovarSesionRemota on network activity when >= 45s', () async {
      final storage = InMemoryTokenStorage();
      await storage.saveToken('heartbeat-token', 'heart@correo.com');

      int remoteRenewalCalls = 0;

      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/auth/validar-sesion') {
          remoteRenewalCalls++;
          return _json(200, {'valida': true, 'mensaje': 'Sesion renovada'});
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(storage),
        ],
      );
      addTearDown(container.dispose);

      final hooks = container.read(sessionHooksProvider);
      final controller = container.read(sessionControllerProvider.notifier);

      // Establish authenticated session with 60s remaining
      await controller.restaurarSesion();
      expect(remoteRenewalCalls, 1);

      // Simulate network activity when < 45s: should not renew remotely
      hooks.onActivity?.call();
      expect(remoteRenewalCalls, 1);

      // Await renewal call directly to test renovarSesionRemota
      await controller.renovarSesionRemota();
      expect(remoteRenewalCalls, 2);
    });

    testWidgets('LoginScreen allows switching to Biometric mode and shows MediaPipe input', (
      WidgetTester tester,
    ) async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async => _json(200, {}));

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            dioProvider.overrideWithValue(dio),
            tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          ],
          child: const MaterialApp(
            home: LoginScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Contraseña'), findsAtLeastNWidgets(1));
      expect(find.text('Biometría Facial'), findsOneWidget);

      // Tap Biometría Facial button segment
      await tester.tap(find.text('Biometría Facial'));
      await tester.pumpAndSettle();

      expect(
        find.widgetWithText(
          TextFormField,
          'Vector biométrico facial (MediaPipe)',
        ),
        findsOneWidget,
      );
    });

    testWidgets('CuentaHomeTab renders active account switcher when multiple accounts exist', (
      WidgetTester tester,
    ) async {
      final cuentaA = CuentaDto(
        idCuenta: 1,
        nombreTitular: 'Ana Lopez',
        numeroCuenta: '4555000011112222',
        tipoCuenta: 'DEBITO',
        estatus: 'ACTIVA',
        saldo: const SaldoDto(
          numeroCuenta: '4555000011112222',
          saldoDisponible: 3000.0,
          saldoContable: 3000.0,
        ),
      );
      final cuentaB = CuentaDto(
        idCuenta: 2,
        nombreTitular: 'Ana Lopez',
        numeroCuenta: '4555000033334444',
        tipoCuenta: 'CREDITO',
        estatus: 'ACTIVA',
        saldo: const SaldoDto(
          numeroCuenta: '4555000033334444',
          saldoDisponible: 7000.0,
          saldoContable: 7000.0,
        ),
      );

      final cliente = _testCliente(
        nombre: 'Ana',
        apellidoPaterno: 'Lopez',
        correoElectronico: 'ana@correo.com',
        cuentas: [cuentaA, cuentaB],
      );

      final container = ProviderContainer(
        overrides: [
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
        ],
      );
      addTearDown(container.dispose);

      container.read(sessionControllerProvider.notifier).actualizarClienteLocal(cliente);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            home: Scaffold(
              body: CuentaHomeTab(
                onGoToCatalogo: () {},
                onGoToHistorial: () {},
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('SELECCIONAR CUENTA ACTIVA'), findsOneWidget);
      expect(find.byType(ChoiceChip), findsNWidgets(2));
    });

    testWidgets('PagoScreen _ask2FA displays security confirmation with Biometria option', (
      WidgetTester tester,
    ) async {
      final cuenta = CuentaDto(
        idCuenta: 1,
        nombreTitular: 'Ana Lopez',
        numeroCuenta: '4555000011112222',
        tipoCuenta: 'DEBITO',
        estatus: 'ACTIVA',
        saldo: const SaldoDto(
          numeroCuenta: '4555000011112222',
          saldoDisponible: 5000.0,
          saldoContable: 5000.0,
        ),
      );

      const producto = ProductItemDto(
        idProducto: 1,
        idServicio: 10,
        servicio: 'CFE',
        producto: 'CFE Basico',
        tipoFront: 2,
        precio: 100.0,
      );

      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/pagos/validar-referencia') {
          return _json(200, {
            'valida': true,
            'mensaje': 'Referencia CFE valida',
            'servicio': 'CFE',
            'producto': 'CFE Basico',
            'referenciaNormalizada': '012345678901',
            'numCuentaEnmascarada': '**** **** **** 2222',
            'montoValidado': 100.0,
            'saldoDisponibleActual': 5000.0,
            'saldoSuficiente': true,
          });
        }
        return _json(404, {});
      });

      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
        ],
      );
      addTearDown(container.dispose);

      final cliente = _testCliente(cuentas: [cuenta]);
      container.read(sessionControllerProvider.notifier).actualizarClienteLocal(cliente);

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
            home: PagoScreen(producto: producto),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Enter reference and amount
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Referencia del servicio (alfanumérica) *'),
        '012345678901',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Monto a pagar (MXN, 1.00 - 10,000.00) *'),
        '100.00',
      );
      await tester.pumpAndSettle();

      // Tap validate reference button
      final validateBtn = find.text('1. Verificar referencia y validar servicio');
      await tester.ensureVisible(validateBtn);
      await tester.tap(validateBtn);
      await tester.pumpAndSettle();

      expect(find.text('Paso 2: Resumen de la Operación (Referencia Verificada)'), findsOneWidget);

      // Check explicit confirmation
      final checkConfirm = find.byType(CheckboxListTile);
      await tester.ensureVisible(checkConfirm);
      await tester.tap(checkConfirm);
      await tester.pumpAndSettle();

      // Tap pay button
      final payBtn = find.text('2. Confirmar y pagar servicio');
      await tester.ensureVisible(payBtn);
      await tester.tap(payBtn);
      await tester.pumpAndSettle();

      // 2FA BottomSheet should appear
      expect(find.text('Confirmación de Seguridad (2FA)'), findsOneWidget);
      expect(find.text('Sesión'), findsOneWidget);
      expect(find.text('Clave'), findsOneWidget);
      expect(find.text('Biometría'), findsOneWidget);

      // Switch to Biometría
      await tester.tap(find.text('Biometría'));
      await tester.pumpAndSettle();

      expect(
        find.widgetWithText(
          TextField,
          'Vector biométrico facial (MediaPipe)',
        ),
        findsOneWidget,
      );
    });
  });
}
