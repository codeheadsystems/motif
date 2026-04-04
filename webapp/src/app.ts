import { isLoggedIn, getCredentialId, logout } from './auth';
import * as api from './api';

const content = document.getElementById('app-content')!;
const navUser = document.getElementById('nav-user')!;
const navUsername = document.getElementById('nav-username')!;
const btnLogout = document.getElementById('btn-logout')!;

btnLogout.addEventListener('click', () => {
  logout();
  render();
});

export function render(): void {
  if (isLoggedIn()) {
    navUser.classList.remove('d-none');
    navUsername.textContent = getCredentialId() ?? 'User';
    renderDashboard();
  } else {
    navUser.classList.add('d-none');
    renderAuthPage();
  }
}

function renderAuthPage(): void {
  content.innerHTML = `
    <div class="row justify-content-center mt-5">
      <div class="col-md-5">
        <div class="card">
          <div class="card-header">
            <ul class="nav nav-tabs card-header-tabs" role="tablist">
              <li class="nav-item">
                <button class="nav-link active" data-tab="login">Login</button>
              </li>
              <li class="nav-item">
                <button class="nav-link" data-tab="register">Register</button>
              </li>
            </ul>
          </div>
          <div class="card-body">
            <div id="tab-login">
              <form id="form-login">
                <div class="mb-3">
                  <label class="form-label">Username</label>
                  <input type="text" class="form-control" id="login-user" required />
                </div>
                <div class="mb-3">
                  <label class="form-label">Password</label>
                  <input type="password" class="form-control" id="login-pass" required />
                </div>
                <button type="submit" class="btn btn-primary w-100">Login</button>
              </form>
            </div>
            <div id="tab-register" class="d-none">
              <form id="form-register">
                <div class="mb-3">
                  <label class="form-label">Username</label>
                  <input type="text" class="form-control" id="reg-user" required />
                </div>
                <div class="mb-3">
                  <label class="form-label">Password</label>
                  <input type="password" class="form-control" id="reg-pass" required />
                </div>
                <div class="mb-3">
                  <label class="form-label">Confirm Password</label>
                  <input type="password" class="form-control" id="reg-confirm" required />
                </div>
                <button type="submit" class="btn btn-success w-100">Register</button>
              </form>
            </div>
            <div id="auth-message" class="mt-3"></div>
          </div>
        </div>
      </div>
    </div>`;

  // Tab switching
  content.querySelectorAll('[data-tab]').forEach(btn => {
    btn.addEventListener('click', () => {
      content.querySelectorAll('[data-tab]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const tab = (btn as HTMLElement).dataset.tab!;
      document.getElementById('tab-login')!.classList.toggle('d-none', tab !== 'login');
      document.getElementById('tab-register')!.classList.toggle('d-none', tab !== 'register');
    });
  });

  bindAuthForms();
}

function bindAuthForms(): void {
  const msg = document.getElementById('auth-message')!;

  document.getElementById('form-login')!.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.innerHTML = '<div class="text-muted">Authenticating...</div>';
    try {
      const { login } = await import('./auth');
      const user = (document.getElementById('login-user') as HTMLInputElement).value;
      const pass = (document.getElementById('login-pass') as HTMLInputElement).value;
      await login(user, pass);
      render();
    } catch (err) {
      msg.innerHTML = `<div class="alert alert-danger">Login failed: ${(err as Error).message}</div>`;
    }
  });

  document.getElementById('form-register')!.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.innerHTML = '';
    const user = (document.getElementById('reg-user') as HTMLInputElement).value;
    const pass = (document.getElementById('reg-pass') as HTMLInputElement).value;
    const confirm = (document.getElementById('reg-confirm') as HTMLInputElement).value;
    if (pass !== confirm) {
      msg.innerHTML = '<div class="alert alert-warning">Passwords do not match</div>';
      return;
    }
    msg.innerHTML = '<div class="text-muted">Registering...</div>';
    try {
      const { register } = await import('./auth');
      await register(user, pass);
      msg.innerHTML = '<div class="alert alert-success">Registration successful! You can now log in.</div>';
      // Switch to login tab
      content.querySelector('[data-tab="login"]')?.dispatchEvent(new Event('click'));
    } catch (err) {
      msg.innerHTML = `<div class="alert alert-danger">Registration failed: ${(err as Error).message}</div>`;
    }
  });
}

async function renderDashboard(): Promise<void> {
  content.innerHTML = `
    <div class="row g-0">
      <div class="col-md-3 bg-light sidebar p-3">
        <h5>Subjects</h5>
        <div class="mb-3">
          <div class="input-group input-group-sm">
            <input type="text" class="form-control" id="category-input" placeholder="Category" />
            <button class="btn btn-outline-secondary" id="btn-load-category">Load</button>
          </div>
        </div>
        <div class="mb-3">
          <div class="input-group input-group-sm">
            <input type="text" class="form-control" id="new-subject-value" placeholder="New subject" />
            <button class="btn btn-outline-primary" id="btn-add-subject">Add</button>
          </div>
        </div>
        <div id="subject-list"></div>
      </div>
      <div class="col-md-9 p-3">
        <div id="dashboard-welcome">
          <h4>Welcome</h4>
          <p>Credential: <code>${getCredentialId()}</code></p>
          <p class="text-muted">Select a category and subject from the sidebar to browse events and notes.</p>
        </div>
        <div id="entity-detail" class="d-none">
          <h5 id="detail-title"></h5>
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item"><button class="nav-link active" data-detail="events">Events</button></li>
            <li class="nav-item"><button class="nav-link" data-detail="notes">Notes</button></li>
          </ul>
          <div id="detail-events"></div>
          <div id="detail-notes" class="d-none"></div>
        </div>
      </div>
    </div>`;

  // Ensure owner exists
  try {
    await api.getOwner();
  } catch {
    // Owner creation might fail if server is down
  }

  bindDashboard();
}

function bindDashboard(): void {
  let currentCategory = '';
  let currentSubject: api.Subject | null = null;

  document.getElementById('btn-load-category')!.addEventListener('click', async () => {
    currentCategory = (document.getElementById('category-input') as HTMLInputElement).value;
    if (!currentCategory) return;
    await loadSubjects();
  });

  document.getElementById('btn-add-subject')!.addEventListener('click', async () => {
    const value = (document.getElementById('new-subject-value') as HTMLInputElement).value;
    if (!value || !currentCategory) return;
    await api.createSubject(currentCategory, value);
    (document.getElementById('new-subject-value') as HTMLInputElement).value = '';
    await loadSubjects();
  });

  // Detail tab switching
  content.querySelectorAll('[data-detail]').forEach(btn => {
    btn.addEventListener('click', () => {
      content.querySelectorAll('[data-detail]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const tab = (btn as HTMLElement).dataset.detail!;
      document.getElementById('detail-events')!.classList.toggle('d-none', tab !== 'events');
      document.getElementById('detail-notes')!.classList.toggle('d-none', tab !== 'notes');
    });
  });

  async function loadSubjects(): Promise<void> {
    const list = document.getElementById('subject-list')!;
    try {
      const page = await api.getSubjects(currentCategory);
      list.innerHTML = page.items.map(s => `
        <div class="card entity-card mb-1" data-subject-id="${s.identifier.uuid}">
          <div class="card-body py-2 px-3 d-flex justify-content-between align-items-center">
            <small>${s.value}</small>
            <button class="btn btn-outline-danger btn-sm delete-subject" data-id="${s.identifier.uuid}">&times;</button>
          </div>
        </div>`).join('');

      list.querySelectorAll('.entity-card').forEach(card => {
        card.addEventListener('click', (e) => {
          if ((e.target as HTMLElement).classList.contains('delete-subject')) return;
          const id = (card as HTMLElement).dataset.subjectId!;
          currentSubject = page.items.find(s => s.identifier.uuid === id) ?? null;
          if (currentSubject) showSubjectDetail(currentSubject);
        });
      });

      list.querySelectorAll('.delete-subject').forEach(btn => {
        btn.addEventListener('click', async (e) => {
          e.stopPropagation();
          await api.deleteSubject((btn as HTMLElement).dataset.id!);
          await loadSubjects();
        });
      });
    } catch {
      list.innerHTML = '<p class="text-danger">Failed to load subjects</p>';
    }
  }

  async function showSubjectDetail(subject: api.Subject): Promise<void> {
    document.getElementById('dashboard-welcome')!.classList.add('d-none');
    document.getElementById('entity-detail')!.classList.remove('d-none');
    document.getElementById('detail-title')!.textContent = `${subject.category.value} / ${subject.value}`;
    await loadEvents(subject);
    await loadNotes(subject);
  }

  async function loadEvents(subject: api.Subject): Promise<void> {
    const container = document.getElementById('detail-events')!;
    try {
      const page = await api.getEvents(subject.identifier.uuid);
      container.innerHTML = `
        <div class="mb-2">
          <div class="input-group input-group-sm">
            <input type="text" class="form-control" id="new-event-value" placeholder="New event" />
            <input type="text" class="form-control" id="new-event-tags" placeholder="Tags (comma-sep)" />
            <button class="btn btn-outline-primary" id="btn-add-event">Add</button>
          </div>
        </div>
        <div class="list-group">
          ${page.items.map(e => `
            <div class="list-group-item d-flex justify-content-between">
              <div>
                <strong>${e.value}</strong>
                ${e.tags.length ? `<br/><small class="text-muted">${e.tags.map(t => t.value).join(', ')}</small>` : ''}
              </div>
              <button class="btn btn-outline-danger btn-sm delete-event" data-id="${e.identifier.uuid}">&times;</button>
            </div>`).join('')}
        </div>`;

      document.getElementById('btn-add-event')?.addEventListener('click', async () => {
        const value = (document.getElementById('new-event-value') as HTMLInputElement).value;
        const tagsStr = (document.getElementById('new-event-tags') as HTMLInputElement).value;
        const tags = tagsStr ? tagsStr.split(',').map(t => t.trim()).filter(Boolean) : [];
        if (!value) return;
        await api.createEvent(subject.identifier.uuid, value, tags);
        await loadEvents(subject);
      });

      container.querySelectorAll('.delete-event').forEach(btn => {
        btn.addEventListener('click', async () => {
          await api.deleteEvent((btn as HTMLElement).dataset.id!);
          await loadEvents(subject);
        });
      });
    } catch {
      container.innerHTML = '<p class="text-danger">Failed to load events</p>';
    }
  }

  async function loadNotes(subject: api.Subject): Promise<void> {
    const container = document.getElementById('detail-notes')!;
    try {
      const page = await api.getNotes(subject.identifier.uuid);
      container.innerHTML = `
        <div class="mb-2">
          <div class="input-group input-group-sm">
            <input type="text" class="form-control" id="new-note-value" placeholder="New note" />
            <button class="btn btn-outline-primary" id="btn-add-note">Add</button>
          </div>
        </div>
        <div class="list-group">
          ${page.items.map(n => `
            <div class="list-group-item d-flex justify-content-between">
              <div>${n.value}</div>
              <button class="btn btn-outline-danger btn-sm delete-note" data-id="${n.identifier.uuid}">&times;</button>
            </div>`).join('')}
        </div>`;

      document.getElementById('btn-add-note')?.addEventListener('click', async () => {
        const value = (document.getElementById('new-note-value') as HTMLInputElement).value;
        if (!value) return;
        await api.createNote(subject.identifier.uuid, value);
        await loadNotes(subject);
      });

      container.querySelectorAll('.delete-note').forEach(btn => {
        btn.addEventListener('click', async () => {
          await api.deleteNote((btn as HTMLElement).dataset.id!);
          await loadNotes(subject);
        });
      });
    } catch {
      container.innerHTML = '<p class="text-danger">Failed to load notes</p>';
    }
  }
}
