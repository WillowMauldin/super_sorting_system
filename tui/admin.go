package main

import (
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type AdminModel struct {
	message string
	width   int
	height  int
}

func NewAdminModel() AdminModel {
	return AdminModel{
		message: "Admin functions - Coming soon!",
	}
}

func (m AdminModel) Init() tea.Cmd {
	return nil
}

func (m AdminModel) Update(msg tea.Msg) (AdminModel, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height - 6
		return m, nil
	}

	return m, nil
}

func (m AdminModel) View() string {
	style := lipgloss.NewStyle().Padding(1)

	content := "Admin Functions\n\n"
	content += "This screen will provide:\n"
	content += "• Agent management\n"
	content += "• System monitoring\n"
	content += "• Operation queue management\n"
	content += "• Alert management\n"
	content += "• System maintenance tools\n\n"
	content += "Implementation coming soon..."

	return style.Render(content)
}