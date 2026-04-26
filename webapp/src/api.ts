import { logout } from './auth';

async function apiFetch(path: string, options?: RequestInit): Promise<Response> {
  const res = await fetch(path, {
    ...options,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });
  if (res.status === 401) {
    await logout();
    window.location.reload();
    throw new Error('Session expired');
  }
  if (!res.ok) {
    let detail = '';
    try {
      const body = await res.clone().json();
      detail = (body && (body.message || body.error)) ?? '';
    } catch {
      detail = await res.clone().text().catch(() => '');
    }
    throw new Error(`API ${res.status} ${path}${detail ? `: ${detail}` : ''}`);
  }
  return res;
}

export interface Owner {
  value: string;
  identifier: { uuid: string };
  deleted: boolean;
}

export interface Category {
  ownerIdentifier: { uuid: string };
  identifier: { uuid: string };
  name: string;
  color: string;
  icon: string;
}

export interface Subject {
  ownerIdentifier: { uuid: string };
  categoryIdentifier: { uuid: string };
  value: string;
  identifier: { uuid: string };
}

export interface Event {
  ownerIdentifier: { uuid: string };
  subject: Subject;
  value: string;
  identifier: { uuid: string };
  timestamp: { timestamp: string };
  tags: { value: string }[];
}

export interface Note {
  ownerIdentifier: { uuid: string };
  subjectIdentifier: { uuid: string };
  value: string;
  identifier: { uuid: string };
  eventIdentifier: { uuid: string } | null;
  timestamp: { timestamp: string };
  tags: { value: string }[];
}

export interface Page<T> {
  items: T[];
  pageNumber: number;
  pageSize: number;
  hasMore: boolean;
}

export async function getOwner(): Promise<Owner> {
  const res = await apiFetch('/api/owner');
  return res.json();
}

// --- categories ---

export async function getCategories(page = 0, size = 100): Promise<Page<Category>> {
  const res = await apiFetch(`/api/categories?page=${page}&size=${size}`);
  return res.json();
}

export async function getCategory(id: string): Promise<Category> {
  const res = await apiFetch(`/api/categories/${id}`);
  return res.json();
}

export async function createCategory(name: string, color: string, icon: string): Promise<Category> {
  const res = await apiFetch('/api/categories', {
    method: 'POST',
    body: JSON.stringify({ name, color, icon }),
  });
  return res.json();
}

export async function updateCategory(
  id: string,
  name: string,
  color: string,
  icon: string,
): Promise<Category> {
  const res = await apiFetch(`/api/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name, color, icon }),
  });
  return res.json();
}

export async function deleteCategory(id: string): Promise<void> {
  await apiFetch(`/api/categories/${id}`, { method: 'DELETE' });
}

// --- subjects ---

export async function getSubjects(categoryId: string, page = 0, size = 50): Promise<Page<Subject>> {
  const res = await apiFetch(`/api/subjects?category=${categoryId}&page=${page}&size=${size}`);
  return res.json();
}

export async function getSubject(id: string): Promise<Subject> {
  const res = await apiFetch(`/api/subjects/${id}`);
  return res.json();
}

export async function createSubject(categoryId: string, value: string): Promise<Subject> {
  const res = await apiFetch('/api/subjects', {
    method: 'POST',
    body: JSON.stringify({ categoryId, value }),
  });
  return res.json();
}

export async function deleteSubject(id: string): Promise<void> {
  await apiFetch(`/api/subjects/${id}`, { method: 'DELETE' });
}

// --- events ---

export async function getRecentEvents(size = 20): Promise<Page<Event>> {
  const res = await apiFetch(`/api/events/recent?size=${size}`);
  return res.json();
}

export async function getEvents(subjectId: string, page = 0, size = 50): Promise<Page<Event>> {
  const res = await apiFetch(`/api/events?subject=${subjectId}&page=${page}&size=${size}`);
  return res.json();
}

export async function createEvent(subjectId: string, value: string, tags: string[] = []): Promise<Event> {
  const res = await apiFetch('/api/events', {
    method: 'POST',
    body: JSON.stringify({ subjectId, value, tags }),
  });
  return res.json();
}

export async function deleteEvent(id: string): Promise<void> {
  await apiFetch(`/api/events/${id}`, { method: 'DELETE' });
}

// --- notes ---

export async function getRecentNotes(size = 20): Promise<Page<Note>> {
  const res = await apiFetch(`/api/notes/recent?size=${size}`);
  return res.json();
}

export async function getNotes(subjectId: string, page = 0, size = 50): Promise<Page<Note>> {
  const res = await apiFetch(`/api/notes?subject=${subjectId}&page=${page}&size=${size}`);
  return res.json();
}

export async function createNote(subjectId: string, value: string, tags: string[] = [], eventId?: string): Promise<Note> {
  const res = await apiFetch('/api/notes', {
    method: 'POST',
    body: JSON.stringify({ subjectId, value, tags, eventId: eventId ?? null }),
  });
  return res.json();
}

export async function deleteNote(id: string): Promise<void> {
  await apiFetch(`/api/notes/${id}`, { method: 'DELETE' });
}
