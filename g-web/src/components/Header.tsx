'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import Combo from './Combo';
import DateNav from './DateNav';

export default function Header() {
  const {
    groups,
    group,
    setGroup,
    date,
    prevDate,
    nextDate,
    setDate,
    refresh,
    loading,
    updatedAt
  } = useScheduleContext();

  const weekLabel = useMemo(() => (/* keep 'Числитель' when unknown */ 'Числитель'), []);

  return (
    <header className="masthead" aria-labelledby="page-title">
      <div className="mast-left">
        <div className="meta-row">
          <div>
            <div className="day" id="day-label">—</div>
            <div className="date mono" id="date-label">—</div>
          </div>
          <div className="week-badge" id="week-badge" aria-hidden="true">{weekLabel}</div>
        </div>

        <div className="group-row" aria-hidden="false" style={{ marginTop: 12 }}>
          <Combo label="Выбрать группу" options={groups} value={group} onChange={setGroup} />
        </div>
      </div>

      <div className="mast-right">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <nav aria-label="Основная навига">
            <Link href="/register" className="nav-link">
				Регистрация
			</Link>
			<Link href="/login" className="nav-link">
				Вход
			</Link>
          </nav>

          <div style={{ width: 12 }} />

          <div className="logo" aria-hidden="true">GradInator</div>

          <div className="controls" style={{ marginLeft: 8 }}>
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

        {/* Date navigation placed under logo area for accessibility */}
        <div style={{ marginTop: 8 }}>
          <DateNav dateIso={date} onPrev={prevDate} onNext={nextDate} onPick={setDate} />
        </div>
      </div>
    </header>
  );
}