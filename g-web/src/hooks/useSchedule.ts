'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import * as api from '../lib/api';
import { toIsoDate } from '../lib/date';
import type { Schedule } from '../types/schedule';

const GROUP_KEY = 'gradinator.selectedGroup';

function readStoredGroup(): string | null {
  try {
    return localStorage.getItem(GROUP_KEY);
  } catch {
    return null;
  }
}

function writeStoredGroup(group: string | null) {
  try {
    if (group) {
      localStorage.setItem(GROUP_KEY, group);
    } else {
      localStorage.removeItem(GROUP_KEY);
    }
  } catch {
    /* ignore */
  }
}

function addDays(iso: string, delta: number) {
  const d = new Date(`${iso}T00:00:00`);
  d.setDate(d.getDate() + delta);
  return toIsoDate(d);
}

type UseScheduleOptions = {
  /**
   * Если false — список групп грузится, но само расписание не запрашивается.
   * Используется на страницах, где расписание не нужно (/profile и т.п.),
   * чтобы не гонять тяжёлые запросы ради скрытого виджета.
   */
  scheduleEnabled?: boolean;
};

/**
 * preferredGroup — «домашняя» группа авторизованного пользователя.
 * Используется как группа по умолчанию, если пользователь ещё не выбирал
 * группу вручную в этом браузере.
 */
export default function useSchedule(
  preferredGroup?: string | null,
  options?: UseScheduleOptions
) {
  const scheduleEnabled = options?.scheduleEnabled ?? true;

  const [groups, setGroups] = useState<string[]>([]);
  const [groupsByDepartment, setGroupsByDepartment] = useState<Record<string, string[]>>({});
  const [department, setDepartmentState] = useState<string>(''); // '' = все отделения
  const [group, setGroupState] = useState<string>('');
  const [date, setDateState] = useState<string>(() => toIsoDate(new Date()));
  const [schedule, setSchedule] = useState<Schedule | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);

  const initializedGroup = useRef(false);
  const autoAppliedPreferred = useRef(false);
  const requestId = useRef(0);
  const abortRef = useRef<AbortController | null>(null);

  // Эффект 1: мгновенно подхватываем сохранённую группу из localStorage,
  // НЕ дожидаясь fetchGroups — запрос расписания стартует параллельно
  // со списком групп.
  useEffect(() => {
    if (initializedGroup.current) return;
    const stored = readStoredGroup();
    if (stored) {
      initializedGroup.current = true;
      autoAppliedPreferred.current = true;
      setGroupState(stored);
    }
  }, []);

  // Эффект 2: список групп (кэширован) + дозаполнение группы по умолчанию,
  // если в localStorage ничего не было.
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const [gs, byDept] = await Promise.all([
          api.fetchGroups(),
          api.fetchGroupsByDepartment()
        ]);
        if (cancelled) return;

        setGroups(gs);
        setGroupsByDepartment(byDept);

        if (initializedGroup.current) return;

        initializedGroup.current = true;

        const stored = readStoredGroup();

        if (stored && gs.includes(stored)) {
          autoAppliedPreferred.current = true;
          setGroupState(stored);
        } else {
          if (stored) writeStoredGroup(null); // протухшая группа — сбрасываем

          if (preferredGroup && gs.includes(preferredGroup)) {
            autoAppliedPreferred.current = true;
            setGroupState(preferredGroup);
          } else if (gs.length) {
            setGroupState(gs[0]);
          }
        }
      } catch (e) {
        if (!cancelled) console.error(e);
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // если пользователь входит в аккаунт уже после первичной загрузки —
  // подхватываем его группу, если он ещё не выбирал группу вручную
  useEffect(() => {
    if (!preferredGroup || autoAppliedPreferred.current) return;

    const stored = readStoredGroup();

    if (stored) {
      autoAppliedPreferred.current = true;
      return;
    }

    if (groups.includes(preferredGroup)) {
      autoAppliedPreferred.current = true;
      setGroupState(preferredGroup);
    }
  }, [preferredGroup, groups]);

  // Загрузка расписания с защитой от гонок:
  // предыдущий запрос отменяется, устаревшие ответы игнорируются.
  const load = useCallback(async (g: string, d: string) => {
    if (!g) return;

    abortRef.current?.abort();
    const ctrl = new AbortController();
    abortRef.current = ctrl;
    const id = ++requestId.current;

    setLoading(true);
    setError(null);
    setWarning(null);

    try {
      const data = await api.fetchSchedule(g, d, ctrl.signal);

      if (id !== requestId.current) return;

      if ((data as { error?: string })?.error) {
        setWarning((data as { error: string }).error);
        setSchedule(null);
      } else {
        setSchedule(data as Schedule);
        setUpdatedAt(new Date());
      }
    } catch (e: any) {
      if (e?.name === 'AbortError') return;
      if (id !== requestId.current) return;
      setError(e?.message ?? String(e));
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (scheduleEnabled && group) load(group, date);
  }, [scheduleEnabled, group, date, load]);

  const setGroup = useCallback((g: string) => {
    autoAppliedPreferred.current = true;
    setGroupState(g);
    writeStoredGroup(g);
  }, []);

  // Выбор отделения фильтрует список групп в комбо (Header, профиль).
  // '' — сброс фильтра, показываем все группы как раньше.
  const setDepartment = useCallback((d: string) => setDepartmentState(d), []);

  const setDate = useCallback((iso: string) => setDateState(iso), []);
  const prevDate = useCallback(() => setDateState((d) => addDays(d, -1)), []);
  const nextDate = useCallback(() => setDateState((d) => addDays(d, 1)), []);
  const goToday = useCallback(() => setDateState(toIsoDate(new Date())), []);

  const refresh = useCallback(() => load(group, date), [load, group, date]);

  const departments = useMemo(
    () => Object.keys(groupsByDepartment),
    [groupsByDepartment]
  );

  return useMemo(
    () => ({
      groups,
      groupsByDepartment,
      departments,
      department,
      setDepartment,
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
    }),
    [
      groups,
      groupsByDepartment,
      departments,
      department,
      group,
      date,
      schedule,
      loading,
      error,
      warning,
      updatedAt,
      setGroup,
      setDate,
      prevDate,
      nextDate,
      goToday,
      refresh
    ]
  );
}
