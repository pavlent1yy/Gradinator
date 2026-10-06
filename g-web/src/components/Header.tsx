'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useAuthContext } from '../app/providers/AuthProvider';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import { fetchCurrentWeekType } from '../lib/api';
import { formatDateLong, formatDayName, weekTypeLabel } from '../lib/date';

function initialsFromEmail(email: string) {
  const name = email.split('@')[0] || email;
  return name.slice(0, 2).toUpperCase();
}

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, initializing } = useAuthContext();
  const { date, schedule } = useScheduleContext();
  const [currentWeekType, setCurrentWeekType] = useState<string | null>(null);

  useEffect(() => {
    fetchCurrentWeekType().then(setCurrentWeekType);
  }, []);

  const weekLabel = weekTypeLabel(pathname === '/' ? (schedule?.weekType ?? currentWeekType) : currentWeekType);

  const navLink = (href: string, label: string) => (
    <Link href={href} className={`nav-link${pathname === href ? ' is-active' : ''}`}>
      {label}
    </Link>
  );

  return (
    <header className="masthead">
      <span className="clamp-rivet clamp-rivet--left" aria-hidden="true" />
      <span className="clamp-rivet clamp-rivet--right" aria-hidden="true" />
      <span className="clamp-spring" aria-hidden="true" />
      <Link href="/" className="logo" aria-label="GradInator, на главную">GradInator</Link>

      <div className="mast-date">
        <div className="date-stamp">
          <div className="day">{formatDayName(date, schedule?.day)}</div>
          <div className="date mono">{formatDateLong(date)}</div>
        </div>
        {weekLabel !== '—' && <div className="week-badge" title="Тип текущей недели">{weekLabel}</div>}
      </div>

      <nav className="main-nav" aria-label="Основная навигация">
        {navLink('/', 'Расписание')}
        {navLink('/week', 'Неделя')}
        {navLink('/search', 'Поиск')}
        {navLink('/rooms', 'Аудитории')}
        {navLink('/absences', 'Пропуски')}
      </nav>

      <div className="mast-user">
        {!initializing && !user && (
          <>
            {navLink('/login', 'Вход')}
            <Link href="/register" className="btn btn-primary btn-sm">Регистрация</Link>
          </>
        )}

        {!initializing && user && (
          <>
            <button
              type="button"
              className={`user-badge${pathname === '/absences' ? ' is-active' : ''}`}
              onClick={() => router.push('/absences#profile')}
              title="Профиль и пропуски"
            >
              <span className="user-avatar">{initialsFromEmail(user.email)}</span>
              <span className="user-badge-text">
                <span className="user-badge-email">{user.email}</span>
                <span className="user-badge-group">{user.group ?? 'без группы'}</span>
              </span>
            </button>
          </>
        )}
      </div>
    </header>
  );
}
