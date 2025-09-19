import java.sql.*;

public class ClearChatData {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://compass-db.ch6mum0221cb.ap-northeast-2.rds.amazonaws.com:5432/compass";
        String username = "compass";
        String password = "compass1004!";

        System.out.println("=== AWS RDS Compass DB 채팅 데이터 삭제 ===");
        System.out.println("연결 URL: " + url);
        System.out.println();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ RDS 연결 성공!");
            System.out.println();

            // 트랜잭션 시작
            conn.setAutoCommit(false);

            try {
                // 1. 삭제 전 레코드 수 확인
                System.out.println("1. 삭제 전 레코드 수:");
                int messagesBeforeCount = getCount(conn, "chat_messages");
                int threadsBeforeCount = getCount(conn, "chat_threads");
                System.out.println("  - chat_messages: " + messagesBeforeCount + "개");
                System.out.println("  - chat_threads: " + threadsBeforeCount + "개");
                System.out.println();

                // 2. chat_messages 테이블 데이터 삭제 (외래 키 때문에 먼저 삭제)
                System.out.println("2. chat_messages 테이블 데이터 삭제 중...");
                String deleteMessagesQuery = "DELETE FROM chat_messages";
                try (Statement stmt = conn.createStatement()) {
                    int deletedMessages = stmt.executeUpdate(deleteMessagesQuery);
                    System.out.println("  ✅ " + deletedMessages + "개의 메시지 삭제 완료");
                }

                // 3. chat_threads 테이블 데이터 삭제
                System.out.println("\n3. chat_threads 테이블 데이터 삭제 중...");
                String deleteThreadsQuery = "DELETE FROM chat_threads";
                try (Statement stmt = conn.createStatement()) {
                    int deletedThreads = stmt.executeUpdate(deleteThreadsQuery);
                    System.out.println("  ✅ " + deletedThreads + "개의 쓰레드 삭제 완료");
                }

                // 4. 커밋
                conn.commit();
                System.out.println("\n✅ 트랜잭션 커밋 완료!");

                // 5. 삭제 후 레코드 수 확인
                System.out.println("\n4. 삭제 후 레코드 수:");
                int messagesAfterCount = getCount(conn, "chat_messages");
                int threadsAfterCount = getCount(conn, "chat_threads");
                System.out.println("  - chat_messages: " + messagesAfterCount + "개");
                System.out.println("  - chat_threads: " + threadsAfterCount + "개");

                System.out.println("\n=== 채팅 데이터 삭제 완료 ===");

            } catch (SQLException e) {
                // 오류 발생 시 롤백
                conn.rollback();
                System.err.println("\n❌ 오류 발생! 롤백 실행됨");
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 연결 실패!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int getCount(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}