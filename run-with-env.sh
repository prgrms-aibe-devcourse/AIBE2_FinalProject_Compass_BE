#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    # Export each valid line as an environment variable
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        if [[ ! "$key" =~ ^#.*$ ]] && [[ -n "$key" ]]; then
            # Remove quotes if present
            value="${value%\"}"
            value="${value#\"}"
            value="${value%\'}"
            value="${value#\'}"
            export "$key=$value"
            echo "Exported: $key"
        fi
    done < .env
fi

# Run the Spring Boot application
./gradlew bootRun