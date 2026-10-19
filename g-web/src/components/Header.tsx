'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { useAuthContext } from '../app/providers/AuthProvider';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import { fetchCurrentWeekType } from '../lib/api';
import ThemeSwitcher from './ThemeSwitcher';
import { formatDateLong, formatDayName, weekTypeLabel } from '../lib/date';

const SECTIONS = [
  { href: '/', label: 'Расписание' },
  { href: '/week', label: 'Неделя' },
  { href: '/search', label: 'Поиск' },
  { href: '/rooms', label: 'Аудитории' },
  { href: '/absences', label: 'Пропуски' },
];

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
  const [menuOpenAt, setMenuOpenAt] = useState<string | null>(null);
  const navRef = useRef<HTMLDivElement>(null);
  const menuOpen = menuOpenAt === pathname;
  const currentSection = SECTIONS.find((s) => s.href === pathname)?.label ?? 'Разделы';

  useEffect(() => {
    fetchCurrentWeekType().then(setCurrentWeekType);
  }, []);

  useEffect(() => {
    if (!menuOpen) return;
    const onPointer = (e: PointerEvent) => {
      if (!navRef.current?.contains(e.target as Node)) setMenuOpenAt(null);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpenAt(null);
    };
    document.addEventListener('pointerdown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [menuOpen]);

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

      <div className={`nav-wrap${menuOpen ? ' is-open' : ''}`} ref={navRef}>
        <button
          type="button"
          className="nav-burger"
          aria-expanded={menuOpen}
          aria-controls="main-nav"
          onClick={() => setMenuOpenAt(menuOpen ? null : pathname)}
        >
          <span>{currentSection}</span>
          <svg className="chev" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="m9 6 6 6-6 6" />
          </svg>
        </button>
        <nav className="main-nav" id="main-nav" aria-label="Основная навигация">
          {SECTIONS.map((s) => (
            <Link key={s.href} href={s.href} className={`nav-link${pathname === s.href ? ' is-active' : ''}`} onClick={() => setMenuOpenAt(null)}>
              {s.label}
            </Link>
          ))}
        </nav>
      </div>

      <div className="mast-user">
        <ThemeSwitcher />
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
