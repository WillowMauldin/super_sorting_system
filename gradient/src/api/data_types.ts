export type ItemType = {
  rawId: number;
  displayName: string;
  key: string;
  maxCount: number;
};

export type ItemTypeById = Map<number, ItemType>;
