import 'package:gestopago_app/core/network/strict_decimal_json.dart';

class SaldoDto {
  const SaldoDto({
    this.idSaldo,
    required this.numeroCuenta,
    required this.saldoDisponible,
    required this.saldoContable,
    this.fechaUltimaActualizacion,
  });

  final int? idSaldo;
  final String numeroCuenta;
  final double saldoDisponible;
  final double saldoContable;
  final String? fechaUltimaActualizacion;

  double get montoRetenido {
    final diff = saldoContable - saldoDisponible;
    return diff > 0.001 ? diff : 0.0;
  }

  factory SaldoDto.fromJson(Map<String, dynamic> json) => SaldoDto(
        idSaldo: (json['idSaldo'] as num?)?.toInt(),
        numeroCuenta: json['numeroCuenta']?.toString() ?? '',
        saldoDisponible: (json['saldoDisponible'] as num?)?.toDouble() ?? 0.0,
        saldoContable: (json['saldoContable'] as num?)?.toDouble() ?? 0.0,
        fechaUltimaActualizacion:
            json['fechaUltimaActualizacion']?.toString(),
      );
}

class CuentaDto {
  const CuentaDto({
    this.idCuenta,
    this.nombreTitular,
    required this.numeroCuenta,
    required this.tipoCuenta,
    required this.estatus,
    this.fechaApertura,
    this.saldo,
  });

  final int? idCuenta;
  final String? nombreTitular;
  final String numeroCuenta;
  final String tipoCuenta;
  final String estatus;
  final String? fechaApertura;
  final SaldoDto? saldo;

  bool get esActiva => estatus.toUpperCase() == 'ACTIVA';

  factory CuentaDto.fromJson(Map<String, dynamic> json) => CuentaDto(
        idCuenta: (json['idCuenta'] as num?)?.toInt(),
        nombreTitular: json['nombreTitular']?.toString(),
        numeroCuenta: json['numeroCuenta']?.toString() ?? '',
        tipoCuenta: json['tipoCuenta']?.toString() ?? 'DEBITO',
        estatus: json['estatus']?.toString() ?? '',
        fechaApertura: json['fechaApertura']?.toString(),
        saldo: json['saldo'] is Map<String, dynamic>
            ? SaldoDto.fromJson(json['saldo'] as Map<String, dynamic>)
            : null,
      );
}

class DomicilioDto {
  const DomicilioDto({
    required this.calle,
    required this.numeroExterior,
    this.numeroInterior,
    required this.colonia,
    required this.municipio,
    required this.estado,
    required this.codigoPostal,
    required this.pais,
  });

  final String calle;
  final String numeroExterior;
  final String? numeroInterior;
  final String colonia;
  final String municipio;
  final String estado;
  final String codigoPostal;
  final String pais;

  factory DomicilioDto.fromJson(Map<String, dynamic> json) => DomicilioDto(
        calle: json['calle']?.toString() ?? '',
        numeroExterior: json['numeroExterior']?.toString() ?? '',
        numeroInterior: json['numeroInterior']?.toString(),
        colonia: json['colonia']?.toString() ?? '',
        municipio: json['municipio']?.toString() ?? '',
        estado: json['estado']?.toString() ?? '',
        codigoPostal: json['codigoPostal']?.toString() ?? '',
        pais: json['pais']?.toString() ?? '',
      );

  Map<String, dynamic> toJson({bool includeEmptyOptional = false}) {
    final map = <String, dynamic>{
      'calle': calle.trim(),
      'numeroExterior': numeroExterior.trim(),
      'colonia': colonia.trim(),
      'municipio': municipio.trim(),
      'estado': estado.trim(),
      'codigoPostal': codigoPostal.trim(),
      'pais': pais.trim(),
    };
    final interior = numeroInterior?.trim();
    if (interior != null && interior.isNotEmpty) {
      map['numeroInterior'] = interior;
    } else if (includeEmptyOptional) {
      map['numeroInterior'] = '';
    }
    return map;
  }
}

class ClienteDto {
  const ClienteDto({
    required this.nombre,
    this.segundoNombre,
    required this.apellidoPaterno,
    required this.apellidoMaterno,
    required this.nombreCompleto,
    required this.fechaNacimiento,
    this.edad,
    required this.curp,
    required this.rfc,
    required this.sexo,
    this.idNacionalidad,
    this.nacionalidad,
    required this.estadoCivil,
    required this.correoElectronico,
    required this.telefonoMovil,
    this.telefonoAlternativo,
    required this.ocupacion,
    required this.empresa,
    required this.ingresoMensual,
    required this.activo,
    this.biometriaFacialActiva = false,
    this.fechaCreacion,
    this.fechaModificacion,
    this.domicilios = const [],
    this.cuentas = const [],
  });

  final String nombre;
  final String? segundoNombre;
  final String apellidoPaterno;
  final String apellidoMaterno;
  final String nombreCompleto;
  final String fechaNacimiento;
  final int? edad;
  final String curp;
  final String rfc;
  final String sexo;
  final int? idNacionalidad;
  final String? nacionalidad;
  final String estadoCivil;
  final String correoElectronico;
  final String telefonoMovil;
  final String? telefonoAlternativo;
  final String ocupacion;
  final String empresa;
  final double ingresoMensual;
  final bool activo;
  final bool biometriaFacialActiva;
  final String? fechaCreacion;
  final String? fechaModificacion;
  final List<DomicilioDto> domicilios;
  final List<CuentaDto> cuentas;

  CuentaDto? get cuentaPrincipal =>
      cuentas.isNotEmpty ? cuentas.first : null;

  ClienteDto copyWith({bool? biometriaFacialActiva}) => ClienteDto(
        nombre: nombre,
        segundoNombre: segundoNombre,
        apellidoPaterno: apellidoPaterno,
        apellidoMaterno: apellidoMaterno,
        nombreCompleto: nombreCompleto,
        fechaNacimiento: fechaNacimiento,
        edad: edad,
        curp: curp,
        rfc: rfc,
        sexo: sexo,
        idNacionalidad: idNacionalidad,
        nacionalidad: nacionalidad,
        estadoCivil: estadoCivil,
        correoElectronico: correoElectronico,
        telefonoMovil: telefonoMovil,
        telefonoAlternativo: telefonoAlternativo,
        ocupacion: ocupacion,
        empresa: empresa,
        ingresoMensual: ingresoMensual,
        activo: activo,
        biometriaFacialActiva:
            biometriaFacialActiva ?? this.biometriaFacialActiva,
        fechaCreacion: fechaCreacion,
        fechaModificacion: fechaModificacion,
        domicilios: domicilios,
        cuentas: cuentas,
      );

  factory ClienteDto.fromJson(Map<String, dynamic> json) => ClienteDto(
        nombre: json['nombre']?.toString() ?? '',
        segundoNombre: json['segundoNombre']?.toString(),
        apellidoPaterno: json['apellidoPaterno']?.toString() ?? '',
        apellidoMaterno: json['apellidoMaterno']?.toString() ?? '',
        nombreCompleto: json['nombreCompleto']?.toString() ?? '',
        fechaNacimiento: json['fechaNacimiento']?.toString() ?? '',
        edad: (json['edad'] as num?)?.toInt(),
        curp: json['curp']?.toString() ?? '',
        rfc: json['rfc']?.toString() ?? '',
        sexo: json['sexo']?.toString() ?? '',
        idNacionalidad: (json['idNacionalidad'] as num?)?.toInt(),
        nacionalidad: json['nacionalidad']?.toString(),
        estadoCivil: json['estadoCivil']?.toString() ?? '',
        correoElectronico: json['correoElectronico']?.toString() ?? '',
        telefonoMovil: json['telefonoMovil']?.toString() ?? '',
        telefonoAlternativo: json['telefonoAlternativo']?.toString(),
        ocupacion: json['ocupacion']?.toString() ?? '',
        empresa: json['empresa']?.toString() ?? '',
        ingresoMensual: (json['ingresoMensual'] as num?)?.toDouble() ?? 0.0,
        activo: json['activo'] == true,
        biometriaFacialActiva: json['biometriaFacialActiva'] == true,
        fechaCreacion: json['fechaCreacion']?.toString(),
        fechaModificacion: json['fechaModificacion']?.toString(),
        domicilios: (json['domicilios'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(DomicilioDto.fromJson)
            .toList(),
        cuentas: (json['cuentas'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(CuentaDto.fromJson)
            .toList(),
      );
}

class ClienteRegistroRequestDto {
  const ClienteRegistroRequestDto({
    this.identificador,
    required this.nombre,
    this.segundoNombre,
    required this.apellidoPaterno,
    required this.apellidoMaterno,
    required this.fechaNacimiento,
    required this.curp,
    required this.rfc,
    required this.sexo,
    this.idNacionalidad,
    this.nacionalidad,
    required this.estadoCivil,
    required this.correoElectronico,
    required this.telefonoMovil,
    this.telefonoAlternativo,
    required this.domicilio,
    required this.ocupacion,
    required this.empresa,
    required this.ingresoMensual,
    required this.saldoInicial,
    required this.password,
    this.biometricoFacialEmbedding,
  });

  final String? identificador;
  final String nombre;
  final String? segundoNombre;
  final String apellidoPaterno;
  final String apellidoMaterno;
  final String fechaNacimiento;
  final String curp;
  final String rfc;
  final String sexo;
  final int? idNacionalidad;
  final String? nacionalidad;
  final String estadoCivil;
  final String correoElectronico;
  final String telefonoMovil;
  final String? telefonoAlternativo;
  final DomicilioDto domicilio;
  final String ocupacion;
  final String empresa;
  final StrictDecimal ingresoMensual;
  final StrictDecimal saldoInicial;
  final String password;
  final String? biometricoFacialEmbedding;

  Map<String, dynamic> toStrictJsonMap() {
    final map = <String, dynamic>{
      if (identificador != null && identificador!.trim().isNotEmpty)
        'identificador': identificador!.trim(),
      'nombre': nombre.trim(),
      if (segundoNombre != null && segundoNombre!.trim().isNotEmpty)
        'segundoNombre': segundoNombre!.trim(),
      'apellidoPaterno': apellidoPaterno.trim(),
      'apellidoMaterno': apellidoMaterno.trim(),
      'fechaNacimiento': fechaNacimiento.trim(),
      'curp': curp.trim().toUpperCase(),
      'rfc': rfc.trim().toUpperCase(),
      'sexo': sexo.trim(),
      if (idNacionalidad != null) 'idNacionalidad': idNacionalidad,
      if (nacionalidad != null && nacionalidad!.trim().isNotEmpty)
        'nacionalidad': nacionalidad!.trim(),
      'estadoCivil': estadoCivil.trim(),
      'correoElectronico': correoElectronico.trim().toLowerCase(),
      'telefonoMovil': telefonoMovil.trim(),
      if (telefonoAlternativo != null &&
          telefonoAlternativo!.trim().isNotEmpty)
        'telefonoAlternativo': telefonoAlternativo!.trim(),
      'domicilio': domicilio.toJson(),
      'ocupacion': ocupacion.trim(),
      'empresa': empresa.trim(),
      'ingresoMensual': ingresoMensual,
      'saldoInicial': saldoInicial,
      'password': password,
      if (biometricoFacialEmbedding != null &&
          biometricoFacialEmbedding!.trim().isNotEmpty)
        'biometricoFacialEmbedding': biometricoFacialEmbedding!.trim(),
    };
    return map;
  }
}

class ClientePatchRequestDto {
  const ClientePatchRequestDto({
    required this.identificador,
    this.nombre,
    this.segundoNombre,
    this.apellidoPaterno,
    this.apellidoMaterno,
    this.fechaNacimiento,
    this.sexo,
    this.idNacionalidad,
    this.nacionalidad,
    this.estadoCivil,
    this.correoElectronico,
    this.telefonoMovil,
    this.telefonoAlternativo,
    this.domicilio,
    this.ocupacion,
    this.empresa,
    this.ingresoMensual,
    this.activo,
  });

  final String identificador;
  final String? nombre;
  final String? segundoNombre;
  final String? apellidoPaterno;
  final String? apellidoMaterno;
  final String? fechaNacimiento;
  final String? sexo;
  final int? idNacionalidad;
  final String? nacionalidad;
  final String? estadoCivil;
  final String? correoElectronico;
  final String? telefonoMovil;
  final String? telefonoAlternativo;
  final DomicilioDto? domicilio;
  final String? ocupacion;
  final String? empresa;
  final StrictDecimal? ingresoMensual;
  final bool? activo;

  Map<String, dynamic> toStrictJsonMap() {
    final map = <String, dynamic>{
      'identificador': identificador.trim(),
    };
    void putIfNotEmpty(String key, String? val) {
      if (val != null && val.trim().isNotEmpty) map[key] = val.trim();
    }

    putIfNotEmpty('nombre', nombre);
    putIfNotEmpty('segundoNombre', segundoNombre);
    putIfNotEmpty('apellidoPaterno', apellidoPaterno);
    putIfNotEmpty('apellidoMaterno', apellidoMaterno);
    putIfNotEmpty('fechaNacimiento', fechaNacimiento);
    putIfNotEmpty('sexo', sexo);
    if (idNacionalidad != null) map['idNacionalidad'] = idNacionalidad;
    putIfNotEmpty('nacionalidad', nacionalidad);
    putIfNotEmpty('estadoCivil', estadoCivil);
    if (correoElectronico != null && correoElectronico!.trim().isNotEmpty) {
      map['correoElectronico'] = correoElectronico!.trim().toLowerCase();
    }
    putIfNotEmpty('telefonoMovil', telefonoMovil);
    putIfNotEmpty('telefonoAlternativo', telefonoAlternativo);
    if (domicilio != null) map['domicilio'] = domicilio!.toJson();
    putIfNotEmpty('ocupacion', ocupacion);
    putIfNotEmpty('empresa', empresa);
    if (ingresoMensual != null) map['ingresoMensual'] = ingresoMensual;
    if (activo != null) map['activo'] = activo;
    return map;
  }
}

class ClienteBusquedaAvanzadaDto {
  const ClienteBusquedaAvanzadaDto({
    this.identificador,
    this.curp,
    this.rfc,
    this.correoElectronico,
    this.numeroCuenta,
    this.activo,
    this.fechaInicio,
    this.fechaFin,
  });

  final String? identificador;
  final String? curp;
  final String? rfc;
  final String? correoElectronico;
  final String? numeroCuenta;
  final bool? activo;
  final String? fechaInicio;
  final String? fechaFin;

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{};
    void put(String key, String? v) {
      if (v != null && v.trim().isNotEmpty) map[key] = v.trim();
    }

    put('identificador', identificador);
    put('curp', curp);
    put('rfc', rfc);
    put('correoElectronico', correoElectronico);
    put('numeroCuenta', numeroCuenta);
    if (activo != null) map['activo'] = activo;
    put('fechaInicio', fechaInicio);
    put('fechaFin', fechaFin);
    return map;
  }
}
