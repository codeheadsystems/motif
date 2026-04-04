import { getToken } from './auth';

async function apiFetch(path: string, options?: RequestInit): Promise<Response> {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  return fetch(path, {
    ...options,
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });
}

export interface Owner {
  value: string;
  identifier: { uuid: string };
  deleted: boolean;
}

export interface Subject {
  ownerIdentifier: { uuid: string };
  category: { value: string };
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

export async function getSubjects(category: string, page = 0, size = 50): Promise<Page<Subject>> {
  const res = await apiFetch(`/api/subjects?category=${encodeURIComponent(category)}&page=${page}&size=${size}`);
  return res.json();
}

export async function createSubject(category: string, value: string): Promise<Subject> {
  const res = await apiFetch('/api/subjects', {
    method: 'POST',
    body: JSON.stringify({ category, value }),
  });
  return res.json();
}

export async function deleteSubject(id: string): Promise<void> {
  await apiFetch(`/api/subjects/${id}`, { method: 'DELETE' });
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
