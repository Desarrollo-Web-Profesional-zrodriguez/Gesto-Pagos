import { apiClient } from '../../../core/network/apiClient';

export interface LoginRequestDto {
  username: string;
  password?: string;
  biometricoFacialEmbedding?: string;
}

export interface LoginResponseDto {
  tokenSesion: string;
  activa?: boolean;
  tipoToken?: string;
  fechaInicio?: string;
  fechaExpiracion?: string;
  minutosInactividadMaximos?: number;
  idUsuario?: number;
  idCliente?: number;
  username: string;
  nombreCliente?: string;
  mensaje?: string;
}

export interface ValidarSesionResponseDto {
  valida: boolean;
  mensaje: string;
}

export interface LogoutResponseDto {
  mensaje: string;
}

export const authRepository = {
  async login(request: LoginRequestDto): Promise<LoginResponseDto> {
    const payload: Record<string, unknown> = {
      username: request.username.trim().toLowerCase(),
    };
    if (request.password && request.password.trim().length > 0) {
      payload.password = request.password;
    }
    if (request.biometricoFacialEmbedding && request.biometricoFacialEmbedding.trim().length > 0) {
      payload.biometricoFacialEmbedding = request.biometricoFacialEmbedding.trim();
    }
    const res = await apiClient.post<LoginResponseDto>('/auth/login', payload);
    return res.data;
  },

  async validarSesion(tokenOverride?: string): Promise<ValidarSesionResponseDto> {
    const res = await apiClient.get<ValidarSesionResponseDto>(
      '/auth/validar-sesion',
      tokenOverride ? { headers: { Authorization: `Bearer ${tokenOverride}` } } : undefined
    );
    return res.data;
  },

  async logout(tokenOverride?: string): Promise<LogoutResponseDto> {
    const res = await apiClient.post<LogoutResponseDto>(
      '/auth/logout',
      {},
      tokenOverride ? { headers: { Authorization: `Bearer ${tokenOverride}` } } : undefined
    );
    return res.data;
  },
};
