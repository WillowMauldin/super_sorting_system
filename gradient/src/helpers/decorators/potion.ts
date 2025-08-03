import { Decorator } from './types';

export const potionDecorator: Decorator = (item): string | null => {
  const nbtPotionValue: string | undefined =
    item.data_components?.value?.Potion?.value;
  if (!nbtPotionValue) return null;

  const valueArr = nbtPotionValue.split(':');
  const value = valueArr[valueArr.length - 1];

  return value
    .split('_')
    .map((word) => word[0].toUpperCase() + word.substring(1))
    .join(' ');
};
