import React, { useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  ShieldCheck,
  UserPlus,
} from 'lucide-react';
import { ErrorBanner, FieldError } from '../../../core/components/UiPrimitives';
import { apiClient } from '../../../core/network/apiClient';
import { useSession } from '../../../core/session/SessionContext';
import {
  BackendValidators,
  collectValidationErrors,
  FieldErrors,
} from '../../../core/validation/backendValidators';

export interface AdminChallengeData {
  challengeId: string;
  emailEnmascarado: string;
  mensaje: string;
}

interface LoginViewProps {
  onBackToLanding?: () => void;
  onOpenOnboarding: () => void;
  onAdminChallenge: (challenge: AdminChallengeData) => void;
}

type LoginField = 'email' | 'password';

export const LoginView: React.FC<LoginViewProps> = ({
  onBackToLanding,
  onOpenOnboarding,
  onAdminChallenge,
}) => {
  const { loginWithPassword, expiredMessage, clearExpiredMessage } = useSession();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<FieldErrors<LoginField>>({});
  const [apiError, setApiError] = useState<unknown>(null);

  const clearFieldError = (field: LoginField) => {
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: undefined }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearExpiredMessage();
    setApiError(null);

    const check = collectValidationErrors<LoginField>({
      email: BackendValidators.email(email),
      password: BackendValidators.passwordLogin(password),
    });

    if (!check.valid) {
      setErrors(check.errors);
      return;
    }

    setErrors({});
    setSubmitting(true);
    try {
      await loginWithPassword(email.trim(), password);
    } catch (clientErr) {
      try {
        const res = await apiClient.post<{
          requiere2fa: boolean;
          challengeId: string;
          emailEnmascarado: string;
          mensaje: string;
        }>('/auth/backoffice/login', {
          email: email.trim(),
          password,
          plataforma: 'WEB',
        });
        if (res.data?.requiere2fa && res.data.challengeId) {
          setPassword('');
          onAdminChallenge({
            challengeId: res.data.challengeId,
            emailEnmascarado: res.data.emailEnmascarado,
            mensaje: res.data.mensaje,
          });
          return;
        }
      } catch {
        // Mostrar error estándar de inicio de sesión
      }
      setApiError(clientErr);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen flex-col justify-between overflow-hidden bg-slate-950 px-4 py-6 sm:px-6">
      <div className="pointer-events-none absolute inset-0 bg-grid-dark opacity-45" />
      <div className="pointer-events-none absolute -left-24 -top-24 h-80 w-80 rounded-full bg-sky-500/15 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-indigo-500/15 blur-3xl" />

      <header className="relative z-10 mx-auto flex w-full max-w-5xl items-center justify-start">
        {onBackToLanding && (
          <button
            type="button"
            onClick={onBackToLanding}
            className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs font-semibold text-slate-300 transition hover:border-white/20 hover:bg-white/10 hover:text-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Volver al inicio
          </button>
        )}
      </header>

      <main className="relative z-10 mx-auto my-8 w-full max-w-md">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-400/25 to-emerald-400/15 text-sky-300 ring-1 ring-inset ring-sky-400/30 shadow-glow">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <h1 className="mt-3 font-display text-2xl font-extrabold tracking-tight text-white">
            Gesto<span className="text-sky-400">Pagos</span>
          </h1>
          <p className="mt-1 text-xs text-slate-400">
            Acceso protegido a tu cuenta digital y portal de pagos
          </p>
        </div>

        <div className="rounded-3xl border border-slate-200/90 bg-white p-6 shadow-2xl sm:p-8">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-bold text-gp-primary">Iniciar Sesión</h2>
              <p className="mt-0.5 text-xs text-gp-muted">
                Sesión protegida con cierre automático a los 5 min de inactividad
              </p>
            </div>
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-sky-50 text-gp-secondary">
              <Lock className="h-5 w-5" />
            </div>
          </div>

          {expiredMessage && (
            <div className="mt-4 flex items-start gap-2.5 rounded-2xl border border-amber-200 bg-amber-50 p-3.5 text-xs text-amber-950">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
              <div>
                <p className="font-bold">Sesión cerrada por seguridad</p>
                <p className="mt-0.5 text-amber-800">{expiredMessage}</p>
              </div>
            </div>
          )}

          <ErrorBanner error={apiError} onDismiss={() => setApiError(null)} className="mt-4" />

          <form noValidate onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label htmlFor="login-email" className="input-label">
                Correo electrónico
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                autoFocus
                aria-invalid={Boolean(errors.email)}
                className={`input-field ${errors.email ? 'input-field-error' : ''}`}
                placeholder="correo@ejemplo.com"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  clearFieldError('email');
                }}
              />
              <FieldError message={errors.email} />
            </div>

            <div>
              <label htmlFor="login-password" className="input-label">
                Contraseña
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  aria-invalid={Boolean(errors.password)}
                  className={`input-field pr-10 ${errors.password ? 'input-field-error' : ''}`}
                  placeholder="Ingresa tu contraseña"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    clearFieldError('password');
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-gp-muted transition hover:bg-slate-100 hover:text-gp-primary"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              <FieldError message={errors.password} />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-primary mt-2 w-full py-3 text-sm"
            >
              {submitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Verificando credenciales...
                </>
              ) : (
                <>
                  Entrar a la plataforma
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 flex flex-col items-center justify-between gap-3 border-t border-slate-100 pt-5 text-center sm:flex-row sm:text-left">
            <div>
              <p className="text-xs font-bold text-gp-primary">¿Aún no tienes cuenta?</p>
              <p className="text-[11px] text-gp-muted">
                Obtén tu cuenta de 16 dígitos al instante.
              </p>
            </div>
            <button
              type="button"
              onClick={onOpenOnboarding}
              className="btn-secondary w-full text-xs sm:w-auto"
            >
              <UserPlus className="h-4 w-4 text-gp-secondary" />
              Crear cuenta
            </button>
          </div>
        </div>
      </main>

      <footer className="relative z-10 text-center text-[11px] text-slate-500">
        GestoPagos · Banca digital y pagos de servicios protegidos
      </footer>
    </div>
  );
};
