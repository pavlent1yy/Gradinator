import type { Pair, Slot } from '../types/schedule';

function isFilled(slot?: Slot | null): slot is Slot {
  return !!slot && !slot.empty;
}

/**
 * Знаменатель: слот знаменателя, иначе fallback на числитель.
 * Числитель: только числитель.
 */
export function pickSlot(pair: Pair, weekType?: string | null): Slot | null {
  if (weekType === 'DENOMINATOR') {
    if (isFilled(pair.denominator)) return pair.denominator;
    if (isFilled(pair.numerator)) return pair.numerator;
    return null;
  }

  return isFilled(pair.numerator) ? pair.numerator : null;
}

export function joinList(items?: string[] | null, fallback = '—') {
  if (!items?.length) return fallback;
  return items.filter(Boolean).join(', ') || fallback;
}
