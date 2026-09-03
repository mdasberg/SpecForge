import type { UserManagerSettings } from 'oidc-client-ts';
import { WebStorageStateStore } from 'oidc-client-ts';

// SpecForge is not an OAuth2 client on the server: the browser logs in against Keycloak and the
// API only ever validates the resulting token. Everything about the login therefore lives here.
export const oidcConfig: UserManagerSettings & { onSigninCallback?: () => void } = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8081/realms/specforge',
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'specforge-web',
  redirect_uri: window.location.origin + '/',
  post_logout_redirect_uri: window.location.origin + '/',
  response_type: 'code',
  scope: 'openid profile email',
  // The refresh token keeps a working session without a full redirect, and the access token is
  // renewed before it expires rather than after a request has already failed.
  automaticSilentRenew: true,
  // Survives a reload; sessionStorage would log the user out on every new tab.
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  // Drop the authorization code from the address bar so a reload cannot replay it.
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
