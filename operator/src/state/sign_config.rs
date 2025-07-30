use std::{
    collections::HashMap,
    num::ParseIntError,
    sync::{Arc, Mutex},
};

use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::types::{Dimension, Location, Vec2, Vec3};

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct Sign {
    pub lines: [String; 4],
    pub location: Location,
}

// Sign Syntax

// Line 1: Marker & Offset for Location (to enable hiding signs)
// SSS[ (+/-)int,(+/-)int,(+/-)int]
// Examples: SSS +0,+2,+0; SSS -0,-2,-0; SSS
// Location offset is an absolute change in X, Y, and Z, not dependent on sign orientation

// Line 2: Sign Type
// Examples: path node

// Lines 3 and 4 are sign type specific

/*
 * Pathfinding Node Signs
 * These signs indicate a node that agents can travel to and from
 */

// Line 2: Sign type "path node"
// Line 3: Pathfinding Node Name
// Line 4: Unused

// Example:
// SSS +0,+2,+0
// path node
// Hallway A
// (4th line empty)

/*
 * Pickup Chest Signs
 * These signs indicate that a given location is a container that items can be picked up from
 * This container must be accessible from the given pathfinding node
 * Only one of these is permitted for a given pathfinding node
 */

// Line 2: Sign type "pickup"
// Line 3: Pathfinding Node Name
// Line 4: Unused

// Example
// SSS 0,-4,0
// pickup
// Hallway A
// (4th line empty)

/*
 * Drop-off Location Signs
 * These signs indicate that a given location is an point where items should be dropped
 * This point will be where the agent looks at while dropping items from the pathfinding node
 * Only one of these is permitted for a given pathfinding node
 */

// Line 2: Sign type "drop-off"
// Line 3: Pathfinding Node Name
// Line 4: Unused

// Example
// SSS 0,-4,0
// drop-off
// Hallway A
// (4th line empty)

/*
 * Portal Signs
 * These signs indicate that a pathfinding node has a portal that connects to another pathfinding
 * node. This point will be inside of the portal. Note that this only establishes
 * a one-way connection - you will need to place another on the other side of the portal for
 * a two-way connection.
 * Only one of these is permitted for a given pathfinding node.
 */

// Line 2: Sign type "portal"
// Line 3: Node this portal is located at
// Line 4: Node this portal leads to

// Example:
// SSS 0,+3,0
// portal
// nether_node
// overworld_node

/*
 * Shulker Station Sign
 * These signs indicate that a pathfinding node is a station for loading / unloading shulkers
 */

// Line 2: Sign type "shulker station"
// Line 3: Node to mark as a shulker station

// Example:
// SSSS
// shulker station
// Hallway A

/*
 * Storage Complex Signs
 * These signs indicate an area of storage containers to be used by the network
 * This will also act as a pathfinding node of the given name
 */

// Line 2: Sign type "storage complex"
// Line 3: Offset similar to line 1 of complex end. This offset is specified from the effective location of the sign
// Line 4: Name of complex

/*
 * Storage Tower Signs
 * These signs indicate a 9x9 tower of chests with a hole in the middle
 */

// Line 2: Sign type: "storage tower"
// Line 3: Height
// Line 4: Name of tower

/*
 * Home Signs
 * These signs mark a pathfinding node as a home station where the bot should return when idle
 */

// Line 2: Sign type "home"
// Line 3: Pathfinding Node Name
// Line 4: Unused

// Example:
// SSS
// home
// Main Hub
// (4th line empty)

#[derive(Debug)]
pub enum ParsedSign {
    PathfindingNode {
        effective_location: Location,
        name: String,
    },
    PickupChest {
        effective_location: Vec3,
        node_name: String,
    },
    DropOffLocation {
        effective_location: Vec3,
        node_name: String,
    },
    Portal {
        effective_location: Vec3,
        source_node_name: String,
        destination_node_name: String,
    },
    ShulkerStation {
        node_name: String,
    },
    StorageComplex {
        dimension: Dimension,
        y_level: i32,
        bounds: (Vec2, Vec2),
        name: String,
    },
    StorageTower {
        dimension: Dimension,
        origin: Vec3,
        height: u32,
        name: String,
    },
    Home {
        name: String,
    },
}

#[derive(Error, Debug, Serialize)]
#[serde(tag = "type")]
pub enum SignParseError {
    #[error("Sign marker not found, this is probably not a SSS config sign")]
    NoMarker,
    #[error("Failed to parse offset")]
    OffsetParseFailed,
    #[error("Sign type unknown")]
    UnknownSignType,
    #[error("Name must not be empty")]
    NameEmpty,
    #[error("Unable to parse height")]
    BadHeight,
}

#[derive(Error, Debug, Serialize)]
#[serde(tag = "type")]
pub enum SignConfigValidationError {
    #[error("Duplicate pathfinding node with name {name}")]
    DuplicatePathfindingNode { name: String },
    #[error("Referenced node {name} is unknown")]
    UnknownNode { name: String },
}

fn parse_offset(offset: &str) -> Result<Vec3, SignParseError> {
    offset
        .split(",")
        .map(|coord| coord.parse())
        .collect::<Result<Vec<i32>, ParseIntError>>()
        .map_err(|_| SignParseError::OffsetParseFailed)
        .and_then(|coords| {
            if coords.len() != 3 {
                return Err(SignParseError::OffsetParseFailed);
            }

            Ok(Vec3 {
                x: coords[0],
                y: coords[1],
                z: coords[2],
            })
        })
}

impl TryFrom<&Sign> for ParsedSign {
    type Error = SignParseError;

    fn try_from(s: &Sign) -> Result<Self, Self::Error> {
        if !s.lines[0].starts_with("SSS") {
            return Err(SignParseError::NoMarker);
        }

        let loc_offset = if let Some(offset) = s.lines[0].strip_prefix("SSS ") {
            parse_offset(offset)?
        } else {
            Vec3 { x: 0, y: 0, z: 0 }
        };

        let effective_location = Location {
            vec3: loc_offset + s.location.vec3,
            dim: s.location.dim,
        };

        match s.lines[1].as_str() {
            "path node" => {
                let name = s.lines[2].clone();

                if name.len() == 0 {
                    return Err(SignParseError::NameEmpty);
                }

                Ok(ParsedSign::PathfindingNode {
                    effective_location,
                    name,
                })
            }
            "pickup" => {
                let node_name = s.lines[2].clone();

                Ok(ParsedSign::PickupChest {
                    node_name,
                    effective_location: effective_location.vec3,
                })
            }
            "drop-off" => {
                let node_name = s.lines[2].clone();

                Ok(ParsedSign::DropOffLocation {
                    node_name,
                    effective_location: effective_location.vec3,
                })
            }
            "portal" => {
                let source_node_name = s.lines[2].clone();
                let destination_node_name = s.lines[3].clone();

                Ok(ParsedSign::Portal {
                    effective_location: effective_location.vec3,
                    source_node_name,
                    destination_node_name,
                })
            }
            "shulker station" => {
                let node_name = s.lines[2].clone();

                Ok(ParsedSign::ShulkerStation { node_name })
            }
            "storage complex" => {
                let name = s.lines[3].clone();
                let second_offset = parse_offset(s.lines[2].as_str())?;

                Ok(ParsedSign::StorageComplex {
                    name,
                    dimension: effective_location.dim,
                    y_level: effective_location.vec3.y,
                    bounds: (
                        effective_location.vec3.into(),
                        (effective_location.vec3 + second_offset).into(),
                    ),
                })
            }
            "storage tower" => Ok(ParsedSign::StorageTower {
                dimension: effective_location.dim,
                origin: effective_location.vec3,
                height: s.lines[2].parse().map_err(|_| SignParseError::BadHeight)?,
                name: s.lines[3].clone(),
            }),
            "home" => {
                let name = s.lines[2].clone();

                if name.len() == 0 {
                    return Err(SignParseError::NameEmpty);
                }

                Ok(ParsedSign::Home { name })
            }
            _ => Err(SignParseError::UnknownSignType),
        }
    }
}

#[derive(Serialize)]
pub struct Portal {
    pub vec3: Vec3,
    pub destination_node_name: String,
}

#[derive(Serialize)]
pub struct PathfindingNode {
    pub location: Location,
    pub name: String,
    pub pickup: Option<Vec3>,
    pub dropoff: Option<Vec3>,
    pub portal: Option<Portal>,
    pub shulker_station: bool,
    pub home: bool,
}

#[derive(Serialize)]
pub enum StorageComplex {
    FlatFloor {
        dimension: Dimension,
        name: String,
        y_level: i32,
        bounds: (Vec2, Vec2),
    },
    Tower {
        dimension: Dimension,
        name: String,
        origin: Vec3,
        height: u32,
    },
}

#[derive(Serialize)]
pub struct CompiledSignConfig {
    pub nodes: HashMap<String, PathfindingNode>,
    pub complexes: HashMap<String, StorageComplex>,

    pub sign_parse_errors: Vec<SignParseError>,
    pub validation_errors: Vec<SignConfigValidationError>,
}

pub struct SignConfigState {
    signs: Vec<Sign>,
    cached_config: Mutex<Option<Arc<CompiledSignConfig>>>,
}

impl Default for SignConfigState {
    fn default() -> Self {
        SignConfigState {
            signs: Default::default(),
            cached_config: Default::default(),
        }
    }
}

impl SignConfigState {
    pub fn clear_area(&mut self, dimension: Dimension, start: Vec2, end: Vec2) {
        self.signs.retain(|sign| {
            !(sign.location.dim == dimension
                && Vec2::from(sign.location.vec3).contained_by(start, end, 0))
        });

        self.set_dirty();
    }

    pub fn add_sign(&mut self, sign: Sign) {
        self.signs.push(sign);

        self.set_dirty();
    }

    fn generate_config(&self) -> CompiledSignConfig {
        let (parsed_signs, parse_errors): (Vec<_>, Vec<_>) = self
            .signs
            .iter()
            .map(|sign| sign.try_into())
            .partition(Result::is_ok);

        let parsed_signs: Vec<ParsedSign> = parsed_signs.into_iter().map(Result::unwrap).collect();
        let sign_parse_errors: Vec<SignParseError> =
            parse_errors.into_iter().map(Result::unwrap_err).collect();

        let mut validation_errors = Vec::new();

        let mut nodes = HashMap::new();

        // Add all nodes to map first
        parsed_signs.iter().for_each(|sign| {
            match sign {
                ParsedSign::PathfindingNode {
                    effective_location,
                    name,
                } => {
                    let existing_node = nodes.insert(
                        name.clone(),
                        PathfindingNode {
                            name: name.clone(),
                            location: *effective_location,
                            pickup: None,
                            dropoff: None,
                            portal: None,
                            shulker_station: false,
                            home: false,
                        },
                    );

                    if existing_node.is_some() {
                        validation_errors.push(
                            SignConfigValidationError::DuplicatePathfindingNode {
                                name: name.clone(),
                            },
                        )
                    }
                }
                ParsedSign::StorageComplex {
                    y_level,
                    bounds,
                    dimension,
                    name,
                } => {
                    let existing_node = nodes.insert(
                        name.clone(),
                        PathfindingNode {
                            name: name.clone(),
                            location: Location {
                                vec3: Vec3 {
                                    x: bounds.0.x,
                                    z: bounds.0.z,
                                    y: y_level + 1,
                                },
                                dim: *dimension,
                            },
                            pickup: None,
                            dropoff: None,
                            portal: None,
                            shulker_station: false,
                            home: false,
                        },
                    );

                    if existing_node.is_some() {
                        validation_errors.push(
                            SignConfigValidationError::DuplicatePathfindingNode {
                                name: name.clone(),
                            },
                        )
                    }
                }
                ParsedSign::StorageTower {
                    origin,
                    height: _height,
                    dimension,
                    name,
                } => {
                    let existing_node = nodes.insert(
                        name.clone(),
                        PathfindingNode {
                            name: name.clone(),
                            location: Location {
                                vec3: *origin,
                                dim: *dimension,
                            },
                            pickup: None,
                            dropoff: None,
                            portal: None,
                            shulker_station: false,
                            home: false,
                        },
                    );

                    if existing_node.is_some() {
                        validation_errors.push(
                            SignConfigValidationError::DuplicatePathfindingNode {
                                name: name.clone(),
                            },
                        )
                    }
                }

                _ => {}
            };
        });

        // Add all pickups and drop-offs
        parsed_signs.iter().for_each(|sign| match sign {
            ParsedSign::DropOffLocation {
                node_name,
                effective_location,
            } => {
                let node = nodes.get_mut(node_name);

                if node.is_none() {
                    validation_errors.push(SignConfigValidationError::UnknownNode {
                        name: node_name.clone(),
                    });
                    return;
                }

                node.unwrap().dropoff = Some(*effective_location)
            }
            ParsedSign::PickupChest {
                node_name,
                effective_location,
            } => {
                let node = nodes.get_mut(node_name);

                if node.is_none() {
                    validation_errors.push(SignConfigValidationError::UnknownNode {
                        name: node_name.clone(),
                    });
                    return;
                }

                node.unwrap().pickup = Some(*effective_location)
            }
            ParsedSign::ShulkerStation { node_name } => {
                let node = nodes.get_mut(node_name);

                if node.is_none() {
                    validation_errors.push(SignConfigValidationError::UnknownNode {
                        name: node_name.clone(),
                    });
                    return;
                }

                node.unwrap().shulker_station = true
            }
            ParsedSign::Home { name } => {
                let node = nodes.get_mut(name);

                if node.is_none() {
                    validation_errors.push(SignConfigValidationError::UnknownNode {
                        name: name.clone(),
                    });
                    return;
                }

                node.unwrap().home = true
            }
            ParsedSign::Portal {
                effective_location,
                source_node_name,
                destination_node_name,
            } => {
                let source_node = nodes.get(source_node_name);
                let destination_node = nodes.get(destination_node_name);

                if source_node.is_none() {
                    validation_errors.push(SignConfigValidationError::UnknownNode {
                        name: source_node_name.clone(),
                    });
                    return;
                }

                if destination_node.is_none() {
                    // Keep portal so it can be scanned, but still report validation error
                    validation_errors.push(SignConfigValidationError::UnknownNode {
                        name: destination_node_name.clone(),
                    });
                }

                let source_node = nodes.get_mut(source_node_name);

                source_node.unwrap().portal = Some(Portal {
                    destination_node_name: destination_node_name.clone(),
                    vec3: *effective_location,
                });
            }
            _ => {}
        });

        let mut complexes = HashMap::new();

        parsed_signs.iter().for_each(|sign| {
            if let ParsedSign::StorageComplex {
                dimension,
                y_level,
                bounds,
                name,
            } = sign
            {
                complexes.insert(
                    name.clone(),
                    StorageComplex::FlatFloor {
                        name: name.clone(),
                        dimension: *dimension,
                        y_level: *y_level,
                        bounds: *bounds,
                    },
                );
            }

            if let ParsedSign::StorageTower {
                dimension,
                origin,
                height,
                name,
            } = sign
            {
                complexes.insert(
                    name.clone(),
                    StorageComplex::Tower {
                        name: name.clone(),
                        dimension: *dimension,
                        origin: *origin,
                        height: *height,
                    },
                );
            }
        });

        CompiledSignConfig {
            nodes,
            complexes,
            sign_parse_errors,
            validation_errors,
        }
    }

    fn set_dirty(&self) {
        let mut cached_config = self.cached_config.lock().unwrap();

        cached_config.take();
    }

    pub fn get_config(&self) -> Arc<CompiledSignConfig> {
        let mut cached_config = self.cached_config.lock().unwrap();

        if cached_config.is_none() {
            cached_config.replace(Arc::new(self.generate_config()));
        }

        cached_config.as_ref().unwrap().clone()
    }
}
