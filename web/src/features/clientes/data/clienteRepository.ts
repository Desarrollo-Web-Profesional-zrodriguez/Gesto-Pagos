import { apiClient } from '../../../core/network/apiClient';
import {
  cleanOptionalText,
  ClienteBusquedaFilter,
  ClienteDto,
  ClientePatchInput,
  ClienteRegistroInput,
} from './clienteModels';

function buildRegistroPayload(input: ClienteRegistroInput): Record<string, unknown> {
  return {
    identificador: cleanOptionalText(input.identificador),
    nombre: input.nombre.trim(),
    segundoNombre: cleanOptionalText(input.segundoNombre),
    apellidoPaterno: input.apellidoPaterno.trim(),
    apellidoMaterno: input.apellidoMaterno.trim(),
    fechaNacimiento: input.fechaNacimiento.trim(),
    curp: input.curp.trim().toUpperCase(),
    rfc: input.rfc.trim().toUpperCase(),
    sexo: input.sexo.trim(),
    idNacionalidad: input.idNacionalidad,
    nacionalidad: cleanOptionalText(input.nacionalidad),
    estadoCivil: input.estadoCivil.trim(),
    correoElectronico: input.correoElectronico.trim().toLowerCase(),
    telefonoMovil: input.telefonoMovil.trim(),
    telefonoAlternativo: cleanOptionalText(input.telefonoAlternativo),
    domicilio: {
      calle: input.domicilio.calle.trim(),
      numeroExterior: input.domicilio.numeroExterior.trim(),
      numeroInterior: cleanOptionalText(input.domicilio.numeroInterior),
      colonia: input.domicilio.colonia.trim(),
      municipio: input.domicilio.municipio.trim(),
      estado: input.domicilio.estado.trim(),
      codigoPostal: input.domicilio.codigoPostal.trim(),
      pais: input.domicilio.pais.trim(),
    },
    ocupacion: input.ocupacion.trim(),
    empresa: input.empresa.trim(),
    ingresoMensual: input.ingresoMensual,
    saldoInicial: input.saldoInicial,
    password: input.password,
    biometricoFacialEmbedding: cleanOptionalText(input.biometricoFacialEmbedding),
  };
}

export interface SolicitarOtpOnboardingInput {
  correoElectronico: string;
  nombre?: string;
  curp?: string;
  rfc?: string;
}

export interface SolicitarOtpOnboardingResponse {
  enviado: boolean;
  emailEnmascarado: string;
  expiraEnSegundos: number;
  mensaje: string;
}

export const clienteRepository = {
  async solicitarOtpOnboarding(
    input: SolicitarOtpOnboardingInput
  ): Promise<SolicitarOtpOnboardingResponse> {
    const res = await apiClient.post<SolicitarOtpOnboardingResponse>(
      '/auth/onboarding/enviar-otp',
      {
        correoElectronico: input.correoElectronico.trim().toLowerCase(),
        nombre: cleanOptionalText(input.nombre),
        curp: cleanOptionalText(input.curp)?.toUpperCase(),
        rfc: cleanOptionalText(input.rfc)?.toUpperCase(),
      }
    );
    return res.data;
  },

  async registrarConOtp(codigoOtp: string, input: ClienteRegistroInput): Promise<ClienteDto> {
    const res = await apiClient.post<ClienteDto>('/auth/onboarding/registrar-verificado', {
      codigoOtp: codigoOtp.trim(),
      registro: buildRegistroPayload(input),
    });
    return res.data;
  },

  async registrar(input: ClienteRegistroInput): Promise<ClienteDto> {
    const res = await apiClient.post<ClienteDto>('/clientes', buildRegistroPayload(input));
    return res.data;
  },

  async consultarPorIdentificador(identificador: string): Promise<ClienteDto> {
    const res = await apiClient.post<ClienteDto>('/clientes/consultar', {
      identificador: identificador.trim(),
    });
    return res.data;
  },

  async buscarAvanzado(filter: ClienteBusquedaFilter = {}): Promise<ClienteDto[]> {
    const payload: Record<string, unknown> = {
      identificador: cleanOptionalText(filter.identificador),
      curp: cleanOptionalText(filter.curp)?.toUpperCase(),
      rfc: cleanOptionalText(filter.rfc)?.toUpperCase(),
      correoElectronico: cleanOptionalText(filter.correoElectronico)?.toLowerCase(),
      numeroCuenta: cleanOptionalText(filter.numeroCuenta),
      activo: filter.activo,
    };
    const res = await apiClient.post<ClienteDto[]>('/clientes/buscar', payload);
    return Array.isArray(res.data) ? res.data : [];
  },

  async actualizarCompleto(input: ClienteRegistroInput): Promise<ClienteDto> {
    const res = await apiClient.put<ClienteDto>('/clientes', buildRegistroPayload(input));
    return res.data;
  },

  async actualizarParcial(input: ClientePatchInput): Promise<ClienteDto> {
    const payload: Record<string, unknown> = {
      identificador: input.identificador.trim(),
      nombre: cleanOptionalText(input.nombre),
      segundoNombre: input.segundoNombre !== undefined ? input.segundoNombre.trim() : undefined,
      apellidoPaterno: cleanOptionalText(input.apellidoPaterno),
      apellidoMaterno: cleanOptionalText(input.apellidoMaterno),
      fechaNacimiento: cleanOptionalText(input.fechaNacimiento),
      sexo: cleanOptionalText(input.sexo),
      idNacionalidad: input.idNacionalidad,
      nacionalidad: cleanOptionalText(input.nacionalidad),
      estadoCivil: cleanOptionalText(input.estadoCivil),
      correoElectronico: cleanOptionalText(input.correoElectronico)?.toLowerCase(),
      telefonoMovil: cleanOptionalText(input.telefonoMovil),
      telefonoAlternativo:
        input.telefonoAlternativo !== undefined ? input.telefonoAlternativo.trim() : undefined,
      ocupacion: cleanOptionalText(input.ocupacion),
      empresa: cleanOptionalText(input.empresa),
      ingresoMensual: input.ingresoMensual,
      activo: input.activo,
    };
    if (input.domicilio) {
      payload.domicilio = {
        calle: cleanOptionalText(input.domicilio.calle),
        numeroExterior: cleanOptionalText(input.domicilio.numeroExterior),
        numeroInterior:
          input.domicilio.numeroInterior !== undefined
            ? input.domicilio.numeroInterior.trim()
            : undefined,
        colonia: cleanOptionalText(input.domicilio.colonia),
        municipio: cleanOptionalText(input.domicilio.municipio),
        estado: cleanOptionalText(input.domicilio.estado),
        codigoPostal: cleanOptionalText(input.domicilio.codigoPostal),
        pais: cleanOptionalText(input.domicilio.pais),
      };
    }
    const res = await apiClient.patch<ClienteDto>('/clientes', payload);
    return res.data;
  },
};
