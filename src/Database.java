import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:qualimed.db";

    static {
        initialize();
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private static void initialize() {
        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ex) {
                // Driver may be loaded automatically in newer JDBC versions
            }

            try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS Customer ("
                        + "customer_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "full_name TEXT NOT NULL,"
                        + "email TEXT NOT NULL UNIQUE,"
                        + "password_hash TEXT NOT NULL,"
                        + "contact_number TEXT,"
                        + "address TEXT,"
                        + "is_admin INTEGER DEFAULT 0"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS Product ("
                        + "product_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "product_name TEXT NOT NULL,"
                        + "description TEXT,"
                        + "category TEXT,"
                        + "price REAL NOT NULL,"
                        + "stock_quantity INTEGER NOT NULL DEFAULT 0,"
                        + "expirydate TEXT,"
                        + "image_path TEXT"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS Cart ("
                        + "cart_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "customer_id INTEGER NOT NULL,"
                        + "created_at TEXT,"
                        + "FOREIGN KEY(customer_id) REFERENCES Customer(customer_id)"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS Cart_item ("
                        + "cart_item_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "cart_id INTEGER NOT NULL,"
                        + "product_id INTEGER NOT NULL,"
                        + "quantity INTEGER NOT NULL,"
                        + "FOREIGN KEY(cart_id) REFERENCES Cart(cart_id),"
                        + "FOREIGN KEY(product_id) REFERENCES Product(product_id)"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS \"Order\" ("
                        + "order_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "customer_id INTEGER NOT NULL,"
                        + "order_date TEXT,"
                        + "total_amount REAL,"
                        + "status TEXT,"
                        + "FOREIGN KEY(customer_id) REFERENCES Customer(customer_id)"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS Order_item ("
                        + "order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "order_id INTEGER NOT NULL,"
                        + "product_id INTEGER NOT NULL,"
                        + "quantity INTEGER NOT NULL,"
                        + "subtotal REAL,"
                        + "FOREIGN KEY(order_id) REFERENCES \"Order\"(order_id),"
                        + "FOREIGN KEY(product_id) REFERENCES Product(product_id)"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS Payment ("
                        + "payment_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "order_id INTEGER NOT NULL,"
                        + "payment_method TEXT,"
                        + "payment_status TEXT,"
                        + "payment_date TEXT,"
                        + "FOREIGN KEY(order_id) REFERENCES \"Order\"(order_id)"
                        + ")");

                seedAdmin(conn);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database initialization error: " + ex.getMessage());
        }
    }

    private static void seedAdmin(Connection conn) throws SQLException {
        String adminEmail = "admin@qualimed.com";
        String checkSql = "SELECT customer_id FROM Customer WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, adminEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO Customer (full_name, email, password_hash, contact_number, address, is_admin) "
                + "VALUES (?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, "Administrator");
            ps.setString(2, adminEmail);
            ps.setString(3, hashPassword("admin123"));
            ps.setString(4, "");
            ps.setString(5, "");
            ps.executeUpdate();
        }
    }

    public static boolean createCustomer(String fullName, String email, String password,
                                         String contactNumber, String address) {
        String sql = "INSERT INTO Customer (full_name, email, password_hash, contact_number, address, is_admin) "
                + "VALUES (?, ?, ?, ?, ?, 0)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, hashPassword(password));
            ps.setString(4, contactNumber);
            ps.setString(5, address);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error creating account: " + ex.getMessage());
            return false;
        }
    }

    public static Customer authenticate(String email, String password) {
        String sql = "SELECT customer_id, full_name, is_admin, password_hash FROM Customer WHERE email = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String inputHash = hashPassword(password);
                    if (storedHash != null && storedHash.equals(inputHash)) {
                        Customer c = new Customer();
                        c.customerId = rs.getInt("customer_id");
                        c.fullName = rs.getString("full_name");
                        c.isAdmin = rs.getInt("is_admin") == 1;
                        c.email = email;
                        return c;
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Login error: " + ex.getMessage());
        }
        return null;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static class Customer {
        public int customerId;
        public String fullName;
        public String email;
        public boolean isAdmin;
    }
}

