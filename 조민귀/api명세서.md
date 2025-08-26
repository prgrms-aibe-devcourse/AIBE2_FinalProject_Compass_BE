# 여행 매칭 서비스 API 명세서

## 기본 정보
- **Base URL**: `https://api.travel-matching.com/v1`
- **Authentication**: Bearer Token (JWT)
- **Content-Type**: `application/json`
- **Date Format**: ISO 8601 (`2025-01-20T10:00:00Z`)

## 공통 Response 구조

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-01-20T10:00:00Z"
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": { ... }
  },
  "timestamp": "2025-01-20T10:00:00Z"
}
```

### 페이지네이션 응답
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "hasNext": true
    }
  }
}
```

---

## 1. 인증 API

### 1.1 회원가입
**POST** `/auth/signup`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "nickname": "traveler123",
  "bio": "여행을 좋아하는 사람입니다"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "email": "user@example.com",
    "nickname": "traveler123",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

### 1.2 로그인
**POST** `/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

### 1.3 관리자 로그인 (REQ-ADMIN-006)
**POST** `/admin/auth/login`

**Request Body:**
```json
{
  "username": "admin",
  "password": "AdminPassword123!"
}
```

---

## 2. 리뷰 API (REQ-REVIEW)

### 2.1 리뷰 작성 (REQ-REVIEW-001)
**POST** `/reviews`

**Request Body:**
```json
{
  "revieweeId": 67890,
  "matchingId": 11111,
  "title": "즐거운 여행이었습니다",
  "content": "함께 여행하기 좋은 동료였습니다",
  "rating": 5,
  "tags": ["친절함", "시간약속", "재미있음", "배려심", "계획적"]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "reviewId": 99999,
    "reviewerId": 12345,
    "revieweeId": 67890,
    "matchingId": 11111,
    "title": "즐거운 여행이었습니다",
    "content": "함께 여행하기 좋은 동료였습니다",
    "rating": 5,
    "tags": ["친절함", "시간약속", "재미있음", "배려심", "계획적"],
    "status": "PENDING",
    "createdAt": "2025-01-20T10:00:00Z"
  }
}
```

**Business Rules:**
- 매칭 종료 후 14일 이내만 작성 가능
- 태그는 최대 5개
- 별점은 1~5

### 2.2 리뷰 수정
**PUT** `/reviews/{reviewId}`

**Request Body:**
```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "rating": 4,
  "tags": ["친절함", "시간약속"]
}
```

**Business Rules:**
- 작성 후 24시간 내 1회만 수정 가능

### 2.3 리뷰 삭제
**DELETE** `/reviews/{reviewId}`

### 2.4 리뷰 목록 조회 (REQ-REVIEW-003)
**GET** `/reviews`

**Query Parameters:**
- `page`: 페이지 번호 (default: 1)
- `size`: 페이지 크기 (default: 20)
- `sort`: 정렬 기준 (`latest`, `rating_high`, `helpful`) (default: `latest`)
- `ratingMin`: 최소 별점 (1-5)
- `ratingMax`: 최대 별점 (1-5)
- `tags`: 태그 필터 (comma-separated)
- `keyword`: 검색어
- `startDate`: 시작일
- `endDate`: 종료일

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "reviewId": 99999,
        "reviewer": {
          "userId": 12345,
          "nickname": "traveler123",
          "profileImageUrl": "https://...",
          "rating": 4.5,
          "trustScore": 0.95
        },
        "reviewee": {
          "userId": 67890,
          "nickname": "explorer456"
        },
        "title": "즐거운 여행이었습니다",
        "content": "함께 여행하기 좋은 동료였습니다",
        "rating": 5,
        "tags": ["친절함", "시간약속"],
        "helpfulCount": 42,
        "isHelpfulByMe": false,
        "isBestWeekly": true,
        "isBestMonthly": false,
        "createdAt": "2025-01-20T10:00:00Z",
        "isEdited": false
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 150,
      "hasNext": true
    }
  }
}
```

### 2.5 리뷰 상세 조회
**GET** `/reviews/{reviewId}`

### 2.6 리뷰 도움됨 표시 (REQ-REVIEW-005)
**POST** `/reviews/{reviewId}/helpful`

**Response:**
```json
{
  "success": true,
  "data": {
    "isHelpful": true,
    "helpfulCount": 43
  }
}
```

### 2.7 베스트 리뷰 조회 (REQ-REVIEW-004)
**GET** `/reviews/best`

**Query Parameters:**
- `type`: `weekly` | `monthly`
- `limit`: 조회 개수 (default: 10)

---

## 3. 댓글 API (REQ-CMT)

### 3.1 댓글 작성 (REQ-CMT-001)
**POST** `/comments`

**Request Body:**
```json
{
  "reviewId": 99999,
  "content": "좋은 리뷰네요! @explorer456 님도 동의하시나요?",
  "parentCommentId": null
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "commentId": 88888,
    "userId": 12345,
    "user": {
      "nickname": "traveler123",
      "profileImageUrl": "https://..."
    },
    "reviewId": 99999,
    "content": "좋은 리뷰네요! @explorer456 님도 동의하시나요?",
    "depth": 1,
    "mentions": [
      {
        "userId": 67890,
        "nickname": "explorer456"
      }
    ],
    "likeCount": 0,
    "createdAt": "2025-01-20T10:00:00Z"
  }
}
```

### 3.2 대댓글 작성 (REQ-CMT-002)
**POST** `/comments`

**Request Body:**
```json
{
  "reviewId": 99999,
  "parentCommentId": 88888,
  "content": "네, 저도 동의합니다!"
}
```

**Business Rules:**
- Depth는 최대 2까지만 허용

### 3.3 댓글 수정
**PUT** `/comments/{commentId}`

### 3.4 댓글 삭제
**DELETE** `/comments/{commentId}`

**Business Rules:**
- 삭제 시 "삭제된 댓글입니다" 표시
- 관리자는 원본 확인 가능

### 3.5 댓글 목록 조회
**GET** `/reviews/{reviewId}/comments`

**Query Parameters:**
- `sort`: `latest` | `empathy` (default: `latest`)
- `page`: 페이지 번호
- `size`: 페이지 크기

### 3.6 멘션 자동완성 (REQ-CMT-003)
**GET** `/users/autocomplete`

**Query Parameters:**
- `query`: 검색어 (닉네임)
- `limit`: 결과 개수 (default: 5)

---

## 4. 피드 API (REQ-FEED)

### 4.1 피드 작성
**POST** `/feeds`

**Request Body:**
```json
{
  "title": "제주도 여행 후기",
  "content": "3박 4일 제주도 여행 다녀왔습니다...",
  "mediaIds": [123, 124, 125]
}
```

### 4.2 피드 목록 조회 (REQ-FEED-008)
**GET** `/feeds`

**Query Parameters:**
- `cursor`: 마지막 피드 ID (무한 스크롤)
- `size`: 페이지 크기 (default: 20)

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "feedId": 77777,
        "user": {
          "userId": 12345,
          "nickname": "traveler123",
          "profileImageUrl": "https://..."
        },
        "title": "제주도 여행 후기",
        "content": "3박 4일 제주도 여행 다녀왔습니다...",
        "media": [
          {
            "mediaId": 123,
            "type": "IMAGE",
            "thumbnailUrl": "https://...",
            "originalUrl": "https://..."
          }
        ],
        "viewCount": 1523,
        "likeCount": 45,
        "commentCount": 12,
        "createdAt": "2025-01-20T10:00:00Z"
      }
    ],
    "nextCursor": 77776,
    "hasNext": true
  }
}
```

---

## 5. 미디어 API (REQ-MEDIA)

### 5.1 미디어 업로드 (REQ-MEDIA-001, REQ-MEDIA-002)
**POST** `/media/upload`

**Request Headers:**
- `Content-Type`: `multipart/form-data`

**Request Body:**
- `file`: 업로드 파일
- `accessLevel`: `PUBLIC` | `PRIVATE` | `FOLLOWERS_ONLY`

**Response:**
```json
{
  "success": true,
  "data": {
    "mediaId": 123,
    "originalUrl": "https://cdn.example.com/original/...",
    "thumbnails": {
      "64": "https://cdn.example.com/thumb/64/...",
      "320": "https://cdn.example.com/thumb/320/...",
      "720": "https://cdn.example.com/thumb/720/...",
      "1280": "https://cdn.example.com/thumb/1280/..."
    },
    "fileType": "IMAGE",
    "mimeType": "image/jpeg",
    "fileSize": 2048576,
    "dimensions": {
      "width": 1920,
      "height": 1080
    },
    "aiTags": [
      { "tag": "beach", "confidence": 0.92 },
      { "tag": "sunset", "confidence": 0.87 }
    ],
    "nsfwScore": 0.02,
    "createdAt": "2025-01-20T10:00:00Z"
  }
}
```

**Business Rules:**
- 이미지: jpg/png/webp, 최대 10MB
- 영상: mp4/mov, 최대 200MB
- 최대 해상도: 4K
- NSFW 점수 임계치 초과 시 차단

### 5.2 프리사인드 URL 획득 (REQ-MEDIA-003)
**POST** `/media/presigned-url`

**Request Body:**
```json
{
  "fileName": "image.jpg",
  "fileType": "image/jpeg",
  "fileSize": 2048576
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://s3.amazonaws.com/...",
    "mediaId": 123,
    "expiresAt": "2025-01-20T11:00:00Z"
  }
}
```

### 5.3 미디어 처리 상태 조회
**GET** `/media/{mediaId}/status`

---

## 6. 신고 API (REQ-REPORT)

### 6.1 신고 제출 (REQ-REPORT-001)
**POST** `/reports`

**Request Body:**
```json
{
  "targetType": "USER",
  "targetId": 67890,
  "type": "SPAM",
  "description": "스팸 게시물을 반복적으로 올립니다",
  "evidenceUrls": ["https://..."]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "reportId": 55555,
    "status": "PENDING",
    "createdAt": "2025-01-20T10:00:00Z"
  }
}
```

### 6.2 신고 상태 조회
**GET** `/reports/{reportId}`

---

## 7. 매칭 API

### 7.1 매칭 요청
**POST** `/matchings`

**Request Body:**
```json
{
  "responderId": 67890,
  "planId": 33333,
  "message": "함께 여행 가실래요?"
}
```

### 7.2 매칭 응답
**PUT** `/matchings/{matchingId}/respond`

**Request Body:**
```json
{
  "action": "ACCEPT",
  "message": "좋아요!"
}
```

### 7.3 매칭 완료
**PUT** `/matchings/{matchingId}/complete`

**Business Rules:**
- 완료 후 14일 이내 리뷰 작성 가능

---

## 8. 포인트/뱃지 API (REQ-BADGE)

### 8.1 포인트 내역 조회 (REQ-BADGE-001)
**GET** `/users/{userId}/points`

**Query Parameters:**
- `startDate`: 시작일
- `endDate`: 종료일
- `type`: 거래 유형

**Response:**
```json
{
  "success": true,
  "data": {
    "currentBalance": 5420,
    "transactions": [
      {
        "transactionId": 44444,
        "type": "REVIEW_WRITE",
        "amount": 100,
        "balanceAfter": 5420,
        "description": "리뷰 작성 포인트",
        "createdAt": "2025-01-20T10:00:00Z"
      }
    ]
  }
}
```

### 8.2 뱃지 목록 조회 (REQ-BADGE-003)
**GET** `/users/{userId}/badges`

**Response:**
```json
{
  "success": true,
  "data": {
    "badges": [
      {
        "badgeId": 1,
        "name": "첫 리뷰 작성",
        "description": "첫 번째 리뷰를 작성했습니다",
        "iconUrl": "https://...",
        "earnedAt": "2025-01-20T10:00:00Z"
      }
    ],
    "totalCount": 5
  }
}
```

### 8.3 레벨 정보 조회 (REQ-BADGE-004)
**GET** `/users/{userId}/level`

**Response:**
```json
{
  "success": true,
  "data": {
    "currentLevel": 3,
    "currentPoints": 5420,
    "nextLevelPoints": 10000,
    "progress": 54.2,
    "rewards": [
      {
        "type": "COUPON",
        "value": "10% 할인 쿠폰",
        "unlockedAt": "Level 5"
      }
    ]
  }
}
```

---

## 9. 알림 API (REQ-CMT-004)

### 9.1 알림 목록 조회
**GET** `/notifications`

**Query Parameters:**
- `isRead`: 읽음 여부 필터
- `page`: 페이지 번호
- `size`: 페이지 크기

### 9.2 알림 읽음 처리
**PUT** `/notifications/{notificationId}/read`

### 9.3 알림 설정 변경
**PUT** `/users/notification-preferences`

**Request Body:**
```json
{
  "inAppEnabled": true,
  "emailEnabled": false,
  "pushEnabled": true,
  "commentOnReview": true,
  "mention": true,
  "helpfulReceived": false
}
```

---

## 10. 관리자 API (REQ-ADMIN)

### 10.1 회원 관리 (REQ-ADMIN-001)
**PUT** `/admin/users/{userId}/block`

**Request Body:**
```json
{
  "reason": "반복적인 스팸 활동",
  "duration": 30
}
```

### 10.2 피드 관리 (REQ-ADMIN-002)
**PUT** `/admin/feeds/{feedId}/status`

**Request Body:**
```json
{
  "status": "HIDDEN",
  "reason": "부적절한 내용"
}
```

### 10.3 매칭 강제 변경 (REQ-ADMIN-003)
**PUT** `/admin/matchings/{matchingId}/reject`

**Request Body:**
```json
{
  "reason": "사기 의심 계정"
}
```

### 10.4 신고 처리 (REQ-REPORT-002)
**PUT** `/admin/reports/{reportId}/process`

**Request Body:**
```json
{
  "status": "RESOLVED",
  "action": "USER_BLOCKED",
  "actionReason": "스팸 활동 확인"
}
```

### 10.5 관리자 대시보드 (REQ-ADMIN-005)
**GET** `/admin/dashboard`

**Response:**
```json
{
  "success": true,
  "data": {
    "statistics": {
      "totalUsers": 15234,
      "newUsersToday": 45,
      "totalFeeds": 8921,
      "pendingReports": 12
    },
    "monthlySignupTrend": [
      { "month": "2025-01", "count": 523 },
      { "month": "2024-12", "count": 456 }
    ],
    "recentReports": [
      {
        "reportId": 55555,
        "type": "SPAM",
        "targetType": "USER",
        "status": "PENDING",
        "createdAt": "2025-01-20T10:00:00Z"
      }
    ]
  }
}
```

### 10.6 관리자 활동 로그 조회 (REQ-ADMIN-004)
**GET** `/admin/action-logs`

**Query Parameters:**
- `startDate`: 시작일
- `endDate`: 종료일
- `actionType`: 액션 유형
- `adminId`: 관리자 ID

---

## 11. 모더레이션 API (REQ-MOD)

### 11.1 컨텐츠 검토 (REQ-MOD-001)
**GET** `/admin/moderation/pending`

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "contentId": 99999,
        "contentType": "REVIEW",
        "status": "PENDING",
        "content": "리뷰 내용...",
        "reportCount": 3,
        "spamScore": 0.75,
        "createdAt": "2025-01-20T10:00:00Z"
      }
    ]
  }
}
```

### 11.2 스팸 필터 관리 (REQ-MOD-003)
**POST** `/admin/spam-keywords`

**Request Body:**
```json
{
  "keyword": "금지어",
  "severity": "HIGH",
  "autoAction": "HIDE"
}
```

### 11.3 화이트리스트 관리
**POST** `/admin/spam-whitelist`

**Request Body:**
```json
{
  "userId": 12345,
  "reason": "신뢰할 수 있는 사용자",
  "expiresAt": "2025-12-31T23:59:59Z"
}
```

---

## 에러 코드

| Code | HTTP Status | Description |
|------|------------|-------------|
| `AUTH_INVALID_CREDENTIALS` | 401 | 잘못된 인증 정보 |
| `AUTH_TOKEN_EXPIRED` | 401 | 토큰 만료 |
| `AUTH_UNAUTHORIZED` | 403 | 권한 없음 |
| `REVIEW_DEADLINE_EXPIRED` | 400 | 리뷰 작성 기한 초과 (14일) |
| `REVIEW_EDIT_LIMIT_EXCEEDED` | 400 | 리뷰 수정 제한 초과 (24시간/1회) |
| `REVIEW_TAG_LIMIT_EXCEEDED` | 400 | 태그 개수 제한 초과 (5개) |
| `COMMENT_MAX_DEPTH_EXCEEDED` | 400 | 댓글 깊이 초과 (최대 2) |
| `MEDIA_FILE_TOO_LARGE` | 400 | 파일 크기 초과 |
| `MEDIA_INVALID_FORMAT` | 400 | 지원하지 않는 파일 형식 |
| `MEDIA_NSFW_DETECTED` | 400 | NSFW 콘텐츠 감지 |
| `POINT_DAILY_LIMIT_EXCEEDED` | 400 | 일일 포인트 제한 초과 |
| `POINT_ANTIABUSE_TRIGGERED` | 400 | 어뷰징 방지 규칙 위반 |
| `USER_BLOCKED` | 403 | 차단된 사용자 |
| `CONTENT_NOT_FOUND` | 404 | 콘텐츠를 찾을 수 없음 |
| `SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## Rate Limiting

- 일반 API: 100 requests/minute per user
- 미디어 업로드: 10 requests/minute per user
- 검색 API: 30 requests/minute per user
- 관리자 API: 제한 없음

## 성능 목표 (REQ-NFR-PERF-001)

- 피드 리스트 API: P95 응답시간 ≤ 1초 (동시접속 10k 기준)
- 일반 API: P95 응답시간 ≤ 500ms
- 미디어 업로드: P95 처리시간 ≤ 4초 (10MB 이미지 기준)