package main

import (
	"fmt"

	"github.com/charmbracelet/bubbles/list"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type PickupState int

const (
	PickupLocationSelectionState PickupState = iota
	PickupConfirmationState
)

type PickupModel struct {
	state            PickupState
	locationList     list.Model
	locations        []string
	repeatUntilEmpty bool
	message          string
	apiClient        *APIClient
	width            int
	height           int
}

type pickupLocationEntry struct {
	name string
}

func (p pickupLocationEntry) Title() string       { return p.name }
func (p pickupLocationEntry) Description() string { return "Pickup location" }
func (p pickupLocationEntry) FilterValue() string { return p.name }

func NewPickupModel() PickupModel {
	return PickupModel{
		state:     PickupLocationSelectionState,
		apiClient: NewAPIClient(),
		message:   "Loading pickup locations...",
	}
}

func (m PickupModel) Init() tea.Cmd {
	return m.loadPickupLocations()
}

func (m PickupModel) loadPickupLocations() tea.Cmd {
	return func() tea.Msg {
		config, err := m.apiClient.GetSignConfig()
		if err != nil {
			return PickupLocationsLoadedMsg{Error: err}
		}

		var locations []string
		for name, node := range config.Nodes {
			if node.Pickup != nil {
				locations = append(locations, name)
			}
		}
		return PickupLocationsLoadedMsg{Locations: locations}
	}
}

func (m PickupModel) Update(msg tea.Msg) (PickupModel, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height - 6

		if m.locationList.Items() != nil {
			m.locationList.SetWidth(msg.Width)
			m.locationList.SetHeight(m.height)
		}
		return m, nil

	case PickupLocationsLoadedMsg:
		if msg.Error != nil {
			m.message = fmt.Sprintf("Error loading pickup locations: %v", msg.Error)
			return m, nil
		}

		m.locations = msg.Locations
		items := make([]list.Item, len(msg.Locations))
		for i, location := range msg.Locations {
			items[i] = pickupLocationEntry{name: location}
		}

		// Use reasonable defaults if we don't have window size yet
		width := m.width
		height := m.height - 6
		if width <= 0 {
			width = 100
		}
		if height <= 0 {
			height = 30
		}

		m.locationList = list.New(items, list.NewDefaultDelegate(), width, height)
		m.locationList.Title = "Select Pickup Location"
		m.locationList.SetShowStatusBar(false)
		m.message = ""
		
		return m, nil

	case PickupCompleteMsg:
		if msg.Error != nil {
			m.message = fmt.Sprintf("Pickup failed: %v", msg.Error)
		} else {
			m.message = "Pickup completed successfully!"
		}
		m.state = PickupLocationSelectionState
		return m, nil

	case tea.KeyMsg:
		switch m.state {
		case PickupLocationSelectionState:
			// If filtering is active, let the list handle all keypresses
			if m.locationList.FilterState() == list.Filtering {
				break // Fall through to list update at bottom
			}
			
			// Handle custom keys only when not filtering
			switch msg.String() {
			case "enter":
				if location, ok := m.locationList.SelectedItem().(pickupLocationEntry); ok {
					m.state = PickupConfirmationState
					m.message = fmt.Sprintf("Ready to pickup from: %s", location.name)
					return m, nil
				}
			case "r":
				m.repeatUntilEmpty = !m.repeatUntilEmpty
				repeatMsg := "disabled"
				if m.repeatUntilEmpty {
					repeatMsg = "enabled"
				}
				m.message = fmt.Sprintf("Repeat until empty: %s", repeatMsg)
				return m, nil
			}

		case PickupConfirmationState:
			switch msg.String() {
			case "y", "enter":
				if location, ok := m.locationList.SelectedItem().(pickupLocationEntry); ok {
					return m, m.pickupItems(location.name)
				}
			case "n", "esc":
				m.state = PickupLocationSelectionState
				return m, nil
			case "r":
				m.repeatUntilEmpty = !m.repeatUntilEmpty
				repeatMsg := "disabled"
				if m.repeatUntilEmpty {
					repeatMsg = "enabled"
				}
				m.message = fmt.Sprintf("Repeat until empty: %s", repeatMsg)
				return m, nil
			}
		}
	}

	var cmd tea.Cmd
	switch m.state {
	case PickupLocationSelectionState:
		if m.locationList.Items() != nil {
			m.locationList, cmd = m.locationList.Update(msg)
		}
	}

	return m, cmd
}

func (m PickupModel) pickupItems(location string) tea.Cmd {
	return func() tea.Msg {
		err := m.apiClient.PickupItems(location, m.repeatUntilEmpty)
		return PickupCompleteMsg{Error: err}
	}
}

func (m PickupModel) View() string {
	if m.message != "" && m.locations == nil {
		return m.message
	}

	style := lipgloss.NewStyle().Padding(1)

	switch m.state {
	case PickupLocationSelectionState:
		content := m.locationList.View()
		
		repeatStatus := "disabled"
		if m.repeatUntilEmpty {
			repeatStatus = "enabled"
		}
		content += fmt.Sprintf("\n\nRepeat until empty: %s", repeatStatus)
		content += "\nPress 'r' to toggle repeat mode, Enter to select location"

		if m.message != "" {
			content += "\n\n" + m.message
		}

		return style.Render(content)

	case PickupConfirmationState:
		content := "Confirm Pickup\n\n"
		
		if location, ok := m.locationList.SelectedItem().(pickupLocationEntry); ok {
			content += fmt.Sprintf("Location: %s\n", location.name)
		}

		repeatStatus := "No"
		if m.repeatUntilEmpty {
			repeatStatus = "Yes"
		}
		content += fmt.Sprintf("Repeat until empty: %s\n", repeatStatus)
		
		content += "\nPress 'y' to confirm, 'n' or Esc to cancel, 'r' to toggle repeat mode"

		if m.message != "" {
			content += "\n\n" + m.message
		}

		return style.Render(content)
	}

	return style.Render("Pickup Screen")
}

type PickupLocationsLoadedMsg struct {
	Locations []string
	Error     error
}

type PickupCompleteMsg struct {
	Error error
}