package main

import (
	"fmt"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type ConfigModel struct {
	config    *SignConfigResponse
	message   string
	apiClient *APIClient
	width     int
	height    int
}

func NewConfigModel() ConfigModel {
	return ConfigModel{
		apiClient: NewAPIClient(),
		message:   "Loading configuration...",
	}
}

func (m ConfigModel) Init() tea.Cmd {
	return m.loadConfig()
}

func (m ConfigModel) loadConfig() tea.Cmd {
	return func() tea.Msg {
		config, err := m.apiClient.GetSignConfig()
		if err != nil {
			return ConfigLoadedMsg{Error: err}
		}
		return ConfigLoadedMsg{Config: config}
	}
}

func (m ConfigModel) Update(msg tea.Msg) (ConfigModel, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height - 6
		return m, nil

	case ConfigLoadedMsg:
		if msg.Error != nil {
			m.message = fmt.Sprintf("Error loading configuration: %v", msg.Error)
			return m, nil
		}
		m.config = msg.Config
		m.message = ""
		return m, nil

	case tea.KeyMsg:
		switch msg.String() {
		case "r":
			return m, m.loadConfig()
		}
	}

	return m, nil
}

func (m ConfigModel) View() string {
	style := lipgloss.NewStyle().Padding(1)

	if m.config == nil {
		if m.message != "" {
			return style.Render(m.message + "\n\nPress 'r' to retry")
		}
		return style.Render("Loading configuration...")
	}

	content := "System Configuration\n\n"
	content += fmt.Sprintf("Number of nodes: %d\n\n", len(m.config.Nodes))

	content += "Nodes:\n"
	for name, node := range m.config.Nodes {
		content += fmt.Sprintf("• %s\n", name)
		content += fmt.Sprintf("  Location: (%d, %d, %d) in %s\n", 
			node.Location.Vec3.X, node.Location.Vec3.Y, node.Location.Vec3.Z, node.Location.Dim)
		
		if node.Pickup != nil {
			content += fmt.Sprintf("  Pickup: (%d, %d, %d)\n", 
				node.Pickup.X, node.Pickup.Y, node.Pickup.Z)
		}
		
		if node.Dropoff != nil {
			content += fmt.Sprintf("  Dropoff: (%d, %d, %d)\n", 
				node.Dropoff.X, node.Dropoff.Y, node.Dropoff.Z)
		}
		content += "\n"
	}

	content += "Press 'r' to refresh configuration"

	if m.message != "" {
		content += "\n\n" + m.message
	}

	return style.Render(content)
}

type ConfigLoadedMsg struct {
	Config *SignConfigResponse
	Error  error
}