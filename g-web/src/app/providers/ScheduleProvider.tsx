'use client';

import React, { createContext, useCallback, useContext, useMemo } from 'react';
import { usePathname } from 'next/navigation';
import useSchedule from '../../hooks/useSchedule';
import { useAuthContext } from './AuthProvider';

type ScheduleContextValue = ReturnType<typeof useSchedule> & {
  homeGroup: string | null;
  isOwnGroup: boolean;
  goToOwnGroup: () => void;
};

const ScheduleContext = createContext<ScheduleContextValue | null>(null);

// Расписание нужно только на главной. На остальных страницах список групп
// по-прежнему доступен (он дешёвый и кэшированный), а тяжёлый запрос
// расписания не выполняется.
function isSchedulePage(pathname: string | null): boolean {
  return pathname === '/';
}

export function ScheduleProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuthContext();
  const pathname = usePathname();
  const scheduleEnabled = isSchedulePage(pathname);

  const homeGroup = user?.group ?? null;
  const base = useSchedule(homeGroup, { scheduleEnabled });

  const isOwnGroup = !homeGroup || base.group === homeGroup;

  const goToOwnGroup = useCallback(() => {
    if (homeGroup) base.setGroup(homeGroup);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [homeGroup, base.setGroup]);

  const value = useMemo<ScheduleContextValue>(
    () => ({ ...base, homeGroup, isOwnGroup, goToOwnGroup }),
    [base, homeGroup, isOwnGroup, goToOwnGroup]
  );

  return <ScheduleContext.Provider value={value}>{children}</ScheduleContext.Provider>;
}

export function useScheduleContext() {
  const ctx = useContext(ScheduleContext);
  if (!ctx) throw new Error('useScheduleContext must be used within ScheduleProvider');
  return ctx;
}
