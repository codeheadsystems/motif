import { useState, type FormEvent } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import { cn } from '@/lib/utils';
import { useCategoriesContext } from '@/hooks/useCategoriesContext';

export function Sidebar() {
  const { data: categories, loading } = useCategoriesContext();
  const { category: activeCategoryRaw } = useParams<{ category: string }>();
  const activeCategory = activeCategoryRaw ? decodeURIComponent(activeCategoryRaw) : null;
  const navigate = useNavigate();
  const [newCategory, setNewCategory] = useState('');

  function handleAddCategory(e: FormEvent) {
    e.preventDefault();
    const name = newCategory.trim();
    if (!name) return;
    setNewCategory('');
    // Categories are implicitly created when the first subject under them is saved.
    // Navigate to the new category's subjects page where the user can add one.
    navigate(`/c/${encodeURIComponent(name)}`);
  }

  return (
    <aside className="space-y-7 md:sticky md:top-24 md:self-start">
      <div>
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            Categories
          </h2>
          <span className="font-mono text-[10px] tabular-nums text-muted-foreground/70">
            {String(categories?.length ?? 0).padStart(2, '0')}
          </span>
        </div>
        <form onSubmit={handleAddCategory}>
          <Input
            placeholder="+  New category…"
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            className="h-8 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none placeholder:text-muted-foreground/70 focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
          />
        </form>
      </div>

      <nav>
        {loading ? (
          <p className="font-mono text-[11px] text-muted-foreground/70">Loading…</p>
        ) : !categories || categories.length === 0 ? (
          <p className="font-serif text-sm italic text-muted-foreground">
            No categories yet — create your first.
          </p>
        ) : (
          <ul className="space-y-px">
            {categories.map((cat) => (
              <SidebarCategory
                key={cat.value}
                value={cat.value}
                active={activeCategory === cat.value}
              />
            ))}
          </ul>
        )}
      </nav>

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

function SidebarCategory({ value, active }: { value: string; active: boolean }) {
  return (
    <li className="relative">
      <Link
        to={`/c/${encodeURIComponent(value)}`}
        className={cn(
          'group flex w-full items-baseline gap-2 py-1.5 pl-0 pr-0 text-left transition-colors outline-none',
          active ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
        )}
      >
        {active && (
          <span className="absolute -left-4 top-1/2 h-[14px] w-[2px] -translate-y-1/2 bg-primary" />
        )}
        <span className="font-serif text-[15px] leading-6">{value}</span>
        <span
          aria-hidden
          className="relative top-[-2px] mx-1 flex-1 border-b border-dotted border-border/90"
        />
        <span className="font-mono text-[11px] tabular-nums text-muted-foreground/80">›</span>
      </Link>
    </li>
  );
}
