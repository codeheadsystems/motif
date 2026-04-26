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

export type Tier = 'FREE_SYNCED' | 'PREMIUM' | 'BUSINESS';

export interface Owner {
  value: string;
  identifier: { uuid: string };
  deleted: boolean;
  tier: Tier;
}

/** True if {@code actual} satisfies the requirement of {@code required}. Mirrors Tier.satisfies. */
export function tierSatisfies(actual: Tier, required: Tier): boolean {
  const order: Tier[] = ['FREE_SYNCED', 'PREMIUM', 'BUSINESS'];
  return order.indexOf(actual) >= order.indexOf(required);
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
  projectIdentifier: { uuid: string } | null;
}

export type ProjectStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED';

export interface Project {
  ownerIdentifier: { uuid: string };
  identifier: { uuid: string };
  name: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: { timestamp: string };
  updatedAt: { timestamp: string };
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

// --- projects (Premium) ---

export async function getProjects(page = 0, size = 100): Promise<Page<Project>> {
  const res = await apiFetch(`/api/projects?page=${page}&size=${size}`);
  return res.json();
}

export async function getProject(id: string): Promise<Project> {
  const res = await apiFetch(`/api/projects/${id}`);
  return res.json();
}

export async function createProject(name: string, description: string | null,
                                    status: ProjectStatus = 'ACTIVE'): Promise<Project> {
  const res = await apiFetch('/api/projects', {
    method: 'POST',
    body: JSON.stringify({ name, description, status }),
  });
  return res.json();
}

export async function updateProject(id: string, name: string, description: string | null,
                                    status: ProjectStatus): Promise<Project> {
  const res = await apiFetch(`/api/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name, description, status }),
  });
  return res.json();
}

export async function deleteProject(id: string): Promise<void> {
  await apiFetch(`/api/projects/${id}`, { method: 'DELETE' });
}

// --- workflows (Premium) ---

export interface WorkflowStep {
  identifier: { uuid: string };
  position: number;
  name: string;
  expectedDurationSeconds: number | null;
  notes: string | null;
}

export interface Workflow {
  ownerIdentifier: { uuid: string };
  identifier: { uuid: string };
  name: string;
  description: string | null;
  steps: WorkflowStep[];
  createdAt: { timestamp: string };
  updatedAt: { timestamp: string };
}

export interface WorkflowStepInput {
  name: string;
  expectedDurationSeconds?: number | null;
  notes?: string | null;
}

export async function getWorkflows(page = 0, size = 100): Promise<Page<Workflow>> {
  const res = await apiFetch(`/api/workflows?page=${page}&size=${size}`);
  return res.json();
}

export async function getWorkflow(id: string): Promise<Workflow> {
  const res = await apiFetch(`/api/workflows/${id}`);
  return res.json();
}

export async function createWorkflow(name: string, description: string | null,
                                     steps: WorkflowStepInput[]): Promise<Workflow> {
  const res = await apiFetch('/api/workflows', {
    method: 'POST',
    body: JSON.stringify({ name, description, steps }),
  });
  return res.json();
}

export async function updateWorkflow(id: string, name: string, description: string | null,
                                     steps: WorkflowStepInput[]): Promise<Workflow> {
  const res = await apiFetch(`/api/workflows/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name, description, steps }),
  });
  return res.json();
}

export async function deleteWorkflow(id: string): Promise<void> {
  await apiFetch(`/api/workflows/${id}`, { method: 'DELETE' });
}

export async function createWorkflowFromPattern(patternId: string): Promise<Workflow> {
  const res = await apiFetch(`/api/workflows/from-pattern/${patternId}`, { method: 'POST' });
  return res.json();
}

// --- patterns ---

export type PeriodClassification = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'OTHER';

export interface Pattern {
  ownerIdentifier: { uuid: string };
  subjectIdentifier: { uuid: string };
  identifier: { uuid: string };
  eventValue: string;
  period: PeriodClassification;
  intervalMeanSeconds: number;
  occurrenceCount: number;
  confidence: number;
  lastSeenAt: { timestamp: string };
  nextExpectedAt: { timestamp: string };
  score: number;
  detectedAt: { timestamp: string };
}

export async function getPatterns(limit = 5): Promise<Page<Pattern>> {
  const res = await apiFetch(`/api/patterns?limit=${limit}`);
  return res.json();
}

export async function recomputePatterns(limit = 5): Promise<Page<Pattern>> {
  const res = await apiFetch(`/api/patterns/recompute?limit=${limit}`, { method: 'POST' });
  return res.json();
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
