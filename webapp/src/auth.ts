import { OpaqueHttpClient } from '@codeheadsystems/hofmann-typescript';

const TOKEN_KEY = 'motif_jwt';

let client: OpaqueHttpClient | null = null;
let token: string | null = sessionStorage.getItem(TOKEN_KEY);

export async function getClient(): Promise<OpaqueHttpClient> {
  if (!client) {
    client = await OpaqueHttpClient.create('');
  }
  return client;
}

export async function register(credentialId: string, password: string): Promise<void> {
  const c = await getClient();
  await c.register(credentialId, password);
}

export async function login(credentialId: string, password: string): Promise<string> {
  const c = await getClient();
  token = await c.authenticate(credentialId, password);
  sessionStorage.setItem(TOKEN_KEY, token);
  return token;
}

export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  if (!token) throw new Error('Not authenticated');
  const credId = getCredentialId();
  if (!credId) throw new Error('Cannot determine credential ID');
  const c = await getClient();
  // Verify the old password by authenticating — throws if wrong
  let freshToken: string;
  try {
    freshToken = await c.authenticate(credId, oldPassword);
  } catch {
    throw new Error('Current password is incorrect');
  }
  token = freshToken;
  sessionStorage.setItem(TOKEN_KEY, token);
  // Old password verified — proceed with change
  await c.changePassword(credId, newPassword, token);
}

export function getToken(): string | null {
  return token;
}

/**
 * Returns the original credential ID (username) used during registration.
 * The JWT sub claim is base64(UTF-8 bytes of the username), so we decode it.
 */
export function getCredentialId(): string | null {
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const sub: string | undefined = payload.sub;
    if (!sub) return null;
    // sub is base64-encoded credential identifier — decode to original string
    return new TextDecoder().decode(Uint8Array.from(atob(sub), c => c.charCodeAt(0)));
  } catch {
    return null;
  }
}

export function logout(): void {
  token = null;
  sessionStorage.removeItem(TOKEN_KEY);
}

export function isLoggedIn(): boolean {
  return token !== null;
}

/**
 * Validates the stored token against the server.
 * If the token is expired or the server rejects it, clears the token.
 * Returns true if the token is valid, false otherwise.
 */
export async function validateToken(): Promise<boolean> {
  if (!token) return false;
  try {
    const res = await fetch('/api/owner', {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    if (res.status === 401 || res.status === 403) {
      logout();
      return false;
    }
    return res.ok;
  } catch {
    // Network error — don't clear the token, it may be a transient issue
    return false;
  }
}
