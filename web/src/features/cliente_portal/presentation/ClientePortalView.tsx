import React, { useState } from 'react';
import {
  ChevronRight,
  Clock,
  CreditCard,
  History,
  LogOut,
  RefreshCw,
  ShieldCheck,
  Store,
  User,
} from 'lucide-react';
import { useToast } from '../../../core/components/ToastContext';
import { useSession } from '../../../core/session/SessionContext';
import { formatCountdown, maskCuenta } from '../../../core/utils/formatters';
import { ProductItemDto } from '../../catalogo_productos/data/productoRepository';
import { CatalogoTab } from '../../catalogo_productos/presentation/CatalogoTab';
import { PerfilTab } from '../../clientes/presentation/PerfilTab';
import { CuentaTab } from '../../cuentas/presentation/CuentaTab';
import { HistorialTab } from '../../pagos/presentation/HistorialTab';
import { PagoFlowModal } from '../../pagos/presentation/PagoFlowModal';

type PortalTab = 'cuenta' | 'catalogo' | 'historial' | 'perfil';

const PORTAL_TAB_KEY = 'gestopago_portal_tab';

const NAV_ITEMS: Array<{
  id: PortalTab;
  label: string;
  shortLabel: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
}> = [
  {
    id: 'cuenta',
    label: 'Mi Cuenta',
    shortLabel: 'Cuenta',
    description: 'Saldo y actividad reciente',
    icon: CreditCard,
  },
  {
    id: 'catalogo',
    label: 'Pagar Servicios',
    shortLabel: 'Catálogo',
    description: 'Servicios, recargas y pines',
    icon: Store,
  },
  {
    id: 'historial',
    label: 'Historial',
    shortLabel: 'Historial',
    description: 'Comprobantes y estatus UPC',
    icon: History,
  },
  {
    id: 'perfil',
    label: 'Mi Perfil',
    shortLabel: 'Perfil',
    description: 'Datos personales y domicilio',
    icon: User,
  },
];

function getInitialTab(): PortalTab {
  try {
    const saved = window.sessionStorage.getItem(PORTAL_TAB_KEY);
    if (saved === 'cuenta' || saved === 'catalogo' || saved === 'historial' || saved === 'perfil') {
      return saved;
    }
  } catch {
    // Ignore storage errors
  }
  return 'cuenta';
}

function getInitials(name?: string | null): string {
  if (!name) return 'GP';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'GP';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

export const ClientePortalView: React.FC = () => {
  const { sesion, cliente, cuentaActiva, remainingSeconds, logout, renovarSesionRemota } =
    useSession();
  const { notify } = useToast();

  const [tab, setTab] = useState<PortalTab>(getInitialTab);
  const [selectedProducto, setSelectedProducto] = useState<ProductItemDto | null>(null);
  const [historyRefreshKey, setHistoryRefreshKey] = useState(0);
  const [renewingSession, setRenewingSession] = useState(false);

  const changeTab = (next: PortalTab) => {
    setTab(next);
    try {
      window.sessionStorage.setItem(PORTAL_TAB_KEY, next);
    } catch {
      // Ignore storage errors
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleRenewClick = async () => {
    setRenewingSession(true);
    try {
      await renovarSesionRemota();
      notify({
        title: 'Sesión renovada',
        description: 'El contador de seguridad se restableció a 5 minutos.',
        variant: 'info',
      });
    } finally {
      setRenewingSession(false);
    }
  };

  const displayName =
    cliente?.nombreCompleto || sesion?.nombreCliente || sesion?.username || 'Cliente';
  const maxSecs = Math.max(60, (sesion?.minutosInactividadMaximos ?? 5) * 60);
  const isLowSession = remainingSeconds <= 60;
  const sessionPct = Math.min(100, Math.max(0, Math.round((remainingSeconds / maxSecs) * 100)));

  return (
    <div className="min-h-screen bg-gp-bg pb-20 lg:flex lg:pb-0">
      {/* Desktop Sidebar */}
      <aside className="hidden border-r border-gp-border bg-white lg:fixed lg:inset-y-0 lg:flex lg:w-[272px] lg:flex-col">
        {/* Brand Header */}
        <div className="flex items-center gap-3 border-b border-slate-100 px-6 py-5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-950 text-sky-400 shadow-sm">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <p className="font-display text-base font-extrabold tracking-tight text-gp-primary">
              Gesto<span className="text-gp-secondary">Pagos</span>
            </p>
            <p className="text-[11px] font-medium text-gp-muted">Banca Digital Web</p>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 space-y-1.5 px-3.5 py-5" aria-label="Navegación principal">
          <p className="mb-2 px-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">
            Menú Principal
          </p>
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const active = tab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => changeTab(item.id)}
                className={`group flex w-full items-center justify-between gap-3 rounded-2xl px-3.5 py-3 text-left transition duration-150 ${
                  active
                    ? 'bg-slate-950 text-white shadow-md shadow-slate-900/10'
                    : 'text-slate-600 hover:bg-slate-100/90 hover:text-gp-primary'
                }`}
              >
                <div className="flex min-w-0 items-center gap-3">
                  <div
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-xl transition ${
                      active
                        ? 'bg-sky-500/20 text-sky-300'
                        : 'bg-slate-100 text-slate-500 group-hover:bg-white group-hover:text-gp-secondary'
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold">{item.label}</p>
                    <p
                      className={`truncate text-[11px] ${
                        active ? 'text-slate-400' : 'text-slate-400 group-hover:text-slate-500'
                      }`}
                    >
                      {item.description}
                    </p>
                  </div>
                </div>
                <ChevronRight
                  className={`h-4 w-4 shrink-0 transition ${
                    active ? 'text-sky-400' : 'text-transparent group-hover:text-slate-400'
                  }`}
                />
              </button>
            );
          })}
        </nav>

        {/* Sidebar Footer: User Mini Card + Logout */}
        <div className="space-y-3 border-t border-slate-100 bg-slate-50/60 p-4">
          <div className="flex items-center gap-3 rounded-2xl border border-slate-200/80 bg-white p-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-sky-600 to-slate-900 text-xs font-extrabold text-white">
              {getInitials(displayName)}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-bold text-gp-primary">{displayName}</p>
              <p className="truncate font-mono text-[11px] text-gp-muted">
                {maskCuenta(cuentaActiva?.numeroCuenta)}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => void logout()}
            className="btn-danger-soft w-full py-2 text-xs"
          >
            <LogOut className="h-3.5 w-3.5" />
            Cerrar sesión
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex min-w-0 flex-1 flex-col lg:pl-[272px]">
        {/* Sticky Glass Top Bar */}
        <header className="sticky top-0 z-30 border-b border-gp-border/80 bg-white/90 px-4 py-3 backdrop-blur-md sm:px-6">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-3">
            <div className="flex min-w-0 items-center gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-950 text-sky-400 lg:hidden">
                <ShieldCheck className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-bold text-gp-primary">Hola, {displayName}</p>
                <p className="truncate text-xs text-gp-muted">
                  Cuenta activa:{' '}
                  <span className="font-mono font-medium text-slate-700">
                    {maskCuenta(cuentaActiva?.numeroCuenta)}
                  </span>
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-2">
              {/* Session Timer Pill with Progress Indicator */}
              <button
                type="button"
                disabled={renewingSession}
                onClick={handleRenewClick}
                title="Haz clic para validar y renovar tu sesión activa de 5 minutos"
                className={`group relative overflow-hidden rounded-xl border px-3 py-1.5 text-xs font-semibold transition ${
                  isLowSession
                    ? 'border-amber-300 bg-amber-50 text-amber-900 shadow-sm'
                    : 'border-gp-border bg-slate-50/90 text-gp-primary hover:border-sky-300 hover:bg-sky-50/50'
                }`}
              >
                <span className="relative z-10 flex items-center gap-1.5">
                  <Clock
                    className={`h-3.5 w-3.5 ${
                      isLowSession ? 'animate-pulse text-amber-600' : 'text-gp-secondary'
                    }`}
                  />
                  <span className="hidden sm:inline">Sesión:</span>
                  <span className="font-mono tabular-nums">
                    {formatCountdown(remainingSeconds)}
                  </span>
                  <RefreshCw
                    className={`ml-0.5 h-3 w-3 text-slate-400 transition group-hover:text-gp-secondary ${
                      renewingSession ? 'animate-spin text-gp-secondary' : ''
                    }`}
                  />
                </span>
                <span
                  className={`absolute bottom-0 left-0 h-0.5 transition-all duration-700 ${
                    isLowSession ? 'bg-amber-500' : 'bg-sky-500/70'
                  }`}
                  style={{ width: `${sessionPct}%` }}
                />
              </button>

              <button
                type="button"
                onClick={() => void logout()}
                className="btn-danger-soft px-2.5 py-1.5 text-xs lg:hidden"
                aria-label="Cerrar sesión"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">Salir</span>
              </button>
            </div>
          </div>
        </header>

        {/* Active Section Body */}
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:py-8">
          {tab === 'cuenta' && (
            <CuentaTab
              refreshSignal={historyRefreshKey}
              onGoCatalog={() => changeTab('catalogo')}
              onGoHistory={() => changeTab('historial')}
            />
          )}
          {tab === 'catalogo' && (
            <CatalogoTab onSelectProducto={(prod) => setSelectedProducto(prod)} />
          )}
          {tab === 'historial' && <HistorialTab key={historyRefreshKey} />}
          {tab === 'perfil' && <PerfilTab />}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav
        aria-label="Navegación móvil"
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-4 border-t border-gp-border bg-white/95 px-2 pb-2 pt-1.5 backdrop-blur-md lg:hidden"
      >
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const active = tab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => changeTab(item.id)}
              className={`relative flex flex-col items-center gap-0.5 rounded-xl py-1.5 text-[11px] font-semibold transition ${
                active ? 'bg-sky-50/80 text-gp-secondary' : 'text-gp-muted hover:text-gp-primary'
              }`}
            >
              {active && (
                <span className="absolute top-0 h-0.5 w-6 rounded-full bg-gp-secondary" />
              )}
              <Icon className="h-4 w-4" />
              <span>{item.shortLabel}</span>
            </button>
          );
        })}
      </nav>

      {/* Guided 3-Phase Payment Flow Modal */}
      <PagoFlowModal
        open={selectedProducto !== null}
        producto={selectedProducto}
        onClose={() => setSelectedProducto(null)}
        onPaymentCompleted={() => setHistoryRefreshKey((n) => n + 1)}
      />
    </div>
  );
};
