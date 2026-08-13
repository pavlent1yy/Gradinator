'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { Schedule } from '../types/schedule';
import * as api from '../lib/api';

const STORAGE_KEY = 'gradinator.selectedGroup';

function toIsoDate(d: Date) {
  return d.toISOString().slice(0, 10);
}

export default function useSchedule(initialDate?: string) {
  const todayIso = initialDate ?? toIsoDate(new Date());
  const [groups, setGroups] = useState<string[]>([]);
  const [group, setGroup] = useState<string>('');
  const [date, setDate] = useState<string>(todayIso);
  const [schedule, setSchedule] = useState<Schedule | null>(null);

  // new: separate warning (server-provided informational message) and error (network/parse failure)
  const [warning, setWarning] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);

  const inited = useRef(false);

  useEffect(() => {
    if (inited.current) return;
    inited.current = true;

    (async () => {
      try {
        const g = await api.fetchGroups();
        setGroups(g);

        try {
          const stored = typeof window !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
          if (stored && g.includes(stored)) {
            setGroup(stored);
          } else {
            setGroup(g.length ? g[0] : '');
          }
        } catch {
          setGroup(g.length ? g[0] : '');
        }
      } catch (e: any) {
        setError(e?.message ?? String(e));
      }
    })();
  }, []);

  useEffect(() => {
    try {
      if (group) localStorage.setItem(STORAGE_KEY, group);
    } catch {}
  }, [group]);

  useEffect(() => {
    if (!group) {
      setSchedule(null);
      setWarning(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setWarning(null);

    (async () => {
      try {
        const res = await api.fetchSchedule(group, date);
        if (cancelled) return;

        // If backend returned { error: "..."} treat it as a warning (not network error)
        if (res && typeof res === 'object' && 'error' in res && typeof (res as any).error === 'string') {
          setWarning((res as any).error);
          setSchedule(null);
          setUpdatedAt(new Date());
        } else {
          setWarning(null);
          setSchedule(res as Schedule);
          setUpdatedAt(new Date());
        }
      } catch (e: any) {
        if (cancelled) return;
        setError(e?.message ?? String(e));
        setSchedule(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => { cancelled = true; };
  }, [group, date]);

  const refresh = useCallback(() => {
    if (!group) return;
    setLoading(true);
    (async () => {
      try {
        const res = await api.fetchSchedule(group, date);
        // server warning?
        if (res && typeof res === 'object' && 'error' in res && typeof (res as any).error === 'string') {
          setWarning((res as any).error);
          setSchedule(null);
          setUpdatedAt(new Date());
        } else {
          setWarning(null);
          setSchedule(res as Schedule);
          setUpdatedAt(new Date());
        }
      } catch (e: any) {
        setError(e?.message ?? String(e));
      } finally {
        setLoading(false);
      }
    })();
  }, [group, date]);

  const prevDate = useCallback(() => {
    setDate(d => {
      const cur = new Date(d + 'T00:00:00');
      cur.setDate(cur.getDate() - 1);
      return toIsoDate(cur);
    });
  }, []);

  const nextDate = useCallback(() => {
    setDate(d => {
      const cur = new Date(d + 'T00:00:00');
      cur.setDate(cur.getDate() + 1);
      return toIsoDate(cur);
    });
  }, []);

  const setDateIso = useCallback((iso: string) => setDate(iso), []);

  return {
    groups,
    group,
    setGroup,
    date,
    setDate: setDateIso,
    prevDate,
    nextDate,
    schedule,
    refresh,
    loading,
    error,
    warning,
    updatedAt
  } as const;
}