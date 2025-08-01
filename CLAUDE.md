# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Super Sorting System is a multi-component system that creates an item management system for Vanilla Minecraft, similar to Applied Energistics or Refined Storage. The system consists of four main components that work together:

## Architecture

### Components

1. **operator/** - Rust-based HTTP API server that manages the system state and coordinates operations
   - Main entry point: `operator/src/main.rs`
   - Provides REST APIs for agent management, data services, automation, and admin functions
   - Runs background services for inventory scanning, pathfinding, agent expiration, etc.
   - Uses Actix-web framework with API key authentication

2. **agent2/** - Java Minecraft bot client that connects to Minecraft servers
   - Main entry point: `agent2/app/src/main/java/me/mauldin/super_sorting_system/McAgent.java`
   - Uses Minecraft protocol library to connect as a bot
   - Handles inventory operations, movement, and world interactions
   - Communicates with operator via HTTP API

3. **gradient/** - React TypeScript web frontend for user interaction
   - Main entry point: `gradient/src/App.tsx`
   - Provides screens for delivery, pickup, configuration, statistics, and admin functions
   - Built with React, styled-components, and react-query
   - Communicates with operator backend

4. **exporter/** - Fabric mod for extracting Minecraft item data
   - Main entry point: `exporter/src/main/java/me/mauldin/super_sorting_system/exporter/Exporter.java`
   - Exports item registry data to JSON for use by other components
   - Runs as a Minecraft Fabric mod

5. **test/** - Integration test suite
   - Rust-based test runner that coordinates Minecraft server and system components
   - Includes setup for Spigot server with WorldEdit plugin

## Development Commands

### Rust Components (operator, test)
- Build: `cargo build`
- Run: `cargo run`
- Test: `cargo test`
- Lint/Format: `cargo clippy` and `cargo fmt`

### Java Components (agent2, exporter)
- Build: `./gradlew build`
- Format code: `./gradlew spotlessApply`
- Run agent2: `./gradlew run`
- Run exporter server: `./gradlew runServer` (from exporter directory)

### Frontend (gradient)
- Install dependencies: `npm install`
- Development server: `npm start`
- Build for production: `npm run build`
- Test: `npm test`
- Deploy: `make all` (builds and deploys to /usr/share/caddy/gradient)

### Test Suite
- Run tests: `cargo run` (from test directory)
- Setup required - see `test/SETUP.md` for Spigot and plugin installation

## Key Architectural Patterns

- **State Management**: Operator maintains global system state with background services running on 1-second intervals
- **Agent Communication**: Java agents register with operator and receive commands via HTTP polling
- **Pathfinding**: Custom pathfinding system for navigating Minecraft world between storage nodes
- **Operation Queue**: Asynchronous operation system for complex multi-step tasks (loading/unloading shulkers, item transfers)
- **Inventory Tracking**: Real-time tracking of item counts across storage locations using Minecraft signs
- **Authentication**: UUID-based API keys for securing operator endpoints

## Important Files

- `operator/src/state/`: Core state management modules
- `operator/src/services/`: Background services (inventory scanning, pathfinding, etc.)
- `operator/src/api/`: HTTP API endpoint definitions
- `agent2/app/src/main/java/me/mauldin/super_sorting_system/bot/`: Minecraft bot implementation
- `gradient/src/api/`: Frontend API client code
- `gradient/src/screens/`: Main UI screens

## Configuration

- Operator config: Environment variables and TOML files via figment crate
- Agent config: Java properties and command line arguments
- Frontend config: React environment variables