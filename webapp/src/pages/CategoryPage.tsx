import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Plus, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { CategoryIcon } from '@/components/CategoryIcon';
import * as api from '@/api';
import { useCategory, useSubjects } from '@/hooks/useApi';
import { useCategoriesContext } from '@/hooks/useCategoriesContext';

export function CategoryPage() {
  const { categoryId: rawId } = useParams<{ categoryId: string }>();
  const categoryId = rawId ?? '';
  const { data: category, loading: categoryLoading } = useCategory(categoryId || null);
  const { refetch: refetchCategories } = useCategoriesContext();
  const { data: page, loading, refetch } = useSubjects(categoryId || null);
  const [newSubject, setNewSubject] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<api.Subject | null>(null);

  async function handleAddSubject(e: FormEvent) {
    e.preventDefault();
    const value = newSubject.trim();
    if (!value || !categoryId) return;
    setError(null);
    try {
      await api.createSubject(categoryId, value);
      setNewSubject('');
      await refetch();
      await refetchCategories();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function handleConfirmDelete() {
    if (!pendingDelete) return;
    try {
      await api.deleteSubject(pendingDelete.identifier.uuid);
      setPendingDelete(null);
      await refetch();
      await refetchCategories();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setPendingDelete(null);
    }
  }

  const subjects = page?.items ?? [];
  const categoryName = category?.name ?? '';

  return (
    <div className="space-y-10">
      <header className="space-y-3">
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
          Category
        </p>
        <h1
          className="flex items-center gap-3 font-serif text-[44px] leading-none tracking-tight"
          style={{ fontVariationSettings: "'opsz' 144" }}
        >
          {category && (
            <CategoryIcon
              name={category.icon}
              size={32}
              className="shrink-0"
              // inline color via style so Tailwind can't tree-shake unknown values
            />
          )}
          {category && <span style={{ color: category.color }} aria-hidden>•</span>}
          <span>
            <span className="italic">{categoryName.slice(0, 1)}</span>
            {categoryName.slice(1)}
          </span>
        </h1>
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
          {loading || categoryLoading
            ? 'Loading…'
            : `${subjects.length} ${subjects.length === 1 ? 'subject' : 'subjects'}`}
        </p>
      </header>

      <section>
        <form
          onSubmit={handleAddSubject}
          className="relative flex items-center gap-3 rounded-md border border-border/70 bg-card/60 p-4"
        >
          <span className="absolute -top-[9px] left-4 bg-background px-2 font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            New subject
          </span>
          <span className="font-serif text-[13px] italic text-muted-foreground">add</span>
          <Input
            value={newSubject}
            onChange={(e) => setNewSubject(e.target.value)}
            placeholder={`e.g. Water Heater, Tesla Model 3…`}
            className="h-9 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none placeholder:text-muted-foreground/60 focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
          />
          <Button
            type="submit"
            size="sm"
            className="h-9 gap-1.5 rounded-[6px] bg-primary px-4 font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90"
          >
            <Plus className="size-3.5" strokeWidth={2.25} />
            Add
          </Button>
        </form>
        {error && <p className="mt-2 font-mono text-[11px] text-primary">{error}</p>}
      </section>

      <section>
        {subjects.length === 0 && !loading ? (
          <p className="font-serif text-base italic text-muted-foreground">
            No subjects in this category yet. Add one above.
          </p>
        ) : (
          <ul className="divide-y divide-border/60">
            {subjects.map((s) => (
              <li key={s.identifier.uuid} className="group flex items-center gap-4 py-3">
                <Link
                  to={`/s/${s.identifier.uuid}`}
                  state={{ subject: s }}
                  className="flex flex-1 items-baseline gap-3 outline-none"
                >
                  <span className="font-serif text-[17px] leading-snug text-foreground group-hover:underline underline-offset-4">
                    {s.value}
                  </span>
                  <span
                    aria-hidden
                    className="relative top-[-3px] flex-1 border-b border-dotted border-border/80"
                  />
                  <span className="font-mono text-[11px] text-muted-foreground/70 group-hover:text-primary">
                    view ›
                  </span>
                </Link>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Delete ${s.value}`}
                  onClick={() => setPendingDelete(s)}
                  className="size-7 text-muted-foreground/70 opacity-0 transition-opacity hover:text-primary group-hover:opacity-100 focus-visible:opacity-100"
                >
                  <X className="size-4" />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <AlertDialog open={!!pendingDelete} onOpenChange={(o) => !o && setPendingDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="font-serif text-2xl italic font-normal">
              Delete “{pendingDelete?.value}”?
            </AlertDialogTitle>
            <AlertDialogDescription className="font-serif italic text-muted-foreground">
              All events and notes attached to this subject will be removed.
              This cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="font-mono text-[11px] uppercase tracking-[0.18em]">
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmDelete}
              className="bg-primary font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
