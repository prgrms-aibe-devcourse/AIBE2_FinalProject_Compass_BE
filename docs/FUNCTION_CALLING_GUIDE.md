# Spring AI Function Calling Guide

## Overview

Function Calling은 LLM이 대화 중 필요한 정보를 얻기 위해 미리 정의된 함수를 자동으로 호출할 수 있게 하는 기능입니다. Spring AI는 이를 간단한 @Bean 등록을 통해 구현할 수 있도록 지원합니다.

## Architecture

```
User Request → LLM → Function Detection → Function Call → Response Integration → Final Answer
```

## Implementation Components

### 1. Function Implementations (`TravelFunctions.java`)

Travel 관련 4개의 핵심 함수가 구현되어 있습니다:

- **searchFlights**: 항공편 검색
- **searchHotels**: 호텔 검색  
- **getWeather**: 날씨 정보 조회
- **searchAttractions**: 관광지 검색

각 함수는 `@Component`로 등록되며, Spring의 `Function` 인터페이스를 구현합니다.

### 2. Function Models

각 함수는 Request/Response 모델을 가집니다:

- `FlightSearchRequest/Response`
- `HotelSearchRequest/Response`
- `WeatherRequest/Response`
- `AttractionSearchRequest/Response`

Request 모델은 `@JsonClassDescription`과 `@JsonPropertyDescription` 어노테이션을 통해 LLM에게 함수 사용법을 설명합니다.

### 3. Configuration (`FunctionCallingConfiguration.java`)

`FunctionCallbackWrapper`를 통해 함수를 LLM에 등록합니다:

```java
@Bean
public FunctionCallback searchFlightsFunctionCallback(ApplicationContext context) {
    return FunctionCallbackWrapper.builder(context.getBean("searchFlights", Function.class))
        .withName("searchFlights")
        .withDescription("Search for available flights")
        .build();
}
```

### 4. Service Layer (`FunctionCallingChatService.java`)

OpenAI와 Gemini 모델 모두에서 Function Calling을 사용할 수 있습니다:

```java
// OpenAI
OpenAiChatOptions.builder()
    .model("gpt-4o-mini")
    .functions(getFunctionNames())
    .build();

// Gemini
VertexAiGeminiChatOptions.builder()
    .model("gemini-2.0-flash")
    .functions(getFunctionNames())
    .build();
```

### 5. REST API (`FunctionCallingController.java`)

3개의 주요 엔드포인트:

- `POST /api/chat/functions/openai` - OpenAI 모델로 대화
- `POST /api/chat/functions/gemini` - Gemini 모델로 대화
- `POST /api/chat/functions/plan-trip` - 종합 여행 계획 생성

## Usage Examples

### 1. Simple Weather Query

```bash
curl -X POST http://localhost:8080/api/chat/functions/gemini \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What's the weather like in Seoul today?"
  }'
```

LLM은 자동으로 `getWeather` 함수를 호출하여 실시간 날씨 정보를 제공합니다.

### 2. Flight Search

```bash
curl -X POST http://localhost:8080/api/chat/functions/openai \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find me flights from Seoul to Tokyo on December 25th, 2024"
  }'
```

LLM은 `searchFlights` 함수를 호출하여 항공편 정보를 검색합니다.

### 3. Complete Trip Planning

```bash
curl -X POST http://localhost:8080/api/chat/functions/plan-trip \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Tokyo",
    "origin": "Seoul",
    "startDate": "2024-12-25",
    "endDate": "2024-12-30",
    "numberOfTravelers": 2,
    "travelStyle": "moderate",
    "interests": ["culture", "food", "shopping"],
    "budgetPerPerson": 2000
  }'
```

LLM은 여러 함수를 조합하여 종합적인 여행 계획을 생성합니다:
- 항공편 검색
- 호텔 추천
- 날씨 예보
- 관광지 추천

## Function Calling Flow

1. **User Request**: 사용자가 정보를 요청
2. **LLM Analysis**: LLM이 요청을 분석하여 필요한 함수 결정
3. **Function Schema**: Spring AI가 함수 스키마를 LLM에 제공
4. **Function Call**: LLM이 적절한 파라미터로 함수 호출
5. **Result Integration**: 함수 결과를 LLM이 자연어로 통합
6. **Final Response**: 사용자에게 최종 응답 전달

## Testing

통합 테스트 실행:

```bash
./gradlew test --tests "*FunctionCallingIntegrationTest*"
```

테스트는 다음을 검증합니다:
- 함수가 Spring Bean으로 등록되었는지
- 각 함수가 올바른 응답을 반환하는지
- Request 모델의 기본값이 제대로 설정되는지

## Configuration

`application.yml`에서 Spring AI 설정:

```yaml
spring:
  ai:
    enabled: true
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
    vertex:
      ai:
        gemini:
          project-id: ${GOOGLE_CLOUD_PROJECT}
          location: ${GOOGLE_CLOUD_LOCATION}
          chat:
            options:
              model: gemini-2.0-flash
```

## Best Practices

1. **함수 이름**: 명확하고 설명적인 이름 사용 (예: `searchFlights`)
2. **설명**: `@JsonClassDescription`으로 함수 목적 명시
3. **파라미터**: `@JsonPropertyDescription`으로 각 파라미터 설명
4. **기본값**: Request 모델 생성자에서 합리적인 기본값 설정
5. **에러 처리**: 함수 실행 실패 시 graceful하게 처리
6. **로깅**: 함수 호출 시 적절한 로깅으로 디버깅 지원

## Extending Functions

새로운 함수 추가 방법:

1. Function 구현체 작성 (`@Component` 필수)
2. Request/Response 모델 정의
3. `FunctionCallingConfiguration`에 FunctionCallback Bean 등록
4. 서비스 레이어에서 함수 활용

## Limitations

현재 구현은 Mock 데이터를 반환합니다. 실제 서비스 연동 시:

1. 외부 API 클라이언트 구현
2. 함수 내부에서 실제 API 호출
3. 응답 매핑 및 에러 처리
4. 캐싱 전략 구현 (Redis 활용)

## Monitoring

Function Calling 메트릭:
- 함수 호출 횟수
- 응답 시간
- 에러율
- 가장 많이 사용되는 함수

Prometheus/Grafana를 통해 모니터링 가능합니다.