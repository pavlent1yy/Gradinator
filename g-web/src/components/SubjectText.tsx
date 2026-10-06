import { joinList } from '../lib/schedule';

const ALERT_PATTERN = /^[\s❗❕‼️!]+/u;

export function stripAlert(text: string) {
  const cleaned = text.replace(ALERT_PATTERN, '').replace(/[❗❕‼️]/gu, '').trim();
  return { alert: cleaned !== text.trim(), text: cleaned };
}

export function isCancelled(subjects?: string[] | null) {
  const list = subjects ?? [];
  return list.length > 0 && list.every((s) => /^снят[оа]$/i.test(stripAlert(s).text));
}

type Props = { subjects?: string[] | null; fallback?: string; hasChanges?: boolean };

export default function SubjectText({ subjects, fallback = 'Предмет', hasChanges = false }: Props) {
  if (isCancelled(subjects)) {
    return <span className="cancelled-stamp">пара снята</span>;
  }

  const parts = (subjects ?? []).map(stripAlert);
  const alert = parts.some((p) => p.alert);
  const text = joinList(parts.map((p) => p.text), fallback);

  return (
    <>
      {text}
      {(hasChanges || alert) && <span className="changed-stamp">замена</span>}
    </>
  );
}
