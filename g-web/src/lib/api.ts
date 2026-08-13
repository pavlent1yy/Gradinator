import type { Schedule } from '../types/schedule';

const API_BASE = '/api/core';

export async function fetchGroups(): Promise<string[]> {
  const res = await fetch(`${API_BASE}/schedule/groups`);
  if (!res.ok) throw new Error(`fetchGroups: ${res.status}`);
  const data = await res.json();
  if (!Array.isArray(data)) throw new Error('Invalid groups response');
  return data;
}

// NOTE: backend may return { error: "..." } with 200 OK — treat that as a valid response (warning)
export async function fetchSchedule(group: string, dateIso: string): Promise<Schedule | { error: string }> {
  const url = `${API_BASE}/schedule?group=${encodeURIComponent(group)}&date=${encodeURIComponent(dateIso)}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`fetchSchedule: ${res.status}`);
  const data = await res.json();
  return data;
}