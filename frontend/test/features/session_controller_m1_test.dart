import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/session/session_storage.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/core/widgets/otp_code_input.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/catalogo_productos/presentation/catalogo_productos_tab.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/presentation/cliente_edit_screen.dart';
import 'package:gestopago_app/features/cuentas/presentation/cuenta_home_tab.dart';
import 'package:gestopago_app/features/onboarding/presentation/onboarding_wizard_screen.dart';
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
    sexo: 'FEMENINO',
    idNacionalidad: 1,
    nacionalidad: 'Mexicana',
    estadoCivil: 'Soltero',
    correoElectronico: correoElectronico,
    telefonoMovil: '5511223344',
    ocupacion: 'Ingeniera',
    empresa: 'Tech MX',
    ingresoMensual: 25000.0,
    activo: true,
    domicilios: const [
      DomicilioDto(
        calle: 'Av. Reforma',
        numeroExterior: '100',
        colonia: 'Centro',
        municipio: 'Cuauhtemoc',
        estado: 'CDMX',
        codigoPostal: '06000',
        pais: 'Mexico',
      ),
    ],
    cuentas: cuentas,
  );
}

void main() {
  group('Session, UI/UX Redesign & Functional Parity Tests', () {
    test('Session restoration restores token and profile when token is valid',
        () async {
      final storage = InMemoryTokenStorage();
      await storage.saveToken('restored-token-999', 'juan@correo.com');

      var validarSesionCalls = 0;
      var consultarClienteCalls = 0;

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
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

    test(
        'Session restoration clears token when remote validation reports invalid',
        () async {
      final storage = InMemoryTokenStorage();
      await storage.saveToken('expired-token', 'juan@correo.com');

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
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
      expect(container.read(sessionControllerProvider).isAuthenticated, isFalse);
    });

    test('loginWithBiometrics sends embedding and establishes session', () async {
      final storage = InMemoryTokenStorage();
      String? sentEmbedding;

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/auth/login') {
          final data = options.data is Map<String, dynamic>
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
      expect(
        container.read(sessionControllerProvider).cuentaPrincipal?.numeroCuenta,
        '9999888877776666',
      );
    });

    test('Multi-account selection switches active account and updates balance',
        () async {
      final storage = InMemoryTokenStorage();
      const cuenta1 = CuentaDto(
        idCuenta: 1,
        nombreTitular: 'Cliente Multi',
        numeroCuenta: '1111111111111111',
        tipoCuenta: 'DEBITO',
        estatus: 'ACTIVA',
        saldo: SaldoDto(
          numeroCuenta: '1111111111111111',
          saldoDisponible: 1000.0,
          saldoContable: 1000.0,
        ),
      );
      const cuenta2 = CuentaDto(
        idCuenta: 2,
        nombreTitular: 'Cliente Multi',
        numeroCuenta: '2222222222222222',
        tipoCuenta: 'CREDITO',
        estatus: 'ACTIVA',
        saldo: SaldoDto(
          numeroCuenta: '2222222222222222',
          saldoDisponible: 8500.0,
          saldoContable: 8500.0,
        ),
      );

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/cuentas/consultar-saldo') {
          final data = options.data is Map<String, dynamic>
              ? options.data as Map<String, dynamic>
              : jsonDecode(options.data.toString()) as Map<String, dynamic>;
          if (data['numeroCuenta'] == '2222222222222222') {
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
      await controller.seleccionarCuenta(cuenta2);

      final state = container.read(sessionControllerProvider);
      expect(state.cuentaPrincipal?.numeroCuenta, '2222222222222222');
      expect(state.cuentaPrincipal?.saldo?.saldoDisponible, 9200.0);
    });

    testWidgets('OtpCodeInput acepta pegado de 6 digitos en 6 casillas',
        (WidgetTester tester) async {
      var captured = '';
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OtpCodeInput(
              value: '',
              onChange: (val) => captured = val,
            ),
          ),
        ),
      );

      await tester.enterText(find.byKey(const ValueKey('otp_box_0')), '482910');
      await tester.pump();
      expect(captured, '482910');
    });

    testWidgets('ErrorBanner oculta nombres internos de enums y muestra texto comercial',
        (WidgetTester tester) async {
      const apiErr = ApiException(
        statusCode: 401,
        codigo: 401,
        codigoCatalogo: 'AUTENTICACION_2FA_FALLIDA',
        mensaje: 'Credenciales inválidas.',
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorBanner(error: apiErr),
          ),
        ),
      );

      expect(find.text('No se pudo completar la operación'), findsOneWidget);
      expect(find.text('Credenciales inválidas.'), findsOneWidget);
      expect(find.textContaining('AUTENTICACION_2FA_FALLIDA'), findsNothing);
    });

    testWidgets(
        'CuentaHomeTab muestra selector de cuentas, modo discreto y actividad reciente',
        (WidgetTester tester) async {
      const cuentaA = CuentaDto(
        idCuenta: 1,
        nombreTitular: 'Ana Lopez',
        numeroCuenta: '4555000011112222',
        tipoCuenta: 'DEBITO',
        estatus: 'ACTIVA',
        saldo: SaldoDto(
          numeroCuenta: '4555000011112222',
          saldoDisponible: 3000.0,
          saldoContable: 3500.0,
        ),
      );
      const cuentaB = CuentaDto(
        idCuenta: 2,
        nombreTitular: 'Ana Lopez',
        numeroCuenta: '4555000033334444',
        tipoCuenta: 'CREDITO',
        estatus: 'ACTIVA',
        saldo: SaldoDto(
          numeroCuenta: '4555000033334444',
          saldoDisponible: 7000.0,
          saldoContable: 7000.0,
        ),
      );

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/pagos/historial') {
          return _json(200, [
            {
              'upc': 'UPC0000001',
              'numeroCuenta': '4555000011112222',
              'idServicio': 76,
              'idProducto': 205,
              'telefono': '1111111111',
              'referencia': '25720381',
              'monto': 150.0,
              'estatus': 'EXITOSA',
              'servicio': 'CFE',
              'producto': 'Pago CFE',
            },
          ]);
        }
        return _json(404, {});
      });

      final cliente = _testCliente(cuentas: [cuentaA, cuentaB]);
      final container = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
        ],
      );
      addTearDown(container.dispose);

      container
          .read(sessionControllerProvider.notifier)
          .actualizarClienteLocal(cliente);

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

      expect(find.text('TUS CUENTAS DIGITALES (2)'), findsOneWidget);
      expect(find.byType(ChoiceChip), findsNWidgets(2));
      expect(find.text('Desglose contable'), findsOneWidget);
      expect(find.text('Retenido en conciliación'), findsOneWidget);

      // Activar modo discreto antes de desplazarse hacia abajo
      await tester.tap(find.byTooltip('Modo discreto'));
      await tester.pumpAndSettle();
      expect(find.text('••••••••'), findsOneWidget);

      await tester.scrollUntilVisible(find.text('Actividad Reciente'), 200);
      expect(find.text('Actividad Reciente'), findsOneWidget);
      expect(find.text('Pago CFE'), findsOneWidget);
    });

    testWidgets(
        'CatalogoProductosTab muestra categorias legibles sin REDIS ni IDs internos',
        (WidgetTester tester) async {
      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/productos') {
          return _json(200, {
            'status': 200,
            'message': 'OK',
            'origen': 'REDIS',
            'totalProductos': 2,
            'categorias': {
              '1': [
                {
                  'idProducto': 101,
                  'idServicio': 10,
                  'servicio': 'Telcel',
                  'producto': 'Recarga 100',
                  'precio': 100.0,
                  'tipoFront': 1,
                },
              ],
              '2': [
                {
                  'idProducto': 205,
                  'idServicio': 76,
                  'servicio': 'CFE',
                  'producto': 'Recibo CFE',
                  'tipoFront': 2,
                },
              ],
            },
          });
        }
        return _json(404, {});
      });

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            dioProvider.overrideWithValue(dio),
            tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          ],
          child: const MaterialApp(
            home: Scaffold(body: CatalogoProductosTab()),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Todas (2)'), findsOneWidget);
      expect(find.text('Recargas y Pines (1)'), findsOneWidget);
      expect(find.text('Pago de Servicios (1)'), findsOneWidget);
      expect(find.textContaining('REDIS'), findsNothing);
      expect(find.textContaining('ID Servicio'), findsNothing);
    });

    testWidgets(
        'PagoScreen ejecuta flujo 3 fases con importes rapidos, 2FA en linea y comprobante con PIN',
        (WidgetTester tester) async {
      const cuenta = CuentaDto(
        idCuenta: 1,
        nombreTitular: 'Ana Lopez',
        numeroCuenta: '4555000011112222',
        tipoCuenta: 'DEBITO',
        estatus: 'ACTIVA',
        saldo: SaldoDto(
          numeroCuenta: '4555000011112222',
          saldoDisponible: 5000.0,
          saldoContable: 5000.0,
        ),
      );

      const producto = ProductItemDto(
        idProducto: 205,
        idServicio: 76,
        servicio: 'CFE',
        producto: 'CFE Basico',
        tipoFront: 2,
      );

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/auth/login') {
          return _json(200, {
            'tokenSesion': 'tok-valid',
            'activa': true,
            'tipoToken': 'Bearer',
            'minutosInactividadMaximos': 5,
            'username': 'ana@correo.com',
            'nombreCliente': 'Ana Lopez',
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
            'domicilios': const [],
            'cuentas': [
              {
                'idCuenta': 1,
                'nombreTitular': 'Ana Lopez',
                'numeroCuenta': '4555000011112222',
                'tipoCuenta': 'DEBITO',
                'estatus': 'ACTIVA',
                'saldo': {
                  'numeroCuenta': '4555000011112222',
                  'saldoDisponible': 5000.0,
                  'saldoContable': 5000.0,
                },
              },
            ],
          });
        } else if (options.path == '/pagos/validar-referencia') {
          return _json(200, {
            'valida': true,
            'mensaje': 'Referencia CFE valida',
            'servicio': 'CFE',
            'producto': 'CFE Basico',
            'referenciaNormalizada': '012345678901',
            'numCuentaEnmascarada': '**** **** **** 2222',
            'montoValidado': 200.0,
            'saldoDisponibleActual': 5000.0,
            'saldoSuficiente': true,
          });
        } else if (options.path == '/pagos') {
          return _json(200, {
            'codigo': '01',
            'mensaje': 'Pago aplicado',
            'numAutorizacion': 'AUT987654',
            'upc': 'UPC1122334',
            'numCuenta': '4555000011112222',
            'idServicio': 76,
            'servicio': 'CFE',
            'idProducto': 205,
            'producto': 'CFE Basico',
            'referencia': '012345678901',
            'montoPagado': 200.0,
            'saldoDisponibleActual': 4800.0,
            'pin': 'PIN-1234-5678',
          });
        } else if (options.path == '/cuentas/4555000011112222/saldo') {
          return _json(200, {
            'numeroCuenta': '4555000011112222',
            'saldoDisponible': 4800.0,
            'saldoContable': 4800.0,
          });
        }
        return _json(404, {});
      });

      final testContainer = ProviderContainer(
        overrides: [
          dioProvider.overrideWithValue(dio),
          tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
        ],
      );
      addTearDown(testContainer.dispose);

      final loginFuture = testContainer
          .read(sessionControllerProvider.notifier)
          .loginWithPassword(
            username: 'ana@correo.com',
            password: 'Password#1',
          );
      await tester.pump(const Duration(milliseconds: 50));
      await loginFuture;
      testContainer
          .read(sessionControllerProvider.notifier)
          .actualizarClienteLocal(_testCliente(cuentas: [cuenta]));

      tester.view.physicalSize = const Size(800, 1400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: testContainer,
          child: const MaterialApp(
            home: PagoScreen(producto: producto),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Captura referencia y usa importe rápido $200.00
      await tester.enterText(
        find.widgetWithText(
          TextFormField,
          'Referencia o línea de captura *',
        ),
        '012345678901',
      );
      await tester.tap(find.text('\$200.00'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Validar y continuar'));
      await tester.pumpAndSettle();

      // Fase 2: Autorización
      expect(find.text('2. Referencia Verificada'), findsOneWidget);
      expect(find.text('CONFIRMACIÓN DE SEGURIDAD (2FA)'), findsOneWidget);

      await tester.ensureVisible(find.byType(CheckboxListTile));
      await tester.tap(find.byType(CheckboxListTile));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.text('Confirmar y pagar'));
      await tester.tap(find.text('Confirmar y pagar'));
      await tester.pumpAndSettle();

      // Fase 3: Comprobante Digital con PIN
      expect(find.text('¡Pago Aplicado Exitosamente!'), findsOneWidget);
      expect(find.text('PIN Digital'), findsOneWidget);
      expect(find.text('PIN-1234-5678'), findsOneWidget);
      expect(find.textContaining('Brevo'), findsNothing);

      final logoutFuture =
          testContainer.read(sessionControllerProvider.notifier).logout();
      await tester.pump(const Duration(milliseconds: 50));
      await logoutFuture;
    });

    testWidgets(
        'ClienteEditScreen organiza todas las secciones y selectores sin terminos PUT/PATCH',
        (WidgetTester tester) async {
      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/catalogos/nacionalidades') {
          return _json(200, [
            {
              'idNacionalidad': 1,
              'claveIso': 'MEX',
              'pais': 'México',
              'gentilicio': 'Mexicana',
            },
          ]);
        }
        return _json(404, {});
      });

      final cliente = _testCliente();
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            dioProvider.overrideWithValue(dio),
            tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          ],
          child: MaterialApp(
            home: ClienteEditScreen(cliente: cliente, allowFullReplace: true),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Información Personal'), findsOneWidget);
      expect(find.text('Femenino'), findsOneWidget);
      expect(find.text('Mexicana'), findsOneWidget);

      final outerScrollable = find.byType(Scrollable).first;
      await tester.scrollUntilVisible(
        find.text('Datos de Contacto'),
        200,
        scrollable: outerScrollable,
      );
      expect(find.text('Datos de Contacto'), findsOneWidget);
      await tester.scrollUntilVisible(
        find.text('Perfil Laboral e Ingresos'),
        200,
        scrollable: outerScrollable,
      );
      expect(find.text('Perfil Laboral e Ingresos'), findsOneWidget);
      await tester.scrollUntilVisible(
        find.text('Domicilio Residencial'),
        200,
        scrollable: outerScrollable,
      );
      expect(find.text('Domicilio Residencial'), findsOneWidget);
      expect(find.textContaining('PUT'), findsNothing);
      expect(find.textContaining('PATCH'), findsNothing);
    });

    testWidgets(
        'OnboardingWizardScreen usa selector searchable de nacionalidad, envia OTP y registra cuenta verificada',
        (WidgetTester tester) async {
      Map<String, dynamic>? sentOtpPayload;
      Map<String, dynamic>? registeredPayload;

      final dio =
          Dio(BaseOptions(baseUrl: 'https://gesto-pagos-api.onrender.com'));
      dio.httpClientAdapter = _FakeAdapter((options) async {
        if (options.path == '/catalogos/nacionalidades') {
          return _json(200, [
            {
              'idNacionalidad': 1,
              'claveIso': 'MEX',
              'pais': 'México',
              'gentilicio': 'Mexicana',
            },
            {
              'idNacionalidad': 2,
              'claveIso': 'COL',
              'pais': 'Colombia',
              'gentilicio': 'Colombiana',
            },
          ]);
        } else if (options.path == '/auth/onboarding/enviar-otp') {
          sentOtpPayload = options.data is String
              ? jsonDecode(options.data as String) as Map<String, dynamic>
              : options.data as Map<String, dynamic>;
          return _json(200, {
            'enviado': true,
            'emailEnmascarado': 'la***@correo.com',
            'expiraEnSegundos': 300,
            'mensaje': 'Código de verificación enviado.',
          });
        } else if (options.path == '/auth/onboarding/registrar-verificado') {
          registeredPayload = options.data is String
              ? jsonDecode(options.data as String) as Map<String, dynamic>
              : options.data as Map<String, dynamic>;
          return _json(201, {
            'nombre': 'Laura',
            'apellidoPaterno': 'Gomez',
            'apellidoMaterno': 'Ruiz',
            'nombreCompleto': 'Laura Gomez Ruiz',
            'fechaNacimiento': '1995-05-20',
            'curp': 'GORL950520MDFRRL09',
            'rfc': 'GORL9505201A0',
            'sexo': 'FEMENINO',
            'idNacionalidad': 2,
            'nacionalidad': 'Colombiana',
            'estadoCivil': 'Soltero',
            'correoElectronico': 'laura@correo.com',
            'telefonoMovil': '5599887766',
            'ocupacion': 'Diseñadora',
            'empresa': 'Estudio NX',
            'ingresoMensual': 25000.0,
            'activo': true,
            'domicilios': const [],
            'cuentas': [
              {
                'idCuenta': 9,
                'nombreTitular': 'Laura Gomez Ruiz',
                'numeroCuenta': '4555999988887777',
                'tipoCuenta': 'DEBITO',
                'estatus': 'ACTIVA',
                'saldo': {
                  'numeroCuenta': '4555999988887777',
                  'saldoDisponible': 1000.0,
                  'saldoContable': 1000.0,
                },
              },
            ],
          });
        }
        return _json(404, {});
      });

      tester.view.physicalSize = const Size(800, 1400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            dioProvider.overrideWithValue(dio),
            tokenStorageProvider.overrideWithValue(InMemoryTokenStorage()),
          ],
          child: const MaterialApp(
            home: OnboardingWizardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Paso 0: Identidad + selector searchable de nacionalidad
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Primer nombre *'),
        'Laura',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Apellido paterno *'),
        'Gomez',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Apellido materno *'),
        'Ruiz',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'CURP (18 caracteres) *'),
        'GORL950520MDFRRL09',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'RFC con homoclave *'),
        'GORL9505201A0',
      );

      // Abrir modal bottom sheet de Nacionalidad y buscar Colombia
      await tester.ensureVisible(find.text('Nacionalidad *'));
      await tester.tap(find.text('Mexicana'));
      await tester.pumpAndSettle();
      expect(find.text('Selecciona tu nacionalidad'), findsOneWidget);
      await tester.enterText(
        find.widgetWithText(TextField, 'Buscar país, nacionalidad o código...'),
        'colom',
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('Colombiana'));
      await tester.pumpAndSettle();
      expect(find.text('Colombiana'), findsOneWidget);

      await tester.tap(find.text('Siguiente'));
      await tester.pumpAndSettle();

      // Paso 1: Domicilio
      await tester.enterText(
        find.widgetWithText(
          TextFormField,
          'Correo electrónico (recibirás código OTP) *',
        ),
        'laura@correo.com',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Teléfono móvil *'),
        '5599887766',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Calle (sin carácter "#") *'),
        'Av. Reforma',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Núm. Exterior *'),
        '250',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Colonia (sin "#") *'),
        'Juarez',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Municipio / Alcaldía *'),
        'Cuauhtemoc',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Estado *'),
        'CDMX',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Código Postal *'),
        '06600',
      );

      await tester.tap(find.text('Siguiente'));
      await tester.pumpAndSettle();

      // Paso 2: Apertura
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Ocupación / Profesión *'),
        'Diseñadora',
      );
      await tester.enterText(
        find.widgetWithText(
          TextFormField,
          'Empresa o centro de trabajo (sin "#") *',
        ),
        'Estudio NX',
      );

      await tester.tap(find.text('Siguiente'));
      await tester.pumpAndSettle();

      // Paso 3: Contraseña y envío OTP
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Contraseña segura *'),
        'Segura#2026',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Confirmar contraseña *'),
        'Segura#2026',
      );
      await tester.pumpAndSettle();
      expect(find.text('5/5'), findsOneWidget);

      await tester.tap(find.text('Enviar código al correo'));
      await tester.pumpAndSettle();

      // Paso 4: 6 casillas OTP + temporizador + confirmación final
      expect(sentOtpPayload?['correoElectronico'], 'laura@correo.com');
      expect(find.text('05:00'), findsOneWidget);
      expect(find.text('Reenviar código'), findsOneWidget);

      await tester.enterText(find.byKey(const ValueKey('otp_box_0')), '654321');
      await tester.pumpAndSettle();

      await tester.tap(find.text('Confirmar y abrir cuenta'));
      await tester.pumpAndSettle();

      expect(registeredPayload?['codigoOtp'], '654321');
      expect(find.text('Apertura de Cuenta Exitosa'), findsOneWidget);
      expect(find.text('4555 9999 8888 7777'), findsOneWidget);
      expect(find.text('Entrar a mi cuenta ahora'), findsOneWidget);
    });
  });
}
