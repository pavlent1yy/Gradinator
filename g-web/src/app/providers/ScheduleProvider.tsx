'use client';

import React, { createContext, useContext } from 'react';
import useSchedule from '../../hooks/useSchedule';

type ScheduleContextValue = ReturnType<typeof useSchedule>;

const ScheduleContext = createContext<ScheduleContextValue | null>(null);

export function ScheduleProvider({ children }: { children: React.ReactNode }) {
  const value = useSchedule();
  return <ScheduleContext.Provider value={value}>{children}</ScheduleContext.Provider>;
}

export function useScheduleContext() {
  const ctx = useContext(ScheduleContext);
  if (!ctx) throw new Error('useScheduleContext must be used within ScheduleProvider');
  return ctx;
}