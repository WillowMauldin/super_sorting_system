# Super Sorting System TUI

A Terminal User Interface (TUI) for the Super Sorting System, built with Go and the Bubbletea framework. This provides a command-line alternative to the web-based gradient frontend.

## Features

- **Delivery**: Select items and deliver them to specified destinations
- **Pickup**: Pick up items from designated pickup locations  
- **Configuration**: View system configuration and node information
- **Statistics**: View system metrics and performance data (coming soon)
- **Admin**: Administrative functions and system management (coming soon)
- **Help**: Built-in help and keyboard shortcuts

## Installation

1. Ensure you have Go 1.19+ installed
2. Clone the repository and navigate to the `tui` directory
3. Build the application:
   ```bash
   go build
   ```

## Usage

### Running the TUI

```bash
./super-sorting-system-tui
```

### Environment Variables

- `API_BASE_URL`: Base URL for the operator API (default: `http://localhost:8080`)
- `API_KEY`: API key for authentication (default: `default-api-key`)

Example:
```bash
export API_BASE_URL="http://your-operator-server:8080"
export API_KEY="your-api-key"
./super-sorting-system-tui
```

### Global Keyboard Shortcuts

- `Ctrl+D`: Go to Delivery screen
- `Ctrl+P`: Go to Pickup screen  
- `Ctrl+N`: Go to Configuration screen
- `Ctrl+S`: Go to Statistics screen
- `Ctrl+H`: Go to Help screen
- `Ctrl+M`: Go to Admin screen
- `Esc`: Return to main menu
- `Q` or `Ctrl+C`: Quit application

### Delivery Screen

1. Navigate through the item list using arrow keys
2. Press `Enter` to select an item
3. Enter the quantity (use `Tab` to switch between item/shulker mode)
4. Press `d` when items are selected to choose destination
5. Confirm delivery

**Shortcuts:**
- `Enter`: Select an item
- `Tab`: Switch between item/shulker mode when entering quantity
- `d`: Proceed to destination selection (when items are selected)
- `c`: Clear selected items

### Pickup Screen

1. Select a pickup location from the list
2. Toggle "repeat until empty" mode if desired (`r` key)
3. Confirm pickup operation

**Shortcuts:**
- `Enter`: Select pickup location
- `r`: Toggle 'repeat until empty' mode

### Configuration Screen

View system configuration including:
- Number of configured nodes
- Node locations and capabilities
- Pickup and dropoff points

**Shortcuts:**
- `r`: Refresh configuration

## Architecture

The TUI is built using the Elm architecture pattern with Bubbletea:

- **Models**: Each screen has its own model that maintains state
- **Messages**: Commands and events are handled as messages
- **Views**: Each model renders its current state
- **Updates**: Models process messages and return updated state

### Key Components

- `app.go`: Main application model and routing
- `menu.go`: Main menu screen
- `delivery.go`: Item delivery functionality
- `pickup.go`: Item pickup functionality  
- `config.go`: Configuration display
- `api.go`: HTTP client for operator API
- `stats.go`, `admin.go`, `help.go`: Additional screens

## API Integration

The TUI communicates with the operator backend using the same REST APIs as the gradient web frontend:

- `/data/items`: Get available items
- `/automation/sign_config`: Get node configuration
- `/automation/hold`: Create and release item holds
- `/automation/operation`: Execute operations

## Development

### Building from Source

```bash
go mod tidy
go build
```

### Adding New Features

1. Create new model files for additional screens
2. Add screen constants to `app.go`
3. Register new screens in the main application model
4. Implement the required interface methods (`Init`, `Update`, `View`)

### Dependencies

- [Bubbletea](https://github.com/charmbracelet/bubbletea): TUI framework
- [Bubbles](https://github.com/charmbracelet/bubbles): TUI components
- [Lipgloss](https://github.com/charmbracelet/lipgloss): Styling and layout

## Comparison with Web Frontend

| Feature | Web Frontend (gradient) | TUI |
|---------|------------------------|-----|
| Item Selection | Visual grid with images | List-based navigation |
| Navigation | Mouse + keyboard | Keyboard only |
| Styling | CSS with styled-components | Terminal colors and layout |
| Deployment | Web server required | Single binary |
| Resource Usage | Browser + web server | Minimal terminal app |
| Accessibility | Screen readers via web standards | Terminal screen readers |

## Troubleshooting

### Connection Issues

- Verify the operator is running and accessible
- Check `API_BASE_URL` environment variable
- Ensure correct `API_KEY` is set

### Display Issues

- Ensure terminal supports UTF-8 for icons
- Try resizing terminal if layout appears broken
- Some terminals may not support all styling features

### Build Issues

- Ensure Go 1.19+ is installed
- Run `go mod tidy` to resolve dependencies
- Check that all required modules are available