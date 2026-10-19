'use client';

import { useSyncExternalStore } from 'react';
import { THEMES, Theme, applyTheme, readTheme } from '../lib/theme';

function subscribe(onChange: () => void) {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  return () => observer.disconnect();
}

const ICONS: Record<Theme, React.ReactNode> = {
  light: (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  ),
  dark: (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  ),
  classic: (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="5" y="4" width="14" height="17" rx="2" />
      <path d="M9 2h6v4H9zM8.5 11h7M8.5 15h5" />
    </svg>
  ),
};

export default function ThemeSwitcher() {
  const theme = useSyncExternalStore<Theme | null>(subscribe, readTheme, () => null);

  const select = (next: Theme) => applyTheme(next);

  return (
    <div className="theme-switch" role="group" aria-label="Тема оформления">
      {THEMES.map((t) => (
        <button
          key={t.id}
          type="button"
          className="theme-switch-btn"
          aria-pressed={theme === t.id}
          title={t.label}
          aria-label={t.label}
          onClick={() => select(t.id)}
        >
          {ICONS[t.id]}
        </button>
      ))}
    </div>
  );
}
