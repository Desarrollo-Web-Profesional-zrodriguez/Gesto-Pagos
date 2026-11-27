import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/features/auth/data/auth_repository.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';

class _FakeHttpClientAdapter implements HttpClientAdapter {
  _FakeHttpClientAdapter(this.onFetch);

  final Future<ResponseBody> Function(RequestOptions options) onFetch;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    return onFetch(options);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody _jsonResponse(int statusCode, Object? body) =>
    ResponseBody.fromString(
      jsonEncode(body),
      statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );

void main() {
  group('ApiClient y repositorios HTTP', () {
    test('PagoRepository envia StrictDecimal sin comillas, Idempotency-Key y Bearer token en 200', () async {
      late RequestOptions captured;
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeHttpClientAdapter((options) async {
        captured = options;
        return _jsonResponse(200, {
          'codigo': '00',
          'mensaje': 'Transaccion aprobada',
          'upc': 'UPC1234567',
          'numCuenta': '4555000011112222',
          'idServicio': 10,
          'servicio': 'Telcel',
          'idProducto': 200,
          'producto': 'Recarga 200',
          'montoPagado': 200.0,
          'saldoDisponibleActual': 800.0,
        });
      });

      var activityCount = 0;
      final client = ApiClient(
        dio: dio,
        tokenProvider: () => 'sesion-tok-01',
        onUserNetworkActivity: () => activityCount++,
      );
      final repo = PagoRepository(client);

      final result = await repo.realizarPago(
        PagoServicioRequestDto(
          numCuenta: '4555000011112222',
          idServicio: 10,
          idProducto: 200,
          monto: StrictDecimal.parse('200'),
          telefono: '5512345678',
          codigo2fa: 'sesion-tok-01',
        ),
        idempotencyKey: 'idem-uuid-1',
      );

      expect(captured.path, '/pagos');
      expect(captured.headers['Idempotency-Key'], 'idem-uuid-1');
      expect(captured.headers['Authorization'], 'Bearer sesion-tok-01');
      expect(captured.receiveTimeout, const Duration(seconds: 75));
      expect(captured.data.toString(), contains('"monto":200.00'));
      expect(activityCount, 1);

      expect(result, isA<PagoExitosoResult>());
      final ok = result as PagoExitosoResult;
      expect(ok.comprobante.upc, 'UPC1234567');
      expect(ok.comprobante.saldoDisponibleActual, 800.0);
    });

    test('PagoRepository interpreta HTTP 202 y extrae UPC de conciliacion', () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeHttpClientAdapter((_) async {
        return _jsonResponse(202, {
          'codigo': 504,
          'codigoCatalogo': 'PAGO_PENDIENTE_CONCILIACION',
          'mensaje':
              'El proveedor tardo en responder. Código de seguimiento (UPC): ZX90123456. Reintenta consulta.',
        });
      });

      final repo = PagoRepository(ApiClient(dio: dio));
      final result = await repo.realizarPago(
        PagoServicioRequestDto(
          numCuenta: '4555000011112222',
          idServicio: 25,
          idProducto: 500,
          monto: StrictDecimal.parse('150.5'),
          referencia: '99887766',
          codigo2fa: 'tok',
        ),
        idempotencyKey: 'idem-uuid-2',
      );

      expect(result, isA<PagoPendienteConciliacionResult>());
      final pending = result as PagoPendienteConciliacionResult;
      expect(pending.upc, 'ZX90123456');
    });

    test('ApiClient propaga SESION_EXPIRADA en 401 y detallesValidacion en 400', () async {
      var expiredTriggered = false;
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeHttpClientAdapter((options) async {
        if (options.path == '/clientes/consultar') {
          return _jsonResponse(401, {
            'codigo': 402,
            'codigoCatalogo': 'SESION_EXPIRADA',
            'mensaje': 'Tu sesión ha expirado por inactividad.',
          });
        }
        return _jsonResponse(400, {
          'codigo': 400,
          'mensaje': 'Error de validación',
          'detallesValidacion': {'curp': 'CURP inválida'},
        });
      });

      final client = ApiClient(
        dio: dio,
        onUnauthorizedExpired: () => expiredTriggered = true,
      );
      final clienteRepo = ClienteRepository(client);

      await expectLater(
        clienteRepo.consultarPorIdentificador('user@correo.com'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.esSesionExpirada, 'esSesionExpirada', isTrue),
        ),
      );
      expect(expiredTriggered, isTrue);

      await expectLater(
        clienteRepo.buscarClientes(),
        throwsA(
          isA<ApiException>()
              .having((e) => e.detallesValidacion['curp'], 'curp', 'CURP inválida'),
        ),
      );
    });

    test('ProductoRepository y AuthRepository parsean categorias y sesion', () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeHttpClientAdapter((options) async {
        if (options.path == '/productos') {
          return _jsonResponse(200, {
            'status': 200,
            'message': 'OK',
            'origen': 'REDIS',
            'totalProductos': 3,
            'categorias': {
              '6': [
                {
                  'idProducto': 601,
                  'idServicio': 60,
                  'servicio': 'CFE',
                  'producto': 'Recibo CFE',
                  'tipoFront': 2,
                },
              ],
              '1': [
                {
                  'idProducto': 101,
                  'idServicio': 10,
                  'servicio': 'Telcel',
                  'producto': 'Recarga 100',
                  'precio': 100.0,
                  'idCatTipoServicio': 1,
                  'tipoFront': 1,
                },
              ],
              '10': [
                {
                  'idProducto': 901,
                  'idServicio': 90,
                  'servicio': 'Netflix',
                  'producto': 'Pin 300',
                  'precio': 300.0,
                  'idCatTipoServicio': 10,
                  'tipoFront': 1,
                },
              ],
            },
          });
        }
        return _jsonResponse(200, {
          'tokenSesion': 'bearer-abc',
          'activa': true,
          'tipoToken': 'Bearer',
          'minutosInactividadMaximos': 5,
          'username': 'ana@correo.com',
          'nombreCliente': 'Ana Lopez',
        });
      });

      final client = ApiClient(dio: dio);
      final prodRepo = ProductoRepository(client);
      final authRepo = AuthRepository(client);

      final cat = await prodRepo.obtenerProductos();
      expect(cat.origen, 'REDIS');
      expect(cat.categorias.keys.toList(), [1, 6, 10]);
      expect(cat.categorias[1]!.first.esRecargaTelefonica, isTrue);
      expect(cat.categorias[10]!.first.esPinOEntretenimiento, isTrue);
      expect(cat.categorias[6]!.first.esPagoServicio, isTrue);
      expect(cat.categorias[6]!.first.esPrecioFijo, isFalse);

      final login = await authRepo.login(
        username: 'ANA@CORREO.COM ',
        password: 'Password#1',
      );
      expect(login.tokenSesion, 'bearer-abc');
      expect(login.minutosInactividadMaximos, 5);
    });

    test('PagoRepository valida referencias y deserializa historial enriquecido', () async {
      late RequestOptions capturedVal;
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
      dio.httpClientAdapter = _FakeHttpClientAdapter((options) async {
        if (options.path == '/pagos/validar-referencia') {
          capturedVal = options;
          return _jsonResponse(200, {
            'valida': true,
            'mensaje': 'Referencia validada correctamente para CFE',
            'idServicio': 76,
            'servicio': 'CFE',
            'idProducto': 205,
            'producto': 'Pago CFE',
            'tipoFront': 2,
            'referenciaNormalizada': '25720381',
            'montoValidado': 150.0,
            'comisionEstimada': 10.0,
            'saldoSuficiente': true,
          });
        }
        return _jsonResponse(200, [
          {
            'upc': 'ABCDE12345',
            'numeroCuenta': '4555000011112222',
            'idServicio': 76,
            'idProducto': 205,
            'telefono': '1111111111',
            'referencia': '25720381',
            'monto': 150.0,
            'estatus': 'EXITOSA',
            'servicio': 'CFE',
            'producto': 'Pago CFE',
            'urlLogo': 'https://example.com/cfe.png',
          },
        ]);
      });

      final repo = PagoRepository(ApiClient(dio: dio));
      final valRes = await repo.validarReferencia(
        ValidarReferenciaRequestDto(
          numCuenta: '4555000011112222',
          idServicio: 76,
          idProducto: 205,
          referencia: '25720381',
          monto: StrictDecimal.parse('150'),
        ),
      );

      expect(capturedVal.data.toString(), contains('"monto":150.00'));
      expect(valRes.valida, isTrue);
      expect(valRes.servicio, 'CFE');
      expect(valRes.saldoSuficiente, isTrue);

      final hist = await repo.consultarHistorial('4555000011112222');
      expect(hist.length, 1);
      expect(hist.first.servicio, 'CFE');
      expect(hist.first.producto, 'Pago CFE');
      expect(hist.first.urlLogo, 'https://example.com/cfe.png');
    });
  });
}
