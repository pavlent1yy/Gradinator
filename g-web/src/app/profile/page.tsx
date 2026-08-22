'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../providers/AuthProvider';

const ROLE_LABELS: Record<string, string> = {
  STUDENT: 'Студент',
  TEACHER: 'Преподаватель',
  ADMIN: 'Администратор'
};

function initialsFromEmail(email: string) {
  const name = email.split('@')[0] || email;
  return name.slice(0, 2).toUpperCase();
}

export default function ProfilePage() {
  const router = useRouter();
  const { user, initializing, logout } = useAuthContext();

  useEffect(() => {
    if (!initializing && !user) {
      router.replace('/login');
    }
  }, [initializing, user, router]);

  if (initializing) {
    return (
      <div className="status-card" role="status">
        Загрузка профиля…
      </div>
    );
  }

  if (!user) {
    return null;
  }

  async function onLogout() {
    await logout();
    router.push('/login');
  }

  return (
    <section className="profile-card" aria-labelledby="profile-title">
      <div className="profile-id">
        <span className="profile-avatar">{initialsFromEmail(user.email)}</span>
        <div>
          <div id="profile-title" className="profile-name">{user.email}</div>
          <span className="profile-role">{ROLE_LABELS[user.role] ?? user.role}</span>
        </div>
      </div>

      <dl className="profile-rows">
        <div className="profile-row">
          <dt>ID</dt>
          <dd>#{user.id}</dd>
        </div>
        <div className="profile-row">
          <dt>Email</dt>
          <dd>{user.email}</dd>
        </div>
        <div className="profile-row">
          <dt>Группа</dt>
          <dd>{user.group ?? '— не указана —'}</dd>
        </div>
        <div className="profile-row">
          <dt>Роль</dt>
          <dd>{ROLE_LABELS[user.role] ?? user.role}</dd>
        </div>
      </dl>

      <div className="auth-actions">
        <button type="button" className="btn btn-primary" onClick={() => router.push('/')}>
          К расписанию
        </button>
        <button type="button" className="btn btn-ghost" onClick={onLogout}>
          Выйти
        </button>
      </div>
    </section>
  );
}
