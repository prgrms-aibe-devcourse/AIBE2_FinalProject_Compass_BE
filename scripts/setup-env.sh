#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}===============================================${NC}"
echo -e "${GREEN}   Compass Backend Environment Setup Script   ${NC}"
echo -e "${GREEN}===============================================${NC}"
echo ""

# Check if .env file already exists
if [ -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file already exists!${NC}"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Setup cancelled.${NC}"
        exit 1
    fi
fi

# Copy from .env.example
if [ -f .env.example ]; then
    cp .env.example .env
    echo -e "${GREEN}✅ Created .env file from .env.example${NC}"
else
    echo -e "${RED}❌ .env.example file not found!${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Please provide the following credentials:${NC}"
echo -e "${YELLOW}(You can get these from your team lead or GitHub Secrets)${NC}"
echo ""

# Function to update .env file
update_env() {
    local key=$1
    local value=$2
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s|^${key}=.*|${key}=${value}|" .env
    else
        # Linux
        sed -i "s|^${key}=.*|${key}=${value}|" .env
    fi
}

# Database Configuration
echo -e "${GREEN}Database Configuration:${NC}"
read -p "DB Password (default: compass_password): " db_password
db_password=${db_password:-compass_password}
update_env "DB_PASSWORD" "$db_password"

# JWT Configuration
echo ""
echo -e "${GREEN}JWT Configuration:${NC}"
echo -e "${YELLOW}(Press Enter to use default development keys)${NC}"
read -p "JWT Access Secret: " jwt_access
jwt_access=${jwt_access:-dev-jwt-access-secret-key-change-in-production-256bits}
update_env "JWT_ACCESS_SECRET" "$jwt_access"

read -p "JWT Refresh Secret: " jwt_refresh
jwt_refresh=${jwt_refresh:-dev-jwt-refresh-secret-key-change-in-production-256bits}
update_env "JWT_REFRESH_SECRET" "$jwt_refresh"

# OpenAI Configuration
echo ""
echo -e "${GREEN}OpenAI Configuration:${NC}"
read -p "OpenAI API Key (optional, press Enter to skip): " openai_key
if [ ! -z "$openai_key" ]; then
    update_env "OPENAI_API_KEY" "$openai_key"
fi

# Google Cloud Configuration
echo ""
echo -e "${GREEN}Google Cloud Configuration:${NC}"
read -p "Google Credentials Base64 (optional, press Enter to skip): " google_creds
if [ ! -z "$google_creds" ]; then
    update_env "GOOGLE_CREDENTIALS_BASE64" "$google_creds"
fi

read -p "Google Cloud Project ID (default: travelagent-468611): " project_id
project_id=${project_id:-travelagent-468611}
update_env "GOOGLE_CLOUD_PROJECT_ID" "$project_id"

read -p "Google Cloud Location (default: us-central1): " location
location=${location:-us-central1}
update_env "GOOGLE_CLOUD_LOCATION" "$location"

echo ""
echo -e "${GREEN}===============================================${NC}"
echo -e "${GREEN}✅ Environment setup completed!${NC}"
echo -e "${GREEN}===============================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Review the .env file to ensure all values are correct"
echo "2. Start the required services:"
echo "   ${GREEN}docker-compose up -d postgres redis${NC}"
echo "3. Run the application:"
echo "   ${GREEN}./gradlew bootRun${NC}"
echo ""
echo -e "${YELLOW}Note: For production credentials, contact your team lead.${NC}"