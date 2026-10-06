import { joinList } from '../lib/schedule';

const ALERT_PATTERN = /^[\s❗❕‼️!]+/u;

export function stripAlert(text: string) {
  const cleaned = text.replace(ALERT_PATTERN, '').replace(/[❗❕‼️]/gu, '').trim();
  return { alert: cleaned !== text.trim(), text: cleaned };
}

export default function SubjectText({ subjects, fallback = 'Предмет' }: { subjects?: string[] | null; fallback?: string }) {
  const parts = (subjects ?? []).map(stripAlert);
  const alert = parts.some((p) => p.alert);
  const text = joinList(parts.map((p) => p.text), fallback);

  return (
    <>
      {alert && (
        <span className="alert-mark" title="Обрати внимание: изменение в расписании" aria-label="Внимание">!</span>
      )}
      {text}
    </>
  );
}
