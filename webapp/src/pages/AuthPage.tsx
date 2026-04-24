import { useEffect, useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useAuth } from '@/hooks/useAuth';

type Tab = 'login' | 'register';

type Status =
  | { kind: 'idle' }
  | { kind: 'working'; label: string }
  | { kind: 'success'; label: string }
  | { kind: 'error'; message: string };

export function AuthPage() {
  const { credentialId, loading, login, register } = useAuth();
  const location = useLocation();
  const [tab, setTab] = useState<Tab>('login');
  const [user, setUser] = useState('');
  const [pass, setPass] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState<Status>({ kind: 'idle' });

  useEffect(() => {
    setStatus({ kind: 'idle' });
    setPass('');
    setConfirm('');
  }, [tab]);

  if (loading) {
    return <FullScreenShimmer />;
  }

  if (credentialId) {
    const from = (location.state as { from?: string } | null)?.from ?? '/';
    return <Navigate to={from} replace />;
  }

  async function handleLogin(e: FormEvent) {
    e.preventDefault();
    if (!user.trim() || !pass) return;
    setStatus({ kind: 'working', label: 'Authenticating…' });
    try {
      await login(user.trim(), pass);
    } catch (err) {
      setStatus({
        kind: 'error',
        message: err instanceof Error ? err.message : String(err),
      });
    }
  }

  async function handleRegister(e: FormEvent) {
    e.preventDefault();
    if (!user.trim()) return;
    if (pass.length < 8) {
      setStatus({ kind: 'error', message: 'Password must be at least 8 characters.' });
      return;
    }
    if (pass !== confirm) {
      setStatus({ kind: 'error', message: 'Passwords do not match.' });
      return;
    }
    setStatus({ kind: 'working', label: 'Registering…' });
    try {
      await register(user.trim(), pass);
      setStatus({ kind: 'success', label: 'Registered. You can now sign in.' });
      setTab('login');
      setPass('');
      setConfirm('');
    } catch (err) {
      setStatus({
        kind: 'error',
        message: err instanceof Error ? err.message : String(err),
      });
    }
  }

  return (
    <div className="relative min-h-screen bg-background text-foreground font-sans">
      <div className="relative z-10 mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-12">
        <div className="mb-10 space-y-3">
          <h1
            className="font-serif text-[48px] leading-none tracking-tight"
            style={{ fontVariationSettings: "'opsz' 144" }}
          >
            <span className="italic">M</span>otif
          </h1>
          <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
            № 001 — a commonplace book
          </p>
          <p className="max-w-sm pt-4 font-serif text-[15px] italic leading-relaxed text-muted-foreground">
            A private ledger for events, notes, and the small details that otherwise
            slip away.
          </p>
        </div>

        <Tabs value={tab} onValueChange={(v) => setTab(v as Tab)}>
          <TabsList className="mb-6 h-9 gap-0.5 rounded-[6px] bg-secondary/70 p-0.5">
            <TabsTrigger
              value="login"
              className="h-8 rounded-[4px] px-4 font-mono text-[10px] uppercase tracking-[0.18em] data-[state=active]:bg-background data-[state=active]:shadow-sm"
            >
              Sign in
            </TabsTrigger>
            <TabsTrigger
              value="register"
              className="h-8 rounded-[4px] px-4 font-mono text-[10px] uppercase tracking-[0.18em] data-[state=active]:bg-background data-[state=active]:shadow-sm"
            >
              Register
            </TabsTrigger>
          </TabsList>

          <TabsContent value="login" className="mt-0">
            <form onSubmit={handleLogin} className="space-y-5">
              <AuthField id="login-user" label="Username" value={user} onChange={setUser} autoComplete="username" />
              <AuthField id="login-pass" label="Password" value={pass} onChange={setPass} type="password" autoComplete="current-password" />
              <AuthFooter
                status={status}
                submitLabel="Sign in"
                disabled={status.kind === 'working'}
              />
            </form>
          </TabsContent>

          <TabsContent value="register" className="mt-0">
            <form onSubmit={handleRegister} className="space-y-5">
              <AuthField id="reg-user" label="Username" value={user} onChange={setUser} autoComplete="username" />
              <AuthField
                id="reg-pass"
                label="Password"
                value={pass}
                onChange={setPass}
                type="password"
                autoComplete="new-password"
                hint="Minimum 8 characters. Cannot be recovered — write it down."
              />
              <AuthField id="reg-confirm" label="Confirm password" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
              <AuthFooter
                status={status}
                submitLabel="Create account"
                disabled={status.kind === 'working'}
              />
            </form>
          </TabsContent>
        </Tabs>

        <p className="mt-16 max-w-sm font-serif text-[12.5px] italic leading-relaxed text-muted-foreground/80">
          Your password is never sent to the server. Authentication uses OPAQUE — a
          password-authenticated key exchange. The server stores only an encrypted
          envelope that can’t be decrypted without the password itself.
        </p>
      </div>
    </div>
  );
}

function AuthField({
  id, label, value, onChange, type = 'text', autoComplete, hint,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  autoComplete?: string;
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
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        className="h-10 rounded-none border-0 border-b border-border/70 bg-transparent px-0 text-[15px] shadow-none focus-visible:border-foreground/40 focus-visible:ring-0 dark:bg-transparent"
      />
      {hint && <p className="font-serif text-[12px] italic text-muted-foreground/80">{hint}</p>}
    </div>
  );
}

function AuthFooter({
  status, submitLabel, disabled,
}: {
  status: Status;
  submitLabel: string;
  disabled: boolean;
}) {
  return (
    <div className="space-y-4 pt-2">
      <Button
        type="submit"
        disabled={disabled}
        className="h-10 w-full rounded-[6px] bg-primary font-mono text-[11px] uppercase tracking-[0.2em] hover:bg-primary/90"
      >
        {submitLabel}
      </Button>
      <div className="min-h-[1.25em]">
        {status.kind === 'working' && (
          <p className="font-mono text-[11px] text-muted-foreground/80">{status.label}</p>
        )}
        {status.kind === 'success' && (
          <p className="font-serif text-[13px] italic text-muted-foreground">{status.label}</p>
        )}
        {status.kind === 'error' && (
          <p className="font-mono text-[11px] text-primary">{status.message}</p>
        )}
      </div>
    </div>
  );
}

function FullScreenShimmer() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <span className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
        Loading
      </span>
    </div>
  );
}
