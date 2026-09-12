'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useMemo } from 'react';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import { useAuthContext } from '../app/providers/AuthProvider';
import { formatDateLong, formatDayName, weekTypeLabel } from '../lib/date';
import Combo from './Combo';
import DateNav from './DateNav';

function initialsFromEmail(email: string) {
  const name = email.split('@')[0] || email;
  return name.slice(0, 2).toUpperCase();
}

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const isAuthPage = pathname === '/login' || pathname === '/register';

  const { user, initializing, logout } = useAuthContext();

  const {
    groups,
    group,
    setGroup,
    date,
    prevDate,
    nextDate,
    setDate,
    goToday,
    refresh,
    loading,
    updatedAt,
    schedule,
    homeGroup,
    isOwnGroup,
    goToOwnGroup
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

  async function onLogout() {
    await logout();
  }

  return (
    <header className="masthead" aria-labelledby="page-title">
      <div className="mast-row mast-row--top">
        <div className="meta-row">
          <div className="date-stamp">
            <div className="day" id="day-label">{dayLabel}</div>
            <div className="date mono" id="date-label">{dateLabel}</div>
          </div>
          {!isAuthPage && weekLabel !== '—' && (
            <div className="week-badge" id="week-badge">{weekLabel}</div>
          )}
        </div>

        <div className="mast-top">
          <nav className="main-nav" aria-label="Основная навигация">
            {!isAuthPage && (
              <Link href="/" className={`nav-link${pathname === '/' ? ' is-active' : ''}`}>
                Расписание
              </Link>
            )}
            {!user && !initializing && (
              <>
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
              </>
            )}
            {isAuthPage && (
              <Link href="/" className="nav-link">
                Отмена
              </Link>
            )}
          </nav>

          <div className="logo" id="page-title">GradInator</div>

          <div className="controls">
            {!isAuthPage && (
              <>
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
              </>
            )}

            {!initializing && user && (
              <div className="auth-links">
                <button
                  type="button"
                  className="user-badge"
                  onClick={() => router.push('/profile')}
                  title="Профиль"
                >
                  <span className="user-avatar">{initialsFromEmail(user.email)}</span>
                  <span className="user-badge-text">
                    <span className="user-badge-email">{user.email}</span>
                    <span className="user-badge-group">{user.group ?? 'без группы'}</span>
                  </span>
                </button>
                <button type="button" className="btn-logout" onClick={onLogout} title="Выйти" aria-label="Выйти">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                    <polyline points="16 17 21 12 16 7" />
                    <line x1="21" y1="12" x2="9" y2="12" />
                  </svg>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {!isAuthPage && (
        <div className="mast-row mast-row--bottom">
          <div className="group-row">
            <Combo label="Выбрать группу" options={groups} value={group} onChange={setGroup} homeGroup={homeGroup} />
            {!isOwnGroup && homeGroup && (
              <button type="button" className="own-group-pin" onClick={goToOwnGroup}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M12 19V5" />
                  <path d="M5 12l7-7 7 7" />
                </svg>
                Моя группа: {homeGroup}
              </button>
            )}
          </div>
          <DateNav dateIso={date} onPrev={prevDate} onNext={nextDate} onPick={setDate} onToday={goToday} />
        </div>
      )}
    </header>
  );
}
