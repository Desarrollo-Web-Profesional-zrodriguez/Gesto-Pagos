import 'package:gestopago_app/core/network/strict_decimal_json.dart';

/// Validaciones idénticas a las anotaciones Jakarta Bean Validation y reglas de
/// negocio del backend Spring Boot.
class BackendValidators {
  BackendValidators._();

  static final RegExp _letrasEspacios = RegExp(
    r'^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$',
  );
  static final RegExp _calleOColonia = RegExp(
    r'^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s.,-]+$',
  );
  static final RegExp _numeroExteriorInterior = RegExp(r'^[a-zA-Z0-9\s-]+$');
  static final RegExp _empresa = RegExp(r'^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s.,&-]+$');
  static final RegExp _curp = RegExp(
    r'^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$',
  );
  static final RegExp _rfc = RegExp(
    r'^[A-ZÑ&]{3,4}\d{6}[A-V1-9][A-Z1-9][0-9A]$',
  );
  static final RegExp _email = RegExp(
    r"^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$",
  );
  static final RegExp _telefono10 = RegExp(r'^\d{10}$');
  static final RegExp _codigoPostal = RegExp(r'^\d{5}$');
  static final RegExp _cuenta16 = RegExp(r'^\d{16}$');
  static final RegExp _referenciaAlfanumerica = RegExp(r'^[a-zA-Z0-9]*$');
  static final RegExp _upc = RegExp(r'^[A-Za-z0-9]{10}$');
  static final RegExp _passwordSegura = RegExp(
    r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9\s]).{8,64}$',
  );

  static String? soloLetrasRequerido(
    String? value,
    String campo, {
    int min = 3,
    int max = 50,
  }) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'El campo $campo es obligatorio';
    if (v.length < min || v.length > max) {
      return '$campo debe tener entre $min y $max caracteres';
    }
    if (!_letrasEspacios.hasMatch(v)) {
      return '$campo solo debe contener letras y espacios (sin números ni #)';
    }
    return null;
  }

  static String? soloLetrasOpcional(
    String? value,
    String campo, {
    int min = 3,
    int max = 50,
  }) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return null;
    return soloLetrasRequerido(v, campo, min: min, max: max);
  }

  static String? fechaNacimientoMayorEdad(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'La fecha de nacimiento es obligatoria';
    if (!RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(v)) {
      return 'Usa formato estricto AAAA-MM-DD (ej. 1992-05-20)';
    }
    final parsed = DateTime.tryParse(v);
    if (parsed == null) return 'Fecha inválida';
    final hoy = DateTime.now();
    if (!parsed.isBefore(DateTime(hoy.year, hoy.month, hoy.day))) {
      return 'La fecha de nacimiento no puede ser futura';
    }
    var edad = hoy.year - parsed.year;
    if (hoy.month < parsed.month ||
        (hoy.month == parsed.month && hoy.day < parsed.day)) {
      edad--;
    }
    if (edad < 18) {
      return 'El cliente debe ser mayor de edad (18 años o más). Edad: $edad';
    }
    return null;
  }

  static String? curp(String? value) {
    final v = value?.trim().toUpperCase() ?? '';
    if (v.isEmpty) return 'La CURP es obligatoria';
    if (!_curp.hasMatch(v)) {
      return 'CURP inválida (debe tener exactamente 18 caracteres oficiales)';
    }
    return null;
  }

  static String? rfc(String? value) {
    final v = value?.trim().toUpperCase() ?? '';
    if (v.isEmpty) return 'El RFC es obligatorio';
    if (!_rfc.hasMatch(v)) {
      return 'RFC inválido con homoclave (12 o 13 caracteres)';
    }
    return null;
  }

  static String? email(String? value, {bool requiredField = true}) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField ? 'El correo electrónico es obligatorio' : null;
    }
    if (v.length > 100) return 'Máximo 100 caracteres';
    if (!_email.hasMatch(v)) {
      return 'Formato inválido (ejemplo: usuario@dominio.com)';
    }
    return null;
  }

  static String? telefono(String? value, {bool requiredField = true}) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField ? 'El teléfono de 10 dígitos es obligatorio' : null;
    }
    if (!_telefono10.hasMatch(v)) {
      return 'Debe contener exactamente 10 dígitos numéricos';
    }
    return null;
  }

  static String? calleOColonia(
    String? value,
    String campo, {
    required int max,
  }) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return '$campo es obligatorio';
    if (v.length < 2 || v.length > max) {
      return '$campo debe tener entre 2 y $max caracteres';
    }
    if (v.contains('#') || !_calleOColonia.hasMatch(v)) {
      return '$campo contiene caracteres no permitidos (sin "#")';
    }
    return null;
  }

  static String? numeroDomicilio(
    String? value,
    String campo, {
    required bool requiredField,
  }) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField ? '$campo es obligatorio' : null;
    }
    if (v.length > 20) return '$campo no puede superar 20 caracteres';
    if (v.contains('#') || !_numeroExteriorInterior.hasMatch(v)) {
      return '$campo solo acepta letras, números y guiones (sin "#")';
    }
    return null;
  }

  static String? codigoPostal(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'El código postal es obligatorio';
    if (!_codigoPostal.hasMatch(v)) {
      return 'Debe tener exactamente 5 dígitos numéricos';
    }
    return null;
  }

  static String? empresa(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'La empresa es obligatoria';
    if (v.length < 2 || v.length > 100) {
      return 'Debe tener entre 2 y 100 caracteres';
    }
    if (v.contains('#') || !_empresa.hasMatch(v)) {
      return 'Contiene caracteres no permitidos (sin "#")';
    }
    return null;
  }

  static String? montoDecimal(
    String? value,
    String campo, {
    double min = 0.01,
    double? max,
  }) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'El campo $campo es obligatorio';
    final parsed = StrictDecimal.tryParse(v);
    if (parsed == null) {
      return 'Ingresa un número válido con máximo 2 decimales (ej. 100.00)';
    }
    if (parsed.asDouble < min) {
      return '$campo debe ser al menos ${StrictDecimal.fromDouble(min).toStrictLiteral()}';
    }
    if (max != null && parsed.asDouble > max) {
      return '$campo no puede superar ${StrictDecimal.fromDouble(max).toStrictLiteral()}';
    }
    return null;
  }

  static String? passwordSegura(String? value) {
    final v = value ?? '';
    if (v.isEmpty) return 'La contraseña es obligatoria';
    if (v.length < 8 || v.length > 64) {
      return 'Debe tener entre 8 y 64 caracteres';
    }
    if (!_passwordSegura.hasMatch(v)) {
      return 'Incluye mayúscula, minúscula, número y carácter especial';
    }
    return null;
  }

  static String? numeroCuenta(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'El número de cuenta es obligatorio';
    if (!_cuenta16.hasMatch(v)) {
      return 'El número de cuenta debe tener exactamente 16 dígitos';
    }
    return null;
  }

  static String? referenciaPago(String? value, {required bool requiredField}) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField
          ? 'La referencia del servicio es obligatoria'
          : null;
    }
    if (v.length > 100) return 'Máximo 100 caracteres';
    if (!_referenciaAlfanumerica.hasMatch(v)) {
      return 'Solo caracteres alfanuméricos sin espacios ni símbolos';
    }
    return null;
  }

  static String? upc(String? value) {
    final v = value?.trim() ?? '';
    if (!_upc.hasMatch(v)) {
      return 'El UPC debe tener 10 caracteres alfanuméricos';
    }
    return null;
  }
}
