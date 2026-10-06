'use client';

import { useEffect, useState } from 'react';
import { useScheduleContext } from '../providers/ScheduleProvider';
import PageToolbar from '../../components/PageToolbar';
import * as api from '../../lib/api';
import type { FreeRooms } from '../../lib/api';
import { isDayOff } from '../../lib/date';

type Loaded = { date: string; rooms: FreeRooms[] | null; error: string | null };

export default function RoomsPage() {
  const { date } = useScheduleContext();
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [selectedPair, setSelectedPair] = useState<number | null>(null);
  const [filter, setFilter] = useState('');
  const dayOff = isDayOff(date);

  useEffect(() => {
    if (dayOff) return;

    const controller = new AbortController();

    api.fetchFreeRooms(date, controller.signal)
      .then((rooms) => setLoaded({ date, rooms, error: null }))
      .catch((e) => {
        if (e?.name !== 'AbortError') setLoaded({ date, rooms: null, error: e.message });
      });

    return () => controller.abort();
  }, [date, dayOff]);

  const current = loaded?.date === date ? loaded : null;
  const pairs = current?.rooms ?? [];
  const active = pairs.find((p) => p.pairNumber === selectedPair) ?? pairs[0] ?? null;
  const needle = filter.trim().toLowerCase();
  const matches = (room: string) => !needle || room.toLowerCase().includes(needle);

  return (
    <div className="page">
      <PageToolbar showGroupPicker={false}>
        <input
          className="field-input rooms-filter"
          type="search"
          placeholder="Корпус или номер, например «А2»"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          aria-label="Фильтр аудиторий"
        />
      </PageToolbar>

      {dayOff && <div className="status-card">Воскресенье, пар нет.</div>}
      {!dayOff && !current && <div className="status-card" role="status">Загрузка…</div>}
      {current?.error && <div className="status-card status-card--error" role="alert">Ошибка: {current.error}</div>}
      {current?.rooms && pairs.length === 0 && <div className="status-card">Нет данных на этот день.</div>}

      {active && (
        <>
          <div className="search-types" role="tablist" aria-label="Пара">
            {pairs.map((p) => (
              <button
                key={p.pairNumber}
                type="button"
                role="tab"
                aria-selected={p.pairNumber === active.pairNumber}
                className={`chip${p.pairNumber === active.pairNumber ? ' is-active' : ''}`}
                onClick={() => setSelectedPair(p.pairNumber)}
              >
                {p.pairNumber} пара
              </button>
            ))}
          </div>

          <h2 className="absence-subtitle">Свободны · {active.freeRooms.filter(matches).length}</h2>
          <div className="room-grid">
            {active.freeRooms.filter(matches).map((room) => (
              <span key={room} className="room-tile room-tile--free mono">{room}</span>
            ))}
          </div>

          <h2 className="absence-subtitle">Заняты · {active.busyRooms.filter(matches).length}</h2>
          <div className="room-grid">
            {active.busyRooms.filter(matches).map((room) => (
              <span key={room} className="room-tile mono">{room}</span>
            ))}
          </div>

          <p className="absence-hint">
            Свободными считаются аудитории, которые встречаются в расписании этого дня, но не заняты на выбранной паре.
          </p>
        </>
      )}
    </div>
  );
}
