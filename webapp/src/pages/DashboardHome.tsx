import { QuickAdd } from '@/components/dashboard/QuickAdd';
import { Timeline, type TimelineEntry } from '@/components/dashboard/Timeline';
import { DiscoveredPatterns } from '@/components/dashboard/DiscoveredPatterns';
import { useCategoriesContext } from '@/hooks/useCategoriesContext';
import { useRecentActivity } from '@/hooks/useApi';
import { useMemo } from 'react';

export function DashboardHome() {
  const { data, loading, refetch } = useRecentActivity(30);
  const { data: categoriesPage } = useCategoriesContext();

  const categoryNameByUuid = useMemo(() => {
    const map = new Map<string, string>();
    for (const c of categoriesPage?.items ?? []) {
      map.set(c.identifier.uuid, c.name);
    }
    return map;
  }, [categoriesPage]);

  const entries = useMemo<TimelineEntry[]>(() => {
    if (!data) return [];
    const merged: TimelineEntry[] = [];
    for (const ev of data.events) {
      merged.push({
        id: `e:${ev.identifier.uuid}`,
        type: 'event',
        value: ev.value,
        category: categoryNameByUuid.get(ev.subject.categoryIdentifier.uuid) ?? '',
        subject: ev.subject.value,
        tags: ev.tags.map((t) => t.value),
        timestamp: ev.timestamp.timestamp,
      });
    }
    for (const n of data.notes) {
      merged.push({
        id: `n:${n.identifier.uuid}`,
        type: 'note',
        // Notes don't carry subject embedded. Show a short id placeholder for
        // the subject path — the detail page resolves it authoritatively.
        value: n.value,
        subject: n.subjectIdentifier.uuid.slice(0, 8),
        tags: n.tags.map((t) => t.value),
        timestamp: n.timestamp.timestamp,
      });
    }
    return merged.sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    );
  }, [data, categoryNameByUuid]);

  return (
    <div className="space-y-14">
      <QuickAdd onCreated={refetch} />
      <DiscoveredPatterns />
      <Timeline entries={entries} loading={loading} />
    </div>
  );
}
