'use client';

import { useSyncExternalStore } from 'react';
import Link from 'next/link';

const KEY = 'gradinator.cookieNotice';
const listeners = new Set<() => void>();

function readDismissed() {
  try {
    return localStorage.getItem(KEY) === '1';
  } catch {
    return true;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export default function CookieNotice() {
  const dismissed = useSyncExternalStore(subscribe, readDismissed, () => true);

  if (dismissed) return null;

  function dismiss() {
    try {
      localStorage.setItem(KEY, '1');
    } catch {
      /* ignore */
    }
    listeners.forEach((l) => l());
  }

  return (
    <div className="cookie-notice" role="region" aria-label="Уведомление о cookie">
      <p>
        Сайт использует только технические cookie, необходимые для входа в аккаунт.{' '}
        <Link href="/cookies">Подробнее</Link>
      </p>
      <button type="button" className="btn btn-primary" onClick={dismiss}>Понятно</button>
    </div>
  );
}
