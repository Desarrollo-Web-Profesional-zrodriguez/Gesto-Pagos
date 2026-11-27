import { StrictDecimal } from '../network/strictDecimal';

const LETRAS_ESPACIOS = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
const CALLE_COLONIA = /^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s.,-]+$/;
const NUM_DOMICILIO = /^[a-zA-Z0-9\s-]+$/;
const EMPRESA = /^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s.,&-]+$/;
const CURP = /^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$/;
const RFC = /^[A-ZÑ&]{3,4}\d{6}[A-V1-9][A-Z1-9][0-9A]$/;
const EMAIL = /^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const TELEFONO_10 = /^\d{10}$/;
const CODIGO_POSTAL = /^\d{5}$/;
const CUENTA_16 = /^\d{16}$/;
const REFERENCIA_ALFANUMERICA = /^[a-zA-Z0-9]*$/;
const UPC = /^[A-Za-z0-9]{10}$/;
const OTP_6 = /^\d{6}$/;
const PASSWORD_SEGURA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9\s]).{8,64}$/;

export type FieldErrors<K extends string = string> = Partial<Record<K, string>>;

export interface ValidationResult<K extends string = string> {
  valid: boolean;
  errors: FieldErrors<K>;
  firstError: string | null;
  errorCount: number;
}

export function collectValidationErrors<K extends string>(
  checks: Record<K, string | null>
): ValidationResult<K> {
  const errors: FieldErrors<K> = {};
  let firstError: string | null = null;
  let errorCount = 0;

  for (const key of Object.keys(checks) as K[]) {
    const msg = checks[key];
    if (msg) {
      errors[key] = msg;
      errorCount++;
      if (!firstError) {
        firstError = msg;
      }
    }
  }

  return {
    valid: errorCount === 0,
    errors,
    firstError,
    errorCount,
  };
}

export const BackendValidators = {
  soloLetrasRequerido(value: string | undefined, campo: string, min = 3, max = 50): string | null {
    const v = (value ?? '').trim();
    if (!v) return `Ingresa ${campo.toLowerCase()}.`;
    if (v.length < min || v.length > max) {
      return `${campo} debe tener entre ${min} y ${max} caracteres.`;
    }
    if (!LETRAS_ESPACIOS.test(v)) {
      return `${campo} solo admite letras y espacios.`;
    }
    return null;
  },

  soloLetrasOpcional(value: string | undefined, campo: string, min = 3, max = 50): string | null {
    const v = (value ?? '').trim();
    if (!v) return null;
    return BackendValidators.soloLetrasRequerido(v, campo, min, max);
  },

  fechaNacimientoMayorEdad(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'Selecciona tu fecha de nacimiento.';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) {
      return 'Selecciona una fecha de nacimiento válida.';
    }
    const [y, m, d] = v.split('-').map((n) => parseInt(n, 10));
    const parsed = new Date(y, m - 1, d);
    if (
      Number.isNaN(parsed.getTime()) ||
      parsed.getFullYear() !== y ||
      parsed.getMonth() !== m - 1 ||
      parsed.getDate() !== d
    ) {
      return 'La fecha seleccionada no es válida.';
    }
    const hoy = new Date();
    const hoyInicio = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    if (parsed >= hoyInicio) {
      return 'La fecha de nacimiento no puede ser futura.';
    }
    let edad = hoy.getFullYear() - parsed.getFullYear();
    if (
      hoy.getMonth() < parsed.getMonth() ||
      (hoy.getMonth() === parsed.getMonth() && hoy.getDate() < parsed.getDate())
    ) {
      edad--;
    }
    if (edad < 18) {
      return 'Debes ser mayor de 18 años para abrir una cuenta.';
    }
    return null;
  },

  curp(value: string | undefined): string | null {
    const v = (value ?? '').trim().toUpperCase();
    if (!v) return 'Ingresa tu CURP de 18 caracteres.';
    if (!CURP.test(v)) {
      return 'Ingresa una CURP válida de 18 caracteres.';
    }
    return null;
  },

  rfc(value: string | undefined): string | null {
    const v = (value ?? '').trim().toUpperCase();
    if (!v) return 'Ingresa tu RFC con homoclave.';
    if (!RFC.test(v)) {
      return 'Ingresa un RFC válido con homoclave (12 o 13 caracteres).';
    }
    return null;
  },

  email(value: string | undefined, requiredField = true): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? 'Ingresa tu correo electrónico.' : null;
    }
    if (v.length > 100) return 'El correo electrónico no debe exceder 100 caracteres.';
    if (!EMAIL.test(v)) {
      return 'Ingresa un correo electrónico válido (ejemplo: nombre@correo.com).';
    }
    return null;
  },

  telefono(value: string | undefined, requiredField = true): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? 'Ingresa el número telefónico a 10 dígitos.' : null;
    }
    if (!TELEFONO_10.test(v)) {
      return 'El número telefónico debe contener exactamente 10 dígitos.';
    }
    return null;
  },

  calleOColonia(value: string | undefined, campo: string, max: number): string | null {
    const v = (value ?? '').trim();
    if (!v) return `Ingresa ${campo.toLowerCase()}.`;
    if (v.length < 2 || v.length > max) {
      return `${campo} debe tener entre 2 y ${max} caracteres.`;
    }
    if (v.includes('#') || !CALLE_COLONIA.test(v)) {
      return `${campo} contiene caracteres no válidos (evita usar "#").`;
    }
    return null;
  },

  numeroDomicilio(value: string | undefined, campo: string, requiredField: boolean): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? `Ingresa ${campo.toLowerCase()}.` : null;
    }
    if (v.length > 20) return `${campo} no debe exceder 20 caracteres.`;
    if (v.includes('#') || !NUM_DOMICILIO.test(v)) {
      return `${campo} solo admite letras, números y guiones (sin "#").`;
    }
    return null;
  },

  codigoPostal(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'Ingresa el código postal.';
    if (!CODIGO_POSTAL.test(v)) {
      return 'El código postal debe tener 5 dígitos numéricos.';
    }
    return null;
  },

  empresa(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'Ingresa el nombre de la empresa o lugar de trabajo.';
    if (v.length < 2 || v.length > 100) {
      return 'Debe tener entre 2 y 100 caracteres.';
    }
    if (v.includes('#') || !EMPRESA.test(v)) {
      return 'Contiene caracteres no permitidos (evita usar "#").';
    }
    return null;
  },

  montoDecimal(value: string | undefined, campo: string, min = 0.01, max?: number): string | null {
    const v = (value ?? '').trim();
    if (!v) return `Ingresa ${campo.toLowerCase()}.`;
    const parsed = StrictDecimal.tryParse(v);
    if (!parsed) {
      return 'Ingresa un importe válido con máximo 2 decimales (ej. 150.00).';
    }
    if (parsed.asNumber < min) {
      return `${campo} debe ser de al menos $${StrictDecimal.fromNumber(min).toStrictLiteral()} MXN.`;
    }
    if (max !== undefined && parsed.asNumber > max) {
      return `${campo} no puede superar $${StrictDecimal.fromNumber(max).toStrictLiteral()} MXN.`;
    }
    return null;
  },

  passwordLogin(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'Ingresa tu contraseña para continuar.';
    return null;
  },

  passwordSegura(value: string | undefined): string | null {
    const v = value ?? '';
    if (!v) return 'Crea una contraseña segura.';
    if (v.length < 8 || v.length > 64) {
      return 'La contraseña debe tener entre 8 y 64 caracteres.';
    }
    if (!PASSWORD_SEGURA.test(v)) {
      return 'Incluye al menos una mayúscula, minúscula, número y carácter especial.';
    }
    return null;
  },

  confirmacionPassword(password: string | undefined, confirm: string | undefined): string | null {
    if (!(confirm ?? '')) return 'Confirma tu contraseña.';
    if ((password ?? '') !== (confirm ?? '')) return 'Las contraseñas no coinciden.';
    return null;
  },

  codigoOtp(value: string | undefined): string | null {
    const clean = (value ?? '').replace(/\D/g, '');
    if (!OTP_6.test(clean)) {
      return 'Ingresa los 6 dígitos del código de seguridad.';
    }
    return null;
  },

  numeroCuenta(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'Ingresa el número de cuenta de 16 dígitos.';
    if (!CUENTA_16.test(v)) {
      return 'El número de cuenta debe tener exactamente 16 dígitos numéricos.';
    }
    return null;
  },

  referenciaPago(value: string | undefined, requiredField = true): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? 'Ingresa la referencia o línea de captura del servicio.' : null;
    }
    if (v.length > 100) return 'La referencia no debe superar 100 caracteres.';
    if (!REFERENCIA_ALFANUMERICA.test(v)) {
      return 'La referencia solo admite letras y números, sin espacios ni símbolos.';
    }
    return null;
  },

  upc(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'Ingresa el folio UPC a consultar.';
    if (!UPC.test(v)) {
      return 'El folio UPC debe contener exactamente 10 caracteres alfanuméricos.';
    }
    return null;
  },
};
