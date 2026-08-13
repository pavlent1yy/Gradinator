'use client';

import React from 'react';

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
};

export default function DateNav({ dateIso, onPrev, onNext }: Props) {
  return (
    <div className="date-nav" role="group" aria-label="Переключение даты">
      <button className="date-btn" aria-label="Вчера" onClick={onPrev}>
        ‹
      </button>
      <div className="date-label mono" aria-live="polite">{formatDateShort(dateIso)}</div>
      <button className="date-btn" aria-label="Завтра" onClick={onNext}>
        ›
      </button>
    </div>
  );
}