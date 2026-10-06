'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../providers/AuthProvider';
import { useScheduleContext } from '../providers/ScheduleProvider';
import * as api from '../../lib/api';
import ConfirmDialog from '../../components/ConfirmDialog';

export default function ProfilePage() {
    const router = useRouter();
    const { user, initializing, logout, refreshUser } = useAuthContext();
    const { setGroup } = useScheduleContext();

    const [groups, setGroups] = useState<string[]>([]);
    const [groupsByDepartment, setGroupsByDepartment] = useState<Record<string, string[]>>({});
    const [department, setDepartment] = useState('');
    const [selectedGroup, setSelectedGroup] = useState('');
    const [groupMessage, setGroupMessage] = useState('');
    const [changingGroup, setChangingGroup] = useState(false);
    const [editingGroup, setEditingGroup] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const closeConfirm = useCallback(() => setConfirmOpen(false), []);
    const [deleteError, setDeleteError] = useState('');

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

    async function onDeleteAccount() {
        setDeleting(true);
        setDeleteError('');

        try {
            await api.deleteAccount();
            await logout().catch(() => null);
            router.replace('/');
        } catch (error) {
            setDeleteError(error instanceof Error ? error.message : 'Не удалось удалить аккаунт');
            setDeleting(false);
            setConfirmOpen(false);
        }
    }

    async function onChangeGroup(e: React.FormEvent) {
        e.preventDefault();

        if (!selectedGroup) {
            setGroupMessage('Выбери группу.');
            return;
        }

        setChangingGroup(true);
        setGroupMessage('');

        try {
            await api.changeGroup(selectedGroup);
            await refreshUser();
            setGroup(selectedGroup);

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

    const roleLabel = user.role === 'ADMIN' ? 'Администратор' : 'Студент';

    return (
        <section className="profile-card" aria-labelledby="profile-title">
            <div className="profile-id">
                <span className="profile-avatar" aria-hidden="true">
                    {user.email.slice(0, 2).toUpperCase()}
                </span>
                <div>
                    <h1 id="profile-title" className="profile-name">Профиль</h1>
                    <span className="profile-role">{roleLabel}</span>
                </div>
            </div>

            <dl className="profile-rows">
                <div className="profile-row">
                    <dt>Email</dt>
                    <dd>{user.email}</dd>
                </div>

                <div className="profile-row">
                    <dt>Группа</dt>
                    <dd>
                        {user.group ?? 'не указана'}
                        {!editingGroup && (
                            <button type="button" className="link-btn" onClick={startGroupEdit}>
                                изменить
                            </button>
                        )}
                    </dd>
                </div>
            </dl>

            {editingGroup && (
                <form className="profile-section group-edit-form" onSubmit={onChangeGroup}>
                    <label htmlFor="groupSelect" className="field-label">Новая группа</label>

                    <div className="group-edit-row">
                        <select
                            className="field-input field-select"
                            value={department}
                            aria-label="Отделение"
                            onChange={(e) => {
                                const d = e.target.value;
                                setDepartment(d);
                                const list = d ? (groupsByDepartment[d] ?? []) : groups;
                                setSelectedGroup((prev) => (prev && list.includes(prev) ? prev : ''));
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
                            onChange={(e) => setSelectedGroup(e.target.value)}
                        >
                            <option value="">Выбери группу</option>
                            {(department ? (groupsByDepartment[department] ?? []) : groups).map((group) => (
                                <option key={group} value={group}>{group}</option>
                            ))}
                        </select>
                    </div>

                    <div className="profile-actions">
                        <button type="submit" className="btn btn-primary" disabled={changingGroup}>
                            {changingGroup ? 'Сохранение…' : 'Сохранить'}
                        </button>
                        <button type="button" className="btn btn-ghost" onClick={() => setEditingGroup(false)}>
                            Отмена
                        </button>
                    </div>
                </form>
            )}

            {groupMessage && (
                <div className="warning-note profile-note" role="alert">{groupMessage}</div>
            )}

            <div className="profile-section">
                <h2 className="profile-section-title">Аккаунт</h2>
                <div className="profile-actions">
                    <button type="button" className="btn btn-ghost" onClick={() => router.push('/change-password')}>
                        Сменить пароль
                    </button>
                    <button type="button" className="btn btn-ghost" onClick={logout}>
                        Выйти
                    </button>
                </div>
            </div>

            <div className="profile-section profile-section--danger">
                <h2 className="profile-section-title">Удаление аккаунта</h2>
                <p className="profile-hint">Удалятся email, группа, пропуски и все сессии. Отменить это нельзя.</p>
                <button type="button" className="btn btn-danger" onClick={() => setConfirmOpen(true)} disabled={deleting}>
                    {deleting ? 'Удаление…' : 'Удалить аккаунт'}
                </button>
                {deleteError && (
                    <div className="warning-note profile-note" role="alert">{deleteError}</div>
                )}
            </div>

            <ConfirmDialog
                open={confirmOpen}
                title="Удалить аккаунт?"
                confirmLabel={deleting ? 'Удаление…' : 'Да, удалить'}
                danger
                busy={deleting}
                onConfirm={onDeleteAccount}
                onCancel={closeConfirm}
            >
                <p>
                    Будут удалены email <strong>{user.email}</strong>, группа, все отметки о пропусках и сессии
                    входа. Восстановить аккаунт будет невозможно.
                </p>
            </ConfirmDialog>
        </section>
    );
}
