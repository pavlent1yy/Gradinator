'use client';

import { useState } from 'react';
import { parseIsoDate, toIsoDate } from '../lib/date';

const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];
const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

type Props = {
  value: string;
  onPick: (iso: string) => void;
};

function monthCells(year: number, month: number) {
  const first = new Date(year, month, 1);
  const lead = (first.getDay() + 6) % 7;
  const cells: (string | null)[] = [];

  for (let i = 0; i < Math.min(lead, 6); i++) cells.push(null);

  const days = new Date(year, month + 1, 0).getDate();
  for (let day = 1; day <= days; day++) {
    const d = new Date(year, month, day);
    if (d.getDay() !== 0) cells.push(toIsoDate(d));
  }

  return cells;
}

export default function MiniCalendar({ value, onPick }: Props) {
  const selected = parseIsoDate(value) ?? new Date();
  const [view, setView] = useState({ year: selected.getFullYear(), month: selected.getMonth() });
  const today = toIsoDate(new Date());

  function shift(delta: number) {
    setView(({ year, month }) => {
      const d = new Date(year, month + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() };
    });
  }

  return (
    <div className="calendar">
      <div className="calendar-head">
        <button type="button" className="date-btn" aria-label="Предыдущий месяц" onClick={() => shift(-1)}>‹</button>
        <span className="calendar-title">{MONTHS[view.month]} {view.year}</span>
        <button type="button" className="date-btn" aria-label="Следующий месяц" onClick={() => shift(1)}>›</button>
      </div>

      <div className="calendar-grid" role="grid">
        {WEEKDAYS.map((w) => (
          <span key={w} className={`calendar-weekday${w === 'Сб' ? ' is-weekend' : ''}`}>{w}</span>
        ))}
        {monthCells(view.year, view.month).map((iso, i) => iso ? (
          <button
            key={iso}
            type="button"
            className={`calendar-day${iso === value ? ' is-selected' : ''}${iso === today ? ' is-today' : ''}`}
            aria-pressed={iso === value}
            aria-label={iso}
            onClick={() => onPick(iso)}
          >
            {Number(iso.slice(8))}
          </button>
        ) : (
          <span key={`empty-${i}`} />
        ))}
      </div>
    </div>
  );
}
