'use client';

import { useMemo } from 'react';
import { useScheduleContext } from '../app/providers/ScheduleProvider';
import Combo from './Combo';
import DateNav from './DateNav';

type Props = {
  showGroupPicker?: boolean;
  showDate?: boolean;
  showRefresh?: boolean;
  children?: React.ReactNode;
};

export default function PageToolbar({ showGroupPicker = true, showDate = true, showRefresh = false, children }: Props) {
  const {
    groups,
    groupsByDepartment,
    departments,
    department,
    setDepartment,
    group,
    setGroup,
    date,
    prevDate,
    nextDate,
    setDate,
    goToday,
    refresh,
    loading,
    updatedAt,
    homeGroup,
    isOwnGroup,
    goToOwnGroup
  } = useScheduleContext();

  const visibleGroups = useMemo(
    () => (department ? (groupsByDepartment[department] ?? groups) : groups),
    [department, groupsByDepartment, groups]
  );

  return (
    <div className="page-toolbar" role="toolbar" aria-label="Параметры страницы">
      {showGroupPicker && (
        <div className="toolbar-group">
          <Combo
            label="Отделение"
            tab="Отделение"
            options={departments}
            value={department}
            onChange={setDepartment}
            includeAll
            allLabel="Все отделения"
          />
          <Combo label="Выбрать группу" options={visibleGroups} value={group} onChange={setGroup} homeGroup={homeGroup} />
          {!isOwnGroup && homeGroup && (
            <button type="button" className="own-group-pin" onClick={goToOwnGroup}>
              Моя группа: {homeGroup}
            </button>
          )}
        </div>
      )}

      {children}

      <div className="toolbar-end">
        {showDate && (
          <DateNav dateIso={date} onPrev={prevDate} onNext={nextDate} onPick={setDate} onToday={goToday} />
        )}
        {showRefresh && (
          <div className="refresh-box">
            <button
              className={`icon-btn${loading ? ' btn-spin' : ''}`}
              aria-label="Обновить расписание"
              title="Обновить"
              onClick={refresh}
              aria-busy={loading}
              type="button"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M21 12a9 9 0 1 0-3.5 6.9" />
                <polyline points="21 3 21 9 15 9" />
              </svg>
            </button>
            <span className="updated-stamp" aria-live="polite">
              {updatedAt ? `обновлено ${updatedAt.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}` : 'обновлено —'}
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
