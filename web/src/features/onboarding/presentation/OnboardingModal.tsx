import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Calendar,
  Check,
  CheckCircle2,
  Circle,
  Clock,
  Eye,
  EyeOff,
  Globe,
  Loader2,
  MailCheck,
  RefreshCw,
  ShieldCheck,
  UserCheck,
  UserPlus,
} from 'lucide-react';
import { CustomSelect, SelectOption } from '../../../core/components/CustomSelect';
import { ModalShell } from '../../../core/components/ModalShell';
import { useToast } from '../../../core/components/ToastContext';
import {
  CopyChip,
  ErrorBanner,
  FieldError,
  OtpCodeInput,
  ValidationSummary,
} from '../../../core/components/UiPrimitives';
import { StrictDecimal } from '../../../core/network/strictDecimal';
import { useSession } from '../../../core/session/SessionContext';
import { formatCountdown, formatMxn } from '../../../core/utils/formatters';
import {
  BackendValidators,
  collectValidationErrors,
  FieldErrors,
  ValidationResult,
} from '../../../core/validation/backendValidators';
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

const STEP_ITEMS = [
  { short: 'Identidad', full: 'Identidad' },
  { short: 'Domicilio', full: 'Domicilio' },
  { short: 'Apertura', full: 'Apertura' },
  { short: 'Clave', full: 'Contraseña' },
  { short: 'Código', full: 'Verificación' },
];

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

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ open, onClose }) => {
  const { loginWithPassword } = useSession();
  const { notify } = useToast();

  const [step, setStep] = useState(0);
  const [nacionalidades, setNacionalidades] = useState<NacionalidadDto[]>([]);
  const [loadingNac, setLoadingNac] = useState(false);
  const [nacError, setNacError] = useState<unknown>(null);

  // Step 0: Personal
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

  // Step 1: Contact & Address
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

  // Step 2: Economic & Initial Balance
  const [ocupacion, setOcupacion] = useState('');
  const [empresa, setEmpresa] = useState('');
  const [ingresoMensual, setIngresoMensual] = useState('25000');
  const [saldoInicial, setSaldoInicial] = useState('1000');

  // Step 3: Credentials
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Step 4: Email OTP Verification
  const [codigoOtp, setCodigoOtp] = useState('');
  const [otpInfoMsg, setOtpInfoMsg] = useState<string | null>(null);
  const [otpSecondsLeft, setOtpSecondsLeft] = useState(0);
  const [sendingOtp, setSendingOtp] = useState(false);

  const [errors, setErrors] = useState<FieldErrors>({});
  const [summaryMsg, setSummaryMsg] = useState<string | null>(null);
  const [summaryCount, setSummaryCount] = useState<number>(0);
  const [apiError, setApiError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);
  const [creado, setCreado] = useState<ClienteDto | null>(null);

  const nacionalidadOptions = useMemo<SelectOption[]>(
    () =>
      nacionalidades.map((n) => ({
        value: String(n.idNacionalidad),
        label: n.gentilicio,
        badge: n.pais,
      })),
    [nacionalidades]
  );

  const clearField = (key: string) => {
    if (errors[key]) {
      setErrors((prev) => ({ ...prev, [key]: undefined }));
    }
    if (summaryMsg) {
      setSummaryMsg(null);
    }
  };

  const resetForm = () => {
    setStep(0);
    setNombre('');
    setSegundoNombre('');
    setApellidoPaterno('');
    setApellidoMaterno('');
    setFechaNacimiento('1995-05-20');
    setCurp('');
    setRfc('');
    setSexo('Masculino');
    setEstadoCivil('Soltero');
    setCorreoElectronico('');
    setTelefonoMovil('');
    setTelefonoAlternativo('');
    setCalle('');
    setNumeroExterior('');
    setNumeroInterior('');
    setColonia('');
    setMunicipio('');
    setEstado('');
    setCodigoPostal('');
    setPais('Mexico');
    setOcupacion('');
    setEmpresa('');
    setIngresoMensual('25000');
    setSaldoInicial('1000');
    setPassword('');
    setConfirmPassword('');
    setShowPassword(false);
    setShowConfirmPassword(false);
    setCodigoOtp('');
    setOtpInfoMsg(null);
    setOtpSecondsLeft(0);
    setErrors({});
    setSummaryMsg(null);
    setSummaryCount(0);
    setApiError(null);
    setCreado(null);
  };

  const fetchNacionalidades = async (preserveSelection = true) => {
    setLoadingNac(true);
    setNacError(null);
    try {
      const list = await nacionalidadRepository.listarTodas();
      setNacionalidades(list);
      if (list.length > 0 && (!preserveSelection || idNacionalidad === '')) {
        const mex = list.find((n) => n.claveIso.toUpperCase() === 'MEX') ?? list[0];
        setIdNacionalidad(String(mex.idNacionalidad));
      }
    } catch (err) {
      setNacError(err);
    } finally {
      setLoadingNac(false);
    }
  };

  useEffect(() => {
    if (open) {
      resetForm();
      void fetchNacionalidades(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  useEffect(() => {
    if (!open || step !== 4 || otpSecondsLeft <= 0) return;
    const interval = window.setInterval(() => {
      setOtpSecondsLeft((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => window.clearInterval(interval);
  }, [open, step, otpSecondsLeft]);

  const validateStep = (targetStep: number): ValidationResult => {
    if (targetStep === 0) {
      return collectValidationErrors({
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
        curp: BackendValidators.curp(curp),
        rfc: BackendValidators.rfc(rfc),
        idNacionalidad:
          idNacionalidad === '' ? 'Selecciona tu nacionalidad.' : null,
      });
    }
    if (targetStep === 1) {
      return collectValidationErrors({
        correoElectronico: BackendValidators.email(correoElectronico),
        telefonoMovil: BackendValidators.telefono(telefonoMovil),
        telefonoAlternativo: BackendValidators.telefono(telefonoAlternativo, false),
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
      });
    }
    if (targetStep === 2) {
      return collectValidationErrors({
        ocupacion: BackendValidators.soloLetrasRequerido(ocupacion, 'La ocupación', 3, 100),
        empresa: BackendValidators.empresa(empresa),
        ingresoMensual: BackendValidators.montoDecimal(ingresoMensual, 'El ingreso mensual', 0.01),
        saldoInicial: BackendValidators.montoDecimal(saldoInicial, 'El saldo inicial', 0),
      });
    }
    if (targetStep === 3) {
      return collectValidationErrors({
        password: BackendValidators.passwordSegura(password),
        confirmPassword: BackendValidators.confirmacionPassword(password, confirmPassword),
      });
    }
    return collectValidationErrors({
      codigoOtp: BackendValidators.codigoOtp(codigoOtp),
    });
  };

  const applyValidationResult = (res: ValidationResult): boolean => {
    setErrors(res.errors);
    setSummaryMsg(res.firstError);
    setSummaryCount(res.errorCount);
    return res.valid;
  };

  const enviarCodigoCorreo = async (esReenvio = false) => {
    setSendingOtp(true);
    setApiError(null);
    setErrors({});
    setSummaryMsg(null);
    try {
      const res = await clienteRepository.solicitarOtpOnboarding({
        correoElectronico,
        nombre,
        curp,
        rfc,
      });
      setCodigoOtp('');
      setOtpInfoMsg(res.mensaje);
      setOtpSecondsLeft(res.expiraEnSegundos || 300);
      setStep(4);
      notify({
        title: esReenvio ? 'Nuevo código enviado' : 'Código de verificación enviado',
        description: `Revisa la bandeja de entrada de ${correoElectronico.trim().toLowerCase()}`,
        variant: 'info',
      });
    } catch (err) {
      setApiError(err);
    } finally {
      setSendingOtp(false);
    }
  };

  const handleNext = () => {
    setApiError(null);
    const res = validateStep(step);
    if (!applyValidationResult(res)) {
      return;
    }
    if (step === 3) {
      void enviarCodigoCorreo(false);
      return;
    }
    setStep((prev) => Math.min(4, prev + 1));
  };

  const handleRegisterVerified = async (e: React.FormEvent) => {
    e.preventDefault();
    if (step < 4) {
      handleNext();
      return;
    }
    setApiError(null);
    const res = validateStep(4);
    if (!applyValidationResult(res)) {
      return;
    }

    setSubmitting(true);
    try {
      const parsedIdNac = idNacionalidad ? Number(idNacionalidad) : undefined;
      const selectedNac = nacionalidades.find((n) => n.idNacionalidad === parsedIdNac);
      const created = await clienteRepository.registrarConOtp(codigoOtp.replace(/\D/g, ''), {
        nombre,
        segundoNombre,
        apellidoPaterno,
        apellidoMaterno,
        fechaNacimiento,
        curp,
        rfc,
        sexo,
        idNacionalidad: parsedIdNac,
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
      });
      setCreado(created);
      notify({
        title: 'Correo verificado y cuenta creada',
        description: 'Tu expediente y cuenta digital a 16 dígitos ya están activos.',
        variant: 'success',
      });
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
  const busy = submitting || sendingOtp;

  return (
    <ModalShell
      open={open}
      onClose={onClose}
      preventClose={busy}
      maxWidthClass="max-w-3xl"
      title={creado ? 'Apertura de Cuenta Exitosa' : 'Abrir Cuenta Digital GestoPagos'}
      subtitle={
        creado
          ? 'Tu correo fue verificado y tu cuenta bancaria a 16 dígitos está activa.'
          : 'Completa tus datos y confirma tu correo electrónico con un código de seguridad.'
      }
      icon={
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-sky-50 text-gp-secondary">
          <UserPlus className="h-5 w-5" />
        </div>
      }
      subheader={
        !creado ? (
          <div className="border-b border-slate-100 bg-slate-50/80 px-5 py-3.5 sm:px-6">
            <ol className="grid grid-cols-5 gap-1.5 sm:gap-2">
              {STEP_ITEMS.map((item, idx) => {
                const done = idx < step;
                const active = idx === step;
                return (
                  <li key={item.short}>
                    <button
                      type="button"
                      disabled={idx > step || busy}
                      onClick={() => {
                        if (idx < step) {
                          setErrors({});
                          setSummaryMsg(null);
                          setApiError(null);
                          setStep(idx);
                        }
                      }}
                      className={`flex w-full items-center gap-1.5 rounded-xl border px-2 py-2 text-left text-xs font-semibold transition sm:gap-2 sm:px-2.5 ${
                        active
                          ? 'border-gp-secondary bg-white text-gp-secondary shadow-sm ring-2 ring-gp-secondary/10'
                          : done
                          ? 'border-emerald-200 bg-emerald-50/70 text-emerald-800 hover:bg-emerald-50'
                          : 'cursor-default border-slate-200/80 bg-white/60 text-slate-400'
                      }`}
                    >
                      <span
                        className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
                          active
                            ? 'bg-gp-secondary text-white'
                            : done
                            ? 'bg-emerald-600 text-white'
                            : 'bg-slate-200 text-slate-500'
                        }`}
                      >
                        {done ? <Check className="h-3 w-3" /> : idx + 1}
                      </span>
                      <span className="truncate">
                        <span className="sm:hidden">{item.short}</span>
                        <span className="hidden sm:inline">{item.full}</span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ol>
          </div>
        ) : undefined
      }
      footer={
        !creado ? (
          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              disabled={step === 0 || busy}
              onClick={() => {
                setErrors({});
                setSummaryMsg(null);
                setApiError(null);
                setStep((prev) => Math.max(0, prev - 1));
              }}
              className="btn-secondary"
            >
              <ArrowLeft className="h-4 w-4" />
              Anterior
            </button>

            <span className="hidden text-[11px] text-gp-muted sm:inline">
              Paso {step + 1} de {STEP_ITEMS.length} · Campos con{' '}
              <span className="font-bold text-rose-500">*</span> obligatorios
            </span>

            {step < 3 ? (
              <button type="button" onClick={handleNext} className="btn-primary">
                Siguiente
                <ArrowRight className="h-4 w-4" />
              </button>
            ) : step === 3 ? (
              <button
                type="button"
                disabled={sendingOtp}
                onClick={handleNext}
                className="btn-primary"
              >
                {sendingOtp ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Enviando código...
                  </>
                ) : (
                  <>
                    <MailCheck className="h-4 w-4" />
                    Enviar código al correo
                  </>
                )}
              </button>
            ) : (
              <button
                type="submit"
                form="onboarding-form"
                disabled={submitting}
                className="btn-primary bg-emerald-600 hover:bg-emerald-700"
              >
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Verificando y activando...
                  </>
                ) : (
                  <>
                    <ShieldCheck className="h-4 w-4" />
                    Confirmar código y abrir cuenta
                  </>
                )}
              </button>
            )}
          </div>
        ) : undefined
      }
    >
      {creado ? (
        <div className="space-y-5 py-2 text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 ring-1 ring-inset ring-emerald-200">
            <CheckCircle2 className="h-9 w-9" />
          </div>
          <div>
            <h4 className="text-xl font-bold text-gp-primary">
              ¡Bienvenido, {creado.nombreCompleto || creado.nombre}!
            </h4>
            <p className="mt-1 text-sm text-gp-muted">
              Verificamos tu correo electrónico y enviamos los detalles de apertura a tu bandeja.
            </p>
          </div>

          <div className="mx-auto max-w-md space-y-2.5 rounded-2xl border border-gp-border bg-slate-50/80 p-5 text-left text-sm">
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs text-gp-muted">Correo verificado</span>
              <span className="font-semibold text-gp-primary">{creado.correoElectronico}</span>
            </div>
            <div className="flex items-center justify-between gap-2 border-t border-slate-200/70 pt-2.5">
              <span className="text-xs text-gp-muted">Cuenta 16 dígitos</span>
              <CopyChip value={cuentaCreada?.numeroCuenta} toastTitle="Número de cuenta copiado" />
            </div>
            <div className="flex items-center justify-between gap-2 border-t border-slate-200/70 pt-2.5">
              <span className="text-xs text-gp-muted">Saldo disponible inicial</span>
              <span className="font-bold text-emerald-700 tabular-nums">
                {formatMxn(cuentaCreada?.saldo?.saldoDisponible ?? 0)}
              </span>
            </div>
          </div>

          <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} />

          <div className="flex flex-wrap justify-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">
              Ir al inicio de sesión
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
              Entrar a mi cuenta ahora
            </button>
          </div>
        </div>
      ) : (
        <form noValidate id="onboarding-form" onSubmit={handleRegisterVerified} className="space-y-5">
          <ErrorBanner error={nacError} onDismiss={() => setNacError(null)} />
          <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} />
          <ValidationSummary
            message={summaryMsg}
            errorCount={summaryCount}
            onDismiss={() => setSummaryMsg(null)}
          />

          {/* Step 0: Identidad */}
          {step === 0 && (
            <div className="grid gap-4 sm:grid-cols-2">
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
                  placeholder="Juan"
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
                  placeholder="Carlos"
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
                  placeholder="Pérez"
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
                  placeholder="López"
                />
                <FieldError message={errors.apellidoMaterno} />
              </div>

              <div className="sm:col-span-2">
                <label className="input-label flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-gp-secondary" />
                  <span>
                    Fecha de nacimiento (mayor de 18 años) <span className="text-rose-500">*</span>
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
                  placeholder="PELJ950520HDFRRN09"
                />
                <FieldError message={errors.curp} />
              </div>
              <div>
                <label className="input-label">
                  RFC con homoclave <span className="text-rose-500">*</span>
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
                  placeholder="PELJ9505201A0"
                />
                <FieldError message={errors.rfc} />
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
                <div className="mb-1.5 flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-600">
                    Nacionalidad <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => void fetchNacionalidades(true)}
                    className="inline-flex items-center gap-1 text-[11px] font-semibold text-gp-secondary hover:underline"
                  >
                    <RefreshCw className={`h-3 w-3 ${loadingNac ? 'animate-spin' : ''}`} />
                    Actualizar lista
                  </button>
                </div>
                <CustomSelect
                  ariaLabel="Nacionalidad"
                  value={idNacionalidad}
                  options={nacionalidadOptions}
                  placeholder="Selecciona una nacionalidad"
                  searchable
                  searchPlaceholder="Buscar nacionalidad o país..."
                  leadingIcon={<Globe className="h-4 w-4" />}
                  hasError={Boolean(errors.idNacionalidad)}
                  onChange={(val) => {
                    setIdNacionalidad(val);
                    clearField('idNacionalidad');
                  }}
                />
                <FieldError message={errors.idNacionalidad} />
              </div>
            </div>
          )}

          {/* Step 1: Contacto y Domicilio */}
          {step === 1 && (
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <label className="input-label">
                  Correo electrónico (recibirás un código de verificación){' '}
                  <span className="text-rose-500">*</span>
                </label>
                <input
                  type="email"
                  className={`input-field ${errors.correoElectronico ? 'input-field-error' : ''}`}
                  value={correoElectronico}
                  onChange={(e) => {
                    setCorreoElectronico(e.target.value);
                    clearField('correoElectronico');
                  }}
                  placeholder="usuario@dominio.com"
                />
                <FieldError message={errors.correoElectronico} />
              </div>
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
                  placeholder="5512345678"
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
                  placeholder="5587654321"
                />
                <FieldError message={errors.telefonoAlternativo} />
              </div>
              <div className="sm:col-span-2">
                <label className="input-label">
                  Calle (sin carácter &ldquo;#&rdquo;) <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field ${errors.calle ? 'input-field-error' : ''}`}
                  value={calle}
                  onChange={(e) => {
                    setCalle(e.target.value);
                    clearField('calle');
                  }}
                  placeholder="Av. Insurgentes Sur"
                />
                <FieldError message={errors.calle} />
              </div>
              <div>
                <label className="input-label">
                  Número exterior (sin &ldquo;#&rdquo;) <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field ${errors.numeroExterior ? 'input-field-error' : ''}`}
                  value={numeroExterior}
                  onChange={(e) => {
                    setNumeroExterior(e.target.value);
                    clearField('numeroExterior');
                  }}
                  placeholder="1602"
                />
                <FieldError message={errors.numeroExterior} />
              </div>
              <div>
                <label className="input-label">
                  Número interior <span className="font-normal text-slate-400">(opcional)</span>
                </label>
                <input
                  className={`input-field ${errors.numeroInterior ? 'input-field-error' : ''}`}
                  value={numeroInterior}
                  onChange={(e) => {
                    setNumeroInterior(e.target.value);
                    clearField('numeroInterior');
                  }}
                  placeholder="Piso 4"
                />
                <FieldError message={errors.numeroInterior} />
              </div>
              <div>
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
                  placeholder="Del Valle"
                />
                <FieldError message={errors.colonia} />
              </div>
              <div>
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
                  placeholder="Benito Juárez"
                />
                <FieldError message={errors.municipio} />
              </div>
              <div>
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
                  placeholder="Ciudad de México"
                />
                <FieldError message={errors.estado} />
              </div>
              <div>
                <label className="input-label">
                  Código postal (5 dígitos) <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field font-mono ${
                    errors.codigoPostal ? 'input-field-error' : ''
                  }`}
                  inputMode="numeric"
                  maxLength={5}
                  value={codigoPostal}
                  onChange={(e) => {
                    setCodigoPostal(e.target.value.replace(/\D/g, ''));
                    clearField('codigoPostal');
                  }}
                  placeholder="03940"
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
                  placeholder="Mexico"
                />
                <FieldError message={errors.pais} />
              </div>
            </div>
          )}

          {/* Step 2: Laboral y Apertura */}
          {step === 2 && (
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="input-label">
                  Ocupación / Profesión <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field ${errors.ocupacion ? 'input-field-error' : ''}`}
                  value={ocupacion}
                  onChange={(e) => {
                    setOcupacion(e.target.value);
                    clearField('ocupacion');
                  }}
                  placeholder="Ingeniero de Software"
                />
                <FieldError message={errors.ocupacion} />
              </div>
              <div>
                <label className="input-label">
                  Empresa o centro de trabajo <span className="text-rose-500">*</span>
                </label>
                <input
                  className={`input-field ${errors.empresa ? 'input-field-error' : ''}`}
                  value={empresa}
                  onChange={(e) => {
                    setEmpresa(e.target.value);
                    clearField('empresa');
                  }}
                  placeholder="Tech Solutions"
                />
                <FieldError message={errors.empresa} />
              </div>
              <div>
                <label className="input-label">
                  Ingreso mensual estimado <span className="text-rose-500">*</span>
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
                    placeholder="25000.00"
                  />
                  <span className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[11px] font-semibold text-gp-muted">
                    MXN
                  </span>
                </div>
                <FieldError message={errors.ingresoMensual} />
              </div>
              <div>
                <label className="input-label">
                  Saldo inicial de apertura <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-gp-muted">
                    $
                  </span>
                  <input
                    type="text"
                    inputMode="decimal"
                    className={`input-field pl-7 pr-14 font-mono ${
                      errors.saldoInicial ? 'input-field-error' : ''
                    }`}
                    value={saldoInicial}
                    onChange={(e) => {
                      setSaldoInicial(e.target.value);
                      clearField('saldoInicial');
                    }}
                    placeholder="1000.00"
                  />
                  <span className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[11px] font-semibold text-gp-muted">
                    MXN
                  </span>
                </div>
                <FieldError message={errors.saldoInicial} />
              </div>
            </div>
          )}

          {/* Step 3: Credenciales */}
          {step === 3 && (
            <div className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="input-label">
                    Contraseña segura <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      className={`input-field pr-10 ${errors.password ? 'input-field-error' : ''}`}
                      value={password}
                      onChange={(e) => {
                        setPassword(e.target.value);
                        clearField('password');
                      }}
                      placeholder="Crea tu contraseña"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  <FieldError message={errors.password} />
                </div>

                <div>
                  <label className="input-label">
                    Confirmar contraseña <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      className={`input-field pr-10 ${
                        errors.confirmPassword ? 'input-field-error' : ''
                      }`}
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        clearField('confirmPassword');
                      }}
                      placeholder="Repite tu contraseña"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((prev) => !prev)}
                      aria-label={
                        showConfirmPassword ? 'Ocultar confirmación' : 'Mostrar confirmación'
                      }
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
                    >
                      {showConfirmPassword ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                  {errors.confirmPassword ? (
                    <FieldError message={errors.confirmPassword} />
                  ) : (
                    confirmPassword.length > 0 &&
                    password === confirmPassword && (
                      <p className="mt-1.5 flex items-center gap-1.5 text-xs font-medium text-emerald-600">
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Las contraseñas coinciden
                      </p>
                    )
                  )}
                </div>
              </div>

              {(() => {
                const rules = [
                  {
                    label: 'Entre 8 y 64 caracteres',
                    ok: password.length >= 8 && password.length <= 64,
                  },
                  { label: 'Al menos una mayúscula (A-Z)', ok: /[A-Z]/.test(password) },
                  { label: 'Al menos una minúscula (a-z)', ok: /[a-z]/.test(password) },
                  { label: 'Al menos un número (0-9)', ok: /\d/.test(password) },
                  {
                    label: 'Un carácter especial (!@#$%*)',
                    ok: /[^a-zA-Z0-9\s]/.test(password),
                  },
                ];
                const passedCount = rules.filter((r) => r.ok).length;
                return (
                  <div className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4">
                    <div className="mb-2.5 flex items-center justify-between">
                      <p className="text-xs font-bold uppercase tracking-wider text-gp-muted">
                        Fortaleza de contraseña
                      </p>
                      <span className="font-mono text-xs font-semibold text-gp-primary">
                        {passedCount}/{rules.length}
                      </span>
                    </div>
                    <div className="mb-3 h-1.5 w-full overflow-hidden rounded-full bg-slate-200">
                      <div
                        className={`h-full transition-all duration-300 ${
                          passedCount === rules.length
                            ? 'bg-emerald-500'
                            : passedCount >= 3
                            ? 'bg-sky-500'
                            : 'bg-amber-500'
                        }`}
                        style={{ width: `${(passedCount / rules.length) * 100}%` }}
                      />
                    </div>
                    <ul className="grid gap-1.5 sm:grid-cols-2">
                      {rules.map((rule) => (
                        <li
                          key={rule.label}
                          className={`flex items-center gap-2 text-xs font-medium transition-colors ${
                            rule.ok ? 'text-emerald-700' : 'text-slate-500'
                          }`}
                        >
                          {rule.ok ? (
                            <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-600" />
                          ) : (
                            <Circle className="h-3.5 w-3.5 shrink-0 text-slate-300" />
                          )}
                          <span>{rule.label}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                );
              })()}

              <div className="flex items-start gap-2.5 rounded-2xl border border-sky-200 bg-sky-50/70 p-3.5 text-xs text-sky-950">
                <MailCheck className="mt-0.5 h-4 w-4 shrink-0 text-gp-secondary" />
                <span>
                  En el siguiente paso enviaremos un código de seguridad de 6 dígitos a{' '}
                  <strong>{correoElectronico || 'tu correo'}</strong> para confirmar la titularidad
                  antes de crear tu cuenta.
                </span>
              </div>
            </div>
          )}

          {/* Step 4: Email OTP Verification */}
          {step === 4 && (
            <div className="mx-auto max-w-md space-y-5 py-2">
              <div className="flex items-start gap-3 rounded-2xl border border-sky-200 bg-sky-50/80 p-4 text-xs text-sky-950">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white text-gp-secondary shadow-sm">
                  <MailCheck className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-bold text-gp-primary">Verifica tu correo electrónico</p>
                  <p className="mt-0.5 leading-relaxed text-sky-900">
                    {otpInfoMsg || 'Enviamos un código de 6 dígitos a:'}{' '}
                    <strong className="break-all">{correoElectronico.trim().toLowerCase()}</strong>
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setErrors({});
                      setSummaryMsg(null);
                      setApiError(null);
                      setStep(1);
                    }}
                    className="mt-1.5 text-[11px] font-semibold text-gp-secondary hover:underline"
                  >
                    ¿Correo incorrecto? Editar dirección
                  </button>
                </div>
              </div>

              <div>
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-600">
                    Código de verificación (6 dígitos) <span className="text-rose-500">*</span>
                  </span>
                  {otpSecondsLeft > 0 ? (
                    <span className="inline-flex items-center gap-1 font-mono text-xs font-semibold text-gp-muted">
                      <Clock className="h-3.5 w-3.5 text-gp-secondary" />
                      {formatCountdown(otpSecondsLeft)}
                    </span>
                  ) : (
                    <span className="text-xs font-semibold text-amber-600">Código expirado</span>
                  )}
                </div>

                <OtpCodeInput
                  value={codigoOtp}
                  autoFocus
                  disabled={submitting}
                  hasError={Boolean(errors.codigoOtp)}
                  onChange={(val) => {
                    setCodigoOtp(val);
                    clearField('codigoOtp');
                  }}
                />
                <FieldError message={errors.codigoOtp} />
              </div>

              <div className="flex items-center justify-between rounded-xl border border-slate-200/80 bg-slate-50 px-3.5 py-2.5 text-xs">
                <span className="text-gp-muted">¿No recibiste el correo?</span>
                <button
                  type="button"
                  disabled={sendingOtp}
                  onClick={() => void enviarCodigoCorreo(true)}
                  className="inline-flex items-center gap-1.5 font-semibold text-gp-secondary hover:underline disabled:opacity-50"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${sendingOtp ? 'animate-spin' : ''}`} />
                  Reenviar código
                </button>
              </div>
            </div>
          )}
        </form>
      )}
    </ModalShell>
  );
};
