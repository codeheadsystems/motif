import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useAuth } from '@/hooks/useAuth';

export function ProfileDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { credentialId } = useAuth();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-serif text-2xl italic font-normal">Profile</DialogTitle>
          <DialogDescription className="font-mono text-[10px] uppercase tracking-[0.22em]">
            Account details
          </DialogDescription>
        </DialogHeader>
        <dl className="mt-2 grid grid-cols-[120px_1fr] gap-y-3 text-sm">
          <dt className="font-mono text-[10px] uppercase tracking-[0.2em] text-muted-foreground self-center">
            Credential ID
          </dt>
          <dd className="font-mono text-[12px] break-all">{credentialId ?? 'unknown'}</dd>
          <dt className="font-mono text-[10px] uppercase tracking-[0.2em] text-muted-foreground self-center">
            Session
          </dt>
          <dd className="font-serif italic text-muted-foreground">
            Active — HttpOnly cookie
          </dd>
        </dl>
      </DialogContent>
    </Dialog>
  );
}
