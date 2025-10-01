import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DeleteAllChats {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://compass-db.ch6mum0221cb.ap-northeast-2.rds.amazonaws.com:5432/postgres";
        String user = "compass";
        String password = System.getenv("DATABASE_PASSWORD");

        if (password == null || password.isEmpty()) {
            password = "compass1004!";
        }

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("연결 성공!");

            // chat_messages 먼저 삭제 (외래키 제약조건)
            int messagesDeleted = stmt.executeUpdate("DELETE FROM chat_messages");
            System.out.println("✅ chat_messages 삭제: " + messagesDeleted + "개");

            // chat_threads 삭제
            int threadsDeleted = stmt.executeUpdate("DELETE FROM chat_threads");
            System.out.println("✅ chat_threads 삭제: " + threadsDeleted + "개");

            System.out.println("\n🎉 모든 대화 내역이 삭제되었습니다!");

        } catch (Exception e) {
            System.err.println("❌ 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}