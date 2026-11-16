import { StrictDecimal } from '../../../core/network/strictDecimal';

export interface DomicilioDto {
  calle: string;
  numeroExterior: string;
  numeroInterior?: string;
  colonia: string;
  municipio: string;
  estado: string;
  codigoPostal: string;
  pais: string;
}

export interface SaldoDto {
  idSaldo?: number;
  numeroCuenta: string;
  saldoDisponible: number;
  saldoContable: number;
  fechaUltimaActualizacion?: string;
}

export interface CuentaDto {
  idCuenta?: number;
  idCliente?: number;
  nombreTitular?: string;
  numeroCuenta: string;
  tipoCuenta?: string;
  estatus?: string;
  fechaApertura?: string;
  saldo?: SaldoDto;
}

export interface ClienteDto {
  idCliente?: number;
  nombre: string;
  segundoNombre?: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  nombreCompleto?: string;
  fechaNacimiento?: string;
  edad?: number;
  curp: string;
  rfc: string;
  sexo: string;
  idNacionalidad?: number;
  nacionalidad?: string;
  estadoCivil: string;
  correoElectronico: string;
  telefonoMovil: string;
  telefonoAlternativo?: string;
  ocupacion: string;
  empresa: string;
  ingresoMensual?: number;
  activo?: boolean;
  fechaCreacion?: string;
  fechaModificacion?: string;
  domicilios?: DomicilioDto[];
  cuentas?: CuentaDto[];
}

export interface ClienteRegistroInput {
  identificador?: string;
  nombre: string;
  segundoNombre?: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  curp: string;
  rfc: string;
  sexo: string;
  idNacionalidad?: number;
  nacionalidad?: string;
  estadoCivil: string;
  correoElectronico: string;
  telefonoMovil: string;
  telefonoAlternativo?: string;
  domicilio: DomicilioDto;
  ocupacion: string;
  empresa: string;
  ingresoMensual: StrictDecimal;
  saldoInicial: StrictDecimal;
  password: string;
  biometricoFacialEmbedding?: string;
}

export interface ClientePatchInput {
  identificador: string;
  nombre?: string;
  segundoNombre?: string;
  apellidoPaterno?: string;
  apellidoMaterno?: string;
  fechaNacimiento?: string;
  sexo?: string;
  idNacionalidad?: number;
  nacionalidad?: string;
  estadoCivil?: string;
  correoElectronico?: string;
  telefonoMovil?: string;
  telefonoAlternativo?: string;
  domicilio?: Partial<DomicilioDto>;
  ocupacion?: string;
  empresa?: string;
  ingresoMensual?: StrictDecimal;
  activo?: boolean;
}

export interface ClienteBusquedaFilter {
  identificador?: string;
  curp?: string;
  rfc?: string;
  correoElectronico?: string;
  numeroCuenta?: string;
  activo?: boolean;
}

export function cleanOptionalText(raw?: string | null): string | undefined {
  if (!raw) return undefined;
  const trimmed = raw.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}
