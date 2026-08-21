'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useMemo } from 'react';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import { formatDateLong, formatDayName, weekTypeLabel } from '../lib/date';
import Combo from './Combo';
import DateNav from './DateNav';

export default function Header() {
  const pathname = usePathname();
  const isAuthPage = pathname === '/login' || pathname === '/register';

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
    updatedAt,
    schedule
  } = useScheduleContext();

  const dayLabel = useMemo(
    () => formatDayName(date, schedule?.day),
    [date, schedule?.day]
  );
  const dateLabel = useMemo(() => formatDateLong(date), [date]);
  const weekLabel = useMemo(
    () => weekTypeLabel(schedule?.weekType),
    [schedule?.weekType]
  );
  const updatedLabel = useMemo(() => {
    if (!updatedAt) return 'Информация от —';
    return `Информация от ${updatedAt.toLocaleString('ru-RU')}`;
  }, [updatedAt]);

  return (
    <header className="masthead" aria-labelledby="page-title">
      <div className="mast-row mast-row--top">
        <div className="meta-row">
          <div>
            <div className="day" id="day-label">{dayLabel}</div>
            <div className="date mono" id="date-label">{dateLabel}</div>
          </div>
          {!isAuthPage && weekLabel !== '—' && (
            <div className="week-badge" id="week-badge">{weekLabel}</div>
          )}
        </div>

        <div className="mast-top">
          <nav className="main-nav" aria-label="Основная навигация">
            <Link
              href="/register"
              className={`nav-link${pathname === '/register' ? ' is-active' : ''}`}
            >
              Регистрация
            </Link>
            <Link
              href="/login"
              className={`nav-link${pathname === '/login' ? ' is-active' : ''}`}
            >
              Вход
            </Link>
            {isAuthPage && (
              <Link href="/" className="nav-link">
                Расписание
              </Link>
            )}
          </nav>

          <div className="logo" id="page-title">GradInator</div>

          {!isAuthPage && (
            <div className="controls">
              <button
                id="refreshBtn"
                className={`icon-btn${loading ? ' btn-spin' : ''}`}
                aria-label="Обновить расписание"
                title="Обновить"
                onClick={refresh}
                aria-busy={loading}
                type="button"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M21 12a9 9 0 1 0-3.5 6.9" />
                  <polyline points="21 3 21 9 15 9" />
                </svg>
              </button>

              <div id="updatedAt" className="updated-note" aria-live="polite">
                {updatedLabel}
              </div>
            </div>
          )}
        </div>
      </div>

      {!isAuthPage && (
        <div className="mast-row mast-row--bottom">
          <div className="group-row">
            <Combo label="Выбрать группу" options={groups} value={group} onChange={setGroup} />
          </div>
          <DateNav dateIso={date} onPrev={prevDate} onNext={nextDate} onPick={setDate} />
        </div>
      )}
    </header>
  );
}
