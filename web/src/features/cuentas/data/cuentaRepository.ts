import { apiClient } from '../../../core/network/apiClient';
import { CuentaDto, SaldoDto } from '../../clientes/data/clienteModels';

export const cuentaRepository = {
  async obtenerCuenta(numeroCuenta: string): Promise<CuentaDto> {
    const res = await apiClient.get<CuentaDto>(
      `/cuentas/${encodeURIComponent(numeroCuenta.trim())}`
    );
    return res.data;
  },

  async obtenerCuentasActivas(): Promise<CuentaDto[]> {
    const res = await apiClient.get<CuentaDto[]>('/cuentas/activas');
    return Array.isArray(res.data) ? res.data : [];
  },

  async consultarSaldo(numeroCuenta: string): Promise<SaldoDto> {
    const res = await apiClient.post<SaldoDto>('/cuentas/consultar-saldo', {
      numeroCuenta: numeroCuenta.trim(),
    });
    return res.data;
  },
};
