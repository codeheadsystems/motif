import { type ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AppShell } from './components/layout/AppShell';
import { AuthPage } from './pages/AuthPage';
import { DashboardHome } from './pages/DashboardHome';
import { CategoryPage } from './pages/CategoryPage';
import { ProjectPage } from './pages/ProjectPage';
import { SubjectDetailPage } from './pages/SubjectDetailPage';
import { WorkflowPage } from './pages/WorkflowPage';
import { useAuth } from './hooks/useAuth';

function RequireAuth({ children }: { children: ReactNode }) {
  const { credentialId, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <span className="font-mono text-[10px] uppercase tracking-[0.22em] text-muted-foreground">
          Loading
        </span>
      </div>
    );
  }
  if (!credentialId) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardHome />} />
        <Route path="c/:categoryId" element={<CategoryPage />} />
        <Route path="p/:projectId" element={<ProjectPage />} />
        <Route path="w/:workflowId" element={<WorkflowPage />} />
        <Route path="s/:subjectId" element={<SubjectDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
