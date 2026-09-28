'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthContext } from '../providers/AuthProvider';

export default function LoginPage() {
  const router = useRouter();
  const { login, authLoading, authError, clearAuthError } = useAuthContext();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [clientMessage, setClientMessage] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setClientMessage(null);
    clearAuthError();

    if (!email.trim() || !password) {
      setClientMessage('Укажите email и пароль');
      return;
    }

    const ok = await login(email, password);
    if (ok) {
      setClientMessage('Вход выполнен. Перенаправление…');
      setTimeout(() => router.push('/'), 700);
    }
  }

  const message = clientMessage ?? authError;

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
          <button type="submit" className="btn btn-primary" disabled={authLoading}>
            {authLoading ? 'Вход…' : 'Войти'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => router.push('/')}>
            Отмена
          </button>
        </div>

        <div className="auth-actions">
          {/*
            Полный редирект (не Link): запускает OAuth2-рукопожатие в g-core.
            После успеха g-core сам перенаправит на /profile.
            Прокси /oauth2/* настроен в next.config.ts.
          */}
          <a href="/oauth2/authorization/google" className="btn btn-ghost">
            Войти через Google
          </a>
        </div>

        <p className="auth-switch">
          Нет аккаунта? <Link href="/register">Зарегистрироваться</Link>
        </p>
      </form>
    </section>
  );
}
