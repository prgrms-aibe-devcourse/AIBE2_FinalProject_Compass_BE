-- H2 데이터베이스 테스트용 샘플 데이터
INSERT INTO tour_places (content_id, name, category, district, area, latitude, longitude, area_code, content_type_id, address, image_url, data_source, crawled_at, created_at, updated_at) VALUES
('1', '경복궁', '관광지', '종로구', '서울', 37.579617, 126.977041, '1', '12', '서울특별시 종로구 사직로 161', 'https://example.com/gyeongbokgung.jpg', 'tour_api', NOW(), NOW(), NOW()),
('2', '창덕궁', '관광지', '종로구', '서울', 37.579617, 126.991241, '1', '12', '서울특별시 종로구 율곡로 99', 'https://example.com/changdeokgung.jpg', 'tour_api', NOW(), NOW(), NOW()),
('3', '남산타워', '관광지', '용산구', '서울', 37.551169, 126.988205, '1', '12', '서울특별시 용산구 남산공원길 105', 'https://example.com/namsantower.jpg', 'tour_api', NOW(), NOW(), NOW()),
('4', '명동', '쇼핑', '중구', '서울', 37.563569, 126.982611, '1', '38', '서울특별시 중구 명동', 'https://example.com/myeongdong.jpg', 'tour_api', NOW(), NOW(), NOW()),
('5', '인사동', '쇼핑', '종로구', '서울', 37.573569, 126.982611, '1', '38', '서울특별시 종로구 인사동', 'https://example.com/insadong.jpg', 'tour_api', NOW(), NOW(), NOW()),
('6', '한강공원', '관광지', '용산구', '서울', 37.521169, 126.988205, '1', '12', '서울특별시 용산구 한강대로', 'https://example.com/hangang.jpg', 'tour_api', NOW(), NOW(), NOW()),
('7', '롯데월드', '관광지', '송파구', '서울', 37.511169, 127.098205, '1', '12', '서울특별시 송파구 올림픽로 240', 'https://example.com/lotteworld.jpg', 'tour_api', NOW(), NOW(), NOW()),
('8', '동대문시장', '쇼핑', '중구', '서울', 37.563569, 127.008205, '1', '38', '서울특별시 중구 을지로 281', 'https://example.com/dongdaemun.jpg', 'tour_api', NOW(), NOW(), NOW()),
('9', '홍대', '쇼핑', '마포구', '서울', 37.556569, 126.922611, '1', '38', '서울특별시 마포구 홍익로', 'https://example.com/hongdae.jpg', 'tour_api', NOW(), NOW(), NOW()),
('10', '강남', '쇼핑', '강남구', '서울', 37.497569, 127.027611, '1', '38', '서울특별시 강남구 강남대로', 'https://example.com/gangnam.jpg', 'tour_api', NOW(), NOW(), NOW());