import type { Schedule } from '../types/schedule';

const API_BASE = '/api/core';

/* ---------------------------------------------------------------------- */
/* Error helpers                                                          */
/* ---------------------------------------------------------------------- */

export const MIN_PASSWORD_LENGTH = 8;

export class ApiError extends Error {
    constructor(message: string, readonly code?: string) {
        super(message);
    }
}

async function readErrorMessage(
    res: Response,
    fallback: string
): Promise<string> {

    const data = await res.json().catch(() => null);

    return data?.error
        ? data.error
        : `${fallback}: ${res.status}`;
}

/* ---------------------------------------------------------------------- */
/* Groups (кэш + dedupe одновременных запросов)                           */
/* ---------------------------------------------------------------------- */

let groupsCache: string[] | null = null;
let groupsInflight: Promise<string[]> | null = null;

/**
 * Список групп меняется редко (раз в семестр), поэтому кэшируем его
 * на время жизни страницы. Повторные вызовы (main + register и т.п.)
 * не порождают новые сетевые запросы.
 */
export function invalidateGroupsCache() {
    groupsCache = null;
}

export async function fetchGroups(): Promise<string[]> {
    if (groupsCache) return groupsCache;
    if (groupsInflight) return groupsInflight;

    groupsInflight = (async () => {
        const res = await fetch(`${API_BASE}/schedule/groups`);

        if (!res.ok) throw new Error(`fetchGroups: ${res.status}`);

        const data = await res.json();

        if (!Array.isArray(data)) throw new Error('Invalid groups response');

        groupsCache = data as string[];
        return groupsCache;
    })();

    try {
        return await groupsInflight;
    } finally {
        groupsInflight = null;
    }
}

/* ---------------------------------------------------------------------- */
/* Departments (кэш + dedupe)                                             */
/* ---------------------------------------------------------------------- */

let departmentsCache: Record<string, string[]> | null = null;
let departmentsInflight: Promise<Record<string, string[]>> | null = null;

const departmentByGroupCache = new Map<string, string>();

/**
 * Группы, разбитые по отделениям: { "OIT": ["ИС1-11", ...], ... }
 * Кэшируется так же, как fetchGroups.
 */
export async function fetchGroupsByDepartment(): Promise<Record<string, string[]>> {
    if (departmentsCache) return departmentsCache;
    if (departmentsInflight) return departmentsInflight;

    departmentsInflight = (async () => {
        const res = await fetch(`${API_BASE}/schedule/groups/departments`);

        if (!res.ok) throw new Error(`fetchGroupsByDepartment: ${res.status}`);

        const data = await res.json();

        if (!data || typeof data !== 'object' || Array.isArray(data)) {
            throw new Error('Invalid departments response');
        }

        departmentsCache = data as Record<string, string[]>;
        return departmentsCache;
    })();

    try {
        return await departmentsInflight;
    } finally {
        departmentsInflight = null;
    }
}

/**
 * Отделение для конкретной группы (/groups/find-department).
 * Маленький кэш на группу — вызывается при редактировании профиля.
 */
export async function findDepartmentByGroup(group: string): Promise<string | null> {
    if (!group) return null;

    const cached = departmentByGroupCache.get(group);
    if (cached) return cached;

    const res = await fetch(
        `${API_BASE}/schedule/groups/find-department?group=${encodeURIComponent(group)}`
    );

    if (!res.ok) return null;

    const dept = (await res.text()).trim();
    if (!dept) return null;

    departmentByGroupCache.set(group, dept);
    return dept;
}

/* ---------------------------------------------------------------------- */
/* Schedule                                                               */
/* ---------------------------------------------------------------------- */

export async function changeGroup(
    newGroup: string
): Promise<void> {

    const res = await authFetch(
        `${API_BASE}/user/change-group`,
        {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                newGroup
            })
        }
    );

    if (!res.ok) {
        throw new Error(
            await readErrorMessage(
                res,
                'Не удалось сменить группу'
            )
        );
    }
}

export async function changePassword(
    oldPassword: string | null,
    newPassword: string
): Promise<void> {

    const res = await authFetch(
        `${API_BASE}/user/change-password`,
        {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                oldPassword,
                newPassword
            })
        }
    );

    if (!res.ok) {
        throw new Error(
            await readErrorMessage(
                res,
                'Не удалось сменить пароль'
            )
        );
    }
}

export async function fetchSchedule(
    group: string,
    dateIso: string,
    signal?: AbortSignal
): Promise<Schedule | { error: string }> {

    const url =
        `${API_BASE}/schedule?group=${encodeURIComponent(group)}&date=${encodeURIComponent(dateIso)}`;

    const res = await fetch(url, { signal });

    if (res.status === 404) {
        return { error: 'Расписания на эту дату пока нет. Попробуй другой день или загляни позже.' };
    }

    if (!res.ok) {
        throw new Error(await readErrorMessage(res, 'Не удалось загрузить расписание, сервер недоступен'));
    }

    return res.json();
}

/* ---------------------------------------------------------------------- */
/* Auth                                                                   */
/* ---------------------------------------------------------------------- */

export type LoginPayload = {
    email: string;
    password: string;
};

export type RegisterPayload = {
    email: string;
    password: string;
    confirmPassword: string;
    group?: string | null;
};

export type AuthTokens = {
    accessToken: string;
};

export type Me = {
    id: number;
    email: string;
    group: string | null;
    role: string;
    hasPassword: boolean;
};

/* ---------------------------------------------------------------------- */
/* Login                                                                  */
/* ---------------------------------------------------------------------- */

export async function login(
    payload: LoginPayload
): Promise<void> {

    const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new ApiError(
            data?.error ?? `Не удалось войти: ${res.status}`,
            data?.code
        );
    }
}

export async function resendVerification(email: string): Promise<string | null> {
    const res = await fetch(`${API_BASE}/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
    });

    if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось отправить письмо'));

    const data = await res.json().catch(() => null);
    return data?.message ?? null;
}

/* ---------------------------------------------------------------------- */
/* Register                                                               */
/* ---------------------------------------------------------------------- */

export async function register(
    payload: RegisterPayload
): Promise<void> {

    const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        throw new Error(await readErrorMessage(res, 'Ошибка регистрации'));
    }
}

/* ---------------------------------------------------------------------- */
/* Logout                                                                 */
/* ---------------------------------------------------------------------- */

export async function logout(): Promise<void> {
    const res = await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        credentials: 'include'
    });

    if (!res.ok) {
        throw new Error(
            await readErrorMessage(
                res,
                'Не удалось выйти'
            )
        );
    }
}

/* ---------------------------------------------------------------------- */
/* Refresh (single-flight: параллельные вызовы = один сетевой запрос)     */
/* ---------------------------------------------------------------------- */

let refreshInflight: Promise<void> | null = null;

export function refreshTokens(): Promise<void> {

    if (!refreshInflight) {
        refreshInflight = (async () => {

            const res = await fetch(
                `${API_BASE}/auth/refresh`,
                {
                    method: 'POST',
                    credentials: 'include'
                }
            );

            if (!res.ok) {
                throw new Error(
                    await readErrorMessage(
                        res,
                        'Не удалось обновить сессию'
                    )
                );
            }

        })();

        refreshInflight
            .catch(() => null)
            .finally(() => {
                refreshInflight = null;
            });
    }

    return refreshInflight;
}

/* ---------------------------------------------------------------------- */
/* Me                                                                     */
/* ---------------------------------------------------------------------- */

export async function fetchMe(): Promise<Me> {
    const res = await authFetch(`${API_BASE}/auth/me`);

    if (!res.ok) {
        throw new Error(
            await readErrorMessage(
                res,
                'Не удалось получить профиль'
            )
        );
    }

    return res.json();
}

/* ---------------------------------------------------------------------- */
/* Запросы под авторизацией: при 401 один раз обновляем сессию и повторяем */
/* ---------------------------------------------------------------------- */

async function authFetch(input: string, init?: RequestInit): Promise<Response> {
    const res = await fetch(input, { ...init, credentials: 'include' });

    if (res.status !== 401) return res;

    try {
        await refreshTokens();
    } catch {
        return res;
    }

    return fetch(input, { ...init, credentials: 'include' });
}

async function sendJson<T>(
    url: string,
    method: string,
    body: unknown,
    fallback: string
): Promise<T> {
    const res = await authFetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });

    if (!res.ok) throw new Error(await readErrorMessage(res, fallback));

    return res.json();
}

async function sendDelete(url: string, fallback: string): Promise<void> {
    const res = await authFetch(url, { method: 'DELETE' });

    if (!res.ok) throw new Error(await readErrorMessage(res, fallback));
}

/* ---------------------------------------------------------------------- */
/* Пропуски                                                               */
/* ---------------------------------------------------------------------- */

export type AbsenceType = 'MISSED' | 'LATE';

export type Absence = {
    id: number;
    date: string;
    pairNumber: number;
    type: AbsenceType;
    subject: string | null;
    hours: number;
};

export type AbsencePeriodStats = {
    from: string | null;
    to: string | null;
    hours: number;
    missedPairs: number;
    lates: number;
};

export type AbsenceStats = {
    week: AbsencePeriodStats;
    month: AbsencePeriodStats;
    semester: AbsencePeriodStats;
    total: AbsencePeriodStats;
};

export async function fetchAbsences(from: string, to: string): Promise<Absence[]> {
    const res = await authFetch(
        `${API_BASE}/absences?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    );

    if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось загрузить пропуски'));

    return res.json();
}

export async function fetchAbsenceStats(): Promise<AbsenceStats> {
    const res = await authFetch(`${API_BASE}/absences/stats`);

    if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось посчитать статистику'));

    return res.json();
}

export function markAbsence(
    date: string,
    pairNumber: number,
    type: AbsenceType,
    subject?: string | null
): Promise<Absence> {
    return sendJson(`${API_BASE}/absences`, 'PUT', { date, pairNumber, type, subject }, 'Не удалось отметить');
}

export function unmarkAbsence(date: string, pairNumber: number): Promise<void> {
    return sendDelete(
        `${API_BASE}/absences?date=${encodeURIComponent(date)}&pairNumber=${pairNumber}`,
        'Не удалось снять отметку'
    );
}

export function markAbsenceDay(date: string): Promise<Absence[]> {
    return sendJson(`${API_BASE}/absences/day`, 'POST', { date }, 'Не удалось отметить день');
}

export function clearAbsenceDay(date: string): Promise<void> {
    return sendDelete(
        `${API_BASE}/absences/day?date=${encodeURIComponent(date)}`,
        'Не удалось очистить день'
    );
}

/* ---------------------------------------------------------------------- */
/* Удаление аккаунта                                                      */
/* ---------------------------------------------------------------------- */

export function deleteAccount(): Promise<void> {
    return sendDelete(`${API_BASE}/user`, 'Не удалось удалить аккаунт');
}

/* ---------------------------------------------------------------------- */
/* Поиск, аудитории, справочники                                          */
/* ---------------------------------------------------------------------- */

export type SearchType = 'ANY' | 'TEACHER' | 'SUBJECT' | 'ROOM';

export type SearchHit = {
    group: string;
    pairNumber: number;
    subjects: string[];
    teachers: string[];
    rooms: string[];
    hasChanges: boolean;
};

export type FreeRooms = {
    pairNumber: number;
    freeRooms: string[];
    busyRooms: string[];
};

export type DictionaryKind = 'teachers' | 'subjects' | 'rooms';

const dictionaryCache = new Map<DictionaryKind, Promise<string[]>>();

export function fetchDictionary(kind: DictionaryKind): Promise<string[]> {
    let cached = dictionaryCache.get(kind);

    if (!cached) {
        cached = fetch(`${API_BASE}/schedule/${kind}`).then((res) => {
            if (!res.ok) throw new Error(`fetchDictionary: ${res.status}`);
            return res.json();
        });
        cached.catch(() => dictionaryCache.delete(kind));
        dictionaryCache.set(kind, cached);
    }

    return cached;
}

export async function searchSchedule(
    query: string,
    type: SearchType,
    dateIso: string,
    signal?: AbortSignal
): Promise<SearchHit[]> {
    const params = new URLSearchParams({ q: query, type, date: dateIso });
    const res = await fetch(`${API_BASE}/schedule/search?${params}`, { signal });

    if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось выполнить поиск'));

    return res.json();
}

export async function fetchFreeRooms(dateIso: string, signal?: AbortSignal): Promise<FreeRooms[]> {
    const res = await fetch(`${API_BASE}/schedule/free-rooms?date=${encodeURIComponent(dateIso)}`, { signal });

    if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось загрузить аудитории'));

    return res.json();
}

let weekTypeCache: Promise<string | null> | null = null;

export function fetchCurrentWeekType(): Promise<string | null> {
    if (!weekTypeCache) {
        weekTypeCache = fetch(`${API_BASE}/schedule/current-weektype`)
            .then((res) => (res.ok ? res.json() : null))
            .then((data) => data?.weekType ?? null)
            .catch(() => null);
    }

    return weekTypeCache;
}

export type WeekDay = {
    date: string;
    schedule: Schedule | null;
};

export async function fetchWeek(group: string, dateIso: string, signal?: AbortSignal): Promise<WeekDay[]> {
    const params = new URLSearchParams({ group, date: dateIso });
    const res = await fetch(`${API_BASE}/schedule/week?${params}`, { signal });

    if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось загрузить неделю'));

    return res.json();
}

/* ---------------------------------------------------------------------- */
/* Восстановление пароля                                                  */
/* ---------------------------------------------------------------------- */

async function postPublic(url: string, body: unknown, fallback: string): Promise<string | null> {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });

    if (!res.ok) throw new Error(await readErrorMessage(res, fallback));

    const data = await res.json().catch(() => null);
    return data?.message ?? null;
}

export function requestPasswordReset(email: string): Promise<string | null> {
    return postPublic(`${API_BASE}/auth/forgot-password`, { email }, 'Не удалось отправить письмо');
}

export function resetPassword(token: string, newPassword: string, confirmPassword: string): Promise<string | null> {
    return postPublic(
        `${API_BASE}/auth/reset-password`,
        { token, newPassword, confirmPassword },
        'Не удалось сменить пароль'
    );
}
