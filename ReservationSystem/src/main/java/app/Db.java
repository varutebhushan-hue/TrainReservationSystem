package app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.HexFormat;
import java.util.Optional;

/** All database access. using PreparedStatement */
public final class Db {
    static final String URL  = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3306/reservation_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
    static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    static final String PASS = System.getenv().getOrDefault("DB_PASS", "");

    private static final SecureRandom RNG = new SecureRandom();

    public record Booking(String pnr, String passenger, int trainNo, String trainName,
                          String classType, String date, String source, String destination) {
        @Override public String toString() {
            return "PNR              : " + pnr + "\n"
                 + "Passenger        : " + passenger + "\n"
                 + "Train            : " + trainNo + " - " + trainName + "\n"
                 + "Class            : " + classType + "\n"
                 + "Date of Journey  : " + date + "\n"
                 + "From             : " + source + "\n"
                 + "To               : " + destination;
        }
    }

    private Db() {}

    static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    static String hash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** Creates tables and seeds demo data. */
    static void init() throws SQLException {
        try (Connection c = get(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (username VARCHAR(50) PRIMARY KEY, password_hash VARCHAR(64) NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS trains (train_no INT PRIMARY KEY, train_name VARCHAR(100) NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS reservations ("
                    + "pnr VARCHAR(10) PRIMARY KEY, passenger_name VARCHAR(100) NOT NULL, train_no INT NOT NULL, "
                    + "train_name VARCHAR(100) NOT NULL, class_type VARCHAR(20) NOT NULL, journey_date VARCHAR(10) NOT NULL, "
                    + "source VARCHAR(100) NOT NULL, destination VARCHAR(100) NOT NULL)");

            if (count(c, "users") == 0) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO users VALUES (?, ?)")) {
                    ps.setString(1, "admin"); ps.setString(2, hash("admin123")); ps.executeUpdate();
                }
            }
            if (count(c, "trains") == 0) {
                Object[][] t = {{12951, "Mumbai Rajdhani"}, {12627, "Karnataka Express"}, {12123, "Deccan Queen"},
                {11007, "Deccan Express"}, {12301, "Howrah Rajdhani"}, {12009, "Shatabdi Express"},
                {12345, "Your Train Name"}};
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO trains VALUES (?, ?)")) {
                    for (Object[] r : t) { ps.setInt(1, (Integer) r[0]); ps.setString(2, (String) r[1]); ps.executeUpdate(); }
                }
            }
        }
    }

    private static int count(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next(); return rs.getInt(1);
        }
    }

    static boolean login(String user, String pass) throws SQLException {
        try (Connection c = get();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username = ? AND password_hash = ?")) {
            ps.setString(1, user); ps.setString(2, hash(pass));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    static Optional<String> trainName(int trainNo) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement("SELECT train_name FROM trains WHERE train_no = ?")) {
            ps.setInt(1, trainNo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(); }
        }
    }

    /** Inserts the booking with a unique random 10-digit PNR  */
    static String book(String name, int trainNo, String trainName, String cls, String date, String src, String dst) throws SQLException {
        String sql = "INSERT INTO reservations (pnr, passenger_name, train_no, train_name, class_type, journey_date, source, destination) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = get()) {
            for (int attempt = 0; attempt < 5; attempt++) {
                String pnr = String.valueOf(1_000_000_000L + (long) (RNG.nextDouble() * 9_000_000_000L));
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, pnr); ps.setString(2, name); ps.setInt(3, trainNo); ps.setString(4, trainName);
                    ps.setString(5, cls); ps.setString(6, date); ps.setString(7, src); ps.setString(8, dst);
                    ps.executeUpdate();
                    return pnr;
                } catch (SQLIntegrityConstraintViolationException dup) {
                    // duplicate key -> you should try a new PNR
                } catch (SQLException e) {
                    if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("unique")) throw e; // SQLite duplicate
                }
            }
            throw new SQLException("Could not generate a unique PNR. Please try again.");
        }
    }

    static Optional<Booking> find(String pnr) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(
                "SELECT pnr, passenger_name, train_no, train_name, class_type, journey_date, source, destination FROM reservations WHERE pnr = ?")) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Booking(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)));
            }
        }
    }

    static boolean cancel(String pnr) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement("DELETE FROM reservations WHERE pnr = ?")) {
            ps.setString(1, pnr);
            return ps.executeUpdate() > 0;
        }
    }
}
