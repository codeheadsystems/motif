import { useEffect, useState, type FormEvent } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';

type Status =
  | { kind: 'idle' }
  | { kind: 'working' }
  | { kind: 'success' }
  | { kind: 'error'; message: string };

export function ChangePasswordDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { changePassword } = useAuth();
  const [oldPass, setOldPass] = useState('');
  const [newPass, setNewPass] = useState('');
  const [confirmPass, setConfirmPass] = useState('');
  const [status, setStatus] = useState<Status>({ kind: 'idle' });

  // Reset form state whenever the dialog is opened or closed.
  useEffect(() => {
    if (!open) {
      setOldPass('');
      setNewPass('');
      setConfirmPass('');
      setStatus({ kind: 'idle' });
    }
  }, [open]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!oldPass) {
      setStatus({ kind: 'error', message: 'Current password is required.' });
      return;
    }
    if (newPass.length < 8) {
      setStatus({ kind: 'error', message: 'New password must be at least 8 characters.' });
      return;
    }
    if (newPass !== confirmPass) {
      setStatus({ kind: 'error', message: 'New passwords do not match.' });
      return;
    }
    setStatus({ kind: 'working' });
    try {
      await changePassword(oldPass, newPass);
      setStatus({ kind: 'success' });
      setOldPass('');
      setNewPass('');
      setConfirmPass('');
    } catch (err) {
      const raw = err instanceof Error ? err.message : String(err);
      const msg = raw.includes('recoverEnvelope') || raw.includes('auth tag')
        ? 'Current password is incorrect.'
        : raw;
      setStatus({ kind: 'error', message: msg });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-serif text-2xl italic font-normal">
            Change password
          </DialogTitle>
          <DialogDescription className="font-mono text-[10px] uppercase tracking-[0.22em]">
            Re-encrypt your OPAQUE envelope
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <PasswordField
            id="cp-old"
            label="Current password"
            value={oldPass}
            onChange={setOldPass}
            autoComplete="current-password"
          />
          <PasswordField
            id="cp-new"
            label="New password"
            value={newPass}
            onChange={setNewPass}
            autoComplete="new-password"
            hint="Minimum 8 characters"
          />
          <PasswordField
            id="cp-confirm"
            label="Confirm new password"
            value={confirmPass}
            onChange={setConfirmPass}
            autoComplete="new-password"
          />

          {status.kind === 'error' && (
            <p className="font-mono text-[11px] text-primary">
              {status.message}
            </p>
          )}
          {status.kind === 'success' && (
            <p className="font-mono text-[11px] text-muted-foreground">
              Password updated.
            </p>
          )}
          {status.kind === 'working' && (
            <p className="font-mono text-[11px] text-muted-foreground">
              Re-encrypting…
            </p>
          )}

          <DialogFooter className="gap-2 sm:gap-2">
            <Button
              type="button"
              variant="ghost"
              onClick={() => onOpenChange(false)}
              className="font-mono text-[11px] uppercase tracking-[0.18em]"
            >
              Close
            </Button>
            <Button
              type="submit"
              disabled={status.kind === 'working'}
              className={cn(
                'font-mono text-[11px] uppercase tracking-[0.18em]',
              )}
            >
              Update
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function PasswordField({
  id,
  label,
  value,
  onChange,
  autoComplete,
  hint,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  autoComplete: string;
  hint?: string;
}) {
  return (
    <div className="space-y-1.5">
      <Label
        htmlFor={id}
        className="font-mono text-[10px] uppercase tracking-[0.2em] text-muted-foreground"
      >
        {label}
      </Label>
      <Input
        id={id}
        type="password"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        className="h-9 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-sm shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
      />
      {hint && <p className="font-mono text-[10px] text-muted-foreground/70">{hint}</p>}
    </div>
  );
}
