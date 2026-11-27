import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';

void main() {
  group('StrictDecimal y encodeStrictJson', () {
    test('normaliza enteros, 1 decimal y comas a exactamente 2 decimales', () {
      expect(StrictDecimal.parse('200').toStrictLiteral(), '200.00');
      expect(StrictDecimal.parse('200.5').toStrictLiteral(), '200.50');
      expect(StrictDecimal.parse('200.00').toStrictLiteral(), '200.00');
      expect(StrictDecimal.parse('1,250.5').toStrictLiteral(), '1250.50');
      expect(StrictDecimal.parse('0').toStrictLiteral(), '0.00');
      expect(StrictDecimal.fromDouble(150.5).toStrictLiteral(), '150.50');
      expect(StrictDecimal.fromCents(99).toStrictLiteral(), '0.99');
    });

    test('rechaza formatos invalidos o con mas de 2 decimales', () {
      expect(StrictDecimal.tryParse(''), isNull);
      expect(StrictDecimal.tryParse('200.123'), isNull);
      expect(StrictDecimal.tryParse('-10.00'), isNull);
      expect(StrictDecimal.tryParse('abc'), isNull);
      expect(() => StrictDecimal.fromDouble(-1), throwsFormatException);
      expect(() => StrictDecimal.fromCents(-1), throwsFormatException);
    });

    test('serializa PagoServicioRequestDto con monto numerico sin comillas .00', () {
      final dto = PagoServicioRequestDto(
        numCuenta: '4555000011112222',
        idServicio: 25,
        idProducto: 101,
        monto: StrictDecimal.parse('200'),
        telefono: '5512345678',
        codigo2fa: 'tok-abc',
        correoElectronico: 'cliente@correo.com',
      );

      final rawJson = encodeStrictJson(dto.toStrictJsonMap());

      expect(rawJson, contains('"monto":200.00'));
      expect(rawJson, isNot(contains('"monto":"200.00"')));
      expect(rawJson, isNot(contains('"monto":200,')));

      final decoded = jsonDecode(rawJson) as Map<String, dynamic>;
      expect(decoded['numCuenta'], '4555000011112222');
      expect(decoded['2fa'], 'tok-abc');
      expect(decoded['monto'], 200.0);
    });

    test('serializa ClienteRegistroRequestDto con ingresoMensual y saldoInicial estrictos', () {
      final dto = ClienteRegistroRequestDto(
        nombre: 'Ana',
        apellidoPaterno: 'Lopez',
        apellidoMaterno: 'Perez',
        fechaNacimiento: '1995-04-12',
        curp: 'LOPA950412MDFRRL09',
        rfc: 'LOPA950412AB1',
        sexo: 'Femenino',
        idNacionalidad: 1,
        estadoCivil: 'Soltero',
        correoElectronico: 'Ana.Lopez@Correo.COM',
        telefonoMovil: '5511223344',
        domicilio: const DomicilioDto(
          calle: 'Av Reforma',
          numeroExterior: '100',
          colonia: 'Centro',
          municipio: 'Cuauhtemoc',
          estado: 'CDMX',
          codigoPostal: '06000',
          pais: 'Mexico',
        ),
        ocupacion: 'Ingeniera',
        empresa: 'Tech MX S.A.',
        ingresoMensual: StrictDecimal.parse('35000'),
        saldoInicial: StrictDecimal.parse('1500.5'),
        password: 'Password#1',
      );

      final rawJson = encodeStrictJson(dto.toStrictJsonMap());

      expect(rawJson, contains('"ingresoMensual":35000.00'));
      expect(rawJson, contains('"saldoInicial":1500.50'));
      expect(rawJson, contains('"correoElectronico":"ana.lopez@correo.com"'));
      expect(rawJson, isNot(contains('numeroInterior')));
    });
  });
}
