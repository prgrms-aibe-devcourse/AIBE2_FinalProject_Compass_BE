import java.sql.*;

public class CheckJamwonPlace {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://compass-db.ch6mum0221cb.ap-northeast-2.rds.amazonaws.com:5432/compass";
        String user = "compass";
        String password = "compass1004!";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String query = "SELECT id, name, address, latitude, longitude, category " +
                          "FROM travel_candidates " +
                          "WHERE name LIKE '%잠원%' " +
                          "ORDER BY id";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                System.out.println("=== 잠원 관련 장소 DB 조회 결과 ===\n");

                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String name = rs.getString("name");
                    String address = rs.getString("address");
                    Double lat = rs.getDouble("latitude");
                    Double lng = rs.getDouble("longitude");
                    String category = rs.getString("category");

                    System.out.println("ID: " + id);
                    System.out.println("이름: " + name);
                    System.out.println("주소: [" + (address != null ? address : "NULL") + "]");
                    System.out.println("주소 길이: " + (address != null ? address.length() : 0));
                    System.out.println("좌표: (" + lat + ", " + lng + ")");
                    System.out.println("카테고리: " + category);
                    System.out.println("---");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}