'use client';

import { useMemo } from 'react';
import { useScheduleContext } from './providers/ScheduleProvider';
import type { Pair } from '../types/schedule';

export default function Page() {
  const {
    schedule,
    loading,
    error,
    warning
  } = useScheduleContext();

  const anyNumeratorPairs = useMemo(() => schedule?.pairs?.some(p => p.numerator && !p.numerator.empty), [schedule]);

  return (
    <section className="schedule" id="schedule" aria-label="Расписание">
      {/* Server-side informational warning (not network error) */}
      {warning && (
        <div className="pair" style={{ gridTemplateColumns: '1fr', padding: 12 }}>
          <div className="warning-note"><strong>Подождите:</strong> {warning}</div>
        </div>
      )}

      {error && <div className="pair" style={{ padding: 12 }}>Ошибка: {error}</div>}
      {!schedule && !error && !warning && <div className="pair" style={{ padding: 12 }}>{loading ? 'Загрузка...' : 'Нет данных'}</div>}

      {schedule && schedule.pairs?.slice().sort((a,b)=> (a.pairNumber||0)-(b.pairNumber||0)).map((pair: Pair) => {
        const numerator = pair.numerator;
        if (!numerator || numerator.empty) return null;
        return (
          <div key={pair.pairNumber} className="pair">
            <div className="pair-num">{pair.pairNumber ?? '—'}</div>
            <div className="pair-body">
              <div className="entry">
                <h3 className="subject">{(numerator.subjects && numerator.subjects[0]) || 'Предмет'}</h3>
                <div className="meta-row-lesson">
                  <span className="room mono">{(numerator.rooms && numerator.rooms[0]) || '—'}</span>
                  <span className="teachers">{(numerator.teachers || []).join(', ')}</span>
                </div>
              </div>
            </div>
          </div>
        );
      })}

      {schedule && !anyNumeratorPairs && (
        <div className="pair" style={{ gridTemplateColumns: '1fr', padding: 12 }}>На этой неделе занятий по числителю нет.</div>
      )}
    </section>
  );
}