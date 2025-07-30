import { Decorator } from './types';
import { anvilNameDecorator } from './anvilName';
import { shulkerDecorator } from './shulker';
import { beeNestDecorator } from './beeNest';
import { potionDecorator } from './potion';

export * from './types';

export const decorators: Decorator[] = [
  anvilNameDecorator,
  shulkerDecorator,
  beeNestDecorator,
  potionDecorator,
];
