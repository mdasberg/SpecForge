import type { ReactNode } from 'react';
import { Route, Routes } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { TopBar } from './shell/TopBar';
import { SidePanel } from './shell/SidePanel';
import { useTheme } from './theme/useTheme';
import { Home } from './pages/Home';
import { Specs } from './pages/Specs';
import { Reviews } from './pages/Reviews';
import { Projects } from './pages/Projects';
import { Activity } from './pages/Activity';

/**
 * There is no login screen: an unauthenticated visitor is sent straight to Keycloak, which owns
 * every credential, password reset and second factor SpecForge deliberately does not build.
 */
function AuthGate({ children }: { children: ReactNode }) {
  const auth = useAuth();

  if (auth.activeNavigator || auth.isLoading) {
    return <div className="main">Signing in…</div>;
  }

  if (auth.error) {
    return (
      <div className="main">
        <div className="card card-pad">
          <div className="card-t">Could not sign in</div>
          <p style={{ color: 'var(--fg-2)' }}>{auth.error.message}</p>
          <button type="button" className="iconbtn" style={{ width: 'auto', padding: '0 10px' }} onClick={() => void auth.signinRedirect()}>
            Try again
          </button>
        </div>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    void auth.signinRedirect();
    return <div className="main">Redirecting to Keycloak…</div>;
  }

  return <>{children}</>;
}

export function App() {
  const { theme } = useTheme();

  return (
    <div className={theme === 'light' ? 'app light' : 'app'}>
      <TopBar />
      <div className="body">
        <SidePanel />
        <main className="main">
          <AuthGate>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/specs" element={<Specs />} />
              <Route path="/reviews" element={<Reviews />} />
              <Route path="/projects" element={<Projects />} />
              <Route path="/activity" element={<Activity />} />
            </Routes>
          </AuthGate>
        </main>
      </div>
    </div>
  );
}
