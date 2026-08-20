'use client';

import React, { useEffect, useRef, useState } from 'react';

const RU_MONTH_SHORT = ['янв.','фев.','мар.','апр.','май','июн.','июл.','авг.','сен.','окт.','ноя.','дек.'];

function formatDateShort(dateStr?: string) {
  if (!dateStr) return '';
  const d = new Date(dateStr + 'T00:00:00');
  if (isNaN(d.getTime())) return '';
  const dd = String(d.getDate()).padStart(2, '0');
  const m = RU_MONTH_SHORT[d.getMonth()];
  const yyyy = d.getFullYear();
  return `${dd} ${m} ${yyyy}`;
}

type Props = {
  dateIso: string;
  onPrev: () => void;
  onNext: () => void;
  onPick: (iso: string) => void;
};

export default function DateNav({ dateIso, onPrev, onNext, onPick }: Props) {
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState<string>(dateIso);
  const popRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setValue(dateIso);
  }, [dateIso]);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!open) return;
      if (!popRef.current) return;
      if (!popRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function onEsc(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('click', onDocClick);
    document.addEventListener('keydown', onEsc);
    return () => {
      document.removeEventListener('click', onDocClick);
      document.removeEventListener('keydown', onEsc);
    };
  }, [open]);

  function onOk() {
    // basic validation: value is YYYY-MM-DD
    if (!value) return;
    onPick(value);
    setOpen(false);
  }

  function onCancel() {
    setValue(dateIso);
    setOpen(false);
  }

  return (
    <div className="date-nav" role="group" aria-label="Переключение даты" style={{ position: 'relative' }}>
      {/* <button className="date-btn" aria-label="Вчера" onClick={onPrev}>‹</button> */}

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div className="date-label mono" aria-live="polite">{formatDateShort(dateIso)}</div>

        <button
          className="date-calendar-btn"
          aria-label="Открыть календарь"
          onClick={() => setOpen(v => !v)}
        >
          📅
        </button>
      </div>

      {/* <button className="date-btn" aria-label="Завтра" onClick={onNext}>›</button> */}

      {open && (
        <div ref={popRef} className="date-picker-popover" role="dialog" aria-label="Выбор даты">
          <input
            type="date"
            className="date-picker-input"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            aria-label="Выберите дату"
          />
          <div style={{ display: 'flex', gap: 8, marginTop: 8, justifyContent: 'flex-end' }}>
            <button className="date-picker-btn" onClick={onCancel} aria-label="Отмена">Отмена</button>
            <button className="date-picker-btn primary" onClick={onOk} aria-label="Подтвердить">ОК</button>
          </div>
        </div>
      )}
    </div>
  );
}