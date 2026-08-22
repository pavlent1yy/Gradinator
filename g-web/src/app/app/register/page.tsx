'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import * as api from '../../lib/api';

type RegisterPayload = {
  email: string;
  password: string;
  confirmPassword: string;
  group?: string | null;
};

export default function RegisterPage() {
  const router = useRouter();
  const [groups, setGroups] = useState<string[]>([]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [group, setGroup] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [serverMessage, setServerMessage] = useState<string | null>(null);
  const [clientError, setClientError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setGroups(await api.fetchGroups());
      } catch (e) {
        console.error(e);
      }
    })();
  }, []);

  function validate() {
    if (!email.trim()) return 'Email обязателен';
    if (!password) return 'Пароль обязателен';
    if (password !== confirmPassword) return 'Пароли не совпадают';
    if (!/^\S+@\S+\.\S+$/.test(email)) return 'Некорректный email';
    return null;
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setClientError(null);
    setServerMessage(null);

    const clientValidation = validate();
    if (clientValidation) {
      setClientError(clientValidation);
      return;
    }

    const payload: RegisterPayload = {
      email,
      password,
      confirmPassword,
      group: group || null
    };

    setSubmitting(true);
    try {
      const res = await fetch('/api/core/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json().catch(() => null);
      if (!res.ok) {
        setServerMessage(data?.error ? data.error : `Ошибка: ${res.status}`);
      } else {
        setServerMessage('Регистрация прошла успешно. Перенаправление на страницу входа…');
        setTimeout(() => router.push('/login'), 1200);
      }
    } catch (err: any) {
      setServerMessage(err?.message ?? String(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="register-title">
      <h1 id="register-title" className="auth-title">Регистрация</h1>
      <p className="auth-lead">Создайте аккаунт, чтобы сохранять группу и настройки.</p>

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
            autoComplete="new-password"
            required
          />
        </label>

        <label className="field">
          <span className="field-label">Подтвердите пароль</span>
          <input
            className="field-input"
            type="password"
            value={confirmPassword}
            onChange={e => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
            required
          />
        </label>

        <label className="field">
          <span className="field-label">Группа (необязательно)</span>
          <select
            className="field-input field-select"
            value={group}
            onChange={e => setGroup(e.target.value || '')}
          >
            <option value="">— без группы —</option>
            {groups.map(g => (
              <option key={g} value={g}>{g}</option>
            ))}
          </select>
        </label>

        {clientError && <div className="warning-note" role="alert">{clientError}</div>}
        {serverMessage && <div className="warning-note" role="status">{serverMessage}</div>}

        <div className="auth-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Отправка…' : 'Зарегистрироваться'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => router.push('/')}>
            Отмена
          </button>
        </div>

        <p className="auth-switch">
          Уже есть аккаунт? <Link href="/login">Войти</Link>
        </p>
      </form>
    </section>
  );
}
