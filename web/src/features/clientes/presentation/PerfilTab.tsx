import React, { useState } from 'react';
import { Briefcase, Edit3, Home, LogOut, User } from 'lucide-react';
import { StatusBadge } from '../../../core/components/UiPrimitives';
import { useSession } from '../../../core/session/SessionContext';
import { formatMxn } from '../../../core/utils/formatters';
import { ClienteEditModal } from './ClienteEditModal';

export const PerfilTab: React.FC = () => {
  const { cliente, refreshPerfilYSaldo, logout } = useSession();
  const [editOpen, setEditOpen] = useState(false);

  if (!cliente) {
    return (
      <div className="card-surface p-8 text-center text-sm text-gp-muted">
        No se ha podido cargar el perfil del cliente autenticado.
      </div>
    );
  }

  const dom = cliente.domicilios?.[0];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gp-primary">Mi Perfil</h2>
          <p className="text-sm text-gp-muted">
            Información personal, domicilio y datos laborales registrados en PostgreSQL
          </p>
        </div>

        <div className="flex gap-2">
          <button type="button" onClick={() => setEditOpen(true)} className="btn-secondary">
            <Edit3 className="h-4 w-4" />
            Editar perfil
          </button>
          <button
            type="button"
            onClick={() => void logout()}
            className="btn-secondary border-red-200 text-red-700 hover:bg-red-50"
          >
            <LogOut className="h-4 w-4" />
            Cerrar sesión
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card-surface space-y-4 p-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                <User className="h-5 w-5" />
              </div>
              <h3 className="text-base font-bold text-gp-primary">Identidad y Contacto</h3>
            </div>
            <StatusBadge estatus={cliente.activo === false ? 'INACTIVO' : 'ACTIVA'} />
          </div>

          <dl className="space-y-2 text-sm">
            {[
              ['Nombre completo', cliente.nombreCompleto || `${cliente.nombre} ${cliente.apellidoPaterno}`],
              ['Correo electrónico', cliente.correoElectronico],
              ['Teléfono móvil', cliente.telefonoMovil],
              ['CURP', cliente.curp],
              ['RFC', cliente.rfc],
              ['Fecha de nacimiento', cliente.fechaNacimiento || '-'],
              ['Nacionalidad', cliente.nacionalidad || '-'],
              ['Estado civil', cliente.estadoCivil],
            ].map(([label, val]) => (
              <div key={label} className="flex justify-between border-b border-slate-100 py-1.5 last:border-0">
                <dt className="text-gp-muted">{label}</dt>
                <dd className="font-medium text-gp-primary">{val}</dd>
              </div>
            ))}
          </dl>
        </div>

        <div className="space-y-6">
          <div className="card-surface space-y-3 p-5">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                <Home className="h-5 w-5" />
              </div>
              <h3 className="text-base font-bold text-gp-primary">Domicilio</h3>
            </div>
            {dom ? (
              <p className="text-sm leading-relaxed text-gp-primary">
                {dom.calle} #{dom.numeroExterior}
                {dom.numeroInterior ? `, Int. ${dom.numeroInterior}` : ''}, Col. {dom.colonia},{' '}
                {dom.municipio}, {dom.estado}, C.P. {dom.codigoPostal}, {dom.pais}
              </p>
            ) : (
              <p className="text-sm text-gp-muted">Sin domicilio registrado.</p>
            )}
          </div>

          <div className="card-surface space-y-3 p-5">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-gp-secondary">
                <Briefcase className="h-5 w-5" />
              </div>
              <h3 className="text-base font-bold text-gp-primary">Actividad Económica</h3>
            </div>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-gp-muted">Ocupación</dt>
                <dd className="font-medium text-gp-primary">{cliente.ocupacion}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gp-muted">Empresa</dt>
                <dd className="font-medium text-gp-primary">{cliente.empresa}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gp-muted">Ingreso mensual</dt>
                <dd className="font-bold text-emerald-700">{formatMxn(cliente.ingresoMensual)}</dd>
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
