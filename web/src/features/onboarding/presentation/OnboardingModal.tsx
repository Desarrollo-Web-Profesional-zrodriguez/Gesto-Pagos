import React, { useEffect, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Loader2,
  RefreshCw,
  UserCheck,
  X,
} from 'lucide-react';
import { ErrorBanner } from '../../../core/components/UiPrimitives';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import { useSession } from '../../../core/session/SessionContext';
import { formatMxn } from '../../../core/utils/formatters';
import { BackendValidators } from '../../../core/validation/backendValidators';
import {
  NacionalidadDto,
  nacionalidadRepository,
} from '../../catalogos_sistema/data/nacionalidadRepository';
import { ClienteDto } from '../../clientes/data/clienteModels';
import { clienteRepository } from '../../clientes/data/clienteRepository';

interface OnboardingModalProps {
  open: boolean;
  onClose: () => void;
}

const STEP_LABELS = [
  '1. Datos Personales',
  '2. Contacto y Domicilio',
  '3. Laboral y Apertura',
  '4. Credenciales',
];

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ open, onClose }) => {
  const { loginWithPassword } = useSession();

  const [step, setStep] = useState(0);
  const [nacionalidades, setNacionalidades] = useState<NacionalidadDto[]>([]);
  const [loadingNac, setLoadingNac] = useState(false);
  const [nacError, setNacError] = useState<unknown>(null);

  // Step 1
  const [nombre, setNombre] = useState('');
  const [segundoNombre, setSegundoNombre] = useState('');
  const [apellidoPaterno, setApellidoPaterno] = useState('');
  const [apellidoMaterno, setApellidoMaterno] = useState('');
  const [fechaNacimiento, setFechaNacimiento] = useState('1995-05-20');
  const [curp, setCurp] = useState('');
  const [rfc, setRfc] = useState('');
  const [sexo, setSexo] = useState('Masculino');
  const [idNacionalidad, setIdNacionalidad] = useState<number | ''>('');
  const [estadoCivil, setEstadoCivil] = useState('Soltero');

  // Step 2
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

  // Step 3
  const [ocupacion, setOcupacion] = useState('');
  const [empresa, setEmpresa] = useState('');
  const [ingresoMensual, setIngresoMensual] = useState('25000.00');
  const [saldoInicial, setSaldoInicial] = useState('1000.00');

  // Step 4
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [embedding, setEmbedding] = useState('');

  const [validationError, setValidationError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);
  const [creado, setCreado] = useState<ClienteDto | null>(null);

  const fetchNacionalidades = async () => {
    setLoadingNac(true);
    setNacError(null);
    try {
      const list = await nacionalidadRepository.listarTodas();
      setNacionalidades(list);
      if (list.length > 0 && idNacionalidad === '') {
        const mex = list.find((n) => n.claveIso.toUpperCase() === 'MEX') ?? list[0];
        setIdNacionalidad(mex.idNacionalidad);
      }
    } catch (err) {
      setNacError(err);
    } finally {
      setLoadingNac(false);
    }
  };

  useEffect(() => {
    if (open) {
      void fetchNacionalidades();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  if (!open) return null;

  const validateStep = (targetStep: number): string | null => {
    if (targetStep === 0) {
      return (
        BackendValidators.soloLetrasRequerido(nombre, 'Nombre') ||
        BackendValidators.soloLetrasOpcional(segundoNombre, 'Segundo nombre') ||
        BackendValidators.soloLetrasRequerido(apellidoPaterno, 'Apellido paterno') ||
        BackendValidators.soloLetrasRequerido(apellidoMaterno, 'Apellido materno') ||
        BackendValidators.fechaNacimientoMayorEdad(fechaNacimiento) ||
        BackendValidators.curp(curp) ||
        BackendValidators.rfc(rfc) ||
        (idNacionalidad === '' ? 'Selecciona una nacionalidad del catálogo' : null)
      );
    }
    if (targetStep === 1) {
      return (
        BackendValidators.email(correoElectronico) ||
        BackendValidators.telefono(telefonoMovil) ||
        BackendValidators.telefono(telefonoAlternativo, false) ||
        BackendValidators.calleOColonia(calle, 'Calle', 150) ||
        BackendValidators.numeroDomicilio(numeroExterior, 'Número exterior', true) ||
        BackendValidators.numeroDomicilio(numeroInterior, 'Número interior', false) ||
        BackendValidators.calleOColonia(colonia, 'Colonia', 100) ||
        BackendValidators.soloLetrasRequerido(municipio, 'Municipio / Alcaldía', 2, 100) ||
        BackendValidators.soloLetrasRequerido(estado, 'Estado', 2, 100) ||
        BackendValidators.codigoPostal(codigoPostal) ||
        BackendValidators.soloLetrasRequerido(pais, 'País', 2, 50)
      );
    }
    if (targetStep === 2) {
      return (
        BackendValidators.soloLetrasRequerido(ocupacion, 'Ocupación', 3, 100) ||
        BackendValidators.empresa(empresa) ||
        BackendValidators.montoDecimal(ingresoMensual, 'Ingreso mensual', 0.01) ||
        BackendValidators.montoDecimal(saldoInicial, 'Saldo inicial', 0)
      );
    }
    if (targetStep === 3) {
      const passErr = BackendValidators.passwordSegura(password);
      if (passErr) return passErr;
      if (password !== confirmPassword) {
        return 'Las contraseñas no coinciden.';
      }
    }
    return null;
  };

  const handleNext = () => {
    setApiError(null);
    const err = validateStep(step);
    if (err) {
      setValidationError(err);
      return;
    }
    setValidationError(null);
    setStep((prev) => Math.min(3, prev + 1));
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setApiError(null);
    const err = validateStep(3);
    if (err) {
      setValidationError(err);
      return;
    }
    setValidationError(null);
    setSubmitting(true);
    try {
      const selectedNac = nacionalidades.find((n) => n.idNacionalidad === idNacionalidad);
      const created = await clienteRepository.registrar({
        nombre,
        segundoNombre,
        apellidoPaterno,
        apellidoMaterno,
        fechaNacimiento,
        curp,
        rfc,
        sexo,
        idNacionalidad: typeof idNacionalidad === 'number' ? idNacionalidad : undefined,
        nacionalidad: selectedNac?.gentilicio,
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
        biometricoFacialEmbedding: embedding,
      });
      setCreado(created);
    } catch (err) {
      setApiError(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleAutoLogin = async () => {
    setSubmitting(true);
    setApiError(null);
    try {
      await loginWithPassword(correoElectronico, password);
      onClose();
    } catch (err) {
      setApiError(err);
    } finally {
      setSubmitting(false);
    }
  };

  const cuentaCreada = creado?.cuentas?.[0];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
      <div className="flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl border border-gp-border bg-white shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gp-border px-6 py-4">
          <div>
            <h3 className="text-lg font-bold text-gp-primary">
              {creado ? 'Registro completado' : 'Registro de Nuevo Cliente (Onboarding)'}
            </h3>
            <p className="text-xs text-gp-muted">
              {creado
                ? 'Tus credenciales ya están activas para Web y Flutter.'
                : 'Se creará tu usuario, cuenta bancaria a 16 dígitos y saldo inicial.'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1.5 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-6 py-5">
          {creado ? (
            <div className="space-y-5 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
                <CheckCircle2 className="h-9 w-9" />
              </div>
              <div>
                <h4 className="text-xl font-bold text-gp-primary">
                  ¡Bienvenido, {creado.nombreCompleto || creado.nombre}!
                </h4>
                <p className="mt-1 text-sm text-gp-muted">
                  Tu alta se guardó en PostgreSQL con acceso habilitado en ambas plataformas.
                </p>
              </div>

              <div className="mx-auto max-w-md rounded-2xl border border-gp-border bg-slate-50 p-4 text-left text-sm">
                <div className="flex justify-between py-1">
                  <span className="text-gp-muted">Usuario / Correo</span>
                  <span className="font-semibold text-gp-primary">{creado.correoElectronico}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-gp-muted">Cuenta 16 dígitos</span>
                  <span className="font-mono font-bold text-gp-secondary">
                    {cuentaCreada?.numeroCuenta || 'Generada'}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-gp-muted">Saldo disponible inicial</span>
                  <span className="font-semibold text-emerald-700">
                    {formatMxn(cuentaCreada?.saldo?.saldoDisponible ?? 0)}
                  </span>
                </div>
              </div>

              <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} />

              <div className="flex flex-wrap justify-center gap-3 pt-2">
                <button type="button" onClick={onClose} className="btn-secondary">
                  Volver al login
                </button>
                <button
                  type="button"
                  disabled={submitting}
                  onClick={handleAutoLogin}
                  className="btn-primary bg-emerald-600 hover:bg-emerald-700"
                >
                  {submitting ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <UserCheck className="h-4 w-4" />
                  )}
                  Entrar automáticamente ahora
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleRegister} className="space-y-5">
              {/* Stepper Pills */}
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {STEP_LABELS.map((label, idx) => (
                  <div
                    key={label}
                    className={`rounded-xl border px-3 py-2 text-xs font-semibold ${
                      idx === step
                        ? 'border-gp-secondary bg-sky-50 text-gp-secondary'
                        : idx < step
                        ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                        : 'border-gp-border bg-slate-50 text-gp-muted'
                    }`}
                  >
                    {label}
                  </div>
                ))}
              </div>

              <ErrorBanner error={nacError} onDismiss={() => setNacError(null)} />
              <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} />
              {validationError && (
                <div className="rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-2.5 text-xs font-medium text-amber-900">
                  {validationError}
                </div>
              )}

              {/* Step 0 */}
              {step === 0 && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Nombre *
                    </label>
                    <input
                      className="input-field"
                      value={nombre}
                      onChange={(e) => setNombre(e.target.value)}
                      placeholder="Juan"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Segundo nombre
                    </label>
                    <input
                      className="input-field"
                      value={segundoNombre}
                      onChange={(e) => setSegundoNombre(e.target.value)}
                      placeholder="Opcional"
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
                      placeholder="Pérez"
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
                      placeholder="López"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Fecha de nacimiento (AAAA-MM-DD) *
                    </label>
                    <input
                      type="date"
                      className="input-field"
                      value={fechaNacimiento}
                      onChange={(e) => setFechaNacimiento(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      CURP (18 caracteres) *
                    </label>
                    <input
                      className="input-field uppercase"
                      maxLength={18}
                      value={curp}
                      onChange={(e) => setCurp(e.target.value.toUpperCase())}
                      placeholder="PELJ950520HDFRRN09"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      RFC con homoclave *
                    </label>
                    <input
                      className="input-field uppercase"
                      maxLength={13}
                      value={rfc}
                      onChange={(e) => setRfc(e.target.value.toUpperCase())}
                      placeholder="PELJ9505201A0"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">Sexo *</label>
                    <select
                      className="input-field"
                      value={sexo}
                      onChange={(e) => setSexo(e.target.value)}
                    >
                      <option value="Masculino">Masculino</option>
                      <option value="Femenino">Femenino</option>
                    </select>
                  </div>
                  <div>
                    <div className="mb-1 flex items-center justify-between">
                      <label className="text-xs font-semibold text-gp-muted">
                        Nacionalidad (BD) *
                      </label>
                      <button
                        type="button"
                        onClick={fetchNacionalidades}
                        className="inline-flex items-center gap-1 text-[11px] font-medium text-gp-secondary hover:underline"
                      >
                        <RefreshCw className={`h-3 w-3 ${loadingNac ? 'animate-spin' : ''}`} />
                        Recargar
                      </button>
                    </div>
                    <select
                      className="input-field"
                      value={idNacionalidad}
                      onChange={(e) =>
                        setIdNacionalidad(e.target.value ? Number(e.target.value) : '')
                      }
                    >
                      <option value="">Selecciona una nacionalidad</option>
                      {nacionalidades.map((n) => (
                        <option key={n.idNacionalidad} value={n.idNacionalidad}>
                          {n.gentilicio} ({n.pais})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Estado civil *
                    </label>
                    <select
                      className="input-field"
                      value={estadoCivil}
                      onChange={(e) => setEstadoCivil(e.target.value)}
                    >
                      <option value="Soltero">Soltero</option>
                      <option value="Casado">Casado</option>
                      <option value="Unión libre">Unión libre</option>
                      <option value="Viudo">Viudo</option>
                    </select>
                  </div>
                </div>
              )}

              {/* Step 1 */}
              {step === 1 && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="sm:col-span-2">
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Correo electrónico (será tu usuario de acceso) *
                    </label>
                    <input
                      type="email"
                      className="input-field"
                      value={correoElectronico}
                      onChange={(e) => setCorreoElectronico(e.target.value)}
                      placeholder="usuario@dominio.com"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Teléfono móvil (10 dígitos) *
                    </label>
                    <input
                      className="input-field"
                      maxLength={10}
                      value={telefonoMovil}
                      onChange={(e) => setTelefonoMovil(e.target.value)}
                      placeholder="5512345678"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Teléfono alternativo (opcional)
                    </label>
                    <input
                      className="input-field"
                      maxLength={10}
                      value={telefonoAlternativo}
                      onChange={(e) => setTelefonoAlternativo(e.target.value)}
                      placeholder="5587654321"
                    />
                  </div>
                  <div className="sm:col-span-2">
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Calle (sin carácter &quot;#&quot;) *
                    </label>
                    <input
                      className="input-field"
                      value={calle}
                      onChange={(e) => setCalle(e.target.value)}
                      placeholder="Av. Insurgentes Sur"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Número exterior (sin &quot;#&quot;) *
                    </label>
                    <input
                      className="input-field"
                      value={numeroExterior}
                      onChange={(e) => setNumeroExterior(e.target.value)}
                      placeholder="1602"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Número interior (opcional)
                    </label>
                    <input
                      className="input-field"
                      value={numeroInterior}
                      onChange={(e) => setNumeroInterior(e.target.value)}
                      placeholder="Piso 4"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Colonia *
                    </label>
                    <input
                      className="input-field"
                      value={colonia}
                      onChange={(e) => setColonia(e.target.value)}
                      placeholder="Del Valle"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Municipio / Alcaldía *
                    </label>
                    <input
                      className="input-field"
                      value={municipio}
                      onChange={(e) => setMunicipio(e.target.value)}
                      placeholder="Benito Juárez"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Estado *
                    </label>
                    <input
                      className="input-field"
                      value={estado}
                      onChange={(e) => setEstado(e.target.value)}
                      placeholder="Ciudad de México"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Código postal (5 dígitos) *
                    </label>
                    <input
                      className="input-field"
                      maxLength={5}
                      value={codigoPostal}
                      onChange={(e) => setCodigoPostal(e.target.value)}
                      placeholder="03940"
                    />
                  </div>
                  <div className="sm:col-span-2">
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">País *</label>
                    <input
                      className="input-field"
                      value={pais}
                      onChange={(e) => setPais(e.target.value)}
                      placeholder="Mexico"
                    />
                  </div>
                </div>
              )}

              {/* Step 2 */}
              {step === 2 && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Ocupación / Puesto *
                    </label>
                    <input
                      className="input-field"
                      value={ocupacion}
                      onChange={(e) => setOcupacion(e.target.value)}
                      placeholder="Ingeniero de Software"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Empresa *
                    </label>
                    <input
                      className="input-field"
                      value={empresa}
                      onChange={(e) => setEmpresa(e.target.value)}
                      placeholder="Tech Solutions"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Ingreso mensual MXN (2 decimales) *
                    </label>
                    <input
                      className="input-field font-mono"
                      value={ingresoMensual}
                      onChange={(e) => setIngresoMensual(e.target.value)}
                      placeholder="25000.00"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Saldo inicial de apertura MXN (2 decimales) *
                    </label>
                    <input
                      className="input-field font-mono"
                      value={saldoInicial}
                      onChange={(e) => setSaldoInicial(e.target.value)}
                      placeholder="1000.00"
                    />
                  </div>
                </div>
              )}

              {/* Step 3 */}
              {step === 3 && (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label className="mb-1 block text-xs font-semibold text-gp-muted">
                        Contraseña segura *
                      </label>
                      <input
                        type="password"
                        className="input-field"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Mínimo 8 caracteres, Mayús, minús, # y símbolo"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-semibold text-gp-muted">
                        Confirmar contraseña *
                      </label>
                      <input
                        type="password"
                        className="input-field"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Repite la contraseña"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-gp-muted">
                      Embedding facial biométrico (opcional)
                    </label>
                    <input
                      className="input-field font-mono text-xs"
                      value={embedding}
                      onChange={(e) => setEmbedding(e.target.value)}
                      placeholder="[0.12, -0.45, 0.89, -0.05, 0.67]"
                    />
                  </div>
                </div>
              )}

              {/* Footer buttons */}
              <div className="flex items-center justify-between border-t border-gp-border pt-4">
                <button
                  type="button"
                  disabled={step === 0 || submitting}
                  onClick={() => {
                    setValidationError(null);
                    setStep((prev) => Math.max(0, prev - 1));
                  }}
                  className="btn-secondary"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Anterior
                </button>

                {step < 3 ? (
                  <button type="button" onClick={handleNext} className="btn-primary">
                    Siguiente
                    <ArrowRight className="h-4 w-4" />
                  </button>
                ) : (
                  <button
                    type="submit"
                    disabled={submitting}
                    className="btn-primary bg-emerald-600 hover:bg-emerald-700"
                  >
                    {submitting ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Registrando...
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="h-4 w-4" />
                        Finalizar registro
                      </>
                    )}
                  </button>
                )}
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
