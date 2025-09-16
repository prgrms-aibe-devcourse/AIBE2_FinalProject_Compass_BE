#!/bin/bash

# ============================================
# 기존 파일 복사 스크립트
# ============================================

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 현재 프로젝트 경로 (기존 코드가 있는 곳)
CURRENT_PROJECT="/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE"
# 새 프로젝트 경로 (스켈레톤을 만들 곳) - 실제로는 같은 경로일 수 있음
NEW_PROJECT="/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE"

echo -e "${GREEN}📋 Copying existing files that can be reused...${NC}"
echo "=================================="

# ========== CONFIG - JWT/OAuth (100% 재사용) ==========
echo -e "${YELLOW}📁 Copying JWT/OAuth configuration files...${NC}"

files_to_copy=(
    "src/main/java/com/compass/config/jwt/JwtTokenProvider.java"
    "src/main/java/com/compass/config/jwt/JwtAuthenticationFilter.java"
    "src/main/java/com/compass/config/oauth/OAuth2AuthenticationSuccessHandler.java"
    "src/main/java/com/compass/config/oauth/OAuth2AuthenticationFailureHandler.java"
    "src/main/java/com/compass/config/oauth/CustomOAuth2UserService.java"
    "src/main/java/com/compass/config/oauth/OAuthAttributes.java"
    "src/main/java/com/compass/config/SecurityConfig.java"
    "src/main/java/com/compass/config/AiConfig.java"
    "src/main/java/com/compass/config/RedisConfig.java"
    "src/main/java/com/compass/config/DotEnvConfig.java"
    "src/main/java/com/compass/config/SwaggerConfig.java"
)

for file in "${files_to_copy[@]}"; do
    if [ -f "$CURRENT_PROJECT/$file" ]; then
        cp "$CURRENT_PROJECT/$file" "$NEW_PROJECT/$file"
        echo "  ✓ Copied: $(basename $file)"
    else
        echo -e "  ${RED}✗ Not found: $(basename $file)${NC}"
    fi
done

# ========== ENTITY (100% 재사용) ==========
echo -e "${YELLOW}📁 Copying entity files...${NC}"

# User entity를 auth 도메인으로 복사
if [ -f "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/entity/User.java" ]; then
    cp "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/entity/User.java" \
       "$NEW_PROJECT/src/main/java/com/compass/domain/auth/entity/User.java"
    echo "  ✓ Copied: User.java to auth domain"
fi

# User 관련 enum들도 복사
if [ -f "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/enums/Role.java" ]; then
    mkdir -p "$NEW_PROJECT/src/main/java/com/compass/domain/auth/enums"
    cp "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/enums/Role.java" \
       "$NEW_PROJECT/src/main/java/com/compass/domain/auth/enums/Role.java"
    echo "  ✓ Copied: Role.java"
fi

if [ -f "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/enums/SocialType.java" ]; then
    cp "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/enums/SocialType.java" \
       "$NEW_PROJECT/src/main/java/com/compass/domain/auth/enums/SocialType.java"
    echo "  ✓ Copied: SocialType.java"
fi

# Chat entity 복사
chat_entities=(
    "src/main/java/com/compass/domain/chat/entity/ChatThread.java"
    "src/main/java/com/compass/domain/chat/entity/ChatMessage.java"
)

for file in "${chat_entities[@]}"; do
    if [ -f "$CURRENT_PROJECT/$file" ]; then
        cp "$CURRENT_PROJECT/$file" "$NEW_PROJECT/$file"
        echo "  ✓ Copied: $(basename $file)"
    fi
done

# ========== REPOSITORY (100% 재사용) ==========
echo -e "${YELLOW}📁 Copying repository files...${NC}"

# User repository를 auth 도메인으로 복사
if [ -f "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/repository/UserRepository.java" ]; then
    cp "$CURRENT_PROJECT/src/main/java/com/compass/domain/user/repository/UserRepository.java" \
       "$NEW_PROJECT/src/main/java/com/compass/domain/auth/repository/UserRepository.java"
    echo "  ✓ Copied: UserRepository.java to auth domain"
fi

# Chat repository 복사
chat_repos=(
    "src/main/java/com/compass/domain/chat/repository/ChatThreadRepository.java"
    "src/main/java/com/compass/domain/chat/repository/ChatMessageRepository.java"
)

for file in "${chat_repos[@]}"; do
    if [ -f "$CURRENT_PROJECT/$file" ]; then
        cp "$CURRENT_PROJECT/$file" "$NEW_PROJECT/$file"
        echo "  ✓ Copied: $(basename $file)"
    fi
done

# ========== COMMON (100% 재사용) ==========
echo -e "${YELLOW}📁 Copying common files...${NC}"

common_files=(
    "src/main/java/com/compass/common/entity/BaseEntity.java"
    "src/main/java/com/compass/common/entity/BaseTimeEntity.java"
    "src/main/java/com/compass/common/exception/GlobalExceptionHandler.java"
)

for file in "${common_files[@]}"; do
    if [ -f "$CURRENT_PROJECT/$file" ]; then
        cp "$CURRENT_PROJECT/$file" "$NEW_PROJECT/$file"
        echo "  ✓ Copied: $(basename $file)"
    fi
done

# ========== AUTH SERVICE (기존 UserService 활용) ==========
echo -e "${YELLOW}📁 Copying auth service files...${NC}"

if [ -f "$CURRENT_PROJECT/src/main/java/com/compass/config/security/UserDetailsServiceImpl.java" ]; then
    cp "$CURRENT_PROJECT/src/main/java/com/compass/config/security/UserDetailsServiceImpl.java" \
       "$NEW_PROJECT/src/main/java/com/compass/domain/auth/service/UserDetailsServiceImpl.java"
    echo "  ✓ Copied: UserDetailsServiceImpl.java to auth service"
fi

# ========== RESOURCES (100% 재사용) ==========
echo -e "${YELLOW}📁 Copying resource files...${NC}"

resource_files=(
    "src/main/resources/application.yml"
    "src/main/resources/application-local.yml"
    "src/main/resources/application-prod.yml"
    "src/main/resources/application-test.yml"
)

for file in "${resource_files[@]}"; do
    if [ -f "$CURRENT_PROJECT/$file" ]; then
        cp "$CURRENT_PROJECT/$file" "$NEW_PROJECT/$file"
        echo "  ✓ Copied: $(basename $file)"
    fi
done

# ========== BUILD FILES (100% 재사용) ==========
echo -e "${YELLOW}📁 Copying build files...${NC}"

build_files=(
    "build.gradle"
    "settings.gradle"
    ".gitignore"
    ".env.example"
    "gradlew"
    "gradlew.bat"
)

for file in "${build_files[@]}"; do
    if [ -f "$CURRENT_PROJECT/$file" ]; then
        cp "$CURRENT_PROJECT/$file" "$NEW_PROJECT/$file"
        echo "  ✓ Copied: $file"
    fi
done

# gradle wrapper 디렉토리도 복사
if [ -d "$CURRENT_PROJECT/gradle" ]; then
    cp -r "$CURRENT_PROJECT/gradle" "$NEW_PROJECT/"
    echo "  ✓ Copied: gradle wrapper directory"
fi

echo "=================================="
echo -e "${GREEN}✅ Existing files copied successfully!${NC}"
echo ""
echo "Summary:"
echo "  - JWT/OAuth configuration ✓"
echo "  - Entity classes ✓"
echo "  - Repository interfaces ✓"
echo "  - Common utilities ✓"
echo "  - Resource files ✓"
echo "  - Build configuration ✓"
echo ""
echo "Next step: Run create_empty_files.sh to create skeleton files"