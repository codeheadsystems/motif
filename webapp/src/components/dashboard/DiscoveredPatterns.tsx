import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { RefreshCw, Workflow as WorkflowIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import * as api from '@/api';
import { useOwner, usePatterns } from '@/hooks/useApi';

const HOUR = 3600;
const DAY = 86400;
const WEEK = 7 * DAY;
const MONTH = 30 * DAY;

function humanInterval(seconds: number): string {
  if (seconds < 1.5 * HOUR) return `${Math.round(seconds / 60)} minutes`;
  if (seconds < 1.5 * DAY) return `${Math.round(seconds / HOUR)} hours`;
  if (seconds < 1.5 * WEEK) return `${Math.round(seconds / DAY)} days`;
  if (seconds < 2 * MONTH) return `${Math.round(seconds / WEEK)} weeks`;
  return `${Math.round(seconds / MONTH)} months`;
}

function relativeWhen(iso: string, now: Date = new Date()): string {
  const target = new Date(iso);
  const deltaMs = target.getTime() - now.getTime();
  const absDays = Math.abs(deltaMs / (DAY * 1000));
  const sign = deltaMs >= 0 ? 'in' : 'ago';
  if (absDays < 1) {
    const hours = Math.max(1, Math.round(Math.abs(deltaMs / (HOUR * 1000))));
    return sign === 'in' ? `in ${hours}h` : `${hours}h ago`;
  }
  const days = Math.round(absDays);
  return sign === 'in' ? `in ${days}d` : `${days}d ago`;
}

function describePeriod(p: api.PeriodClassification): string {
  switch (p) {
    case 'DAILY': return 'daily';
    case 'WEEKLY': return 'weekly';
    case 'MONTHLY': return 'monthly';
    default: return '';
  }
}

export function DiscoveredPatterns() {
  const { data, loading, refetch } = usePatterns(5);
  const { data: owner } = useOwner();
  const navigate = useNavigate();
  const [refreshing, setRefreshing] = useState(false);
  const [convertingId, setConvertingId] = useState<string | null>(null);
  const isPremium = owner != null && api.tierSatisfies(owner.tier, 'PREMIUM');

  async function handleMakeWorkflow(patternId: string) {
    setConvertingId(patternId);
    try {
      const workflow = await api.createWorkflowFromPattern(patternId);
      navigate(`/w/${workflow.identifier.uuid}`);
    } catch (err) {
      console.error('from-pattern failed', err);
    } finally {
      setConvertingId(null);
    }
  }

  async function handleRecompute() {
    setRefreshing(true);
    try {
      await api.recomputePatterns(5);
      await refetch();
    } finally {
      setRefreshing(false);
    }
  }

  const patterns = data?.items ?? [];
  const showSpinner = loading || refreshing;

  return (
    <section
      className="relative rounded-md border border-border/70 bg-card/60 p-5"
      aria-labelledby="patterns-heading"
    >
      <div className="mb-4 flex items-baseline justify-between">
        <h2
          id="patterns-heading"
          className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground"
        >
          Discovered patterns
        </h2>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleRecompute}
          disabled={refreshing}
          className="h-7 gap-1.5 px-2 font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground hover:text-foreground"
          aria-label="Recompute patterns"
        >
          <RefreshCw className={`size-3 ${refreshing ? 'animate-spin' : ''}`} strokeWidth={2.25} />
          Refresh
        </Button>
      </div>

      {showSpinner && patterns.length === 0 ? (
        <p className="font-mono text-[11px] text-muted-foreground/70">Looking for rhythms…</p>
      ) : patterns.length === 0 ? (
        <p className="font-serif text-sm italic text-muted-foreground">
          Nothing yet. Patterns appear once you’ve logged the same event a few times at consistent intervals.
        </p>
      ) : (
        <ul className="space-y-2.5">
          {patterns.map((p) => {
            const period = describePeriod(p.period);
            const cadence = period || `every ~${humanInterval(p.intervalMeanSeconds)}`;
            return (
              <li
                key={p.identifier.uuid}
                className="flex items-baseline justify-between gap-3 border-b border-border/40 pb-2.5 last:border-0 last:pb-0"
              >
                <span className="font-serif text-[14px] leading-snug text-foreground">
                  You tend to{' '}
                  <span className="italic">{p.eventValue}</span>
                  {' '}on this subject{' '}
                  <span className="font-mono text-[11px] tracking-tight text-muted-foreground">
                    ({cadence}, ×{p.occurrenceCount})
                  </span>
                </span>
                <span className="flex shrink-0 items-baseline gap-2">
                  <span className="rounded-sm bg-secondary/70 px-2 py-0.5 font-mono text-[10px] tracking-[0.12em] text-muted-foreground">
                    next {relativeWhen(p.nextExpectedAt.timestamp)}
                  </span>
                  {isPremium && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleMakeWorkflow(p.identifier.uuid)}
                      disabled={convertingId === p.identifier.uuid}
                      title="Convert this pattern into a workflow"
                      className="h-6 gap-1 px-1.5 font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground hover:text-foreground"
                    >
                      <WorkflowIcon className="size-3" strokeWidth={2.25} />
                      Workflow
                    </Button>
                  )}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
