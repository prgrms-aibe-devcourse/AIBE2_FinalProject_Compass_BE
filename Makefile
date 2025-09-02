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
		cp .env.example .env; \
		echo "⚠️  .env 파일이 생성되었습니다."; \
		echo "GitHub Secrets 페이지에서 다음 값들을 복사해주세요:"; \
		echo "https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/settings/secrets/actions"; \
		echo ""; \
		echo "필요한 값:"; \
		echo "- GOOGLE_CREDENTIALS_BASE64"; \
		echo "- OPENAI_API_KEY"; \
	else \
		echo "✅ .env 파일이 이미 존재합니다."; \
	fi
	@./scripts/setup-local-env.sh

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