/**
 * @vitest-environment jsdom
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';

// Helper: build a fake JWT with a given sub claim (base64-encoded credential ID)
function fakeJwt(sub: string): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub, iss: 'test', iat: 1700000000, exp: 1700003600 }));
  const signature = 'fakesig';
  return `${header}.${payload}.${signature}`;
}

// auth.ts reads sessionStorage at module load time, so we must reset modules
// between tests to re-evaluate the module-level `let token = sessionStorage.getItem(...)`.
async function loadAuth() {
  vi.resetModules();
  const mod = await import('../auth');
  return mod;
}

describe('getCredentialId', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('decodes base64 sub claim to original username "alice"', async () => {
    const sub = btoa('alice'); // "YWxpY2U="
    sessionStorage.setItem('motif_jwt', fakeJwt(sub));
    const { getCredentialId } = await loadAuth();

    expect(getCredentialId()).toBe('alice');
  });

  it('decodes base64 sub claim to original username "user@example.com"', async () => {
    const sub = btoa('user@example.com');
    sessionStorage.setItem('motif_jwt', fakeJwt(sub));
    const { getCredentialId } = await loadAuth();

    expect(getCredentialId()).toBe('user@example.com');
  });

  it('returns null when no token is set', async () => {
    const { getCredentialId } = await loadAuth();

    expect(getCredentialId()).toBeNull();
  });
});

describe('isLoggedIn', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('returns false when no token', async () => {
    const { isLoggedIn } = await loadAuth();

    expect(isLoggedIn()).toBe(false);
  });

  it('returns true when token exists in sessionStorage', async () => {
    sessionStorage.setItem('motif_jwt', fakeJwt(btoa('alice')));
    const { isLoggedIn } = await loadAuth();

    expect(isLoggedIn()).toBe(true);
  });
});

describe('logout', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('clears token and sessionStorage', async () => {
    sessionStorage.setItem('motif_jwt', fakeJwt(btoa('alice')));
    const { logout, isLoggedIn, getToken } = await loadAuth();

    expect(isLoggedIn()).toBe(true);

    logout();

    expect(isLoggedIn()).toBe(false);
    expect(getToken()).toBeNull();
    expect(sessionStorage.getItem('motif_jwt')).toBeNull();
  });
});
