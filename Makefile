# Compass Backend Makefile

.PHONY: help setup run test clean

help: ## 도움말 표시
	@echo "사용 가능한 명령어:"
	@echo "  make setup  - 개발 환경 초기 설정"
	@echo "  make run    - 애플리케이션 실행"
	@echo "  make test   - 테스트 실행"
	@echo "  make clean  - 빌드 파일 정리"

setup: ## 개발 환경 설정
	@echo "🚀 개발 환경을 설정합니다..."
	@if [ ! -f .env ]; then \
		echo "❌ .env 파일이 없습니다!"; \
		echo ""; \
		echo "📥 .env 파일을 받는 방법:"; \
		echo "1. Discord #compass-backend 채널 접속"; \
		echo "2. 고정 메시지에서 .env 파일 다운로드"; \
		echo "3. 프로젝트 루트에 복사"; \
		exit 1; \
	else \
		echo "✅ .env 파일이 확인되었습니다."; \
	fi
	@echo "🐳 Docker 서비스를 시작합니다..."
	@docker-compose up -d postgres redis

run: ## 애플리케이션 실행
	@echo "🚀 애플리케이션을 시작합니다..."
	@docker-compose up -d postgres redis
	@source .env && ./gradlew bootRun

test: ## 테스트 실행
	@echo "🧪 테스트를 실행합니다..."
	@source .env && ./gradlew test

clean: ## 빌드 파일 정리
	@echo "🧹 빌드 파일을 정리합니다..."
	@./gradlew clean
	@rm -f gcp-key*.json