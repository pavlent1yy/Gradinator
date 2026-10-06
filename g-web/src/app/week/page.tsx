'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useScheduleContext } from '../providers/ScheduleProvider';
import PageToolbar from '../../components/PageToolbar';
import * as api from '../../lib/api';
import { addDaysIso, formatDateShort, formatDayName, toIsoDate, weekStartIso } from '../../lib/date';
import { joinList, pickSlot } from '../../lib/schedule';
import type { WeekDay } from '../../lib/api';
import SubjectText from '../../components/SubjectText';

const WORK_DAYS = 6;

export default function WeekPage() {
  const { group, date, setDate } = useScheduleContext();
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
                        <SubjectText subjects={slot?.subjects} />
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
