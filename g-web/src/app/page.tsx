'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import Combo from '../components/Combo';
import type { Schedule, Pair } from '../types/schedule';

const TIMES_MAP: Record<number, string> = {
  1: '08:00 — 09:30',
  2: '09:45 — 11:15',
  3: '11:30 — 13:00',
  4: '13:20 — 14:50',
  5: '15:00 — 16:30',
  6: '16:45 — 18:15',
  7: '18:30 — 20:00'
};

const RU_MONTH_SHORT = ['янв.','фев.','мар.','апр.','май','июн.','июл.','авг.','сен.','окт.','ноя.','дек.'];

function formatDateShort(dateStr?: string) {
  const d = dateStr ? new Date(dateStr) : new Date();
  if (isNaN(d.getTime())) return '';
  const dd = String(d.getDate()).padStart(2, '0');
  const m = RU_MONTH_SHORT[d.getMonth()];
  const yyyy = d.getFullYear();
  return `${dd} ${m} ${yyyy}`;
}

function toIsoDate(d = new Date()) {
  return d.toISOString().slice(0, 10);
}

export default function Page() {
  const [groups, setGroups] = useState<string[]>([]);
  const [group, setGroup] = useState<string>('');
  const [schedule, setSchedule] = useState<Schedule | null>(null);
  const [date, setDate] = useState<string>(toIsoDate());
  const [loading, setLoading] = useState(false);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);

  const inited = useRef(false);

  // load groups on first mount
  useEffect(() => {
    if (inited.current) return;
    inited.current = true;
    fetchGroups();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (group) fetchSchedule(group, date);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [group, date]);

  async function fetchGroups() {
    setError(null);
    try {
      const res = await fetch('/api/core/schedule/groups');
      if (!res.ok) throw new Error(`Ошибка загрузки групп: ${res.status}`);
      const data = await res.json();
      if (!Array.isArray(data)) throw new Error('Неверный формат групп');
      setGroups(data);
      if (data.length) setGroup(prev => prev || data[0]);
    } catch (e: any) {
      console.error(e);
      setError(e?.message || String(e));
    }
  }

  async function fetchSchedule(g: string, d: string) {
    setLoading(true);
    setError(null);
    try {
      const url = `/api/core/schedule?group=${encodeURIComponent(g)}&date=${encodeURIComponent(d)}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`Ошибка загрузки расписания: ${res.status}`);
      const data = await res.json();
      setSchedule(data);
      setUpdatedAt(new Date());
    } catch (e: any) {
      console.error(e);
      setError(e?.message || String(e));
      setSchedule(null);
    } finally {
      setLoading(false);
    }
  }

  function onRefresh() {
    if (!group) return;
    fetchSchedule(group, date);
  }

  const anyNumeratorPairs = useMemo(() => {
    return schedule?.pairs?.some(p => p.numerator && !p.numerator.empty);
  }, [schedule]);

  return (
    <main className="app" id="app">
      <header className="masthead" aria-labelledby="page-title">
        <div className="mast-left">
          <div className="meta-row">
            <div>
              <div className="day" id="day-label">{schedule?.day || '—'}</div>
              <div className="date" id="date-label">{schedule?.date ? formatDateShort(schedule.date) : formatDateShort(date)}</div>
            </div>
            <div className="week-badge" id="week-badge" aria-hidden="true">
              {schedule?.weekType === 'NUMERATOR' ? 'Числитель' : 'Числитель'}
            </div>
          </div>

          <div className="group-row" aria-hidden="false">
            <Combo
              label="Выбрать группу"
              options={groups}
              value={group}
              onChange={g => { setGroup(g); }}
            />
          </div>
        </div>

        <div className="mast-right">
          <div className="logo" aria-hidden="true">GradInator</div>
          <div className="controls">
            <button
              id="refreshBtn"
              className={`icon-btn ${loading ? 'btn-spin' : ''}`}
              aria-label="Обновить расписание"
              title="Обновить"
              onClick={onRefresh}
              aria-busy={loading}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M21 12a9 9 0 1 0-3.5 6.9"></path>
                <polyline points="21 3 21 9 15 9"></polyline>
              </svg>
            </button>

            <div id="updatedAt" className="updated-note" aria-live="polite">
              {updatedAt ? `Информация от ${updatedAt.toLocaleString()}` : 'Информация от —'}
            </div>
          </div>
        </div>
      </header>

      <section className="schedule" id="schedule" aria-label="Расписание">
        {error && <div className="pair" style={{ padding: 12 }}>Ошибка: {error}</div>}
        {!schedule && !error && <div className="pair" style={{ padding: 12 }}>Загрузка...</div>}

        {schedule && schedule.pairs?.slice().sort((a,b)=> (a.pairNumber||0)-(b.pairNumber||0)).map((pair: Pair) => {
          const numerator = pair.numerator;
          if (!numerator || numerator.empty) return null;
          return (
            <div key={pair.pairNumber} className="pair">
              <div className="pair-num">{pair.pairNumber ?? '—'}</div>
              <div className="pair-body">
                <div className="pair-time mono">{TIMES_MAP[pair.pairNumber as number] || '—'}</div>

                <div className="entry">
                  <h3 className="subject">{(numerator.subjects && numerator.subjects[0]) || 'Предмет'}</h3>
                  <div className="meta-row-lesson">
                    <span className="room mono">{(numerator.rooms && numerator.rooms[0]) || '—'}</span>
                    <span className="teachers">{(numerator.teachers || []).join(', ')}</span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}

        {schedule && !anyNumeratorPairs && (
          <div className="pair" style={{ gridTemplateColumns: '1fr', padding: 12 }}>На этой неделе занятий по числителю нет.</div>
        )}
      </section>

      <footer className="page-foot">
        <div className="foot-left mono">Источник: учебный план — 2026</div>
        <div className="foot-right">GradInator · analog editorial</div>
      </footer>
    </main>
  );
}