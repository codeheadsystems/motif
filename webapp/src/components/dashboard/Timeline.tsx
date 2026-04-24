import { type ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { relativeTime, shortDate, shortTime } from '@/lib/format';

export type TimelineEntry = {
  id: string;
  type: 'event' | 'note';
  value: string;
  category?: string;
  subject?: string;
  tags: string[];
  timestamp: string;
};

type TimelineProps = {
  entries: TimelineEntry[];
  title?: string;
  showPath?: boolean;
  emptyMessage?: string;
  loading?: boolean;
  renderActions?: (entry: TimelineEntry) => ReactNode;
};

export function Timeline({
  entries,
  title = 'Recent',
  showPath = true,
  emptyMessage = 'No activity yet. Add something above, or pick a category.',
  loading = false,
  renderActions,
}: TimelineProps) {
  return (
    <section>
      <div className="mb-8 flex items-baseline justify-between">
        <div className="flex items-baseline gap-3">
          <h2 className="font-serif text-[32px] leading-none tracking-tight">
            <span className="italic">{title.slice(0, 2)}</span>
            {title.slice(2)}
          </h2>
          <span className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            {loading
              ? 'Loading…'
              : `${entries.length} ${entries.length === 1 ? 'entry' : 'entries'}`}
          </span>
        </div>
        <span className="hidden font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground/80 sm:inline">
          Reverse chronological
        </span>
      </div>

      {loading && entries.length === 0 ? (
        <p className="font-mono text-[11px] text-muted-foreground/70">Loading entries…</p>
      ) : entries.length === 0 ? (
        <p className="font-serif text-base italic text-muted-foreground">{emptyMessage}</p>
      ) : (
        <ol className="relative">
          {entries.map((entry, i) => {
            const prev = entries[i - 1];
            const showDateHeader = !prev || shortDate(prev.timestamp) !== shortDate(entry.timestamp);
            return (
              <TimelineRow
                key={entry.id}
                entry={entry}
                index={i}
                showDateHeader={showDateHeader}
                showPath={showPath}
                actions={renderActions?.(entry)}
              />
            );
          })}
        </ol>
      )}
    </section>
  );
}

function TimelineRow({
  entry,
  index,
  showDateHeader,
  showPath,
  actions,
}: {
  entry: TimelineEntry;
  index: number;
  showDateHeader: boolean;
  showPath: boolean;
  actions?: ReactNode;
}) {
  return (
    <li
      className="group grid grid-cols-[92px_1fr] gap-6 animate-in fade-in slide-in-from-bottom-1 fill-mode-backwards"
      style={{ animationDelay: `${Math.min(index, 14) * 28}ms`, animationDuration: '520ms' }}
    >
      <div className="pt-[2px] font-mono text-[11px] leading-[1.55] tabular-nums text-muted-foreground">
        {showDateHeader ? (
          <div className="text-foreground/80">{shortDate(entry.timestamp)}</div>
        ) : (
          <div className="text-muted-foreground/40">·</div>
        )}
        <div className="text-muted-foreground/70">{shortTime(entry.timestamp)}</div>
      </div>

      <div className="relative min-w-0 border-l border-border/70 pb-6 pl-6 last:pb-0">
        <span
          aria-hidden
          className={cn(
            'absolute left-[-4px] top-[7px] size-[7px] rounded-full ring-4 ring-background',
            entry.type === 'event' ? 'bg-primary' : 'bg-border',
          )}
        />
        <div className="flex items-start gap-2">
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-baseline gap-x-2.5 gap-y-1">
              <TypeBadge type={entry.type} />
              <p className="text-[15.5px] leading-snug text-foreground">{entry.value}</p>
            </div>
            <div className="mt-1.5 flex flex-wrap items-baseline gap-x-2 gap-y-1 text-[13px]">
              {showPath && (entry.category || entry.subject) && (
                <span className="font-serif italic text-muted-foreground">
                  {entry.category}
                  {entry.category && entry.subject && <span className="px-1.5 text-border">·</span>}
                  {entry.subject}
                </span>
              )}
              {entry.tags.length > 0 && (
                <>
                  {showPath && (entry.category || entry.subject) && (
                    <span className="text-border" aria-hidden>—</span>
                  )}
                  <span className="flex flex-wrap gap-x-2 font-mono text-[11px] text-muted-foreground">
                    {entry.tags.map((t) => <span key={t}>#{t}</span>)}
                  </span>
                </>
              )}
              <span className="ml-auto font-mono text-[11px] text-muted-foreground/70">
                {relativeTime(entry.timestamp)}
              </span>
            </div>
          </div>
          {actions && (
            <div className="flex shrink-0 items-center opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
              {actions}
            </div>
          )}
        </div>
      </div>
    </li>
  );
}

function TypeBadge({ type }: { type: 'event' | 'note' }) {
  if (type === 'event') {
    return (
      <span className="inline-flex h-[18px] items-center rounded-sm bg-primary px-1.5 font-mono text-[9px] font-medium uppercase tracking-[0.16em] text-primary-foreground">
        Event
      </span>
    );
  }
  return (
    <span className="inline-flex h-[18px] items-center rounded-sm border border-border px-1.5 font-mono text-[9px] font-medium uppercase tracking-[0.16em] text-muted-foreground">
      Note
    </span>
  );
}
