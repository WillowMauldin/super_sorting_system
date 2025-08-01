# Super Sorting System TUI Makefile

BINARY_NAME=super-sorting-system-tui
BUILD_DIR=build
INSTALL_PATH=/usr/local/bin

.PHONY: all build clean install uninstall run dev test fmt vet

# Default target
all: build

# Build the binary
build:
	@echo "Building $(BINARY_NAME)..."
	@mkdir -p $(BUILD_DIR)
	go build -o $(BUILD_DIR)/$(BINARY_NAME) .
	@echo "Build complete: $(BUILD_DIR)/$(BINARY_NAME)"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	rm -rf $(BUILD_DIR)
	go clean

# Install the binary to system path
install: build
	@echo "Installing $(BINARY_NAME) to $(INSTALL_PATH)..."
	sudo cp $(BUILD_DIR)/$(BINARY_NAME) $(INSTALL_PATH)/
	@echo "Installation complete"

# Uninstall the binary from system path
uninstall:
	@echo "Uninstalling $(BINARY_NAME) from $(INSTALL_PATH)..."
	sudo rm -f $(INSTALL_PATH)/$(BINARY_NAME)
	@echo "Uninstallation complete"

# Run the application
run: build
	@echo "Running $(BINARY_NAME)..."
	./$(BUILD_DIR)/$(BINARY_NAME)

# Development mode with live reload (requires air)
dev:
	@if command -v air > /dev/null; then \
		air; \
	else \
		echo "Air not found. Install with: go install github.com/cosmtrek/air@latest"; \
		echo "Falling back to regular run..."; \
		make run; \
	fi

# Run tests
test:
	go test -v ./...

# Format code
fmt:
	go fmt ./...

# Vet code for issues
vet:
	go vet ./...

# Tidy dependencies
tidy:
	go mod tidy

# Check for updates
update:
	go get -u ./...
	go mod tidy

# Build for multiple platforms
build-all:
	@echo "Building for multiple platforms..."
	@mkdir -p $(BUILD_DIR)
	
	# Linux AMD64
	GOOS=linux GOARCH=amd64 go build -o $(BUILD_DIR)/$(BINARY_NAME)-linux-amd64 .
	
	# Linux ARM64
	GOOS=linux GOARCH=arm64 go build -o $(BUILD_DIR)/$(BINARY_NAME)-linux-arm64 .
	
	# macOS AMD64
	GOOS=darwin GOARCH=amd64 go build -o $(BUILD_DIR)/$(BINARY_NAME)-darwin-amd64 .
	
	# macOS ARM64
	GOOS=darwin GOARCH=arm64 go build -o $(BUILD_DIR)/$(BINARY_NAME)-darwin-arm64 .
	
	# Windows AMD64
	GOOS=windows GOARCH=amd64 go build -o $(BUILD_DIR)/$(BINARY_NAME)-windows-amd64.exe .
	
	@echo "Multi-platform build complete"

# Help target
help:
	@echo "Available targets:"
	@echo "  all        - Build the application (default)"
	@echo "  build      - Build the binary"
	@echo "  clean      - Clean build artifacts"
	@echo "  install    - Install to system path"
	@echo "  uninstall  - Remove from system path"
	@echo "  run        - Build and run the application"
	@echo "  dev        - Run in development mode with live reload"
	@echo "  test       - Run tests"
	@echo "  fmt        - Format code"
	@echo "  vet        - Vet code for issues"
	@echo "  tidy       - Tidy dependencies"
	@echo "  update     - Update dependencies"
	@echo "  build-all  - Build for multiple platforms"
	@echo "  help       - Show this help message"