package main

import (
	"fmt"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type Screen int

const (
	DeliveryScreen Screen = iota
	PickupScreen
	ConfigScreen
	StatsScreen
	HelpScreen
	AdminScreen
)

type App struct {
	currentScreen Screen
	width         int
	height        int
	
	// Screen models
	deliveryModel DeliveryModel
	pickupModel   PickupModel
	configModel   ConfigModel
	statsModel    StatsModel
	helpModel     HelpModel
	adminModel    AdminModel
}

func NewApp() *App {
	return &App{
		currentScreen: DeliveryScreen,
		deliveryModel: NewDeliveryModel(),
		pickupModel:   NewPickupModel(),
		configModel:   NewConfigModel(),
		statsModel:    NewStatsModel(),
		helpModel:     NewHelpModel(),
		adminModel:    NewAdminModel(),
	}
}

func (a *App) Init() tea.Cmd {
	// Set initial dimensions when available
	if a.width > 0 && a.height > 0 {
		a.deliveryModel.width = a.width
		a.deliveryModel.height = a.height - 6
	}
	return a.deliveryModel.Init()
}

func (a *App) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		a.width = msg.Width
		a.height = msg.Height
		
		// Update current screen dimensions
		switch a.currentScreen {
		case DeliveryScreen:
			a.deliveryModel.width = a.width
			a.deliveryModel.height = a.height - 6
		case PickupScreen:
			a.pickupModel.width = a.width
			a.pickupModel.height = a.height - 6
		case ConfigScreen:
			a.configModel.width = a.width
			a.configModel.height = a.height - 6
		case StatsScreen:
			a.statsModel.width = a.width
			a.statsModel.height = a.height - 6
		case HelpScreen:
			a.helpModel.width = a.width
			a.helpModel.height = a.height - 6
		case AdminScreen:
			a.adminModel.width = a.width
			a.adminModel.height = a.height - 6
		}
		return a, nil

	case tea.KeyMsg:
		switch msg.String() {
		case "q", "ctrl+c":
			return a, tea.Quit
		case "ctrl+d":
			a.currentScreen = DeliveryScreen
			a.deliveryModel.width = a.width
			a.deliveryModel.height = a.height - 6
			return a, a.deliveryModel.Init()
		case "ctrl+p":
			a.currentScreen = PickupScreen
			a.pickupModel.width = a.width
			a.pickupModel.height = a.height - 6
			return a, a.pickupModel.Init()
		case "ctrl+n":
			a.currentScreen = ConfigScreen
			a.configModel.width = a.width
			a.configModel.height = a.height - 6
			return a, a.configModel.Init()
		case "ctrl+s":
			a.currentScreen = StatsScreen
			a.statsModel.width = a.width
			a.statsModel.height = a.height - 6
			return a, a.statsModel.Init()
		case "ctrl+h":
			a.currentScreen = HelpScreen
			a.helpModel.width = a.width
			a.helpModel.height = a.height - 6
			return a, a.helpModel.Init()
		case "ctrl+m":
			a.currentScreen = AdminScreen
			a.adminModel.width = a.width
			a.adminModel.height = a.height - 6
			return a, a.adminModel.Init()
		}

	case ScreenChangeMsg:
		a.currentScreen = msg.Screen
		switch msg.Screen {
		case DeliveryScreen:
			a.deliveryModel.width = a.width
			a.deliveryModel.height = a.height - 6
			return a, a.deliveryModel.Init()
		case PickupScreen:
			a.pickupModel.width = a.width
			a.pickupModel.height = a.height - 6
			return a, a.pickupModel.Init()
		case ConfigScreen:
			a.configModel.width = a.width
			a.configModel.height = a.height - 6
			return a, a.configModel.Init()
		case StatsScreen:
			a.statsModel.width = a.width
			a.statsModel.height = a.height - 6
			return a, a.statsModel.Init()
		case HelpScreen:
			a.helpModel.width = a.width
			a.helpModel.height = a.height - 6
			return a, a.helpModel.Init()
		case AdminScreen:
			a.adminModel.width = a.width
			a.adminModel.height = a.height - 6
			return a, a.adminModel.Init()
		default:
			return a, nil
		}
	}

	// Pass messages to current screen
	var cmd tea.Cmd
	switch a.currentScreen {
	case DeliveryScreen:
		a.deliveryModel, cmd = a.deliveryModel.Update(msg)
	case PickupScreen:
		a.pickupModel, cmd = a.pickupModel.Update(msg)
	case ConfigScreen:
		a.configModel, cmd = a.configModel.Update(msg)
	case StatsScreen:
		a.statsModel, cmd = a.statsModel.Update(msg)
	case HelpScreen:
		a.helpModel, cmd = a.helpModel.Update(msg)
	case AdminScreen:
		a.adminModel, cmd = a.adminModel.Update(msg)
	}

	return a, cmd
}

func (a *App) View() string {
	headerStyle := lipgloss.NewStyle().
		Bold(true).
		Foreground(lipgloss.Color("205")).
		Align(lipgloss.Center).
		Width(a.width)

	header := headerStyle.Render("🗃️  Super Sorting System TUI")

	navigationStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("241")).
		Align(lipgloss.Center).
		Width(a.width)

	navigation := navigationStyle.Render("Ctrl+D: Delivery | Ctrl+P: Pickup | Ctrl+N: Config | Ctrl+S: Stats | Ctrl+H: Help | Ctrl+M: Admin | Q: Quit")

	var content string
	switch a.currentScreen {
	case DeliveryScreen:
		content = a.deliveryModel.View()
	case PickupScreen:
		content = a.pickupModel.View()
	case ConfigScreen:
		content = a.configModel.View()
	case StatsScreen:
		content = a.statsModel.View()
	case HelpScreen:
		content = a.helpModel.View()
	case AdminScreen:
		content = a.adminModel.View()
	}

	return fmt.Sprintf("%s\n%s\n\n%s", header, navigation, content)
}

type ScreenChangeMsg struct {
	Screen Screen
}

