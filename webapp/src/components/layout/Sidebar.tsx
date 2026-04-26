import { useState, type FormEvent } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import { CategoryIcon } from '@/components/CategoryIcon';
import { cn } from '@/lib/utils';
import * as api from '@/api';
import { useCategoriesContext } from '@/hooks/useCategoriesContext';
import { useOwner, useProjects } from '@/hooks/useApi';

const DEFAULT_NEW_COLOR = '#9CA3AF';
const DEFAULT_NEW_ICON = 'tag';

export function Sidebar() {
  const { data: categoriesPage, loading, refetch } = useCategoriesContext();
  const { data: owner } = useOwner();
  const { categoryId: activeCategoryId, projectId: activeProjectId } =
    useParams<{ categoryId: string; projectId: string }>();
  const navigate = useNavigate();
  const [newCategory, setNewCategory] = useState('');
  const [newProject, setNewProject] = useState('');
  const [error, setError] = useState<string | null>(null);

  const categories = categoriesPage?.items ?? [];
  const isPremium = owner != null && api.tierSatisfies(owner.tier, 'PREMIUM');
  const { data: projectsPage, refetch: refetchProjects } = useProjects();
  const projects = projectsPage?.items ?? [];

  async function handleAddCategory(e: FormEvent) {
    e.preventDefault();
    const name = newCategory.trim();
    if (!name) return;
    setError(null);
    try {
      const created = await api.createCategory(name, DEFAULT_NEW_COLOR, DEFAULT_NEW_ICON);
      setNewCategory('');
      await refetch();
      navigate(`/c/${created.identifier.uuid}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function handleAddProject(e: FormEvent) {
    e.preventDefault();
    const name = newProject.trim();
    if (!name) return;
    setError(null);
    try {
      const created = await api.createProject(name, null);
      setNewProject('');
      await refetchProjects();
      navigate(`/p/${created.identifier.uuid}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  return (
    <aside className="space-y-7 md:sticky md:top-24 md:self-start">
      <div>
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            Categories
          </h2>
          <span className="font-mono text-[10px] tabular-nums text-muted-foreground/70">
            {String(categories.length).padStart(2, '0')}
          </span>
        </div>
        <form onSubmit={handleAddCategory}>
          <Input
            placeholder="+  New category…"
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            className="h-8 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none placeholder:text-muted-foreground/70 focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
          />
          {error && <p className="mt-1 font-mono text-[10px] text-primary">{error}</p>}
        </form>
      </div>

      <nav>
        {loading ? (
          <p className="font-mono text-[11px] text-muted-foreground/70">Loading…</p>
        ) : categories.length === 0 ? (
          <p className="font-serif text-sm italic text-muted-foreground">
            No categories yet — create your first.
          </p>
        ) : (
          <ul className="space-y-px">
            {categories.map((cat) => (
              <SidebarCategory
                key={cat.identifier.uuid}
                category={cat}
                active={activeCategoryId === cat.identifier.uuid}
              />
            ))}
          </ul>
        )}
      </nav>

      {isPremium && (
        <>
          <Separator className="opacity-60" />
          <div>
            <div className="mb-3 flex items-baseline justify-between">
              <h2 className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
                Projects
              </h2>
              <span className="font-mono text-[10px] tabular-nums text-muted-foreground/70">
                {String(projects.length).padStart(2, '0')}
              </span>
            </div>
            <form onSubmit={handleAddProject}>
              <Input
                placeholder="+  New project…"
                value={newProject}
                onChange={(e) => setNewProject(e.target.value)}
                className="mb-3 h-8 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none placeholder:text-muted-foreground/70 focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
              />
            </form>
            {projects.length === 0 ? (
              <p className="font-serif text-sm italic text-muted-foreground">
                No projects yet.
              </p>
            ) : (
              <ul className="space-y-px">
                {projects.map((p) => (
                  <SidebarProject
                    key={p.identifier.uuid}
                    project={p}
                    active={activeProjectId === p.identifier.uuid}
                  />
                ))}
              </ul>
            )}
          </div>
        </>
      )}

      <Separator className="opacity-60" />

      <div className="space-y-1.5 font-serif text-[13px] italic leading-relaxed text-muted-foreground/90">
        <p className="text-[11px] not-italic font-mono uppercase tracking-[0.2em] text-muted-foreground/70">
          Prompt
        </p>
        <p>
          “A writer should keep a common-place book, into which he may transcribe whatever he
          reads worth remembering.”
        </p>
        <p className="text-[11px] not-italic font-mono text-muted-foreground/60">
          — Jonathan Swift
        </p>
      </div>
    </aside>
  );
}

function SidebarProject({ project, active }: { project: api.Project; active: boolean }) {
  const dim = project.status === 'ARCHIVED' || project.status === 'COMPLETED';
  return (
    <li className="relative">
      <Link
        to={`/p/${project.identifier.uuid}`}
        className={cn(
          'group flex w-full items-baseline gap-2 py-1.5 pl-0 pr-0 text-left transition-colors outline-none',
          active ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
          dim && 'opacity-60',
        )}
      >
        {active && (
          <span className="absolute -left-4 top-1/2 h-[14px] w-[2px] -translate-y-1/2 bg-primary" />
        )}
        <span className="font-serif text-[15px] leading-6">{project.name}</span>
        <span
          aria-hidden
          className="relative top-[-2px] mx-1 flex-1 border-b border-dotted border-border/90"
        />
        <span className="font-mono text-[10px] tabular-nums text-muted-foreground/80">
          {project.status.slice(0, 1)}
        </span>
      </Link>
    </li>
  );
}

function SidebarCategory({ category, active }: { category: api.Category; active: boolean }) {
  return (
    <li className="relative">
      <Link
        to={`/c/${category.identifier.uuid}`}
        className={cn(
          'group flex w-full items-center gap-2 py-1.5 pl-0 pr-0 text-left transition-colors outline-none',
          active ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
        )}
      >
        {active && (
          <span className="absolute -left-4 top-1/2 h-[14px] w-[2px] -translate-y-1/2 bg-primary" />
        )}
        <CategoryIcon
          name={category.icon}
          size={14}
          className="shrink-0"
        />
        <span style={{ color: category.color }} aria-hidden className="text-[10px]">●</span>
        <span className="font-serif text-[15px] leading-6">{category.name}</span>
        <span
          aria-hidden
          className="relative top-[-2px] mx-1 flex-1 border-b border-dotted border-border/90"
        />
        <span className="font-mono text-[11px] tabular-nums text-muted-foreground/80">›</span>
      </Link>
    </li>
  );
}
