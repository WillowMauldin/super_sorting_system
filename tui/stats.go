package main

import (
	"fmt"
	"sort"
	"time"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type StatsModel struct {
	stats     *Stats
	message   string
	apiClient *APIClient
	width     int
	height    int
}

func NewStatsModel() StatsModel {
	return StatsModel{
		apiClient: NewAPIClient(),
		message:   "Loading statistics...",
	}
}

func (m StatsModel) Init() tea.Cmd {
	return tea.Batch(
		m.loadStats(),
		tea.Tick(time.Second*3, func(t time.Time) tea.Msg {
			return RefreshStatsMsg{}
		}),
	)
}

func (m StatsModel) loadStats() tea.Cmd {
	return func() tea.Msg {
		stats, err := m.apiClient.GetStats()
		if err != nil {
			return StatsLoadedMsg{Error: err}
		}
		return StatsLoadedMsg{Stats: stats}
	}
}

func (m StatsModel) Update(msg tea.Msg) (StatsModel, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height - 6
		return m, nil

	case StatsLoadedMsg:
		if msg.Error != nil {
			m.message = fmt.Sprintf("Error loading stats: %v", msg.Error)
		} else {
			m.stats = msg.Stats
			m.message = ""
		}
		return m, nil

	case RefreshStatsMsg:
		// Auto-refresh every 3 seconds
		return m, tea.Batch(
			m.loadStats(),
			tea.Tick(time.Second*3, func(t time.Time) tea.Msg {
				return RefreshStatsMsg{}
			}),
		)
	}

	return m, nil
}

func (m StatsModel) View() string {
	if m.stats == nil {
		return m.message
	}

	style := lipgloss.NewStyle().Padding(1)

	content := "📊 System Statistics\n\n"

	// Operations section
	content += "🔄 Operations\n"
	content += fmt.Sprintf("  Pending: %d\n", m.stats.OperationsPending)
	content += fmt.Sprintf("  In Progress: %d\n", m.stats.OperationsInProgress)
	content += fmt.Sprintf("  Complete: %d\n", m.stats.OperationsComplete)
	content += fmt.Sprintf("  Aborted: %d\n\n", m.stats.OperationsAborted)

	// Storage section
	content += "🗃️  Storage\n"
	content += fmt.Sprintf("  Inventories Loaded: %d\n", m.stats.InventoriesInMem)
	content += fmt.Sprintf("  Slots Free: %d\n", m.stats.FreeSlots)
	content += fmt.Sprintf("  Total Slots: %d\n", m.stats.TotalSlots)
	
	// Calculate usage percentage
	if m.stats.TotalSlots > 0 {
		usagePercent := float64(m.stats.TotalSlots-m.stats.FreeSlots) / float64(m.stats.TotalSlots) * 100
		content += fmt.Sprintf("  Usage: %.1f%%\n\n", usagePercent)
	} else {
		content += "\n"
	}

	// System section
	content += "⚙️  System\n"
	content += fmt.Sprintf("  Agents Connected: %d\n", m.stats.AgentsConnected)
	content += fmt.Sprintf("  Slot Holds: %d\n\n", m.stats.CurrentHolds)

	// Service timing section
	if len(m.stats.ServicesTickTimesMicros) > 0 {
		content += "⏱️  Service Performance\n"
		
		// Sort services by execution time (descending)
		type serviceTime struct {
			name  string
			micros int
		}
		var times []serviceTime
		for name, micros := range m.stats.ServicesTickTimesMicros {
			times = append(times, serviceTime{name: name, micros: micros})
		}
		sort.Slice(times, func(i, j int) bool {
			return times[i].micros > times[j].micros
		})

		for _, st := range times {
			ms := float64(st.micros) / 1000.0
			content += fmt.Sprintf("  %s: %.1fms\n", st.name, ms)
		}
		content += "\n"
	}

	content += "Press 'r' to refresh manually"

	if m.message != "" {
		content += "\n\n" + m.message
	}

	return style.Render(content)
}

type StatsLoadedMsg struct {
	Stats *Stats
	Error error
}

type RefreshStatsMsg struct {}