import 'dart:convert';

/// Representa un monto numérico que DEBE serializarse al cuerpo JSON sin comillas
/// y con exactamente dos decimales en su representación textual cruda (ej. `200.00`),
/// cumpliendo con `StrictTwoDecimalNumberDeserializer` del backend Spring Boot.
class StrictDecimal {
  StrictDecimal._(this.cents) : assert(cents >= 0);

  /// Valor entero en centavos para evitar imprecisiones binarias de IEEE 754.
  final int cents;

  factory StrictDecimal.fromCents(int cents) {
    if (cents < 0) {
      throw const FormatException('El monto no puede ser negativo');
    }
    return StrictDecimal._(cents);
  }

  factory StrictDecimal.fromDouble(double value) {
    if (value.isNaN || value.isInfinite || value < 0) {
      throw const FormatException('Monto numérico inválido');
    }
    return StrictDecimal._((value * 100).round());
  }

  /// Parsea cadenas como "200", "200.5", "200.00" o "1,250.50" hacia centavos exactos.
  static StrictDecimal? tryParse(String raw) {
    final cleaned = raw.trim().replaceAll(',', '');
    if (cleaned.isEmpty) return null;
    final match = RegExp(r'^(\d+)(?:\.(\d{1,2}))?$').firstMatch(cleaned);
    if (match == null) return null;

    final whole = int.parse(match.group(1)!);
    final fracRaw = match.group(2) ?? '';
    final fracPadded = fracRaw.padRight(2, '0');
    final cents = whole * 100 + int.parse(fracPadded);
    return StrictDecimal._(cents);
  }

  factory StrictDecimal.parse(String raw) {
    final parsed = tryParse(raw);
    if (parsed == null) {
      throw FormatException(
        'Formato numérico inválido ($raw). Debe tener hasta 2 decimales.',
      );
    }
    return parsed;
  }

  double get asDouble => cents / 100.0;

  /// Retorna siempre `entero.XX` (ej. `200.00`, `0.00`, `150.50`).
  String toStrictLiteral() {
    final whole = cents ~/ 100;
    final dec = (cents % 100).toString().padLeft(2, '0');
    return '$whole.$dec';
  }

  @override
  String toString() => toStrictLiteral();

  @override
  bool operator ==(Object other) =>
      other is StrictDecimal && other.cents == cents;

  @override
  int get hashCode => cents.hashCode;
}

/// Codifica cualquier mapa/lista a JSON estándar preservando literales numéricos
/// exactos de 2 decimales para cualquier instancia de [StrictDecimal].
String encodeStrictJson(Object? value) {
  final buffer = StringBuffer();
  _writeJsonValue(value, buffer);
  return buffer.toString();
}

void _writeJsonValue(Object? value, StringBuffer buffer) {
  if (value is StrictDecimal) {
    buffer.write(value.toStrictLiteral());
    return;
  }
  if (value == null || value is bool || value is num) {
    buffer.write(jsonEncode(value));
    return;
  }
  if (value is String) {
    buffer.write(jsonEncode(value));
    return;
  }
  if (value is List) {
    buffer.write('[');
    for (var i = 0; i < value.length; i++) {
      if (i > 0) buffer.write(',');
      _writeJsonValue(value[i], buffer);
    }
    buffer.write(']');
    return;
  }
  if (value is Map) {
    buffer.write('{');
    var first = true;
    for (final entry in value.entries) {
      if (!first) buffer.write(',');
      first = false;
      buffer.write(jsonEncode(entry.key.toString()));
      buffer.write(':');
      _writeJsonValue(entry.value, buffer);
    }
    buffer.write('}');
    return;
  }
  buffer.write(jsonEncode(value));
}
