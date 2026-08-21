'use client';

import { useEffect, useRef, useState } from 'react';
import { formatDateShort } from '../lib/date';

type Props = {
  dateIso: string;
  onPrev: () => void;
  onNext: () => void;
  onPick: (iso: string) => void;
};

export default function DateNav({ dateIso, onPrev, onNext, onPick }: Props) {
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState(dateIso);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setValue(dateIso);
  }, [dateIso]);

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

  function onOk() {
    if (!value) return;
    onPick(value);
    setOpen(false);
  }

  function onCancel() {
    setValue(dateIso);
    setOpen(false);
  }

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

      {open && (
        <div className="date-picker-popover" role="dialog" aria-label="Выбор даты">
          <input
            type="date"
            className="date-picker-input"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            aria-label="Выберите дату"
          />
          <div className="date-picker-actions">
            <button type="button" className="date-picker-btn" onClick={onCancel}>Отмена</button>
            <button type="button" className="date-picker-btn primary" onClick={onOk}>ОК</button>
          </div>
        </div>
      )}
    </div>
  );
}
