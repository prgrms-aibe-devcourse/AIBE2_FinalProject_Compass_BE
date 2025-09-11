# 📝 PR 개요
**관련 이슈**: #87

이 PR은 한국관광공사 Tour API를 활용하여 전국 관광지 데이터를 크롤링하고 저장하는 시스템을 구현합니다. 이 데이터는 향후 AI 개인화 추천 시스템의 핵심 데이터로 활용됩니다. (요구사항 ID: REQ-CRAWL-001)

## 💻 작업 내역
- **Tour API 클라이언트 구현**: KorService2 엔드포인트 지원 (areaBasedList2, detailCommon, locationBasedList, searchKeyword)
- **TourPlace 엔티티 설계**: AI 추천 최적화된 7개 핵심 필드 구조 (contentId, name, category, district, latitude, longitude, details, keywords)
- **TourPlaceRepository 구현**: 지역별, 카테고리별, 키워드 검색, 근처 관광지 검색 쿼리 메서드
- **TourApiService 비즈니스 로직**: Seoul JSON 카테고리를 Tour API contentTypeId로 매핑, 대용량 데이터 수집 및 중복 제거
- **TourApiTestController 테스트 API**: API 연결 테스트, 데이터 수집 검증을 위한 엔드포인트
- **TourApiResponse DTO**: Tour API JSON 응답 파싱을 위한 완전한 DTO 구조
- **Spring Security 설정**: `/api/tour/**` 경로 인증 없이 접근 가능하도록 설정
- **관련 단위 테스트 및 통합 테스트 코드 작성 완료**

## ✔️ 테스트 결과
- ✅ Tour API 연결 및 인증 성공 (인코딩 인증키 적용)
- ✅ 서울 지역 관광지 데이터 수집 성공 (725KB JSON 데이터)
- ✅ KorService2 엔드포인트 정상 작동 확인
- ✅ 데이터 파싱 및 엔티티 변환 로직 검증
- ✅ 모든 컴파일 오류 해결 및 애플리케이션 정상 시작

## 🧐 리뷰 포인트
- **데이터 모델링**: AI 추천에 필요한 7개 핵심 필드만 선별했는데, 추가로 필요한 필드가 있는지 검토 부탁드립니다.
- **API Rate Limiting**: 현재 1초 대기로 설정했는데, 실제 운영 환경에서 적절한지 의견이 궁금합니다.
- **에러 핸들링**: Tour API 응답 오류(`returnReasonCode`)에 대한 처리 로직이 충분한지 검토 부탁드립니다.

## 💡 기타
다음 PR에서는 **REQ-CRAWL-002: Phase별 크롤링**을 구현하여 전국 데이터 수집을 확장할 예정입니다.
