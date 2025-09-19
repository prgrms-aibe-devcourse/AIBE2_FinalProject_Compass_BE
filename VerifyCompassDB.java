import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VerifyCompassDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://compass-db.ch6mum0221cb.ap-northeast-2.rds.amazonaws.com:5432/compass";
        String username = "compass";
        String password = "compass1004!";

        System.out.println("=== AWS RDS Compass DB 데이터 확인 ===");
        System.out.println("연결 URL: " + url);
        System.out.println();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ RDS 연결 성공!");
            System.out.println();

            // 1. chat_threads 테이블 확인
            System.out.println("1. ChatThread 테이블 데이터 (최근 5개):");
            System.out.println("-".repeat(100));
            String threadQuery = "SELECT id, title, current_phase, created_at FROM chat_threads ORDER BY created_at DESC LIMIT 5";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(threadQuery)) {

                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.printf("Thread %d:\n", count);
                    System.out.printf("  ID: %s\n", rs.getString("id"));
                    System.out.printf("  Title: %s\n", rs.getString("title"));
                    System.out.printf("  Phase: %s\n", rs.getString("current_phase"));
                    System.out.printf("  Created: %s\n", rs.getTimestamp("created_at"));
                    System.out.println();
                }
                if (count == 0) {
                    System.out.println("  ❌ chat_threads 테이블에 데이터가 없습니다!");
                }
            }

            // 2. chat_messages 테이블 확인
            System.out.println("\n2. ChatMessage 테이블 데이터 (최근 10개):");
            System.out.println("-".repeat(100));
            String messageQuery = "SELECT id, thread_id, role, LEFT(content, 100) as content_preview, timestamp FROM chat_messages ORDER BY timestamp DESC LIMIT 10";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(messageQuery)) {

                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.printf("Message %d:\n", count);
                    System.out.printf("  ID: %s\n", rs.getString("id"));
                    System.out.printf("  Thread ID: %s\n", rs.getString("thread_id"));
                    System.out.printf("  Role: %s\n", rs.getString("role"));
                    System.out.printf("  Content: %s...\n", rs.getString("content_preview"));
                    System.out.printf("  Time: %s\n", rs.getTimestamp("timestamp"));
                    System.out.println();
                }
                if (count == 0) {
                    System.out.println("  ❌ chat_messages 테이블에 데이터가 없습니다!");
                }
            }

            // 3. 총 레코드 수 확인
            System.out.println("\n3. 전체 레코드 수:");
            System.out.println("-".repeat(50));

            String countQuery1 = "SELECT COUNT(*) FROM chat_threads";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countQuery1)) {
                if (rs.next()) {
                    System.out.printf("  chat_threads: %d개\n", rs.getInt(1));
                }
            }

            String countQuery2 = "SELECT COUNT(*) FROM chat_messages";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countQuery2)) {
                if (rs.next()) {
                    System.out.printf("  chat_messages: %d개\n", rs.getInt(1));
                }
            }

            // 4. 최근 1시간 내 생성된 쓰레드 확인
            System.out.println("\n4. 최근 1시간 내 생성된 쓰레드:");
            System.out.println("-".repeat(50));
            String recentQuery = "SELECT id, created_at FROM chat_threads WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour' ORDER BY created_at DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(recentQuery)) {

                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.printf("  %s (created: %s)\n",
                        rs.getString("id"),
                        rs.getTimestamp("created_at"));
                }
                if (count == 0) {
                    System.out.println("  최근 1시간 내 생성된 쓰레드가 없습니다.");
                } else {
                    System.out.printf("  총 %d개의 쓰레드가 최근 1시간 내 생성됨\n", count);
                }
            }

            System.out.println("\n=== 확인 완료 ===");

        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 연결 실패!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}