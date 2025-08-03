import { Decorator } from './types';

export const shulkerDecorator: Decorator = (item, mcData) => {
  if (!mcData.items.get(item.item_id)?.key?.endsWith('shulker_box'))
    return null;

  const itemsList: [] | null = item.data_components?.['minecraft:container'];

  if (!itemsList || itemsList.length === 0) return 'Empty';

  return null;
};
