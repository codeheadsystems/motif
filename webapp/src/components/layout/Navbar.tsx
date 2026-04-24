import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useAuth } from '@/hooks/useAuth';
import { ProfileDialog } from '@/components/dialogs/ProfileDialog';
import { ChangePasswordDialog } from '@/components/dialogs/ChangePasswordDialog';

export function Navbar() {
  const { credentialId, logout } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);
  const [changePwOpen, setChangePwOpen] = useState(false);

  return (
    <>
      <header className="sticky top-0 z-20 border-b border-border/70 bg-background/85 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-[1320px] items-center justify-between px-6 md:px-10">
          <Link to="/" className="flex items-baseline gap-3 outline-none">
            <span
              className="font-serif text-[26px] leading-none tracking-tight"
              style={{ fontVariationSettings: "'opsz' 144" }}
            >
              <span className="italic">M</span>otif
            </span>
            <span className="hidden font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground sm:inline">
              № 001 — a commonplace book
            </span>
          </Link>

          {credentialId && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="h-8 gap-1.5 px-2 font-normal text-sm hover:bg-transparent hover:text-foreground"
                >
                  <span className="font-mono text-[11px] text-muted-foreground">{credentialId}</span>
                  <ChevronDown className="size-3.5 text-muted-foreground" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" sideOffset={8} className="w-52 font-sans">
                <DropdownMenuLabel className="pb-1 font-mono text-[10px] uppercase tracking-[0.2em] text-muted-foreground">
                  Signed in as
                </DropdownMenuLabel>
                <DropdownMenuLabel className="pt-0 font-serif text-base italic font-normal">
                  {credentialId}
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onSelect={() => setProfileOpen(true)}>Profile</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => setChangePwOpen(true)}>
                  Change password
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onSelect={() => { void logout(); }}
                  className="text-primary focus:text-primary"
                >
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </header>

      <ProfileDialog open={profileOpen} onOpenChange={setProfileOpen} />
      <ChangePasswordDialog open={changePwOpen} onOpenChange={setChangePwOpen} />
    </>
  );
}
