package main

import (
	"fmt"
	"log"
	"sort"
	"strconv"
	"strings"

	"github.com/charmbracelet/bubbles/list"
	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type DeliveryState int

const (
	ItemSelectionState DeliveryState = iota
	QuantityInputState
	DestinationSelectionState
	ConfirmationState
)

type DeliveryModel struct {
	state            DeliveryState
	items            []InventoryItem
	selectedItems    []DeliveryItem
	itemList         list.Model
	quantityInput    textinput.Model
	destinationList  list.Model
	destinations     []string
	currentItem      *InventoryItem
	isShulkerMode    bool
	message          string
	apiClient        *APIClient
	width            int
	height           int
}

type deliveryItemEntry struct {
	item InventoryItem
}

func (i deliveryItemEntry) Title() string { return i.item.DisplayName }
func (i deliveryItemEntry) Description() string {
	return fmt.Sprintf("Available: %s, Key: %s", formatNumber(i.item.Count), i.item.ItemType.Key)
}
func (i deliveryItemEntry) FilterValue() string { return i.item.DisplayName }

// formatNumber adds commas to numbers for better readability
func formatNumber(n int) string {
	str := strconv.Itoa(n)
	if len(str) <= 3 {
		return str
	}
	
	var result strings.Builder
	for i, digit := range str {
		if i > 0 && (len(str)-i)%3 == 0 {
			result.WriteString(",")
		}
		result.WriteRune(digit)
	}
	return result.String()
}

type destinationEntry struct {
	name string
}

func (d destinationEntry) Title() string       { return d.name }
func (d destinationEntry) Description() string { return "Delivery destination" }
func (d destinationEntry) FilterValue() string { return d.name }

func NewDeliveryModel() DeliveryModel {
	ti := textinput.New()
	ti.Placeholder = "Enter quantity..."
	ti.CharLimit = 10
	ti.Width = 20

	return DeliveryModel{
		state:         ItemSelectionState,
		apiClient:     NewAPIClient(),
		quantityInput: ti,
		message:       "Loading items...",
	}
}

func (m DeliveryModel) Init() tea.Cmd {
	return tea.Batch(
		m.loadItems(),
		m.loadDestinations(),
	)
}

func (m DeliveryModel) loadItems() tea.Cmd {
	return func() tea.Msg {
		items, err := m.apiClient.GetAvailableItems()
		if err != nil {
			return ItemsLoadedMsg{Error: err}
		}
		return ItemsLoadedMsg{Items: items}
	}
}

func (m DeliveryModel) loadDestinations() tea.Cmd {
	return func() tea.Msg {
		config, err := m.apiClient.GetSignConfig()
		if err != nil {
			return DestinationsLoadedMsg{Error: err}
		}

		var destinations []string
		if m.apiClient.debug {
			log.Printf("DEBUG: Processing %d nodes from sign config", len(config.Nodes))
		}
		
		for name, node := range config.Nodes {
			if m.apiClient.debug {
				log.Printf("DEBUG: Node '%s': dropoff=%v, pickup=%v", name, node.Dropoff != nil, node.Pickup != nil)
			}
			if node.Dropoff != nil {
				destinations = append(destinations, name)
				if m.apiClient.debug {
					log.Printf("DEBUG: Added '%s' as delivery destination", name)
				}
			}
		}

		if m.apiClient.debug {
			log.Printf("DEBUG: Found %d delivery destinations", len(destinations))
			if len(destinations) > 0 {
				log.Printf("DEBUG: First destination: %s", destinations[0])
			}
		}

		return DestinationsLoadedMsg{Destinations: destinations}
	}
}

func (m DeliveryModel) Update(msg tea.Msg) (DeliveryModel, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height - 6

		if m.itemList.Items() != nil {
			m.itemList.SetWidth(msg.Width)
			m.itemList.SetHeight(m.height)
		}
		if m.destinationList.Items() != nil {
			m.destinationList.SetWidth(msg.Width)
			m.destinationList.SetHeight(m.height)
		}
		return m, nil

	case ItemsLoadedMsg:
		if msg.Error != nil {
			m.message = fmt.Sprintf("Error loading items: %v", msg.Error)
			return m, nil
		}

		m.items = msg.Items
		
		// Sort items by quantity (descending) so items with most stock appear first
		sort.Slice(msg.Items, func(i, j int) bool {
			return msg.Items[i].Count > msg.Items[j].Count
		})
		
		items := make([]list.Item, len(msg.Items))
		for i, item := range msg.Items {
			items[i] = deliveryItemEntry{item: item}
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

		m.itemList = list.New(items, list.NewDefaultDelegate(), width, height)
		m.itemList.Title = "Select Items for Delivery"
		m.itemList.SetShowStatusBar(false)
		m.message = ""
		
		return m, nil

	case DestinationsLoadedMsg:
		if msg.Error != nil {
			return m, nil
		}

		m.destinations = msg.Destinations
		items := make([]list.Item, len(msg.Destinations))
		for i, dest := range msg.Destinations {
			items[i] = destinationEntry{name: dest}
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

		m.destinationList = list.New(items, list.NewDefaultDelegate(), width, height)
		m.destinationList.Title = "Select Destination"
		m.destinationList.SetShowStatusBar(false)
		
		return m, nil

	case DeliveryCompleteMsg:
		if msg.Error != nil {
			m.message = fmt.Sprintf("Delivery failed: %v", msg.Error)
		} else {
			m.message = "Delivery completed successfully!"
			m.selectedItems = []DeliveryItem{}
			m.state = ItemSelectionState
		}
		return m, nil

	case tea.KeyMsg:
		switch m.state {
		case ItemSelectionState:
			// If filtering is active, let the list handle all keypresses
			if m.itemList.FilterState() == list.Filtering {
				break // Fall through to list update at bottom
			}
			
			// Handle custom keys only when not filtering
			switch msg.String() {
			case "enter":
				if item, ok := m.itemList.SelectedItem().(deliveryItemEntry); ok {
					m.currentItem = &item.item
					m.quantityInput.SetValue("")
					m.quantityInput.Focus()
					m.state = QuantityInputState
					return m, textinput.Blink
				}
			case "d":
				if len(m.selectedItems) > 0 {
					m.state = DestinationSelectionState
					return m, nil
				}
			case "c":
				m.selectedItems = []DeliveryItem{}
				m.message = "Selection cleared"
				return m, nil
			}

		case QuantityInputState:
			switch msg.String() {
			case "enter":
				quantity, err := strconv.Atoi(m.quantityInput.Value())
				if err != nil || quantity <= 0 {
					m.message = "Please enter a valid positive number"
					return m, nil
				}

				deliveryItem := DeliveryItem{
					Item: Item{
						ItemID:        m.currentItem.ItemType.RawID,
						StackSize:     m.currentItem.ItemType.MaxCount,
						StackableHash: m.currentItem.InventoryData.StackableHash,
					},
					DisplayName: m.currentItem.DisplayName,
				}

				if m.isShulkerMode {
					deliveryItem.ShulkerCount = quantity
				} else {
					deliveryItem.ItemCount = quantity
				}

				m.selectedItems = append(m.selectedItems, deliveryItem)
				m.message = fmt.Sprintf("Added %s x%d to delivery", m.currentItem.DisplayName, quantity)
				m.state = ItemSelectionState
				return m, nil

			case "esc":
				m.state = ItemSelectionState
				return m, nil

			case "tab":
				m.isShulkerMode = !m.isShulkerMode
				placeholder := "Enter item quantity..."
				if m.isShulkerMode {
					placeholder = "Enter shulker quantity..."
				}
				m.quantityInput.Placeholder = placeholder
				return m, nil
			}

		case DestinationSelectionState:
			// If filtering is active, let the list handle all keypresses
			if m.destinationList.FilterState() == list.Filtering {
				break // Fall through to list update at bottom
			}
			
			// Handle custom keys only when not filtering
			switch msg.String() {
			case "enter":
				if dest, ok := m.destinationList.SelectedItem().(destinationEntry); ok {
					m.state = ConfirmationState
					m.message = fmt.Sprintf("Ready to deliver to: %s", dest.name)
					return m, nil
				}
			case "esc":
				m.state = ItemSelectionState
				return m, nil
			}

		case ConfirmationState:
			switch msg.String() {
			case "y", "enter":
				if dest, ok := m.destinationList.SelectedItem().(destinationEntry); ok {
					return m, m.deliverItems(dest.name)
				}
			case "n", "esc":
				m.state = ItemSelectionState
				return m, nil
			}
		}
	}

	var cmd tea.Cmd
	switch m.state {
	case ItemSelectionState:
		if m.itemList.Items() != nil {
			m.itemList, cmd = m.itemList.Update(msg)
		}
	case QuantityInputState:
		m.quantityInput, cmd = m.quantityInput.Update(msg)
	case DestinationSelectionState:
		if m.destinationList.Items() != nil {
			m.destinationList, cmd = m.destinationList.Update(msg)
		}
	}

	return m, cmd
}

func (m DeliveryModel) deliverItems(destination string) tea.Cmd {
	return func() tea.Msg {
		err := m.apiClient.DeliverItems(destination, m.selectedItems)
		return DeliveryCompleteMsg{Error: err}
	}
}

func (m DeliveryModel) View() string {
	if m.message != "" && m.items == nil {
		return m.message
	}

	style := lipgloss.NewStyle().Padding(1)

	switch m.state {
	case ItemSelectionState:
		content := m.itemList.View()
		
		if len(m.selectedItems) > 0 {
			content += "\n\nSelected Items:\n"
			for _, item := range m.selectedItems {
				if item.ShulkerCount > 0 {
					content += fmt.Sprintf("• %s x%d shulkers\n", item.DisplayName, item.ShulkerCount)
				} else {
					content += fmt.Sprintf("• %s x%d items\n", item.DisplayName, item.ItemCount)
				}
			}
			content += "\nPress 'd' to select destination, 'c' to clear selection"
		} else {
			content += "\n\nPress Enter to select an item"
		}

		if m.message != "" {
			content += "\n\n" + m.message
		}

		return style.Render(content)

	case QuantityInputState:
		mode := "items"
		if m.isShulkerMode {
			mode = "shulkers"
		}

		content := fmt.Sprintf("Selected: %s\n\n", m.currentItem.DisplayName)
		content += fmt.Sprintf("Enter quantity of %s:\n", mode)
		content += m.quantityInput.View()
		content += "\n\nPress Tab to switch between items/shulkers, Esc to cancel"

		if m.message != "" {
			content += "\n\n" + m.message
		}

		return style.Render(content)

	case DestinationSelectionState:
		content := m.destinationList.View()
		content += "\n\nPress Enter to select destination, Esc to go back"

		if m.message != "" {
			content += "\n\n" + m.message
		}

		return style.Render(content)

	case ConfirmationState:
		content := "Confirm Delivery\n\n"
		content += "Items to deliver:\n"
		for _, item := range m.selectedItems {
			if item.ShulkerCount > 0 {
				content += fmt.Sprintf("• %s x%d shulkers\n", item.DisplayName, item.ShulkerCount)
			} else {
				content += fmt.Sprintf("• %s x%d items\n", item.DisplayName, item.ItemCount)
			}
		}

		if dest, ok := m.destinationList.SelectedItem().(destinationEntry); ok {
			content += fmt.Sprintf("\nDestination: %s\n", dest.name)
		}

		content += "\nPress 'y' to confirm, 'n' or Esc to cancel"

		if m.message != "" {
			content += "\n\n" + m.message
		}

		return style.Render(content)
	}

	return style.Render("Delivery Screen")
}

type ItemsLoadedMsg struct {
	Items []InventoryItem
	Error error
}

type DestinationsLoadedMsg struct {
	Destinations []string
	Error        error
}

type DeliveryCompleteMsg struct {
	Error error
}