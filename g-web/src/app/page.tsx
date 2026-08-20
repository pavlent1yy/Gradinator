'use client';

import useSchedule from '../hooks/useSchedule';
import Combo from '../components/Combo';
import DateNav from '../components/DateNav';
import type { Pair } from '../types/schedule';

export default function Page() {
  const {
    groups,
    group,
    setGroup,
    date,
    prevDate,
    nextDate,
    setDate,    // <-- используется для onPick
    schedule,
    refresh,
    loading,
    error,
    warning,
    updatedAt
  } = useSchedule();

  const anyNumeratorPairs = schedule?.pairs?.some(p => p.numerator && !p.numerator.empty);

  return (
    <main className="app" id="app">
      <header className="masthead" aria-labelledby="page-title">
        <div className="mast-left">
          <div className="meta-row">
            <div>
              <div className="day" id="day-label">{schedule?.day || '—'}</div>
              {/* IMPORTANT: show the requested date immediately (date state), not schedule.date */}
			<DateNav dateIso={date} onPrev={prevDate} onNext={nextDate} onPick={setDate} />
            </div>
            <div className="week-badge" id="week-badge" aria-hidden="true">
              {schedule?.weekType === 'NUMERATOR' ? 'Числитель' : 'Числитель'}
            </div>
          </div>

          <div className="group-row" aria-hidden="false">
            <Combo label="Выбрать группу" options={groups} value={group} onChange={setGroup} />
          </div>
        </div>

        <div className="mast-right">
          <div className="logo" aria-hidden="true">GradInator</div>
          <div className="controls">
            <button
              id="refreshBtn"
              className={`icon-btn ${loading ? 'btn-spin' : ''}`}
              aria-label="Обновить расписание"
              title="Обновить"
              onClick={refresh}
              aria-busy={loading}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M21 12a9 9 0 1 0-3.5 6.9"></path>
                <polyline points="21 3 21 9 15 9"></polyline>
              </svg>
            </button>

            <div id="updatedAt" className="updated-note" aria-live="polite">
              {updatedAt ? `Информация от ${updatedAt.toLocaleString()}` : 'Информация от —'}
            </div>
          </div>
        </div>
      </header>

      <section className="schedule" id="schedule" aria-label="Расписание">
        {/* Server-side informational warning (not network error) */}
        {warning && (
          <div className="pair" style={{ gridTemplateColumns: '1fr', padding: 12 }}>
            <div className="warning-note"><strong>Подождите:</strong> {warning}</div>
          </div>
        )}

        {error && <div className="pair" style={{ padding: 12 }}>Ошибка: {error}</div>}
        {!schedule && !error && !warning && <div className="pair" style={{ padding: 12 }}>Загрузка...</div>}

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

      <footer className="page-foot">
        <div className="foot-left mono">Источник: учебный план — 2026</div>
        <div className="foot-right">GradInator · analog editorial</div>
      </footer>
    </main>
  );
}