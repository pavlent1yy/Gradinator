import type { Schedule } from '../types/schedule';

const API_BASE = '/api/core';

/* ---------------------------------------------------------------------- */
/* Error helpers                                                          */
/* ---------------------------------------------------------------------- */

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
/* Schedule                                                               */
/* ---------------------------------------------------------------------- */

export async function changeGroup(
    newGroup: string
): Promise<void> {

    const res = await fetch(
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
                'Не удалось изменить группу'
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

    if (!res.ok) throw new Error(`fetchSchedule: ${res.status}`);

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
        throw new Error(
            await readErrorMessage(
                res,
                'Не удалось войти'
            )
        );
    }
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
    const res = await fetch(`${API_BASE}/auth/me`, {
        credentials: 'include'
    });

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
