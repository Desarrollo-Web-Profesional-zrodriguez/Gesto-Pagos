import React, { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { ServerConfigModal } from './core/components/UiPrimitives';
import { SessionProvider, useSession } from './core/session/SessionContext';
import { LoginView } from './features/auth/presentation/LoginView';
import { BackofficeView } from './features/backoffice/presentation/BackofficeView';
import { ClientePortalView } from './features/cliente_portal/presentation/ClientePortalView';
import { OnboardingModal } from './features/onboarding/presentation/OnboardingModal';

const AppContent: React.FC = () => {
  const { sesion, loadingSession } = useSession();

  const [showBackoffice, setShowBackoffice] = useState(false);
  const [onboardingOpen, setOnboardingOpen] = useState(false);
  const [serverModalOpen, setServerModalOpen] = useState(false);

  if (loadingSession) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-gp-bg text-gp-primary">
        <Loader2 className="h-9 w-9 animate-spin text-gp-secondary" />
        <p className="text-sm font-medium text-gp-muted">Verificando sesión...</p>
      </div>
    );
  }

  return (
    <>
      {sesion ? (
        <ClientePortalView onOpenServerConfig={() => setServerModalOpen(true)} />
      ) : showBackoffice ? (
        <BackofficeView
          onBack={() => setShowBackoffice(false)}
          onOpenOnboarding={() => setOnboardingOpen(true)}
          onOpenServerConfig={() => setServerModalOpen(true)}
        />
      ) : (
        <LoginView
          onOpenOnboarding={() => setOnboardingOpen(true)}
          onOpenBackoffice={() => setShowBackoffice(true)}
          onOpenServerConfig={() => setServerModalOpen(true)}
        />
      )}

      <OnboardingModal open={onboardingOpen} onClose={() => setOnboardingOpen(false)} />
      <ServerConfigModal open={serverModalOpen} onClose={() => setServerModalOpen(false)} />
    </>
  );
};

export function App() {
  return (
    <SessionProvider>
      <AppContent />
    </SessionProvider>
  );
}

export default App;
