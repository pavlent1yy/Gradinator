import type { Schedule } from '../types/schedule';

const API_BASE = '/api/core';

export async function fetchGroups(): Promise<string[]> {
    const res = await fetch(`${API_BASE}/schedule/groups`);

    if (!res.ok) throw new Error(`fetchGroups: ${res.status}`);

    const data = await res.json();

    if (!Array.isArray(data)) throw new Error('Invalid groups response');

    return data;
}

export async function fetchSchedule(
    group: string,
    dateIso: string
): Promise<Schedule | { error: string }> {

    const url =
        `${API_BASE}/schedule?group=${encodeURIComponent(group)}&date=${encodeURIComponent(dateIso)}`;

    const res = await fetch(url);

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

export type AuthTokens = {
    accessToken: string;
};

export type Me = {
    id: number;
    email: string;
    group: string | null;
    role: string;
};

async function readErrorMessage(
    res: Response,
    fallback: string
): Promise<string> {

    const data = await res.json().catch(() => null);

    return data?.error
        ? data.error
        : `${fallback}: ${res.status}`;
}

async function readLoginError(
    fallback: string
): Promise<string> {

    return `${fallback}`;
}


/* ---------------------------------------------------------------------- */
/* Access token                                                           */
/* ---------------------------------------------------------------------- */

let accessToken: string | null = null;

export function getAccessToken(): string | null {
    return accessToken;
}

export function setAccessToken(token: string) {
    accessToken = token;
}

export function clearAccessToken() {
    accessToken = null;
}


/* ---------------------------------------------------------------------- */
/* Login                                                                  */
/* ---------------------------------------------------------------------- */

export async function login(
    payload: LoginPayload
): Promise<AuthTokens> {

    const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        throw new Error(await readLoginError('Не удалось войти'));
    }

    const data: AuthTokens = await res.json();

    setAccessToken(data.accessToken);

    return data;
}


/* ---------------------------------------------------------------------- */
/* Logout                                                                 */
/* ---------------------------------------------------------------------- */

export async function logout(): Promise<void> {

    await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        credentials: 'include'
    }).catch(() => null);

    clearAccessToken();
}


/* ---------------------------------------------------------------------- */
/* Refresh                                                                */
/* ---------------------------------------------------------------------- */

export async function refreshTokens(): Promise<AuthTokens> {

    const res = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        credentials: 'include'
    });

    if (!res.ok) {
        clearAccessToken();

        throw new Error(
            await readErrorMessage(
                res,
                'Не удалось обновить сессию'
            )
        );
    }

    const data: AuthTokens = await res.json();

    setAccessToken(data.accessToken);

    return data;
}


/* ---------------------------------------------------------------------- */
/* Me                                                                     */
/* ---------------------------------------------------------------------- */

export async function fetchMe(
    accessToken: string
): Promise<Me> {

    const res = await fetch(`${API_BASE}/auth/me`, {
        headers: {
            Authorization: `Bearer ${accessToken}`
        }
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