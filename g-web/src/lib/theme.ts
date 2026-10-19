export const THEMES = [
  { id: 'light', label: 'Светлая' },
  { id: 'dark', label: 'Тёмная' },
  { id: 'classic', label: 'Classic' },
] as const;

export type Theme = (typeof THEMES)[number]['id'];

export const THEME_STORAGE_KEY = 'gradinator-theme';
export const DEFAULT_THEME: Theme = 'classic';

export const themeInitScript = `(function(){try{var t=localStorage.getItem('${THEME_STORAGE_KEY}');if(t==='light'||t==='dark'||t==='classic'){document.documentElement.setAttribute('data-theme',t);}}catch(e){}})();`;

export function readTheme(): Theme {
  const attr = document.documentElement.getAttribute('data-theme');
  return THEMES.some((t) => t.id === attr) ? (attr as Theme) : DEFAULT_THEME;
}

export function applyTheme(theme: Theme) {
  document.documentElement.setAttribute('data-theme', theme);
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {}
}
