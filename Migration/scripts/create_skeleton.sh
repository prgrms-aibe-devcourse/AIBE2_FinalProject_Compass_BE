#!/bin/bash

# ============================================
# 오케스트레이터 스켈레톤 디렉토리 생성 스크립트
# ============================================

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 프로젝트 루트 설정
PROJECT_ROOT="/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE"
SRC_ROOT="$PROJECT_ROOT/src/main/java/com/compass"

echo -e "${GREEN}🏗️ Creating Orchestrator Skeleton Structure...${NC}"
echo "=================================="

# ========== CONFIG 디렉토리 ==========
echo -e "${YELLOW}📁 Creating config directories...${NC}"
mkdir -p $SRC_ROOT/config/jwt
mkdir -p $SRC_ROOT/config/oauth
mkdir -p $SRC_ROOT/config/security
echo "  ✓ Config directories created"

# ========== DOMAIN/AUTH 디렉토리 ==========
echo -e "${YELLOW}📁 Creating auth domain directories...${NC}"
mkdir -p $SRC_ROOT/domain/auth/controller
mkdir -p $SRC_ROOT/domain/auth/service
mkdir -p $SRC_ROOT/domain/auth/entity
mkdir -p $SRC_ROOT/domain/auth/repository
mkdir -p $SRC_ROOT/domain/auth/dto
echo "  ✓ Auth domain directories created"

# ========== DOMAIN/CHAT 디렉토리 ==========
echo -e "${YELLOW}📁 Creating chat domain directories...${NC}"
mkdir -p $SRC_ROOT/domain/chat/controller
mkdir -p $SRC_ROOT/domain/chat/orchestrator
mkdir -p $SRC_ROOT/domain/chat/function/config
mkdir -p $SRC_ROOT/domain/chat/function/collection
mkdir -p $SRC_ROOT/domain/chat/function/processing
mkdir -p $SRC_ROOT/domain/chat/function/planning
mkdir -p $SRC_ROOT/domain/chat/function/external
mkdir -p $SRC_ROOT/domain/chat/function/refinement
mkdir -p $SRC_ROOT/domain/chat/service/internal
mkdir -p $SRC_ROOT/domain/chat/service/external
mkdir -p $SRC_ROOT/domain/chat/model/enums
mkdir -p $SRC_ROOT/domain/chat/model/request
mkdir -p $SRC_ROOT/domain/chat/model/response
mkdir -p $SRC_ROOT/domain/chat/model/context
mkdir -p $SRC_ROOT/domain/chat/model/dto
mkdir -p $SRC_ROOT/domain/chat/entity
mkdir -p $SRC_ROOT/domain/chat/repository
echo "  ✓ Chat domain directories created"

# ========== COMMON 디렉토리 ==========
echo -e "${YELLOW}📁 Creating common directories...${NC}"
mkdir -p $SRC_ROOT/common/entity
mkdir -p $SRC_ROOT/common/exception
mkdir -p $SRC_ROOT/common/util
echo "  ✓ Common directories created"

# ========== RESOURCES 디렉토리 ==========
echo -e "${YELLOW}📁 Creating resources directories...${NC}"
mkdir -p $PROJECT_ROOT/src/main/resources
mkdir -p $PROJECT_ROOT/src/test/java/com/compass/domain/chat
mkdir -p $PROJECT_ROOT/src/test/java/com/compass/domain/auth
mkdir -p $PROJECT_ROOT/src/test/resources
echo "  ✓ Resources directories created"

# ========== APPLICATION 파일 ==========
echo -e "${YELLOW}📁 Creating application main class...${NC}"
cat > $SRC_ROOT/CompassApplication.java << 'EOF'
package com.compass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompassApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompassApplication.class, args);
    }
}
EOF
echo "  ✓ Application main class created"

echo "=================================="
echo -e "${GREEN}✅ Directory structure created successfully!${NC}"
echo ""
echo "Next steps:"
echo "1. Run copy_existing_files.sh to copy existing code"
echo "2. Run create_empty_files.sh to create skeleton files"
echo "3. Commit and push to repository"