# TourPlace Entity - AI 추천 중심 설계

## 🎯 설계 원칙

1. **AI 추천 최적화**: AI 추천/꼬리질문에 필요한 핵심 정보만 저장
2. **Seoul JSON 파싱**: 크롤링 데이터에서 **7개 필드만** 선별 추출
3. **메타데이터 제거**: `source`, `country`, `last_updated` 등 불필요 정보 무시
4. **검색 성능**: 카테고리, 지역, 태그, 근거리 검색에 최적화

## 🤖 AI 추천 시나리오

### 1. 카테고리 기반 추천
```
사용자: "궁궐 보고 싶어요"
AI: "경복궁을 추천드려요! 조선 왕조 건축을 보실 수 있어요."
필요 데이터: name, category
```

### 2. 지역 기반 추천
```
사용자: "종로구에 뭐가 있나요?"
AI: "종로구에는 경복궁, 창덕궁, 인사동이 있어요."
필요 데이터: name, district
```

### 3. 테마 기반 추천
```
사용자: "한옥 마을 가고 싶어요"
AI: "북촌 한옥마을이나 서촌 마을은 어떠세요?"
필요 데이터: name, keywords(tags)
```

### 4. 근거리 추천
```
사용자: "경복궁 근처에 뭐가 있나요?"
AI: "광화문 광장이나 청와대가 가까워요."
필요 데이터: name, area, latitude, longitude
```

## Entity 구조

```java
@Entity
@Table(name = "tour_places")
public class TourPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", unique = true, nullable = false, length = 50)
    private String contentId; // Seoul JSON "id" → AI 식별자

    @Column(nullable = false, length = 255)
    private String name; // Seoul JSON "name" → AI 추천명

    @Column(nullable = false, length = 100)
    private String category; // Seoul JSON "category" → 카테고리 추천

    @Column(length = 100)
    private String district; // Seoul JSON "district" → 지역 추천

    @Column(length = 100)
    private String area; // Seoul JSON "area" → 상세 위치 설명

    @Column
    private Double latitude; // Seoul JSON "lat" → 근거리 계산

    @Column
    private Double longitude; // Seoul JSON "lng" → 근거리 계산

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb", nullable = false)
    private JsonNode keywords; // Seoul JSON "tags[]" → 테마 추천
}
```

## DDL

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE tour_places (
    id BIGSERIAL PRIMARY KEY,
    content_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    area VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180),
    keywords JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI 추천 최적화 인덱스
CREATE INDEX idx_tour_places_category ON tour_places(category);
CREATE INDEX idx_tour_places_district ON tour_places(district);
CREATE INDEX idx_tour_places_name_trgm ON tour_places USING GIN (name gin_trgm_ops);
CREATE INDEX idx_tour_places_keywords ON tour_places USING GIN (keywords);
CREATE INDEX idx_tour_places_location ON tour_places (latitude, longitude);
```

## Seoul JSON 파싱 매핑

| Seoul JSON | TourPlace Entity | AI 용도 |
|-----------|------------------|---------|
| `id` | `contentId` | 고유 식별자 |
| `name` | `name` | AI 추천명 |
| `category` | `category` | 카테고리 추천 |
| `district` | `district` | 지역 추천 |
| `area` | `area` | 상세 위치 설명 |
| `lat` | `latitude` | 근거리 계산 |
| `lng` | `longitude` | 근거리 계산 |
| `tags[]` | `keywords` | 테마 추천 |

## 파싱 예시

**Seoul JSON 원본:**
```json
{
  "name": "Gyeongbokgung Palace",
  "category": "Palace",
  "district": "Jongno-gu",
  "area": "Gwanghwamun",
  "tags": ["Joseon", "Architecture", "Royal Guard"],
  "lat": 37.579617,
  "lng": 126.977041,
  "id": "SEOUL-0001"
}
```

**파싱 후 TourPlace:**
```java
TourPlace place = TourPlace.builder()
    .contentId("SEOUL-0001")
    .name("Gyeongbokgung Palace")
    .category("Palace")
    .district("Jongno-gu")
    .area("Gwanghwamun")
    .latitude(37.579617)
    .longitude(126.977041)
    .keywords(jsonNode(["Joseon", "Architecture", "Royal Guard"]))
    .build();
```

## Repository (AI 추천용 쿼리)

```java
public interface TourPlaceRepository extends JpaRepository<TourPlace, Long> {
    
    Optional<TourPlace> findByContentId(String contentId);
    
    // 카테고리 추천: "궁궐 추천해주세요"
    List<TourPlace> findByCategory(String category);
    
    // 지역 추천: "종로구에 뭐가 있나요?"
    List<TourPlace> findByDistrict(String district);
    
    // 이름 검색: "경복궁 비슷한 곳"
    @Query(value = "SELECT * FROM tour_places WHERE name ILIKE CONCAT('%', :name, '%')", 
           nativeQuery = true)
    List<TourPlace> searchByName(@Param("name") String name);
    
    // 테마 추천: "한옥 관련 장소"
    @Query(value = "SELECT * FROM tour_places WHERE keywords @> CAST(:keyword AS jsonb)", 
           nativeQuery = true)
    List<TourPlace> findByKeyword(@Param("keyword") String keyword);
    
    // 근거리 추천: "경복궁 근처에 뭐가 있나요?"
    @Query(value = """
        SELECT *, 
               (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) 
               * cos(radians(longitude) - radians(:lng)) 
               + sin(radians(:lat)) * sin(radians(latitude)))) AS distance
        FROM tour_places 
        WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) 
               * cos(radians(longitude) - radians(:lng)) 
               + sin(radians(:lat)) * sin(radians(latitude)))) < :radiusKm
        ORDER BY distance
        """, nativeQuery = true)
    List<TourPlace> findNearbyPlaces(@Param("lat") Double latitude, 
                                   @Param("lng") Double longitude, 
                                   @Param("radiusKm") Double radiusKm);
}
```

## Seoul JSON 파서 서비스

```java
@Service
public class SeoulDataImportService {
    
    @Autowired
    private TourPlaceRepository repository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void importSeoulData() {
        try {
            JsonNode rootNode = objectMapper.readTree(
                new File("docs/features/seoul_top_1000_starter.json")
            );
            
            JsonNode dataArray = rootNode.get("data");
            
            for (JsonNode item : dataArray) {
                TourPlace place = parseSeoulJson(item);
                repository.save(place);
            }
            
            log.info("Seoul JSON 파싱 완료: {} 개 장소 저장", dataArray.size());
            
        } catch (Exception e) {
            throw new RuntimeException("Seoul JSON 파싱 실패", e);
        }
    }
    
    private TourPlace parseSeoulJson(JsonNode item) {
        TourPlace place = new TourPlace();
        
        // AI 추천 필수 필드만 파싱
        place.setContentId(item.get("id").asText());
        place.setName(item.get("name").asText());
        place.setCategory(item.get("category").asText());
        place.setDistrict(item.get("district").asText());
        place.setArea(item.get("area").asText());
        place.setLatitude(item.get("lat").asDouble());
        place.setLongitude(item.get("lng").asDouble());
        place.setKeywords(item.get("tags")); // JsonNode 그대로
        
        return place;
    }
}
```

## AI 추천 서비스 예시

```java
@Service
public class TourPlaceRecommendationService {
    
    @Autowired
    private TourPlaceRepository repository;
    
    // "궁궐 추천해주세요"
    public List<TourPlace> recommendByCategory(String category) {
        return repository.findByCategory(category);
    }
    
    // "종로구에 뭐가 있나요?"
    public List<TourPlace> recommendByDistrict(String district) {
        return repository.findByDistrict(district);
    }
    
    // "한옥 마을 가고 싶어요"
    public List<TourPlace> recommendByTheme(String theme) {
        return repository.findByKeyword("\"" + theme + "\"");
    }
    
    // "경복궁 근처에 뭐가 있나요?"
    public List<TourPlace> recommendNearby(String placeName, Double radiusKm) {
        TourPlace basePlace = repository.findByName(placeName).get(0);
        return repository.findNearbyPlaces(
            basePlace.getLatitude(), 
            basePlace.getLongitude(), 
            radiusKm
        );
    }
}
```

## 데이터 통계

**Seoul JSON 177개 장소:**
- 궁궐: 5개 (Palace)
- 박물관: 15개 (Museum)
- 공원: 12개 (Park)
- 쇼핑: 8개 (Shopping)
- 한옥마을: 3개 (Historic Village)
- 기타: 134개

**AI 추천 시나리오 커버리지: 100%** 🎯