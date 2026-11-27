import React, { useEffect, useMemo, useState } from 'react';
import {
  Briefcase,
  Calendar,
  Eye,
  EyeOff,
  FileEdit,
  Globe,
  Info,
  Loader2,
  MapPin,
  Phone,
  RefreshCw,
  Save,
  ShieldAlert,
  User,
} from 'lucide-react';
import { CustomSelect, SelectOption } from '../../../core/components/CustomSelect';
import { ModalShell } from '../../../core/components/ModalShell';
import { useToast } from '../../../core/components/ToastContext';
import {
  ErrorBanner,
  FieldError,
  ValidationSummary,
} from '../../../core/components/UiPrimitives';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import {
  BackendValidators,
  collectValidationErrors,
  FieldErrors,
} from '../../../core/validation/backendValidators';
import {
  NacionalidadDto,
  nacionalidadRepository,
} from '../../catalogos_sistema/data/nacionalidadRepository';
import { ClienteDto } from '../data/clienteModels';
import { clienteRepository } from '../data/clienteRepository';

interface ClienteEditModalProps {
  open: boolean;
  cliente: ClienteDto | null;
  allowFullReplace?: boolean;
  onClose: () => void;
  onSaved: (updated: ClienteDto) => void;
}

const MESES_OPTIONS: SelectOption[] = [
  { value: '01', label: 'Enero' },
  { value: '02', label: 'Febrero' },
  { value: '03', label: 'Marzo' },
  { value: '04', label: 'Abril' },
  { value: '05', label: 'Mayo' },
  { value: '06', label: 'Junio' },
  { value: '07', label: 'Julio' },
  { value: '08', label: 'Agosto' },
  { value: '09', label: 'Septiembre' },
  { value: '10', label: 'Octubre' },
  { value: '11', label: 'Noviembre' },
  { value: '12', label: 'Diciembre' },
];

const SEXO_OPTIONS: SelectOption[] = [
  { value: 'Masculino', label: 'Masculino' },
  { value: 'Femenino', label: 'Femenino' },
];

const ESTADO_CIVIL_OPTIONS: SelectOption[] = [
  { value: 'Soltero', label: 'Soltero(a)' },
  { value: 'Casado', label: 'Casado(a)' },
  { value: 'Unión libre', label: 'Unión libre' },
  { value: 'Viudo', label: 'Viudo(a)' },
];

function diasEnMes(anio: number, mes: number): number {
  return new Date(anio, mes, 0).getDate();
}

export const ClienteEditModal: React.FC<ClienteEditModalProps> = ({
  open,
  cliente,
  allowFullReplace = false,
  onClose,
  onSaved,
}) => {
  const { notify } = useToast();

  const [mode, setMode] = useState<'patch' | 'put'>('patch');
  const [nacionalidades, setNacionalidades] = useState<NacionalidadDto[]>([]);
  const [showPassword, setShowPassword] = useState(false);

  const [nombre, setNombre] = useState('');
  const [segundoNombre, setSegundoNombre] = useState('');
  const [apellidoPaterno, setApellidoPaterno] = useState('');
  const [apellidoMaterno, setApellidoMaterno] = useState('');
  const [fechaNacimiento, setFechaNacimiento] = useState('1995-05-20');
  const [curp, setCurp] = useState('');
  const [rfc, setRfc] = useState('');
  const [sexo, setSexo] = useState('Masculino');
  const [idNacionalidad, setIdNacionalidad] = useState<string>('');
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

  const [errors, setErrors] = useState<FieldErrors>({});
  const [summaryMsg, setSummaryMsg] = useState<string | null>(null);
  const [summaryCount, setSummaryCount] = useState<number>(0);
  const [apiError, setApiError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);

  const clearField = (key: string) => {
    if (errors[key]) {
      setErrors((prev) => ({ ...prev, [key]: undefined }));
    }
    if (summaryMsg) {
      setSummaryMsg(null);
    }
  };

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
    setIdNacionalidad(cliente.idNacionalidad !== undefined ? String(cliente.idNacionalidad) : '');
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
    setShowPassword(false);
    setMode('patch');
    setErrors({});
    setSummaryMsg(null);
    setSummaryCount(0);
    setApiError(null);

    nacionalidadRepository
      .listarTodas()
      .then(setNacionalidades)
      .catch(() => {
        // Non-blocking in modal
      });
  }, [open, cliente]);

  const nacionalidadOptions = useMemo<SelectOption[]>(() => {
    const keepOption: SelectOption = {
      value: '',
      label: `Conservar actual (${cliente?.nacionalidad || 'N/A'})`,
    };
    const items = nacionalidades.map((n) => ({
      value: String(n.idNacionalidad),
      label: n.gentilicio,
      badge: n.pais,
    }));
    return [keepOption, ...items];
  }, [nacionalidades, cliente?.nacionalidad]);

  if (!cliente) return null;

  const identificador = cliente.curp || cliente.correoElectronico || cliente.rfc;
  const effectiveMode: 'patch' | 'put' = allowFullReplace ? mode : 'patch';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setApiError(null);

    const checks: Record<string, string | null> = {
      nombre: BackendValidators.soloLetrasRequerido(nombre, 'El nombre'),
      segundoNombre: BackendValidators.soloLetrasOpcional(segundoNombre, 'El segundo nombre'),
      apellidoPaterno: BackendValidators.soloLetrasRequerido(
        apellidoPaterno,
        'El apellido paterno'
      ),
      apellidoMaterno: BackendValidators.soloLetrasRequerido(
        apellidoMaterno,
        'El apellido materno'
      ),
      fechaNacimiento: BackendValidators.fechaNacimientoMayorEdad(fechaNacimiento),
      correoElectronico: BackendValidators.email(correoElectronico),
      telefonoMovil: BackendValidators.telefono(telefonoMovil),
      telefonoAlternativo: BackendValidators.telefono(telefonoAlternativo, false),
      ocupacion: BackendValidators.soloLetrasRequerido(ocupacion, 'La ocupación', 3, 100),
      empresa: BackendValidators.empresa(empresa),
      ingresoMensual: BackendValidators.montoDecimal(ingresoMensual, 'El ingreso mensual', 0.01),
      calle: BackendValidators.calleOColonia(calle, 'La calle', 150),
      numeroExterior: BackendValidators.numeroDomicilio(
        numeroExterior,
        'El número exterior',
        true
      ),
      numeroInterior: BackendValidators.numeroDomicilio(
        numeroInterior,
        'El número interior',
        false
      ),
      colonia: BackendValidators.calleOColonia(colonia, 'La colonia', 100),
      municipio: BackendValidators.soloLetrasRequerido(municipio, 'El municipio', 2, 100),
      estado: BackendValidators.soloLetrasRequerido(estado, 'El estado', 2, 100),
      codigoPostal: BackendValidators.codigoPostal(codigoPostal),
      pais: BackendValidators.soloLetrasRequerido(pais, 'El país', 2, 50),
    };

    if (effectiveMode === 'put') {
      checks.curp = BackendValidators.curp(curp);
      checks.rfc = BackendValidators.rfc(rfc);
      checks.saldoInicial = BackendValidators.montoDecimal(saldoInicial, 'El saldo inicial', 0);
      checks.password = BackendValidators.passwordSegura(password);
    }

    const res = collectValidationErrors(checks);
    if (!res.valid) {
      setErrors(res.errors);
      setSummaryMsg(res.firstError);
      setSummaryCount(res.errorCount);
      return;
    }

    setErrors({});
    setSummaryMsg(null);
    setSaving(true);
    try {
      const parsedIdNac = idNacionalidad ? Number(idNacionalidad) : undefined;
      const selectedNac = nacionalidades.find((n) => n.idNacionalidad === parsedIdNac);
      let updated: ClienteDto;
      if (effectiveMode === 'patch') {
        updated = await clienteRepository.actualizarParcial({
          identificador,
          nombre,
          segundoNombre,
          apellidoPaterno,
          apellidoMaterno,
          fechaNacimiento,
          sexo,
          idNacionalidad: parsedIdNac,
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
          idNacionalidad: parsedIdNac,
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
      notify({
        title: 'Expediente actualizado',
        description: 'Los cambios del cliente se guardaron correctamente.',
        variant: 'success',
      });
      onSaved(updated);
      onClose();
    } catch (err) {
      setApiError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <ModalShell
      open={open}
      onClose={onClose}
      preventClose={saving}
      maxWidthClass="max-w-3xl"
      title="Editar Expediente del Cliente"
      subtitle={`Titular: ${cliente.nombreCompleto || cliente.correoElectronico}`}
      icon={
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-sky-50 text-gp-secondary">
          {effectiveMode === 'patch' ? (
            <FileEdit className="h-5 w-5" />
          ) : (
            <RefreshCw className="h-5 w-5" />
          )}
        </div>
      }
      subheader={
        allowFullReplace ? (
          <div className="border-b border-slate-100 bg-slate-50/70 px-5 py-3.5 sm:px-6">
            <div className="grid grid-cols-2 gap-2 rounded-2xl bg-slate-200/70 p-1">
              <button
                type="button"
                onClick={() => {
                  setMode('patch');
                  setErrors({});
                  setSummaryMsg(null);
                }}
                className={`flex items-center justify-center gap-2 rounded-xl py-2 text-xs font-semibold transition ${
                  effectiveMode === 'patch'
                    ? 'bg-white text-gp-primary shadow-sm'
                    : 'text-gp-muted hover:text-gp-primary'
                }`}
              >
                <FileEdit className="h-3.5 w-3.5" />
                <span>Actualización de Datos</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('put');
                  setErrors({});
                  setSummaryMsg(null);
                }}
                className={`flex items-center justify-center gap-2 rounded-xl py-2 text-xs font-semibold transition ${
                  effectiveMode === 'put'
                    ? 'bg-white text-gp-primary shadow-sm'
                    : 'text-gp-muted hover:text-gp-primary'
                }`}
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Reemplazo de Expediente</span>
              </button>
            </div>

            <div
              className={`mt-2.5 flex items-start gap-2 rounded-xl px-3 py-2 text-xs ${
                effectiveMode === 'patch'
                  ? 'bg-sky-50/80 text-sky-900'
                  : 'border border-amber-200/80 bg-amber-50/90 text-amber-900'
              }`}
            >
              {effectiveMode === 'patch' ? (
                <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-sky-600" />
              ) : (
                <ShieldAlert className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-600" />
              )}
              <p>
                {effectiveMode === 'patch'
                  ? 'Modifica datos personales, contacto, actividad laboral y domicilio conservando CURP, RFC y contraseña actuales.'
                  : 'Actualiza el expediente integral incluyendo validación fiscal (CURP/RFC), saldo de referencia y nueva contraseña.'}
              </p>
            </div>
          </div>
        ) : undefined
      }
      footer={
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-[11px] text-gp-muted">
            Campos con <span className="font-semibold text-rose-500">*</span> obligatorios. Evita
            usar el carácter &ldquo;#&rdquo; en domicilio.
          </p>
          <div className="flex items-center gap-2.5">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancelar
            </button>
            <button
              type="submit"
              form="cliente-edit-form"
              disabled={saving}
              className="btn-primary"
            >
              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Guardar cambios
            </button>
          </div>
        </div>
      }
    >
      <form noValidate id="cliente-edit-form" onSubmit={handleSubmit} className="space-y-5">
        <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} />
        <ValidationSummary
          message={summaryMsg}
          errorCount={summaryCount}
          onDismiss={() => setSummaryMsg(null)}
        />

        {/* Section 1: Personal Info */}
        <section className="rounded-2xl border border-slate-200/80 bg-slate-50/40 p-4">
          <div className="mb-3.5 flex items-center gap-2 border-b border-slate-200/70 pb-2.5">
            <User className="h-4 w-4 text-gp-secondary" />
            <h4 className="text-xs font-bold uppercase tracking-wider text-gp-primary">
              Información Personal
            </h4>
          </div>
          <div className="grid gap-3.5 sm:grid-cols-2">
            <div>
              <label className="input-label">
                Nombre <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.nombre ? 'input-field-error' : ''}`}
                value={nombre}
                onChange={(e) => {
                  setNombre(e.target.value);
                  clearField('nombre');
                }}
              />
              <FieldError message={errors.nombre} />
            </div>
            <div>
              <label className="input-label">
                Segundo nombre <span className="font-normal text-slate-400">(opcional)</span>
              </label>
              <input
                className={`input-field ${errors.segundoNombre ? 'input-field-error' : ''}`}
                value={segundoNombre}
                onChange={(e) => {
                  setSegundoNombre(e.target.value);
                  clearField('segundoNombre');
                }}
              />
              <FieldError message={errors.segundoNombre} />
            </div>
            <div>
              <label className="input-label">
                Apellido paterno <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.apellidoPaterno ? 'input-field-error' : ''}`}
                value={apellidoPaterno}
                onChange={(e) => {
                  setApellidoPaterno(e.target.value);
                  clearField('apellidoPaterno');
                }}
              />
              <FieldError message={errors.apellidoPaterno} />
            </div>
            <div>
              <label className="input-label">
                Apellido materno <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.apellidoMaterno ? 'input-field-error' : ''}`}
                value={apellidoMaterno}
                onChange={(e) => {
                  setApellidoMaterno(e.target.value);
                  clearField('apellidoMaterno');
                }}
              />
              <FieldError message={errors.apellidoMaterno} />
            </div>

            <div className="sm:col-span-2">
              <label className="input-label flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-gp-secondary" />
                <span>
                  Fecha de nacimiento <span className="text-rose-500">*</span>
                </span>
              </label>
              {(() => {
                const [yStr = '1995', mStr = '05', dStr = '20'] = fechaNacimiento.split('-');
                const anioActual = new Date().getFullYear();
                const anioOptions: SelectOption[] = Array.from({ length: 83 }, (_, i) => {
                  const yr = String(anioActual - 18 - i);
                  return { value: yr, label: yr };
                });
                const maxDias = diasEnMes(Number(yStr) || 1995, Number(mStr) || 1);
                const diaOptions: SelectOption[] = Array.from({ length: maxDias }, (_, i) => {
                  const dy = String(i + 1).padStart(2, '0');
                  return { value: dy, label: dy };
                });
                const updateFecha = (newY: string, newM: string, newD: string) => {
                  const limite = diasEnMes(Number(newY), Number(newM));
                  const dAjustado = String(Math.min(Number(newD), limite)).padStart(2, '0');
                  setFechaNacimiento(`${newY}-${newM}-${dAjustado}`);
                  clearField('fechaNacimiento');
                };
                return (
                  <div className="grid grid-cols-3 gap-2.5">
                    <CustomSelect
                      ariaLabel="Día de nacimiento"
                      value={dStr}
                      options={diaOptions}
                      searchable={false}
                      mono
                      hasError={Boolean(errors.fechaNacimiento)}
                      onChange={(val) => updateFecha(yStr, mStr, val)}
                    />
                    <CustomSelect
                      ariaLabel="Mes de nacimiento"
                      value={mStr}
                      options={MESES_OPTIONS}
                      searchable={false}
                      hasError={Boolean(errors.fechaNacimiento)}
                      onChange={(val) => updateFecha(yStr, val, dStr)}
                    />
                    <CustomSelect
                      ariaLabel="Año de nacimiento"
                      value={yStr}
                      options={anioOptions}
                      searchable
                      searchPlaceholder="Buscar año..."
                      mono
                      hasError={Boolean(errors.fechaNacimiento)}
                      onChange={(val) => updateFecha(val, mStr, dStr)}
                    />
                  </div>
                );
              })()}
              <FieldError message={errors.fechaNacimiento} />
            </div>

            <div>
              <label className="input-label">
                Sexo <span className="text-rose-500">*</span>
              </label>
              <CustomSelect
                ariaLabel="Sexo"
                value={sexo}
                options={SEXO_OPTIONS}
                searchable={false}
                onChange={setSexo}
              />
            </div>

            <div>
              <label className="input-label">
                Estado civil <span className="text-rose-500">*</span>
              </label>
              <CustomSelect
                ariaLabel="Estado civil"
                value={estadoCivil}
                options={ESTADO_CIVIL_OPTIONS}
                searchable={false}
                onChange={setEstadoCivil}
              />
            </div>

            <div className="sm:col-span-2">
              <label className="input-label">Nacionalidad</label>
              <CustomSelect
                ariaLabel="Nacionalidad"
                value={idNacionalidad}
                options={nacionalidadOptions}
                searchable
                searchPlaceholder="Buscar nacionalidad o país..."
                leadingIcon={<Globe className="h-4 w-4" />}
                onChange={setIdNacionalidad}
              />
            </div>
          </div>
        </section>

        {/* Section 2: Contact */}
        <section className="rounded-2xl border border-slate-200/80 bg-slate-50/40 p-4">
          <div className="mb-3.5 flex items-center gap-2 border-b border-slate-200/70 pb-2.5">
            <Phone className="h-4 w-4 text-gp-secondary" />
            <h4 className="text-xs font-bold uppercase tracking-wider text-gp-primary">
              Datos de Contacto
            </h4>
          </div>
          <div className="grid gap-3.5 sm:grid-cols-2">
            <div>
              <label className="input-label">
                Teléfono móvil (10 dígitos) <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field font-mono ${
                  errors.telefonoMovil ? 'input-field-error' : ''
                }`}
                inputMode="numeric"
                maxLength={10}
                value={telefonoMovil}
                onChange={(e) => {
                  setTelefonoMovil(e.target.value.replace(/\D/g, ''));
                  clearField('telefonoMovil');
                }}
              />
              <FieldError message={errors.telefonoMovil} />
            </div>
            <div>
              <label className="input-label">
                Teléfono alternativo <span className="font-normal text-slate-400">(opcional)</span>
              </label>
              <input
                className={`input-field font-mono ${
                  errors.telefonoAlternativo ? 'input-field-error' : ''
                }`}
                inputMode="numeric"
                maxLength={10}
                value={telefonoAlternativo}
                onChange={(e) => {
                  setTelefonoAlternativo(e.target.value.replace(/\D/g, ''));
                  clearField('telefonoAlternativo');
                }}
                placeholder="10 dígitos"
              />
              <FieldError message={errors.telefonoAlternativo} />
            </div>
            <div className="sm:col-span-2">
              <label className="input-label">
                Correo electrónico <span className="text-rose-500">*</span>
              </label>
              <input
                type="email"
                className={`input-field ${errors.correoElectronico ? 'input-field-error' : ''}`}
                value={correoElectronico}
                onChange={(e) => {
                  setCorreoElectronico(e.target.value);
                  clearField('correoElectronico');
                }}
              />
              <FieldError message={errors.correoElectronico} />
            </div>
          </div>
        </section>

        {/* Section 3: Work & Income */}
        <section className="rounded-2xl border border-slate-200/80 bg-slate-50/40 p-4">
          <div className="mb-3.5 flex items-center gap-2 border-b border-slate-200/70 pb-2.5">
            <Briefcase className="h-4 w-4 text-gp-secondary" />
            <h4 className="text-xs font-bold uppercase tracking-wider text-gp-primary">
              Perfil Laboral e Ingresos
            </h4>
          </div>
          <div className="grid gap-3.5 sm:grid-cols-2">
            <div>
              <label className="input-label">
                Ocupación <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.ocupacion ? 'input-field-error' : ''}`}
                value={ocupacion}
                onChange={(e) => {
                  setOcupacion(e.target.value);
                  clearField('ocupacion');
                }}
              />
              <FieldError message={errors.ocupacion} />
            </div>
            <div>
              <label className="input-label">
                Empresa <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.empresa ? 'input-field-error' : ''}`}
                value={empresa}
                onChange={(e) => {
                  setEmpresa(e.target.value);
                  clearField('empresa');
                }}
              />
              <FieldError message={errors.empresa} />
            </div>
            <div className="sm:col-span-2">
              <label className="input-label">
                Ingreso mensual <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-gp-muted">
                  $
                </span>
                <input
                  type="text"
                  inputMode="decimal"
                  className={`input-field pl-7 pr-14 font-mono ${
                    errors.ingresoMensual ? 'input-field-error' : ''
                  }`}
                  value={ingresoMensual}
                  onChange={(e) => {
                    setIngresoMensual(e.target.value);
                    clearField('ingresoMensual');
                  }}
                />
                <span className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[11px] font-semibold text-gp-muted">
                  MXN
                </span>
              </div>
              <FieldError message={errors.ingresoMensual} />
            </div>
          </div>
        </section>

        {/* Section 4: Address */}
        <section className="rounded-2xl border border-slate-200/80 bg-slate-50/40 p-4">
          <div className="mb-3.5 flex items-center gap-2 border-b border-slate-200/70 pb-2.5">
            <MapPin className="h-4 w-4 text-gp-secondary" />
            <h4 className="text-xs font-bold uppercase tracking-wider text-gp-primary">
              Domicilio Residencial
            </h4>
          </div>
          <div className="grid gap-3.5 sm:grid-cols-6">
            <div className="sm:col-span-6">
              <label className="input-label">
                Calle <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.calle ? 'input-field-error' : ''}`}
                value={calle}
                onChange={(e) => {
                  setCalle(e.target.value);
                  clearField('calle');
                }}
              />
              <FieldError message={errors.calle} />
            </div>
            <div className="sm:col-span-3">
              <label className="input-label">
                Núm. exterior <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.numeroExterior ? 'input-field-error' : ''}`}
                value={numeroExterior}
                onChange={(e) => {
                  setNumeroExterior(e.target.value);
                  clearField('numeroExterior');
                }}
              />
              <FieldError message={errors.numeroExterior} />
            </div>
            <div className="sm:col-span-3">
              <label className="input-label">
                Núm. interior <span className="font-normal text-slate-400">(opcional)</span>
              </label>
              <input
                className={`input-field ${errors.numeroInterior ? 'input-field-error' : ''}`}
                value={numeroInterior}
                onChange={(e) => {
                  setNumeroInterior(e.target.value);
                  clearField('numeroInterior');
                }}
                placeholder="Ej. Piso 4 / Depto B"
              />
              <FieldError message={errors.numeroInterior} />
            </div>
            <div className="sm:col-span-3">
              <label className="input-label">
                Colonia <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.colonia ? 'input-field-error' : ''}`}
                value={colonia}
                onChange={(e) => {
                  setColonia(e.target.value);
                  clearField('colonia');
                }}
              />
              <FieldError message={errors.colonia} />
            </div>
            <div className="sm:col-span-3">
              <label className="input-label">
                Municipio / Alcaldía <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.municipio ? 'input-field-error' : ''}`}
                value={municipio}
                onChange={(e) => {
                  setMunicipio(e.target.value);
                  clearField('municipio');
                }}
              />
              <FieldError message={errors.municipio} />
            </div>
            <div className="sm:col-span-3">
              <label className="input-label">
                Estado <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.estado ? 'input-field-error' : ''}`}
                value={estado}
                onChange={(e) => {
                  setEstado(e.target.value);
                  clearField('estado');
                }}
              />
              <FieldError message={errors.estado} />
            </div>
            <div className="sm:col-span-1">
              <label className="input-label">
                C.P. <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field font-mono ${errors.codigoPostal ? 'input-field-error' : ''}`}
                inputMode="numeric"
                maxLength={5}
                value={codigoPostal}
                onChange={(e) => {
                  setCodigoPostal(e.target.value.replace(/\D/g, ''));
                  clearField('codigoPostal');
                }}
              />
              <FieldError message={errors.codigoPostal} />
            </div>
            <div className="sm:col-span-2">
              <label className="input-label">
                País <span className="text-rose-500">*</span>
              </label>
              <input
                className={`input-field ${errors.pais ? 'input-field-error' : ''}`}
                value={pais}
                onChange={(e) => {
                  setPais(e.target.value);
                  clearField('pais');
                }}
              />
              <FieldError message={errors.pais} />
            </div>
          </div>
        </section>

        {/* Section 5: Full-replace Fiscal & Security */}
        {effectiveMode === 'put' && (
          <section className="rounded-2xl border border-amber-200/90 bg-amber-50/40 p-4">
            <div className="mb-3.5 flex items-center gap-2 border-b border-amber-200/70 pb-2.5">
              <ShieldAlert className="h-4 w-4 text-amber-600" />
              <h4 className="text-xs font-bold uppercase tracking-wider text-amber-950">
                Identidad Fiscal y Seguridad
              </h4>
            </div>

            <div className="grid gap-3.5 sm:grid-cols-2">
              <div>
                <label className="input-label">
                  CURP (18 caracteres) <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field font-mono uppercase ${
                    errors.curp ? 'input-field-error' : ''
                  }`}
                  maxLength={18}
                  value={curp}
                  onChange={(e) => {
                    setCurp(e.target.value.toUpperCase());
                    clearField('curp');
                  }}
                />
                <FieldError message={errors.curp} />
              </div>
              <div>
                <label className="input-label">
                  RFC (12–13 caracteres) <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field font-mono uppercase ${
                    errors.rfc ? 'input-field-error' : ''
                  }`}
                  maxLength={13}
                  value={rfc}
                  onChange={(e) => {
                    setRfc(e.target.value.toUpperCase());
                    clearField('rfc');
                  }}
                />
                <FieldError message={errors.rfc} />
              </div>
              <div>
                <label className="input-label">
                  Saldo inicial / referencia <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-gp-muted">
                    $
                  </span>
                  <input
                    className={`input-field pl-7 pr-14 font-mono ${
                      errors.saldoInicial ? 'input-field-error' : ''
                    }`}
                    value={saldoInicial}
                    onChange={(e) => {
                      setSaldoInicial(e.target.value);
                      clearField('saldoInicial');
                    }}
                  />
                  <span className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[11px] font-semibold text-gp-muted">
                    MXN
                  </span>
                </div>
                <FieldError message={errors.saldoInicial} />
              </div>
              <div>
                <label className="input-label">
                  Nueva contraseña segura <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className={`input-field pr-10 ${errors.password ? 'input-field-error' : ''}`}
                    placeholder="Mín. 8 caracteres, mayús., número y símbolo"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      clearField('password');
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
                    aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <FieldError message={errors.password} />
              </div>
            </div>
          </section>
        )}
      </form>
    </ModalShell>
  );
};
