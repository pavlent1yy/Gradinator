'use client';

import { useEffect, useState } from 'react';
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

  const [editingGroup, setEditingGroup] = useState(false);
  const [groups, setGroups] = useState<string[]>([]);
  const [selectedGroup, setSelectedGroup] = useState('');
  const [changingGroup, setChangingGroup] = useState(false);
  const [groupMessage, setGroupMessage] = useState('');

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

	async function startGroupEdit() {
	if (!user) {
		return;
	}

	try {
		const response = await fetch(
		'http://localhost:9091/core/schedule/groups'
		);

		if (!response.ok) {
		throw new Error('Не удалось загрузить список групп');
		}

		const data: string[] = await response.json();

		setGroups(data);
		setSelectedGroup(user.group ?? '');
		setGroupMessage('');
		setEditingGroup(true);
	} catch (error) {
		setGroupMessage(
		error instanceof Error
			? error.message
			: 'Не удалось загрузить группы'
		);
	}
	}

  async function onChangeGroup() {
    if (!user) {
      return;
    }

    if (!selectedGroup || selectedGroup === user.group) {
      return;
    }

    setChangingGroup(true);
    setGroupMessage('');

    try {
      const response = await fetch(
        'http://localhost:9091/user/change-group',
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({
            newGroup: selectedGroup
          })
        }
      );

      if (!response.ok) {
        throw new Error('Не удалось изменить группу');
      }

      setEditingGroup(false);
      setGroupMessage('Группа успешно изменена.');

      // После подключения обновления user через AuthProvider
      // здесь нужно будет обновить user.group.
    } catch (error) {
      setGroupMessage(
        error instanceof Error
          ? error.message
          : 'Не удалось изменить группу'
      );
    } finally {
      setChangingGroup(false);
    }
  }

  async function onLogout() {
    await logout();
    router.push('/login');
  }

  return (
    <section
      className="auth-panel profile-card"
      aria-labelledby="profile-title"
    >
      <div className="profile-id">
        <span className="profile-avatar">
          {initialsFromEmail(user.email)}
        </span>

        <div>
          <div id="profile-title" className="profile-name">
            {user.email}
          </div>

          <span className="profile-role">
            {ROLE_LABELS[user.role] ?? user.role}
          </span>
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

          <dd>
            {!editingGroup ? (
              <>
                <span>
                  {user.group ?? '— не указана —'}
                </span>

                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={startGroupEdit}
                >
                  Сменить группу
                </button>
              </>
            ) : (
              <>
                <select
                  className="field-input field-select"
                  value={selectedGroup}
                  onChange={(e) => setSelectedGroup(e.target.value)}
                >
                  <option value="">
                    Выберите группу
                  </option>

                  {groups.map((group) => (
                    <option key={group} value={group}>
                      {group}
                    </option>
                  ))}
                </select>

                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={onChangeGroup}
                  disabled={
                    !selectedGroup ||
                    selectedGroup === user.group ||
                    changingGroup
                  }
                >
                  {changingGroup ? 'Сохранение…' : 'Сохранить'}
                </button>

                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => {
                    setEditingGroup(false);
                    setGroupMessage('');
                  }}
                >
                  Отмена
                </button>
              </>
            )}
          </dd>
        </div>

        <div className="profile-row">
          <dt>Роль</dt>
          <dd>
            {ROLE_LABELS[user.role] ?? user.role}
          </dd>
        </div>
      </dl>

      {groupMessage && (
        <div className="warning-note" role="status">
          {groupMessage}
        </div>
      )}

      <div className="auth-actions">
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => router.push('/')}
        >
          К расписанию
        </button>

        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => router.push('/change-password')}
        >
          Сменить пароль
        </button>

        <button
          type="button"
          className="btn btn-ghost"
          onClick={onLogout}
        >
          Выйти
        </button>
      </div>
    </section>
  );
}