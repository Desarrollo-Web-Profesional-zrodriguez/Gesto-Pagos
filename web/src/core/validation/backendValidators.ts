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
const PASSWORD_SEGURA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9\s]).{8,64}$/;

export const BackendValidators = {
  soloLetrasRequerido(value: string | undefined, campo: string, min = 3, max = 50): string | null {
    const v = (value ?? '').trim();
    if (!v) return `El campo ${campo} es obligatorio`;
    if (v.length < min || v.length > max) {
      return `${campo} debe tener entre ${min} y ${max} caracteres`;
    }
    if (!LETRAS_ESPACIOS.test(v)) {
      return `${campo} solo debe contener letras y espacios (sin números ni #)`;
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
    if (!v) return 'La fecha de nacimiento es obligatoria';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) {
      return 'Usa formato estricto AAAA-MM-DD (ej. 1992-05-20)';
    }
    const [y, m, d] = v.split('-').map((n) => parseInt(n, 10));
    const parsed = new Date(y, m - 1, d);
    if (
      Number.isNaN(parsed.getTime()) ||
      parsed.getFullYear() !== y ||
      parsed.getMonth() !== m - 1 ||
      parsed.getDate() !== d
    ) {
      return 'Fecha inválida';
    }
    const hoy = new Date();
    const hoyInicio = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    if (parsed >= hoyInicio) {
      return 'La fecha de nacimiento no puede ser futura';
    }
    let edad = hoy.getFullYear() - parsed.getFullYear();
    if (
      hoy.getMonth() < parsed.getMonth() ||
      (hoy.getMonth() === parsed.getMonth() && hoy.getDate() < parsed.getDate())
    ) {
      edad--;
    }
    if (edad < 18) {
      return `El cliente debe ser mayor de edad (18 años o más). Edad: ${edad}`;
    }
    return null;
  },

  curp(value: string | undefined): string | null {
    const v = (value ?? '').trim().toUpperCase();
    if (!v) return 'La CURP es obligatoria';
    if (!CURP.test(v)) {
      return 'CURP inválida (debe tener exactamente 18 caracteres oficiales)';
    }
    return null;
  },

  rfc(value: string | undefined): string | null {
    const v = (value ?? '').trim().toUpperCase();
    if (!v) return 'El RFC es obligatorio';
    if (!RFC.test(v)) {
      return 'RFC inválido con homoclave (12 o 13 caracteres)';
    }
    return null;
  },

  email(value: string | undefined, requiredField = true): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? 'El correo electrónico es obligatorio' : null;
    }
    if (v.length > 100) return 'Máximo 100 caracteres';
    if (!EMAIL.test(v)) {
      return 'Formato inválido (ejemplo: usuario@dominio.com)';
    }
    return null;
  },

  telefono(value: string | undefined, requiredField = true): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? 'El teléfono de 10 dígitos es obligatorio' : null;
    }
    if (!TELEFONO_10.test(v)) {
      return 'Debe contener exactamente 10 dígitos numéricos';
    }
    return null;
  },

  calleOColonia(value: string | undefined, campo: string, max: number): string | null {
    const v = (value ?? '').trim();
    if (!v) return `${campo} es obligatorio`;
    if (v.length < 2 || v.length > max) {
      return `${campo} debe tener entre 2 y ${max} caracteres`;
    }
    if (v.includes('#') || !CALLE_COLONIA.test(v)) {
      return `${campo} contiene caracteres no permitidos (sin "#")`;
    }
    return null;
  },

  numeroDomicilio(value: string | undefined, campo: string, requiredField: boolean): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? `${campo} es obligatorio` : null;
    }
    if (v.length > 20) return `${campo} no puede superar 20 caracteres`;
    if (v.includes('#') || !NUM_DOMICILIO.test(v)) {
      return `${campo} solo acepta letras, números y guiones (sin "#")`;
    }
    return null;
  },

  codigoPostal(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'El código postal es obligatorio';
    if (!CODIGO_POSTAL.test(v)) {
      return 'Debe tener exactamente 5 dígitos numéricos';
    }
    return null;
  },

  empresa(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'La empresa es obligatoria';
    if (v.length < 2 || v.length > 100) {
      return 'Debe tener entre 2 y 100 caracteres';
    }
    if (v.includes('#') || !EMPRESA.test(v)) {
      return 'Contiene caracteres no permitidos (sin "#")';
    }
    return null;
  },

  montoDecimal(value: string | undefined, campo: string, min = 0.01, max?: number): string | null {
    const v = (value ?? '').trim();
    if (!v) return `El campo ${campo} es obligatorio`;
    const parsed = StrictDecimal.tryParse(v);
    if (!parsed) {
      return 'Ingresa un número válido con máximo 2 decimales (ej. 100.00)';
    }
    if (parsed.asNumber < min) {
      return `${campo} debe ser al menos ${StrictDecimal.fromNumber(min).toStrictLiteral()}`;
    }
    if (max !== undefined && parsed.asNumber > max) {
      return `${campo} no puede superar ${StrictDecimal.fromNumber(max).toStrictLiteral()}`;
    }
    return null;
  },

  passwordSegura(value: string | undefined): string | null {
    const v = value ?? '';
    if (!v) return 'La contraseña es obligatoria';
    if (v.length < 8 || v.length > 64) {
      return 'Debe tener entre 8 y 64 caracteres';
    }
    if (!PASSWORD_SEGURA.test(v)) {
      return 'Incluye mayúscula, minúscula, número y carácter especial';
    }
    return null;
  },

  numeroCuenta(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!v) return 'El número de cuenta es obligatorio';
    if (!CUENTA_16.test(v)) {
      return 'El número de cuenta debe tener exactamente 16 dígitos';
    }
    return null;
  },

  referenciaPago(value: string | undefined, requiredField = true): string | null {
    const v = (value ?? '').trim();
    if (!v) {
      return requiredField ? 'La referencia del servicio es obligatoria' : null;
    }
    if (v.length > 100) return 'Máximo 100 caracteres';
    if (!REFERENCIA_ALFANUMERICA.test(v)) {
      return 'Solo caracteres alfanuméricos sin espacios ni símbolos';
    }
    return null;
  },

  upc(value: string | undefined): string | null {
    const v = (value ?? '').trim();
    if (!UPC.test(v)) {
      return 'El UPC debe tener 10 caracteres alfanuméricos';
    }
    return null;
  },
};
