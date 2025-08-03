import { Decorator } from './types';

export const beeNestDecorator: Decorator = (item, mcData) => {
  const itemType = mcData.items.get(item.item_id);
  if (!itemType || !['bee_nest', 'beehive'].includes(itemType.key)) return null;

  const beesList: [] | null = item.data_components?.['minecraft:bees'];

  if (!beesList || beesList.length === 0) return '0 Bees';

  if (beesList.length === 1) return '1 Bee';

  return `${beesList.length} Bees`;
};
