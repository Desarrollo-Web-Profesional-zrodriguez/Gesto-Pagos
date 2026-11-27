import React, { useEffect, useState } from 'react';
import {
  Building2,
  Loader2,
  Lock,
  MailCheck,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';
import { ModalShell } from '../../../core/components/ModalShell';
import { useToast } from '../../../core/components/ToastContext';
import { ErrorBanner, FieldError, OtpCodeInput } from '../../../core/components/UiPrimitives';
import { apiClient } from '../../../core/network/apiClient';
import { BackendValidators } from '../../../core/validation/backendValidators';
import { AdminChallengeData } from '../../auth/presentation/LoginView';

interface BackofficeLoginModalProps {
  challenge: AdminChallengeData | null;
  onClose: () => void;
  onAuthenticated: (tokenBackoffice: string) => void;
}

interface VerifyStep2Response {
  autenticado: boolean;
  tokenBackoffice: string;
  email: string;
  metodoUsado: string;
  mensaje: string;
}

export const BackofficeLoginModal: React.FC<BackofficeLoginModalProps> = ({
  challenge,
  onClose,
  onAuthenticated,
}) => {
  const { notify } = useToast();
  const [codigoOtp, setCodigoOtp] = useState('');
  const [infoMessage, setInfoMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [otpError, setOtpError] = useState<string | null>(null);

  useEffect(() => {
    if (challenge) {
      setCodigoOtp('');
      setInfoMessage(challenge.mensaje);
      setError(null);
      setOtpError(null);
    }
  }, [challenge]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!challenge) return;
    setError(null);

    const validationMsg = BackendValidators.codigoOtp(codigoOtp);
    if (validationMsg) {
      setOtpError(validationMsg);
      return;
    }
    setOtpError(null);

    setLoading(true);
    try {
      const res = await apiClient.post<VerifyStep2Response>('/auth/backoffice/verificar-2fa', {
        challengeId: challenge.challengeId,
        metodo: 'EMAIL_OTP',
        codigo: codigoOtp.replace(/\D/g, ''),
        plataforma: 'WEB',
      });
      if (res.data.autenticado && res.data.tokenBackoffice) {
        notify({
          title: 'Acceso administrativo autorizado',
          description: 'Bienvenido a la Consola Operativa.',
          variant: 'success',
        });
        onAuthenticated(res.data.tokenBackoffice);
        onClose();
      }
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!challenge) return;
    setResending(true);
    setError(null);
    setOtpError(null);
    try {
      const res = await apiClient.post<{ mensaje: string }>('/auth/backoffice/reenviar-otp', {
        challengeId: challenge.challengeId,
      });
      setCodigoOtp('');
      setInfoMessage(res.data.mensaje);
      notify({
        title: 'Código reenviado',
        description: res.data.mensaje,
        variant: 'info',
      });
    } catch (err) {
      setError(err);
    } finally {
      setResending(false);
    }
  };

  return (
    <ModalShell
      open={Boolean(challenge)}
      onClose={onClose}
      preventClose={loading}
      maxWidthClass="max-w-md"
      title="Verificación en Dos Pasos"
      subtitle="Acceso Administrativo · Consola Operativa"
      icon={
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
          <Building2 className="h-5 w-5" />
        </div>
      }
    >
      {challenge && (
        <form noValidate onSubmit={handleVerify} className="space-y-4">
          <ErrorBanner error={error} onDismiss={() => setError(null)} />

          <div className="flex items-start gap-3 rounded-2xl border border-sky-200 bg-sky-50/80 p-3.5 text-xs text-sky-950">
            <MailCheck className="mt-0.5 h-4 w-4 shrink-0 text-gp-secondary" />
            <div>
              <p className="font-bold">Código enviado por correo electrónico</p>
              <p className="mt-0.5 text-sky-800">
                {infoMessage ||
                  `Ingresa el código de 6 dígitos enviado a ${challenge.emailEnmascarado}.`}
              </p>
            </div>
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-600">
                Código de seguridad (6 dígitos)
              </span>
              <button
                type="button"
                onClick={handleResend}
                disabled={resending}
                className="inline-flex items-center gap-1 text-xs font-semibold text-gp-secondary hover:underline disabled:opacity-50"
              >
                <RefreshCw className={`h-3 w-3 ${resending ? 'animate-spin' : ''}`} />
                Reenviar código
              </button>
            </div>

            <OtpCodeInput
              value={codigoOtp}
              autoFocus
              disabled={loading}
              hasError={Boolean(otpError)}
              onChange={(val) => {
                setCodigoOtp(val);
                if (otpError) setOtpError(null);
              }}
            />
            <FieldError message={otpError} />
          </div>

          <div className="flex items-start gap-2.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-[11px] text-gp-muted">
            <Lock className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
            <span>
              Este código es de un solo uso y tiene una vigencia de <strong>5 minutos</strong> para
              proteger el acceso administrativo.
            </span>
          </div>

          <button type="submit" disabled={loading} className="btn-primary w-full py-3">
            {loading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <ShieldCheck className="h-4 w-4" />
            )}
            Verificar e ingresar
          </button>
        </form>
      )}
    </ModalShell>
  );
};
