use hashbrown::{hash_map::Iter, HashMap};

use crate::types::{Inventory, Item, Location, Vec3};

pub struct InventoryState {
    inventory_map: HashMap<Location, Inventory>,
}

impl Default for InventoryState {
    fn default() -> Self {
        InventoryState {
            inventory_map: Default::default(),
        }
    }
}

impl InventoryState {
    pub fn set_inventory_at(&mut self, location: Location, inventory: Inventory) {
        self.inventory_map.insert(location, inventory);
    }

    pub fn inventory_contents_at(&self, location: &Location) -> Option<&Inventory> {
        self.inventory_map.get(location)
    }

    pub fn iter_inventories(&self) -> Iter<Location, Inventory> {
        self.inventory_map.iter()
    }

    pub fn iter_slots(&self) -> impl Iterator<Item = (Location, usize, &Option<Item>, Vec3)> {
        self.iter_inventories().flat_map(|(&loc, inv)| {
            inv.slots
                .iter()
                .enumerate()
                .map(move |(slot, item)| (loc, slot, item, inv.open_from))
        })
    }

    pub fn get_listing(&self, options: InventoryListingOptions) -> Vec<Item> {
        let mut item_map = HashMap::<u64, Item>::new();
        let mut full_shulker_hashes = HashMap::<u64, u64>::new(); // item_hash -> shulker_hash

        let insert_item = |item: &Item, item_map: &mut HashMap<u64, Item>| {
            let mapped_item = item_map.get_mut(&item.stackable_hash);

            if let Some(mapped_item) = mapped_item {
                (*mapped_item).count += item.count;
            } else {
                item_map.insert(item.stackable_hash, item.clone());
            }
        };

        for (_loc, _slot, item, _open_from) in self.iter_slots() {
            if let Some(item) = item {
                if let Some(shulker_data) = &item.shulker_data {
                    let is_empty = shulker_data.contained_items.len() == 0;

                    let should_unpack = !is_empty
                        && (options.shulker_unpacking == ShulkerUnpacking::FullListing
                            || (options.shulker_unpacking == ShulkerUnpacking::UnnamedOnly
                                && shulker_data.name.is_none()));

                    // Check if this shulker contains full stacks of a single item type
                    let full_shulker_hash = if !is_empty && shulker_data.contained_items.len() > 0 {
                        let first_item = &shulker_data.contained_items[0];
                        let all_same_type = shulker_data.contained_items.iter()
                            .all(|item| item.stackable_hash == first_item.stackable_hash);
                        let all_full_stacks = shulker_data.contained_items.iter()
                            .all(|item| item.count == item.stack_size);
                        
                        if all_same_type && all_full_stacks {
                            full_shulker_hashes.insert(first_item.stackable_hash, item.stackable_hash);
                            Some(item.stackable_hash) // This shulker's hash represents a full shulker of the contained item
                        } else {
                            None
                        }
                    } else {
                        None
                    };

                    if should_unpack {
                        for contained_item in shulker_data.contained_items.clone() {
                            insert_item(&contained_item, &mut item_map);
                        }
                    } else {
                        insert_item(item, &mut item_map);
                    }
                } else {
                    insert_item(item, &mut item_map);
                }
            }
        }

        // Update all items in the map with their corresponding full_shulker_stackable_hash
        for (item_hash, shulker_hash) in full_shulker_hashes {
            if let Some(item) = item_map.get_mut(&item_hash) {
                item.full_shulker_stackable_hash = Some(shulker_hash.to_string());
            }
        }

        item_map.into_values().collect()
    }
}

#[derive(PartialEq, Eq)]
pub enum ShulkerUnpacking {
    FullListing,
    UnnamedOnly,
    None,
}

pub struct InventoryListingOptions {
    pub shulker_unpacking: ShulkerUnpacking,
}
