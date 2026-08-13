'use client';

import { useEffect, useMemo, useRef, useState } from 'react';

type Props = {
  label?: string;
  options: string[];
  value?: string;
  onChange: (v: string) => void;
};

const STORAGE_KEY = 'gradinator.selectedGroup';

export default function Combo({ label = 'Выбрать', options, value, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState<number>(-1);
  const listRef = useRef<HTMLUListElement | null>(null);
  const toggleRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    if (!open) {
      setFocusedIndex(options.findIndex(o => o === value));
    } else {
      setFocusedIndex(options.findIndex(o => o === value) ?? 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, options, value]);

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
      setFocusedIndex(i => Math.min(options.length - 1, (i + 1) || 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setFocusedIndex(i => Math.max(0, (i - 1) || 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (focusedIndex >= 0 && options[focusedIndex]) {
        const sel = options[focusedIndex];
        onChange(sel);
        try { localStorage.setItem(STORAGE_KEY, sel); } catch {}
        setOpen(false);
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

  const selectedLabel = useMemo(() => value || '—', [value]);

  return (
    <div id="combo" className="combo combo--small" role="combobox" aria-haspopup="listbox" aria-expanded={open} aria-controls="combo-list" aria-labelledby="combo-label">
      <div className="combo-field" id="combo-label">
        <button
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
        id="combo-list"
        className={`combo-list ${open ? 'open' : ''}`}
        role="listbox"
        tabIndex={-1}
        aria-hidden={!open}
        ref={listRef}
        onKeyDown={onKeyDown}
      >
        {options.map((opt, idx) => (
          <li
            key={opt}
            role="option"
            data-value={opt}
            aria-selected={opt === value}
            className={focusedIndex === idx ? 'focused' : undefined}
            onClick={() => { onChange(opt); try { localStorage.setItem(STORAGE_KEY, opt); } catch {} setOpen(false); }}
            onMouseEnter={() => setFocusedIndex(idx)}
          >
            {opt}
          </li>
        ))}
      </ul>
    </div>
  );
}