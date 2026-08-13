export type Teacher = string;
export type Room = string;
export type Subject = string;

export type Slot = {
  empty: boolean;
  rooms: Room[];
  subjects: Subject[];
  teachers: Teacher[];
};

export type Pair = {
  pairNumber: number;
  numerator: Slot | null;
  denominator: Slot | null;
  hasChanges?: boolean;
};

export type Schedule = {
  group?: string;
  day?: string;
  weekType?: string;
  date?: string; // YYYY-MM-DD
  pairs?: Pair[];
};