'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
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
  const [group, setGroup] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [serverMessage, setServerMessage] = useState<string | null>(null);
  const [clientError, setClientError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const g = await api.fetchGroups();
        setGroups(g);
      } catch (e: any) {
        console.error(e);
      }
    })();
  }, []);

  function validate() {
    if (!email.trim()) return 'Email обязателен';
    if (!password) return 'Пароль обязателен';
    if (password !== confirmPassword) return 'Пароли не совпадают';
    // optionally validate email format here (simple)
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
        // server error response
        const msg = data && data.error ? data.error : `Ошибка: ${res.status}`;
        setServerMessage(msg);
      } else {
        // success — redirect to login or show message
        setServerMessage('Регистрация прошла успешно. Перенаправление на страницу входа...');
        setTimeout(() => router.push('/login'), 1200);
      }
    } catch (err: any) {
      setServerMessage(err?.message ?? String(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{ padding: 12, maxWidth: 720, margin: '0 auto' }}>
      <h1>Регистрация</h1>

      <form onSubmit={onSubmit} style={{ display: 'grid', gap: 10 }}>
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>

        <label>
          Пароль
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
        </label>

        <label>
          Подтвердите пароль
          <input type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required />
        </label>

        <label>
          Группа (необязательно)
          <select value={group} onChange={e => setGroup(e.target.value || '')}>
            <option value="">— без группы —</option>
            {groups.map(g => <option key={g} value={g}>{g}</option>)}
          </select>
        </label>

        {clientError && <div className="warning-note">{clientError}</div>}
        {serverMessage && <div className="warning-note">{serverMessage}</div>}

        <div style={{ display: 'flex', gap: 8 }}>
          <button type="submit" disabled={submitting} className="combo-toggle">
            {submitting ? 'Отправка...' : 'Зарегистрироваться'}
          </button>
          <button type="button" className="date-picker-btn" onClick={() => router.push('/')}>Отмена</button>
        </div>
      </form>
    </main>
  );
}