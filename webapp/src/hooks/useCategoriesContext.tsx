import { createContext, useContext, type ReactNode } from 'react';
import { useCategories } from './useApi';

type CategoriesContextValue = ReturnType<typeof useCategories>;

const CategoriesContext = createContext<CategoriesContextValue | null>(null);

export function CategoriesProvider({ children }: { children: ReactNode }) {
  const resource = useCategories();
  return <CategoriesContext.Provider value={resource}>{children}</CategoriesContext.Provider>;
}

export function useCategoriesContext(): CategoriesContextValue {
  const ctx = useContext(CategoriesContext);
  if (!ctx) throw new Error('useCategoriesContext must be used within CategoriesProvider');
  return ctx;
}
