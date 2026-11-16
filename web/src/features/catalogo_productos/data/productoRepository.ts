import { apiClient } from '../../../core/network/apiClient';

export interface ProductItemDto {
  idProducto: number;
  idServicio: number;
  servicio: string;
  producto: string;
  precio?: number | null;
  idCatTipoServicio?: number | null;
  tipoFront: number;
  hasDigitoVerificador?: boolean | null;
  showAyuda?: boolean | null;
  tipoReferencia?: string | null;
  legend?: string | null;
  urlLogo?: string | null;
  urlReferenciaAyuda?: string | null;
}

export interface ProductCategorizedResponseDto {
  status: number;
  message: string;
  origen: string;
  totalProductos: number;
  categorias: Record<string, ProductItemDto[]>;
}

export function esRecargaOPin(p: ProductItemDto): boolean {
  return p.tipoFront === 1;
}

export function esServicioConReferencia(p: ProductItemDto): boolean {
  return p.tipoFront === 2;
}

export function esMontoFijo(p: ProductItemDto): boolean {
  return (p.precio ?? 0) > 0 && p.tipoFront === 1;
}

export function esPinEntretenimiento(p: ProductItemDto): boolean {
  return p.idCatTipoServicio === 10 || p.idCatTipoServicio === 11;
}

export function etiquetaFlujo(p: ProductItemDto): string {
  if (p.tipoFront === 1) {
    return esPinEntretenimiento(p) ? 'PIN / Entretenimiento' : 'Recarga Telefónica';
  }
  return 'Pago de Servicio';
}

export const productoRepository = {
  async obtenerProductos(): Promise<ProductCategorizedResponseDto> {
    const res = await apiClient.get<ProductCategorizedResponseDto>('/productos');
    return res.data;
  },
};
