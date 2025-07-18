import { Bot, Chest } from 'mineflayer';
import { Window } from 'prismarine-windows';
import { setTimeout } from 'timers/promises';
import vec3 from 'vec3';
import { navigateTo } from '.';

import { Agent, Location, Vec3 } from '../../types';

export const openChestAt = async (
  chestLoc: Location,
  openFrom: Vec3,
  bot: Bot,
  agent: Agent,
  skipNavigation?: boolean
): Promise<Chest & Window> => {
  if (!skipNavigation)
    await navigateTo(
      {
        dim: chestLoc.dim,
        vec3: openFrom
      },
      bot,
      agent
    );

  let tries = 0;
  while (!bot.world.getColumnAt(vec3(chestLoc.vec3))) {
    await setTimeout(100);

    tries++;
    if (tries > 20) {
      throw new Error(
        'Timeout exceeded while waiting for chunk containing chest.'
      );
    }
  }

  const chestBlock = bot.blockAt(vec3(chestLoc.vec3));
  if (!chestBlock) throw new Error('No block at chest destination!');

  // @ts-ignore mineflayer typing is wrong
  const chest: Chest & Window = await bot.openBlock(chestBlock).catch((err) => {
    throw new Error(
      `Error while opening chest at (${chestLoc.vec3.x}, ${chestLoc.vec3.y}, ${chestLoc.vec3.z}) [${chestLoc.dim}]`,
      { cause: err }
    );
  });

  return chest;
};
