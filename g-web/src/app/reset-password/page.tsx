'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import * as api from '../../lib/api';

type ResetState = 'checking' | 'form' | 'missing' | 'done';

export default function ResetPasswordPage() {
  const tokenRef = useRef<string | null>(null);
  const [state, setState] = useState<ResetState>('checking');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (tokenRef.current !== null) return;

    const token = new URLSearchParams(window.location.hash.slice(1)).get('token');
    tokenRef.current = token ?? '';
    window.history.replaceState(null, '', '/reset-password');

    queueMicrotask(() => setState(token ? 'form' : 'missing'));
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!newPassword || !confirmPassword) {
      setError('Заполни оба поля');
      return;
    }
    if (newPassword.length < api.MIN_PASSWORD_LENGTH) {
      setError(`Пароль должен быть не короче ${api.MIN_PASSWORD_LENGTH} символов`);
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Пароли не совпадают');
      return;
    }

    setSaving(true);
    try {
      const result = await api.resetPassword(tokenRef.current ?? '', newPassword, confirmPassword);
      setMessage(result ?? 'Пароль изменён. Теперь можно войти с новым паролем.');
      setState('done');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Не удалось сменить пароль');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="reset-title">
      <h1 id="reset-title" className="auth-title">Новый пароль</h1>

      {state === 'checking' && <div className="status-card" role="status">Загрузка…</div>}

      {state === 'missing' && (
        <>
          <div className="warning-note" role="alert">
            В ссылке нет токена сброса. Открой ссылку из письма целиком или запроси новую.
          </div>
          <div className="auth-actions">
            <Link className="btn btn-primary" href="/forgot-password">Запросить новую ссылку</Link>
          </div>
        </>
      )}

      {state === 'done' && (
        <>
          <div className="warning-note" role="status">{message}</div>
          <div className="auth-actions">
            <Link className="btn btn-primary" href="/login">Войти</Link>
          </div>
        </>
      )}

      {state === 'form' && (
        <form className="auth-form" onSubmit={onSubmit}>
          <p className="auth-lead">Придумай новый пароль. После сохранения все входы на других устройствах завершатся.</p>

          <label className="field">
            <span className="field-label">Новый пароль</span>
            <input
              className="field-input"
              type="password"
              value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
              autoComplete="new-password"
              autoFocus
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

          {error && (
            <div className="warning-note" role="alert">
              {error}{' '}
              {/ссылк/i.test(error) && <Link href="/forgot-password">Запросить новую</Link>}
            </div>
          )}

          <div className="auth-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Сохранение…' : 'Сохранить пароль'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
