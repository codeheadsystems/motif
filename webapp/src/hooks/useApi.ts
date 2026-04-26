import { useCallback, useEffect, useState } from 'react';
import * as api from '@/api';

type Resource<T> = {
  data: T | null;
  error: Error | null;
  loading: boolean;
  refetch: () => Promise<void>;
};

function useResource<T>(fetcher: () => Promise<T>, deps: ReadonlyArray<unknown>): Resource<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [loading, setLoading] = useState(true);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetcher();
      setData(result);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
    } finally {
      setLoading(false);
    }
  }, deps);

  useEffect(() => {
    load();
  }, [load]);

  return { data, error, loading, refetch: load };
}

const EMPTY_PAGE = { items: [], pageNumber: 0, pageSize: 0, hasMore: false };

export function useCategories() {
  return useResource<api.Page<api.Category>>(() => api.getCategories(), []);
}

export function useCategory(id: string | null) {
  return useResource<api.Category | null>(
    () => (id ? api.getCategory(id) : Promise.resolve(null)),
    [id],
  );
}

export function useSubjects(categoryId: string | null) {
  return useResource<api.Page<api.Subject>>(
    () => (categoryId ? api.getSubjects(categoryId) : Promise.resolve(EMPTY_PAGE)),
    [categoryId],
  );
}

export function useSubject(id: string | null) {
  return useResource<api.Subject | null>(
    () => (id ? api.getSubject(id) : Promise.resolve(null)),
    [id],
  );
}

export function useEvents(subjectId: string | null) {
  return useResource<api.Page<api.Event>>(
    () => (subjectId ? api.getEvents(subjectId) : Promise.resolve(EMPTY_PAGE)),
    [subjectId],
  );
}

export function useNotes(subjectId: string | null) {
  return useResource<api.Page<api.Note>>(
    () => (subjectId ? api.getNotes(subjectId) : Promise.resolve(EMPTY_PAGE)),
    [subjectId],
  );
}

export function useRecentActivity(size = 20) {
  return useResource(async () => {
    const [events, notes] = await Promise.all([
      api.getRecentEvents(size),
      api.getRecentNotes(size),
    ]);
    return { events: events.items, notes: notes.items };
  }, [size]);
}
