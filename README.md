# Compass Travel Planner

Full-stack 여행 계획 생성 서비스의 백엔드(Spring Boot)와 프론트엔드(React)를 한 저장소에서 관리합니다. 이 문서는 로컬 개발 및 배포 시 백엔드와 프론트엔드를 연결하는 방법을 안내합니다.

## 1. 환경 변수 구성

- 백엔드: `src/main/resources/application.yml`에서 기본값을 읽으며, 필요 시 OS 환경 변수 또는 `.env`(개발용)로 덮어쓸 수 있습니다.
- 프론트엔드: `Frontend/.env.example`를 복사해 `.env`를 생성하고 `REACT_APP_API_URL`을 원하는 백엔드 주소로 설정하세요.

```bash
cp Frontend/.env.example Frontend/.env
# 로컬 백엔드가 8080 포트에서 동작한다면 기본값 그대로 사용

# 배포 환경 예시
REACT_APP_API_URL=https://api.my-compass-service.com
```

> `REACT_APP_API_URL`가 비어 있으면 개발 모드에서는 `http://localhost:8080`, 배포 모드에서는 현재 도메인에 상대 경로로 요청합니다.

## 2. 로컬 개발 실행 순서

### 2.1 백엔드 (Spring Boot)

```bash
# 의존성 설치 및 bootJar 생성
./gradlew clean bootJar

# 애플리케이션 실행 (기본 포트 8080)
./gradlew bootRun
```

필요한 경우 Postgres 대신 AWS RDS를, Redis는 docker-compose 또는 로컬 설치를 활용할 수 있습니다. CORS는 개발 프로필에서 `http://localhost:3000`을 허용하도록 설정되어 있습니다.

### 2.2 프론트엔드 (React)

```bash
cd Frontend
npm install
npm start
```

React 개발 서버(`http://localhost:3000`)가 `REACT_APP_API_URL`로 지정된 백엔드에 API 요청을 전달합니다.

## 3. Docker 빌드 & 실행

Dockerfile은 멀티 스테이지 빌드로 구성되어 있어 로컬에 JAR 파일이 없더라도 이미지를 만들 수 있습니다.

```bash
# 이미지 빌드
docker build -t compass-backend .

# 컨테이너 실행 (예시)
docker run -p 8080:8080 --env-file backend.env compass-backend
```

`docker-compose.yml`을 사용할 경우, Redis 컨테이너와 함께 백엔드를 띄울 수 있습니다. AWS RDS를 사용한다면 DB 관련 환경변수를 `.env`에 입력한 뒤 compose를 실행하세요.

```bash
docker compose up --build
```

## 4. 주요 연결 포인트

- 모든 프론트엔드 서비스(`authService`, `chatService`, `tripService`, `followUpService`, `adminAuthService`)는 `src/config/api.ts`의 `API_BASE_URL`을 통해 백엔드 엔드포인트를 공유합니다.
- 백엔드는 `SecurityConfig`에서 CORS를 관리하며, docker 프로필을 사용하면 모든 Origin을 허용합니다. 배포 시에는 허용 Origin을 명시적으로 설정하는 것이 좋습니다.
- 실시간 채팅 흐름을 위해 다음 REST 엔드포인트가 제공됩니다.
  - `POST /api/chat/threads` : 새 스레드 생성
  - `GET /api/chat/threads` : 사용자별 스레드 목록 조회
  - `POST /api/chat/threads/{threadId}/messages` : 메시지 전송 및 AI 응답 수신
  - `POST /api/chat/follow-up/start` / `respond` : 여행 정보 수집용 Follow-up 흐름 시작 및 응답

## 5. 배포 시 권장 사항

1. **Secrets 관리**: OpenAI, Perplexity, AWS, Google Service Account 등 민감 정보는 CI/CD의 secret manager 또는 클라우드 시크릿 매니저를 사용하세요.
2. **헬스 체크**: `/health`, `/actuator/health` 엔드포인트를 활용해 배포 플랫폼(ECS, GKE, Kubernetes 등)에서 liveness/readiness 체크를 설정합니다.
3. **캐시/세션**: Redis는 상태가 필요한 기능(OCR, Stage cache 등)에 사용되므로 매니지드 Redis(ElastiCache 등) 또는 고가용성 구성을 권장합니다.
4. **모니터링**: Spring Actuator + CloudWatch/Prometheus 연동을 통해 Stage별 처리 지표, 오류 로그를 수집합니다.

### 5.1 Vercel + Elastic Beanstalk 연동 가이드

- **백엔드(Elastic Beanstalk)**
  - `CORS_ALLOWED_ORIGINS` 환경 변수를 EB 환경 설정에 추가합니다.<br>
    예: `https://your-frontend.vercel.app`
  - Elastic Beanstalk의 `.ebextensions` 또는 콘솔을 통해 위 값을 설정하면 런타임에서 자동으로 적용됩니다.
  - HTTPS를 사용하는 경우 로드밸런서/ALB가 요청 헤더에 `X-Forwarded-Proto`를 전달하도록 설정하세요.

- **프론트엔드(Vercel)**
  - Vercel 프로젝트 환경 변수에 `REACT_APP_API_URL=https://your-backend.elasticbeanstalk.com`을 입력합니다.
  - Vercel은 빌드 시점에 환경 변수를 읽어 정적 번들을 생성하므로 Production/Preview 환경 각각에 값을 지정해 주세요.
  - 프론트엔드 요청이 HTTPS라면 백엔드도 HTTPS로 접근해야 합니다. (혼합 콘텐츠 차단 예방)

- **기타 체크 포인트**
  - `SecurityConfig`에서 지정한 origin과 실제 배포 URL이 정확히 일치해야 합니다. Vercel의 경우 `https://<project>.vercel.app` 또는 커스텀 도메인을 모두 등록할 수 있습니다.
  - 인증이 필요한 엔드포인트는 JWT 토큰을 전달해야 하므로, 프론트엔드 axios 인터셉터가 올바르게 Authorization 헤더를 세팅하는지 확인합니다.
  - 배포 후 CORS 오류가 발생하면 EB 환경 변수에 설정된 도메인과 실제 요청 도메인이 일치하는지 먼저 확인하세요.

## 6. 문제 해결

- **API 401**: 토큰 만료 시 프론트엔드가 자동으로 refresh를 시도합니다. refresh 실패 시 로그인 페이지로 리다이렉트되는지 확인하세요.
- **CORS 에러**: `SPRING_PROFILES_ACTIVE=docker` 또는 SecurityConfig의 허용 Origin 목록을 점검합니다.
- **빌드 실패**: `./gradlew bootJar` 로그를 확인하고, QueryDSL 등 annotationProcessor 관련 에러가 있는지 살펴보세요.

궁금한 점이 있다면 이 README와 `docs/PROJECT_OVERVIEW.md`를 참고하거나 문의해주세요.
