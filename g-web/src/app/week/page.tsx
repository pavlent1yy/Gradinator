'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useScheduleContext } from '../providers/ScheduleProvider';
import { useAuthContext } from '../providers/AuthProvider';
import PageToolbar from '../../components/PageToolbar';
import * as api from '../../lib/api';
import { addDaysIso, formatDateShort, formatDayName, toIsoDate, weekStartIso } from '../../lib/date';
import { joinList, pickSlot } from '../../lib/schedule';
import type { Absence, WeekDay } from '../../lib/api';
import SubjectText, { isCancelled } from '../../components/SubjectText';

const WORK_DAYS = 6;

export default function WeekPage() {
  const { group, date, setDate } = useScheduleContext();
  const { user } = useAuthContext();
  const weekStart = weekStartIso(date);
  const weekEnd = addDaysIso(weekStart, WORK_DAYS - 1);
  const key = group ? `${group}|${weekStart}` : null;

  const [loaded, setLoaded] = useState<{ key: string; days: WeekDay[] | null; error: string | null } | null>(null);

  useEffect(() => {
    if (!key || !group) return;

    const controller = new AbortController();

    api.fetchWeek(group, weekStart, controller.signal)
      .then((days) => setLoaded({ key, days, error: null }))
      .catch((e) => {
        if (e?.name !== 'AbortError') setLoaded({ key, days: null, error: e.message });
      });

    return () => controller.abort();
  }, [key, group, weekStart]);

  const absencesKey = user && group && user.group === group ? `${user.id}|${weekStart}` : null;
  const [loadedAbsences, setLoadedAbsences] = useState<{ key: string; items: Absence[] } | null>(null);

  useEffect(() => {
    if (!absencesKey) return;

    let cancelled = false;

    api.fetchAbsences(weekStart, weekEnd)
      .then((items) => {
        if (!cancelled) setLoadedAbsences({ key: absencesKey, items });
      })
      .catch(() => null);

    return () => {
      cancelled = true;
    };
  }, [absencesKey, weekStart, weekEnd]);

  const absenceByPair = new Map(
    (absencesKey && loadedAbsences?.key === absencesKey ? loadedAbsences.items : [])
      .map((a) => [`${a.date}|${a.pairNumber}`, a] as const)
  );

  const current = loaded?.key === key ? loaded : null;
  const result = current?.days ?? null;
  const today = toIsoDate(new Date());

  return (
    <div className="page">
      <PageToolbar showDate={false}>
        <div className="week-nav">
          <button type="button" className="date-btn" aria-label="Предыдущая неделя" onClick={() => setDate(addDaysIso(weekStart, -7))}>‹</button>
          <span className="date-label">{formatDateShort(weekStart)} — {formatDateShort(weekEnd)}</span>
          <button type="button" className="date-btn" aria-label="Следующая неделя" onClick={() => setDate(addDaysIso(weekStart, 7))}>›</button>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setDate(today)}>Эта неделя</button>
        </div>
      </PageToolbar>

      {!group && <div className="status-card">Выбери группу, чтобы увидеть неделю.</div>}
      {group && !current && <div className="status-card" role="status">Загрузка…</div>}
      {current?.error && <div className="status-card status-card--error" role="alert">Ошибка: {current.error}</div>}

      {result && (
        <div className="week-grid">
          {result.map(({ date: day, schedule }) => {
            const pairs = (schedule?.pairs ?? [])
              .slice()
              .sort((a, b) => a.pairNumber - b.pairNumber)
              .map((pair) => ({ pair, slot: pickSlot(pair, schedule?.weekType) }))
              .filter((item) => item.slot);

            const dayHours = pairs.reduce(
              (sum, { pair }) => sum + (absenceByPair.get(`${day}|${pair.pairNumber}`)?.hours ?? 0),
              0
            );

            return (
              <section key={day} className={`week-day${day === today ? ' is-today' : ''}`}>
                <header className="week-day-head">
                  <Link href="/" className="week-day-link" onClick={() => setDate(day)}>
                    {formatDayName(day)}
                  </Link>
                  <span className="week-day-date">
                    {dayHours > 0 && <span className="week-day-hours" title="Пропущено часов">−{dayHours} ч</span>}
                    <span className="mono">{formatDateShort(day)}</span>
                  </span>
                </header>

                {!schedule && <p className="week-empty">Нет данных</p>}
                {schedule && pairs.length === 0 && <p className="week-empty">Занятий нет</p>}

                {pairs.map(({ pair, slot }) => {
                  const absence = absenceByPair.get(`${day}|${pair.pairNumber}`);
                  return (
                  <div
                    key={pair.pairNumber}
                    className={`week-pair${pair.hasChanges && !isCancelled(slot?.subjects) ? ' pair--changed' : ''}${absence ? ` week-pair--${absence.type.toLowerCase()}` : ''}`}
                  >
                    <span className="week-pair-num">{pair.pairNumber}</span>
                    <div>
                      <div className="week-pair-subject">
                        <SubjectText subjects={slot?.subjects} hasChanges={pair.hasChanges} />
                        {absence && (
                          <span className={`absence-stamp absence-stamp--${absence.type.toLowerCase()}`}>
                            {absence.type === 'MISSED' ? 'пропуск' : 'опоздание'}
                          </span>
                        )}
                      </div>
                      <div className="week-pair-meta">
                        <span className="room mono">{joinList(slot?.rooms)}</span>
                        <span className="week-pair-teacher">{joinList(slot?.teachers)}</span>
                      </div>
                    </div>
                  </div>
                  );
                })}
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
