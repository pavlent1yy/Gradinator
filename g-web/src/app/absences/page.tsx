'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../providers/AuthProvider';
import { useScheduleContext } from '../providers/ScheduleProvider';
import * as api from '../../lib/api';
import type { Absence, AbsenceStats, AbsenceType } from '../../lib/api';
import { formatDateLong, formatDateShort, isDayOff, toIsoDate } from '../../lib/date';
import { joinList, pickSlot } from '../../lib/schedule';
import type { Schedule } from '../../types/schedule';
import PageToolbar from '../../components/PageToolbar';
import { stripAlert } from '../../components/SubjectText';

const FALLBACK_PAIRS = [0, 1, 2, 3, 4, 5, 6];

const PERIODS: { key: keyof AbsenceStats; label: string }[] = [
  { key: 'week', label: 'Неделя' },
  { key: 'month', label: 'Месяц' },
  { key: 'semester', label: 'Семестр' },
  { key: 'total', label: 'Всё время' }
];

type Row = { pairNumber: number; subject: string | null };

function daysAgoIso(days: number) {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return toIsoDate(d);
}

export default function AbsencesPage() {
  const router = useRouter();
  const { user, initializing } = useAuthContext();
  const { date, setDate } = useScheduleContext();

  const [loadedSchedule, setLoadedSchedule] = useState<{ key: string; data: Schedule | null } | null>(null);
  const [dayAbsences, setDayAbsences] = useState<Absence[]>([]);
  const [history, setHistory] = useState<Absence[]>([]);
  const [stats, setStats] = useState<AbsenceStats | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const group = user?.group ?? null;
  const today = toIsoDate(new Date());
  const isFuture = date > today;
  const dayOff = isDayOff(date);
  const scheduleKey = group && !dayOff ? `${group}|${date}` : null;
  const scheduleLoaded = scheduleKey !== null && loadedSchedule?.key === scheduleKey;
  const schedule = scheduleLoaded ? loadedSchedule.data : null;
  const scheduleMissing = scheduleLoaded && schedule === null;

  useEffect(() => {
    if (!initializing && !user) router.replace('/login');
  }, [initializing, user, router]);

  const reloadSummary = useCallback(async () => {
    const [s, h] = await Promise.all([
      api.fetchAbsenceStats(),
      api.fetchAbsences(daysAgoIso(120), today)
    ]);
    setStats(s);
    setHistory(h);
  }, [today]);

  const reloadDay = useCallback(async () => {
    setDayAbsences(await api.fetchAbsences(date, date));
  }, [date]);

  useEffect(() => {
    if (!user) return;
    Promise.all([api.fetchAbsenceStats(), api.fetchAbsences(daysAgoIso(120), today)])
      .then(([s, h]) => {
        setStats(s);
        setHistory(h);
      })
      .catch((e) => setMessage(e.message));
  }, [user, today]);

  useEffect(() => {
    if (!user) return;
    api.fetchAbsences(date, date)
      .then(setDayAbsences)
      .catch((e) => setMessage(e.message));
  }, [user, date]);

  useEffect(() => {
    if (!scheduleKey || !group) return;

    const controller = new AbortController();

    api.fetchSchedule(group, date, controller.signal)
      .then((data) => setLoadedSchedule({ key: scheduleKey, data: 'error' in data ? null : data }))
      .catch((e) => {
        if (e?.name !== 'AbortError') setLoadedSchedule({ key: scheduleKey, data: null });
      });

    return () => controller.abort();
  }, [scheduleKey, group, date]);

  const rows = useMemo<Row[]>(() => {
    const byNumber = new Map<number, Row>();

    schedule?.pairs?.forEach((pair) => {
      const slot = pickSlot(pair, schedule.weekType);
      if (slot) byNumber.set(pair.pairNumber, { pairNumber: pair.pairNumber, subject: joinList(slot.subjects.map((t) => stripAlert(t).text), '') || null });
    });

    if (!schedule && scheduleMissing && !dayOff) {
      FALLBACK_PAIRS.forEach((n) => byNumber.set(n, { pairNumber: n, subject: null }));
    }

    dayAbsences.forEach((a) => {
      if (!byNumber.has(a.pairNumber)) byNumber.set(a.pairNumber, { pairNumber: a.pairNumber, subject: a.subject });
    });

    return [...byNumber.values()].sort((a, b) => a.pairNumber - b.pairNumber);
  }, [schedule, scheduleMissing, dayOff, dayAbsences]);

  const markByPair = useMemo(
    () => new Map(dayAbsences.map((a) => [a.pairNumber, a])),
    [dayAbsences]
  );

  const historyByDate = useMemo(() => {
    const map = new Map<string, Absence[]>();
    history.forEach((a) => map.set(a.date, [...(map.get(a.date) ?? []), a]));
    return [...map.entries()];
  }, [history]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setMessage(null);
    try {
      await action();
      await Promise.all([reloadDay(), reloadSummary()]);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Произошла ошибка');
    } finally {
      setBusy(false);
    }
  }

  function setPairState(row: Row, type: AbsenceType | null) {
    const current = markByPair.get(row.pairNumber)?.type ?? null;
    if (current === type) return;
    run(() => type
      ? api.markAbsence(date, row.pairNumber, type, row.subject)
      : api.unmarkAbsence(date, row.pairNumber));
  }

  if (initializing || !user) {
    return <div className="status-card" role="status">Загрузка…</div>;
  }

  return (
    <div className="page">
      <PageToolbar showGroupPicker={false}>
        <p className="absence-hint">Пропущенная пара — 2 часа, опоздание — 1 час.</p>
      </PageToolbar>
      <section className="absences" aria-labelledby="absences-title">
        <h1 id="absences-title" className="auth-title">Пропуски</h1>

        <div className="absence-stats">
          {PERIODS.map(({ key, label }) => {
            const period = stats?.[key];
            return (
              <div key={key} className="absence-stat">
                <div className="absence-stat-label">{label}</div>
                <div className="absence-stat-hours">{period ? period.hours : '—'}<span> ч</span></div>
                <div className="absence-stat-meta mono">
                  {period ? `${period.missedPairs} пар · ${period.lates} опозд.` : ''}
                </div>
              </div>
            );
          })}
        </div>

        <h2 className="absence-subtitle">{formatDateLong(date)}</h2>

        {message && <div className="status-card status-card--error" role="alert">{message}</div>}

        {!group ? (
          <div className="status-card">
            Укажи группу в <Link href="/profile">профиле</Link> — тогда появятся пары.
          </div>
        ) : isFuture ? (
          <div className="status-card">Будущие даты отметить нельзя.</div>
        ) : dayOff && dayAbsences.length === 0 ? (
          <div className="status-card">Воскресенье, пар нет.</div>
        ) : (
          <>
            {scheduleMissing && (
              <div className="status-card status-card--warn">
                Расписания на этот день нет, пары показаны по номерам.
              </div>
            )}

            {rows.length === 0 && !scheduleMissing && (
              <div className="status-card">Пар в этот день нет.</div>
            )}

            <div className="schedule">
              {rows.map((row) => {
                const state = markByPair.get(row.pairNumber)?.type ?? null;
                return (
                  <article key={row.pairNumber} className={`pair absence-pair${state ? ` absence-pair--${state.toLowerCase()}` : ''}`}>
                    <div className="pair-num">{row.pairNumber}</div>
                    <div className="pair-body">
                      <h3 className="subject">{row.subject ?? `Пара ${row.pairNumber}`}</h3>
                      <div className="absence-toggle" role="group" aria-label={`Пара ${row.pairNumber}`}>
                        <button type="button" disabled={busy} aria-pressed={state === null} onClick={() => setPairState(row, null)}>Был</button>
                        <button type="button" disabled={busy} aria-pressed={state === 'LATE'} onClick={() => setPairState(row, 'LATE')}>Опоздал</button>
                        <button type="button" disabled={busy} aria-pressed={state === 'MISSED'} onClick={() => setPairState(row, 'MISSED')}>Пропустил</button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>

            <div className="auth-actions">
              {schedule && rows.length > 0 && (
                <button type="button" className="btn btn-primary" disabled={busy} onClick={() => run(() => api.markAbsenceDay(date))}>
                  Пропустил весь день
                </button>
              )}
              {dayAbsences.length > 0 && (
                <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => run(() => api.clearAbsenceDay(date))}>
                  Очистить день
                </button>
              )}
            </div>
          </>
        )}

        <h2 className="absence-subtitle">Последние отметки</h2>
        {historyByDate.length === 0 ? (
          <div className="status-card">Отметок пока нет.</div>
        ) : (
          <ul className="absence-history">
            {historyByDate.map(([day, items]) => (
              <li key={day}>
                <button type="button" className="absence-history-item" onClick={() => setDate(day)}>
                  <span className="mono">{formatDateShort(day)}</span>
                  <span>
                    {items.filter((a) => a.type === 'MISSED').length} пр. · {items.filter((a) => a.type === 'LATE').length} опозд.
                  </span>
                  <strong>{items.reduce((sum, a) => sum + a.hours, 0)} ч</strong>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
