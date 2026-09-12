'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import * as api from '../lib/api';
import { toIsoDate } from '../lib/date';
import type { Schedule } from '../types/schedule';

const GROUP_KEY = 'gradinator.selectedGroup';

function addDays(iso: string, delta: number) {
  const d = new Date(`${iso}T00:00:00`);
  d.setDate(d.getDate() + delta);
  return toIsoDate(d);
}

/**
 * preferredGroup — «домашняя» группа авторизованного пользователя.
 * Используется как группа по умолчанию, если пользователь ещё не выбирал
 * группу вручную в этом браузере.
 */
export default function useSchedule(preferredGroup?: string | null) {
  const [groups, setGroups] = useState<string[]>([]);
  const [group, setGroupState] = useState<string>('');
  const [date, setDateState] = useState<string>(() => toIsoDate(new Date()));
  const [schedule, setSchedule] = useState<Schedule | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);

  const initializedGroup = useRef(false);
  const autoAppliedPreferred = useRef(false);

  useEffect(() => {
    (async () => {
      try {
        const gs = await api.fetchGroups();
        setGroups(gs);
        if (!initializedGroup.current) {
          initializedGroup.current = true;
          let stored: string | null = null;
          try {
            stored = localStorage.getItem(GROUP_KEY);
          } catch {
            /* ignore */
          }
          if (stored && gs.includes(stored)) {
            setGroupState(stored);
            autoAppliedPreferred.current = true;
          } else if (preferredGroup && gs.includes(preferredGroup)) {
            setGroupState(preferredGroup);
            autoAppliedPreferred.current = true;
          } else if (gs.length) {
            setGroupState(gs[0]);
          }
        }
      } catch (e) {
        console.error(e);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // если пользователь входит в аккаунт уже после первичной загрузки —
  // подхватываем его группу, если он ещё не выбирал группу вручную
  useEffect(() => {
    if (!preferredGroup || autoAppliedPreferred.current) return;
    let stored: string | null = null;
    try {
      stored = localStorage.getItem(GROUP_KEY);
    } catch {
      /* ignore */
    }
    if (stored) {
      autoAppliedPreferred.current = true;
      return;
    }
    if (groups.includes(preferredGroup)) {
      autoAppliedPreferred.current = true;
      setGroupState(preferredGroup);
    }
  }, [preferredGroup, groups]);

  const load = useCallback(async (g: string, d: string) => {
    if (!g) return;
    setLoading(true);
    setError(null);
    setWarning(null);
    try {
      const data = await api.fetchSchedule(g, d);
      if ((data as { error?: string })?.error) {
        setWarning((data as { error: string }).error);
        setSchedule(null);
      } else {
        setSchedule(data as Schedule);
        setUpdatedAt(new Date());
      }
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (group) load(group, date);
  }, [group, date, load]);

  const setGroup = useCallback((g: string) => {
    autoAppliedPreferred.current = true;
    setGroupState(g);
    try {
      localStorage.setItem(GROUP_KEY, g);
    } catch {
      /* ignore */
    }
  }, []);

  const setDate = useCallback((iso: string) => setDateState(iso), []);
  const prevDate = useCallback(() => setDateState((d) => addDays(d, -1)), []);
  const nextDate = useCallback(() => setDateState((d) => addDays(d, 1)), []);
  const goToday = useCallback(() => setDateState(toIsoDate(new Date())), []);

  const refresh = useCallback(() => load(group, date), [load, group, date]);

  return {
    groups,
    group,
    setGroup,
    date,
    setDate,
    prevDate,
    nextDate,
    goToday,
    schedule,
    loading,
    error,
    warning,
    updatedAt,
    refresh
  };
}
