import React, { useState } from 'react';
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  Loader2,
  Lock,
  ScanFace,
  Server,
  ShieldCheck,
  UserPlus,
} from 'lucide-react';
import { ErrorBanner } from '../../../core/components/UiPrimitives';
import { apiClient } from '../../../core/network/apiClient';
import { useSession } from '../../../core/session/SessionContext';
import { BackendValidators } from '../../../core/validation/backendValidators';

interface LoginViewProps {
  onOpenOnboarding: () => void;
  onOpenBackoffice: () => void;
  onOpenServerConfig: () => void;
}

export const LoginView: React.FC<LoginViewProps> = ({
  onOpenOnboarding,
  onOpenBackoffice,
  onOpenServerConfig,
}) => {
  const { loginWithPassword, loginWithBiometrics, expiredMessage, clearExpiredMessage } =
    useSession();

  const [mode, setMode] = useState<'password' | 'biometric'>('password');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [embedding, setEmbedding] = useState('[0.12, -0.45, 0.89, -0.05, 0.67]');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<unknown>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearExpiredMessage();
    setApiError(null);
    setFieldError(null);

    const emailErr = BackendValidators.email(email);
    if (emailErr) {
      setFieldError(emailErr);
      return;
    }

    if (mode === 'password') {
      if (!password.trim()) {
        setFieldError('Ingresa tu contraseña para continuar.');
        return;
      }
    } else if (!embedding.trim()) {
      setFieldError('Ingresa el vector de embedding biométrico facial.');
      return;
    }

    setSubmitting(true);
    try {
      if (mode === 'password') {
        await loginWithPassword(email, password);
      } else {
        await loginWithBiometrics(email, embedding);
      }
    } catch (err) {
      setApiError(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-sky-950 px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-6xl flex-col gap-8 lg:grid lg:grid-cols-12 lg:items-center lg:gap-12 lg:py-8">
        {/* Left Informational Panel */}
        <div className="text-white lg:col-span-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-sky-400/30 bg-sky-500/10 px-3.5 py-1 text-xs font-semibold text-sky-300">
            <ShieldCheck className="h-4 w-4" />
            Plataforma Unificada Web + Flutter
          </div>

          <h1 className="mt-5 text-3xl font-extrabold tracking-tight sm:text-4xl lg:text-5xl">
            Gesto<span className="text-sky-400">Pagos</span> Servicios
          </h1>
          <p className="mt-4 max-w-xl text-base leading-relaxed text-slate-300">
            Consulta el catálogo real de servicios y recargas, valida referencias antes de pagar,
            autoriza operaciones con confirmación explícita y revisa tu historial compartido con la
            app móvil usando las mismas credenciales.
          </p>

          <div className="mt-8 grid gap-3.5 sm:grid-cols-2">
            {[
              'Mismas credenciales y saldo en Web y Flutter',
              'Catálogo dinámico con logotipos reales de proveedores',
              'Validación obligatoria de referencias previo al cargo',
              'Sesión protegida con expiración por inactividad (5 min)',
            ].map((item) => (
              <div
                key={item}
                className="flex items-start gap-2.5 rounded-xl border border-white/10 bg-white/5 p-3.5 text-sm text-slate-200"
              >
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
                <span>{item}</span>
              </div>
            ))}
          </div>

          <div className="mt-8 flex flex-wrap items-center gap-3 text-xs text-slate-300">
            <button
              type="button"
              onClick={onOpenServerConfig}
              className="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-white/10 px-3.5 py-2 font-medium text-white transition hover:bg-white/20"
            >
              <Server className="h-4 w-4 text-sky-300" />
              Backend: <span className="font-mono">{apiClient.getBaseUrl()}</span>
            </button>

            <button
              type="button"
              onClick={onOpenBackoffice}
              className="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-transparent px-3.5 py-2 font-medium text-slate-200 transition hover:bg-white/10"
            >
              <Building2 className="h-4 w-4 text-amber-300" />
              Consola Backoffice
            </button>
          </div>
        </div>

        {/* Right Authentication Card */}
        <div className="lg:col-span-6">
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-8">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="text-2xl font-bold text-gp-primary">Iniciar Sesión</h2>
                <p className="mt-1 text-sm text-gp-muted">
                  Accede con tu correo registrado en la plataforma
                </p>
              </div>
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-sky-50 text-gp-secondary">
                <Lock className="h-6 w-6" />
              </div>
            </div>

            {expiredMessage && (
              <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3.5 text-sm text-amber-900">
                <p className="font-semibold">Sesión cerrada por seguridad</p>
                <p className="mt-0.5 text-xs text-amber-800">{expiredMessage}</p>
              </div>
            )}

            <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} className="mt-4" />

            {/* Auth method selector */}
            <div className="mt-6 grid grid-cols-2 gap-2 rounded-xl bg-slate-100 p-1">
              <button
                type="button"
                onClick={() => {
                  setMode('password');
                  setFieldError(null);
                }}
                className={`flex items-center justify-center gap-2 rounded-lg py-2 text-xs font-semibold transition ${
                  mode === 'password'
                    ? 'bg-white text-gp-primary shadow-sm'
                    : 'text-gp-muted hover:text-gp-primary'
                }`}
              >
                <KeyRound className="h-4 w-4" />
                Contraseña
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('biometric');
                  setFieldError(null);
                }}
                className={`flex items-center justify-center gap-2 rounded-lg py-2 text-xs font-semibold transition ${
                  mode === 'biometric'
                    ? 'bg-white text-gp-primary shadow-sm'
                    : 'text-gp-muted hover:text-gp-primary'
                }`}
              >
                <ScanFace className="h-4 w-4" />
                Biometría (Vector)
              </button>
            </div>

            <form onSubmit={handleSubmit} className="mt-5 space-y-4">
              <div>
                <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                  Correo electrónico
                </label>
                <input
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setFieldError(null);
                  }}
                  placeholder="usuario@dominio.com"
                  className="input-field"
                  disabled={submitting}
                />
              </div>

              {mode === 'password' ? (
                <div>
                  <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                    Contraseña
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="current-password"
                      value={password}
                      onChange={(e) => {
                        setPassword(e.target.value);
                        setFieldError(null);
                      }}
                      placeholder="••••••••••••"
                      className="input-field pr-10"
                      disabled={submitting}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-md p-1 text-gp-muted hover:text-gp-primary"
                      aria-label={showPassword ? 'Ocultar contraseña' : 'Ver contraseña'}
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
              ) : (
                <div>
                  <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gp-muted">
                    Embedding Biométrico Facial (JSON float array)
                  </label>
                  <textarea
                    rows={3}
                    value={embedding}
                    onChange={(e) => {
                      setEmbedding(e.target.value);
                      setFieldError(null);
                    }}
                    placeholder="[0.12, -0.45, 0.89, -0.05, 0.67]"
                    className="input-field font-mono text-xs"
                    disabled={submitting}
                  />
                  <p className="mt-1 text-xs text-gp-muted">
                    Compatibilidad directa con el perfil facial MediaPipe registrado.
                  </p>
                </div>
              )}

              {fieldError && <p className="text-xs font-medium text-red-600">{fieldError}</p>}

              <button
                type="submit"
                disabled={submitting}
                className="btn-primary w-full py-3 text-sm"
              >
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Validando credenciales...
                  </>
                ) : (
                  <>
                    Entrar a la plataforma
                    <ArrowRight className="h-4 w-4" />
                  </>
                )}
              </button>
            </form>

            <div className="mt-6 border-t border-gp-border pt-5">
              <div className="flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-center">
                <div>
                  <p className="text-sm font-semibold text-gp-primary">¿Aún no tienes cuenta?</p>
                  <p className="text-xs text-gp-muted">
                    Regístrate en el onboarding y úsala en Web y Flutter.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={onOpenOnboarding}
                  className="btn-secondary shrink-0 border-sky-200 bg-sky-50 text-sky-800 hover:bg-sky-100"
                >
                  <UserPlus className="h-4 w-4" />
                  Registrar cliente
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
