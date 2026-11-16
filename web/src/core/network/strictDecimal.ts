/**
 * Representa un monto numérico que DEBE serializarse al cuerpo JSON sin comillas
 * y con exactamente dos decimales en su representación textual cruda (ej. 200.00),
 * cumpliendo con StrictTwoDecimalNumberDeserializer del backend Spring Boot.
 */
export class StrictDecimal {
  private constructor(public readonly cents: number) {
    if (!Number.isInteger(cents) || cents < 0) {
      throw new Error('El monto no puede ser negativo');
    }
  }

  static fromCents(cents: number): StrictDecimal {
    return new StrictDecimal(Math.round(cents));
  }

  static fromNumber(value: number): StrictDecimal {
    if (!Number.isFinite(value) || value < 0) {
      throw new Error('Monto numérico inválido');
    }
    return new StrictDecimal(Math.round(value * 100));
  }

  static tryParse(raw: string): StrictDecimal | null {
    const cleaned = raw.trim().replace(/,/g, '');
    if (!cleaned) return null;
    const match = /^(\d+)(?:\.(\d{1,2}))?$/.exec(cleaned);
    if (!match) return null;

    const whole = parseInt(match[1], 10);
    const fracRaw = match[2] ?? '';
    const fracPadded = fracRaw.padEnd(2, '0');
    const cents = whole * 100 + parseInt(fracPadded, 10);
    return new StrictDecimal(cents);
  }

  static parse(raw: string): StrictDecimal {
    const parsed = StrictDecimal.tryParse(raw);
    if (!parsed) {
      throw new Error(`Formato numérico inválido (${raw}). Debe tener hasta 2 decimales.`);
    }
    return parsed;
  }

  get asNumber(): number {
    return this.cents / 100;
  }

  toStrictLiteral(): string {
    const whole = Math.floor(this.cents / 100);
    const dec = String(this.cents % 100).padStart(2, '0');
    return `${whole}.${dec}`;
  }

  toString(): string {
    return this.toStrictLiteral();
  }
}

export function encodeStrictJson(value: unknown): string {
  return writeJsonValue(value);
}

function writeJsonValue(value: unknown): string {
  if (value instanceof StrictDecimal) {
    return value.toStrictLiteral();
  }
  if (value === null || typeof value === 'boolean' || typeof value === 'number') {
    return JSON.stringify(value);
  }
  if (typeof value === 'string') {
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return `[${value.map((item) => writeJsonValue(item)).join(',')}]`;
  }
  if (typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>).filter(
      ([, v]) => v !== undefined
    );
    const body = entries
      .map(([k, v]) => `${JSON.stringify(k)}:${writeJsonValue(v)}`)
      .join(',');
    return `{${body}}`;
  }
  return JSON.stringify(value);
}
