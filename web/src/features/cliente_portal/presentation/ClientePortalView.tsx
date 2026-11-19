import React, { useState } from 'react';
import {
  Clock,
  CreditCard,
  History,
  LogOut,
  RefreshCw,
  Server,
  ShieldCheck,
  Store,
  User,
} from 'lucide-react';
import { useSession } from '../../../core/session/SessionContext';
import { formatCountdown, maskCuenta } from '../../../core/utils/formatters';
import { ProductItemDto } from '../../catalogo_productos/data/productoRepository';
import { CatalogoTab } from '../../catalogo_productos/presentation/CatalogoTab';
import { PerfilTab } from '../../clientes/presentation/PerfilTab';
import { CuentaTab } from '../../cuentas/presentation/CuentaTab';
import { HistorialTab } from '../../pagos/presentation/HistorialTab';
import { PagoFlowModal } from '../../pagos/presentation/PagoFlowModal';

type PortalTab = 'cuenta' | 'catalogo' | 'historial' | 'perfil';

interface ClientePortalViewProps {
  onOpenServerConfig: () => void;
}

const NAV_ITEMS: Array<{ id: PortalTab; label: string; icon: React.ComponentType<{ className?: string }> }> = [
  { id: 'cuenta', label: 'Mi Cuenta', icon: CreditCard },
  { id: 'catalogo', label: 'Catálogo', icon: Store },
  { id: 'historial', label: 'Historial', icon: History },
  { id: 'perfil', label: 'Mi Perfil', icon: User },
];

export const ClientePortalView: React.FC<ClientePortalViewProps> = ({ onOpenServerConfig }) => {
  const {
    sesion,
    cliente,
    cuentaActiva,
    remainingSeconds,
    logout,
    renovarSesionRemota,
  } = useSession();

  const [tab, setTab] = useState<PortalTab>('cuenta');
  const [selectedProducto, setSelectedProducto] = useState<ProductItemDto | null>(null);
  const [historyRefreshKey, setHistoryRefreshKey] = useState(0);

  const isLowSession = remainingSeconds <= 60;

  return (
    <div className="min-h-screen bg-gp-bg pb-20 lg:flex lg:pb-0">
      {/* Desktop Sidebar */}
      <aside className="hidden border-r border-gp-border bg-white lg:fixed lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
        <div className="flex items-center gap-3 border-b border-gp-border px-6 py-5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gp-primary text-white">
            <ShieldCheck className="h-5 w-5 text-sky-400" />
          </div>
          <div>
            <p className="text-base font-extrabold text-gp-primary">GestoPagos</p>
            <p className="text-[11px] font-medium text-gp-muted">Portal Web Servicios</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1.5 px-3 py-5">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const active = tab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setTab(item.id)}
                className={`flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${
                  active
                    ? 'bg-gp-primary text-white shadow-sm'
                    : 'text-gp-muted hover:bg-slate-100 hover:text-gp-primary'
                }`}
              >
                <Icon className={`h-4 w-4 ${active ? 'text-sky-400' : ''}`} />
                {item.label}
              </button>
            );
          })}
        </nav>

        <div className="space-y-2 border-t border-gp-border p-4">
          <button
            type="button"
            onClick={onOpenServerConfig}
            className="btn-secondary w-full justify-start text-xs"
          >
            <Server className="h-4 w-4 text-gp-secondary" />
            Configurar backend
          </button>
          <button
            type="button"
            onClick={() => void logout()}
            className="btn-secondary w-full justify-start border-red-200 text-xs text-red-700 hover:bg-red-50"
          >
            <LogOut className="h-4 w-4" />
            Cerrar sesión
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
        {/* Top Bar */}
        <header className="sticky top-0 z-30 border-b border-gp-border bg-white/95 px-4 py-3 backdrop-blur-sm sm:px-6">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-bold text-gp-primary">
                Hola, {cliente?.nombreCompleto || sesion?.nombreCliente || sesion?.username}
              </p>
              <p className="text-xs text-gp-muted">
                Cuenta activa: {maskCuenta(cuentaActiva?.numeroCuenta)}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => void renovarSesionRemota()}
                title="Renovar sesión activa (GET /auth/validar-sesion)"
                className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-semibold transition ${
                  isLowSession
                    ? 'border-amber-300 bg-amber-50 text-amber-800'
                    : 'border-gp-border bg-slate-50 text-gp-primary hover:bg-slate-100'
                }`}
              >
                <Clock className="h-3.5 w-3.5 text-gp-secondary" />
                Sesión: {formatCountdown(remainingSeconds)}
                <RefreshCw className="ml-1 h-3 w-3 text-gp-muted" />
              </button>

              <button
                type="button"
                onClick={() => void logout()}
                className="btn-secondary px-3 py-1.5 text-xs text-red-700 lg:hidden"
                aria-label="Cerrar sesión"
              >
                <LogOut className="h-3.5 w-3.5" />
                Salir
              </button>
            </div>
          </div>
        </header>

        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6">
          {tab === 'cuenta' && (
            <CuentaTab
              onGoCatalog={() => setTab('catalogo')}
              onGoHistory={() => setTab('historial')}
            />
          )}
          {tab === 'catalogo' && (
            <CatalogoTab onSelectProducto={(prod) => setSelectedProducto(prod)} />
          )}
          {tab === 'historial' && <HistorialTab key={historyRefreshKey} />}
          {tab === 'perfil' && <PerfilTab />}
        </main>
      </div>

      {/* Mobile Bottom Nav */}
      <nav className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-4 border-t border-gp-border bg-white py-1.5 lg:hidden">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const active = tab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setTab(item.id)}
              className={`flex flex-col items-center gap-0.5 py-1 text-[11px] font-semibold ${
                active ? 'text-gp-secondary' : 'text-gp-muted'
              }`}
            >
              <Icon className="h-4 w-4" />
              {item.label}
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
