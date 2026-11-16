import 'package:flutter_test/flutter_test.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';

void main() {
  group('BackendValidators alineados al backend Spring Boot', () {
    test('valida nombres solo con letras y acentos sin numeros ni #', () {
      expect(BackendValidators.soloLetrasRequerido('María José', 'Nombre'), isNull);
      expect(BackendValidators.soloLetrasRequerido('Al', 'Nombre'), isNotNull);
      expect(BackendValidators.soloLetrasRequerido('Juan 2', 'Nombre'), isNotNull);
      expect(BackendValidators.soloLetrasRequerido('Ana #1', 'Nombre'), isNotNull);
    });

    test('valida CURP y RFC oficiales', () {
      expect(BackendValidators.curp('LOPA950412MDFRRL09'), isNull);
      expect(BackendValidators.curp('CURP_MALA'), isNotNull);

      expect(BackendValidators.rfc('LOPA950412AB1'), isNull);
      expect(BackendValidators.rfc('ABC950412AB1'), isNull);
      expect(BackendValidators.rfc('12345'), isNotNull);
    });

    test('valida fecha de nacimiento AAAA-MM-DD y mayoria de edad (18+)', () {
      expect(BackendValidators.fechaNacimientoMayorEdad('1992-05-20'), isNull);
      expect(BackendValidators.fechaNacimientoMayorEdad('20/05/1992'), isNotNull);

      final now = DateTime.now();
      final menorYear = (now.year - 15).toString().padLeft(4, '0');
      expect(
        BackendValidators.fechaNacimientoMayorEdad('$menorYear-01-01'),
        contains('mayor de edad'),
      );
    });

    test('rechaza numeral (#) en calle, numeroExterior y empresa', () {
      expect(
        BackendValidators.calleOColonia('Av. Reforma Norte 10', 'Calle', max: 150),
        isNull,
      );
      expect(
        BackendValidators.calleOColonia('Calle #10', 'Calle', max: 150),
        isNotNull,
      );
      expect(
        BackendValidators.numeroDomicilio('120-B', 'Número exterior', requiredField: true),
        isNull,
      );
      expect(
        BackendValidators.numeroDomicilio('#120', 'Número exterior', requiredField: true),
        isNotNull,
      );
      expect(BackendValidators.empresa('Grupo Alfa, S.A.'), isNull);
      expect(BackendValidators.empresa('Empresa #1'), isNotNull);
    });

    test('valida CP 5 digitos, telefono 10 digitos, cuenta 16 digitos, UPC y password', () {
      expect(BackendValidators.codigoPostal('06000'), isNull);
      expect(BackendValidators.codigoPostal('0600'), isNotNull);

      expect(BackendValidators.telefono('5512345678'), isNull);
      expect(BackendValidators.telefono('551234'), isNotNull);

      expect(BackendValidators.numeroCuenta('4555000011112222'), isNull);
      expect(BackendValidators.numeroCuenta('12345'), isNotNull);

      expect(BackendValidators.upc('ABC1234567'), isNull);
      expect(BackendValidators.upc('ABC-123'), isNotNull);

      expect(BackendValidators.referenciaPago('REF998877', requiredField: true), isNull);
      expect(BackendValidators.referenciaPago('REF 998', requiredField: true), isNotNull);

      expect(BackendValidators.passwordSegura('ClaveFuerte#1'), isNull);
      expect(BackendValidators.passwordSegura('debil123'), isNotNull);
    });
  });
}
