'use client';

import { Suspense, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import * as api from '../../lib/api';

export default function ForgotPasswordPage() {
  return (
    <Suspense>
      <ForgotPasswordForm />
    </Suspense>
  );
}

function ForgotPasswordForm() {
  const searchParams = useSearchParams();
  const [email, setEmail] = useState(searchParams.get('email') ?? '');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setError('Укажи корректный email');
      return;
    }

    setSending(true);
    try {
      const message = await api.requestPasswordReset(email.trim());
      setSent(message ?? 'Если аккаунт с такой почтой существует, мы отправили на неё ссылку для сброса пароля.');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Не удалось отправить письмо');
    } finally {
      setSending(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="forgot-title">
      <h1 id="forgot-title" className="auth-title">Восстановление пароля</h1>
      <p className="auth-lead">Укажи почту аккаунта — пришлём ссылку, по которой можно задать новый пароль.</p>

      {sent ? (
        <>
          <div className="warning-note" role="status">
            {sent} Проверь также папку «Спам». Ссылка действует час.
          </div>
          <div className="auth-actions">
            <Link className="btn btn-primary" href="/login">Вернуться ко входу</Link>
            <button type="button" className="btn btn-ghost" onClick={() => setSent(null)}>
              Отправить ещё раз
            </button>
          </div>
        </>
      ) : (
        <form className="auth-form" onSubmit={onSubmit}>
          <label className="field">
            <span className="field-label">Email</span>
            <input
              className="field-input"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              autoComplete="email"
              autoFocus
              required
            />
          </label>

          {error && <div className="warning-note" role="alert">{error}</div>}

          <div className="auth-actions">
            <button type="submit" className="btn btn-primary" disabled={sending}>
              {sending ? 'Отправка…' : 'Отправить ссылку'}
            </button>
            <Link className="btn btn-ghost" href="/login">Отмена</Link>
          </div>
        </form>
      )}
    </section>
  );
}
