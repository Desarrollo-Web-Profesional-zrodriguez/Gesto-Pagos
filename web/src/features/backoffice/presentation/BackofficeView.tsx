import React, { useEffect, useState } from 'react';
import {
  ArrowLeft,
  Building2,
  CreditCard,
  Edit3,
  Globe,
  Loader2,
  Plus,
  Power,
  RefreshCw,
  Search,
  Server,
  ShieldCheck,
} from 'lucide-react';
import { ErrorBanner, StatusBadge } from '../../../core/components/UiPrimitives';
import { formatFecha, formatMxn, maskCuenta } from '../../../core/utils/formatters';
import { BackendValidators } from '../../../core/validation/backendValidators';
import {
  NacionalidadDto,
  nacionalidadRepository,
} from '../../catalogos_sistema/data/nacionalidadRepository';
import { ClienteDto, CuentaDto } from '../../clientes/data/clienteModels';
import { clienteRepository } from '../../clientes/data/clienteRepository';
import { ClienteEditModal } from '../../clientes/presentation/ClienteEditModal';
import { cuentaRepository } from '../../cuentas/data/cuentaRepository';
import { PagoEstatusResponseDto, pagoRepository } from '../../pagos/data/pagoRepository';

interface BackofficeViewProps {
  onBack: () => void;
  onOpenOnboarding: () => void;
  onOpenServerConfig: () => void;
}

type AdminTab = 'clientes' | 'cuentas' | 'upc' | 'nacionalidades';

export const BackofficeView: React.FC<BackofficeViewProps> = ({
  onBack,
  onOpenOnboarding,
  onOpenServerConfig,
}) => {
  const [tab, setTab] = useState<AdminTab>('clientes');
  const [error, setError] = useState<unknown>(null);

  // Clientes state
  const [identificadorFilter, setIdentificadorFilter] = useState('');
  const [soloActivos, setSoloActivos] = useState<'todos' | 'true' | 'false'>('todos');
  const [clientes, setClientes] = useState<ClienteDto[]>([]);
  const [loadingClientes, setLoadingClientes] = useState(false);
  const [editingCliente, setEditingCliente] = useState<ClienteDto | null>(null);

  // Cuentas state
  const [cuentasActivas, setCuentasActivas] = useState<CuentaDto[]>([]);
  const [loadingCuentas, setLoadingCuentas] = useState(false);
  const [cuentaQuery, setCuentaQuery] = useState('');
  const [cuentaConsultada, setCuentaConsultada] = useState<CuentaDto | null>(null);

  // UPC state
  const [upcQuery, setUpcQuery] = useState('');
  const [upcResult, setUpcResult] = useState<PagoEstatusResponseDto | null>(null);
  const [loadingUpc, setLoadingUpc] = useState(false);

  // Nacionalidades state
  const [nacionalidades, setNacionalidades] = useState<NacionalidadDto[]>([]);
  const [nacQuery, setNacQuery] = useState('');
  const [nacResult, setNacResult] = useState<NacionalidadDto | null>(null);
  const [loadingNac, setLoadingNac] = useState(false);

  const buscarClientes = async () => {
    setLoadingClientes(true);
    setError(null);
    try {
      const list = await clienteRepository.buscarAvanzado({
        identificador: identificadorFilter.trim() || undefined,
        activo: soloActivos === 'todos' ? undefined : soloActivos === 'true',
      });
      setClientes(list);
    } catch (err) {
      setError(err);
    } finally {
      setLoadingClientes(false);
    }
  };

  const cargarCuentasActivas = async () => {
    setLoadingCuentas(true);
    setError(null);
    try {
      const list = await cuentaRepository.obtenerCuentasActivas();
      setCuentasActivas(list);
    } catch (err) {
      setError(err);
    } finally {
      setLoadingCuentas(false);
    }
  };

  const cargarNacionalidades = async () => {
    setLoadingNac(true);
    setError(null);
    try {
      const list = await nacionalidadRepository.listarTodas();
      setNacionalidades(list);
    } catch (err) {
      setError(err);
    } finally {
      setLoadingNac(false);
    }
  };

  useEffect(() => {
    if (tab === 'clientes') void buscarClientes();
    if (tab === 'cuentas') void cargarCuentasActivas();
    if (tab === 'nacionalidades') void cargarNacionalidades();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const handleToggleActivo = async (cli: ClienteDto) => {
    setError(null);
    try {
      await clienteRepository.actualizarParcial({
        identificador: cli.curp || cli.correoElectronico,
        activo: !(cli.activo !== false),
      });
      await buscarClientes();
    } catch (err) {
      setError(err);
    }
  };

  const handleBuscarCuentaEspecifica = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const errMsg = BackendValidators.numeroCuenta(cuentaQuery);
    if (errMsg) {
      setError(new Error(errMsg));
      return;
    }
    setLoadingCuentas(true);
    try {
      const res = await cuentaRepository.obtenerCuenta(cuentaQuery);
      setCuentaConsultada(res);
    } catch (err) {
      setError(err);
      setCuentaConsultada(null);
    } finally {
      setLoadingCuentas(false);
    }
  };

  const handleConsultarUpc = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const errMsg = BackendValidators.upc(upcQuery);
    if (errMsg) {
      setError(new Error(errMsg));
      return;
    }
    setLoadingUpc(true);
    try {
      const res = await pagoRepository.consultarEstatusPorUpc(upcQuery);
      setUpcResult(res);
    } catch (err) {
      setError(err);
      setUpcResult(null);
    } finally {
      setLoadingUpc(false);
    }
  };

  const handleBuscarNacionalidad = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nacQuery.trim()) return;
    setLoadingNac(true);
    setError(null);
    try {
      const res = await nacionalidadRepository.buscarPorCriterio(nacQuery);
      setNacResult(res);
    } catch (err) {
      setError(err);
      setNacResult(null);
    } finally {
      setLoadingNac(false);
    }
  };

  return (
    <div className="min-h-screen bg-gp-bg">
      <header className="border-b border-gp-border bg-white px-4 py-3.5 sm:px-6">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button type="button" onClick={onBack} className="btn-secondary px-3 py-2 text-xs">
              <ArrowLeft className="h-4 w-4" />
              Volver al login
            </button>
            <div>
              <h1 className="text-lg font-bold text-gp-primary">Consola Operativa Backoffice</h1>
              <p className="text-xs text-gp-muted">
                Administración de clientes, cuentas, conciliación UPC y catálogos
              </p>
            </div>
          </div>

          <button type="button" onClick={onOpenServerConfig} className="btn-secondary text-xs">
            <Server className="h-4 w-4 text-gp-secondary" />
            Servidor API
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-7xl space-y-6 px-4 py-6 sm:px-6">
        {/* Sub-nav */}
        <div className="flex flex-wrap gap-2">
          {[
            { id: 'clientes' as const, label: 'Clientes', icon: Building2 },
            { id: 'cuentas' as const, label: 'Cuentas Bancarias', icon: CreditCard },
            { id: 'upc' as const, label: 'Conciliación UPC', icon: ShieldCheck },
            { id: 'nacionalidades' as const, label: 'Nacionalidades', icon: Globe },
          ].map((item) => {
            const Icon = item.icon;
            const active = tab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setTab(item.id);
                  setError(null);
                }}
                className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-semibold transition ${
                  active
                    ? 'bg-gp-primary text-white shadow-sm'
                    : 'border border-gp-border bg-white text-gp-muted hover:text-gp-primary'
                }`}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </button>
            );
          })}
        </div>

        <ErrorBanner error={error} onDismiss={() => setError(null)} />

        {/* CLIENTES */}
        {tab === 'clientes' && (
          <div className="space-y-4">
            <div className="card-surface flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
                <input
                  className="input-field pl-10"
                  value={identificadorFilter}
                  onChange={(e) => setIdentificadorFilter(e.target.value)}
                  placeholder="Buscar por CURP, RFC, correo o número de cuenta (vacío = todos)..."
                />
              </div>
              <select
                className="input-field sm:w-44"
                value={soloActivos}
                onChange={(e) => setSoloActivos(e.target.value as 'todos' | 'true' | 'false')}
              >
                <option value="todos">Todos los estados</option>
                <option value="true">Solo activos</option>
                <option value="false">Solo inactivos</option>
              </select>
              <button type="button" onClick={buscarClientes} className="btn-primary">
                <Search className="h-4 w-4" />
                Buscar
              </button>
              <button type="button" onClick={onOpenOnboarding} className="btn-secondary">
                <Plus className="h-4 w-4" />
                Nuevo cliente
              </button>
            </div>

            {loadingClientes ? (
              <div className="card-surface flex items-center justify-center py-12">
                <Loader2 className="h-6 w-6 animate-spin text-gp-secondary" />
              </div>
            ) : (
              <div className="space-y-3">
                {clientes.map((c) => (
                  <div
                    key={c.curp || c.correoElectronico}
                    className="card-surface flex flex-col justify-between gap-4 p-4 sm:flex-row sm:items-center"
                  >
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="text-sm font-bold text-gp-primary">
                          {c.nombreCompleto || `${c.nombre} ${c.apellidoPaterno}`}
                        </h3>
                        <StatusBadge estatus={c.activo === false ? 'INACTIVO' : 'ACTIVA'} />
                      </div>
                      <p className="mt-0.5 text-xs text-gp-muted">
                        {c.correoElectronico} · CURP: <span className="font-mono">{c.curp}</span> ·
                        RFC: <span className="font-mono">{c.rfc}</span>
                      </p>
                      {c.cuentas && c.cuentas.length > 0 && (
                        <p className="mt-1 text-xs font-medium text-emerald-700">
                          Cuenta: {maskCuenta(c.cuentas[0].numeroCuenta)} · Disp:{' '}
                          {formatMxn(c.cuentas[0].saldo?.saldoDisponible ?? 0)}
                        </p>
                      )}
                    </div>

                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => setEditingCliente(c)}
                        className="btn-secondary px-3 py-2 text-xs"
                      >
                        <Edit3 className="h-3.5 w-3.5" />
                        Editar
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleToggleActivo(c)}
                        className="btn-secondary px-3 py-2 text-xs"
                      >
                        <Power className="h-3.5 w-3.5" />
                        {c.activo === false ? 'Reactivar' : 'Desactivar'}
                      </button>
                    </div>
                  </div>
                ))}
                {clientes.length === 0 && (
                  <div className="card-surface py-12 text-center text-sm text-gp-muted">
                    No se encontraron clientes con ese criterio.
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* CUENTAS */}
        {tab === 'cuentas' && (
          <div className="space-y-4">
            <form
              onSubmit={handleBuscarCuentaEspecifica}
              className="card-surface flex flex-col gap-3 p-4 sm:flex-row"
            >
              <input
                className="input-field flex-1 font-mono"
                maxLength={16}
                value={cuentaQuery}
                onChange={(e) => setCuentaQuery(e.target.value)}
                placeholder="Consultar cuenta exacta de 16 dígitos..."
              />
              <button type="submit" className="btn-primary">
                Consultar cuenta
              </button>
              <button type="button" onClick={cargarCuentasActivas} className="btn-secondary">
                <RefreshCw className={`h-4 w-4 ${loadingCuentas ? 'animate-spin' : ''}`} />
                Recargar activas
              </button>
            </form>

            {cuentaConsultada && (
              <div className="card-surface border-sky-300 bg-sky-50/40 p-4 text-sm">
                <p className="font-bold text-gp-primary">
                  Cuenta encontrada: {cuentaConsultada.numeroCuenta} ({cuentaConsultada.nombreTitular})
                </p>
                <p className="mt-1 text-xs text-gp-muted">
                  Disponible: {formatMxn(cuentaConsultada.saldo?.saldoDisponible ?? 0)} · Contable:{' '}
                  {formatMxn(cuentaConsultada.saldo?.saldoContable ?? 0)}
                </p>
              </div>
            )}

            <div className="grid gap-3 md:grid-cols-2">
              {cuentasActivas.map((cta) => (
                <div key={cta.numeroCuenta} className="card-surface p-4 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-bold text-gp-primary">{cta.numeroCuenta}</span>
                    <StatusBadge estatus={cta.estatus} />
                  </div>
                  <p className="mt-1 text-xs text-gp-muted">
                    Titular: {cta.nombreTitular || 'N/A'} · Tipo: {cta.tipoCuenta || 'DÉBITO'}
                  </p>
                  <p className="mt-2 font-bold text-emerald-700">
                    Disponible: {formatMxn(cta.saldo?.saldoDisponible ?? 0)}
                  </p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* CONCILIACIÓN UPC */}
        {tab === 'upc' && (
          <div className="space-y-4">
            <form onSubmit={handleConsultarUpc} className="card-surface flex flex-col gap-3 p-4 sm:flex-row">
              <input
                className="input-field flex-1 font-mono"
                maxLength={10}
                value={upcQuery}
                onChange={(e) => setUpcQuery(e.target.value)}
                placeholder="Ingresa UPC de 10 caracteres alfanuméricos..."
              />
              <button type="submit" disabled={loadingUpc} className="btn-primary">
                {loadingUpc ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Search className="h-4 w-4" />
                )}
                Consultar estatus
              </button>
            </form>

            {upcResult && (
              <div className="card-surface space-y-2 p-5 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-base font-bold text-gp-primary">
                    UPC: {upcResult.upc}
                  </span>
                  <StatusBadge estatus={upcResult.estatus} />
                </div>
                <p className="text-xs text-gp-muted">{upcResult.mensaje}</p>
                <div className="grid gap-2 pt-2 text-xs sm:grid-cols-3">
                  <div>Monto: {formatMxn(upcResult.monto)}</div>
                  <div>Autorización: {upcResult.numAutorizacion || 'Pendiente'}</div>
                  <div>Actualizado: {formatFecha(upcResult.fechaActualizacion)}</div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* NACIONALIDADES */}
        {tab === 'nacionalidades' && (
          <div className="space-y-4">
            <form
              onSubmit={handleBuscarNacionalidad}
              className="card-surface flex flex-col gap-3 p-4 sm:flex-row"
            >
              <input
                className="input-field flex-1"
                value={nacQuery}
                onChange={(e) => setNacQuery(e.target.value)}
                placeholder="Buscar nacionalidad (código ISO, país o gentilicio, ej. MEX)..."
              />
              <button type="submit" disabled={loadingNac} className="btn-primary">
                Buscar en catálogo
              </button>
            </form>

            {nacResult && (
              <div className="card-surface border-emerald-200 bg-emerald-50/40 p-4 text-sm">
                Resultado: <strong>{nacResult.gentilicio}</strong> ({nacResult.pais} · ISO:{' '}
                <span className="font-mono">{nacResult.claveIso}</span> · ID #{nacResult.idNacionalidad})
              </div>
            )}

            <div className="card-surface grid gap-2 p-4 sm:grid-cols-2 md:grid-cols-3">
              {nacionalidades.map((n) => (
                <div key={n.idNacionalidad} className="rounded-xl bg-slate-50 px-3 py-2 text-xs">
                  <span className="font-mono font-bold text-gp-secondary">{n.claveIso}</span> ·{' '}
                  <strong>{n.gentilicio}</strong> ({n.pais})
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      <ClienteEditModal
        open={editingCliente !== null}
        cliente={editingCliente}
        onClose={() => setEditingCliente(null)}
        onSaved={() => {
          void buscarClientes();
        }}
      />
    </div>
  );
};
