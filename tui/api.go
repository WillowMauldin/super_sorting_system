package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"time"
)

type APIClient struct {
	baseURL string
	apiKey  string
	client  *http.Client
	debug   bool
}

func NewAPIClient() *APIClient {
	baseURL := os.Getenv("API_BASE_URL")
	if baseURL == "" {
		baseURL = "http://localhost:8080"
	}

	apiKey := os.Getenv("API_KEY")
	if apiKey == "" {
		apiKey = "default-api-key"
	}

	debug := os.Getenv("DEBUG") != ""

	// Set up debug logging to file if debug is enabled
	if debug {
		logFile, err := os.OpenFile("tui-debug.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
		if err == nil {
			log.SetOutput(logFile)
			log.Printf("=== TUI Debug Session Started ===")
			log.Printf("Base URL: %s", baseURL)
			log.Printf("API Key: %s", apiKey)
		}
	}

	return &APIClient{
		baseURL: baseURL,
		apiKey:  apiKey,
		debug:   debug,
		client: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (c *APIClient) makeRequest(method, endpoint string, body interface{}) ([]byte, error) {
	url := c.baseURL + endpoint

	if c.debug {
		log.Printf("DEBUG: Making %s request to %s", method, url)
	}

	var reqBody io.Reader
	if body != nil {
		jsonBody, err := json.Marshal(body)
		if err != nil {
			if c.debug {
				log.Printf("DEBUG: Failed to marshal request body: %v", err)
			}
			return nil, fmt.Errorf("failed to marshal request body: %w", err)
		}
		reqBody = bytes.NewBuffer(jsonBody)
		if c.debug {
			log.Printf("DEBUG: Request body: %s", string(jsonBody))
		}
	}

	req, err := http.NewRequest(method, url, reqBody)
	if err != nil {
		if c.debug {
			log.Printf("DEBUG: Failed to create request: %v", err)
		}
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("X-Api-Key", c.apiKey)
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}

	if c.debug {
		log.Printf("DEBUG: Request headers: %v", req.Header)
	}

	resp, err := c.client.Do(req)
	if err != nil {
		if c.debug {
			log.Printf("DEBUG: Request failed: %v", err)
		}
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	if c.debug {
		log.Printf("DEBUG: Response status: %d", resp.StatusCode)
		log.Printf("DEBUG: Response headers: %v", resp.Header)
	}

	responseBody, err := io.ReadAll(resp.Body)
	if err != nil {
		if c.debug {
			log.Printf("DEBUG: Failed to read response: %v", err)
		}
		return nil, fmt.Errorf("failed to read response: %w", err)
	}

	if c.debug {
		log.Printf("DEBUG: Response body length: %d bytes", len(responseBody))
		if len(responseBody) < 1000 {
			log.Printf("DEBUG: Response body: %s", string(responseBody))
		} else {
			log.Printf("DEBUG: Response body (first 500 chars): %s...", string(responseBody[:500]))
		}
	}

	if resp.StatusCode >= 400 {
		if c.debug {
			log.Printf("DEBUG: API error response: %s", string(responseBody))
		}
		return nil, fmt.Errorf("API error (status %d): %s", resp.StatusCode, string(responseBody))
	}

	return responseBody, nil
}

// Data API
type ItemType struct {
	RawID       int    `json:"rawId"`
	Key         string `json:"key"`
	MaxCount    int    `json:"maxCount"`
	DisplayName string `json:"displayName"`
}

func (c *APIClient) GetItemMetadata() ([]ItemType, error) {
	if c.debug {
		log.Printf("DEBUG: GetItemMetadata() called")
	}

	data, err := c.makeRequest("GET", "/data/items", nil)
	if err != nil {
		if c.debug {
			log.Printf("DEBUG: GetItemMetadata() request failed: %v", err)
		}
		return nil, err
	}

	var items []ItemType
	if err := json.Unmarshal(data, &items); err != nil {
		if c.debug {
			log.Printf("DEBUG: GetItemMetadata() unmarshal failed: %v", err)
			log.Printf("DEBUG: Raw response data: %s", string(data))
		}
		return nil, fmt.Errorf("failed to unmarshal item metadata: %w", err)
	}

	if c.debug {
		log.Printf("DEBUG: GetItemMetadata() success - loaded %d items", len(items))
		if len(items) > 0 {
			log.Printf("DEBUG: First item: %+v", items[0])
		}
	}

	return items, nil
}

func (c *APIClient) GetInventoryListing() ([]Item, error) {
	if c.debug {
		log.Printf("DEBUG: GetInventoryListing() called")
	}

	data, err := c.makeRequest("GET", "/automation/inventory_listing", nil)
	if err != nil {
		if c.debug {
			log.Printf("DEBUG: GetInventoryListing() request failed: %v", err)
		}
		return nil, err
	}

	var items []Item
	if err := json.Unmarshal(data, &items); err != nil {
		if c.debug {
			log.Printf("DEBUG: GetInventoryListing() unmarshal failed: %v", err)
			log.Printf("DEBUG: Raw response data: %s", string(data))
		}
		return nil, fmt.Errorf("failed to unmarshal inventory listing: %w", err)
	}

	if c.debug {
		log.Printf("DEBUG: GetInventoryListing() success - loaded %d items", len(items))
		if len(items) > 0 {
			log.Printf("DEBUG: First item: %+v", items[0])
		}
	}

	return items, nil
}

type InventoryItem struct {
	ItemType      ItemType
	InventoryData Item
	DisplayName   string
	Count         int
}

func (c *APIClient) GetAvailableItems() ([]InventoryItem, error) {
	if c.debug {
		log.Printf("DEBUG: GetAvailableItems() called")
	}

	// Get both metadata and inventory listing in parallel
	metadataChan := make(chan []ItemType, 1)
	metadataErrChan := make(chan error, 1)
	inventoryChan := make(chan []Item, 1)
	inventoryErrChan := make(chan error, 1)

	go func() {
		metadata, err := c.GetItemMetadata()
		metadataChan <- metadata
		metadataErrChan <- err
	}()

	go func() {
		inventory, err := c.GetInventoryListing()
		inventoryChan <- inventory
		inventoryErrChan <- err
	}()

	// Wait for both to complete
	metadata := <-metadataChan
	metadataErr := <-metadataErrChan
	inventory := <-inventoryChan
	inventoryErr := <-inventoryErrChan

	if metadataErr != nil {
		if c.debug {
			log.Printf("DEBUG: GetAvailableItems() metadata failed: %v", metadataErr)
		}
		return nil, fmt.Errorf("failed to get item metadata: %w", metadataErr)
	}

	if inventoryErr != nil {
		if c.debug {
			log.Printf("DEBUG: GetAvailableItems() inventory failed: %v", inventoryErr)
		}
		return nil, fmt.Errorf("failed to get inventory listing: %w", inventoryErr)
	}

	// Create a map of item metadata by ID for quick lookup
	itemMetadataMap := make(map[int]ItemType)
	for _, item := range metadata {
		itemMetadataMap[item.RawID] = item
	}

	// Combine inventory data with metadata
	var availableItems []InventoryItem
	for _, invItem := range inventory {
		if itemType, exists := itemMetadataMap[invItem.ItemID]; exists {
			availableItems = append(availableItems, InventoryItem{
				ItemType:      itemType,
				InventoryData: invItem,
				DisplayName:   itemType.DisplayName,
				Count:         invItem.Count,
			})
		} else {
			// Fallback if metadata not found
			availableItems = append(availableItems, InventoryItem{
				ItemType: ItemType{
					RawID:       invItem.ItemID,
					DisplayName: fmt.Sprintf("Unknown Item %d", invItem.ItemID),
					Key:         fmt.Sprintf("unknown:%d", invItem.ItemID),
					MaxCount:    invItem.StackSize,
				},
				InventoryData: invItem,
				DisplayName:   fmt.Sprintf("Unknown Item %d", invItem.ItemID),
				Count:         invItem.Count,
			})
		}
	}

	if c.debug {
		log.Printf("DEBUG: GetAvailableItems() success - merged %d available items", len(availableItems))
		if len(availableItems) > 0 {
			log.Printf("DEBUG: First available item: %+v", availableItems[0])
		}
	}

	return availableItems, nil
}

// Automation API
type Vec3 struct {
	X int `json:"x"`
	Y int `json:"y"`
	Z int `json:"z"`
}

type Loc struct {
	Vec3 Vec3   `json:"vec3"`
	Dim  string `json:"dim"`
}

type SignNode struct {
	Location Loc   `json:"location"`
	Name     string `json:"name"`
	Pickup   *Vec3 `json:"pickup"`
	Dropoff  *Vec3 `json:"dropoff"`
	Home     bool  `json:"home"`
}

type SignConfigResponse struct {
	Nodes map[string]SignNode `json:"nodes"`
}

func (c *APIClient) GetSignConfig() (*SignConfigResponse, error) {
	data, err := c.makeRequest("GET", "/automation/sign_config", nil)
	if err != nil {
		return nil, err
	}

	if c.debug {
		log.Printf("DEBUG: Attempting to unmarshal sign config...")
	}

	var config SignConfigResponse
	if err := json.Unmarshal(data, &config); err != nil {
		if c.debug {
			log.Printf("DEBUG: Sign config unmarshal failed: %v", err)
			log.Printf("DEBUG: Raw sign config data (first 1000 chars): %s", string(data[:min(1000, len(data))]))
		}
		return nil, fmt.Errorf("failed to unmarshal sign config: %w", err)
	}

	if c.debug {
		log.Printf("DEBUG: Sign config unmarshal successful, nodes count: %d", len(config.Nodes))
		if len(config.Nodes) > 0 {
			// Get first node name
			for name := range config.Nodes {
				log.Printf("DEBUG: First node: %s", name)
				break
			}
		}
	}

	return &config, nil
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

// Item and hold structures for delivery
type Item struct {
	ItemID                    int         `json:"item_id"`
	Count                     int         `json:"count"`
	Metadata                  int         `json:"metadata"`
	NBT                       interface{} `json:"nbt"`
	StackSize                 int         `json:"stack_size"`
	StackableHash             string      `json:"stackable_hash"`
	FullShulkerStackableHash  *string     `json:"full_shulker_stackable_hash,omitempty"`
}

type DeliveryItem struct {
	Item         Item   `json:"item"`
	ShulkerCount int    `json:"shulkerCount"`
	ItemCount    int    `json:"itemCount"`
	DisplayName  string `json:"-"` // Don't serialize to JSON, just for display
}

// Hold request structures
type StackableHashCriteria struct {
	StackableHash string `json:"stackable_hash"`
}

type MatchCriteria struct {
	StackableHash StackableHashCriteria `json:"StackableHash"`
}

type ItemMatch struct {
	MatchCriteria MatchCriteria `json:"match_criteria"`
	Total         int           `json:"total"`
}

// HoldRequestFilter can be either "EmptySlot" or an ItemMatch
type HoldRequestFilter interface {
	isHoldRequestFilter()
}

// EmptySlotFilter represents an empty slot request
type EmptySlotFilter struct{}

func (EmptySlotFilter) isHoldRequestFilter() {}
func (e EmptySlotFilter) MarshalJSON() ([]byte, error) {
	return []byte(`"EmptySlot"`), nil
}

// ItemMatchFilter represents an item match request
type ItemMatchFilter struct {
	ItemMatch ItemMatch `json:"ItemMatch"`
}

func (ItemMatchFilter) isHoldRequestFilter() {}

type Hold struct {
	ID         string `json:"id"`
	Location   Loc    `json:"location"`
	Slot       int    `json:"slot"`
	ValidUntil string `json:"valid_until"`
}

type HoldResponse struct {
	Holds []Hold `json:"holds"`
}

type HoldResult struct {
	Holds *HoldResponse `json:"Holds,omitempty"`
	Error *string       `json:"Error,omitempty"`
}

type CreateHoldResponse struct {
	Results []HoldResult `json:"results"`
}

func (c *APIClient) CreateHold(requests []HoldRequestFilter) (*CreateHoldResponse, error) {
	requestBody := map[string][]HoldRequestFilter{
		"requests": requests,
	}
	
	data, err := c.makeRequest("POST", "/automation/holds", requestBody)
	if err != nil {
		return nil, err
	}

	var response CreateHoldResponse
	if err := json.Unmarshal(data, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal hold response: %w", err)
	}

	return &response, nil
}

func (c *APIClient) ReleaseHolds(holdIDs []string) error {
	_, err := c.makeRequest("DELETE", "/automation/holds", holdIDs)
	return err
}

// Operation structures
type DropItemsOperation struct {
	Type        string   `json:"type"`
	SourceHolds []string `json:"source_holds"`
	DropFrom    Loc      `json:"drop_from"`
	AimTowards  Vec3     `json:"aim_towards"`
}

type ImportInventoryOperation struct {
	Type             string   `json:"type"`
	ChestLocation    Vec3     `json:"chest_location"`
	NodeLocation     Loc      `json:"node_location"`
	DestinationHolds []string `json:"destination_holds"`
}

type OperationRequest struct {
	Operation interface{} `json:"operation"`
	Priority  string      `json:"priority"`
}

func (c *APIClient) ExecuteOperation(operation interface{}, priority string) error {
	request := map[string]interface{}{
		"kind":     operation,
		"priority": priority,
	}

	_, err := c.makeRequest("POST", "/automation/operations", request)
	return err
}

func (c *APIClient) DeliverItems(destinationLoc string, items []DeliveryItem) error {
	config, err := c.GetSignConfig()
	if err != nil {
		return fmt.Errorf("failed to get sign config: %w", err)
	}

	destNode, exists := config.Nodes[destinationLoc]
	if !exists {
		return fmt.Errorf("destination location does not exist")
	}

	if destNode.Dropoff == nil {
		return fmt.Errorf("destination does not have a drop-off location")
	}

	var holdIDs []string
	defer func() {
		if len(holdIDs) > 0 {
			c.ReleaseHolds(holdIDs)
		}
	}()

	var itemRequests []HoldRequestFilter
	for _, item := range items {
		if item.ItemCount > 0 {
			itemRequests = append(itemRequests, ItemMatchFilter{
				ItemMatch: ItemMatch{
					MatchCriteria: MatchCriteria{
						StackableHash: StackableHashCriteria{
							StackableHash: item.Item.StackableHash,
						},
					},
					Total: item.ItemCount,
				},
			})
		}

		if item.ShulkerCount > 0 {
			if item.Item.FullShulkerStackableHash != nil {
				itemRequests = append(itemRequests, ItemMatchFilter{
					ItemMatch: ItemMatch{
						MatchCriteria: MatchCriteria{
							StackableHash: StackableHashCriteria{
								StackableHash: *item.Item.FullShulkerStackableHash,
							},
						},
						Total: item.ShulkerCount,
					},
				})
			} else {
				totalItems := item.ShulkerCount * item.Item.StackSize * 27
				itemRequests = append(itemRequests, ItemMatchFilter{
					ItemMatch: ItemMatch{
						MatchCriteria: MatchCriteria{
							StackableHash: StackableHashCriteria{
								StackableHash: item.Item.StackableHash,
							},
						},
						Total: totalItems,
					},
				})
			}
		}
	}

	holdResponse, err := c.CreateHold(itemRequests)
	if err != nil {
		return fmt.Errorf("failed to create holds: %w", err)
	}

	for _, result := range holdResponse.Results {
		if result.Error != nil {
			return fmt.Errorf("failed to acquire items: %s", *result.Error)
		}
		if result.Holds != nil {
			for _, hold := range result.Holds.Holds {
				holdIDs = append(holdIDs, hold.ID)
			}
		}
	}

	if len(holdIDs) > 27 {
		return fmt.Errorf("too many slots to deliver")
	}

	operation := DropItemsOperation{
		Type:        "DropItems",
		SourceHolds: holdIDs,
		DropFrom:    destNode.Location,
		AimTowards:  *destNode.Dropoff,
	}

	return c.ExecuteOperation(operation, "UserInteractive")
}

func (c *APIClient) PickupItems(destinationLoc string, repeatUntilEmpty bool) error {
	config, err := c.GetSignConfig()
	if err != nil {
		return fmt.Errorf("failed to get sign config: %w", err)
	}

	destNode, exists := config.Nodes[destinationLoc]
	if !exists {
		return fmt.Errorf("destination location does not exist")
	}

	if destNode.Pickup == nil {
		return fmt.Errorf("destination does not have a pickup chest")
	}

	// Create requests for 27 empty slots
	freeSpaceRequests := make([]HoldRequestFilter, 27)
	for i := 0; i < 27; i++ {
		freeSpaceRequests[i] = EmptySlotFilter{}
	}

	holdResponse, err := c.CreateHold(freeSpaceRequests)
	if err != nil {
		return fmt.Errorf("failed to acquire free spaces: %w", err)
	}

	var holdIDs []string
	for _, result := range holdResponse.Results {
		if result.Error != nil {
			continue // Skip failed free space acquisitions
		}
		if result.Holds != nil {
			for _, hold := range result.Holds.Holds {
				holdIDs = append(holdIDs, hold.ID)
			}
		}
	}

	defer c.ReleaseHolds(holdIDs)

	operation := ImportInventoryOperation{
		Type:             "ImportInventory",
		ChestLocation:    *destNode.Pickup,
		NodeLocation:     destNode.Location,
		DestinationHolds: holdIDs,
	}

	return c.ExecuteOperation(operation, "UserInteractive")
}

// Admin API
type Stats struct {
	InventoriesInMem      int                    `json:"inventories_in_mem"`
	TotalSlots           int                    `json:"total_slots"`
	FreeSlots            int                    `json:"free_slots"`
	CurrentHolds         int                    `json:"current_holds"`
	OperationsPending    int                    `json:"operations_pending"`
	OperationsInProgress int                    `json:"operations_in_progress"`
	OperationsComplete   int                    `json:"operations_complete"`
	OperationsAborted    int                    `json:"operations_aborted"`
	AgentsConnected      int                    `json:"agents_connected"`
	ServicesTickTimesMicros map[string]int      `json:"services_tick_times_micros"`
}

func (c *APIClient) GetStats() (*Stats, error) {
	data, err := c.makeRequest("GET", "/admin/stats", nil)
	if err != nil {
		return nil, err
	}

	var stats Stats
	if err := json.Unmarshal(data, &stats); err != nil {
		return nil, fmt.Errorf("failed to unmarshal stats: %w", err)
	}

	return &stats, nil
}