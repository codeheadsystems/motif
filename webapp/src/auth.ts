import { OpaqueHttpClient } from '@codeheadsystems/hofmann-typescript';

let client: OpaqueHttpClient | null = null;
let credentialId: string | null = null;

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

export async function login(user: string, password: string): Promise<void> {
  const c = await getClient();
  const token = await c.authenticate(user, password);
  // Persist the JWT in an HttpOnly cookie via the server
  const res = await fetch('/api/session', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!res.ok) {
    throw new Error('Failed to establish session');
  }
  credentialId = user;
}

export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  if (!credentialId) throw new Error('Not authenticated');
  const c = await getClient();
  // Verify the old password by authenticating — throws if wrong
  let freshToken: string;
  try {
    freshToken = await c.authenticate(credentialId, oldPassword);
  } catch {
    throw new Error('Current password is incorrect');
  }
  // Update the session cookie with the fresh token
  await fetch('/api/session', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${freshToken}` },
  });
  // Old password verified — proceed with change
  await c.changePassword(credentialId, newPassword, freshToken);
}

export function getCredentialId(): string | null {
  return credentialId;
}

export async function logout(): Promise<void> {
  try {
    await fetch('/api/session', { method: 'DELETE', credentials: 'same-origin' });
  } catch {
    // Network error — still clear local state
  }
  credentialId = null;
}

export function isLoggedIn(): boolean {
  return credentialId !== null;
}

/**
 * Validates the session by calling the server with the HttpOnly cookie.
 * If valid, populates the credential ID from the server response.
 * Returns true if the session is valid, false otherwise.
 */
export async function validateToken(): Promise<boolean> {
  try {
    const res = await fetch('/api/owner', {
      credentials: 'same-origin',
    });
    if (res.ok) {
      const owner = await res.json();
      // Owner.value is the credential ID (uppercased by server).
      // We store it for display; OPAQUE operations are done via fresh authenticate() calls.
      credentialId = owner.value;
      return true;
    }
    credentialId = null;
    return false;
  } catch {
    // Network error — don't clear state, may be transient
    return false;
  }
}
