# Epic 4 Media Pipeline 보강 계획

## 1. 이미지 업로드 (Task 4.1.1)
- MultipartFile → DTO 전환 헬퍼 마련 ✅
- PDF 업로드 시 페이지 처리 / Vision 전송 방식 정비
- 업로드 성공 후 실제 큐 서비스 연동(메시지 브로커 / 이벤트 어댑터)

## 2. S3 업로드 (Task 4.3.1)
- 멀티파트 업로드/Presigned URL 기능 검증 및 예외 보강
- CloudFront 도메인 환경 변수(.env) 및 문서 정리✅

## 3. OCR 실행 (Task 4.1.2)
- 문서 타입별 파서 자동 등록 구조 보완 (항공권/호텔 ✅, 기타 확장 가능하게)
- 품질 지표(길이, 키워드) 기반 재시도 로직 구축 ✅
- Vision API 호출 수/재시도 로깅 강화

## 4. 항공권 정보 추출 (Task 4.2.1)
- IATA 코드 전체 매핑(외부 JSON/DB 로딩) ✅
- FlightReservation JPA 엔티티 전환 + Repository/Service 구성 ✅
- 추출 결과 DB 저장 및 단위 테스트 작성 ✅

## 5. 호텔 예약 정보 추출 (Task 4.2.2)
- ExtractHotelInfoFunction 구현 ✅
- HotelReservation DTO → JPA 엔티티 확장 ✅
- Google Maps 좌표 연동

## 6. 테스트 체계
- Vision/S3 통합 테스트 환경 구성
- 파서 단위 테스트 및 정확도 측정 케이스 준비

> ✅ 표시는 이미 구현된 항목입니다. 나머지는 순차적으로 진행합니다.
