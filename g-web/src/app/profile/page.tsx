'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../providers/AuthProvider';
import * as api from '../../lib/api';

export default function ProfilePage() {
    const router = useRouter();
    const { user, initializing, logout } = useAuthContext();

    const [groups, setGroups] = useState<string[]>([]);
    const [groupsByDepartment, setGroupsByDepartment] = useState<Record<string, string[]>>({});
    const [department, setDepartment] = useState('');
    const [selectedGroup, setSelectedGroup] = useState('');
    const [groupMessage, setGroupMessage] = useState('');
    const [changingGroup, setChangingGroup] = useState(false);
    const [editingGroup, setEditingGroup] = useState(false);

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

    async function startGroupEdit() {
        setEditingGroup(true);
        setGroupMessage('');

        try {
            const [data, byDept] = await Promise.all([
                api.fetchGroups(),
                api.fetchGroupsByDepartment()
            ]);

            setGroups(data);
            setGroupsByDepartment(byDept);

            const dept = user!.group
                ? await api.findDepartmentByGroup(user!.group)
                : null;

            setDepartment(dept ?? '');
            setSelectedGroup(user!.group ?? '');
        } catch {
            setGroupMessage('Не удалось загрузить список групп.');
            setEditingGroup(false);
        }
    }

    async function onChangeGroup(e: React.FormEvent) {
        e.preventDefault();

        if (!selectedGroup) {
            setGroupMessage('Выберите группу.');
            return;
        }

        setChangingGroup(true);
        setGroupMessage('');

        try {
            await api.changeGroup(selectedGroup);

            setGroupMessage('Группа успешно изменена.');
            setEditingGroup(false);
        } catch (error) {
            setGroupMessage(
                error instanceof Error
                    ? error.message
                    : 'Не удалось изменить группу.'
            );
        } finally {
            setChangingGroup(false);
        }
    }

    return (
        <section className="profile-panel" aria-labelledby="profile-title">
            <h1 id="profile-title" className="auth-title">
                Профиль
            </h1>

            <div className="profile-field">
                <span className="profile-label">Email</span>
                <span className="profile-value">{user.email}</span>
            </div>

            <div className="profile-field">
                <span className="profile-label">Роль</span>
                <span className="profile-value">{user.role}</span>
            </div>

            <div className="profile-field">
                <span className="profile-label">Группа</span>

                {!editingGroup && (
                    <span className="profile-value">
                        {user.group ?? 'не указана'}
                    </span>
                )}

                {editingGroup && (
                    <form
                        className="group-edit-form"
                        onSubmit={onChangeGroup}
                    >
                        <div className="field">
                            <label
                                htmlFor="groupSelect"
                                className="field-label"
                            >
                                Новая группа
                            </label>

                            <div className="group-edit-row">
                                <select
                                    className="field-input field-select"
                                    value={department}
                                    onChange={(e) => {
                                        const d = e.target.value;
                                        setDepartment(d);
                                        const list = d
                                            ? (groupsByDepartment[d] ?? [])
                                            : groups;
                                        setSelectedGroup((prev) =>
                                            prev && list.includes(prev) ? prev : ''
                                        );
                                    }}
                                >
                                    <option value="">Все отделения</option>
                                    {Object.keys(groupsByDepartment).map((d) => (
                                        <option key={d} value={d}>{d}</option>
                                    ))}
                                </select>

                                <select
                                    id="groupSelect"
                                    className="field-input field-select"
                                    value={selectedGroup}
                                    onChange={(e) =>
                                        setSelectedGroup(e.target.value)
                                    }
                                >
                                    <option value="">
                                        Выберите группу
                                    </option>

                                    {(department
                                        ? (groupsByDepartment[department] ?? [])
                                        : groups
                                    ).map((group) => (
                                        <option key={group} value={group}>
                                            {group}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="auth-actions">
                            <button
                                type="submit"
                                className="btn btn-primary"
                                disabled={changingGroup}
                            >
                                {changingGroup
                                    ? 'Сохранение…'
                                    : 'Сохранить'}
                            </button>

                            <button
                                type="button"
                                className="btn btn-ghost"
                                onClick={() => setEditingGroup(false)}
                            >
                                Отмена
                            </button>
                        </div>
                    </form>
                )}
            </div>

            {groupMessage && (
                <div className="warning-note" role="alert">
                    {groupMessage}
                </div>
            )}

            {!editingGroup && (
                <div className="auth-actions">
                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={startGroupEdit}
                    >
                        Сменить группу
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
                        onClick={logout}
                    >
                        Выйти
                    </button>
                </div>
            )}
        </section>
    );
}
