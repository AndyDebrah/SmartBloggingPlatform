import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckPassword {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/smart_blog";
        String user = "root";
        String pass = "aND#.814352";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT id, username, password, role FROM users WHERE username='andy'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    System.out.println("ID: " + rs.getLong("id"));
                    System.out.println("Username: " + rs.getString("username"));
                    System.out.println("Password: " + rs.getString("password"));
                    System.out.println("Role: " + rs.getString("role"));
                } else {
                    System.out.println("User 'andy' not found");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
