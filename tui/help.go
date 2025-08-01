package main

import (
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type HelpModel struct {
	width  int
	height int
}

func NewHelpModel() HelpModel {
	return HelpModel{}
}

func (m HelpModel) Init() tea.Cmd {
	return nil
}

func (m HelpModel) Update(msg tea.Msg) (HelpModel, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height - 6
		return m, nil
	}

	return m, nil
}

func (m HelpModel) View() string {
	style := lipgloss.NewStyle().Padding(1)

	content := "Super Sorting System TUI Help\n\n"
	
	content += "Global Shortcuts:\n"
	content += "• Ctrl+D: Go to Delivery screen\n"
	content += "• Ctrl+P: Go to Pickup screen\n"
	content += "• Ctrl+N: Go to Configuration screen\n"
	content += "• Ctrl+S: Go to Statistics screen\n"
	content += "• Ctrl+H: Go to Help screen\n"
	content += "• Ctrl+M: Go to Admin screen\n"
	content += "• Esc: Return to main menu\n"
	content += "• Q or Ctrl+C: Quit application\n\n"

	content += "Delivery Screen:\n"
	content += "• Enter: Select an item\n"
	content += "• Tab: Switch between item/shulker mode when entering quantity\n"
	content += "• D: Proceed to destination selection (when items are selected)\n"
	content += "• C: Clear selected items\n\n"

	content += "Pickup Screen:\n"
	content += "• Enter: Select pickup location\n"
	content += "• R: Toggle 'repeat until empty' mode\n\n"

	content += "Configuration Screen:\n"
	content += "• R: Refresh configuration\n\n"

	content += "Environment Variables:\n"
	content += "• API_BASE_URL: Base URL for the operator API (default: http://localhost:8080)\n"
	content += "• API_KEY: API key for authentication (default: default-api-key)\n"

	return style.Render(content)
}