import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import * as api from '@/api';
import { useProject } from '@/hooks/useApi';

const STATUSES: api.ProjectStatus[] = ['ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED'];

export function ProjectPage() {
  const { projectId: rawId } = useParams<{ projectId: string }>();
  const projectId = rawId ?? '';
  const { data: project, loading, refetch } = useProject(projectId || null);

  const [subjects, setSubjects] = useState<api.Subject[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<api.ProjectStatus>('ACTIVE');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (project) {
      setName(project.name);
      setDescription(project.description ?? '');
      setStatus(project.status);
    }
  }, [project]);

  // Load subjects in this project. (No backend list endpoint by project yet on the resource
  // layer — falls back to the general subjects index by category once available. Phase 3b
  // ships project as a Subject FK; querying subjects-by-project lands when ProjectPage
  // wants it. For now, render an empty hint.)
  useEffect(() => {
    setSubjects([]);
  }, [projectId]);

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!project) return;
    setSaving(true);
    setError(null);
    try {
      await api.updateProject(
        project.identifier.uuid,
        name.trim(),
        description.trim() || null,
        status,
      );
      await refetch();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-10">
      <header className="space-y-3">
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
          Project
        </p>
        {loading && !project ? (
          <p className="font-mono text-[11px] text-muted-foreground/70">Loading…</p>
        ) : !project ? (
          <p className="font-serif text-base italic text-muted-foreground">Project not found.</p>
        ) : (
          <>
            <h1
              className="font-serif text-[44px] leading-none tracking-tight"
              style={{ fontVariationSettings: "'opsz' 144" }}
            >
              <span className="italic">{project.name.slice(0, 1)}</span>
              {project.name.slice(1)}
            </h1>
            <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
              {project.status} · {subjects.length}
              {' '}
              {subjects.length === 1 ? 'subject' : 'subjects'}
            </p>
          </>
        )}
      </header>

      {project && (
        <form
          onSubmit={handleSave}
          className="relative flex flex-col gap-3 rounded-md border border-border/70 bg-card/60 p-4"
        >
          <span className="absolute -top-[9px] left-4 bg-background px-2 font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            Edit project
          </span>
          <label className="flex flex-col gap-1">
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
              Name
            </span>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="h-9 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
            />
          </label>
          <label className="flex flex-col gap-1">
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
              Description
            </span>
            <Input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="h-9 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
            />
          </label>
          <label className="flex flex-col gap-1">
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
              Status
            </span>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as api.ProjectStatus)}
              className="h-9 border-0 border-b border-border/70 bg-transparent text-sm focus-visible:border-foreground/40 focus-visible:outline-none"
            >
              {STATUSES.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </label>
          <div className="flex items-center gap-3">
            <Button type="submit" size="sm" disabled={saving}
              className="h-9 rounded-[6px] bg-primary px-4 font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90">
              {saving ? 'Saving…' : 'Save'}
            </Button>
            {error && <span className="font-mono text-[11px] text-primary">{error}</span>}
          </div>
        </form>
      )}

      <section>
        <p className="font-serif text-sm italic text-muted-foreground">
          Linking subjects to this project lands in a follow-up.{' '}
          <Link to="/" className="underline underline-offset-4">Back to dashboard</Link>
        </p>
      </section>
    </div>
  );
}
