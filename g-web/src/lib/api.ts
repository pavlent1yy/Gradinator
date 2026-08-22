import type { Schedule } from '../types/schedule';

const API_BASE = '/api/core';

export async function fetchGroups(): Promise<string[]> {
  const res = await fetch(`${API_BASE}/schedule/groups`);
  if (!res.ok) throw new Error(`fetchGroups: ${res.status}`);
  const data = await res.json();
  if (!Array.isArray(data)) throw new Error('Invalid groups response');
  return data;
}

export async function fetchSchedule(group: string, dateIso: string): Promise<Schedule | { error: string }> {
  const url = `${API_BASE}/schedule?group=${encodeURIComponent(group)}&date=${encodeURIComponent(dateIso)}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`fetchSchedule: ${res.status}`);
  const data = await res.json();
  return data;
}

/* ---------------------------------------------------------------------- */
/* Auth                                                                    */
/* ---------------------------------------------------------------------- */

export type LoginPayload = { email: string; password: string };

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
};

export type Me = {
  id: number;
  email: string;
  group: string | null;
  role: string;
};

async function readErrorMessage(res: Response, fallback: string): Promise<string> {
  const data = await res.json().catch(() => null);
  return data?.error ? data.error : `${fallback}: ${res.status}`;
}

export async function login(payload: LoginPayload): Promise<AuthTokens> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось войти'));
  return res.json();
}

export async function logout(refreshToken: string): Promise<void> {
  await fetch(`${API_BASE}/auth/logout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  }).catch(() => null);
}

export async function refreshTokens(refreshToken: string): Promise<AuthTokens> {
  const res = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось обновить сессию'));
  return res.json();
}

export async function fetchMe(accessToken: string): Promise<Me> {
  const res = await fetch(`${API_BASE}/auth/me`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Не удалось получить профиль'));
  return res.json();
}
