import { NavLink } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { useTheme } from '../theme/useTheme';

const SCREENS = [
  { to: '/', label: 'Home' },
  { to: '/specs', label: 'Specs' },
  { to: '/reviews', label: 'Reviews' },
  { to: '/projects', label: 'Projects' },
  { to: '/activity', label: 'Activity' },
];

/**
 * The one persistent chrome in the app: where you are, who you are, and the theme switch. Signing
 * out ends the Keycloak session rather than only dropping the local token, so the next visit is a
 * real login instead of a silent re-entry.
 */
export function TopBar() {
  const auth = useAuth();
  const { theme, toggle } = useTheme();

  return (
    <header className="topbar">
      <div className="brand">SpecForge</div>
      <nav className="nav">
        {SCREENS.map((screen) => (
          <NavLink key={screen.to} to={screen.to} end={screen.to === '/'}>
            {({ isActive }) => <span className={isActive ? 'on' : undefined}>{screen.label}</span>}
          </NavLink>
        ))}
      </nav>
      <div className="spacer" />
      <button type="button" className="iconbtn" onClick={toggle} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}>
        {theme === 'dark' ? '☀' : '☾'}
      </button>
      {auth.isAuthenticated && (
        <>
          <span style={{ color: 'var(--fg-2)' }}>{auth.user?.profile.name ?? auth.user?.profile.preferred_username}</span>
          <button type="button" className="iconbtn" style={{ width: 'auto', padding: '0 10px' }} onClick={() => void auth.signoutRedirect()}>
            Sign out
          </button>
        </>
      )}
    </header>
  );
}
