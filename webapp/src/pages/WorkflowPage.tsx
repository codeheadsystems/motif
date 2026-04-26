import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import * as api from '@/api';
import { useWorkflow } from '@/hooks/useApi';

interface DraftStep {
  name: string;
  expectedDurationSeconds: number | null;
  notes: string | null;
}

function toDraft(step: api.WorkflowStep): DraftStep {
  return {
    name: step.name,
    expectedDurationSeconds: step.expectedDurationSeconds,
    notes: step.notes,
  };
}

export function WorkflowPage() {
  const { workflowId: rawId } = useParams<{ workflowId: string }>();
  const workflowId = rawId ?? '';
  const navigate = useNavigate();
  const { data: workflow, loading, refetch } = useWorkflow(workflowId || null);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [steps, setSteps] = useState<DraftStep[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (workflow) {
      setName(workflow.name);
      setDescription(workflow.description ?? '');
      setSteps(workflow.steps.map(toDraft));
    }
  }, [workflow]);

  function updateStep(idx: number, patch: Partial<DraftStep>) {
    setSteps((s) => s.map((st, i) => (i === idx ? { ...st, ...patch } : st)));
  }
  function addStep() {
    setSteps((s) => [...s, { name: '', expectedDurationSeconds: null, notes: null }]);
  }
  function removeStep(idx: number) {
    setSteps((s) => s.filter((_, i) => i !== idx));
  }
  function moveStep(idx: number, delta: -1 | 1) {
    setSteps((s) => {
      const j = idx + delta;
      if (j < 0 || j >= s.length) return s;
      const copy = s.slice();
      [copy[idx], copy[j]] = [copy[j], copy[idx]];
      return copy;
    });
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!workflow) return;
    setSaving(true);
    setError(null);
    try {
      await api.updateWorkflow(
        workflow.identifier.uuid,
        name.trim(),
        description.trim() || null,
        steps.map((s) => ({
          name: s.name.trim(),
          expectedDurationSeconds: s.expectedDurationSeconds,
          notes: s.notes,
        })),
      );
      await refetch();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!workflow) return;
    if (!window.confirm(`Delete workflow "${workflow.name}"? This cannot be undone.`)) return;
    try {
      await api.deleteWorkflow(workflow.identifier.uuid);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  return (
    <div className="space-y-10">
      <header className="space-y-3">
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
          Workflow
        </p>
        {loading && !workflow ? (
          <p className="font-mono text-[11px] text-muted-foreground/70">Loading…</p>
        ) : !workflow ? (
          <p className="font-serif text-base italic text-muted-foreground">Workflow not found.</p>
        ) : (
          <h1
            className="font-serif text-[44px] leading-none tracking-tight"
            style={{ fontVariationSettings: "'opsz' 144" }}
          >
            <span className="italic">{workflow.name.slice(0, 1)}</span>
            {workflow.name.slice(1)}
          </h1>
        )}
      </header>

      {workflow && (
        <form
          onSubmit={handleSave}
          className="relative space-y-6 rounded-md border border-border/70 bg-card/60 p-5"
        >
          <span className="absolute -top-[9px] left-4 bg-background px-2 font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            Edit
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

          <div>
            <p className="mb-3 font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
              Steps
            </p>
            {steps.length === 0 ? (
              <p className="mb-3 font-serif text-sm italic text-muted-foreground">
                No steps yet. Add one below.
              </p>
            ) : (
              <ol className="space-y-3">
                {steps.map((step, idx) => (
                  <li key={idx} className="flex flex-col gap-2 rounded-sm border border-border/40 p-3">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-[11px] tabular-nums text-muted-foreground/80">
                        {String(idx + 1).padStart(2, '0')}
                      </span>
                      <Input
                        value={step.name}
                        onChange={(e) => updateStep(idx, { name: e.target.value })}
                        placeholder="Step name"
                        className="h-9 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
                      />
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Move up"
                        onClick={() => moveStep(idx, -1)}
                        disabled={idx === 0}
                        className="size-7 text-muted-foreground/70 hover:text-foreground"
                      >
                        <ArrowUp className="size-3.5" />
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Move down"
                        onClick={() => moveStep(idx, 1)}
                        disabled={idx === steps.length - 1}
                        className="size-7 text-muted-foreground/70 hover:text-foreground"
                      >
                        <ArrowDown className="size-3.5" />
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Delete step"
                        onClick={() => removeStep(idx)}
                        className="size-7 text-muted-foreground/70 hover:text-primary"
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                    <div className="flex items-center gap-3 pl-6">
                      <label className="flex items-baseline gap-2">
                        <span className="font-serif text-[12px] italic text-muted-foreground">
                          ~ duration (sec)
                        </span>
                        <Input
                          type="number"
                          min={1}
                          value={step.expectedDurationSeconds ?? ''}
                          onChange={(e) => {
                            const v = e.target.value === '' ? null : Number(e.target.value);
                            updateStep(idx, { expectedDurationSeconds: v });
                          }}
                          className="h-8 w-24 rounded-none border-0 border-b border-border/70 bg-transparent px-0 font-mono text-[12px] shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
                        />
                      </label>
                      <Input
                        value={step.notes ?? ''}
                        onChange={(e) => updateStep(idx, { notes: e.target.value || null })}
                        placeholder="Notes (optional)"
                        className="h-8 flex-1 rounded-none border-0 border-b border-border/70 bg-transparent px-0 font-serif text-[13px] italic text-muted-foreground shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
                      />
                    </div>
                  </li>
                ))}
              </ol>
            )}
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={addStep}
              className="mt-3 h-8 gap-1.5 px-2 font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground hover:text-foreground"
            >
              <Plus className="size-3" strokeWidth={2.25} />
              Add step
            </Button>
          </div>

          <div className="flex items-center gap-3 border-t border-border/40 pt-4">
            <Button
              type="submit"
              size="sm"
              disabled={saving}
              className="h-9 rounded-[6px] bg-primary px-4 font-mono text-[11px] uppercase tracking-[0.18em] hover:bg-primary/90"
            >
              {saving ? 'Saving…' : 'Save'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleDelete}
              className="h-9 px-3 font-mono text-[11px] uppercase tracking-[0.18em] text-muted-foreground hover:text-primary"
            >
              Delete workflow
            </Button>
            {error && <span className="font-mono text-[11px] text-primary">{error}</span>}
          </div>
        </form>
      )}
    </div>
  );
}
