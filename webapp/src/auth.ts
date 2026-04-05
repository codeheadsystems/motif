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

export function getToken(): string | null {
  return token;
}

export function getCredentialId(): string | null {
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub ?? null;
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
