'use client';

import { usePathname } from 'next/navigation';
import { useScheduleContext } from '../app/providers/ScheduleProvider';

const DATE_PAGES = new Set(['/', '/rooms']);

export default function PageFlip({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { date } = useScheduleContext();
  const sheetKey = DATE_PAGES.has(pathname) ? `${pathname}|${date}` : pathname;

  return (
    <div key={sheetKey} className="sheet-flip">
      {children}
    </div>
  );
}
