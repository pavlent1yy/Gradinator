'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';

type VerificationState = 'checking' | 'success' | 'error';

export default function VerifyEmailPage() {
  const started = useRef(false);
  const [state, setState] = useState<VerificationState>('checking');
  const [message, setMessage] = useState('Проверяем ссылку подтверждения…');
  const [email, setEmail] = useState('');
  const [resending, setResending] = useState(false);
  const [resendMessage, setResendMessage] = useState<string | null>(null);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    const params = new URLSearchParams(window.location.hash.slice(1));
    const token = params.get('token');
    window.history.replaceState(null, '', '/verify-email');

    if (!token) {
      setState('error');
      setMessage('В ссылке отсутствует токен подтверждения.');
      return;
    }

    (async () => {
      try {
        const response = await fetch('/api/core/auth/verify-email', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token })
        });
        const data = await response.json().catch(() => null);

        if (!response.ok) {
          throw new Error(data?.error ?? 'Не удалось подтвердить почту');
        }

        setState('success');
        setMessage(data?.message ?? 'Почта подтверждена. Теперь можно войти.');
      } catch (error: unknown) {
        setState('error');
        setMessage(error instanceof Error ? error.message : 'Не удалось подтвердить почту');
      }
    })();
  }, []);

  async function resend(e: React.FormEvent) {
    e.preventDefault();
    setResending(true);
    setResendMessage(null);

    try {
      const response = await fetch('/api/core/auth/resend-verification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });
      const data = await response.json().catch(() => null);
      setResendMessage(data?.message ?? 'Если аккаунт ожидает подтверждения, письмо будет отправлено.');
    } catch {
      setResendMessage('Не удалось отправить письмо. Попробуйте позже.');
    } finally {
      setResending(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="verify-title">
      <h1 id="verify-title" className="auth-title">Подтверждение почты</h1>
      <p className="auth-lead">{message}</p>

      {state === 'checking' && <div className="loading-note">Пожалуйста, подождите…</div>}

      {state === 'success' && (
        <div className="auth-actions">
          <Link className="btn btn-primary" href="/login">Перейти ко входу</Link>
        </div>
      )}

      {state === 'error' && (
        <form className="auth-form" onSubmit={resend}>
          <label className="field">
            <span className="field-label">Email</span>
            <input
              className="field-input"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              autoComplete="email"
              required
            />
          </label>
          {resendMessage && <div className="warning-note" role="status">{resendMessage}</div>}
          <div className="auth-actions">
            <button className="btn btn-primary" type="submit" disabled={resending}>
              {resending ? 'Отправка…' : 'Отправить новую ссылку'}
            </button>
            <Link className="btn btn-ghost" href="/login">Вернуться ко входу</Link>
          </div>
        </form>
      )}
    </section>
  );
}
