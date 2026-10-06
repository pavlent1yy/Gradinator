'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useScheduleContext } from '../providers/ScheduleProvider';
import PageToolbar from '../../components/PageToolbar';
import * as api from '../../lib/api';
import { addDaysIso, formatDateShort, formatDayName, toIsoDate, weekStartIso } from '../../lib/date';
import { joinList, pickSlot } from '../../lib/schedule';
import type { Schedule } from '../../types/schedule';

const WORK_DAYS = 6;

type DayResult = { date: string; schedule: Schedule | null };

export default function WeekPage() {
  const { group, date, setDate } = useScheduleContext();
  const weekStart = weekStartIso(date);
  const days = useMemo(
    () => Array.from({ length: WORK_DAYS }, (_, i) => addDaysIso(weekStart, i)),
    [weekStart]
  );
  const key = group ? `${group}|${weekStart}` : null;

  const [loaded, setLoaded] = useState<{ key: string; days: DayResult[] } | null>(null);

  useEffect(() => {
    if (!key || !group) return;

    const controller = new AbortController();

    Promise.all(days.map((day) =>
      api.fetchSchedule(group, day, controller.signal)
        .then((data) => ({ date: day, schedule: 'error' in data ? null : data }))
        .catch(() => ({ date: day, schedule: null }))
    )).then((result) => {
      if (!controller.signal.aborted) setLoaded({ key, days: result });
    });

    return () => controller.abort();
  }, [key, group, days]);

  const result = loaded?.key === key ? loaded.days : null;
  const today = toIsoDate(new Date());

  return (
    <div className="page">
      <PageToolbar showDate={false}>
        <div className="week-nav">
          <button type="button" className="date-btn" aria-label="Предыдущая неделя" onClick={() => setDate(addDaysIso(weekStart, -7))}>‹</button>
          <span className="date-label">{formatDateShort(days[0])} — {formatDateShort(days[WORK_DAYS - 1])}</span>
          <button type="button" className="date-btn" aria-label="Следующая неделя" onClick={() => setDate(addDaysIso(weekStart, 7))}>›</button>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setDate(today)}>Эта неделя</button>
        </div>
      </PageToolbar>

      {!group && <div className="status-card">Выбери группу, чтобы увидеть неделю.</div>}
      {group && !result && <div className="status-card" role="status">Загрузка…</div>}

      {result && (
        <div className="week-grid">
          {result.map(({ date: day, schedule }) => {
            const pairs = (schedule?.pairs ?? [])
              .slice()
              .sort((a, b) => a.pairNumber - b.pairNumber)
              .map((pair) => ({ pair, slot: pickSlot(pair, schedule?.weekType) }))
              .filter((item) => item.slot);

            return (
              <section key={day} className={`week-day${day === today ? ' is-today' : ''}`}>
                <header className="week-day-head">
                  <Link href="/" className="week-day-link" onClick={() => setDate(day)}>
                    {formatDayName(day)}
                  </Link>
                  <span className="mono">{formatDateShort(day)}</span>
                </header>

                {!schedule && <p className="week-empty">Нет данных</p>}
                {schedule && pairs.length === 0 && <p className="week-empty">Занятий нет</p>}

                {pairs.map(({ pair, slot }) => (
                  <div key={pair.pairNumber} className={`week-pair${pair.hasChanges ? ' pair--changed' : ''}`}>
                    <span className="week-pair-num">{pair.pairNumber}</span>
                    <div>
                      <div className="week-pair-subject">
                        {joinList(slot?.subjects, 'Предмет')}
                        {pair.hasChanges && <span className="changed-stamp">замена</span>}
                      </div>
                      <div className="week-pair-meta mono">
                        {joinList(slot?.rooms)} · {joinList(slot?.teachers)}
                      </div>
                    </div>
                  </div>
                ))}
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
