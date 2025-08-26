네, 알겠습니다. 최종 요구사항 명세서와 ERD 설계를 기반으로, 개발팀이 바로 참고하여 작업할 수 있는 상세 API 명세서를 작성해 드리겠습니다.

RESTful API 원칙을 준수했으며, OpenAPI (Swagger) 명세서로 쉽게 변환할 수 있는 구조로 작성했습니다.

---

### **API 명세서**

#### **1. 기본 정보**

*   **Base URL**: `/api/v1`
*   **데이터 형식**: 모든 요청과 응답의 본문(Body)은 `JSON` 형식을 사용합니다.
*   **인증**: 인증이 필요한 모든 API는 요청 헤더에 `Authorization: Bearer {JWT}` 토큰을 포함해야 합니다.

#### **2. 공통 응답 코드**

*   `200 OK`: 요청 성공 (주로 GET, PUT, PATCH)
*   `201 Created`: 리소스 생성 성공 (주로 POST)
*   `204 No Content`: 요청은 성공했으나 반환할 콘텐츠 없음 (주로 DELETE)
*   `400 Bad Request`: 요청 형식이 잘못됨 (파라미터 누락, 타입 오류 등)
*   `401 Unauthorized`: 인증되지 않은 사용자
*   `403 Forbidden`: 인증은 되었으나 해당 리소스에 접근할 권한이 없음
*   `404 Not Found`: 요청한 리소스가 존재하지 않음
*   `500 Internal Server Error`: 서버 내부 오류

---

### **3. API Endpoints**

#### **3.1. 사용자 및 인증 (Auth & Users)**

*   **`POST /auth/register`**
    *   **설명**: 회원가입
    *   **인증**: 불필요
    *   **Request Body**: `{ "username": "string", "email": "string", "password": "string" }`
    *   **Response (201)**: `{ "user_id": int, "username": "string", "email": "string" }`

*   **`POST /auth/login`**
    *   **설명**: 로그인
    *   **인증**: 불필요
    *   **Request Body**: `{ "email": "string", "password": "string" }`
    *   **Response (200)**: `{ "access_token": "string" }`

*   **`GET /users/me`**
    *   **설명**: 현재 로그인된 사용자 정보 조회 (내 프로필)
    *   **인증**: **필수**
    *   **Response (200)**: `{ "user_id": int, "username": "string", "email": "string", "profile_image_url": "string", "total_points": int, "level": int, "trust_score": float, "storage_used_bytes": int }`

*   **`GET /users/{user_id}`**
    *   **설명**: 특정 사용자 정보 조회 (다른 사람 프로필)
    *   **인증**: 선택
    *   **Response (200)**: `{ "user_id": int, "username": "string", "profile_image_url": "string", "level": int, "badges": [...] }`

#### **3.2. 리뷰 (Reviews)**

*   **`POST /reviews`**
    *   **설명**: 새 리뷰 작성 (REQ-REVIEW-001)
    *   **인증**: **필수**
    *   **Request Body**: `{ "plan_id": int, "title": "string", "body": "string", "rating": float, "tags": ["string"], "media_ids": [int] }`
    *   **Response (201)**: 생성된 리뷰 객체

*   **`GET /reviews`**
    *   **설명**: 리뷰 목록 조회 및 검색 (REQ-REVIEW-003)
    *   **인증**: 선택
    *   **Query Parameters**:
        *   `sort`: `latest` (최신순), `rating_desc` (평점높은순), `helpful_desc` (도움됨순)
        *   `rating_min`, `rating_max`: 별점 범위 (e.g., `4.0`)
        *   `tags`: 쉼표로 구분된 태그 문자열 (e.g., `"맛집,카페"`)
        *   `keyword`: 검색어
        *   `page`, `limit`: 페이지네이션 (e.g., `1`, `20`)
    *   **Response (200)**: `{ "reviews": [...], "pagination": { "total_items": int, "total_pages": int, "current_page": int } }`

*   **`GET /reviews/{review_id}`**
    *   **설명**: 특정 리뷰 상세 조회
    *   **인증**: 선택
    *   **Response (200)**: 리뷰 상세 객체 (작성자 정보, 댓글, 미디어 포함)

*   **`PUT /reviews/{review_id}`**
    *   **설명**: 리뷰 수정 (작성 후 24시간 내, 1회만 가능) (REQ-REVIEW-001)
    *   **인증**: **필수** (작성자 본인)
    *   **Request Body**: `{ "title": "string", "body": "string", "rating": float, "tags": ["string"], "media_ids": [int] }`
    *   **Response (200)**: 수정된 리뷰 객체

*   **`DELETE /reviews/{review_id}`**
    *   **설명**: 리뷰 삭제 (REQ-REVIEW-001)
    *   **인증**: **필수** (작성자 본인 또는 관리자)
    *   **Response (204)**: No Content

*   **`POST /reviews/{review_id}/helpful`**
    *   **설명**: 리뷰에 '도움됨' 토글 (REQ-REVIEW-005)
    *   **인증**: **필수**
    *   **Response (200)**: `{ "review_id": int, "helpful_count": int, "is_helpful_by_user": boolean }`

#### **3.3. 댓글 (Comments)**

*   **`POST /reviews/{review_id}/comments`**
    *   **설명**: 리뷰에 댓글 또는 대댓글 작성 (REQ-CMT-001, 002)
    *   **인증**: **필수**
    *   **Request Body**: `{ "body": "string", "parent_comment_id": int (optional) }`
    *   **Response (201)**: 생성된 댓글 객체

*   **`GET /reviews/{review_id}/comments`**
    *   **설명**: 특정 리뷰의 댓글 목록 조회
    *   **인증**: 선택
    *   **Query Parameters**: `sort` (`latest`, `likes_desc`), `page`, `limit`
    *   **Response (200)**: 댓글 목록 (계층 구조) 및 페이지네이션 정보

*   **`PUT /comments/{comment_id}`**
    *   **설명**: 댓글 수정
    *   **인증**: **필수** (작성자 본인)
    *   **Request Body**: `{ "body": "string" }`
    *   **Response (200)**: 수정된 댓글 객체

*   **`DELETE /comments/{comment_id}`**
    *   **설명**: 댓글 삭제 (Soft Delete)
    *   **인증**: **필수** (작성자 본인 또는 관리자)
    *   **Response (204)**: No Content

#### **3.4. 미디어 (Media)**

*   **`POST /media/upload-url`**
    *   **설명**: 파일(이미지/영상)을 업로드할 Pre-signed URL 요청 (REQ-MEDIA-003)
    *   **인증**: **필수**
    *   **Request Body**: `{ "filename": "string", "content_type": "string", "file_size": int }`
    *   **Response (200)**: `{ "media_id": int, "upload_url": "string" }`
    *   **프로세스**: 클라이언트는 이 응답을 받아 `upload_url`로 실제 파일을 PUT 요청으로 업로드합니다.

#### **3.5. 신고 (Reports)**

*   **`POST /reports`**
    *   **설명**: 콘텐츠(리뷰, 댓글) 또는 사용자 신고 (REQ-MOD-002)
    *   **인증**: **필수**
    *   **Request Body**: `{ "target_type": "review" | "comment" | "user", "target_id": int, "report_type": "SPAM" | "ABUSE" | ..., "description": "string" }`
    *   **Response (201)**: `{ "report_id": int, "status": "PENDING" }`

#### **3.6. 관리자 (Admin)**

*   **`GET /admin/reviews`**
    *   **설명**: 관리자용 리뷰 목록 조회 (상태별 필터링)
    *   **인증**: **필수 (관리자)**
    *   **Query Parameters**: `status` (`PENDING`, `HIDDEN`), `page`, `limit`
    *   **Response (200)**: 리뷰 목록 및 페이지네이션 정보

*   **`PATCH /admin/reviews/{review_id}/status`**
    *   **설명**: 리뷰 상태 변경 (승인/숨김) (REQ-MOD-001)
    *   **인증**: **필수 (관리자)**
    *   **Request Body**: `{ "status": "APPROVED" | "HIDDEN", "reason": "string" }`
    *   **Response (200)**: 상태가 변경된 리뷰 객체

*   **`GET /admin/reports`**
    *   **설명**: 신고 목록 조회
    *   **인증**: **필수 (관리자)**
    *   **Query Parameters**: `status` (`PENDING`, `REVIEWED`), `page`, `limit`
    *   **Response (200)**: 신고 목록 및 페이지네이션 정보

*   **`PATCH /admin/reports/{report_id}`**
    *   **설명**: 신고 처리 (REQ-MOD-002)
    *   **인증**: **필수 (관리자)**
    *   **Request Body**: `{ "status": "RESOLVED", "action_taken": "HIDE_CONTENT" | "WARN_USER" | ... }`
    *   **Response (200)**: 처리된 신고 객체

*   **`PATCH /admin/users/{user_id}/status`**
    *   **설명**: 사용자 계정 상태 변경 (REQ-MOD-004)
    *   **인증**: **필수 (관리자)**
    *   **Request Body**: `{ "status": "ACTIVE" | "SUSPENDED" | "BANNED", "reason": "string" }`
    *   **Response (200)**: 상태가 변경된 사용자 객체