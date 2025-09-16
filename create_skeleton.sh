#!/bin/bash

# 프로젝트 루트 설정
PROJECT_ROOT="/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE"
SRC_ROOT="$PROJECT_ROOT/src/main/java/com/compass"

echo "🏗️ Creating Skeleton Structure..."

# ========== CONFIG 디렉토리 ==========
echo "📁 Creating config directories..."
mkdir -p $SRC_ROOT/config/jwt
mkdir -p $SRC_ROOT/config/oauth
mkdir -p $SRC_ROOT/config/security

# ========== DOMAIN/AUTH 디렉토리 ==========
echo "📁 Creating auth domain directories..."
mkdir -p $SRC_ROOT/domain/auth/controller
mkdir -p $SRC_ROOT/domain/auth/service
mkdir -p $SRC_ROOT/domain/auth/entity
mkdir -p $SRC_ROOT/domain/auth/repository
mkdir -p $SRC_ROOT/domain/auth/dto

# ========== DOMAIN/CHAT 디렉토리 ==========
echo "📁 Creating chat domain directories..."
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
mkdir -p $SRC_ROOT/domain/chat/entity
mkdir -p $SRC_ROOT/domain/chat/repository

# ========== COMMON 디렉토리 ==========
echo "📁 Creating common directories..."
mkdir -p $SRC_ROOT/common/entity
mkdir -p $SRC_ROOT/common/exception
mkdir -p $SRC_ROOT/common/util

# ========== RESOURCES 디렉토리 ==========
echo "📁 Creating resources..."
mkdir -p $PROJECT_ROOT/src/main/resources
mkdir -p $PROJECT_ROOT/src/test/java/com/compass

echo "✅ Directory structure created successfully!"
