use serde::{Deserialize, Serialize};
use std::collections::HashMap;

pub struct McData {
    pub items: Vec<McDataItem>,
    pub items_by_id: HashMap<u32, McDataItem>,
    pub items_by_name: HashMap<String, McDataItem>,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct McDataItem {
    #[serde(rename = "rawId")]
    pub id: u32,
    #[serde(rename = "displayName")]
    pub display_name: String,
    #[serde(rename = "key")]
    pub name: String,
    #[serde(rename = "maxCount")]
    pub stack_size: u32,
}

impl McData {
    pub fn init() -> Self {
        let items: Vec<McDataItem> =
            serde_json::from_str(include_str!("../assets/minecraft-data/items.json")).unwrap();
        let mut items_by_id = HashMap::new();
        let mut items_by_name = HashMap::new();

        for item in items.iter() {
            items_by_id.insert(item.id, item.clone());
            items_by_name.insert(item.name.clone(), item.clone());
        }

        Self {
            items,
            items_by_id,
            items_by_name,
        }
    }
}
