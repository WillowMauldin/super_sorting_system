import { Decorator } from './types';

export const beeNestDecorator: Decorator = (item, mcData) => {
  const itemType = mcData.items.get(item.item_id);
  if (!itemType || !['bee_nest', 'beehive'].includes(itemType.key))
    return null;

  const nbtBeesList: [] | null =
    item.nbt?.value?.BlockEntityTag?.value?.Bees?.value?.value;

  if (!nbtBeesList || nbtBeesList.length === 0) return '0 Bees';

  if (nbtBeesList.length === 1) return '1 Bee';

  return `${nbtBeesList.length} Bees`;
};
