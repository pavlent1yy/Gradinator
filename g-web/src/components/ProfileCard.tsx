'use client';

import { useCallback, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../app/providers/AuthProvider';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import * as api from '../lib/api';
import ConfirmDialog from './ConfirmDialog';
import LogoutButton from './LogoutButton';

export default function ProfileCard() {
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
    const [editingPassword, setEditingPassword] = useState(false);
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [passwordMessage, setPasswordMessage] = useState('');
    const [passwordError, setPasswordError] = useState(false);
    const [changingPassword, setChangingPassword] = useState(false);

    if (initializing || !user) {
        return null;
    }

    async function startGroupEdit() {
        setEditingPassword(false);
        setPasswordMessage('');
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

    function openPasswordEdit() {
        setEditingGroup(false);
        setEditingPassword(true);
        setOldPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setPasswordMessage('');
    }

    async function onChangePassword(e: React.FormEvent) {
        e.preventDefault();
        setPasswordMessage('');
        setPasswordError(true);

        if ((user!.hasPassword && !oldPassword) || !newPassword || !confirmPassword) {
            setPasswordMessage('Заполни все поля.');
            return;
        }
        if (newPassword.length < api.MIN_PASSWORD_LENGTH) {
            setPasswordMessage(`Пароль должен быть не короче ${api.MIN_PASSWORD_LENGTH} символов.`);
            return;
        }
        if (newPassword !== confirmPassword) {
            setPasswordMessage('Пароли не совпадают.');
            return;
        }

        setChangingPassword(true);
        try {
            await api.changePassword(user!.hasPassword ? oldPassword : null, newPassword);
            await refreshUser();
            setEditingPassword(false);
            setPasswordError(false);
            setPasswordMessage(user!.hasPassword ? 'Пароль успешно изменён.' : 'Пароль установлен. Теперь можно входить и по email.');
        } catch (error) {
            setPasswordMessage(error instanceof Error ? error.message : 'Не удалось изменить пароль.');
        } finally {
            setChangingPassword(false);
        }
    }

    const passwordLabel = user.hasPassword ? 'Сменить пароль' : 'Установить пароль';
    const roleLabel = user.role === 'ADMIN' ? 'Администратор' : 'Студент';

    return (
        <section className="account-strip" id="profile" aria-label="Аккаунт">
            <div className="account-main">
                <span className="profile-avatar" aria-hidden="true">
                    {user.email.slice(0, 2).toUpperCase()}
                </span>
                <div className="account-info">
                    <div className="account-email">
                        {user.email}
                        <span className="profile-role">{roleLabel}</span>
                    </div>
                    <div className="account-group">
                        Группа: <strong className="mono">{user.group ?? 'не указана'}</strong>
                        {!editingGroup && (
                            <button type="button" className="link-btn" onClick={startGroupEdit}>
                                изменить
                            </button>
                        )}
                    </div>
                </div>
                <div className="account-actions">
                    <button
                        type="button"
                        className={`btn btn-ghost btn-sm${editingPassword ? ' is-active' : ''}`}
                        onClick={() => (editingPassword ? setEditingPassword(false) : openPasswordEdit())}
                        aria-expanded={editingPassword}
                    >
                        {passwordLabel}
                    </button>
                    <LogoutButton className="btn btn-ghost btn-sm">Выйти</LogoutButton>
                </div>
            </div>

            <div className="account-danger">
                <button type="button" className="account-delete" onClick={() => setConfirmOpen(true)} disabled={deleting}>
                    {deleting ? 'Удаление…' : 'Удалить аккаунт'}
                </button>
            </div>

            {editingPassword && (
                <form className="account-edit password-form" onSubmit={onChangePassword}>
                    {!user.hasPassword && (
                        <p className="profile-hint">
                            Ты входишь через Google или GitHub, поэтому пароля у аккаунта пока нет.
                            Установи его, чтобы входить и по email.
                        </p>
                    )}
                    <div className="password-fields">
                        {user.hasPassword && (
                            <label className="field">
                                <span className="field-label">Текущий пароль</span>
                                <input
                                    className="field-input"
                                    type="password"
                                    value={oldPassword}
                                    onChange={(e) => setOldPassword(e.target.value)}
                                    autoComplete="current-password"
                                    autoFocus
                                />
                            </label>
                        )}
                        <label className="field">
                            <span className="field-label">Новый пароль</span>
                            <input
                                className="field-input"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                autoComplete="new-password"
                                autoFocus={!user.hasPassword}
                            />
                        </label>
                        <label className="field">
                            <span className="field-label">Подтверждение пароля</span>
                            <input
                                className="field-input"
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                autoComplete="new-password"
                            />
                        </label>
                    </div>
                    <div className="profile-actions">
                        <button type="submit" className="btn btn-primary" disabled={changingPassword}>
                            {changingPassword ? 'Сохранение…' : 'Сохранить'}
                        </button>
                        <button type="button" className="btn btn-ghost" onClick={() => setEditingPassword(false)}>
                            Отмена
                        </button>
                    </div>
                </form>
            )}

            {passwordMessage && (
                <div className="warning-note profile-note" role={passwordError ? 'alert' : 'status'}>{passwordMessage}</div>
            )}

            {editingGroup && (
                <form className="account-edit group-edit-form" onSubmit={onChangeGroup}>
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

            {deleteError && (
                <div className="warning-note profile-note" role="alert">{deleteError}</div>
            )}

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
