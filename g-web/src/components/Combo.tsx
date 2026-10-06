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
};

export default function Combo({
  label = 'Выбрать',
  tab = 'Группа',
  options,
  value,
  onChange,
  homeGroup,
  includeAll = false,
  allLabel = 'Все'
}: Props) {
  const uid = useId();
  const [open, setOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState<number>(-1);
  const listRef = useRef<HTMLUListElement | null>(null);
  const toggleRef = useRef<HTMLButtonElement | null>(null);

  // Эффективный список пунктов: при includeAll первым идёт псевдо-опция «все»
  const items = useMemo(
    () => (includeAll ? [ALL_VALUE, ...options] : options),
    [includeAll, options]
  );

  const isAllSelected = includeAll && !value;

  useEffect(() => {
    if (!open) {
      setFocusedIndex(items.findIndex(o => o === value));
    } else {
      setFocusedIndex(items.findIndex(o => o === value) ?? 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, items, value]);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!listRef.current || !toggleRef.current) return;
      if (listRef.current.contains(e.target as Node)) return;
      if (toggleRef.current.contains(e.target as Node)) return;
      setOpen(false);
    }
    document.addEventListener('click', onDocClick);
    return () => document.removeEventListener('click', onDocClick);
  }, []);

  useEffect(() => {
    if (open && listRef.current) {
      listRef.current.focus();
    }
  }, [open]);

  function select(sel: string) {
    onChange(sel === ALL_VALUE ? '' : sel);
    setOpen(false);
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (!open) {
      if (e.key === 'ArrowDown') {
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
    <div id={`combo-${uid}`} className="combo combo--small" role="combobox" aria-haspopup="listbox" aria-expanded={open} aria-controls={`combo-list-${uid}`} aria-labelledby={`combo-label-${uid}`}>
      <div className="combo-field" id={`combo-label-${uid}`}>
        <span className="combo-tab">{tab}</span>
        <button
          type="button"
          ref={toggleRef}
          className="combo-toggle"
          aria-label={label}
          aria-expanded={open}
          onClick={() => setOpen(v => !v)}
        >
          <span id="comboValue">{selectedLabel}</span>
          <svg className="chev" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>
      </div>

      <ul
        id={`combo-list-${uid}`}
        className={`combo-list ${open ? 'open' : ''}`}
        role="listbox"
        tabIndex={-1}
        aria-hidden={!open}
        ref={listRef}
        onKeyDown={onKeyDown}
      >
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
    </div>
  );
}
