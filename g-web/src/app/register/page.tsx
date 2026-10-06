'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import * as api from '../../lib/api';
import { useAuthContext } from '../providers/AuthProvider';

export default function RegisterPage() {
  const router = useRouter();
  const { login, clearAuthError } = useAuthContext();
  const [groups, setGroups] = useState<string[]>([]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [group, setGroup] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [serverMessage, setServerMessage] = useState<string | null>(null);
  const [clientError, setClientError] = useState<string | null>(null);

  // fetchGroups кэширован: повторный заход на страницу не порождает запрос
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const gs = await api.fetchGroups();
        if (!cancelled) setGroups(gs);
      } catch (e) {
        if (!cancelled) console.error(e);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  function validate() {
    if (!email.trim()) return 'Email обязателен';
    if (!password) return 'Пароль обязателен';
    if (password.length < api.MIN_PASSWORD_LENGTH) return `Пароль должен быть не короче ${api.MIN_PASSWORD_LENGTH} символов`;
    if (password !== confirmPassword) return 'Пароли не совпадают';
    if (!/^\S+@\S+\.\S+$/.test(email)) return 'Некорректный email';
    if (!agreed) return 'Нужно согласие с условиями';
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

    const payload: api.RegisterPayload = {
      email,
      password,
      confirmPassword,
      group: group || null
    };

    setSubmitting(true);
    try {
      await api.register(payload);

      if (await login(email, password)) {
        router.push('/');
        return;
      }

      clearAuthError();
      setServerMessage('Мы отправили письмо со ссылкой подтверждения. Проверь также папку «Спам».');
      setPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      setServerMessage(err instanceof Error ? err.message : String(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="register-title">
      <h1 id="register-title" className="auth-title">Регистрация</h1>
      <p className="auth-lead">Создай аккаунт, чтобы сохранять группу и настройки.</p>

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
            placeholder="не короче 8 символов"
            minLength={8}
            required
          />
        </label>

        <label className="field">
          <span className="field-label">Подтверди пароль</span>
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

        <label className="consent-check">
          <input type="checkbox" checked={agreed} onChange={e => setAgreed(e.target.checked)} required />
          <span>
            Принимаю <Link href="/terms">пользовательское соглашение</Link> и даю{' '}
            <Link href="/consent">согласие на обработку персональных данных</Link> на условиях{' '}
            <Link href="/privacy">политики конфиденциальности</Link>.
          </span>
        </label>

        {clientError && <div className="warning-note" role="alert">{clientError}</div>}
        {serverMessage && <div className="warning-note" role="status">{serverMessage}</div>}

        <div className="auth-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting || !agreed}>
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
