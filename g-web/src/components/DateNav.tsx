'use client';

import { useEffect, useRef, useState } from 'react';
import { formatDateShort, toIsoDate } from '../lib/date';
import MiniCalendar from './MiniCalendar';

type Props = {
  dateIso: string;
  onPrev: () => void;
  onNext: () => void;
  onPick: (iso: string) => void;
  onToday: () => void;
};

export default function DateNav({ dateIso, onPrev, onNext, onPick, onToday }: Props) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;

    function onDocClick(e: MouseEvent) {
      if (!rootRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function onEsc(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }

    document.addEventListener('mousedown', onDocClick);
    document.addEventListener('keydown', onEsc);
    return () => {
      document.removeEventListener('mousedown', onDocClick);
      document.removeEventListener('keydown', onEsc);
    };
  }, [open]);

  function pick(iso: string) {
    onPick(iso);
    setOpen(false);
  }

  const isToday = dateIso === toIsoDate(new Date());

  return (
    <div className="date-nav" role="group" aria-label="Переключение даты" ref={rootRef}>
      <button type="button" className="date-btn" aria-label="Предыдущий день" onClick={onPrev}>
        ‹
      </button>

      <div className="date-current">
        <div className="date-label mono" aria-live="polite">{formatDateShort(dateIso)}</div>

        <button
          type="button"
          className="date-calendar-btn"
          aria-label="Открыть календарь"
          aria-expanded={open}
          onClick={() => setOpen(v => !v)}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
        </button>
      </div>

      <button type="button" className="date-btn" aria-label="Следующий день" onClick={onNext}>
        ›
      </button>

      <button
        type="button"
        className={`today-flag${isToday ? ' is-hidden' : ''}`}
        onClick={onToday}
        aria-label="Вернуться к сегодняшней дате"
        aria-hidden={isToday}
        tabIndex={isToday ? -1 : 0}
      >
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M3 12a9 9 0 1 0 9-9" />
          <path d="M3 4v5h5" />
        </svg>
        <span>Сегодня</span>
      </button>

      {open && (
        <div className="date-picker-popover" role="dialog" aria-label="Выбор даты">
          <MiniCalendar value={dateIso} onPick={pick} />
        </div>
      )}
    </div>
  );
}
