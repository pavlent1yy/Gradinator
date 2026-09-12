'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../providers/AuthProvider';

export default function ChangePasswordPage() {
  const router = useRouter();
  const { user, initializing } = useAuthContext();

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [changing, setChanging] = useState(false);

  useEffect(() => {
    if (!initializing && !user) {
      router.replace('/login');
    }
  }, [initializing, user, router]);

  if (initializing) {
    return (
      <div className="status-card" role="status">
        Загрузка…
      </div>
    );
  }

  if (!user) {
    return null;
  }

  async function onChangePassword(e: React.FormEvent) {
    e.preventDefault();

    if (!oldPassword || !newPassword || !confirmPassword) {
      setMessage('Заполните все поля.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setMessage('Пароли не совпадают.');
      return;
    }

    setChanging(true);
    setMessage('');

    try {
      const response = await fetch(
        'http://localhost:9091/user/change-password',
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({
            oldPassword,
            newPassword,
          }),
        }
      );

      if (!response.ok) {
        throw new Error('Не удалось изменить пароль.');
      }

      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setMessage('Пароль успешно изменён.');
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Не удалось изменить пароль.'
      );
    } finally {
      setChanging(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="change-password-title">
      <h1 id="change-password-title" className="auth-title">
        Смена пароля
      </h1>

      <p className="auth-lead">
        Введите текущий пароль и новый пароль.
      </p>

      <form className="auth-form" onSubmit={onChangePassword}>
        <div className="field">
          <label htmlFor="oldPassword" className="field-label">
            Текущий пароль
          </label>

          <input
            id="oldPassword"
            type="password"
            className="field-input"
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            autoComplete="current-password"
          />
        </div>

        <div className="field">
          <label htmlFor="newPassword" className="field-label">
            Новый пароль
          </label>

          <input
            id="newPassword"
            type="password"
            className="field-input"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
          />
        </div>

        <div className="field">
          <label htmlFor="confirmPassword" className="field-label">
            Подтверждение нового пароля
          </label>

          <input
            id="confirmPassword"
            type="password"
            className="field-input"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
          />
        </div>

        {message && (
          <div className="warning-note" role="alert">
            {message}
          </div>
        )}

        <div className="auth-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={changing}
          >
            {changing ? 'Сохранение…' : 'Изменить пароль'}
          </button>

          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => router.push('/profile')}
          >
            Назад
          </button>
        </div>
      </form>
    </section>
  );
}

