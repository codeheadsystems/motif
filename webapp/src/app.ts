import { isLoggedIn, getCredentialId, logout, changePassword, getToken } from './auth';
import * as api from './api';

declare const bootstrap: { Modal: new (el: Element) => { show(): void; hide(): void } };

const content = document.getElementById('app-content')!;
const navUser = document.getElementById('nav-user')!;
const navUsername = document.getElementById('nav-username')!;

// Logout
document.getElementById('btn-logout')!.addEventListener('click', () => {
  logout();
  render();
});

// Profile modal
document.getElementById('btn-profile')!.addEventListener('click', () => {
  const credId = getCredentialId() ?? 'Unknown';
  const token = getToken();
  let jwtInfo = '';
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const issuedAt = payload.iat ? new Date(payload.iat * 1000).toLocaleString() : 'N/A';
      const expiresAt = payload.exp ? new Date(payload.exp * 1000).toLocaleString() : 'N/A';
      jwtInfo = `
        <tr><td class="text-muted">Issued</td><td>${issuedAt}</td></tr>
        <tr><td class="text-muted">Expires</td><td>${expiresAt}</td></tr>
        <tr><td class="text-muted">Issuer</td><td>${payload.iss ?? 'N/A'}</td></tr>`;
    } catch { /* ignore */ }
  }
  document.getElementById('profile-body')!.innerHTML = `
    <table class="table table-sm mb-0">
      <tbody>
        <tr><td class="text-muted">Credential ID</td><td><code>${credId}</code></td></tr>
        ${jwtInfo}
      </tbody>
    </table>`;
  new bootstrap.Modal(document.getElementById('modal-profile')!).show();
});

// Change password modal
document.getElementById('btn-change-password')!.addEventListener('click', () => {
  (document.getElementById('cp-old-pass') as HTMLInputElement).value = '';
  (document.getElementById('cp-new-pass') as HTMLInputElement).value = '';
  (document.getElementById('cp-confirm-pass') as HTMLInputElement).value = '';
  document.getElementById('cp-message')!.innerHTML = '';
  new bootstrap.Modal(document.getElementById('modal-change-password')!).show();
});

document.getElementById('form-change-password')!.addEventListener('submit', async (e) => {
  e.preventDefault();
  const msg = document.getElementById('cp-message')!;
  const oldPass = (document.getElementById('cp-old-pass') as HTMLInputElement).value;
  const newPass = (document.getElementById('cp-new-pass') as HTMLInputElement).value;
  const confirmPass = (document.getElementById('cp-confirm-pass') as HTMLInputElement).value;
  if (!oldPass) {
    msg.innerHTML = '<div class="alert alert-warning py-1 small">Current password is required</div>';
    return;
  }
  if (newPass !== confirmPass) {
    msg.innerHTML = '<div class="alert alert-warning py-1 small">New passwords do not match</div>';
    return;
  }
  if (newPass.length < 1) {
    msg.innerHTML = '<div class="alert alert-warning py-1 small">New password cannot be empty</div>';
    return;
  }
  msg.innerHTML = '<div class="text-muted small">Verifying current password and changing...</div>';
  try {
    await changePassword(oldPass, newPass);
    msg.innerHTML = '<div class="alert alert-success py-1 small">Password changed successfully</div>';
    (document.getElementById('cp-old-pass') as HTMLInputElement).value = '';
    (document.getElementById('cp-new-pass') as HTMLInputElement).value = '';
    (document.getElementById('cp-confirm-pass') as HTMLInputElement).value = '';
  } catch (err) {
    msg.innerHTML = `<div class="alert alert-danger py-1 small">${(err as Error).message}</div>`;
  }
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
      content.querySelector('[data-tab="login"]')?.dispatchEvent(new Event('click'));
    } catch (err) {
      msg.innerHTML = `<div class="alert alert-danger">Registration failed: ${(err as Error).message}</div>`;
    }
  });
}

async function renderDashboard(): Promise<void> {
  content.innerHTML = `
    <div class="row g-0">
      <div class="col-md-3 bg-light sidebar p-3 border-end">
        <h6 class="text-uppercase text-muted mb-3">Categories</h6>
        <form id="form-add-category" class="mb-3">
          <div class="input-group input-group-sm">
            <input type="text" class="form-control" id="new-category-name" placeholder="New category (e.g. House)" required />
            <button class="btn btn-outline-primary" type="submit">+</button>
          </div>
        </form>
        <div id="category-list"></div>
      </div>
      <div class="col-md-9 p-3">
        <div id="dashboard-home">
          <!-- Quick-Add Form -->
          <div class="card mb-3">
            <div class="card-body">
              <h6 class="card-title text-muted text-uppercase mb-3">Quick Add</h6>
              <form id="form-quick-add">
                <div class="row g-2 align-items-end">
                  <div class="col-auto">
                    <div class="btn-group btn-group-sm" role="group">
                      <input type="radio" class="btn-check" name="qa-type" id="qa-type-event" value="event" checked />
                      <label class="btn btn-outline-primary" for="qa-type-event">Event</label>
                      <input type="radio" class="btn-check" name="qa-type" id="qa-type-note" value="note" />
                      <label class="btn btn-outline-primary" for="qa-type-note">Note</label>
                    </div>
                  </div>
                  <div class="col">
                    <input list="qa-categories" class="form-control form-control-sm" id="qa-category" placeholder="Category" required />
                    <datalist id="qa-categories"></datalist>
                  </div>
                  <div class="col">
                    <input list="qa-subjects" class="form-control form-control-sm" id="qa-subject" placeholder="Subject" required />
                    <datalist id="qa-subjects"></datalist>
                  </div>
                  <div class="col-3">
                    <input type="text" class="form-control form-control-sm" id="qa-value" placeholder="Description" required />
                  </div>
                  <div class="col" id="qa-tags-col">
                    <input type="text" class="form-control form-control-sm" id="qa-tags" placeholder="Tags (comma-sep)" />
                  </div>
                  <div class="col-auto">
                    <button type="submit" class="btn btn-primary btn-sm">Add</button>
                  </div>
                </div>
              </form>
              <div id="qa-message" class="mt-2"></div>
            </div>
          </div>
          <!-- Activity Timeline -->
          <h6 class="text-muted text-uppercase mb-2">Recent Activity</h6>
          <div id="timeline"></div>
        </div>
        <div id="subject-panel" class="d-none">
          <div class="d-flex align-items-center mb-3">
            <h5 class="mb-0" id="subject-panel-title"></h5>
          </div>
          <form id="form-add-subject" class="mb-3">
            <div class="input-group input-group-sm">
              <input type="text" class="form-control" id="new-subject-value" placeholder="New subject (e.g. Water Heater)" required />
              <button class="btn btn-outline-primary" type="submit">Add Subject</button>
            </div>
          </form>
          <div id="subject-list"></div>
        </div>
        <div id="entity-detail" class="d-none">
          <div class="d-flex align-items-center mb-3">
            <button class="btn btn-outline-secondary btn-sm me-2" id="btn-back-to-subjects">&larr; Back</button>
            <h5 class="mb-0" id="detail-title"></h5>
          </div>
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item"><button class="nav-link active" data-detail="events">Events</button></li>
            <li class="nav-item"><button class="nav-link" data-detail="notes">Notes</button></li>
          </ul>
          <div id="detail-events"></div>
          <div id="detail-notes" class="d-none"></div>
        </div>
      </div>
    </div>`;

  try {
    await api.getOwner();
  } catch {
    // ignore
  }

  bindDashboard();
}

function bindDashboard(): void {
  let currentCategory = '';
  let currentSubject: api.Subject | null = null;
  let cachedCategories: { value: string }[] = [];

  // Load categories + timeline on init
  loadCategories();
  loadTimeline();
  populateQuickAddCategories();

  // Quick-add: populate subjects when category changes
  document.getElementById('qa-category')!.addEventListener('change', populateQuickAddSubjects);
  document.getElementById('qa-category')!.addEventListener('input', populateQuickAddSubjects);

  // Quick-add: toggle tags field visibility based on type
  document.querySelectorAll('input[name="qa-type"]').forEach(radio => {
    radio.addEventListener('change', () => {
      const isEvent = (document.getElementById('qa-type-event') as HTMLInputElement).checked;
      document.getElementById('qa-tags-col')!.classList.toggle('d-none', !isEvent);
    });
  });

  // Quick-add: submit
  document.getElementById('form-quick-add')!.addEventListener('submit', async (e) => {
    e.preventDefault();
    const msg = document.getElementById('qa-message')!;
    const isEvent = (document.getElementById('qa-type-event') as HTMLInputElement).checked;
    const category = (document.getElementById('qa-category') as HTMLInputElement).value.trim();
    const subjectValue = (document.getElementById('qa-subject') as HTMLInputElement).value.trim();
    const value = (document.getElementById('qa-value') as HTMLInputElement).value.trim();
    const tagsStr = (document.getElementById('qa-tags') as HTMLInputElement).value;

    if (!category || !subjectValue || !value) return;

    try {
      // Find or create subject
      let subjects = (await api.getSubjects(category)).items;
      let subject = subjects.find(s => s.value === subjectValue);
      if (!subject) {
        subject = await api.createSubject(category, subjectValue);
      }

      if (isEvent) {
        const tags = tagsStr ? tagsStr.split(',').map(t => t.trim()).filter(Boolean) : [];
        await api.createEvent(subject.identifier.uuid, value, tags);
      } else {
        await api.createNote(subject.identifier.uuid, value);
      }

      // Clear form and refresh
      (document.getElementById('qa-value') as HTMLInputElement).value = '';
      (document.getElementById('qa-tags') as HTMLInputElement).value = '';
      msg.innerHTML = `<div class="alert alert-success alert-dismissible py-1 small">${isEvent ? 'Event' : 'Note'} added to ${category} / ${subjectValue}</div>`;
      setTimeout(() => { msg.innerHTML = ''; }, 3000);
      await loadTimeline();
      await loadCategories();
      await populateQuickAddCategories();
    } catch (err) {
      msg.innerHTML = `<div class="alert alert-danger py-1 small">Failed: ${(err as Error).message}</div>`;
    }
  });

  // Add category form
  document.getElementById('form-add-category')!.addEventListener('submit', async (e) => {
    e.preventDefault();
    const input = document.getElementById('new-category-name') as HTMLInputElement;
    const name = input.value.trim();
    if (!name) return;
    input.value = '';
    currentCategory = name;
    showSubjectPanel(name);
    await loadCategories();
    await populateQuickAddCategories();
  });

  // Back button — return to home view
  document.getElementById('btn-back-to-subjects')!.addEventListener('click', () => {
    document.getElementById('entity-detail')!.classList.add('d-none');
    document.getElementById('subject-panel')!.classList.remove('d-none');
    currentSubject = null;
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

  async function populateQuickAddCategories(): Promise<void> {
    try {
      cachedCategories = await api.getCategories();
      const datalist = document.getElementById('qa-categories')!;
      datalist.innerHTML = cachedCategories.map(c => `<option value="${c.value}">`).join('');
    } catch { /* ignore */ }
  }

  async function populateQuickAddSubjects(): Promise<void> {
    const category = (document.getElementById('qa-category') as HTMLInputElement).value.trim();
    const datalist = document.getElementById('qa-subjects')!;
    if (!category) { datalist.innerHTML = ''; return; }
    try {
      const page = await api.getSubjects(category);
      datalist.innerHTML = page.items.map(s => `<option value="${s.value}">`).join('');
    } catch { datalist.innerHTML = ''; }
  }

  async function loadTimeline(): Promise<void> {
    const container = document.getElementById('timeline')!;
    try {
      const [events, notes] = await Promise.all([
        api.getRecentEvents(20),
        api.getRecentNotes(20),
      ]);

      type TimelineItem = { type: 'event' | 'note'; label: string; value: string; timestamp: string; tags: string[]; subjectId: string; category: string; subject: string };
      const items: TimelineItem[] = [];

      for (const e of events.items) {
        items.push({
          type: 'event',
          label: `${e.subject.category.value} / ${e.subject.value}`,
          value: e.value,
          timestamp: e.timestamp.timestamp,
          tags: e.tags.map(t => t.value),
          subjectId: e.subject.identifier.uuid,
          category: e.subject.category.value,
          subject: e.subject.value,
        });
      }
      for (const n of notes.items) {
        items.push({
          type: 'note',
          label: n.subjectIdentifier.uuid,
          value: n.value,
          timestamp: n.timestamp.timestamp,
          tags: n.tags.map(t => t.value),
          subjectId: n.subjectIdentifier.uuid,
          category: '',
          subject: '',
        });
      }

      items.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());

      if (items.length === 0) {
        container.innerHTML = '<p class="text-muted">No activity yet. Use the quick-add form above or browse categories in the sidebar.</p>';
        return;
      }

      container.innerHTML = `<div class="list-group">
        ${items.slice(0, 30).map(item => {
          const badge = item.type === 'event'
            ? '<span class="badge bg-primary me-2">Event</span>'
            : '<span class="badge bg-secondary me-2">Note</span>';
          const tagStr = item.tags.length ? ` <small class="text-muted">[${item.tags.join(', ')}]</small>` : '';
          const time = new Date(item.timestamp).toLocaleString();
          const label = item.type === 'event' ? item.label : item.label.substring(0, 8) + '...';
          return `<div class="list-group-item">
            <div class="d-flex justify-content-between align-items-start">
              <div>${badge}<strong>${item.value}</strong>${tagStr}</div>
              <small class="text-muted text-nowrap ms-2">${time}</small>
            </div>
            <small class="text-muted">${item.type === 'event' ? item.label : ''}</small>
          </div>`;
        }).join('')}
      </div>`;
    } catch {
      container.innerHTML = '<p class="text-danger small">Failed to load recent activity</p>';
    }
  }

  async function loadCategories(): Promise<void> {
    const list = document.getElementById('category-list')!;
    try {
      const categories = await api.getCategories();
      if (categories.length === 0) {
        list.innerHTML = '<p class="text-muted small">No categories yet. Create one above.</p>';
        return;
      }
      list.innerHTML = categories.map(c => `
        <button class="btn btn-sm w-100 text-start mb-1 ${c.value === currentCategory ? 'btn-primary' : 'btn-outline-secondary'}"
                data-category="${c.value}">
          ${c.value}
        </button>`).join('');

      list.querySelectorAll('[data-category]').forEach(btn => {
        btn.addEventListener('click', () => {
          const cat = (btn as HTMLElement).dataset.category!;
          currentCategory = cat;
          showSubjectPanel(cat);
          // Update active state
          list.querySelectorAll('[data-category]').forEach(b => {
            b.classList.toggle('btn-primary', (b as HTMLElement).dataset.category === cat);
            b.classList.toggle('btn-outline-secondary', (b as HTMLElement).dataset.category !== cat);
          });
        });
      });
    } catch {
      list.innerHTML = '<p class="text-danger small">Failed to load categories</p>';
    }
  }

  function showSubjectPanel(category: string): void {
    document.getElementById('dashboard-home')!.classList.add('d-none');
    document.getElementById('entity-detail')!.classList.add('d-none');
    const panel = document.getElementById('subject-panel')!;
    panel.classList.remove('d-none');
    document.getElementById('subject-panel-title')!.textContent = category;

    // Rebind add subject form for this category
    const form = document.getElementById('form-add-subject')!;
    const newForm = form.cloneNode(true) as HTMLElement;
    form.parentNode!.replaceChild(newForm, form);
    newForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const input = document.getElementById('new-subject-value') as HTMLInputElement;
      const value = input.value.trim();
      if (!value) return;
      await api.createSubject(category, value);
      input.value = '';
      await loadSubjects(category);
      await loadCategories();
    });

    loadSubjects(category);
  }

  async function loadSubjects(category: string): Promise<void> {
    const list = document.getElementById('subject-list')!;
    try {
      const page = await api.getSubjects(category);
      if (page.items.length === 0) {
        list.innerHTML = '<p class="text-muted small">No subjects in this category. Add one above.</p>';
        return;
      }
      list.innerHTML = page.items.map(s => `
        <div class="card mb-1 entity-card" data-subject-id="${s.identifier.uuid}">
          <div class="card-body py-2 px-3 d-flex justify-content-between align-items-center">
            <span>${s.value}</span>
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
          await loadSubjects(category);
          await loadCategories();
        });
      });
    } catch {
      list.innerHTML = '<p class="text-danger small">Failed to load subjects</p>';
    }
  }

  function showSubjectDetail(subject: api.Subject): void {
    document.getElementById('subject-panel')!.classList.add('d-none');
    document.getElementById('entity-detail')!.classList.remove('d-none');
    document.getElementById('detail-title')!.textContent = `${subject.category.value} / ${subject.value}`;
    loadEvents(subject);
    loadNotes(subject);
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
          ${page.items.length === 0 ? '<p class="text-muted small mt-2">No events yet.</p>' : ''}
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
          ${page.items.length === 0 ? '<p class="text-muted small mt-2">No notes yet.</p>' : ''}
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
