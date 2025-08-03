import { Decorator } from './types';

export const anvilNameDecorator: Decorator = (item) => {
  const text = item.data_components?.['minecraft:custom_name']?.text_content;

  if (text && text.length > 0) return `"${text}"`;

  return null;
};
