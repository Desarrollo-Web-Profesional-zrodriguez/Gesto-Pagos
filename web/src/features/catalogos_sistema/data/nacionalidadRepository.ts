import { apiClient } from '../../../core/network/apiClient';

export interface NacionalidadDto {
  idNacionalidad: number;
  claveIso: string;
  pais: string;
  gentilicio: string;
}

export const nacionalidadRepository = {
  async listarTodas(): Promise<NacionalidadDto[]> {
    const res = await apiClient.get<NacionalidadDto[]>('/catalogos/nacionalidades');
    return Array.isArray(res.data) ? res.data : [];
  },

  async buscarPorCriterio(criterio: string): Promise<NacionalidadDto> {
    const res = await apiClient.post<NacionalidadDto>('/catalogos/nacionalidades/buscar', {
      criterio: criterio.trim(),
    });
    return res.data;
  },
};
