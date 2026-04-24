import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { Sidebar } from './Sidebar';
import { CategoriesProvider } from '@/hooks/useCategoriesContext';

export function AppShell() {
  return (
    <CategoriesProvider>
      <div className="relative min-h-screen bg-background text-foreground font-sans">
        <Navbar />
        <div className="relative z-10 mx-auto grid max-w-[1320px] grid-cols-1 gap-x-12 gap-y-10 px-6 py-10 md:grid-cols-[232px_1fr] md:px-10 md:py-14">
          <Sidebar />
          <main className="min-w-0">
            <Outlet />
          </main>
        </div>
      </div>
    </CategoriesProvider>
  );
}
