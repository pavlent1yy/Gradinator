'use client';

import { useMemo } from 'react';
import { useScheduleContext } from './providers/ScheduleProvider';
import { isDayOff, weekTypeLabel } from '../lib/date';
import { joinList, pickSlot } from '../lib/schedule';
import PageToolbar from '../components/PageToolbar';

export default function Page() {
  const { schedule, loading, error, warning, date, nextDate } = useScheduleContext();

  const dayOff = isDayOff(date);
  const weekType = schedule?.weekType;
  const weekLabel = weekTypeLabel(weekType).toLowerCase();

  const visiblePairs = useMemo(() => {
    if (!schedule?.pairs) return [];
    return schedule.pairs
      .slice()
      .sort((a, b) => (a.pairNumber || 0) - (b.pairNumber || 0))
      .map((pair) => ({ pair, slot: pickSlot(pair, weekType) }))
      .filter((item) => item.slot);
  }, [schedule, weekType]);

  return (
    <div className="page">
      <PageToolbar showRefresh />
      <section className="schedule" id="schedule" aria-label="Расписание">
        {warning && (
          <div className="status-card status-card--warn" role="status">
            <strong>Подожди:</strong> {warning}
          </div>
        )}

        {error && (
          <div className="status-card status-card--error" role="alert">
            Ошибка: {error}
          </div>
        )}

        {!schedule && !error && !warning && (
          <div className="status-card" role="status">
            {loading ? 'Загрузка…' : 'Нет данных'}
          </div>
        )}

        {visiblePairs.map(({ pair, slot }) => (
          <article key={pair.pairNumber} className={`pair${pair.hasChanges ? ' pair--changed' : ''}`}>
            <div className="pair-num">{pair.pairNumber ?? '—'}</div>
            <div className="pair-body">
              <div className="entry">
                <h3 className="subject">
                  {joinList(slot?.subjects, 'Предмет')}
                  {pair.hasChanges && <span className="changed-stamp">замена</span>}
                </h3>
                <div className="meta-row-lesson">
                  <span className="room mono">{joinList(slot?.rooms)}</span>
                  <span className="teachers">{joinList(slot?.teachers)}</span>
                </div>
              </div>
            </div>
          </article>
        ))}

        {schedule && !loading && visiblePairs.length === 0 && !warning && !error && (
          dayOff ? (
            <div className="status-card" role="status">
              Воскресенье, пар нет.{' '}
              <button type="button" className="btn btn-ghost" onClick={nextDate}>
                Расписание на понедельник
              </button>
            </div>
          ) : (
            <div className="status-card" role="status">
              На этой неделе ({weekLabel}) занятий нет.
            </div>
          )
        )}
      </section>
    </div>
  );
}
