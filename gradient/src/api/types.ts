export type Vec3 = {
  x: number;
  y: number;
  z: number;
};

export type Dimension = 'TheNether' | 'Overworld' | 'TheEnd';

export type Loc = {
  vec3: Vec3;
  dim: Dimension;
};

export const vecEq = (a: Vec3, b: Vec3) =>
  a.x === b.x && a.y === b.y && a.z === b.z;

export const locEq = (a: Loc, b: Loc) =>
  vecEq(a.vec3, b.vec3) && a.dim === b.dim;

export type Vec2 = Omit<Vec3, 'y'>;

export const vec2ContainedBy = (bounds: [Vec2, Vec2], vec: Vec2) => {
  const minX = Math.min(bounds[0].x, bounds[1].x);
  const maxX = Math.max(bounds[0].x, bounds[1].x);
  const minZ = Math.min(bounds[0].z, bounds[1].z);
  const maxZ = Math.max(bounds[0].z, bounds[1].z);

  return minX <= vec.x && maxX >= vec.x && minZ <= vec.z && maxZ >= vec.z;
};

export type Hold = {
  id: string;
  location: Loc;
  slot: number;
  valid_until: string;
};

export type Item = {
  item_id: number;
  count: number;
  metadata: number;
  data_components: any;
  stack_size: number;
  stackable_hash: string;
  full_shulker_stackable_hash?: string;
};
