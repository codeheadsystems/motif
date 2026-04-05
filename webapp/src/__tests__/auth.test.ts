/**
 * @vitest-environment jsdom
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';

// auth.ts no longer reads sessionStorage at module load, but we still reset
// modules between tests to get a clean credential state.
async function loadAuth() {
  vi.resetModules();
  const mod = await import('../auth');
  return mod;
}

describe('isLoggedIn', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('returns false when no credential ID is set', async () => {
    const { isLoggedIn } = await loadAuth();
    expect(isLoggedIn()).toBe(false);
  });
});

describe('getCredentialId', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('returns null when not logged in', async () => {
    const { getCredentialId } = await loadAuth();
    expect(getCredentialId()).toBeNull();
  });

  it('returns the owner value after successful validateToken', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ value: 'ALICE', identifier: { uuid: '123' }, deleted: false }),
    }));
    const { validateToken, getCredentialId } = await loadAuth();

    const valid = await validateToken();

    expect(valid).toBe(true);
    expect(getCredentialId()).toBe('ALICE');
  });
});

describe('validateToken', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('returns true and sets credentialId when server returns 200', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ value: 'BOB', identifier: { uuid: '456' }, deleted: false }),
    }));
    const { validateToken, isLoggedIn, getCredentialId } = await loadAuth();

    const result = await validateToken();

    expect(result).toBe(true);
    expect(isLoggedIn()).toBe(true);
    expect(getCredentialId()).toBe('BOB');
  });

  it('returns false when server returns 401', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
    }));
    const { validateToken, isLoggedIn } = await loadAuth();

    const result = await validateToken();

    expect(result).toBe(false);
    expect(isLoggedIn()).toBe(false);
  });

  it('returns false on network error without clearing state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Network error')));
    const { validateToken } = await loadAuth();

    const result = await validateToken();

    expect(result).toBe(false);
  });
});

describe('logout', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('calls DELETE /api/session and clears credentialId', async () => {
    const mockFetch = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ value: 'ALICE' }) })
      .mockResolvedValueOnce({ ok: true });
    vi.stubGlobal('fetch', mockFetch);
    const { validateToken, logout, isLoggedIn } = await loadAuth();
    await validateToken();
    expect(isLoggedIn()).toBe(true);

    await logout();

    expect(isLoggedIn()).toBe(false);
    // Second call should be DELETE /api/session
    expect(mockFetch).toHaveBeenCalledWith('/api/session', {
      method: 'DELETE',
      credentials: 'same-origin',
    });
  });
});
