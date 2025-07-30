import { Decorator } from './types';

export const shulkerDecorator: Decorator = (item, mcData) => {
  if (!mcData.items.get(item.item_id)?.key?.endsWith('shulker_box'))
    return null;

  const nbtItemsList: [] | null =
    item.nbt?.value?.BlockEntityTag?.value?.Items?.value?.value;

  if (!nbtItemsList || nbtItemsList.length === 0) return 'Empty';

  return null;
};
