import { useMemo, useState, type FormEvent } from 'react';
import { Plus } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Separator } from '@/components/ui/separator';
import * as api from '@/api';
import { useCategoriesContext } from '@/hooks/useCategoriesContext';

type Status =
  | { kind: 'idle' }
  | { kind: 'saving' }
  | { kind: 'success'; label: string }
  | { kind: 'error'; message: string };

const DEFAULT_NEW_COLOR = '#9CA3AF';
const DEFAULT_NEW_ICON = 'tag';

export function QuickAdd({ onCreated }: { onCreated: () => void }) {
  const { data: categoriesPage, refetch: refetchCategories } = useCategoriesContext();
  const [type, setType] = useState<'event' | 'note'>('event');
  const [categoryName, setCategoryName] = useState('');
  const [subject, setSubject] = useState('');
  const [desc, setDesc] = useState('');
  const [tags, setTags] = useState('');
  const [subjectsForCategory, setSubjectsForCategory] = useState<api.Subject[]>([]);
  const [status, setStatus] = useState<Status>({ kind: 'idle' });

  const categories = categoriesPage?.items ?? [];
  const categoryOptions = useMemo(
    () => categories.map((c) => c.name),
    [categories],
  );

  function findCategoryByName(name: string): api.Category | undefined {
    const target = name.trim().toLowerCase();
    return categories.find((c) => c.name.toLowerCase() === target);
  }

  async function refreshSubjectsForCategory(name: string) {
    const cat = findCategoryByName(name);
    if (!cat) { setSubjectsForCategory([]); return; }
    try {
      const page = await api.getSubjects(cat.identifier.uuid);
      setSubjectsForCategory(page.items);
    } catch {
      setSubjectsForCategory([]);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const catName = categoryName.trim();
    const subjName = subject.trim();
    const value = desc.trim();
    if (!catName || !subjName || !value) return;

    setStatus({ kind: 'saving' });
    try {
      // Find or create the category.
      let cat = findCategoryByName(catName);
      if (!cat) {
        cat = await api.createCategory(catName, DEFAULT_NEW_COLOR, DEFAULT_NEW_ICON);
      }

      // Find or create the subject within that category.
      let page = await api.getSubjects(cat.identifier.uuid);
      let subj = page.items.find((s) => s.value === subjName);
      if (!subj) {
        subj = await api.createSubject(cat.identifier.uuid, subjName);
      }

      if (type === 'event') {
        const parsedTags = tags
          ? tags.split(',').map((t) => t.trim().replace(/^#/, '')).filter(Boolean)
          : [];
        await api.createEvent(subj.identifier.uuid, value, parsedTags);
      } else {
        await api.createNote(subj.identifier.uuid, value);
      }

      setStatus({ kind: 'success', label: `${type === 'event' ? 'Event' : 'Note'} added to ${cat.name} / ${subjName}` });
      setDesc('');
      setTags('');
      await refetchCategories();
      onCreated();
      window.setTimeout(() => setStatus({ kind: 'idle' }), 2500);
    } catch (err) {
      setStatus({ kind: 'error', message: err instanceof Error ? err.message : String(err) });
    }
  }

  return (
    <section>
      <form
        onSubmit={handleSubmit}
        className="relative rounded-md border border-border/70 bg-card/60 p-5 shadow-[0_1px_0_rgba(25,20,10,0.03)]"
      >
        <span className="absolute -top-[9px] left-4 bg-background px-2 font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
          Quick add
        </span>
        <span className="absolute -top-[9px] right-4 bg-background px-2 font-mono text-[10px] tracking-[0.2em] text-muted-foreground/70">
          ⌘↵
        </span>

        <div className="flex flex-wrap items-stretch gap-3">
          <Tabs value={type} onValueChange={(v) => setType(v as 'event' | 'note')}>
            <TabsList className="h-9 gap-0.5 rounded-[6px] bg-secondary/70 p-0.5">
              <TabsTrigger
                value="event"
                className="h-8 rounded-[4px] px-3.5 font-mono text-[10px] uppercase tracking-[0.18em] data-[state=active]:bg-background data-[state=active]:shadow-sm"
              >
                Event
              </TabsTrigger>
              <TabsTrigger
                value="note"
                className="h-8 rounded-[4px] px-3.5 font-mono text-[10px] uppercase tracking-[0.18em] data-[state=active]:bg-background data-[state=active]:shadow-sm"
              >
                Note
              </TabsTrigger>
            </TabsList>
          </Tabs>

          <Separator orientation="vertical" className="h-9" />

          <div className="flex flex-1 flex-wrap items-center gap-x-3 gap-y-2">
            <FieldInput
              label="in"
              placeholder="Category"
              value={categoryName}
              onChange={(v) => { setCategoryName(v); refreshSubjectsForCategory(v); }}
              listId="qa-categories"
              options={categoryOptions}
              widthClass="w-[150px]"
            />
            <FieldInput
              label="on"
              placeholder="Subject"
              value={subject}
              onChange={setSubject}
              listId="qa-subjects"
              options={subjectsForCategory.map((s) => s.value)}
              widthClass="w-[180px]"
            />
            <FieldInput
              label={type === 'event' ? 'did' : 'noted'}
              placeholder={type === 'event' ? 'What happened?' : 'What did you observe?'}
              value={desc}
              onChange={setDesc}
              widthClass="flex-1 min-w-[220px]"
            />
            <div
              aria-hidden={type !== 'event'}
              className={cn(
                'flex items-center gap-2 overflow-hidden transition-all duration-300 ease-out',
                type === 'event' ? 'max-w-[200px] opacity-100' : 'pointer-events-none max-w-0 opacity-0',
              )}
            >
              <span className="font-serif text-[13px] italic text-muted-foreground">tagged</span>
              <Input
                value={tags}
                onChange={(e) => setTags(e.target.value)}
                placeholder="comma, separated"
                className="h-9 w-[160px] rounded-none border-0 border-b border-border/70 bg-transparent px-0 font-mono text-[12px] shadow-none placeholder:text-muted-foreground/60 focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
              />
            </div>
          </div>

          <Button
            type="submit"
            size="sm"
            disabled={status.kind === 'saving'}
            className="h-9 gap-1.5 self-center rounded-[6px] bg-primary px-4 font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90"
          >
            <Plus className="size-3.5" strokeWidth={2.25} />
            Add
          </Button>
        </div>
      </form>

      <div className="mt-2 min-h-[1em]">
        {status.kind === 'success' && (
          <p className="font-serif text-[13px] italic text-muted-foreground">
            {status.label}
          </p>
        )}
        {status.kind === 'error' && (
          <p className="font-mono text-[11px] text-primary">Couldn’t save: {status.message}</p>
        )}
        {status.kind === 'saving' && (
          <p className="font-mono text-[11px] text-muted-foreground/70">Saving…</p>
        )}
      </div>
    </section>
  );
}

type FieldInputProps = {
  label: string;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
  listId?: string;
  options?: string[];
  widthClass: string;
};

function FieldInput({ label, placeholder, value, onChange, listId, options, widthClass }: FieldInputProps) {
  return (
    <label className={cn('flex items-baseline gap-2', widthClass)}>
      <span className="font-serif text-[13px] italic text-muted-foreground">{label}</span>
      <Input
        list={listId}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="h-9 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none placeholder:text-muted-foreground/60 focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
      />
      {listId && options && (
        <datalist id={listId}>
          {options.map((o) => <option key={o} value={o} />)}
        </datalist>
      )}
    </label>
  );
}
