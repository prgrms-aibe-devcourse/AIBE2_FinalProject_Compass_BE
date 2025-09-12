# 사용자 & 인증 API 명세서 (User & Auth API Specification)

이 문서는 Compass 프로젝트의 사용자 및 인증 관련 API의 명세를 정의합니다.

---

## 1. 인증 (Authentication)

### 1.1. 회원가입

* **Description**: 새로운 사용자를 시스템에 등록합니다.
* **Endpoint**: `POST /api/users/signup`
* **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "password": "password123",
      "nickname": "테스트유저"
    }
    ```
* **Success Response (`200 OK`)**:
    ```json
    {
      "id": 1,
      "email": "user@example.com",
      "nickname": "테스트유저"
    }
    ```
* **Failure Response (`400 Bad Request`)**:
    * 이메일 중복 시: `{"message": "이미 존재하는 이메일입니다."}`
    * 유효성 검사 실패 시: `{"message": "잘못된 이메일 형식입니다."}`

### 1.2. 로그인

* **Description**: 이메일과 비밀번호로 사용자를 인증하고, API 접근을 위한 JWT 토큰을 발급합니다.
* **Endpoint**: `POST /api/users/login`
* **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "password": "password123"
    }
    ```
* **Success Response (`200 OK`)**:
    ```json
    {
      "accessToken": "ey...",
      "refreshToken": "ey..."
    }
    ```
* **Failure Response (`400 Bad Request`)**:
    * `{"message": "이메일 또는 비밀번호가 일치하지 않습니다."}`

### 1.3. 로그아웃

* **Description**: 현재 사용자의 세션을 종료하고, 사용된 Access Token을 블랙리스트에 등록하여 무효화합니다.
* **Endpoint**: `POST /api/users/logout`
* **Headers**: `Authorization: Bearer <accessToken>`
* **Success Response (`200 OK`)**:
    ```json
    {
      "message": "로그아웃 되었습니다."
    }
    ```
* **Failure Response (`401 Unauthorized`)**:
    * 유효하지 않은 토큰으로 요청 시 발생합니다.

### 1.4. 토큰 재발급 (Reissue)

*   **Description**: 유효한 Refresh Token을 사용하여 만료된 Access Token을 새로 발급받습니다.
*   **Endpoint**: `POST /api/auth/reissue`
*   **Request Body**:
---

## 2. 프로필 관리 (Profile Management)

> ❗️ **Note**: 아래 모든 API는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.

### 2.1. 내 프로필 조회

* **Description**: 현재 로그인된 사용자의 프로필 정보를 조회합니다.
* **Endpoint**: `GET /api/users/profile`
* **Success Response (`200 OK`)**:
    ```json
    {
      "id": 1,
      "email": "user@example.com",
      "nickname": "테스트유저",
      "profileImageUrl": "[http://example.com/image.jpg](http://example.com/image.jpg)"
    }
    ```

### 2.2. 내 프로필 수정

* **Description**: 현재 로그인된 사용자의 닉네임 또는 프로필 이미지를 수정합니다.
* **Endpoint**: `PATCH /api/users/profile`
* **Request Body**:
    ```json
    {
      "nickname": "새로운닉네임",
      "profileImageUrl": "[http://new.image/url](http://new.image/url)"
    }
    ```
* **Success Response (`200 OK`)**:
    * 수정된 사용자 프로필 정보 (2.1. 응답과 동일)

---

## 3. 사용자 선호도 관리 (User Preference Management)

> ❗️ **Note**: 아래 모든 API는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.

### 3.1. 여행 스타일 선호도 저장/수정

* **Description**: 사용자의 여행 스타일에 대한 가중치를 저장하거나 수정합니다. 값의 총합은 반드시 `1.0`이어야 합니다.
* **Endpoint**: `PUT /api/users/preferences`
* **Request Body**:
    ```json
    {
      "preferences": [
        { "key": "RELAXATION", "value": 0.7 },
        { "key": "ACTIVITY", "value": 0.3 }
      ]
    }
    ```
* **Success Response (`200 OK`)**:
    ```json
    [
      { "preferenceKey": "RELAXATION", "preferenceValue": 0.7 },
      { "preferenceKey": "ACTIVITY", "preferenceValue": 0.3 }
    ]
    ```

### 3.2. 예산 수준 설정

* **Description**: 사용자의 일반적인 예산 수준을 `BUDGET`, `STANDARD`, `LUXURY` 중에서 선택하여 설정합니다.
* **Endpoint**: `PUT /api/users/preferences/budget-level`
* **Request Body**:
    ```json
    {
      "level": "STANDARD"
    }
    ```
* **Success Response (`200 OK`)**:
    ```json
    {
      "preferenceKey": "STANDARD",
      "preferenceValue": 1.0
    }
    ```

### 3.3. AI 기반 선호도 분석

* **Description**: 사용자의 최근 여행 기록 10개를 AI(Gemini)로 분석하여, 복합적인 여행 스타일을 자동으로 분류하고 저장합니다.
* **Endpoint**: `POST /api/users/preferences/analyze`
* **Success Responses**:
    * **`200 OK`**: 분석 결과가 있을 경우, AI가 생성한 타입 반환
        ```json
        {
          "preferenceKey": "ACTIVITY_SOLO_TRAVELER",
          "preferenceValue": 1.0
        }
        ```
    * **`204 No Content`**: 분석할 여행 기록이 없어, 기존 선호도를 보호하고 아무 작업도 하지 않은 경우

---

## 4. 피드백 (Feedback)

> ❗️ **Note**: 아래 API는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.

### 4.1. 피드백 제출

* **Description**: Compass 서비스 전체에 대한 사용자의 만족도 및 의견을 제출합니다.
* **Endpoint**: `POST /api/users/feedback`
* **Request Body**:
    ```json
    {
      "satisfaction": 5,
      "comment": "AI 추천 기능이 매우 만족스러웠습니다.",
      "revisitIntent": true
    }
    ```
* **Success Response (`201 Created`)**:
    ```json
    {
      "id": 1,
      "message": "Feedback submitted successfully."
    }
    ```
* **Failure Response (`400 Bad Request`)**:
    * 만족도 점수가 1~5 범위를 벗어나는 등 유효성 검사 실패 시 발생합니다.

---

## 5. 향후 개선 및 추가 기능 (Future Improvements & To-Do)


### 5.1. 인증 (Authentication)
- **토큰 재발급 로직 강화**: 현재 `reissue` API의 보안 강화를 위해, Redis에 저장된 Refresh Token과 일치하는지 검증하는 로직을 `AuthService`에 추가해야 합니다. (현재는 유효성만 검사)
- **비밀번호 찾기/재설정**: 이메일 인증을 통한 비밀번호 재설정 기능을 추가


### 5.2. 프로필 (Profile)
- **회원 탈퇴**: 사용자가 자신의 계정을 안전하게 비활성화하거나 삭제할 수 있는 `DELETE /api/users/profile` API를 구현
- **이메일/비밀번호 변경**: 보안 검증(예: 현재 비밀번호 확인) 후 이메일 주소나 비밀번호를 변경할 수 있는 기능을 추가

### 5.3. 선호도 (Preference)
- **선택 선호도 종류**: 지금은 선택 선호도가 3개 휴양, 액티비티, 관광으로만 하게 해뒀는데 늘릴 예정.
- **선호도 분석 자동화**: 현재 수동으로 호출하는 AI 선호도 분석(`POST /api/users/preferences/analyze`)을, 사용자가 여행을 마칠 때마다 또는 정기적으로 자동 실행되는 배치(Batch) 작업으로 전환.
- **분석 모델 고도화**: AI 분석 시, `travelStyle` 외에도 예산(`totalBudget`), 동행인(`travelType`), 활동(`preferredActivities`) 등 더 많은 데이터를 종합하여 "가성비 액티비티 여행가"와 같이 더욱 정교한 사용자 타입을 도출하도록 프롬프트를 개선


### 5.4. 피드백 (Feedback)
- **피드백 분석**: 수집된 피드백 `comment`를 AI 감성 분석(Sentiment Analysis)을 통해 긍정/부정으로 자동 분류하고, 주요 키워드를 추출하여 서비스 개선 지표로 활용하는 기능을 구현

### 5.5. 향후 계획
- **코드 정리**: 불 필요한 코드들 정리
- **고도화 작업**: 남아있는 고도화 작업과 테스트 준비