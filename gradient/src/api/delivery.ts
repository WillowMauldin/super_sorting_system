import assert from 'assert';
import { releaseHolds, ExtendedItem, executeOperation } from '../helpers';
import { createHold, getSignConfig } from './automation';
import { HoldRequestFilter } from './automation_types';

export type DeliveryItems = {
  item: ExtendedItem;
  shulkerCount: number;
  itemCount: number;
}[];

export const deliverItems = async (
  destinationLoc: string,
  itemList: DeliveryItems,
): Promise<void> => {
  const {
    data: { nodes },
  } = await getSignConfig();
  const destNode = nodes[destinationLoc];

  assert(destNode, 'Destination location does not exist');
  assert(destNode.dropoff, 'Destination does not have a drop-off location');

  const holdsToDeliver: string[] = [];

  try {
    const itemRequests: HoldRequestFilter[] = [];
    
    for (const { item, shulkerCount, itemCount } of itemList) {
      if (itemCount > 0) {
        itemRequests.push({
          ItemMatch: {
            match_criteria: {
              StackableHash: { stackable_hash: item.stackable_hash },
            },
            total: itemCount,
          },
        });
      }
      
      if (shulkerCount > 0) {
        // Request full shulkers containing the item using the full_shulker_stackable_hash
        if (item.full_shulker_stackable_hash) {
          itemRequests.push({
            ItemMatch: {
              match_criteria: {
                StackableHash: { stackable_hash: item.full_shulker_stackable_hash },
              },
              total: shulkerCount,
            },
          });
        } else {
          // Fallback: request equivalent number of individual items
          const totalItems = shulkerCount * item.stack_size * 27;
          itemRequests.push({
            ItemMatch: {
              match_criteria: {
                StackableHash: { stackable_hash: item.stackable_hash },
              },
              total: totalItems,
            },
          });
        }
      }
    }
    const holdRequestResults = await createHold(itemRequests);

    for (const holdRes of holdRequestResults.data.results) {
      if ('Error' in holdRes) {
        throw new Error('Failed to acquire items');
      }

      holdsToDeliver.push(...holdRes.Holds.holds.map(({ id }) => id));
    }

    // TODO Chunk deliveries
    assert(holdsToDeliver.length <= 27, 'Too many slots to deliver!');

    await executeOperation(
      {
        type: 'DropItems',
        source_holds: holdsToDeliver,
        drop_from: destNode.location,
        aim_towards: destNode.dropoff,
      },
      'UserInteractive',
    );
  } finally {
    await releaseHolds(holdsToDeliver).catch(() => null);
  }
};
