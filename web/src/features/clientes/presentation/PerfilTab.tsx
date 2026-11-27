import React, { useState } from 'react';
import {
  Briefcase,
  Edit3,
  Home,
  LogOut,
  Mail,
  Phone,
  ShieldCheck,
  User,
} from 'lucide-react';
import { CopyChip, EmptyState, StatusBadge } from '../../../core/components/UiPrimitives';
import { useSession } from '../../../core/session/SessionContext';
import { formatFecha, formatMxn } from '../../../core/utils/formatters';
import { ClienteEditModal } from './ClienteEditModal';

function getInitials(name?: string | null): string {
  if (!name) return 'GP';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'GP';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

export const PerfilTab: React.FC = () => {
  const { cliente, refreshPerfilYSaldo, logout } = useSession();
  const [editOpen, setEditOpen] = useState(false);

  if (!cliente) {
    return (
      <EmptyState
        icon={User}
        title="No se pudo cargar tu expediente"
        description="Intenta recargar la sesión para sincronizar tus datos personales."
      />
    );
  }

  const dom = cliente.domicilios?.[0];
  const fullName =
    cliente.nombreCompleto ||
    [cliente.nombre, cliente.segundoNombre, cliente.apellidoPaterno, cliente.apellidoMaterno]
      .filter(Boolean)
      .join(' ');

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-extrabold tracking-tight text-gp-primary">Mi Perfil</h2>
          <p className="mt-0.5 text-sm text-gp-muted">
            Expediente personal, fiscal, de contacto y domicilio verificado
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button type="button" onClick={() => setEditOpen(true)} className="btn-primary">
            <Edit3 className="h-4 w-4" />
            Editar expediente
          </button>
          <button type="button" onClick={() => void logout()} className="btn-danger-soft">
            <LogOut className="h-4 w-4" />
            Cerrar sesión
          </button>
        </div>
      </div>

      {/* Executive Profile Banner */}
      <div className="card-surface overflow-hidden">
        <div className="bg-gradient-to-r from-slate-950 via-slate-900 to-sky-950 px-6 py-5 text-white">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-sky-500/20 font-display text-lg font-extrabold text-sky-300 ring-1 ring-inset ring-sky-400/35">
                {getInitials(fullName)}
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2.5">
                  <h3 className="font-display text-lg font-extrabold text-white sm:text-xl">
                    {fullName}
                  </h3>
                  <StatusBadge estatus={cliente.activo === false ? 'INACTIVO' : 'ACTIVA'} inverse />
                </div>
                <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-300">
                  <span className="inline-flex items-center gap-1.5">
                    <Mail className="h-3.5 w-3.5 text-sky-400" />
                    {cliente.correoElectronico}
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    <Phone className="h-3.5 w-3.5 text-emerald-400" />
                    {cliente.telefonoMovil}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs text-slate-300">
              <ShieldCheck className="h-4 w-4 text-emerald-400" />
              <span>
                Alta: <strong className="text-white">{formatFecha(cliente.fechaCreacion)}</strong>
              </span>
            </div>
          </div>
        </div>

        {/* Fiscal Quick Strip */}
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-slate-100 bg-slate-50/80 px-6 py-3 text-xs">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-gp-muted">CURP:</span>
            <CopyChip value={cliente.curp} toastTitle="CURP copiada" />
          </div>
          <div className="flex items-center gap-2">
            <span className="font-semibold text-gp-muted">RFC:</span>
            <CopyChip value={cliente.rfc} toastTitle="RFC copiado" />
          </div>
          {cliente.nacionalidad && (
            <div className="flex items-center gap-1.5">
              <span className="font-semibold text-gp-muted">Nacionalidad:</span>
              <span className="font-medium text-gp-primary">{cliente.nacionalidad}</span>
            </div>
          )}
        </div>
      </div>

      {/* Detail Cards Grid */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Personal & Contact Card */}
        <div className="card-surface space-y-4 p-5 sm:p-6">
          <div className="flex items-center gap-3 border-b border-slate-100 pb-3.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
              <User className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-gp-primary">Identidad y Contacto</h3>
              <p className="text-xs text-gp-muted">Datos personales y medios de notificación</p>
            </div>
          </div>

          <dl className="space-y-2.5 text-sm">
            {[
              ['Nombre(s)', [cliente.nombre, cliente.segundoNombre].filter(Boolean).join(' ')],
              ['Apellidos', `${cliente.apellidoPaterno} ${cliente.apellidoMaterno}`],
              [
                'Fecha de nacimiento',
                cliente.fechaNacimiento
                  ? `${cliente.fechaNacimiento}${cliente.edad ? ` (${cliente.edad} años)` : ''}`
                  : '-',
              ],
              ['Sexo', cliente.sexo || '-'],
              ['Estado civil', cliente.estadoCivil || '-'],
              ['Nacionalidad', cliente.nacionalidad || '-'],
              ['Teléfono móvil', cliente.telefonoMovil],
              ['Teléfono alternativo', cliente.telefonoAlternativo || 'No registrado'],
              ['Correo electrónico', cliente.correoElectronico],
            ].map(([label, val]) => (
              <div
                key={label}
                className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5 last:border-0"
              >
                <dt className="text-xs font-medium text-gp-muted">{label}</dt>
                <dd className="text-right text-xs font-semibold text-gp-primary sm:text-sm">
                  {val}
                </dd>
              </div>
            ))}
          </dl>
        </div>

        {/* Right Stack: Domicilio + Laboral */}
        <div className="space-y-6">
          <div className="card-surface space-y-4 p-5 sm:p-6">
            <div className="flex items-center gap-3 border-b border-slate-100 pb-3.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                <Home className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-gp-primary">Domicilio Registrado</h3>
                <p className="text-xs text-gp-muted">Dirección residencial del titular</p>
              </div>
            </div>

            {dom ? (
              <div className="space-y-2 text-sm">
                <p className="font-semibold text-gp-primary">
                  {dom.calle} {dom.numeroExterior}
                  {dom.numeroInterior ? `, Int. ${dom.numeroInterior}` : ''}
                </p>
                <div className="grid grid-cols-2 gap-2 rounded-2xl bg-slate-50 p-3.5 text-xs">
                  <div>
                    <span className="block text-gp-muted">Colonia</span>
                    <span className="font-semibold text-gp-primary">{dom.colonia}</span>
                  </div>
                  <div>
                    <span className="block text-gp-muted">Municipio / Alcaldía</span>
                    <span className="font-semibold text-gp-primary">{dom.municipio}</span>
                  </div>
                  <div>
                    <span className="block text-gp-muted">Estado</span>
                    <span className="font-semibold text-gp-primary">{dom.estado}</span>
                  </div>
                  <div>
                    <span className="block text-gp-muted">Código Postal / País</span>
                    <span className="font-mono font-semibold text-gp-primary">
                      {dom.codigoPostal} · {dom.pais}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <p className="text-sm text-gp-muted">Sin domicilio registrado.</p>
            )}
          </div>

          <div className="card-surface space-y-4 p-5 sm:p-6">
            <div className="flex items-center gap-3 border-b border-slate-100 pb-3.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                <Briefcase className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-gp-primary">Actividad Económica</h3>
                <p className="text-xs text-gp-muted">Ocupación e ingresos declarados</p>
              </div>
            </div>

            <dl className="space-y-2.5 text-sm">
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <dt className="text-xs font-medium text-gp-muted">Ocupación</dt>
                <dd className="text-right text-xs font-semibold text-gp-primary sm:text-sm">
                  {cliente.ocupacion}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-1.5">
                <dt className="text-xs font-medium text-gp-muted">Empresa</dt>
                <dd className="text-right text-xs font-semibold text-gp-primary sm:text-sm">
                  {cliente.empresa}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4 pt-1">
                <dt className="text-xs font-medium text-gp-muted">Ingreso mensual</dt>
                <dd className="font-mono text-sm font-extrabold text-emerald-700 tabular-nums">
                  {formatMxn(cliente.ingresoMensual)}
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </div>

      <ClienteEditModal
        open={editOpen}
        cliente={cliente}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          void refreshPerfilYSaldo();
        }}
      />
    </div>
  );
};
