import React, { useEffect, useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { ToastProvider } from './core/components/ToastContext';
import { apiClient } from './core/network/apiClient';
import { SessionProvider, useSession } from './core/session/SessionContext';
import { LandingView } from './features/auth/presentation/LandingView';
import { AdminChallengeData, LoginView } from './features/auth/presentation/LoginView';
import { BackofficeLoginModal } from './features/backoffice/presentation/BackofficeLoginModal';
import { BackofficeView } from './features/backoffice/presentation/BackofficeView';
import { ClientePortalView } from './features/cliente_portal/presentation/ClientePortalView';
import { OnboardingModal } from './features/onboarding/presentation/OnboardingModal';

const BACKOFFICE_TOKEN_KEY = 'gestopago_backoffice_token';

function getInitialPublicView(): 'landing' | 'login' {
  if (typeof window !== 'undefined' && window.location.hash === '#login') {
    return 'login';
  }
  return 'landing';
}

const AppContent: React.FC = () => {
  const { sesion, loadingSession, expiredMessage } = useSession();

  const [publicView, setPublicView] = useState<'landing' | 'login'>(getInitialPublicView);
  const [backofficeToken, setBackofficeToken] = useState<string | null>(() => {
    try {
      return window.sessionStorage.getItem(BACKOFFICE_TOKEN_KEY);
    } catch {
      return null;
  }
  });
  const [checkingBackoffice, setCheckingBackoffice] = useState<boolean>(() => {
    try {
      return Boolean(window.sessionStorage.getItem(BACKOFFICE_TOKEN_KEY));
    } catch {
      return false;
    }
  });
  const [adminChallenge, setAdminChallenge] = useState<AdminChallengeData | null>(null);
  const [onboardingOpen, setOnboardingOpen] = useState(false);

  // Sync browser hash for public navigation
  useEffect(() => {
    const onHashChange = () => {
      if (window.location.hash === '#login') {
        setPublicView('login');
      } else if (window.location.hash === '' || window.location.hash === '#inicio') {
        setPublicView('landing');
      }
    };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  // Validate stored Backoffice token on reload
  useEffect(() => {
    if (!backofficeToken) {
      setCheckingBackoffice(false);
      return;
    }
    let active = true;
    (async () => {
      try {
        const res = await apiClient.get<{ valida?: boolean }>('/auth/backoffice/validar-sesion', {
          headers: { Authorization: `Bearer ${backofficeToken}` },
        });
        if (!active) return;
        if (!res.data?.valida) {
          setBackofficeToken(null);
          try {
            window.sessionStorage.removeItem(BACKOFFICE_TOKEN_KEY);
          } catch {
            // Ignore storage errors
          }
        }
      } catch {
        if (!active) return;
        setBackofficeToken(null);
        try {
          window.sessionStorage.removeItem(BACKOFFICE_TOKEN_KEY);
        } catch {
          // Ignore storage errors
        }
      } finally {
        if (active) setCheckingBackoffice(false);
      }
    })();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const navigatePublic = (target: 'landing' | 'login') => {
    setPublicView(target);
    if (typeof window !== 'undefined') {
      const nextHash = target === 'login' ? '#login' : '';
      if (window.location.hash !== nextHash) {
        window.history.replaceState(
          null,
          '',
          nextHash ? `${window.location.pathname}${nextHash}` : window.location.pathname
        );
      }
    }
  };

  const handleBackofficeAuthenticated = (token: string) => {
    setBackofficeToken(token);
    try {
      window.sessionStorage.setItem(BACKOFFICE_TOKEN_KEY, token);
    } catch {
      // Ignore storage errors
    }
  };

  const handleExitBackoffice = () => {
    const token = backofficeToken;
    setBackofficeToken(null);
    navigatePublic('login');
    try {
      window.sessionStorage.removeItem(BACKOFFICE_TOKEN_KEY);
    } catch {
      // Ignore storage errors
    }
    if (token) {
      void apiClient
        .post('/auth/backoffice/logout', {}, { headers: { Authorization: `Bearer ${token}` } })
        .catch(() => {});
    }
  };

  if (loadingSession || checkingBackoffice) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-950 text-white">
        <div className="relative flex h-14 w-14 items-center justify-center rounded-2xl bg-sky-500/15 text-sky-400 ring-1 ring-inset ring-sky-400/30">
          <ShieldCheck className="h-7 w-7 animate-pulse" />
        </div>
        <div className="text-center">
          <p className="font-display text-base font-bold tracking-tight">GestoPagos</p>
          <p className="mt-1 text-xs text-slate-400">Verificando sesión segura...</p>
        </div>
      </div>
    );
  }

  const showLogin = publicView === 'login' || Boolean(expiredMessage);

  return (
    <>
      {sesion ? (
        <ClientePortalView />
      ) : backofficeToken ? (
        <BackofficeView
          onBack={handleExitBackoffice}
          onOpenOnboarding={() => setOnboardingOpen(true)}
        />
      ) : showLogin ? (
        <LoginView
          onBackToLanding={() => navigatePublic('landing')}
          onOpenOnboarding={() => setOnboardingOpen(true)}
          onAdminChallenge={(challenge) => setAdminChallenge(challenge)}
        />
      ) : (
        <LandingView
          onGoLogin={() => navigatePublic('login')}
          onOpenOnboarding={() => setOnboardingOpen(true)}
        />
      )}

      <BackofficeLoginModal
        challenge={adminChallenge}
        onClose={() => setAdminChallenge(null)}
        onAuthenticated={handleBackofficeAuthenticated}
      />
      <OnboardingModal open={onboardingOpen} onClose={() => setOnboardingOpen(false)} />
    </>
  );
};

export function App() {
  return (
    <ToastProvider>
      <SessionProvider>
        <AppContent />
      </SessionProvider>
    </ToastProvider>
  );
}

export default App;
