import { useMemo, useState, type FormEvent } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
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
import * as api from '@/api';
import { useSubject, useEvents, useNotes } from '@/hooks/useApi';
import { Timeline, type TimelineEntry } from '@/components/dashboard/Timeline';

type PendingDelete =
  | { kind: 'event'; id: string; label: string }
  | { kind: 'note'; id: string; label: string }
  | null;

export function SubjectDetailPage() {
  const { subjectId } = useParams<{ subjectId: string }>();
  const location = useLocation();
  const hinted = (location.state as { subject?: api.Subject } | null)?.subject;

  const { data: fetchedSubject, loading: subjectLoading } = useSubject(subjectId ?? null);
  const subject = hinted ?? fetchedSubject;

  const { data: eventsPage, refetch: refetchEvents, loading: eventsLoading } = useEvents(subjectId ?? null);
  const { data: notesPage, refetch: refetchNotes, loading: notesLoading } = useNotes(subjectId ?? null);

  const [tab, setTab] = useState<'events' | 'notes'>('events');
  const [eventValue, setEventValue] = useState('');
  const [eventTags, setEventTags] = useState('');
  const [noteValue, setNoteValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<PendingDelete>(null);

  const eventEntries = useMemo<TimelineEntry[]>(
    () =>
      (eventsPage?.items ?? []).map((e) => ({
        id: `e:${e.identifier.uuid}`,
        type: 'event',
        value: e.value,
        tags: e.tags.map((t) => t.value),
        timestamp: e.timestamp.timestamp,
      })),
    [eventsPage],
  );

  const noteEntries = useMemo<TimelineEntry[]>(
    () =>
      (notesPage?.items ?? []).map((n) => ({
        id: `n:${n.identifier.uuid}`,
        type: 'note',
        value: n.value,
        tags: n.tags.map((t) => t.value),
        timestamp: n.timestamp.timestamp,
      })),
    [notesPage],
  );

  async function handleAddEvent(e: FormEvent) {
    e.preventDefault();
    if (!subjectId) return;
    const value = eventValue.trim();
    if (!value) return;
    setError(null);
    try {
      const tags = eventTags
        ? eventTags.split(',').map((t) => t.trim().replace(/^#/, '')).filter(Boolean)
        : [];
      await api.createEvent(subjectId, value, tags);
      setEventValue('');
      setEventTags('');
      await refetchEvents();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function handleAddNote(e: FormEvent) {
    e.preventDefault();
    if (!subjectId) return;
    const value = noteValue.trim();
    if (!value) return;
    setError(null);
    try {
      await api.createNote(subjectId, value);
      setNoteValue('');
      await refetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function handleConfirmDelete() {
    if (!pendingDelete) return;
    try {
      if (pendingDelete.kind === 'event') {
        await api.deleteEvent(pendingDelete.id);
        await refetchEvents();
      } else {
        await api.deleteNote(pendingDelete.id);
        await refetchNotes();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setPendingDelete(null);
    }
  }

  return (
    <div className="space-y-10">
      <header className="space-y-3">
        <div className="flex items-center gap-3">
          <Link
            to={subject ? `/c/${encodeURIComponent(subject.category.value)}` : '/'}
            className="inline-flex items-center gap-1.5 font-mono text-[11px] uppercase tracking-[0.18em] text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" />
            Back
          </Link>
        </div>
        {!subject && subjectLoading ? (
          <p className="font-mono text-[11px] text-muted-foreground/70">Loading subject…</p>
        ) : !subject ? (
          <p className="font-serif text-base italic text-muted-foreground">
            Subject not found.
          </p>
        ) : (
          <>
            <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
              {subject.category.value}
            </p>
            <h1
              className="font-serif text-[44px] leading-none tracking-tight"
              style={{ fontVariationSettings: "'opsz' 144" }}
            >
              <span className="italic">{subject.value.slice(0, 1)}</span>
              {subject.value.slice(1)}
            </h1>
            <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
              {(eventsPage?.items.length ?? 0)} events · {(notesPage?.items.length ?? 0)} notes
            </p>
          </>
        )}
        {error && <p className="font-mono text-[11px] text-primary">{error}</p>}
      </header>

      {subject && (
        <Tabs value={tab} onValueChange={(v) => setTab(v as 'events' | 'notes')}>
          <TabsList className="h-9 gap-0.5 rounded-[6px] bg-secondary/70 p-0.5">
            <TabsTrigger
              value="events"
              className="h-8 rounded-[4px] px-4 font-mono text-[10px] uppercase tracking-[0.18em] data-[state=active]:bg-background data-[state=active]:shadow-sm"
            >
              Events
            </TabsTrigger>
            <TabsTrigger
              value="notes"
              className="h-8 rounded-[4px] px-4 font-mono text-[10px] uppercase tracking-[0.18em] data-[state=active]:bg-background data-[state=active]:shadow-sm"
            >
              Notes
            </TabsTrigger>
          </TabsList>

          <TabsContent value="events" className="mt-8 space-y-10">
            <section>
              <form
                onSubmit={handleAddEvent}
                className="relative flex flex-wrap items-center gap-3 rounded-md border border-border/70 bg-card/60 p-4"
              >
                <span className="absolute -top-[9px] left-4 bg-background px-2 font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
                  New event
                </span>
                <label className="flex flex-1 items-baseline gap-2 min-w-[240px]">
                  <span className="font-serif text-[13px] italic text-muted-foreground">did</span>
                  <Input
                    value={eventValue}
                    onChange={(e) => setEventValue(e.target.value)}
                    placeholder="What happened?"
                    className="h-9 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
                  />
                </label>
                <label className="flex items-baseline gap-2 w-[220px]">
                  <span className="font-serif text-[13px] italic text-muted-foreground">tagged</span>
                  <Input
                    value={eventTags}
                    onChange={(e) => setEventTags(e.target.value)}
                    placeholder="comma, separated"
                    className="h-9 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 font-mono text-[12px] shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
                  />
                </label>
                <Button
                  type="submit"
                  size="sm"
                  className="h-9 gap-1.5 rounded-[6px] bg-primary px-4 font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90"
                >
                  <Plus className="size-3.5" strokeWidth={2.25} />
                  Add
                </Button>
              </form>
            </section>
            <Timeline
              entries={eventEntries}
              loading={eventsLoading}
              title="Events"
              showPath={false}
              emptyMessage="No events yet. Log the first one above."
              renderActions={(entry) => {
                const event = eventsPage?.items.find((e) => `e:${e.identifier.uuid}` === entry.id);
                if (!event) return null;
                return (
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Delete event “${event.value}”`}
                    onClick={() => setPendingDelete({ kind: 'event', id: event.identifier.uuid, label: event.value })}
                    className="size-7 text-muted-foreground/70 hover:text-primary"
                  >
                    <X className="size-4" />
                  </Button>
                );
              }}
            />
          </TabsContent>

          <TabsContent value="notes" className="mt-8 space-y-10">
            <section>
              <form
                onSubmit={handleAddNote}
                className="relative flex flex-wrap items-center gap-3 rounded-md border border-border/70 bg-card/60 p-4"
              >
                <span className="absolute -top-[9px] left-4 bg-background px-2 font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
                  New note
                </span>
                <label className="flex flex-1 items-baseline gap-2 min-w-[240px]">
                  <span className="font-serif text-[13px] italic text-muted-foreground">noted</span>
                  <Input
                    value={noteValue}
                    onChange={(e) => setNoteValue(e.target.value)}
                    placeholder="What did you observe?"
                    className="h-9 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
                  />
                </label>
                <Button
                  type="submit"
                  size="sm"
                  className="h-9 gap-1.5 rounded-[6px] bg-primary px-4 font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90"
                >
                  <Plus className="size-3.5" strokeWidth={2.25} />
                  Add
                </Button>
              </form>
            </section>
            <Timeline
              entries={noteEntries}
              loading={notesLoading}
              title="Notes"
              showPath={false}
              emptyMessage="No notes yet. Write the first one above."
              renderActions={(entry) => {
                const note = notesPage?.items.find((n) => `n:${n.identifier.uuid}` === entry.id);
                if (!note) return null;
                return (
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Delete note`}
                    onClick={() => setPendingDelete({ kind: 'note', id: note.identifier.uuid, label: note.value })}
                    className="size-7 text-muted-foreground/70 hover:text-primary"
                  >
                    <X className="size-4" />
                  </Button>
                );
              }}
            />
          </TabsContent>
        </Tabs>
      )}

      <AlertDialog open={!!pendingDelete} onOpenChange={(o) => !o && setPendingDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="font-serif text-2xl italic font-normal">
              Delete this {pendingDelete?.kind}?
            </AlertDialogTitle>
            <AlertDialogDescription className="font-serif italic text-muted-foreground">
              “{pendingDelete?.label}” — cannot be undone.
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

