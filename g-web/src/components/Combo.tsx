'use client';

import { useEffect, useId, useMemo, useRef, useState } from 'react';

const ALL_VALUE = '__all__';

type Props = {
  label?: string;
  tab?: string;
  options: string[];
  value?: string;
  onChange: (v: string) => void;
  homeGroup?: string | null;
  /** Добавить первым пунктом «все» (value === '') — для фильтра по отделениям */
  includeAll?: boolean;
  allLabel?: string;
  searchable?: boolean;
  className?: string;
};

function normalize(s: string) {
  return s.toLowerCase().replace(/[\s\-–—_.]/g, '');
}

export default function Combo({
  label = 'Выбрать',
  tab = 'Группа',
  options,
  value,
  onChange,
  homeGroup,
  includeAll = false,
  allLabel = 'Все',
  searchable = false,
  className
}: Props) {
  const uid = useId();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [focusedIndex, setFocusedIndex] = useState<number>(-1);
  const popRef = useRef<HTMLDivElement | null>(null);
  const listRef = useRef<HTMLUListElement | null>(null);
  const searchRef = useRef<HTMLInputElement | null>(null);
  const toggleRef = useRef<HTMLButtonElement | null>(null);

  const q = normalize(query);

  const items = useMemo(() => {
    const filtered = q ? options.filter(o => normalize(o).includes(q)) : options;
    return includeAll && !q ? [ALL_VALUE, ...filtered] : filtered;
  }, [includeAll, options, q]);

  const isAllSelected = includeAll && !value;

  const focusSyncKey = `${open}|${value}|${items.join('\n')}`;
  const [lastFocusSyncKey, setLastFocusSyncKey] = useState(focusSyncKey);
  if (lastFocusSyncKey !== focusSyncKey) {
    setLastFocusSyncKey(focusSyncKey);
    const selectedIndex = items.findIndex(o => o === value);
    setFocusedIndex(q && selectedIndex < 0 ? 0 : selectedIndex);
  }

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!popRef.current || !toggleRef.current) return;
      if (popRef.current.contains(e.target as Node)) return;
      if (toggleRef.current.contains(e.target as Node)) return;
      setOpen(false);
    }
    document.addEventListener('click', onDocClick);
    return () => document.removeEventListener('click', onDocClick);
  }, []);

  useEffect(() => {
    if (!open) return;
    if (searchable) searchRef.current?.focus();
    else listRef.current?.focus();
  }, [open, searchable]);

  function toggle() {
    if (!open) setQuery('');
    setOpen(v => !v);
  }

  function select(sel: string) {
    onChange(sel === ALL_VALUE ? '' : sel);
    setOpen(false);
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (!open) {
      if (e.key === 'ArrowDown') {
        setQuery('');
        setOpen(true);
        e.preventDefault();
      }
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setFocusedIndex(i => Math.min(items.length - 1, (i + 1) || 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setFocusedIndex(i => Math.max(0, (i - 1) || 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (focusedIndex >= 0 && items[focusedIndex]) {
        select(items[focusedIndex]);
      }
    } else if (e.key === 'Escape') {
      setOpen(false);
      toggleRef.current?.focus();
    }
  }

  useEffect(() => {
    if (focusedIndex >= 0 && listRef.current) {
      const el = listRef.current.children[focusedIndex] as HTMLElement | undefined;
      el?.scrollIntoView({ block: 'nearest' });
    }
  }, [focusedIndex]);

  const selectedLabel = useMemo(() => {
    if (isAllSelected) return allLabel;
    return value || '—';
  }, [isAllSelected, allLabel, value]);

  return (
    <div id={`combo-${uid}`} className={`combo combo--small${className ? ` ${className}` : ''}`} role="combobox" aria-haspopup="listbox" aria-expanded={open} aria-controls={`combo-list-${uid}`} aria-labelledby={`combo-label-${uid}`}>
      <div className="combo-field" id={`combo-label-${uid}`}>
        <span className="combo-tab">{tab}</span>
        <button
          type="button"
          ref={toggleRef}
          className="combo-toggle"
          aria-label={label}
          aria-expanded={open}
          onClick={toggle}
          onKeyDown={onKeyDown}
        >
          <span id="comboValue">{selectedLabel}</span>
          <svg className="chev" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>
      </div>

      <div
        className={`combo-list${searchable ? ' combo-list--search' : ''}${open ? ' open' : ''}`}
        aria-hidden={!open}
        ref={popRef}
        onKeyDown={onKeyDown}
      >
        {searchable && (
          <input
            ref={searchRef}
            type="search"
            className="combo-search"
            placeholder="Поиск…"
            aria-label={`Поиск: ${tab.toLowerCase()}`}
            value={query}
            onChange={e => setQuery(e.target.value)}
            tabIndex={open ? 0 : -1}
            autoComplete="off"
            enterKeyHint="go"
          />
        )}
        <ul id={`combo-list-${uid}`} className="combo-options" role="listbox" tabIndex={-1} ref={listRef}>
          {items.map((opt, idx) => {
            const isAll = opt === ALL_VALUE;
            return (
              <li
                key={isAll ? ALL_VALUE : opt}
                role="option"
                data-value={opt}
                aria-selected={isAll ? isAllSelected : opt === value}
                className={focusedIndex === idx ? 'focused' : undefined}
                onClick={() => select(opt)}
                onMouseEnter={() => setFocusedIndex(idx)}
              >
                <span>{isAll ? allLabel : opt}</span>
                {homeGroup && !isAll && opt === homeGroup && <span className="own-tag">моя</span>}
              </li>
            );
          })}
        </ul>
        {items.length === 0 && <p className="combo-empty">Ничего не найдено</p>}
      </div>
    </div>
  );
}
