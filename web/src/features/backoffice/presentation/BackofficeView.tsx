import React, { useEffect, useMemo, useState } from 'react';
import {
  Building2,
  CreditCard,
  Edit3,
  Globe,
  Loader2,
  LogOut,
  Plus,
  Power,
  RefreshCw,
  Search,
  ShieldCheck,
  Users,
  X,
} from 'lucide-react';
import { CustomSelect, SelectOption } from '../../../core/components/CustomSelect';
import { useToast } from '../../../core/components/ToastContext';
import {
  CopyChip,
  EmptyState,
  ErrorBanner,
  FieldError,
  SkeletonBlock,
  StatusBadge,
} from '../../../core/components/UiPrimitives';
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
}

type AdminTab = 'clientes' | 'cuentas' | 'upc' | 'nacionalidades';

const ADMIN_TAB_KEY = 'gestopago_backoffice_tab';

const ESTADO_CLIENTE_OPTIONS: SelectOption[] = [
  { value: 'todos', label: 'Todos los estados', dotClass: 'bg-slate-400' },
  { value: 'true', label: 'Solo activos', dotClass: 'bg-emerald-500' },
  { value: 'false', label: 'Solo inactivos', dotClass: 'bg-rose-500' },
];

function getInitialAdminTab(): AdminTab {
  try {
    const saved = window.sessionStorage.getItem(ADMIN_TAB_KEY);
    if (
      saved === 'clientes' ||
      saved === 'cuentas' ||
      saved === 'upc' ||
      saved === 'nacionalidades'
    ) {
      return saved;
    }
  } catch {
    // Ignore storage errors
  }
  return 'clientes';
}

export const BackofficeView: React.FC<BackofficeViewProps> = ({ onBack, onOpenOnboarding }) => {
  const { notify } = useToast();
  const [tab, setTab] = useState<AdminTab>(getInitialAdminTab);
  const [error, setError] = useState<unknown>(null);

  // Clientes state
  const [identificadorFilter, setIdentificadorFilter] = useState('');
  const [soloActivos, setSoloActivos] = useState<'todos' | 'true' | 'false'>('todos');
  const [clientes, setClientes] = useState<ClienteDto[]>([]);
  const [loadingClientes, setLoadingClientes] = useState(false);
  const [togglingKey, setTogglingKey] = useState<string | null>(null);
  const [editingCliente, setEditingCliente] = useState<ClienteDto | null>(null);

  // Cuentas state
  const [cuentasActivas, setCuentasActivas] = useState<CuentaDto[]>([]);
  const [loadingCuentas, setLoadingCuentas] = useState(false);
  const [cuentaFilter, setCuentaFilter] = useState('');
  const [cuentaQuery, setCuentaQuery] = useState('');
  const [cuentaQueryError, setCuentaQueryError] = useState<string | null>(null);
  const [cuentaConsultada, setCuentaConsultada] = useState<CuentaDto | null>(null);

  // UPC state
  const [upcQuery, setUpcQuery] = useState('');
  const [upcQueryError, setUpcQueryError] = useState<string | null>(null);
  const [upcResult, setUpcResult] = useState<PagoEstatusResponseDto | null>(null);
  const [loadingUpc, setLoadingUpc] = useState(false);

  // Nacionalidades state
  const [nacionalidades, setNacionalidades] = useState<NacionalidadDto[]>([]);
  const [nacFilter, setNacFilter] = useState('');
  const [nacQuery, setNacQuery] = useState('');
  const [nacQueryError, setNacQueryError] = useState<string | null>(null);
  const [nacResult, setNacResult] = useState<NacionalidadDto | null>(null);
  const [loadingNac, setLoadingNac] = useState(false);

  const changeTab = (next: AdminTab) => {
    setTab(next);
    setError(null);
    setCuentaQueryError(null);
    setUpcQueryError(null);
    setNacQueryError(null);
    try {
      window.sessionStorage.setItem(ADMIN_TAB_KEY, next);
    } catch {
      // Ignore storage errors
    }
  };

  const buscarClientes = async (filtroEstado: 'todos' | 'true' | 'false' = soloActivos) => {
    setLoadingClientes(true);
    setError(null);
    try {
      const list = await clienteRepository.buscarAvanzado({
        activo: filtroEstado === 'todos' ? undefined : filtroEstado === 'true',
      });
      setClientes(list);
    } catch (err) {
      setError(err);
    } finally {
      setLoadingClientes(false);
    }
  };

  useEffect(() => {
    if (tab === 'clientes') {
      void buscarClientes(soloActivos);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [soloActivos]);

  const clientesStats = useMemo(() => {
    let activos = 0;
    let inactivos = 0;
    for (const c of clientes) {
      if (c.activo === false) inactivos++;
      else activos++;
    }
    return { total: clientes.length, activos, inactivos };
  }, [clientes]);

  const clientesFiltrados = useMemo(() => {
    const q = identificadorFilter.trim().toLowerCase();
    if (!q) return clientes;
    return clientes.filter((c) => {
      const nombre = (
        c.nombreCompleto ||
        `${c.nombre ?? ''} ${c.apellidoPaterno ?? ''} ${c.apellidoMaterno ?? ''}`
      ).toLowerCase();
      const email = (c.correoElectronico ?? '').toLowerCase();
      const curp = (c.curp ?? '').toLowerCase();
      const rfc = (c.rfc ?? '').toLowerCase();
      const tel = (c.telefonoMovil ?? '').toLowerCase();
      const cuentasMatch = (c.cuentas ?? []).some((cta) =>
        (cta.numeroCuenta ?? '').toLowerCase().includes(q)
      );
      return (
        nombre.includes(q) ||
        email.includes(q) ||
        curp.includes(q) ||
        rfc.includes(q) ||
        tel.includes(q) ||
        cuentasMatch
      );
    });
  }, [clientes, identificadorFilter]);

  const cuentasFiltradas = useMemo(() => {
    const q = cuentaFilter.trim().toLowerCase();
    if (!q) return cuentasActivas;
    return cuentasActivas.filter(
      (c) =>
        c.numeroCuenta.toLowerCase().includes(q) ||
        (c.nombreTitular ?? '').toLowerCase().includes(q)
    );
  }, [cuentasActivas, cuentaFilter]);

  const nacionalidadesFiltradas = useMemo(() => {
    const q = nacFilter.trim().toLowerCase();
    if (!q) return nacionalidades;
    return nacionalidades.filter(
      (n) =>
        n.claveIso.toLowerCase().includes(q) ||
        n.gentilicio.toLowerCase().includes(q) ||
        n.pais.toLowerCase().includes(q)
    );
  }, [nacionalidades, nacFilter]);

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
    const key = cli.curp || cli.correoElectronico;
    const nextActivo = cli.activo === false;
    setTogglingKey(key);
    setError(null);
    try {
      await clienteRepository.actualizarParcial({
        identificador: key,
        activo: nextActivo,
      });
      notify({
        title: nextActivo ? 'Cliente reactivado' : 'Cliente desactivado',
        description: cli.nombreCompleto || cli.correoElectronico,
        variant: nextActivo ? 'success' : 'warning',
      });
      await buscarClientes();
    } catch (err) {
      setError(err);
    } finally {
      setTogglingKey(null);
    }
  };

  const handleBuscarCuentaEspecifica = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const errMsg = BackendValidators.numeroCuenta(cuentaQuery);
    if (errMsg) {
      setCuentaQueryError(errMsg);
      return;
    }
    setCuentaQueryError(null);
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
      setUpcQueryError(errMsg);
      return;
    }
    setUpcQueryError(null);
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
    setError(null);
    if (!nacQuery.trim()) {
      setNacQueryError('Ingresa una clave ISO, país o gentilicio para buscar.');
      return;
    }
    setNacQueryError(null);
    setLoadingNac(true);
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

  const totalSaldoActivas = useMemo(
    () => cuentasActivas.reduce((acc, c) => acc + (c.saldo?.saldoDisponible ?? 0), 0),
    [cuentasActivas]
  );

  return (
    <div className="min-h-screen bg-gp-bg">
      <header className="border-b border-slate-800 bg-slate-950 px-4 py-4 text-white sm:px-6">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 text-amber-400 ring-1 ring-inset ring-amber-400/30">
              <Building2 className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-display text-base font-extrabold tracking-tight sm:text-lg">
                  Consola Operativa
                </h1>
                <span className="rounded-full bg-amber-400/15 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-amber-300">
                  Administración
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Supervisión de clientes, cuentas digitales, seguimiento UPC y catálogos
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={onOpenOnboarding}
              className="inline-flex items-center gap-1.5 rounded-xl bg-sky-500 px-3.5 py-2 text-xs font-bold text-slate-950 transition hover:bg-sky-400"
            >
              <Plus className="h-4 w-4" />
              Alta de cliente
            </button>
            <button
              type="button"
              onClick={onBack}
              className="inline-flex items-center gap-1.5 rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs font-semibold text-rose-200 transition hover:bg-rose-500/20"
            >
              <LogOut className="h-3.5 w-3.5" />
              Cerrar consola
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl space-y-6 px-4 py-6 sm:px-6">
        <div className="flex flex-wrap gap-2">
          {[
            { id: 'clientes' as const, label: 'Expedientes de Clientes', icon: Users },
            { id: 'cuentas' as const, label: 'Cuentas Bancarias', icon: CreditCard },
            { id: 'upc' as const, label: 'Seguimiento UPC', icon: ShieldCheck },
            { id: 'nacionalidades' as const, label: 'Catálogo Nacionalidades', icon: Globe },
          ].map((item) => {
            const Icon = item.icon;
            const active = tab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => changeTab(item.id)}
                className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-semibold transition ${
                  active
                    ? 'bg-slate-950 text-white shadow-sm'
                    : 'border border-gp-border bg-white text-gp-muted hover:border-slate-300 hover:text-gp-primary'
                }`}
              >
                <Icon className={`h-4 w-4 ${active ? 'text-sky-400' : ''}`} />
                {item.label}
              </button>
            );
          })}
        </div>

        <ErrorBanner error={error} onDismiss={() => setError(null)} />

        {/* TAB 1: CLIENTES */}
        {tab === 'clientes' && (
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-3">
              <div className="card-surface p-4">
                <p className="text-xs font-medium text-gp-muted">Clientes listados</p>
                <p className="mt-1 font-display text-2xl font-extrabold text-gp-primary tabular-nums">
                  {clientesStats.total}
                </p>
              </div>
              <div className="card-surface p-4">
                <p className="text-xs font-medium text-gp-muted">Activos</p>
                <p className="mt-1 font-display text-2xl font-extrabold text-emerald-700 tabular-nums">
                  {clientesStats.activos}
                </p>
              </div>
              <div className="card-surface p-4">
                <p className="text-xs font-medium text-gp-muted">Inactivos</p>
                <p className="mt-1 font-display text-2xl font-extrabold text-slate-600 tabular-nums">
                  {clientesStats.inactivos}
                </p>
              </div>
            </div>

            <div className="card-surface flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
                <input
                  className="input-field pl-10 pr-9"
                  value={identificadorFilter}
                  onChange={(e) => setIdentificadorFilter(e.target.value)}
                  placeholder="Filtrar por nombre, CURP, RFC, correo, teléfono o cuenta..."
                />
                {identificadorFilter && (
                  <button
                    type="button"
                    onClick={() => setIdentificadorFilter('')}
                    aria-label="Limpiar búsqueda"
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>

              <div className="sm:w-56">
                <CustomSelect
                  ariaLabel="Filtrar estado de clientes"
                  value={soloActivos}
                  options={ESTADO_CLIENTE_OPTIONS}
                  searchable={false}
                  onChange={(val) => setSoloActivos(val as 'todos' | 'true' | 'false')}
                />
              </div>

              <button
                type="button"
                onClick={() => void buscarClientes(soloActivos)}
                disabled={loadingClientes}
                className="btn-secondary"
              >
                <RefreshCw className={`h-4 w-4 ${loadingClientes ? 'animate-spin' : ''}`} />
                <span>Actualizar</span>
              </button>
            </div>

            {loadingClientes ? (
              <div className="space-y-3">
                {Array.from({ length: 4 }).map((_, idx) => (
                  <SkeletonBlock key={idx} className="h-24" />
                ))}
              </div>
            ) : clientesFiltrados.length === 0 ? (
              <EmptyState
                icon={Users}
                title="No se encontraron expedientes de clientes"
                description="Ajusta el filtro de texto o estado, o da de alta un nuevo cliente."
              />
            ) : (
              <div className="space-y-3">
                {clientesFiltrados.map((c) => {
                  const rowKey = c.curp || c.correoElectronico;
                  const isToggling = togglingKey === rowKey;
                  return (
                    <div
                      key={rowKey}
                      className="card-surface flex flex-col justify-between gap-4 p-4 transition hover:border-slate-300 sm:flex-row sm:items-center sm:p-5"
                    >
                      <div className="min-w-0 space-y-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-sm font-bold text-gp-primary">
                            {c.nombreCompleto || `${c.nombre} ${c.apellidoPaterno}`}
                          </h3>
                          <StatusBadge estatus={c.activo === false ? 'INACTIVO' : 'ACTIVA'} />
                        </div>
                        <p className="text-xs text-gp-muted">
                          {c.correoElectronico} · Tel:{' '}
                          <span className="font-mono">{c.telefonoMovil}</span>
                        </p>
                        <div className="flex flex-wrap items-center gap-2 pt-0.5 text-xs">
                          <CopyChip value={c.curp} label={`CURP: ${c.curp}`} toastTitle="CURP copiada" />
                          <CopyChip value={c.rfc} label={`RFC: ${c.rfc}`} toastTitle="RFC copiado" />
                          {c.cuentas && c.cuentas.length > 0 && (
                            <span className="rounded-lg bg-emerald-50 px-2.5 py-0.5 font-mono text-xs font-semibold text-emerald-700">
                              {maskCuenta(c.cuentas[0].numeroCuenta)} ·{' '}
                              {formatMxn(c.cuentas[0].saldo?.saldoDisponible ?? 0)}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex shrink-0 flex-wrap gap-2">
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
                          disabled={isToggling}
                          onClick={() => void handleToggleActivo(c)}
                          className={
                            c.activo === false
                              ? 'btn-secondary border-emerald-200 bg-emerald-50/60 px-3 py-2 text-xs text-emerald-700 hover:bg-emerald-100/70'
                              : 'btn-danger-soft px-3 py-2 text-xs'
                          }
                        >
                          {isToggling ? (
                            <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          ) : (
                            <Power className="h-3.5 w-3.5" />
                          )}
                          {c.activo === false ? 'Reactivar' : 'Desactivar'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: CUENTAS */}
        {tab === 'cuentas' && (
          <div className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="card-surface p-4">
                <p className="text-xs font-medium text-gp-muted">Cuentas activas en el sistema</p>
                <p className="mt-1 font-display text-2xl font-extrabold text-gp-primary tabular-nums">
                  {cuentasActivas.length}
                </p>
              </div>
              <div className="card-surface p-4">
                <p className="text-xs font-medium text-gp-muted">Liquidez disponible acumulada</p>
                <p className="mt-1 font-display text-2xl font-extrabold text-emerald-700 tabular-nums">
                  {formatMxn(totalSaldoActivas)}
                </p>
              </div>
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <form
                noValidate
                onSubmit={handleBuscarCuentaEspecifica}
                className="card-surface p-4"
              >
                <div className="flex flex-col gap-2.5 sm:flex-row">
                  <input
                    className={`input-field flex-1 font-mono ${
                      cuentaQueryError ? 'input-field-error' : ''
                    }`}
                    inputMode="numeric"
                    maxLength={16}
                    value={cuentaQuery}
                    onChange={(e) => {
                      setCuentaQuery(e.target.value.replace(/\D/g, ''));
                      if (cuentaQueryError) setCuentaQueryError(null);
                    }}
                    placeholder="Consulta directa por 16 dígitos exactos..."
                  />
                  <button type="submit" disabled={loadingCuentas} className="btn-primary shrink-0">
                    Consultar
                  </button>
                </div>
                <FieldError message={cuentaQueryError} />
              </form>

              <div className="card-surface flex items-start gap-2.5 p-4">
                <div className="relative flex-1">
                  <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
                  <input
                    className="input-field pl-10"
                    value={cuentaFilter}
                    onChange={(e) => setCuentaFilter(e.target.value)}
                    placeholder="Filtrar listado por titular o dígitos..."
                  />
                </div>
                <button
                  type="button"
                  onClick={cargarCuentasActivas}
                  disabled={loadingCuentas}
                  className="btn-secondary shrink-0"
                >
                  <RefreshCw className={`h-4 w-4 ${loadingCuentas ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>

            {cuentaConsultada && (
              <div className="card-surface flex flex-wrap items-center justify-between gap-4 border-sky-300 bg-sky-50/50 p-4 text-sm">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-gp-secondary">
                      Resultado directo
                    </span>
                    <StatusBadge estatus={cuentaConsultada.estatus} />
                  </div>
                  <p className="mt-1 font-bold text-gp-primary">
                    {cuentaConsultada.nombreTitular || 'Titular'} ·{' '}
                    <span className="font-mono">{cuentaConsultada.numeroCuenta}</span>
                  </p>
                  <p className="mt-0.5 text-xs text-gp-muted">
                    Disponible:{' '}
                    <strong>{formatMxn(cuentaConsultada.saldo?.saldoDisponible ?? 0)}</strong> ·
                    Contable:{' '}
                    <strong>{formatMxn(cuentaConsultada.saldo?.saldoContable ?? 0)}</strong>
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setCuentaConsultada(null)}
                  className="btn-secondary px-2.5 py-1.5 text-xs"
                >
                  Cerrar
                </button>
              </div>
            )}

            {loadingCuentas ? (
              <div className="grid gap-3 md:grid-cols-2">
                {Array.from({ length: 4 }).map((_, idx) => (
                  <SkeletonBlock key={idx} className="h-28" />
                ))}
              </div>
            ) : cuentasFiltradas.length === 0 ? (
              <EmptyState
                icon={CreditCard}
                title="Sin cuentas para mostrar"
                description="No hay cuentas activas que coincidan con el filtro."
              />
            ) : (
              <div className="grid gap-3 md:grid-cols-2">
                {cuentasFiltradas.map((cta) => (
                  <div key={cta.numeroCuenta} className="card-surface p-4 text-sm">
                    <div className="flex items-center justify-between gap-2">
                      <CopyChip value={cta.numeroCuenta} toastTitle="Número de cuenta copiado" />
                      <StatusBadge estatus={cta.estatus} />
                    </div>
                    <p className="mt-2 text-xs font-semibold text-gp-primary">
                      {cta.nombreTitular || 'Titular sin nombre'} ·{' '}
                      <span className="font-normal text-gp-muted">
                        {cta.tipoCuenta || 'DÉBITO'}
                      </span>
                    </p>
                    <div className="mt-2.5 flex items-center justify-between border-t border-slate-100 pt-2.5 text-xs">
                      <span className="text-gp-muted">Saldo disponible</span>
                      <span className="font-mono font-bold text-emerald-700 tabular-nums">
                        {formatMxn(cta.saldo?.saldoDisponible ?? 0)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: CONCILIACIÓN UPC */}
        {tab === 'upc' && (
          <div className="space-y-4">
            <form noValidate onSubmit={handleConsultarUpc} className="card-surface p-5">
              <label htmlFor="upc-search-input" className="input-label">
                Consulta de Operaciones por Folio UPC (10 caracteres)
              </label>
              <div className="flex flex-col gap-3 sm:flex-row">
                <input
                  id="upc-search-input"
                  className={`input-field flex-1 font-mono uppercase ${
                    upcQueryError ? 'input-field-error' : ''
                  }`}
                  maxLength={10}
                  value={upcQuery}
                  onChange={(e) => {
                    setUpcQuery(e.target.value.trim());
                    if (upcQueryError) setUpcQueryError(null);
                  }}
                  placeholder="Ej. A1B2C3D4E5"
                />
                <button type="submit" disabled={loadingUpc} className="btn-primary shrink-0">
                  {loadingUpc ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Search className="h-4 w-4" />
                  )}
                  Verificar estatus
                </button>
              </div>
              <FieldError message={upcQueryError} />
            </form>

            {upcResult ? (
              <div className="card-surface space-y-4 p-5 sm:p-6">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                  <div className="flex items-center gap-2.5">
                    <span className="text-xs font-bold uppercase tracking-wider text-gp-muted">
                      UPC
                    </span>
                    <CopyChip value={upcResult.upc} toastTitle="UPC copiado" />
                  </div>
                  <StatusBadge estatus={upcResult.estatus} />
                </div>

                {upcResult.mensaje && (
                  <p className="rounded-xl bg-slate-50 p-3.5 text-xs text-slate-700">
                    {upcResult.mensaje}
                  </p>
                )}

                <div className="grid gap-3 text-xs sm:grid-cols-3">
                  <div className="rounded-xl border border-slate-100 p-3">
                    <span className="block text-gp-muted">Importe registrado</span>
                    <span className="mt-0.5 block font-mono text-sm font-bold text-gp-primary">
                      {formatMxn(upcResult.monto)}
                    </span>
                  </div>
                  <div className="rounded-xl border border-slate-100 p-3">
                    <span className="block text-gp-muted">Folio autorización</span>
                    <span className="mt-0.5 block font-mono text-sm font-bold text-gp-secondary">
                      {upcResult.numAutorizacion || 'Sin autorización'}
                    </span>
                  </div>
                  <div className="rounded-xl border border-slate-100 p-3">
                    <span className="block text-gp-muted">Última actualización</span>
                    <span className="mt-0.5 block text-sm font-semibold text-gp-primary">
                      {formatFecha(upcResult.fechaActualizacion)}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <EmptyState
                icon={ShieldCheck}
                title="Consulta el estado de cualquier pago"
                description="Ingresa un folio UPC alfanumérico de 10 caracteres para verificar su estado en tiempo real."
              />
            )}
          </div>
        )}

        {/* TAB 4: NACIONALIDADES */}
        {tab === 'nacionalidades' && (
          <div className="space-y-4">
            <div className="grid gap-4 lg:grid-cols-2">
              <form
                noValidate
                onSubmit={handleBuscarNacionalidad}
                className="card-surface p-4"
              >
                <div className="flex flex-col gap-2.5 sm:flex-row">
                  <input
                    className={`input-field flex-1 ${nacQueryError ? 'input-field-error' : ''}`}
                    value={nacQuery}
                    onChange={(e) => {
                      setNacQuery(e.target.value);
                      if (nacQueryError) setNacQueryError(null);
                    }}
                    placeholder="Buscar por código ISO, país o gentilicio (ej. MEX)..."
                  />
                  <button type="submit" disabled={loadingNac} className="btn-primary shrink-0">
                    Buscar
                  </button>
                </div>
                <FieldError message={nacQueryError} />
              </form>

              <div className="card-surface flex items-start gap-2.5 p-4">
                <div className="relative flex-1">
                  <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
                  <input
                    className="input-field pl-10"
                    value={nacFilter}
                    onChange={(e) => setNacFilter(e.target.value)}
                    placeholder={`Filtrar listado (${nacionalidades.length} registros)...`}
                  />
                </div>
                <button
                  type="button"
                  onClick={cargarNacionalidades}
                  disabled={loadingNac}
                  className="btn-secondary shrink-0"
                >
                  <RefreshCw className={`h-4 w-4 ${loadingNac ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>

            {nacResult && (
              <div className="card-surface flex items-center justify-between gap-3 border-emerald-200 bg-emerald-50/50 p-4 text-sm">
                <div>
                  Coincidencia encontrada: <strong>{nacResult.gentilicio}</strong> ({nacResult.pais}{' '}
                  · ISO <span className="font-mono font-bold">{nacResult.claveIso}</span>)
                </div>
                <button
                  type="button"
                  onClick={() => setNacResult(null)}
                  className="btn-secondary px-2.5 py-1 text-xs"
                >
                  Cerrar
                </button>
              </div>
            )}

            {loadingNac ? (
              <SkeletonBlock className="h-48" />
            ) : nacionalidadesFiltradas.length === 0 ? (
              <EmptyState
                icon={Globe}
                title="Sin nacionalidades que coincidan"
                description="Intenta con otro término o actualiza el catálogo."
              />
            ) : (
              <div className="card-surface grid gap-2.5 p-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                {nacionalidadesFiltradas.map((n) => (
                  <div
                    key={n.idNacionalidad}
                    className="flex items-center gap-2.5 rounded-xl border border-slate-100 bg-slate-50/80 px-3 py-2.5 text-xs"
                  >
                    <span className="rounded-md bg-sky-100/80 px-2 py-0.5 font-mono font-bold text-gp-secondary">
                      {n.claveIso}
                    </span>
                    <div className="min-w-0">
                      <p className="truncate font-bold text-gp-primary">{n.gentilicio}</p>
                      <p className="truncate text-[11px] text-gp-muted">{n.pais}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      <ClienteEditModal
        open={editingCliente !== null}
        cliente={editingCliente}
        allowFullReplace
        onClose={() => setEditingCliente(null)}
        onSaved={() => {
          void buscarClientes();
        }}
      />
    </div>
  );
};
