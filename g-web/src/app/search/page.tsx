'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useScheduleContext } from '../providers/ScheduleProvider';
import PageToolbar from '../../components/PageToolbar';
import * as api from '../../lib/api';
import type { DictionaryKind, SearchHit, SearchType } from '../../lib/api';
import { joinList } from '../../lib/schedule';
import SubjectText from '../../components/SubjectText';

const TYPES: { value: SearchType; label: string; dictionary: DictionaryKind | null }[] = [
  { value: 'ANY', label: 'Везде', dictionary: null },
  { value: 'TEACHER', label: 'Преподаватель', dictionary: 'teachers' },
  { value: 'SUBJECT', label: 'Предмет', dictionary: 'subjects' },
  { value: 'ROOM', label: 'Аудитория', dictionary: 'rooms' }
];

const MIN_QUERY = 2;
const DEBOUNCE_MS = 300;

type Loaded = { key: string; hits: SearchHit[] | null; error: string | null };

export default function SearchPage() {
  const router = useRouter();
  const { date, setGroup } = useScheduleContext();
  const [query, setQuery] = useState('');
  const [type, setType] = useState<SearchType>('ANY');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [debounced, setDebounced] = useState('');
  const [loaded, setLoaded] = useState<Loaded | null>(null);

  const dictionary = TYPES.find((t) => t.value === type)?.dictionary ?? null;

  useEffect(() => {
    if (!dictionary) return;
    api.fetchDictionary(dictionary).then(setSuggestions).catch(() => setSuggestions([]));
  }, [dictionary]);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(query.trim()), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query]);

  const key = debounced.length >= MIN_QUERY ? `${type}|${date}|${debounced}` : null;

  useEffect(() => {
    if (!key) return;

    const controller = new AbortController();

    api.searchSchedule(debounced, type, date, controller.signal)
      .then((hits) => setLoaded({ key, hits, error: null }))
      .catch((e) => {
        if (e?.name !== 'AbortError') setLoaded({ key, hits: null, error: e.message });
      });

    return () => controller.abort();
  }, [key, debounced, type, date]);

  const current = key && loaded?.key === key ? loaded : null;

  const byPair = useMemo(() => {
    const map = new Map<number, SearchHit[]>();
    current?.hits?.forEach((hit) => map.set(hit.pairNumber, [...(map.get(hit.pairNumber) ?? []), hit]));
    return [...map.entries()];
  }, [current]);

  return (
    <div className="page">
      <PageToolbar showGroupPicker={false}>
        <div className="search-controls">
          <div className="search-types" role="radiogroup" aria-label="Где искать">
            {TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                role="radio"
                aria-checked={type === t.value}
                className={`chip${type === t.value ? ' is-active' : ''}`}
                onClick={() => setType(t.value)}
              >
                {t.label}
              </button>
            ))}
          </div>
          <input
            className="field-input search-input"
            type="search"
            placeholder="Фамилия преподавателя, предмет или аудитория"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            list={dictionary ? 'search-suggestions' : undefined}
            aria-label="Поисковый запрос"
            autoFocus
          />
          {dictionary && (
            <datalist id="search-suggestions">
              {suggestions.map((s) => <option key={s} value={s} />)}
            </datalist>
          )}
        </div>
      </PageToolbar>

      {!key && (
        <div className="status-card">
          Введи хотя бы {MIN_QUERY} символа: поиск идёт по расписанию всех групп на выбранный день.
        </div>
      )}
      {key && !current && <div className="status-card" role="status">Ищем…</div>}
      {current?.error && <div className="status-card status-card--error" role="alert">Ошибка: {current.error}</div>}
      {current?.hits && current.hits.length === 0 && (
        <div className="status-card">Ничего не найдено. Попробуй другую дату или часть слова.</div>
      )}

      <div className="schedule">
        {byPair.map(([pairNumber, hits]) => (
          <article key={pairNumber} className="pair">
            <div className="pair-num">{pairNumber}</div>
            <div className="pair-body search-hits">
              {hits.map((hit) => (
                <div key={`${hit.group}-${pairNumber}`} className="search-hit">
                  <button
                    type="button"
                    className="search-hit-group mono"
                    onClick={() => {
                      setGroup(hit.group);
                      router.push('/');
                    }}
                    title="Открыть расписание этой группы"
                  >
                    {hit.group}
                  </button>
                  <div className="entry">
                    <h3 className="subject">
                      <SubjectText subjects={hit.subjects} hasChanges={hit.hasChanges} />
                    </h3>
                    <div className="meta-row-lesson">
                      <span className="room mono">{joinList(hit.rooms)}</span>
                      <span className="teachers">{joinList(hit.teachers)}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
