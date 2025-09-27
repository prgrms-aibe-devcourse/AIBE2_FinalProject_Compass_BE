import java.sql.*;

public class CheckTravelCandidates {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://compass-db.ch6mum0221cb.ap-northeast-2.rds.amazonaws.com:5432/compass";
        String user = "compass";
        String password = "compass1004!";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ compass 데이터베이스 연결 성공!");

            // 테이블 존재 확인
            String checkTable = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'travel_candidates'";
            try (PreparedStatement pstmt = conn.prepareStatement(checkTable);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("✅ travel_candidates 테이블 존재");
                } else {
                    System.out.println("❌ travel_candidates 테이블 없음");
                    return;
                }
            }

            // 전체 데이터 개수
            String countAll = "SELECT COUNT(*) FROM travel_candidates";
            try (PreparedStatement pstmt = conn.prepareStatement(countAll);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("전체 travel_candidates: " + rs.getInt(1) + "개");
                }
            }

            // 서울 지역 데이터
            String countSeoul = "SELECT COUNT(*) FROM travel_candidates WHERE region='서울'";
            try (PreparedStatement pstmt = conn.prepareStatement(countSeoul);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("서울 지역: " + rs.getInt(1) + "개");
                }
            }

            // active 상태 데이터
            String countActive = "SELECT COUNT(*) FROM travel_candidates WHERE is_active=true";
            try (PreparedStatement pstmt = conn.prepareStatement(countActive);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Active 상태: " + rs.getInt(1) + "개");
                }
            }

            // 서울 + Active
            String countSeoulActive = "SELECT COUNT(*) FROM travel_candidates WHERE region='서울' AND is_active=true";
            try (PreparedStatement pstmt = conn.prepareStatement(countSeoulActive);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("서울 + Active: " + rs.getInt(1) + "개");
                }
            }

            // 샘플 데이터 출력
            String sample = "SELECT name, category, google_place_id, rating FROM travel_candidates WHERE region='서울' LIMIT 5";
            try (PreparedStatement pstmt = conn.prepareStatement(sample);
                 ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n서울 지역 샘플 데이터:");
                while (rs.next()) {
                    System.out.printf("- %s (%s) - Google ID: %s, Rating: %.1f\n",
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("google_place_id"),
                        rs.getDouble("rating"));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 에러 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}