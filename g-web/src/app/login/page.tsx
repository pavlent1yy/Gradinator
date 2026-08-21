'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);

    if (!email.trim() || !password) {
      setMessage('Укажите email и пароль');
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch('/api/core/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json().catch(() => null);
      if (!res.ok) {
        setMessage(data?.error ? data.error : `Ошибка: ${res.status}`);
        return;
      }
      setMessage('Вход выполнен. Перенаправление…');
      setTimeout(() => router.push('/'), 800);
    } catch (err: any) {
      setMessage(err?.message ?? String(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="login-title">
      <h1 id="login-title" className="auth-title">Вход</h1>
      <p className="auth-lead">Войдите, чтобы продолжить работу с расписанием.</p>

      <form className="auth-form" onSubmit={onSubmit}>
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

        <label className="field">
          <span className="field-label">Пароль</span>
          <input
            className="field-input"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        {message && <div className="warning-note" role="status">{message}</div>}

        <div className="auth-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Вход…' : 'Войти'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => router.push('/')}>
            Отмена
          </button>
        </div>

        <p className="auth-switch">
          Нет аккаунта? <Link href="/register">Зарегистрироваться</Link>
        </p>
      </form>
    </section>
  );
}
