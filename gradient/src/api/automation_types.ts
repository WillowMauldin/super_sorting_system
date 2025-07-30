import { Dimension, Item, Loc, Vec2, Vec3 } from './types';

export type InventoryWithLoc = {
  slots: Array<Item | null>;
  loc: Loc;
  open_from: Vec3;
};

export type InventoriesWithLoc = Array<InventoryWithLoc>;

export type PathfindingNode = {
  location: Loc;
  name: string;
  pickup?: Vec3;
  dropoff?: Vec3;
  home: boolean;
};

export type StorageComplex = {
  FlatFloor: {
    dimension: Dimension;
    y_level: number;
    bounds: [Vec2, Vec2];
    name: string;
  };
  Tower: {
    dimension: Dimension;
    origin: Vec3;
    name: string;
    height: number;
  };
};

type SignParseError = {
  type: 'NoMarker' | 'OffsetParseFailed' | 'UnknownSignType' | 'NameEmpty';
};

type SignValidationError =
  | {
      type: 'DuplicatePathfindingNode';
      name: string;
    }
  | {
      type: 'UnknownNode';
      name: string;
    };

export type CompiledSignConfig = {
  nodes: { [name: string]: PathfindingNode };
  complexes: { [name: string]: StorageComplex };

  sign_parse_errors: SignParseError[];
  validation_errors: SignValidationError[];
};

export type Hold = {
  id: string;
  location: Loc;
  slot: number;
  valid_until: string;
};

export type ItemMatchCriteria = {
  StackableHash: { stackable_hash: string };
};

export type HoldRequestFilter =
  | 'EmptySlot'
  | {
      ItemMatch: {
        match_criteria: ItemMatchCriteria;
        total: number;
      };
    }
  | {
      SlotLocation: {
        location: Loc;
        slot: number;
        open_from: Vec3;
      };
    };

export type HoldMatchResult =
  | { Holds: { holds: Hold[] } }
  | { Error: { error: HoldMatchError } };

export type HoldMatchError =
  | {
      AlreadyHeld: {};
    }
  | { NoMatch: {} };

export type OperationPriority =
  | 'SystemCritical'
  | 'UserInteractive'
  | 'Background'
  | 'LowPriority';

export type OperationStatus = 'Pending' | 'InProgress' | 'Complete' | 'Aborted';

export type ScanInventoryOperationKind = {
  type: 'ScanInventory';
  location: Loc;
};

export type ScanSignsOperationKind = {
  type: 'ScanSigns';
  location: Loc;
};

export type MoveItemsOperationKind = {
  type: 'MoveItems';
  source_holds: string[];
  destination_holds: string[];
  counts: number[];
};

export type DropItemsOperationKind = {
  type: 'DropItems';
  drop_from: Loc;
  aim_towards: Vec3;
  source_holds: string[];
};

export type ImportInventoryOperationKind = {
  type: 'ImportInventory';
  chest_location: Vec3;
  node_location: Loc;
  destination_holds: string[];
};

export type OperationKind =
  | ScanInventoryOperationKind
  | ScanSignsOperationKind
  | MoveItemsOperationKind
  | DropItemsOperationKind
  | ImportInventoryOperationKind;

export type Operation = {
  id: string;
  priority: OperationPriority;
  status: OperationStatus;
  kind: OperationKind;
};
