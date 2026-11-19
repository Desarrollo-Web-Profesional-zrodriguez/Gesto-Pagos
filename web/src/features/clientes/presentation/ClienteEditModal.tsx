import React, { useEffect, useState } from 'react';
import { Loader2, Save, X } from 'lucide-react';
import { ErrorBanner } from '../../../core/components/UiPrimitives';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import { BackendValidators } from '../../../core/validation/backendValidators';
import {
  NacionalidadDto,
  nacionalidadRepository,
} from '../../catalogos_sistema/data/nacionalidadRepository';
import { ClienteDto } from '../data/clienteModels';
import { clienteRepository } from '../data/clienteRepository';

interface ClienteEditModalProps {
  open: boolean;
  cliente: ClienteDto | null;
  onClose: () => void;
  onSaved: (updated: ClienteDto) => void;
}

export const ClienteEditModal: React.FC<ClienteEditModalProps> = ({
  open,
  cliente,
  onClose,
  onSaved,
}) => {
  const [mode, setMode] = useState<'patch' | 'put'>('patch');
  const [nacionalidades, setNacionalidades] = useState<NacionalidadDto[]>([]);

  const [nombre, setNombre] = useState('');
  const [segundoNombre, setSegundoNombre] = useState('');
  const [apellidoPaterno, setApellidoPaterno] = useState('');
  const [apellidoMaterno, setApellidoMaterno] = useState('');
  const [fechaNacimiento, setFechaNacimiento] = useState('');
  const [curp, setCurp] = useState('');
  const [rfc, setRfc] = useState('');
  const [sexo, setSexo] = useState('Masculino');
  const [idNacionalidad, setIdNacionalidad] = useState<number | ''>('');
  const [estadoCivil, setEstadoCivil] = useState('Soltero');
  const [correoElectronico, setCorreoElectronico] = useState('');
  const [telefonoMovil, setTelefonoMovil] = useState('');
  const [telefonoAlternativo, setTelefonoAlternativo] = useState('');
  const [calle, setCalle] = useState('');
  const [numeroExterior, setNumeroExterior] = useState('');
  const [numeroInterior, setNumeroInterior] = useState('');
  const [colonia, setColonia] = useState('');
  const [municipio, setMunicipio] = useState('');
  const [estado, setEstado] = useState('');
  const [codigoPostal, setCodigoPostal] = useState('');
  const [pais, setPais] = useState('Mexico');
  const [ocupacion, setOcupacion] = useState('');
  const [empresa, setEmpresa] = useState('');
  const [ingresoMensual, setIngresoMensual] = useState('');
  const [saldoInicial, setSaldoInicial] = useState('0.00');
  const [password, setPassword] = useState('');

  const [validationError, setValidationError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open || !cliente) return;
    setNombre(cliente.nombre || '');
    setSegundoNombre(cliente.segundoNombre || '');
    setApellidoPaterno(cliente.apellidoPaterno || '');
    setApellidoMaterno(cliente.apellidoMaterno || '');
    setFechaNacimiento(cliente.fechaNacimiento || '1995-05-20');
    setCurp(cliente.curp || '');
    setRfc(cliente.rfc || '');
    setSexo(cliente.sexo || 'Masculino');
    setIdNacionalidad(cliente.idNacionalidad ?? '');
    setEstadoCivil(cliente.estadoCivil || 'Soltero');
    setCorreoElectronico(cliente.correoElectronico || '');
    setTelefonoMovil(cliente.telefonoMovil || '');
    setTelefonoAlternativo(cliente.telefonoAlternativo || '');
    const dom = cliente.domicilios?.[0];
    setCalle(dom?.calle || '');
    setNumeroExterior(dom?.numeroExterior || '');
    setNumeroInterior(dom?.numeroInterior || '');
    setColonia(dom?.colonia || '');
    setMunicipio(dom?.municipio || '');
    setEstado(dom?.estado || '');
    setCodigoPostal(dom?.codigoPostal || '');
    setPais(dom?.pais || 'Mexico');
    setOcupacion(cliente.ocupacion || '');
    setEmpresa(cliente.empresa || '');
    setIngresoMensual(
      StrictDecimal.fromNumber(cliente.ingresoMensual ?? 10000).toStrictLiteral()
    );
    const disp = cliente.cuentas?.[0]?.saldo?.saldoDisponible ?? 0;
    setSaldoInicial(StrictDecimal.fromNumber(disp).toStrictLiteral());
    setPassword('');
    setValidationError(null);
    setApiError(null);

    nacionalidadRepository
      .listarTodas()
      .then(setNacionalidades)
      .catch(() => {
        // Non-blocking in modal
      });
  }, [open, cliente]);

  if (!open || !cliente) return null;

  const identificador = cliente.curp || cliente.correoElectronico || cliente.rfc;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);
    setApiError(null);

    const baseErr =
      BackendValidators.soloLetrasRequerido(nombre, 'Nombre') ||
      BackendValidators.soloLetrasOpcional(segundoNombre, 'Segundo nombre') ||
      BackendValidators.soloLetrasRequerido(apellidoPaterno, 'Apellido paterno') ||
      BackendValidators.soloLetrasRequerido(apellidoMaterno, 'Apellido materno') ||
      BackendValidators.fechaNacimientoMayorEdad(fechaNacimiento) ||
      BackendValidators.email(correoElectronico) ||
      BackendValidators.telefono(telefonoMovil) ||
      BackendValidators.telefono(telefonoAlternativo, false) ||
      BackendValidators.calleOColonia(calle, 'Calle', 150) ||
      BackendValidators.numeroDomicilio(numeroExterior, 'Número exterior', true) ||
      BackendValidators.numeroDomicilio(numeroInterior, 'Número interior', false) ||
      BackendValidators.calleOColonia(colonia, 'Colonia', 100) ||
      BackendValidators.soloLetrasRequerido(municipio, 'Municipio', 2, 100) ||
      BackendValidators.soloLetrasRequerido(estado, 'Estado', 2, 100) ||
      BackendValidators.codigoPostal(codigoPostal) ||
      BackendValidators.soloLetrasRequerido(pais, 'País', 2, 50) ||
      BackendValidators.soloLetrasRequerido(ocupacion, 'Ocupación', 3, 100) ||
      BackendValidators.empresa(empresa) ||
      BackendValidators.montoDecimal(ingresoMensual, 'Ingreso mensual', 0.01);

    if (baseErr) {
      setValidationError(baseErr);
      return;
    }

    if (mode === 'put') {
      const putErr =
        BackendValidators.curp(curp) ||
        BackendValidators.rfc(rfc) ||
        BackendValidators.montoDecimal(saldoInicial, 'Saldo inicial', 0) ||
        BackendValidators.passwordSegura(password);
      if (putErr) {
        setValidationError(putErr);
        return;
      }
    }

    setSaving(true);
    try {
      const selectedNac = nacionalidades.find((n) => n.idNacionalidad === idNacionalidad);
      let updated: ClienteDto;
      if (mode === 'patch') {
        updated = await clienteRepository.actualizarParcial({
          identificador,
          nombre,
          segundoNombre,
          apellidoPaterno,
          apellidoMaterno,
          fechaNacimiento,
          sexo,
          idNacionalidad: typeof idNacionalidad === 'number' ? idNacionalidad : undefined,
          nacionalidad: selectedNac?.gentilicio ?? cliente.nacionalidad,
          estadoCivil,
          correoElectronico,
          telefonoMovil,
          telefonoAlternativo,
          domicilio: {
            calle,
            numeroExterior,
            numeroInterior,
            colonia,
            municipio,
            estado,
            codigoPostal,
            pais,
          },
          ocupacion,
          empresa,
          ingresoMensual: StrictDecimal.parse(ingresoMensual),
        });
      } else {
        updated = await clienteRepository.actualizarCompleto({
          identificador,
          nombre,
          segundoNombre,
          apellidoPaterno,
          apellidoMaterno,
          fechaNacimiento,
          curp,
          rfc,
          sexo,
          idNacionalidad: typeof idNacionalidad === 'number' ? idNacionalidad : undefined,
          nacionalidad: selectedNac?.gentilicio ?? cliente.nacionalidad,
          estadoCivil,
          correoElectronico,
          telefonoMovil,
          telefonoAlternativo,
          domicilio: {
            calle,
            numeroExterior,
            numeroInterior,
            colonia,
            municipio,
            estado,
            codigoPostal,
            pais,
          },
          ocupacion,
          empresa,
          ingresoMensual: StrictDecimal.parse(ingresoMensual),
          saldoInicial: StrictDecimal.parse(saldoInicial),
          password,
        });
      }
      onSaved(updated);
      onClose();
    } catch (err) {
      setApiError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
      <div className="flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl border border-gp-border bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-gp-border px-6 py-4">
          <div>
            <h3 className="text-lg font-bold text-gp-primary">Editar Datos del Cliente</h3>
            <p className="text-xs text-gp-muted">Identificador: {identificador}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1.5 text-gp-muted hover:bg-slate-100"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 space-y-4 overflow-y-auto px-6 py-5">
          <div className="grid grid-cols-2 gap-2 rounded-xl bg-slate-100 p-1">
            <button
              type="button"
              onClick={() => setMode('patch')}
              className={`rounded-lg py-2 text-xs font-semibold ${
                mode === 'patch' ? 'bg-white text-gp-primary shadow-sm' : 'text-gp-muted'
              }`}
            >
              Actualización Parcial (PATCH)
            </button>
            <button
              type="button"
              onClick={() => setMode('put')}
              className={`rounded-lg py-2 text-xs font-semibold ${
                mode === 'put' ? 'bg-white text-gp-primary shadow-sm' : 'text-gp-muted'
              }`}
            >
              Reemplazo Completo (PUT)
            </button>
          </div>

          <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} />
          {validationError && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs font-medium text-amber-900">
              {validationError}
            </div>
          )}

          <div className="grid gap-3.5 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Nombre *</label>
              <input className="input-field" value={nombre} onChange={(e) => setNombre(e.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Segundo nombre</label>
              <input
                className="input-field"
                value={segundoNombre}
                onChange={(e) => setSegundoNombre(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">
                Apellido paterno *
              </label>
              <input
                className="input-field"
                value={apellidoPaterno}
                onChange={(e) => setApellidoPaterno(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">
                Apellido materno *
              </label>
              <input
                className="input-field"
                value={apellidoMaterno}
                onChange={(e) => setApellidoMaterno(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Teléfono móvil *</label>
              <input
                className="input-field"
                maxLength={10}
                value={telefonoMovil}
                onChange={(e) => setTelefonoMovil(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Correo *</label>
              <input
                type="email"
                className="input-field"
                value={correoElectronico}
                onChange={(e) => setCorreoElectronico(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Ocupación *</label>
              <input
                className="input-field"
                value={ocupacion}
                onChange={(e) => setOcupacion(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Empresa *</label>
              <input
                className="input-field"
                value={empresa}
                onChange={(e) => setEmpresa(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">
                Ingreso mensual (2 decimales) *
              </label>
              <input
                className="input-field font-mono"
                value={ingresoMensual}
                onChange={(e) => setIngresoMensual(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Nacionalidad</label>
              <select
                className="input-field"
                value={idNacionalidad}
                onChange={(e) => setIdNacionalidad(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">Conservar actual ({cliente.nacionalidad || 'N/A'})</option>
                {nacionalidades.map((n) => (
                  <option key={n.idNacionalidad} value={n.idNacionalidad}>
                    {n.gentilicio} ({n.pais})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Calle *</label>
              <input className="input-field" value={calle} onChange={(e) => setCalle(e.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">
                Número exterior *
              </label>
              <input
                className="input-field"
                value={numeroExterior}
                onChange={(e) => setNumeroExterior(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Colonia *</label>
              <input
                className="input-field"
                value={colonia}
                onChange={(e) => setColonia(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Municipio *</label>
              <input
                className="input-field"
                value={municipio}
                onChange={(e) => setMunicipio(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">Estado *</label>
              <input
                className="input-field"
                value={estado}
                onChange={(e) => setEstado(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gp-muted">
                Código postal *
              </label>
              <input
                className="input-field"
                maxLength={5}
                value={codigoPostal}
                onChange={(e) => setCodigoPostal(e.target.value)}
              />
            </div>

            {mode === 'put' && (
              <>
                <div>
                  <label className="mb-1 block text-xs font-semibold text-gp-muted">CURP *</label>
                  <input
                    className="input-field uppercase"
                    maxLength={18}
                    value={curp}
                    onChange={(e) => setCurp(e.target.value.toUpperCase())}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-semibold text-gp-muted">RFC *</label>
                  <input
                    className="input-field uppercase"
                    maxLength={13}
                    value={rfc}
                    onChange={(e) => setRfc(e.target.value.toUpperCase())}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-semibold text-gp-muted">
                    Saldo inicial / referencia PUT *
                  </label>
                  <input
                    className="input-field font-mono"
                    value={saldoInicial}
                    onChange={(e) => setSaldoInicial(e.target.value)}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-semibold text-gp-muted">
                    Nueva contraseña segura (requerida en PUT) *
                  </label>
                  <input
                    type="password"
                    className="input-field"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </div>
              </>
            )}
          </div>

          <div className="flex justify-end gap-2 border-t border-gp-border pt-4">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancelar
            </button>
            <button type="submit" disabled={saving} className="btn-primary">
              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Guardar cambios ({mode.toUpperCase()})
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
