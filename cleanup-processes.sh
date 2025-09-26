#!/bin/bash

echo "🧹 Compass BE 프로세스 정리 스크립트"
echo "====================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Spring Boot 프로세스 종료
echo -e "${YELLOW}1. Spring Boot 프로세스 종료 중...${NC}"
ps aux | grep -E 'bootRun|spring-boot:run' | grep -v grep | awk '{print $2}' | while read pid; do
    if [ ! -z "$pid" ]; then
        echo -e "  ${RED}→ PID $pid 종료${NC}"
        kill -9 $pid 2>/dev/null
    fi
done

# 2. 포트 8080 사용 프로세스 종료
echo -e "${YELLOW}2. 포트 8080 정리 중...${NC}"
lsof -ti :8080 | while read pid; do
    if [ ! -z "$pid" ]; then
        echo -e "  ${RED}→ PID $pid 종료 (포트 8080 사용)${NC}"
        kill -9 $pid 2>/dev/null
    fi
done

# 3. Gradle Daemon 종료
echo -e "${YELLOW}3. Gradle Daemon 정리 중...${NC}"
./gradlew --stop 2>/dev/null
sleep 2

# 남아있는 Gradle 프로세스 강제 종료
ps aux | grep -E 'GradleDaemon|gradle' | grep -v grep | awk '{print $2}' | while read pid; do
    if [ ! -z "$pid" ]; then
        echo -e "  ${RED}→ PID $pid 종료 (Gradle)${NC}"
        kill -9 $pid 2>/dev/null
    fi
done

# 4. Java 프로세스 중 Compass 관련 프로세스만 종료 (IDE 제외)
echo -e "${YELLOW}4. Compass Java 프로세스 정리 중...${NC}"
ps aux | grep java | grep -E 'compass|Compass' | grep -v -E 'cursor|Code|idea|eclipse' | grep -v grep | awk '{print $2}' | while read pid; do
    if [ ! -z "$pid" ]; then
        echo -e "  ${RED}→ PID $pid 종료 (Compass Java)${NC}"
        kill -9 $pid 2>/dev/null
    fi
done

# 5. Docker 컨테이너 정리
echo -e "${YELLOW}5. Docker 컨테이너 정리 중...${NC}"
if docker ps >/dev/null 2>&1; then
    # Compass 관련 컨테이너만 종료
    docker ps -a | grep -E 'compass|COMPASS' | awk '{print $1}' | while read container; do
        if [ ! -z "$container" ]; then
            echo -e "  ${RED}→ Container $container 종료${NC}"
            docker stop $container 2>/dev/null
            docker rm $container 2>/dev/null
        fi
    done
else
    echo -e "  ${GREEN}Docker가 실행되지 않음${NC}"
fi

# 6. Gradle 캐시 정리 (선택적)
echo -e "${YELLOW}6. Gradle 캐시 정리 여부 확인${NC}"
read -p "Gradle 빌드 캐시를 정리하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "  ${RED}→ Gradle 캐시 정리 중...${NC}"
    rm -rf ~/.gradle/caches/build-cache-*
    rm -rf .gradle/
    echo -e "  ${GREEN}✓ 캐시 정리 완료${NC}"
fi

# 7. 상태 확인
echo ""
echo -e "${GREEN}=== 정리 완료 ===${NC}"
echo ""

# 남은 프로세스 확인
REMAINING_JAVA=$(ps aux | grep -E 'java.*compass|gradle' | grep -v -E 'cursor|Code|idea|eclipse|grep' | wc -l)
REMAINING_PORT=$(lsof -ti :8080 2>/dev/null | wc -l)

if [ "$REMAINING_JAVA" -eq 0 ] && [ "$REMAINING_PORT" -eq 0 ]; then
    echo -e "${GREEN}✅ 모든 프로세스가 정리되었습니다!${NC}"
else
    echo -e "${YELLOW}⚠️  일부 프로세스가 남아있을 수 있습니다:${NC}"
    if [ "$REMAINING_JAVA" -gt 0 ]; then
        echo -e "${YELLOW}  - Java/Gradle 프로세스: $REMAINING_JAVA 개${NC}"
    fi
    if [ "$REMAINING_PORT" -gt 0 ]; then
        echo -e "${YELLOW}  - 포트 8080 사용: $REMAINING_PORT 개${NC}"
    fi
fi

echo ""
echo "이제 'run-spring.sh' 스크립트로 안전하게 실행할 수 있습니다."