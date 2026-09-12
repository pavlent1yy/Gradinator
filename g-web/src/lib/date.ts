const RU_DAYS = ['Воскресенье', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
const RU_MONTHS = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
const RU_MONTH_SHORT = ['янв.', 'фев.', 'мар.', 'апр.', 'май', 'июн.', 'июл.', 'авг.', 'сен.', 'окт.', 'ноя.', 'дек.'];

/** Local calendar date → YYYY-MM-DD (no UTC shift). */
export function toIsoDate(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function parseIsoDate(iso?: string): Date | null {
  if (!iso) return null;
  const d = new Date(`${iso}T00:00:00`);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function formatDayName(iso?: string, fallback?: string) {
  const d = parseIsoDate(iso);
  if (d) return RU_DAYS[d.getDay()];
  return fallback || '—';
}

export function formatDateLong(iso?: string) {
  const d = parseIsoDate(iso);
  if (!d) return '—';
  return `${d.getDate()} ${RU_MONTHS[d.getMonth()]} ${d.getFullYear()}`;
}

export function formatDateShort(iso?: string) {
  const d = parseIsoDate(iso);
  if (!d) return '';
  const dd = String(d.getDate()).padStart(2, '0');
  return `${dd} ${RU_MONTH_SHORT[d.getMonth()]} ${d.getFullYear()}`;
}

export function weekTypeLabel(weekType?: string | null) {
  if (weekType === 'DENOMINATOR') return 'Знаменатель';
  if (weekType === 'NUMERATOR') return 'Числитель';
  return '—';
}
