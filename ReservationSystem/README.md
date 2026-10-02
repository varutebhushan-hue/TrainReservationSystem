# Online Reservation System (Java Swing + JDBC + MySQL)

## Run (MySQL, default)
1. Install JDK 17+ and Maven; start MySQL.
2. Set credentials (the database `reservation_db` and all tables are created automatically):
   - Linux/macOS: `export DB_USER=root DB_PASS=yourpassword`
   - Windows (PowerShell): `$env:DB_USER="root"; $env:DB_PASS="yourpassword"`
3. `mvn compile exec:java`
4. Log in with **admin / admin123**.

## Run (SQLite, no server needed)
`DB_URL=jdbc:sqlite:reservation.db mvn compile exec:java`

## Demo trains
11111, 22222, 33333, 44444, 55555, 66666 (name auto-fills when typed).
