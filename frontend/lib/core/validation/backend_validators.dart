import 'package:gestopago_app/core/network/strict_decimal_json.dart';

/// Validaciones centralizadas para formularios y reglas de negocio de GestoPagos.
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
  static final RegExp _otp6 = RegExp(r'^\d{6}$');
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
    if (v.isEmpty) return 'Ingresa ${campo.toLowerCase()}.';
    if (v.length < min || v.length > max) {
      return '$campo debe tener entre $min y $max caracteres.';
    }
    if (!_letrasEspacios.hasMatch(v)) {
      return '$campo solo admite letras y espacios.';
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
    if (v.isEmpty) return 'Selecciona tu fecha de nacimiento.';
    final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(v);
    if (match == null) {
      return 'Selecciona una fecha de nacimiento válida.';
    }
    final y = int.parse(match.group(1)!);
    final m = int.parse(match.group(2)!);
    final d = int.parse(match.group(3)!);
    final parsed = DateTime(y, m, d);
    if (parsed.year != y || parsed.month != m || parsed.day != d) {
      return 'La fecha seleccionada no es válida.';
    }
    final hoy = DateTime.now();
    if (!parsed.isBefore(DateTime(hoy.year, hoy.month, hoy.day))) {
      return 'La fecha de nacimiento no puede ser futura.';
    }
    var edad = hoy.year - parsed.year;
    if (hoy.month < parsed.month ||
        (hoy.month == parsed.month && hoy.day < parsed.day)) {
      edad--;
    }
    if (edad < 18) {
      return 'Debes ser mayor de edad (18 años o más) para abrir una cuenta.';
    }
    return null;
  }

  static String? curp(String? value) {
    final v = value?.trim().toUpperCase() ?? '';
    if (v.isEmpty) return 'Ingresa tu CURP de 18 caracteres.';
    if (!_curp.hasMatch(v)) {
      return 'Ingresa una CURP válida de 18 caracteres.';
    }
    return null;
  }

  static String? rfc(String? value) {
    final v = value?.trim().toUpperCase() ?? '';
    if (v.isEmpty) return 'Ingresa tu RFC con homoclave.';
    if (!_rfc.hasMatch(v)) {
      return 'Ingresa un RFC válido con homoclave (12 o 13 caracteres).';
    }
    return null;
  }

  static String? email(String? value, {bool requiredField = true}) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField ? 'Ingresa tu correo electrónico.' : null;
    }
    if (v.length > 100) {
      return 'El correo electrónico no debe exceder 100 caracteres.';
    }
    if (!_email.hasMatch(v)) {
      return 'Ingresa un correo electrónico válido (ejemplo: nombre@correo.com).';
    }
    return null;
  }

  static String? telefono(String? value, {bool requiredField = true}) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField
          ? 'Ingresa el número telefónico a 10 dígitos.'
          : null;
    }
    if (!_telefono10.hasMatch(v)) {
      return 'El número telefónico debe contener exactamente 10 dígitos.';
    }
    return null;
  }

  static String? calleOColonia(
    String? value,
    String campo, {
    required int max,
  }) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Ingresa ${campo.toLowerCase()}.';
    if (v.length < 2 || v.length > max) {
      return '$campo debe tener entre 2 y $max caracteres.';
    }
    if (v.contains('#') || !_calleOColonia.hasMatch(v)) {
      return '$campo contiene caracteres no válidos (evita usar "#").';
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
      return requiredField ? 'Ingresa ${campo.toLowerCase()}.' : null;
    }
    if (v.length > 20) return '$campo no debe exceder 20 caracteres.';
    if (v.contains('#') || !_numeroExteriorInterior.hasMatch(v)) {
      return '$campo solo admite letras, números y guiones (sin "#").';
    }
    return null;
  }

  static String? codigoPostal(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Ingresa el código postal.';
    if (!_codigoPostal.hasMatch(v)) {
      return 'El código postal debe tener 5 dígitos numéricos.';
    }
    return null;
  }

  static String? empresa(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Ingresa el nombre de la empresa o lugar de trabajo.';
    if (v.length < 2 || v.length > 100) {
      return 'Debe tener entre 2 y 100 caracteres.';
    }
    if (v.contains('#') || !_empresa.hasMatch(v)) {
      return 'Contiene caracteres no permitidos (evita usar "#").';
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
    if (v.isEmpty) return 'Ingresa ${campo.toLowerCase()}.';
    final parsed = StrictDecimal.tryParse(v);
    if (parsed == null) {
      return 'Ingresa un importe válido con máximo 2 decimales (ej. 150.00).';
    }
    if (parsed.asDouble < min) {
      return '$campo debe ser de al menos \$${StrictDecimal.fromDouble(min).toStrictLiteral()} MXN.';
    }
    if (max != null && parsed.asDouble > max) {
      return '$campo no puede superar \$${StrictDecimal.fromDouble(max).toStrictLiteral()} MXN.';
    }
    return null;
  }

  static String? passwordLogin(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Ingresa tu contraseña para continuar.';
    return null;
  }

  static String? passwordSegura(String? value) {
    final v = value ?? '';
    if (v.isEmpty) return 'Crea una contraseña segura.';
    if (v.length < 8 || v.length > 64) {
      return 'La contraseña debe tener entre 8 y 64 caracteres.';
    }
    if (!_passwordSegura.hasMatch(v)) {
      return 'Incluye al menos una mayúscula, minúscula, número y carácter especial.';
    }
    return null;
  }

  static String? confirmacionPassword(String? password, String? confirm) {
    if (confirm == null || confirm.isEmpty) return 'Confirma tu contraseña.';
    if ((password ?? '') != confirm) return 'Las contraseñas no coinciden.';
    return null;
  }

  static String? codigoOtp(String? value) {
    final clean = (value ?? '').replaceAll(RegExp(r'\D'), '');
    if (!_otp6.hasMatch(clean)) {
      return 'Ingresa los 6 dígitos del código de seguridad.';
    }
    return null;
  }

  static String? numeroCuenta(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Ingresa el número de cuenta de 16 dígitos.';
    if (!_cuenta16.hasMatch(v)) {
      return 'El número de cuenta debe tener exactamente 16 dígitos numéricos.';
    }
    return null;
  }

  static String? referenciaPago(String? value, {required bool requiredField}) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) {
      return requiredField
          ? 'Ingresa la referencia o línea de captura del servicio.'
          : null;
    }
    if (v.length > 100) return 'La referencia no debe superar 100 caracteres.';
    if (!_referenciaAlfanumerica.hasMatch(v)) {
      return 'La referencia solo admite letras y números, sin espacios ni símbolos.';
    }
    return null;
  }

  static String? upc(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Ingresa el folio UPC a consultar.';
    if (!_upc.hasMatch(v)) {
      return 'El folio UPC debe contener exactamente 10 caracteres alfanuméricos.';
    }
    return null;
  }

  static String friendlyFieldName(String rawKey) {
    final normalized = rawKey.replaceAll('registro.', '').trim();
    const map = <String, String>{
      'nombre': 'Nombre',
      'segundoNombre': 'Segundo nombre',
      'apellidoPaterno': 'Apellido paterno',
      'apellidoMaterno': 'Apellido materno',
      'fechaNacimiento': 'Fecha de nacimiento',
      'curp': 'CURP',
      'rfc': 'RFC',
      'sexo': 'Sexo',
      'estadoCivil': 'Estado civil',
      'idNacionalidad': 'Nacionalidad',
      'nacionalidad': 'Nacionalidad',
      'correoElectronico': 'Correo electrónico',
      'telefonoMovil': 'Teléfono móvil',
      'telefonoAlternativo': 'Teléfono alternativo',
      'ocupacion': 'Ocupación',
      'empresa': 'Empresa',
      'ingresoMensual': 'Ingreso mensual',
      'saldoInicial': 'Saldo inicial',
      'password': 'Contraseña',
      'codigoOtp': 'Código de verificación',
      'domicilio.calle': 'Calle',
      'domicilio.numeroExterior': 'Número exterior',
      'domicilio.numeroInterior': 'Número interior',
      'domicilio.colonia': 'Colonia',
      'domicilio.municipio': 'Municipio / Alcaldía',
      'domicilio.estado': 'Estado',
      'domicilio.codigoPostal': 'Código postal',
      'domicilio.pais': 'País',
      'numCuenta': 'Número de cuenta',
      'referencia': 'Referencia',
      'telefono': 'Teléfono',
      'monto': 'Importe',
    };
    return map[normalized] ?? normalized;
  }
}
